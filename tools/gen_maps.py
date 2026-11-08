#!/usr/bin/env python3
"""Generate the three level maps and check map PNGs.

Writes (256x256 opaque RGB PNG):
  core/assets/maps/prototype.png   (id 0)
  core/assets/maps/sprint.png      (id 1)
  core/assets/maps/release.png     (id 2)

Every pixel uses a color from ColorMapper. The output is deterministic: each
map has a fixed seed, and the noise is plain numpy value noise.

Each map is built in this order:
  1. base floor from noise (stone / grass / blackstone / sand)
  2. rock walls from noise plus a ragged border
  3. liquid pools (water with deep water inside, oil, lava)
  4. resource veins: code vein (iron floor), iGPU stock (coal floor),
     PCIe stock (uranium floor) and a few decorative titanium patches
  5. a carved corridor from every enemy spawn to the player spawn
  6. open area around the player spawn and around every enemy spawn
  7. player spawn (green) and enemy spawns (red)

Rules checked after generation and by --check (see check_map()):
  - 256x256, opaque, only ColorMapper colors
  - exactly one 00ff00 pixel, 1..15 ff0000 pixels
  - all six resource colors present (c3a490 161616 83bc58 506eb4 292929 ed5334)
  - the starting base footprint (World.setDefaultBlocks(): core 3x3 at the
    green pixel, 8 human-engineers, 2 turrets, conveyors) plus a margin is
    plain floor: no walls, liquids, resource floors or spawns
  - no resource floor closer than RESOURCE_SPAWN_DIST tiles to a red pixel
  - every red pixel reaches the green pixel over walkable tiles (4-neighbour
    BFS; walls and liquid floors are solid, see Tile.passable()), also with a
    3x3 clearance
  - each resource color has a tile reachable from the green pixel (ores: the
    tile itself, liquids: a reachable tile next to it)

Coordinates in this file are PNG coordinates (x right, y down).
WorldGenerator reads pixel (x, height-1-y), so world y = 255 - png y, and the
base that setDefaultBlocks() builds below the core (world y-5..y+1) is at png
y-1..y+5.

Needs numpy and Pillow.

Usage (from the repository root):
    python3 tools/gen_maps.py                 # write the 3 maps and check them
    python3 tools/gen_maps.py --check A.png   # only check existing PNGs
"""

import argparse
import os
import sys
from collections import deque

import numpy as np
from PIL import Image

MAP_DIR = "core/assets/maps"
SIZE = 256

# ColorMapper colors (hex -> role)
GREEN = 0x00FF00
RED = 0xFF0000
STONE, STONE_WALL = 0x323232, 0x646464
GRASS, GRASS_WALL = 0x50965A, 0x5AB464
BLACK, BLACK_WALL = 0x252525, 0x575757
SAND, SAND_WALL = 0x988A67, 0xE5D8BB
SNOW, ICE, SNOW_WALL = 0xC2D1D2, 0xC4E3E7, 0xF7FEFF
DIRT = 0x6E501E
WATER, DEEP_WATER = 0x506EB4, 0x465A96
LAVA, OIL = 0xED5334, 0x292929
IRON, COAL, TITANIUM, URANIUM = 0xC3A490, 0x161616, 0x6277BC, 0x83BC58

COLOR_MAPPER = {
    RED, GREEN, STONE, STONE_WALL, GRASS, GRASS_WALL, WATER, DEEP_WATER,
    BLACK, BLACK_WALL, SAND, SAND_WALL, SNOW, ICE, SNOW_WALL, DIRT, LAVA, OIL,
    IRON, COAL, TITANIUM, URANIUM,
}
WALLS = {STONE_WALL, GRASS_WALL, BLACK_WALL, SAND_WALL, SNOW_WALL}
LIQUIDS = {WATER, DEEP_WATER, LAVA, OIL}
ORES = {IRON, COAL, TITANIUM, URANIUM}
WALL_OF = {STONE: STONE_WALL, GRASS: GRASS_WALL, BLACK: BLACK_WALL, SAND: SAND_WALL}

# resources the game needs on every map: code vein, iGPU, PCIe, water, oil, lava
REQUIRED = [("code vein (iron)", IRON), ("iGPU (coal)", COAL), ("PCIe (uranium)", URANIUM),
            ("water", WATER), ("oil", OIL), ("lava", LAVA)]
RESOURCE_COLORS = ORES | LIQUIDS
LIQUID_ADJ = {WATER: {WATER, DEEP_WATER}, OIL: {OIL}, LAVA: {LAVA}}

