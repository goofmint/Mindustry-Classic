#!/usr/bin/env python3
# Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
"""Convert a BDF bitmap font into an AngelCode BMFont (text .fnt + one .png page).

Glyphs are copied dot for dot at their original size (no scaling, no
interpolation). The PNG is 8-bit RGBA, white RGB, alpha 0 or 255.

Only the Python 3 standard library is used. The output is deterministic for the
same inputs.

Usage (from the repository root):
    python3 tools/bdf2fnt.py --zip tools/fonts/misaki/misaki_bdf_2021-05-05.zip \
        --chars tools/chars_ja.txt --out core/assets/ui/misaki

writes core/assets/ui/misaki.fnt and core/assets/ui/misaki.png.

The BDF must be encoded in ISO 10646 (Unicode): CHARSET_REGISTRY "ISO10646"
and CHARSET_ENCODING "1". The Misaki BDF files since 2019-06-03 are, so the
ENCODING value of each glyph is used directly as the Unicode code point. Any
other registry is rejected instead of being guessed.
"""

import argparse
import os
import struct
import sys
import zipfile
import zlib

DEFAULT_BDF_NAME = "misaki_gothic_2nd.bdf"

# The conversion fails when any of these is not in the BDF.
REQUIRED_TEXT = "日本語設計書レビュー済"

PAGE_SIZES = (256, 512, 1024)
SPACING = 1  # empty pixels between glyphs and around the page edge


def fail(message):
    print("error: " + message, file=sys.stderr)
    sys.exit(1)


# ---------------------------------------------------------------------------
# BDF


def parse_bdf(text):
    """Parse a BDF file. Returns (header dict, {codepoint: glyph dict})."""
    lines = text.split("\n")
    header = {}
    glyphs = {}
    i = 0
    n = len(lines)

    def ints(values, count, what, line_no):
        if len(values) != count:
            fail("BDF line %d: %s needs %d numbers" % (line_no, what, count))
        return [int(v) for v in values]

    while i < n:
        line = lines[i].strip()
        i += 1
        if not line:
            continue
        parts = line.split()
        keyword = parts[0]
        if keyword == "FONTBOUNDINGBOX":
            header["FONTBOUNDINGBOX"] = ints(parts[1:], 4, keyword, i)
        elif keyword == "SIZE":
            header["SIZE"] = ints(parts[1:], 3, keyword, i)
        elif keyword in ("FONT_ASCENT", "FONT_DESCENT"):
            header[keyword] = ints(parts[1:], 1, keyword, i)[0]
        elif keyword in ("CHARSET_REGISTRY", "CHARSET_ENCODING", "FAMILY_NAME"):
            header[keyword] = line[len(keyword):].strip().strip('"')
        elif keyword == "STARTCHAR":
            start = i
            glyph = {"name": line[len("STARTCHAR"):].strip()}
            while True:
                if i >= n:
                    fail("BDF line %d: STARTCHAR without ENDCHAR" % start)
                line = lines[i].strip()
                i += 1
                parts = line.split()
                if not parts:
                    continue
                kw = parts[0]
                if kw == "ENCODING":
                    glyph["encoding"] = int(parts[1])
                elif kw == "DWIDTH":
                    glyph["dwidth"] = ints(parts[1:], 2, kw, i)
                elif kw == "BBX":
                    glyph["bbx"] = ints(parts[1:], 4, kw, i)
                elif kw == "BITMAP":
                    rows = []
                    while True:
                        if i >= n:
                            fail("BDF line %d: BITMAP without ENDCHAR" % start)
                        row = lines[i].strip()
                        i += 1
                        if row == "ENDCHAR":
                            break
                        rows.append(row)
                    glyph["rows"] = rows
                    break
                elif kw == "ENDCHAR":
                    fail("BDF line %d: glyph %r has no BITMAP" % (start, glyph["name"]))
            for key in ("encoding", "dwidth", "bbx", "rows"):
                if key not in glyph:
                    fail("BDF line %d: glyph %r has no %s" % (start, glyph["name"], key))
            glyph["pixels"] = decode_rows(glyph, start)
            cp = glyph["encoding"]
            if cp < 0:
                continue  # unencoded glyph
            if cp in glyphs:
                fail("BDF line %d: duplicate ENCODING %d" % (start, cp))
            glyphs[cp] = glyph
    for key in ("FONTBOUNDINGBOX", "SIZE", "FONT_ASCENT", "FONT_DESCENT",
                "CHARSET_REGISTRY", "CHARSET_ENCODING", "FAMILY_NAME"):
        if key not in header:
            fail("BDF has no %s" % key)
    return header, glyphs


