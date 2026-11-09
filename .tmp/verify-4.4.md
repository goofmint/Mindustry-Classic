# Task 4.4 検証記録: タイトル、About、ブランディング(Issue #26)

## 変更内容

### タイトル画面のメニュー(`f9a5020`)
- `MenuFragment` の非モバイルのレイアウトから Editor(GWT の非対応表示の分岐ごと)と Donate を外した。残りは 1 行目 Play(幅 410、2 列分)、2 行目 Settings / About(幅 200 ずつ)、3 行目 Quit(GWT 以外のみ、幅 410)。2 行目は 2 個で 2 列を埋めるので、Play と左右がそろう。
  - 依頼文では 1 行目を Play / Load / Join としていたが、実際の非モバイルのメニューは Play 1 個で、Load / Join は Play を押した後の選択ダイアログ(`showPlaySelect()`)にある。そのため行の構成は上のとおりで、選択ダイアログは変えていない。
- モバイルのレイアウトは 1 行目 Play / Load / Join を変えず、2 行目の内側の表から Editor と Donate(iOS 除外の分岐ごと)を外して Settings / About の 2 個にした(`colspan(3)` の中央寄せのまま)。
- 右上の Discord バナーと `Platform.instance.hasDiscord()` の分岐を削除した。`DiscordDialog`、`Platform` の API、`MapEditorDialog`、`Vars.discordURL` は変えていない。
- 左下のバージョン表示の固定文字列 `Mindustry` を `Bundles.get("text.gamename")` にした。表示形式(`<名前> <code> / <build>`)は同じ。
- `text.editor` と `text.donate` はコードから参照されなくなったが、本タスクの範囲外として bundle に残した(両言語にあり、`check_bundles.py` は通る)。

### About とクレジット(`dc1a54e`)
- `Links`: 7 件(discord、trello、wiki、itch.io、google-play、github、dev-builds)を消し、`coderabbit` / `https://coderabbit.ai` / `ff570a` の 1 件にした。`LinkEntry` に `icon`(アトラスの領域名)と `title`(`text.link.<name>.name`)を足した。アイコンは既存の `icon-check`(uiskin.atlas)。アトラスは作り直していない。以前は `icon-<name>` と `name` をそのまま見出しにしていたため、`coderabbit` という小文字表記とアトラスに無いアイコン名になるのを避けた。
- `AboutDialog`: iOS で Google Play を飛ばす分岐を削除。リンク行の描画、`openURI` 失敗時のエラー表示と URL のコピーは変えていない。
- クレジットのダイアログ: `text.about` 1 個を出していたのを、スクロールペイン(`"clear"`)に幅 600 の折り返しラベルを 4 個並べる形にした。順に `text.credits.coderabbit` / `text.about`(本家クレジット) / `text.credits.license` / `text.credits.font`。各値の先頭に `[accent]` の見出しがある。About 本体と同じく、表示時にスクロールのフォーカスを渡す。ラベルは既定のスケールのまま(`setFontScale` を使っていないので `UI.labelScale()` は不要)。
- 文言の要点:
  - CodeRabbit: プルリクエストのコードを AI でレビューするツール。ゲーム内の CodeRabbit ブロックは通ったコードの品質を 2 段階上げ、本番環境をおそうバグを撃ち落とす。
  - 本家: Anuken の Mindustry Classic を改変したもの、改変は 2026 年。本家のクレジット(Anuken、GDL Metal Monstrosity Jam、bfxr、a drop a day、MitchellFJN、Luxray5474、Epowerj、ベータテスター)は残し、メールアドレスと「YouTube で聴けます」は外した。
  - ライセンス: 本家と同じ GPL-3.0 のフリーソフトウェア、無保証。ソースコード `https://github.com/goofmint/Mindustry-Classic` はクリックできないテキスト。
  - フォント: 門真なむ (Num Kadoma) さんの美咲フォント(美咲ゴシック第2、2021-05-05 版)、配布元 littlelimit.net。版と作者名は `tools/fonts/misaki/misaki.txt` の表記に合わせた。
- 新しいキー(両言語): `text.gamename`、`text.credits.coderabbit`、`text.credits.license`、`text.credits.font`、`text.link.coderabbit.name`、`text.link.coderabbit.description`。書き直し: `text.about`。削除: `text.link.{discord,github,dev-builds,trello,itch.io,google-play,wiki}.description`。

### Web 版の HTML シェル(`fd1c422`)
- `html/webapp/index.html` の `<title>` を `AI SDLC` にした。外部リンクは足していない。
- `html/webapp/styles.css`: 背景 `#191919` はそのまま。ロードメーターの `background-color` を `#ff570a`、グラデーションを `#ff7a3d` → `#ff570a` にした。
- `html/webapp/html/logo.png`: `tools/gen_logo.py` で作った 610×124 RGBA。`text.gamename` の値を bundle.properties から読み、スクリプト内の 5×7 のブロック文字(1 マス 12 px)で白い文字とオレンジ(`#ff570a`)の影を描く。余白は左右 68 px、上下 17 px で透明。CodeRabbit の公式ロゴやマスコットは使っていない。同じ入力からは同じ PNG になる(2 回実行して SHA-1 `9a2e9f0b…` が一致)。A–Z、0–9、空白、`-` 以外の文字や、入りきらない名前はエラーで止まる。
- `HtmlLauncher`、ゲーム内の `logotext` アトラス画像、デスクトップのウィンドウタイトルは変えていない。

