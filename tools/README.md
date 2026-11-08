# tools

ゲーム本体とは別に、アセットを作るための補助スクリプトと素材を置く。

## 美咲フォント(日本語フォント)

### 配布物

8×8 ドットの日本語ビットマップフォント「美咲フォント」2021-05-05 版を、配布元 <https://littlelimit.net/misaki.htm> から取得して `tools/fonts/misaki/` に置いている(2026-11-08 取得)。

| ファイル | URL | サイズ (byte) | SHA-256 |
|---|---|---|---|
| `misaki_bdf_2021-05-05.zip` | https://littlelimit.net/arc/misaki/misaki_bdf_2021-05-05.zip | 318046 | `a275f173cf5935890f84d3e65d05b1bf73028e4d4bf41cb3de0ef3b5ebe8e217` |
| `misaki_ttf_2021-05-05.zip` | https://littlelimit.net/arc/misaki/misaki_ttf_2021-05-05.zip | 794317 | `c1851af85a0a010dd84f40803159c2d8a23feecb6c7e800cfeb5c6edc50da46e` |

同じディレクトリに、zip に同梱の文書を展開して置いている。

- `misaki.txt`: 美咲フォントのマニュアル。ライセンスはこの中の「ライセンス」節(両 zip で同一のファイル)
- `readme_bdf.txt`: BDF 版 zip の `readme.txt`
- `readme_ttf.txt`: TTF 版 zip の `readme.txt`

### ライセンス

`misaki.txt` より。利用・複製・再配布・改変が無制限で、表記義務はない。無保証。

> These fonts are free softwares.
> Unlimited permission is granted to use, copy, and distribute it, with or without modification, either commercially and noncommercially.
> THESE FONTS ARE PROVIDED "AS IS" WITHOUT WARRANTY.

### BDF から AngelCode 形式への変換

libGDX の `BitmapFont` は AngelCode BMFont 形式(テキストの `.fnt` + ページ画像 `.png`)しか読まない。GWT 版では FreeType も使えない。そこで BDF 版の `misaki_gothic_2nd.bdf`(美咲ゴシック第2)を、使う文字だけに絞って変換する。Hiero(gdx-tools)は GUI ツールで自動実行できないので使っていない。

BDF は `CHARSET_REGISTRY "ISO10646"` / `CHARSET_ENCODING "1"` なので、各グリフの `ENCODING` がそのまま Unicode のコードポイントになる。JIS からの変換は要らない。ほかの文字コードの BDF を渡すと `bdf2fnt.py` はエラーで止まる。

スクリプトはすべて Python 3 の標準ライブラリだけで動く。リポジトリのルートで次の順に実行する。

```sh
# 1. 収録する文字を集める -> tools/chars_ja.txt
python3 tools/collect_chars.py

# 2. 変換する -> core/assets/ui/misaki.fnt, core/assets/ui/misaki.png
python3 tools/bdf2fnt.py --zip tools/fonts/misaki/misaki_bdf_2021-05-05.zip \
    --chars tools/chars_ja.txt --out core/assets/ui/misaki

# 3. 出力を読み戻して検査する(見本画像は任意。リポジトリの外に書く)
python3 tools/check_fnt.py --fnt core/assets/ui/misaki.fnt --chars tools/chars_ja.txt \
    --sample "設計書レビュー済" --sample-out /tmp/misaki-sample.png
```

#### `collect_chars.py`

次の文字を集め、重複を除いてコードポイント順に `tools/chars_ja.txt` へ書く。

- `core/assets/bundles/bundle_ja.properties` と `bundle.properties` の値(キーは表示しないので集めない)。`.properties` の書式(`key=value` / `key:value`、`\uXXXX` などのエスケープ、行末 `\` の継続行、`#` / `!` のコメント)に従って読む。`[accent]` のようなマークアップも表示される文字として残し、制御文字は捨てる。書式の崩れた行は行番号付きで警告する。`bundle_ja.properties` が無いときは警告だけ出して続ける
- スクリプト内の定数 `EXTRA_STRINGS`(確認用の「日本語」「設計書」「レビュー済」と、設計書のゲーム用語)と `EXTRA_SYMBOLS`(`・ー※→←↑↓〜…`)
- 常に入れる範囲: ASCII 印字文字 U+0020–U+007E、ひらがな U+3041–U+309F、カタカナ U+30A0–U+30FF、全角英数記号 U+FF01–U+FF5E
- CJK 記号 U+3000–U+303F のうち、BDF に字形があるもの

半角カタカナ(U+FF61–U+FF9F)と BMP 外の文字は警告を出して捨てる。

`chars_ja.txt` の書式は UTF-8(BOM なし)、改行 LF、1 行 1 コードポイントで `U+XXXX<空白><文字>`。読むのは先頭の `U+XXXX` だけで、後ろの文字は目で確認するためのもの(空白文字 U+0020 を 1 文字だけの行にすると見えず、行末の空白削除で壊れるため)。

#### `bdf2fnt.py`

- zip から BDF を直接読む(`--zip`、zip 内の名前は `--bdf-name`、既定は `misaki_gothic_2nd.bdf`)。展開済みのファイルを使うときは `--bdf-path`
- `chars_ja.txt` の文字のうち BDF にあるものだけを、コードポイント順に 1 枚の正方形ページへ詰める。ページは 256×256 から始め、入らなければ 512、1024 と広げ、1024 で入らなければエラー。グリフの間とページの縁は 1 px 空ける。拡大・補間はしない
- PNG は 8 ビット RGBA。RGB はすべて白、アルファは 0 か 255 だけ
- `.fnt` の `lineHeight` は `FONT_ASCENT + FONT_DESCENT`、`base` は `FONT_ASCENT`。各文字の `width` / `height` / `xoffset` は BDF の `BBX`、`yoffset` は `base - (BBX の y + 高さ)`、`xadvance` は `DWIDTH`。空白のように点の無い文字は幅と高さを 0 にして `xadvance` だけ残す
- BDF に無い文字は一覧を表示する(エラーにはしない)。「日本語設計書レビュー済」の 1 字でも無ければエラーで止まる
- 行の項目順は libGDX の `BitmapFontData.load()` が前提にしている順(`info` 行の `padding=` の後に空白、`common` 行は `lineHeight` / `base` / … / `pages` の位置固定、`char` 行は `id x y width height xoffset yoffset xadvance page`)に合わせている

#### 出力と使い方

- `core/assets/ui/misaki.fnt` と `core/assets/ui/misaki.png`。ucore の `Skin` は `.fnt` と同じ名前の `.png` を探すので、名前をそろえる
- `core/assets/ui/uiskin.json` の `BitmapFont` に `japanese` として登録している(`scale: 2`)
- GWT 版では `./gradlew :html:dist` で `html/build/dist/assets/assets.txt` に両ファイルが載る

### 再変換が要るとき

日本語の文言を増やしたとき(Task 2.4 の `bundle_ja.properties` の翻訳、Task 2.5 の名称追加など)は、上の 1〜3 を実行し、`tools/chars_ja.txt`、`core/assets/ui/misaki.fnt`、`core/assets/ui/misaki.png` をまとめてコミットする。`bdf2fnt.py` の出力に「missing」として出た文字は美咲フォントに無い文字なので、文言のほうを言い換える。同じ入力からは同じ出力になる(バイト単位で一致する)。
