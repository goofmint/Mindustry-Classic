# Task 2.2 verification (Issue #8)

- Date: 2026-11-08
- Branch: `issue-8-font` (stacked on `issue-7-locales`)
- Python 3 (standard library only), JDK 17 for `:html:dist`

## Distribution files (fetched 2026-11-08 from https://littlelimit.net/misaki.htm)
| file | bytes | SHA-256 |
|---|---|---|
| `tools/fonts/misaki/misaki_bdf_2021-05-05.zip` | 318046 | `a275f173cf5935890f84d3e65d05b1bf73028e4d4bf41cb3de0ef3b5ebe8e217` |
| `tools/fonts/misaki/misaki_ttf_2021-05-05.zip` | 794317 | `c1851af85a0a010dd84f40803159c2d8a23feecb6c7e800cfeb5c6edc50da46e` |

BDF zip contents: `misaki.txt` (manual + license), `readme.txt`, `misaki_gothic.bdf`, `misaki_gothic_2nd.bdf`, `misaki_mincho.bdf`.
License (misaki.txt): unlimited use/copy/distribute with or without modification, commercial or not, "AS IS". Copied as `tools/fonts/misaki/misaki.txt` (+ `readme_bdf.txt`, `readme_ttf.txt`).

## Source font
- `misaki_gothic_2nd.bdf` read directly from the zip (no fallback to `misaki_gothic.bdf` was needed)
- `FONT -Kadoma-MisakiGothic2nd-Regular-R-Normal--8-80-75-75-C-79-ISO10646-1`, `SIZE 8 75 75`, `FONTBOUNDINGBOX 8 8 0 -1`,
  `FONT_ASCENT 7`, `FONT_DESCENT 1`, `CHARS 7170`
- `CHARSET_REGISTRY "ISO10646"`, `CHARSET_ENCODING "1"` -> ENCODING is the Unicode code point; no JIS mapping.
  Check: 日 = ENCODING 26085 = U+65E5, 本 = U+672C, 語 = U+8A9E (glyph bitmaps below match the characters)

## Commands and output
```
$ python3 tools/collect_chars.py
wrote 445 code points to tools/chars_ja.txt

$ python3 tools/bdf2fnt.py --zip tools/fonts/misaki/misaki_bdf_2021-05-05.zip --chars tools/chars_ja.txt --out core/assets/ui/misaki
source:     tools/fonts/misaki/misaki_bdf_2021-05-05.zip!misaki_gothic_2nd.bdf
charset:    ISO10646-1
requested:  445 code points (tools/chars_ja.txt)
glyphs:     431
page:       256x256
lineHeight: 8, base: 7
missing:    14
  U+3094 ゔ  U+3095 ゕ  U+3096 ゖ  U+3097 (unassigned)  U+3098 (unassigned)
  U+3099 (combining dakuten)  U+309A (combining handakuten)  U+309F ゟ  U+30A0 ゠
  U+30F7 ヷ  U+30F8 ヸ  U+30F9 ヹ  U+30FA ヺ  U+30FF ヿ
wrote core/assets/ui/misaki.fnt and core/assets/ui/misaki.png

$ python3 tools/check_fnt.py --fnt core/assets/ui/misaki.fnt --chars tools/chars_ja.txt --sample "設計書レビュー済" --sample-out /tmp/misaki-sample.png
fnt:      core/assets/ui/misaki.fnt
page:     misaki.png 256x256
chars:    431 (list has 445)
alpha:    [0, 255]
rects:    all inside the page, no overlaps, no stray pixels
missing:  U+3094 U+3095 U+3096 U+3097 U+3098 U+3099 U+309A U+309F U+30A0 U+30F7 U+30F8 U+30F9 U+30FA U+30FF
sample: '設計書レビュー済' -> /tmp/misaki-sample.png (256x32)
```
All missing characters are rare/unassigned kana that come from the fixed Hiragana/Katakana ranges; none is used in the bundles or the vocabulary. 日本語設計書レビュー済 are all present (the converter fails otherwise).

`check_fnt.py` also confirms: header order as libGDX expects, `page id=0 file="misaki.png"`, ids unique/ascending and a subset of the list, PNG 8-bit RGBA 256x256, RGB white everywhere, every non-empty glyph has set pixels, no set pixel outside a glyph rect.

Determinism: running `collect_chars.py` and `bdf2fnt.py` twice gives byte-identical files.
```
eb61219f9447385a38f5768450e148461155a49f02ad1f0f410d4ddee80fb333  core/assets/ui/misaki.fnt  (37628 bytes)
3962e7358ea168ed4214b3dca43aaadb859a207d510988d18a07c1dc98d31423  core/assets/ui/misaki.png  (4244 bytes)
26727188cbc0ec6839359a1750827f7788d24223d1e71e9bf1880d8b3af65476  tools/chars_ja.txt
```

Glyphs read back from misaki.png:
```
日 (6x7)   本 (7x7)   語 (7x7)
######     ...#...    .#.###.
#....#     #######    ##..#..
#....#     ...#...    ..####.
######     ..###..    ##.#.#.
#....#     .#.#.#.    ..#####
#....#     #.###.#    ##.#.#.
######     ...#...    ##.###.
```
Sample `設計書レビュー済` composed with the fnt metrics (xoffset/yoffset/xadvance), scale 1 shown here (the /tmp PNG is x4, nearest neighbour):
```
.#.###...#...#.....#.....#.......#..#.#.................#...#...
##.#.#..###..#...#####...#.......#........................#####.
..#..##....####....#.##..#.......#..###.........#.......#..#.#..
##.###..##...#..######...#....#..###.....####....######.....#...
...#.#.......#.....#.....#...#...#..........#...........#.##.##.
##..#...###..#..#######..#.##....#..........#...........#..###..
####.##.###..#...#####...##.......#####.######..........#.#..#..
................................................................
```