MAX_SPAWNS = 15
# no-build radius around enemy spawns is 8.125 tiles; keep resources out of it
RESOURCE_SPAWN_DIST = 9
# setDefaultBlocks() footprint relative to the green pixel, png coordinates
FOOTPRINT = (-3, 3, -1, 5)  # x0, x1, y0, y1 (inclusive)
FOOTPRINT_MARGIN = 2
# walls / liquids are kept out of this radius around the green pixel
BASE_OPEN_RADIUS = 14
# walls / liquids are kept out of this radius around red pixels
SPAWN_OPEN_RADIUS = 4
# carved spawn -> base corridor: bend points and half width
CORRIDOR_POINTS = 4
CORRIDOR_RADIUS = 2.5
# minimum distance of spawns from the map edge
EDGE_MARGIN = 6


# ---------------------------------------------------------------- noise

def value_noise(rng, cell):
    """Smooth value noise in [0, 1] with features of about `cell` pixels."""
    n = SIZE // cell + 2
    grid = rng.random((n, n))
    t = np.arange(SIZE) / cell
    i = t.astype(int)
    f = t - i
    f = f * f * (3 - 2 * f)
    iy, ix = np.meshgrid(i, i, indexing="ij")
    fy, fx = np.meshgrid(f, f, indexing="ij")
    a = grid[iy, ix]
    b = grid[iy, ix + 1]
    c = grid[iy + 1, ix]
    d = grid[iy + 1, ix + 1]
    top = a + (b - a) * fx
    bottom = c + (d - c) * fx
    return top + (bottom - top) * fy


def fractal(rng, cells):
    total = np.zeros((SIZE, SIZE))
    weight = 0.0
    w = 1.0
    for cell in cells:
        total += value_noise(rng, cell) * w
        weight += w
        w *= 0.5
    total /= weight
    lo, hi = total.min(), total.max()
    return (total - lo) / (hi - lo)


YY, XX = np.mgrid[0:SIZE, 0:SIZE]


def dist(x, y):
    return np.hypot(XX - x, YY - y)


def blob(rng, x, y, r, rough=0.45):
    """Irregular disc of radius about r."""
    n = value_noise(rng, max(4, int(r)))
    return dist(x, y) < r * (1 - rough / 2 + rough * n)


# ---------------------------------------------------------------- maps

# Each map: seed, floors (noise thresholds), wall threshold, spawns,
# liquid pools, resource veins. Positions are png coordinates.
MAPS = [
    {
        "name": "prototype",
        "seed": 1001,
        # wide open grassland: few walls, one spawn far from the base
        "floors": [(0.0, STONE), (0.30, GRASS), (0.82, SAND)],
        "wall_threshold": 0.80,
        "wall_cells": (48, 24, 12),
        "green": (70, 150),
        "reds": [(226, 44)],
        "pools": [
            (WATER, 150, 180, 14), (WATER, 40, 228, 8),
            (OIL, 52, 62, 7), (OIL, 196, 214, 5),
            (LAVA, 214, 140, 6),
        ],
        "veins": [
            (IRON, 96, 146, 7), (IRON, 52, 182, 6), (IRON, 108, 110, 6),
            (IRON, 150, 120, 8), (IRON, 30, 120, 5), (IRON, 180, 70, 6),
            (COAL, 92, 186, 5), (COAL, 40, 96, 5), (COAL, 128, 220, 6),
            (URANIUM, 118, 168, 4), (URANIUM, 26, 160, 4), (URANIUM, 170, 238, 5),
            (TITANIUM, 200, 100, 4), (TITANIUM, 90, 30, 4),
        ],
        "corridor_wiggle": 20,
    },
    {
        "name": "sprint",
        "seed": 2002,
        # rocky ridges cut by a river; two spawns on the far side
        "floors": [(0.0, BLACK), (0.38, STONE), (0.72, GRASS)],
        "wall_threshold": 0.66,
        "wall_cells": (40, 20, 10),
        "green": (128, 206),
        "reds": [(36, 30), (220, 34)],
        "river": {"y": 120, "amp": 18, "period": 140, "half_width": 5},
        "pools": [
            (OIL, 50, 170, 7), (OIL, 214, 186, 6),
            (LAVA, 128, 58, 7), (LAVA, 226, 236, 4),
        ],
        "veins": [
            (IRON, 100, 196, 6), (IRON, 160, 220, 6), (IRON, 128, 166, 5),
            (IRON, 70, 226, 5), (IRON, 196, 150, 6), (IRON, 60, 140, 5),
            (COAL, 92, 160, 5), (COAL, 178, 196, 5), (COAL, 128, 96, 5),
            (URANIUM, 164, 166, 4), (URANIUM, 36, 206, 4), (URANIUM, 84, 76, 5),
            (TITANIUM, 230, 120, 4), (TITANIUM, 20, 100, 4),
        ],
        "corridor_wiggle": 28,
    },
    {
        "name": "release",
        "seed": 3003,
        # dark caves with lava; four spawns in the corners, base in the middle
        "floors": [(0.0, BLACK), (0.50, STONE), (0.86, SAND)],
        "wall_threshold": 0.58,
        "wall_cells": (32, 16, 8),
        "green": (128, 124),
        "reds": [(22, 22), (234, 24), (24, 234), (232, 232)],
        "pools": [
            (LAVA, 70, 128, 8), (LAVA, 190, 120, 7), (LAVA, 128, 40, 6), (LAVA, 128, 214, 6),
            (WATER, 168, 176, 7), (WATER, 64, 74, 6),
            (OIL, 92, 182, 6), (OIL, 186, 66, 5),
        ],
        "veins": [
            (IRON, 104, 110, 5), (IRON, 152, 106, 5), (IRON, 128, 156, 6),
            (IRON, 90, 150, 5), (IRON, 170, 146, 5), (IRON, 128, 80, 5),
            (COAL, 100, 84, 4), (COAL, 160, 86, 4), (COAL, 142, 186, 5),
            (URANIUM, 110, 196, 4), (URANIUM, 206, 170, 4), (URANIUM, 50, 104, 4),
            (TITANIUM, 214, 90, 4), (TITANIUM, 40, 160, 4),
        ],
        "corridor_wiggle": 24,
    },
]


