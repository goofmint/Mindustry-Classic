#!/usr/bin/env python3
"""Collect the characters the Japanese bitmap font must contain.

Reads the UI bundles (.properties), adds fixed ranges and fixed strings, and
writes the sorted list of unique code points to tools/chars_ja.txt.

Output format (UTF-8 without BOM, LF line endings, ascending code point order):
    U+XXXX<space><character>
one code point per line. Only the leading "U+XXXX" token is meaningful; the
character after it is there for human review. (A bare character per line would
make U+0020 SPACE invisible and fragile against whitespace trimming.)

Only the Python 3 standard library is used. The result is deterministic for the
same inputs.

Usage (from the repository root):
    python3 tools/collect_chars.py
"""

import argparse
import re
import sys
import unicodedata
import zipfile

DEFAULT_JA_BUNDLE = "core/assets/bundles/bundle_ja.properties"
DEFAULT_EN_BUNDLE = "core/assets/bundles/bundle.properties"
DEFAULT_BDF_ZIP = "tools/fonts/misaki/misaki_bdf_2021-05-05.zip"
DEFAULT_BDF_NAME = "misaki_gothic_2nd.bdf"
DEFAULT_OUT = "tools/chars_ja.txt"

# Strings that must render even before the bundle contains them
# (Task 2.2 check strings and the game vocabulary from .tmp/design.md).
EXTRA_STRINGS = (
    "日本語",
    "設計書",
    "レビュー済",
    "コード",
    "品質",
    "重要度",
    "レビュー",
    "テスト",
    "納品",
    "残高",
    "目標",
    "敵",
    "ウェーブ",
    "パイプライン",
    "トークン",
    "発電機",
    "砲台",
    "人間のエンジニア",
    "シニアエンジニア",
    "ユニットテスト",
    "エンドツーエンド",
    "継続的インテグレーション",
    "本番環境",
    "勝利",
    "敗北",
)

# Symbols that are always added.
EXTRA_SYMBOLS = "・ー※→←↑↓〜…"

# Ranges that are always added, inclusive.
ALWAYS_RANGES = (
    (0x0020, 0x007E),  # ASCII printable
    (0x3041, 0x309F),  # Hiragana
    (0x30A0, 0x30FF),  # Katakana
    (0xFF01, 0xFF5E),  # Fullwidth ASCII variants
)

# Added only for the code points that the BDF actually contains.
BDF_FILTERED_RANGES = (
    (0x3000, 0x303F),  # CJK symbols and punctuation
)

# Half-width katakana are never collected.
HALFWIDTH_KATAKANA = (0xFF61, 0xFF9F)

SIMPLE_ESCAPES = {"t": "\t", "n": "\n", "r": "\r", "f": "\f"}


def warn(message):
    print("warning: " + message, file=sys.stderr)


def logical_lines(text):
    """Yield (line_number, logical_line) following java.util.Properties rules.

    A line ending in an odd number of backslashes continues on the next line;
    the leading whitespace of a continuation line is dropped. Comment lines
    (first non-blank character '#' or '!') and blank lines are skipped and
    never continue.
    """
    lines = text.split("\n")
    i = 0
    while i < len(lines):
        start = i + 1
        line = lines[i].rstrip("\r")
        i += 1
        stripped = line.lstrip(" \t\f")
        if stripped == "" or stripped[0] in "#!":
            continue
        parts = [stripped]
        while _ends_with_continuation(parts[-1]):
            parts[-1] = parts[-1][:-1]
            if i >= len(lines):
                warn("line %d: continuation backslash at end of file" % start)
                break
            parts.append(lines[i].rstrip("\r").lstrip(" \t\f"))
            i += 1
        yield start, "".join(parts)


def _ends_with_continuation(line):
    count = len(line) - len(line.rstrip("\\"))
    return count % 2 == 1


def split_key_value(line):
    """Return (raw_key, raw_value, has_separator)."""
    i = 0
    n = len(line)
    while i < n:
        c = line[i]
        if c == "\\":
            i += 2
            continue
        if c in "=: \t\f":
            break
        i += 1
    key = line[:i]
    j = i
    while j < n and line[j] in " \t\f":
        j += 1
    has_sep = False
    if j < n and line[j] in "=:":
        has_sep = True
        j += 1
        while j < n and line[j] in " \t\f":
            j += 1
    elif j > i:
        has_sep = True  # whitespace separator is valid in .properties
    return key, line[j:], has_sep


