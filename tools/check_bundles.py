#!/usr/bin/env python3
# Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
"""Check that the Japanese bundle covers the English bundle.

Compares core/assets/bundles/bundle.properties (English) with
core/assets/bundles/bundle_ja.properties (Japanese), after removing the keys
listed in tools/bundle_exclude_keys.txt from the English side.

Problems reported (exit code 1 if any, 0 when clean):
  - keys in English (minus exclusions) that are missing in Japanese
  - keys in Japanese that are not in English (minus exclusions)
  - exclusion entries that do not exist in English
  - duplicate exclusion entries
  - duplicate keys in either bundle. libGDX keeps the last value at runtime,
    so this check is stricter than the game on purpose: a duplicate is almost
    always an editing mistake.
  - Japanese key order differing from the English order
  - per key, the set of {n} format arguments differing between the languages

The .properties files are read the way libGDX PropertiesUtils reads them:
comment lines start with '#' or '!', the key ends at the first unescaped '=',
':' or whitespace, backslash escapes (\\t \\n \\r \\f \\uXXXX, any other char is
taken literally) and line continuation with a trailing backslash. Files must
be valid UTF-8; a missing file is an error.

Only the Python 3 standard library is used.

Usage (from the repository root):
    python3 tools/check_bundles.py
"""

import argparse
import re
import sys

DEFAULT_EN = "core/assets/bundles/bundle.properties"
DEFAULT_JA = "core/assets/bundles/bundle_ja.properties"
DEFAULT_EXCLUDE = "tools/bundle_exclude_keys.txt"

WHITESPACE = " \t\f"
SEPARATORS = "=:" + WHITESPACE
SIMPLE_ESCAPES = {"t": "\t", "n": "\n", "r": "\r", "f": "\f"}
ARG_PATTERN = re.compile(r"\{(\d+)[^}]*\}")


class CheckError(Exception):
    pass


def read_text(path):
    try:
        with open(path, "rb") as f:
            data = f.read()
    except FileNotFoundError:
        raise CheckError("%s: file not found" % path)
    try:
        text = data.decode("utf-8", errors="strict")
    except UnicodeDecodeError as e:
        raise CheckError("%s: invalid UTF-8 (%s)" % (path, e))
    if text.startswith("﻿"):
        text = text[1:]
    return text


def logical_lines(text):
    """Yield (line_number, logical_line), joining continuation lines."""
    lines = text.split("\n")
    i = 0
    while i < len(lines):
        start = i + 1
        line = lines[i].rstrip("\r")
        i += 1
        stripped = line.lstrip(WHITESPACE)
        if stripped == "" or stripped[0] in "#!":
            continue
        parts = [stripped]
        while _ends_with_continuation(parts[-1]):
            parts[-1] = parts[-1][:-1]
            if i >= len(lines):
                break
            parts.append(lines[i].rstrip("\r").lstrip(WHITESPACE))
            i += 1
        yield start, "".join(parts)


def _ends_with_continuation(line):
    count = len(line) - len(line.rstrip("\\"))
    return count % 2 == 1


def unescape(raw, path, line_no, problems):
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
            i += 1
            continue
        e = raw[i + 1]
        if e == "u":
            digits = raw[i + 2:i + 6]
            if re.fullmatch(r"[0-9A-Fa-f]{4}", digits):
                out.append(chr(int(digits, 16)))
                i += 6
            else:
                problems.append("%s:%d: malformed \\u escape" % (path, line_no))
                i += 2
            continue
        out.append(SIMPLE_ESCAPES.get(e, e))
        i += 2
    return "".join(out)


def parse_properties(path, problems):
    """Return a list of (key, value, line_number) in file order."""
    text = read_text(path)
    entries = []
    for line_no, line in logical_lines(text):
        i = 0
        n = len(line)
        while i < n:
            c = line[i]
            if c == "\\":
                i += 2
                continue
            if c in SEPARATORS:
                break
            i += 1
        raw_key = line[:i]
        j = i
        while j < n and line[j] in WHITESPACE:
            j += 1
        if j < n and line[j] in "=:":
            j += 1
            while j < n and line[j] in WHITESPACE:
                j += 1
        key = unescape(raw_key, path, line_no, problems)
        value = unescape(line[j:], path, line_no, problems)
        if key == "":
            problems.append("%s:%d: line has no key" % (path, line_no))
            continue
        entries.append((key, value, line_no))
    return entries