def footprint_box(gx, gy, margin):
    x0, x1, y0, y1 = FOOTPRINT
    return (gx + x0 - margin, gx + x1 + margin, gy + y0 - margin, gy + y1 + margin)


def box_mask(box):
    x0, x1, y0, y1 = box
    return (XX >= x0) & (XX <= x1) & (YY >= y0) & (YY <= y1)


def carve_line(mask, ax, ay, bx, by, radius):
    steps = int(max(abs(bx - ax), abs(by - ay))) + 1
    for t in np.linspace(0, 1, steps):
        x = ax + (bx - ax) * t
        y = ay + (by - ay) * t
        mask |= dist(x, y) <= radius


def generate(spec):
    rng = np.random.default_rng(spec["seed"])
    gx, gy = spec["green"]
    reds = spec["reds"]

    # 1. base floor
    biome = fractal(rng, (64, 32, 16))
    floor = np.zeros((SIZE, SIZE), dtype=np.int64)
    for threshold, color in spec["floors"]:
        floor[biome >= threshold] = color
    img = floor.copy()

    # 2. walls from noise plus a ragged border
    walln = fractal(rng, spec["wall_cells"])
    edge = np.minimum(np.minimum(XX, SIZE - 1 - XX), np.minimum(YY, SIZE - 1 - YY))
    border = edge < 2 + 4 * value_noise(rng, 16)
    wall = (walln > spec["wall_threshold"]) | border
    wall_colors = np.vectorize(WALL_OF.get)(floor)
    img[wall] = wall_colors[wall]

    # 3. liquids
    if "river" in spec:
        r = spec["river"]
        center = r["y"] + r["amp"] * np.sin(XX * 2 * np.pi / r["period"])
        width = r["half_width"] * (0.7 + 0.6 * value_noise(rng, 16))
        off = np.abs(YY - center)
        img[off < width] = WATER
        img[off < width * 0.45] = DEEP_WATER
    for color, x, y, rad in spec["pools"]:
        m = blob(rng, x, y, rad)
        img[m] = color
        if color == WATER:
            img[blob(rng, x, y, rad * 0.5) & m] = DEEP_WATER

    # 4. resource veins (overwrite walls so the ore is open ground)
    for color, x, y, rad in spec["veins"]:
        img[blob(rng, x, y, rad)] = color

    # 5. corridors from each spawn to the base
    corridor = np.zeros((SIZE, SIZE), dtype=bool)
    for rx, ry in reds:
        # polyline through CORRIDOR_POINTS points shifted sideways at random
        dx, dy = gx - rx, gy - ry
        length = max(1.0, float(np.hypot(dx, dy)))
        points = [(rx, ry)]
        for i in range(1, CORRIDOR_POINTS + 1):
            t = i / (CORRIDOR_POINTS + 1)
            off = rng.uniform(-1, 1) * spec["corridor_wiggle"]
            points.append((rx + dx * t - dy / length * off, ry + dy * t + dx / length * off))
        points.append((gx, gy))
        for (ax, ay), (bx, by) in zip(points, points[1:]):
            carve_line(corridor, ax, ay, bx, by, CORRIDOR_RADIUS)
    blocked = np.isin(img, list(WALLS | LIQUIDS))
    img[corridor & blocked] = floor[corridor & blocked]

    # 6. open areas around the base and the spawns
    blocked = np.isin(img, list(WALLS | LIQUIDS))
    around_base = dist(gx, gy) <= BASE_OPEN_RADIUS
    img[around_base & blocked] = floor[around_base & blocked]
    fp = box_mask(footprint_box(gx, gy, FOOTPRINT_MARGIN))
    img[fp] = floor[fp]
    for rx, ry in reds:
        blocked = np.isin(img, list(WALLS | LIQUIDS))
        near = dist(rx, ry) <= SPAWN_OPEN_RADIUS
        img[near & blocked] = floor[near & blocked]
        ores = np.isin(img, list(RESOURCE_COLORS))
        near = dist(rx, ry) < RESOURCE_SPAWN_DIST
        img[near & ores] = floor[near & ores]

    # 7. spawns
    img[gy, gx] = GREEN
    for rx, ry in reds:
        img[ry, rx] = RED
    return img