## libGDX parser check (no window)
Throwaway Java file outside the repo, gdx 1.13.1 `new BitmapFontData(new FileHandle(file), false)`:
```
misaki.fnt: imagePaths=.../ui/misaki.png lineHeight=8.0 capHeight=7.0 ascent=0.0 descent=0.0 xHeight=5.0 spaceXadvance=4.0
  日=6x7@203,57 本=7x7@218,57 語=7x7@104,65 設=7x7@96,65 計=7x7@88,65 書=7x7@210,57 レ=6x7@15,57 ビ=6x7@77,49 ュ=6x4@218,49 ー=7x2@102,57 済=7x7@1,65 A=3x7@117,1 g=3x5@13,9
square.fnt: imagePaths=.../ui/square.png lineHeight=44.0 capHeight=20.0 ascent=16.0 descent=-8.0 xHeight=20.0 spaceXadvance=8.0
```
The file parses with libGDX's own loader. This does not exercise texture upload or drawing.

## Skin
`core/assets/ui/uiskin.json`, `BitmapFont` section: added `japanese: { file: misaki.fnt, markupEnabled: true, scale: 2 }` after `title`. Other entries unchanged.
ucore `Skin` loads `ui/misaki.png` next to the fnt (no atlas region named `misaki`); libGDX's default texture filter is Nearest.

## GWT preloader
```
$ ./gradlew :html:dist   -> exit 0 (only existing deprecation/unchecked notes)
html/build/dist/assets/assets.txt:
t:ui/misaki.fnt:ui/misaki-0b95a999c1db8af17bc8d0e9b5f148de.fnt:37628:application/unknown:1
i:ui/misaki.png:ui/misaki-111744cf7726448feb589051cbc45daa.png:4244:image/png:1
```
(`assets.txt` is at `html/build/dist/assets/assets.txt`.) The hashed copies are byte-identical to the sources, and the packaged `uiskin.json` contains `japanese`.

## Scale (metrics; the value was decided in the browser check below)
Metrics only, not a visual comparison:
- `square`: capHeight 20, lineHeight 44. `UI.loadSkin()` overrides its skin scale with `Vars.fontscale` (= max(dp(1)/2, 0.5); 0.5 on a 1x display) -> cap 10 px, line 22 px before the `down`/`lineHeight` adjustments
- `japanese`: capHeight 7 (Gothic 2nd half-width glyphs use 7 px), kanji 7x7 in an 8x8 cell, lineHeight 8. Not in the `loadSkin()` list, so `scale: 2` stays absolute -> cap 14 px, line 16 px; scale 1 -> 7/8 px
- Integer scales cannot match square exactly (ratio 10/7 at fontscale 0.5). On a 2x display (fontscale 1.0) square becomes cap 20, so a fixed `scale: 2` would look small there. Task 2.3 (font switching in `loadSkin()`) is where the scale should be tied to the display. Resolved: `scale: 2` was adopted after the browser comparison recorded below.

## PENDING (orchestrator / user)
- Resolved (see the browser check below): rendering at original dots without blur was confirmed in the browser with the menu and language-dialog strings; `scale: 2` is the final value.
- NOT verified in this task: the exact strings 「設計書」「レビュー済」 (they are not in the UI until Task 2.4/2.5 add the Japanese text), and the desktop build (no display in this environment; desktop `./gradlew :desktop:run` is the owner's check). See 未確認 below.
- Regenerate after Task 2.4/2.5 add Japanese text (procedure in `tools/README.md`)


## オーケストレーターによるブラウザ描画確認(2026-11-08 10:00Z ごろ)

- 方法: このワークツリーで一時的に(コミットせず)`uiskin.json` の `default-font` を `misaki.fnt`(scale 2)に差し替え、`UI.loadSkin()` の `Vars.fontscale` 上書き対象から `default-font` を外して `./gradlew :html:dist` を作り、Playwright + Chromium(ヘッドレス)でメニューと Settings → Language を表示した。確認後に 2 ファイルは `git checkout --` で元に戻した。
- 結果: 「日本語 (Japanese)」「English」「Game」「Graphics」「Controls」などがすべて美咲フォントの原寸ドットで描かれ、にじみや欠けは無い。英字(半角 3×7 ドット幅)も読める。
- 大きさ: scale 2 で大文字の高さは約 14px(キャンバス座標)。元の `square.fnt`(`Vars.fontscale` = 0.5 で約 10px)よりやや大きいが、ボタンやダイアログの中に収まり、レイアウトの崩れは見られない。scale 1 は 7px で小さすぎる。非整数はにじむため不可。したがって候補値 2 を採用する。画面倍率(`Vars.fontscale`)との連動は Task 2.3 で `japanese` を切り替え対象に加えるときに扱う。
- 未確認: ゲーム内 HUD での日本語(Task 2.4 以降で文言が入ってから)。完了条件の文字列「設計書」「レビュー済」そのものの描画(この時点の UI に無い。Task 2.4/2.5 で文言が入ってから確認する)。デスクトップ版での描画(この環境では画面が無く起動できない。所有者の確認事項)。