def read_exclusions(path):
    """Return a list of (key, line_number). '#' starts a comment line."""
    text = read_text(path)
    result = []
    for line_no, line in enumerate(text.split("\n"), 1):
        line = line.strip()
        if line == "" or line.startswith("#"):
            continue
        result.append((line, line_no))
    return result


def duplicates(entries, path, problems):
    seen = {}
    for key, _value, line_no in entries:
        if key in seen:
            problems.append("%s:%d: duplicate key %s (first at line %d)" % (path, line_no, key, seen[key]))
        else:
            seen[key] = line_no


def arguments(value):
    return set(int(m.group(1)) for m in ARG_PATTERN.finditer(value))


def check(en_path, ja_path, exclude_path):
    problems = []
    en = parse_properties(en_path, problems)
    ja = parse_properties(ja_path, problems)
    excl = read_exclusions(exclude_path)

    duplicates(en, en_path, problems)
    duplicates(ja, ja_path, problems)

    en_keys = [k for k, _, _ in en]
    en_set = set(en_keys)
    excl_seen = {}
    for key, line_no in excl:
        if key in excl_seen:
            problems.append("%s:%d: duplicate exclusion %s (first at line %d)" % (exclude_path, line_no, key, excl_seen[key]))
            continue
        excl_seen[key] = line_no
        if key not in en_set:
            problems.append("%s:%d: excluded key %s does not exist in %s" % (exclude_path, line_no, key, en_path))
    excl_set = set(excl_seen)

    expected = []
    for key in en_keys:
        if key not in excl_set and key not in expected:
            expected.append(key)
    expected_set = set(expected)
    ja_keys = []
    for key, _, _ in ja:
        if key not in ja_keys:
            ja_keys.append(key)
    ja_set = set(ja_keys)

    for key in expected:
        if key not in ja_set:
            problems.append("missing in %s: %s" % (ja_path, key))
    ja_lines = dict((k, l) for k, _, l in ja)
    for key in ja_keys:
        if key not in expected_set:
            reason = "excluded" if key in excl_set else "not in %s" % en_path
            problems.append("%s:%d: extra key %s (%s)" % (ja_path, ja_lines[key], key, reason))

    common_en = [k for k in expected if k in ja_set]
    common_ja = [k for k in ja_keys if k in expected_set]
    if common_en != common_ja:
        for index, (a, b) in enumerate(zip(common_en, common_ja)):
            if a != b:
                problems.append("%s:%d: key order differs from %s: expected %s, found %s"
                                % (ja_path, ja_lines[b], en_path, a, b))
                break

    en_values = dict((k, v) for k, v, _ in en)
    for key, value, line_no in ja:
        if key in expected_set:
            en_args = arguments(en_values[key])
            ja_args = arguments(value)
            if en_args != ja_args:
                problems.append("%s:%d: %s format arguments differ: en %s, ja %s"
                                % (ja_path, line_no, key, sorted(en_args), sorted(ja_args)))

    summary = ("en keys: %d, excluded: %d, expected in ja: %d, ja keys: %d"
               % (len(set(en_keys)), len(excl_set), len(expected), len(ja_set)))
    return problems, summary


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("--en", default=DEFAULT_EN)
    parser.add_argument("--ja", default=DEFAULT_JA)
    parser.add_argument("--exclude", default=DEFAULT_EXCLUDE)
    args = parser.parse_args()
    try:
        problems, summary = check(args.en, args.ja, args.exclude)
    except CheckError as e:
        print("error: %s" % e, file=sys.stderr)
        return 1
    print(summary)
    if problems:
        for p in problems:
            print("error: %s" % p)
        print("%d problem(s) found" % len(problems))
        return 1
    print("OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