# ---------------------------------------------------------------- check

def to_rgb_ints(path):
    im = Image.open(path)
    errors = []
    if im.size != (SIZE, SIZE):
        errors.append(f"size is {im.size[0]}x{im.size[1]}, expected {SIZE}x{SIZE}")
    if im.mode == "RGBA":
        alpha = np.array(im)[:, :, 3]
        if (alpha != 255).any():
            errors.append(f"{int((alpha != 255).sum())} pixels are not opaque")
    elif im.mode != "RGB":
        errors.append(f"mode is {im.mode}, expected RGB or RGBA")
    a = np.array(im.convert("RGB")).astype(np.int64)
    return (a[:, :, 0] << 16) | (a[:, :, 1] << 8) | a[:, :, 2], errors


def bfs(walk, start):
    seen = np.zeros_like(walk)
    sx, sy = start
    if not walk[sy, sx]:
        return seen
    seen[sy, sx] = True
    q = deque([(sx, sy)])
    h, w = walk.shape
    while q:
        x, y = q.popleft()
        for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if 0 <= nx < w and 0 <= ny < h and walk[ny, nx] and not seen[ny, nx]:
                seen[ny, nx] = True
                q.append((nx, ny))
    return seen


def check_map(img):
    """Returns (errors, stats) for a map given as a HxW array of 0xRRGGBB."""
    errors = []
    stats = {}
    colors, counts = np.unique(img, return_counts=True)
    unknown = [f"{c:06x}" for c in colors if int(c) not in COLOR_MAPPER]
    if unknown:
        errors.append("colors not in ColorMapper: " + ", ".join(unknown))
    histogram = {int(c): int(n) for c, n in zip(colors, counts)}
    stats["histogram"] = histogram

    gys, gxs = np.nonzero(img == GREEN)
    rys, rxs = np.nonzero(img == RED)
    stats["green"] = list(zip(gxs.tolist(), gys.tolist()))
    stats["reds"] = list(zip(rxs.tolist(), rys.tolist()))
    if len(gxs) != 1:
        errors.append(f"{len(gxs)} green (00ff00) pixels, expected 1")
    if not 1 <= len(rxs) <= MAX_SPAWNS:
        errors.append(f"{len(rxs)} red (ff0000) pixels, expected 1..{MAX_SPAWNS}")
    for x, y in stats["reds"]:
        if min(x, y, SIZE - 1 - x, SIZE - 1 - y) < EDGE_MARGIN:
            errors.append(f"red pixel ({x},{y}) is closer than {EDGE_MARGIN} to the edge")

    for label, color in REQUIRED:
        if histogram.get(color, 0) == 0:
            errors.append(f"no {label} ({color:06x}) pixels")

    resource = np.isin(img, list(RESOURCE_COLORS))
    for x, y in stats["reds"]:
        near = (dist(x, y) < RESOURCE_SPAWN_DIST) & resource
        if near.any():
            errors.append(f"{int(near.sum())} resource pixels closer than {RESOURCE_SPAWN_DIST} to red ({x},{y})")

    walk = ~np.isin(img, list(WALLS | LIQUIDS))
    if len(gxs) == 1:
        gx, gy = int(gxs[0]), int(gys[0])
        box = footprint_box(gx, gy, FOOTPRINT_MARGIN)
        stats["footprint"] = box
        x0, x1, y0, y1 = box
        if x0 < 0 or y0 < 0 or x1 >= SIZE or y1 >= SIZE:
            errors.append(f"base footprint {box} leaves the map")
        inside = img[max(0, y0):y1 + 1, max(0, x0):x1 + 1]
        bad = np.isin(inside, list(WALLS | LIQUIDS | ORES | {RED}))
        if bad.any():
            errors.append(f"{int(bad.sum())} wall/liquid/resource/spawn pixels in the base footprint {box}")

        reach = bfs(walk, (gx, gy))
        # 3x3 clearance: a tile counts only if its 8 neighbours are walkable too
        pad = np.pad(walk, 1, constant_values=False)
        clear = np.ones_like(walk)
        for dy in (0, 1, 2):
            for dx in (0, 1, 2):
                clear &= pad[dy:dy + SIZE, dx:dx + SIZE]
        reach_clear = bfs(clear, (gx, gy))
        stats["bfs"] = []
        for x, y in stats["reds"]:
            ok, ok_clear = bool(reach[y, x]), bool(reach_clear[y, x])
            stats["bfs"].append(((x, y), ok, ok_clear))
            if not ok:
                errors.append(f"red ({x},{y}) cannot reach green over walkable tiles")
            if not ok_clear:
                errors.append(f"red ({x},{y}) cannot reach green with 3x3 clearance")

        # resource access from the base
        pad_reach = np.pad(reach, 1, constant_values=False)
        next_to_reach = (pad_reach[:-2, 1:-1] | pad_reach[2:, 1:-1] |
                         pad_reach[1:-1, :-2] | pad_reach[1:-1, 2:])
        stats["reachable"] = {}
        for label, color in REQUIRED:
            if color in LIQUID_ADJ:
                m = np.isin(img, list(LIQUID_ADJ[color]))
                n = int((m & next_to_reach).sum())
            else:
                m = img == color
                n = int((m & reach).sum())
            stats["reachable"][color] = (n, int(m.sum()))
            if m.any() and n == 0:
                errors.append(f"no {label} pixel is reachable from the base")
    return errors, stats