## 暫定の決定と変更箇所
- 製品名 `AI SDLC` は仕様で決まっていない暫定名。最終名はユーザーが決める。名前があるのは次の 2 か所だけ:
  1. `core/assets/bundles/bundle.properties` と `bundle_ja.properties` の `text.gamename`(両言語で同じ値。タイトル画面左下の表示)
  2. `html/webapp/index.html` の `<title>`(HTML シェルは多言語化していないので直書き)
  - 名前を変えたら `python3 -I tools/gen_logo.py` でロゴを作り直す(ロゴは 1 の値から作る)。`BrandingTest.gameNameIsTheSameEverywhere` が 1 と 2 の不一致を検出する。日本語の名前にする場合は、`gen_logo.py` が英数字しか描けないことと、美咲フォントの再生成が要ることに注意。
- オレンジ `#ff570a` は CodeRabbit 風の暫定色で、公式のブランドガイドラインとは照合していない(Web を見ていない)。公開前に確認が要る。使っている場所: `styles.css`(メーター)、`Links.java`(About のリンクの色帯)、`tools/gen_logo.py` の `ORANGE`(ロゴの影)。

## bundle とフォント
- `python3 -I tools/check_bundles.py`: `en keys: 581, excluded: 67, expected in ja: 514, ja keys: 514` / `OK`(4.3 時点は en 582、ja 515。+6 新規 − 7 削除 = −1)。
- `tools/bundle_exclude_keys.txt`: 変更なし(新キーはすべて両言語にあり、除外していない)。
- フォント(`collect_chars.py` → `bdf2fnt.py` → `check_fnt.py`): 818 → 829 コードポイント。追加 14 字(咲 問 字 布 年 改 文 真 第 美 行 見 証 題)、削除 3 字(公 式 聴。消した本家のリンク説明と「YouTube で聴けます」だけで使っていた字)。グリフ 815 個、alpha [0, 255]、矩形の重なり・はみ出し無し。missing は 2.2 からの既知の 14 個(U+3094–309A、309F、30A0、30F7–30FA、30FF)だけで、本タスクの文に BDF に無い字は無い。見本「美咲フォント門真なむ改変無保証」をスクラッチに書き出して確認した。

## テスト
- 新規 `core/src/test/java/io/anuke/mindustry/ui/BrandingTest.java`(5 件): `Links.getLinks()` が `coderabbit` / `https://coderabbit.ai` / 見出し `CodeRabbit` の 1 件だけ、新キー 7 個が両言語で `???` にならない、消した 7 件のリンク説明キーが両言語に無い、`text.gamename` が両言語で同じで `index.html` の `<title>` と一致する、`logo.png` が 610×124 でアルファ付き、隅が透明。
- `./gradlew :core:test --rerun`: 成功(18 クラス 119 件、失敗 0、エラー 0。4.3 時点の 114 件 + 5 件)。
- `./gradlew :html:dist`: 成功(`Compiling module io.anuke.mindustry.GdxDefinition` → `BUILD SUCCESSFUL`)。`html/build/dist` の `index.html` の `<title>` は `AI SDLC`、`html/logo.png` と `styles.css` は `html/webapp` のものと同一、`assets/ui/misaki-*.png` と `assets/bundles/bundle-*.properties` / `bundle_ja-*.properties` は `core/assets` のものと同一。`html/build/dist` を残してある。

## 確認していないこと(ユーザーがブラウザで確認する)
- タイトル画面: Editor / Donate / Discord が無いこと、Settings / About の行のバランス、左下の `AI SDLC <code> / <build>` 表示(英語・日本語)。
- About ダイアログ: リンクが CodeRabbit の 1 件だけで、色帯がオレンジ、アイコンがチェックマーク、リンクボタンで `https://coderabbit.ai` が開くこと。
- クレジットのダイアログ(英語・日本語): 4 節の折り返し、スクロール、字欠けが無いこと。日本語は 1 行 37 字ほど(幅 600 / 16 px)。
- HTML シェル: タブのタイトル、ロード中のロゴとオレンジのメーター。
- モバイルのレイアウトは実機が無いので確認していない(コードを読んだだけ)。
- デスクトップ版の起動(`:desktop:run`)はしていない。

## ブラウザ確認(Playwright、HTML ビルド)
- タイトル画面は Play / Settings(設定) / About(ゲーム情報)の 3 ボタンだけ。Editor、Donate、右上の Discord バナーは無い。左下は `AI SDLC classic / custom build`(`text.gamename` + `Version.code`)。
- About のリンクは CodeRabbit の 1 件(オレンジの帯、チェックのアイコン、英: "The AI code review tool for pull requests" / 日: 「プルリクエストを AI でレビューするツール」)。リンクボタンの `openURI` はヘッドレスでは追えないので未確認。
- クレジット: 当初は About の上に透けて重なり、Back ボタンも 2 つ重なって見えた。クレジットを開く間は About を閉じて戻ると About に戻す(`fba2a4b`)、本文の表に `pane` 背景を付ける(`43d3823`)の 2 点を直し、英日とも 4 節が読める状態になった。
- HTML シェル: `<title>` は `AI SDLC`、ロゴは白文字にオレンジの影のブロック文字、ロードメーターはオレンジ。
- 未確認: 外部リンクが実際に `https://coderabbit.ai` を開くこと(ブラウザの別タブ)、モバイルレイアウト、デスクトップ起動。
