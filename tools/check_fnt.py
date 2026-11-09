#!/usr/bin/env python3
# Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
"""Check a .fnt/.png pair written by bdf2fnt.py.

Checks:
  - the header lines are in the order libGDX BitmapFontData.load() expects
  - one page, and the page file exists next to the .fnt
  - char ids are unique, ascending, and all in the character list
  - every char rectangle lies inside the page and does not overlap another
  - the PNG is 8-bit RGBA of size scaleW x scaleH, RGB white, alpha only 0/255
  - every non-empty glyph has at least one set pixel; no set pixel lies
    outside the glyph rectangles
Prints the characters of the list that are not in the font.

Optionally renders --sample text with the font metrics into a PNG, enlarged
by --sample-scale with nearest neighbour (pixel repetition), white on black.

Only the Python 3 standard library is used.

Usage (from the repository root):
    python3 tools/check_fnt.py --fnt core/assets/ui/misaki.fnt --chars tools/chars_ja.txt
"""

import argparse
import os
import struct
import sys
import zlib


def fail(message):
    print("error: " + message, file=sys.stderr)
    sys.exit(1)


def parse_fields(line):
    fields = {}
    for token in line.split()[1:]:
        key, _, value = token.partition("=")
        fields[key] = value.strip('"')
    return fields


def read_fnt(path):
    with open(path, "r", encoding="ascii") as f:
        lines = f.read().split("\n")
    if not lines[0].startswith("info ") or "padding=0,0,0,0 " not in lines[0]:
        fail("line 1 must be 'info ... padding=a,b,c,d ...'")
    common = lines[1].split(" ")
    if common[0] != "common" or not common[1].startswith("lineHeight=") \
            or not common[2].startswith("base=") or common[5] != "pages=1":
        fail("line 2 must be 'common lineHeight= base= scaleW= scaleH= pages=1 ...'")
    if not lines[2].startswith("page id=0 "):
        fail("line 3 must be 'page id=0 file=...'")
    if not lines[3].startswith("chars count="):
        fail("line 4 must be 'chars count=N'")
    common_f = parse_fields(lines[1])
    page_file = parse_fields(lines[2])["file"]
    count = int(parse_fields(lines[3])["count"])
    chars = []
    for line in lines[4:]:
        if not line:
            continue
        if not line.startswith("char "):
            fail("unexpected line: %r" % line)
        keys = [t.partition("=")[0] for t in line.split()[1:]]
        if keys[:8] != ["id", "x", "y", "width", "height", "xoffset", "yoffset", "xadvance"]:
            fail("char fields out of order: %r" % line)
        chars.append({k: int(v) for k, v in parse_fields(line).items()})
    if len(chars) != count:
        fail("chars count=%d but %d char lines" % (count, len(chars)))
    return common_f, page_file, chars


def read_png(path):
    with open(path, "rb") as f:
        data = f.read()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        fail("%s is not a PNG" % path)
    pos = 8
    idat = b""
    ihdr = None
    while pos < len(data):
        length = struct.unpack(">I", data[pos:pos + 4])[0]
        kind = data[pos + 4:pos + 8]
        payload = data[pos + 8:pos + 8 + length]
        crc = struct.unpack(">I", data[pos + 8 + length:pos + 12 + length])[0]
        if zlib.crc32(kind + payload) & 0xFFFFFFFF != crc:
            fail("bad CRC in %s chunk" % kind.decode("ascii"))
        if kind == b"IHDR":
            ihdr = struct.unpack(">IIBBBBB", payload)
        elif kind == b"IDAT":
            idat += payload
        pos += 12 + length
    width, height, depth, color, _, _, interlace = ihdr
    if depth != 8 or color != 6 or interlace != 0:
        fail("PNG must be 8-bit RGBA, not interlaced (got depth=%d color=%d interlace=%d)"
             % (depth, color, interlace))
    raw = zlib.decompress(idat)
    stride = width * 4
    rows = []
    for y in range(height):
        start = y * (stride + 1)
        if raw[start] != 0:
            fail("row %d uses PNG filter %d; this checker reads filter 0 only" % (y, raw[start]))
        rows.append(raw[start + 1:start + 1 + stride])
    return width, height, rows


def read_chars(path):
    codes = []
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            if line.strip():
                codes.append(int(line.split(" ", 1)[0][2:], 16))
    return codes


