#!/usr/bin/env python3
# Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
"""Generate the base images for code item icons and the credit icon.

Writes (8x8 RGBA PNG, transparent background):
  core/assets-raw/sprites/code-base/q<stage>-i<importance>.png  (14 files)
  core/assets-raw/sprites/icon-credit.png

Stage sets the color (design.md, IconGenerator table), importance sets the
shape: 1 = circle, 2 = star. Both shapes fit in the top-left 7x7 area, so the
bottom row stays transparent; IconGenerator puts the passed dots at
(5,7), (6,7), (7,7) and refuses base images whose dot pixels are not
transparent.

Only the Python 3 standard library is used.

Usage (from the repository root):
    python3 tools/gen_code_base.py
"""

import os
import struct
import zlib

SPRITES = "core/assets-raw/sprites"
BASE_DIR = os.path.join(SPRITES, "code-base")
SIZE = 8

# stage -> fill color (design.md §2.2 IconGenerator)
STAGE_COLORS = [
    0xE0342C,
    0xE8702A,
    0xE8B02A,
    0xB8C832,
    0x5EC8A0,
    0x3A9EE0,
    0x2A5AE8,
]

# '#' = shape pixel. Rows top to bottom, 8 columns; row 7 must stay empty.
SHAPES = {
    1: [  # circle
        "..###...",
        ".#####..",
        "#######.",
        "#######.",
        "#######.",
        ".#####..",
        "..###...",
        "........",
    ],
    2: [  # star
        "...#....",
        "...#....",
        "#######.",
        ".#####..",
        "..###...",
        ".##.##..",
        ".#...#..",
        "........",
    ],
}

CREDIT_SHAPE = [  # coin: o = outline, f = fill, s = slot
    "..oooo..",
    ".offffo.",
    "offsffo.",
    "offsffo.",
    "offsffo.",
    "offsffo.",
    ".offffo.",
    "..oooo..",
]
CREDIT_COLORS = {"o": 0xA07818, "f": 0xF0C838, "s": 0xA07818}


def rgba(color, alpha=255):
    return ((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, alpha)


def write_png(path, pixels):
    """pixels: list of rows, each a list of (r, g, b, a)."""
    raw = b"".join(b"\x00" + b"".join(bytes(p) for p in row) for row in pixels)

    def chunk(kind, data):
        body = kind + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    header = struct.pack(">IIBBBBB", SIZE, SIZE, 8, 6, 0, 0, 0)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(png)


def code_pixels(mask, color):
    """Flat fill: the stage is read from the color, so no shading."""
    return [[rgba(color) if c == "#" else (0, 0, 0, 0) for c in row] for row in mask]


def main():
    for importance, mask in SHAPES.items():
        if len(mask) != SIZE or any(len(r) != SIZE for r in mask):
            raise SystemExit("shape %d is not %dx%d" % (importance, SIZE, SIZE))
        if any(mask[SIZE - 1][x] != "." for x in (5, 6, 7)):
            raise SystemExit("shape %d covers a dot pixel" % importance)

    os.makedirs(BASE_DIR, exist_ok=True)
    count = 0
    for stage, color in enumerate(STAGE_COLORS):
        for importance, mask in SHAPES.items():
            write_png(os.path.join(BASE_DIR, "q%d-i%d.png" % (stage, importance)), code_pixels(mask, color))
            count += 1

    credit = [[rgba(CREDIT_COLORS[c]) if c in CREDIT_COLORS else (0, 0, 0, 0) for c in row] for row in CREDIT_SHAPE]
    write_png(os.path.join(SPRITES, "icon-credit.png"), credit)

    print("wrote %d base images to %s and %s" % (count, BASE_DIR, os.path.join(SPRITES, "icon-credit.png")))


if __name__ == "__main__":
    main()