def decode_rows(glyph, line_no):
    """Return the glyph bitmap as a list of rows of 0/1 values (BBX size)."""
    w, h = glyph["bbx"][0], glyph["bbx"][1]
    rows = glyph["rows"]
    if w == 0 or h == 0:
        if any(r for r in rows):
            fail("BDF line %d: empty BBX but bitmap data in %r" % (line_no, glyph["name"]))
        return []
    if len(rows) != h:
        fail("BDF line %d: %r has %d bitmap rows, BBX height is %d"
             % (line_no, glyph["name"], len(rows), h))
    nbytes = (w + 7) // 8
    pixels = []
    for row in rows:
        if len(row) != nbytes * 2:
            fail("BDF line %d: %r bitmap row %r is not %d hex digits"
                 % (line_no, glyph["name"], row, nbytes * 2))
        bits = int(row, 16)
        total = nbytes * 8
        pixels.append([(bits >> (total - 1 - x)) & 1 for x in range(w)])
    return pixels


def read_bdf(zip_path, bdf_name, bdf_path):
    if bdf_path:
        with open(bdf_path, "rb") as f:
            data = f.read()
        source = bdf_path
    else:
        with zipfile.ZipFile(zip_path) as z:
            names = z.namelist()
            if bdf_name not in names:
                fail("%s is not in %s (contains: %s)" % (bdf_name, zip_path, ", ".join(names)))
            data = z.read(bdf_name)
        source = "%s!%s" % (zip_path, bdf_name)
    return source, data.decode("ascii")


# ---------------------------------------------------------------------------
# Character list


def read_chars(path):
    """Read tools/chars_ja.txt ("U+XXXX <char>" per line)."""
    codes = []
    with open(path, "r", encoding="utf-8") as f:
        for line_no, line in enumerate(f, 1):
            line = line.rstrip("\n")
            if not line:
                continue
            token = line.split(" ", 1)[0]
            if not token.startswith("U+"):
                fail("%s:%d: expected U+XXXX, got %r" % (path, line_no, line))
            codes.append(int(token[2:], 16))
    if codes != sorted(set(codes)):
        fail("%s: code points must be unique and in ascending order" % path)
    return codes


# ---------------------------------------------------------------------------
# Packing


def pack(glyph_list, size):
    """Shelf-pack glyphs in the given order. Returns {cp: (x, y)} or None."""
    positions = {}
    x = y = SPACING
    shelf_h = 0
    for cp, glyph in glyph_list:
        w, h = glyph["bbx"][0], glyph["bbx"][1]
        if w == 0 or h == 0:
            positions[cp] = (0, 0)
            continue
        if x + w + SPACING > size:
            x = SPACING
            y += shelf_h + SPACING
            shelf_h = 0
        if y + h + SPACING > size:
            return None
        positions[cp] = (x, y)
        x += w + SPACING
        shelf_h = max(shelf_h, h)
    return positions


# ---------------------------------------------------------------------------
# PNG


def png_chunk(kind, payload):
    body = kind + payload
    return struct.pack(">I", len(payload)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)


def write_png(path, size, alpha):
    """alpha: bytearray size*size of 0/255. Writes 8-bit RGBA, white RGB."""
    raw = bytearray()
    for y in range(size):
        raw.append(0)  # filter type: None
        row = alpha[y * size:(y + 1) * size]
        for a in row:
            raw += b"\xff\xff\xff" + bytes((a,))
    ihdr = struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0)
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n")
        f.write(png_chunk(b"IHDR", ihdr))
        f.write(png_chunk(b"IDAT", zlib.compress(bytes(raw), 9)))
        f.write(png_chunk(b"IEND", b""))


