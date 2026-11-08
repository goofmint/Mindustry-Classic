#!/usr/bin/env python3
# Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
"""Generate the loading-screen logo of the web build.

Writes html/webapp/html/logo.png (610x124 RGBA, transparent background).
HtmlLauncher shows it above the loading meter while GWT loads the assets.

The title is the value of text.gamename in core/assets/bundles/bundle.properties,
so the logo follows the product name. It is drawn with the 5x7 block glyphs
below (one glyph cell = SCALE x SCALE pixels): white letters with an orange
shadow, centered with a transparent margin. Only A-Z, 0-9, space and '-' are
supported; any other character, a missing key or a title that does not fit
stops the script with an error. The output is deterministic.

Requires Pillow. Usage (from the repository root):
    python3 -I tools/gen_logo.py
"""

import sys

from PIL import Image

BUNDLE = "core/assets/bundles/bundle.properties"
KEY = "text.gamename"
OUT = "html/webapp/html/logo.png"

WIDTH = 610
HEIGHT = 124
MIN_MARGIN = 12
SCALE = 12  # largest scale; reduced until the title fits
SHADOW = 0.5  # shadow offset in glyph cells
WHITE = (255, 255, 255, 255)
ORANGE = (255, 87, 10, 255)  # #ff570a, same as styles.css and Links.java

GLYPHS = {
    "A": ["01110", "10001", "10001", "11111", "10001", "10001", "10001"],
    "B": ["11110", "10001", "10001", "11110", "10001", "10001", "11110"],
    "C": ["01110", "10001", "10000", "10000", "10000", "10001", "01110"],
    "D": ["11110", "10001", "10001", "10001", "10001", "10001", "11110"],
    "E": ["11111", "10000", "10000", "11110", "10000", "10000", "11111"],
    "F": ["11111", "10000", "10000", "11110", "10000", "10000", "10000"],
    "G": ["01110", "10001", "10000", "10111", "10001", "10001", "01111"],
    "H": ["10001", "10001", "10001", "11111", "10001", "10001", "10001"],
    "I": ["11111", "00100", "00100", "00100", "00100", "00100", "11111"],
    "J": ["00111", "00010", "00010", "00010", "00010", "10010", "01100"],
    "K": ["10001", "10010", "10100", "11000", "10100", "10010", "10001"],
    "L": ["10000", "10000", "10000", "10000", "10000", "10000", "11111"],
    "M": ["10001", "11011", "10101", "10101", "10001", "10001", "10001"],
    "N": ["10001", "11001", "10101", "10011", "10001", "10001", "10001"],
    "O": ["01110", "10001", "10001", "10001", "10001", "10001", "01110"],
    "P": ["11110", "10001", "10001", "11110", "10000", "10000", "10000"],
    "Q": ["01110", "10001", "10001", "10001", "10101", "10010", "01101"],
    "R": ["11110", "10001", "10001", "11110", "10100", "10010", "10001"],
    "S": ["01111", "10000", "10000", "01110", "00001", "00001", "11110"],
    "T": ["11111", "00100", "00100", "00100", "00100", "00100", "00100"],
    "U": ["10001", "10001", "10001", "10001", "10001", "10001", "01110"],
    "V": ["10001", "10001", "10001", "10001", "10001", "01010", "00100"],
    "W": ["10001", "10001", "10001", "10101", "10101", "10101", "01010"],
    "X": ["10001", "10001", "01010", "00100", "01010", "10001", "10001"],
    "Y": ["10001", "10001", "01010", "00100", "00100", "00100", "00100"],
    "Z": ["11111", "00001", "00010", "00100", "01000", "10000", "11111"],
    "0": ["01110", "10001", "10011", "10101", "11001", "10001", "01110"],
    "1": ["00100", "01100", "00100", "00100", "00100", "00100", "01110"],
    "2": ["01110", "10001", "00001", "00010", "00100", "01000", "11111"],
    "3": ["11110", "00001", "00001", "01110", "00001", "00001", "11110"],
    "4": ["00010", "00110", "01010", "10010", "11111", "00010", "00010"],
    "5": ["11111", "10000", "11110", "00001", "00001", "10001", "01110"],
    "6": ["00110", "01000", "10000", "11110", "10001", "10001", "01110"],
    "7": ["11111", "00001", "00010", "00100", "01000", "01000", "01000"],
    "8": ["01110", "10001", "10001", "01110", "10001", "10001", "01110"],
    "9": ["01110", "10001", "10001", "01111", "00001", "00010", "01100"],
    "-": ["00000", "00000", "00000", "11111", "00000", "00000", "00000"],
    " ": ["000", "000", "000", "000", "000", "000", "000"],
}
GLYPH_HEIGHT = 7
SPACING = 1  # empty columns between glyphs


def read_title():
    """Return the value of KEY from BUNDLE (plain one-line value, no escapes)."""
    with open(BUNDLE, encoding="utf-8") as f:
        for line in f:
            if line.startswith(KEY + "="):
                value = line[len(KEY) + 1:].rstrip("\r\n")
                if value == "" or "\\" in value:
                    sys.exit("%s: %s must be a non-empty plain value" % (BUNDLE, KEY))
                return value
    sys.exit("%s: key %s not found" % (BUNDLE, KEY))


def cells(title):
    """Return the set of filled (x, y) glyph cells and the size in cells."""
    filled = set()
    x = 0
    for i, ch in enumerate(title.upper()):
        if ch not in GLYPHS:
            sys.exit("unsupported character %r in %r" % (ch, title))
        rows = GLYPHS[ch]
        if i > 0:
            x += SPACING
        for y, row in enumerate(rows):
            for dx, bit in enumerate(row):
                if bit == "1":
                    filled.add((x + dx, y))
        x += len(rows[0])
    return filled, x, GLYPH_HEIGHT


def main():
    title = read_title()
    filled, cw, ch = cells(title)
    scale = SCALE
    while scale > 0:
        w = cw * scale + int(SHADOW * scale)
        h = ch * scale + int(SHADOW * scale)
        if w <= WIDTH - 2 * MIN_MARGIN and h <= HEIGHT - 2 * MIN_MARGIN:
            break
        scale -= 1
    if scale == 0:
        sys.exit("title %r does not fit in %dx%d" % (title, WIDTH, HEIGHT))
    shadow = int(SHADOW * scale)
    ox = (WIDTH - w) // 2
    oy = (HEIGHT - h) // 2

    image = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    for color, off in ((ORANGE, shadow), (WHITE, 0)):
        for (x, y) in sorted(filled):
            x0 = ox + x * scale + off
            y0 = oy + y * scale + off
            image.paste(color, (x0, y0, x0 + scale, y0 + scale))
    image.save(OUT, optimize=False)
    print("wrote %s (%dx%d, title %r, cell %d px, margin %d/%d px)" % (OUT, WIDTH, HEIGHT, title, scale, ox, oy))


if __name__ == "__main__":
    main()