def report(name, errors, stats):
    h = stats["histogram"]
    print(f"== {name}")
    print(f"  green: {stats['green']}  reds ({len(stats['reds'])}): {stats['reds']}")
    if "footprint" in stats:
        print(f"  base footprint incl. margin (x0, x1, y0, y1): {stats['footprint']}")
    parts = [f"{label} {h.get(color, 0)}" for label, color in REQUIRED]
    parts.append(f"deep water {h.get(DEEP_WATER, 0)}")
    parts.append(f"titanium {h.get(TITANIUM, 0)}")
    print("  pixels: " + ", ".join(parts))
    walls = sum(h.get(c, 0) for c in WALLS)
    floors = sum(h.get(c, 0) for c in (STONE, GRASS, BLACK, SAND))
    print(f"  walls {walls}, plain floor (stone/grass/blackstone/sand) {floors}")
    if "reachable" in stats:
        parts = [f"{label} {n}/{total}" for label, color in REQUIRED
                 for n, total in [stats["reachable"][color]]]
        print("  reachable from base: " + ", ".join(parts))
    if "bfs" in stats:
        for (x, y), ok, ok_clear in stats["bfs"]:
            print(f"  BFS red ({x},{y}) -> green: {'OK' if ok else 'NG'}, 3x3 clearance: {'OK' if ok_clear else 'NG'}")
    if errors:
        for e in errors:
            print("  ERROR: " + e)
    else:
        print("  OK")


def check_file(path):
    img, errors = to_rgb_ints(path)
    if img.shape != (SIZE, SIZE):
        print(f"== {path}")
        for e in errors:
            print("  ERROR: " + e)
        return False
    more, stats = check_map(img)
    errors += more
    report(path, errors, stats)
    return not errors


def write_png(img, path):
    rgb = np.stack([(img >> 16) & 0xFF, (img >> 8) & 0xFF, img & 0xFF], axis=-1).astype(np.uint8)
    Image.fromarray(rgb, "RGB").save(path, optimize=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("--check", nargs="+", metavar="PNG", help="only check these PNG files")
    parser.add_argument("--out", default=MAP_DIR, help="output directory (default: %(default)s)")
    args = parser.parse_args()

    if args.check:
        ok = all([check_file(p) for p in args.check])
        sys.exit(0 if ok else 1)

    ok = True
    for spec in MAPS:
        img = generate(spec)
        errors, _ = check_map(img)
        if errors:
            ok = False
            print(f"== {spec['name']}: generated map breaks the rules, not written")
            for e in errors:
                print("  ERROR: " + e)
            continue
        path = os.path.join(args.out, spec["name"] + ".png")
        write_png(img, path)
        ok = check_file(path) and ok
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