def render_sample(text, glyphs, rows, line_height, scale, out_path):
    pen = 0
    placed = []
    for ch in text:
        g = glyphs.get(ord(ch))
        if g is None:
            fail("sample character %r is not in the font" % ch)
        placed.append((pen, g))
        pen += g["xadvance"]
    width, height = pen, line_height
    canvas = [[0] * width for _ in range(height)]
    for px, g in placed:
        for gy in range(g["height"]):
            for gx in range(g["width"]):
                if rows[g["y"] + gy][(g["x"] + gx) * 4 + 3]:
                    cx = px + g["xoffset"] + gx
                    cy = g["yoffset"] + gy
                    if 0 <= cx < width and 0 <= cy < height:
                        canvas[cy][cx] = 255
    raw = bytearray()
    for y in range(height * scale):
        raw.append(0)
        for x in range(width * scale):
            v = canvas[y // scale][x // scale]
            raw += bytes((v, v, v, 255))
    ihdr = struct.pack(">IIBBBBB", width * scale, height * scale, 8, 6, 0, 0, 0)

    def chunk(kind, payload):
        body = kind + payload
        return struct.pack(">I", len(payload)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    with open(out_path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr)
                + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b""))
    print("sample: %r -> %s (%dx%d)" % (text, out_path, width * scale, height * scale))


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--fnt", required=True)
    ap.add_argument("--chars", required=True)
    ap.add_argument("--sample", help="text to render")
    ap.add_argument("--sample-out", help="PNG path for --sample")
    ap.add_argument("--sample-scale", type=int, default=4)
    args = ap.parse_args()

    common, page_file, chars = read_fnt(args.fnt)
    scale_w, scale_h = int(common["scaleW"]), int(common["scaleH"])
    png_path = os.path.join(os.path.dirname(args.fnt), page_file)
    expected_png = os.path.splitext(os.path.basename(args.fnt))[0] + ".png"
    if page_file != expected_png:
        fail("page file %r should be %r (ucore Skin looks for <fnt name>.png)" % (page_file, expected_png))
    if not os.path.isfile(png_path):
        fail("page file %s does not exist" % png_path)

    ids = [c["id"] for c in chars]
    if ids != sorted(set(ids)):
        fail("char ids are not unique and ascending")
    requested = read_chars(args.chars)
    extra = sorted(set(ids) - set(requested))
    if extra:
        fail("font has ids not in the list: %s" % extra)
    missing = [cp for cp in requested if cp not in set(ids)]

    width, height, rows = read_png(png_path)
    if (width, height) != (scale_w, scale_h):
        fail("PNG is %dx%d, fnt says %dx%d" % (width, height, scale_w, scale_h))

    covered = [[False] * width for _ in range(height)]
    for c in chars:
        if c["page"] != 0:
            fail("char %d is on page %d" % (c["id"], c["page"]))
        x, y, w, h = c["x"], c["y"], c["width"], c["height"]
        if w == 0 or h == 0:
            continue
        if x < 0 or y < 0 or x + w > width or y + h > height:
            fail("char %d rect (%d,%d %dx%d) is outside the page" % (c["id"], x, y, w, h))
        set_pixels = 0
        for gy in range(y, y + h):
            for gx in range(x, x + w):
                if covered[gy][gx]:
                    fail("char %d overlaps another glyph at (%d,%d)" % (c["id"], gx, gy))
                covered[gy][gx] = True
                if rows[gy][gx * 4 + 3]:
                    set_pixels += 1
        if set_pixels == 0:
            fail("char %d has an empty bitmap" % c["id"])

    alpha_values = set()
    rgb_values = set()
    stray = 0
    for y in range(height):
        row = rows[y]
        for x in range(width):
            a = row[x * 4 + 3]
            alpha_values.add(a)
            rgb_values.add(bytes(row[x * 4:x * 4 + 3]))
            if a and not covered[y][x]:
                stray += 1
    if not alpha_values <= {0, 255}:
        fail("alpha values other than 0/255: %s" % sorted(alpha_values - {0, 255}))
    if rgb_values != {b"\xff\xff\xff"}:
        fail("RGB is not white everywhere")
    if stray:
        fail("%d set pixels outside glyph rectangles" % stray)

    glyphs = {c["id"]: c for c in chars}
    print("fnt:      %s" % args.fnt)
    print("page:     %s %dx%d" % (page_file, width, height))
    print("chars:    %d (list has %d)" % (len(chars), len(requested)))
    print("alpha:    %s" % sorted(alpha_values))
    print("rects:    all inside the page, no overlaps, no stray pixels")
    print("missing:  %s" % " ".join("U+%04X" % cp for cp in missing))
    if args.sample:
        if not args.sample_out:
            fail("--sample needs --sample-out")
        render_sample(args.sample, glyphs, rows, int(common["lineHeight"]),
                      args.sample_scale, args.sample_out)


if __name__ == "__main__":
    main()