def unescape(raw, line_no, path):
    out = []
    i = 0
    n = len(raw)
    while i < n:
        c = raw[i]
        if c != "\\":
            out.append(c)
            i += 1
            continue
        if i + 1 >= n:
            warn("%s:%d: dangling backslash" % (path, line_no))
            i += 1
            continue
        e = raw[i + 1]
        if e == "u":
            hexdigits = raw[i + 2:i + 6]
            if len(hexdigits) == 4 and re.fullmatch(r"[0-9A-Fa-f]{4}", hexdigits):
                out.append(chr(int(hexdigits, 16)))
                i += 6
            else:
                warn("%s:%d: malformed \\u escape: %r" % (path, line_no, raw[i:i + 6]))
                i += 2
            continue
        out.append(SIMPLE_ESCAPES.get(e, e))
        i += 2
    return "".join(out)


def bundle_values(path, required):
    """Return the list of unescaped values in a .properties file."""
    try:
        with open(path, "r", encoding="utf-8") as f:
            text = f.read()
    except FileNotFoundError:
        if required:
            raise
        warn("%s not found; collecting without it" % path)
        return []
    if text.startswith("﻿"):
        text = text[1:]
    values = []
    for line_no, line in logical_lines(text):
        raw_key, raw_value, has_sep = split_key_value(line)
        if raw_key == "":
            warn("%s:%d: line has no key: %r" % (path, line_no, line))
            continue
        if not has_sep:
            warn("%s:%d: line has no '=' or ':' separator: %r" % (path, line_no, line))
            continue
        values.append(unescape(raw_value, line_no, path))
    return values


def bdf_encodings(zip_path, bdf_name):
    """Return the set of ENCODING values in a BDF stored in a zip."""
    with zipfile.ZipFile(zip_path) as z:
        data = z.read(bdf_name).decode("ascii")
    codes = set()
    for line in data.split("\n"):
        if line.startswith("ENCODING "):
            codes.add(int(line.split()[1]))
    return codes


def collect(ja_bundle, en_bundle, bdf_zip, bdf_name):
    chars = set()

    def add_text(text, origin):
        for ch in text:
            cp = ord(ch)
            if unicodedata.category(ch) == "Cc":
                continue
            if cp > 0xFFFF:
                warn("%s: skipping non-BMP character U+%X" % (origin, cp))
                continue
            if HALFWIDTH_KATAKANA[0] <= cp <= HALFWIDTH_KATAKANA[1]:
                warn("%s: skipping half-width katakana U+%04X" % (origin, cp))
                continue
            chars.add(cp)

    for value in bundle_values(ja_bundle, required=False):
        add_text(value, ja_bundle)
    for value in bundle_values(en_bundle, required=True):
        add_text(value, en_bundle)
    for s in EXTRA_STRINGS:
        add_text(s, "EXTRA_STRINGS")
    add_text(EXTRA_SYMBOLS, "EXTRA_SYMBOLS")
    for lo, hi in ALWAYS_RANGES:
        chars.update(range(lo, hi + 1))
    present = bdf_encodings(bdf_zip, bdf_name)
    for lo, hi in BDF_FILTERED_RANGES:
        chars.update(cp for cp in range(lo, hi + 1) if cp in present)
    return sorted(chars)


def write_list(codes, out_path):
    with open(out_path, "w", encoding="utf-8", newline="\n") as f:
        for cp in codes:
            f.write("U+%04X %s\n" % (cp, chr(cp)))


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--ja", default=DEFAULT_JA_BUNDLE, help="Japanese bundle (warned if missing)")
    ap.add_argument("--en", default=DEFAULT_EN_BUNDLE, help="base bundle (required)")
    ap.add_argument("--zip", default=DEFAULT_BDF_ZIP, help="Misaki BDF zip, used to filter U+3000-U+303F")
    ap.add_argument("--bdf", default=DEFAULT_BDF_NAME, help="BDF file name inside the zip")
    ap.add_argument("--out", default=DEFAULT_OUT, help="output list")
    args = ap.parse_args()

    codes = collect(args.ja, args.en, args.zip, args.bdf)
    write_list(codes, args.out)
    print("wrote %d code points to %s" % (len(codes), args.out))


if __name__ == "__main__":
    main()