# ---------------------------------------------------------------------------


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    src = ap.add_mutually_exclusive_group(required=True)
    src.add_argument("--zip", help="zip archive that contains the BDF")
    src.add_argument("--bdf-path", help="BDF file on disk")
    ap.add_argument("--bdf-name", default=DEFAULT_BDF_NAME, help="BDF file name inside --zip")
    ap.add_argument("--chars", required=True, help="character list from collect_chars.py")
    ap.add_argument("--out", required=True, help="output path without extension")
    args = ap.parse_args()

    source, text = read_bdf(args.zip, args.bdf_name, args.bdf_path)
    header, glyphs = parse_bdf(text)
    if header["CHARSET_REGISTRY"].upper() != "ISO10646" or header["CHARSET_ENCODING"] != "1":
        fail("BDF charset is %s-%s; only ISO10646-1 (Unicode) is supported"
             % (header["CHARSET_REGISTRY"], header["CHARSET_ENCODING"]))

    requested = read_chars(args.chars)
    missing = [cp for cp in requested if cp not in glyphs]
    required_missing = [ch for ch in REQUIRED_TEXT if ord(ch) not in glyphs]
    if required_missing:
        fail("required characters missing from the BDF: %s" % "".join(required_missing))
    selected = [(cp, glyphs[cp]) for cp in requested if cp in glyphs]

    page = None
    positions = None
    for size in PAGE_SIZES:
        positions = pack(selected, size)
        if positions is not None:
            page = size
            break
    if page is None:
        fail("%d glyphs do not fit in a %dx%d page" % (len(selected), PAGE_SIZES[-1], PAGE_SIZES[-1]))

    ascent = header["FONT_ASCENT"]
    descent = header["FONT_DESCENT"]
    line_height = ascent + descent
    base = ascent

    alpha = bytearray(page * page)
    char_lines = []
    for cp, glyph in selected:
        w, h, bx, by = glyph["bbx"]
        x, y = positions[cp]
        if w == 0 or h == 0:
            w = h = 0
            yoffset = 0
        else:
            yoffset = base - (by + h)
            for gy, row in enumerate(glyph["pixels"]):
                for gx, bit in enumerate(row):
                    if bit:
                        alpha[(y + gy) * page + (x + gx)] = 255
        char_lines.append(
            "char id=%d x=%d y=%d width=%d height=%d xoffset=%d yoffset=%d xadvance=%d page=0 chnl=15"
            % (cp, x, y, w, h, bx, yoffset, glyph["dwidth"][0]))

    out_dir = os.path.dirname(args.out)
    if out_dir and not os.path.isdir(out_dir):
        fail("output directory %s does not exist" % out_dir)
    png_name = os.path.basename(args.out) + ".png"
    face = header["FAMILY_NAME"]
    if not face.isascii() or '"' in face:
        fail("font face name must be ASCII without quotes: %r" % face)

    # libGDX BitmapFontData.load() reads the info line up to the space after
    # padding=..., and the common line split on single spaces with lineHeight,
    # base and pages at fixed positions. Keep this field order.
    fnt = [
        'info face="%s" size=%d bold=0 italic=0 charset="" unicode=1 stretchH=100 smooth=0 aa=1 '
        'padding=0,0,0,0 spacing=%d,%d outline=0' % (face, header["SIZE"][0], SPACING, SPACING),
        "common lineHeight=%d base=%d scaleW=%d scaleH=%d pages=1 packed=0 "
        "alphaChnl=0 redChnl=4 greenChnl=4 blueChnl=4" % (line_height, base, page, page),
        'page id=0 file="%s"' % png_name,
        "chars count=%d" % len(char_lines),
    ] + char_lines
    with open(args.out + ".fnt", "w", encoding="ascii", newline="\n") as f:
        f.write("\n".join(fnt) + "\n")
    write_png(args.out + ".png", page, alpha)

    print("source:     %s" % source)
    print("charset:    %s-%s" % (header["CHARSET_REGISTRY"], header["CHARSET_ENCODING"]))
    print("requested:  %d code points (%s)" % (len(requested), args.chars))
    print("glyphs:     %d" % len(selected))
    print("page:       %dx%d" % (page, page))
    print("lineHeight: %d, base: %d" % (line_height, base))
    print("missing:    %d" % len(missing))
    for cp in missing:
        ch = chr(cp)
        print("  U+%04X %s" % (cp, ch if ch.isprintable() else ""))
    print("wrote %s.fnt and %s" % (args.out, os.path.join(out_dir, png_name)))


if __name__ == "__main__":
    main()
