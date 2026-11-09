# Issue #79 検証記録: メインメニューのタイトルを「AI SDLC by CodeRabbit」にする

## 概要
- 日時: 2026-11-09
- ブランチ: `issue-79-title`(`origin/main` から分岐)
- 結論: メインメニュー上部のロゴが「AI SDLC / BY CODERABBIT」の 2 行になった。読み込み画面のロゴ(`html/webapp/html/logo.png`)、タブのタイトル、`text.gamename`、左下のバージョン表示は変わらない
- 証跡: `.tmp/verify-issue-79/`(PNG 9 枚)

## 変更

### 画像の生成(`tools/gen_logo.py`)
- Web 版の読み込みロゴに加えて `core/assets-raw/sprites/ui/logotext.png` も書き出すようにした(1 回の実行で両方)
- 1 行目は `bundle.properties` の `text.gamename`(「AI SDLC」)を 1 セル 2 px、2 行目はモジュール定数 `SUBTITLE`(「BY CODERABBIT」)を 1 セル 1 px で描く。既存の 5x7 グリフ、白い文字と `#ff570a` の影(オフセット 1 px)を使い、横は中央揃え、行間 1 px、背景は透明
- 上端に `MENU_TOP_MARGIN`(3 px)の透明行を置く。`BackgroundFragment` はロゴを画面上端より 15 px 上(6 倍表示で元画像 2.5 px 分)に描くので、余白がないと 1 行目の上端が欠ける。旧ロゴも上 2 行が透明だった
- 出来上がりは 79x27(旧ロゴは 89x21)。上限は `MENU_MAX_WIDTH`=96、`MENU_MAX_HEIGHT`=28 で、超えると `sys.exit` で止まる。未対応の文字も従来どおりエラー
- 既存の描画ループは `paint()` に切り出して両方のロゴで使う。Web 版の `logo.png` は実行前後で sha1 が一致(`9a2e9f0b08ba34e02593ab22204153d5ce6ecfd6`)
- `BackgroundFragment.java` は変えていない

### アトラス(`./gradlew :core:pack`)
- `core/assets/sprites/sprites.atlas` と `sprites.png` を再生成した。`logotext` は `xy: 424, 258`、`size: 79, 27`、`orig: 79, 27`、`rotate: false`、`offset: 0, 0`。ページは 1024x512 の `sprites.png` 1 枚のまま。他の領域の座標は移動している
- `core/assets/ui/uiskin.atlas` にも `logotext` があるが、メニューは `Core.atlas`(sprites.atlas)から作ったスキンの領域を使うので触っていない

### テスト(`BrandingTest`、2 件追加、既存 5 件は変更なし)
- `menuLogoSourceIsSmallWithTransparentCorners`: 元画像がアルファ付き、96x28 以内、四隅が透明
- `menuLogoIsPackedUnchanged`: アトラスの `logotext` が 1 件だけ、`size` が元画像と一致、`orig` と同じ、`rotate: false`、`offset: 0, 0`、ページが 1 枚。`xy` と `size` でページから切り出した領域を元画像と比較(アルファ > 0 の画素は RGBA 一致、アルファ 0 の画素はアルファだけ比較)

## 計画からの逸脱
- 計画は「文字と影にぴったりのキャンバス、高さ 24 px 以下」だったが、上端の 3 px の透明余白を足したので高さは 27 px、上限は 28 px にした。余白なしでは 6 倍表示で 1 行目の上 2.5 px が画面外に出て、「AI SDLC」の上端が欠ける(`BackgroundFragment` は変更しない指示のため、画像側で吸収した)
- 縦長画面(600x900)の確認は Web 版の仕組み上、縦長にならない。`HtmlLauncher` がキャンバスを 4:3 に固定して中央に置くため、600x900 のビューポートでは 600x449 の横長キャンバスになり、`BackgroundFragment` の縦長分岐(5/7 倍、30 px 下げ)は通らない

## 環境
- macOS(arm64)、OpenJDK 17、Gradle 8.14(wrapper)、Python 3 + Pillow 12.2.0
- Playwright 1.61.1 + Chromium(ヘッドレス、SwiftShader)。ビューポート 1280x800、1024x640、600x900
- `html/build/dist` を `ThreadingHTTPServer`(待ち行列 512)で 127.0.0.1:8766 に配信し、終了後に停止した

## ビルドとテスト
- `python3 -I tools/gen_logo.py`: `logo.png`(610x124、変更なし)と `logotext.png`(79x27)を書き出した
- `./gradlew :core:pack`: 成功
- `./gradlew :core:test`: 成功(154 件、失敗 0。`BrandingTest` 7 件)
- `python3 -I tools/check_bundles.py`: OK(en 588、除外 67、ja 521)
- `./gradlew :html:dist`: 成功
- `core/assets/crash-report-*.txt` は出ていない

## ブラウザでの確認
新しいブラウザコンテキスト(ローカルストレージが空)で、ビューポートごとに英語のメニューを撮り、Settings → Language → 日本語 → 再読み込みで日本語のメニューを撮った。

| # | ビューポート | 内容 | 結果 | 証跡 |
|---|---|---|---|---|
| 1 | 1280x800 | 読み込み画面 | ロゴは「AI SDLC」1 行のまま(変更なし)。`document.title` は「AI SDLC」 | `07-loading-1280x800.png` |
| 2 | 1280x800 | 英語メニュー | 「AI SDLC / BY CODERABBIT」。白い画素はキャンバスの y=3〜140、x=403〜870。Play ボタンの上端は y≈335。左下の「AI SDLC classic / custom build」は変わらない | `01-en-menu-1280x800.png`、`09-logo-crop-1280x800.png`(等倍の切り抜き) |
| 3 | 1280x800 | 言語を日本語に変更 | 「Please restart your game…」の案内 | `08-language-selected-1280x800.png` |
| 4 | 1280x800 | 再読み込み後の日本語メニュー | ロゴは同じ。ボタンは「プレイ / 設定 / ゲーム情報」 | `02-ja-menu-1280x800.png` |
| 5 | 1024x640 | 英語 / 日本語 | 白い画素は y=3〜140、x=274〜741。Play の上端は y≈255 | `03-en-menu-1024x640.png`、`04-ja-menu-1024x640.png` |
| 6 | 600x900 | 英語 / 日本語 | キャンバスは 600x449(4:3 固定、上下に黒帯)。白い画素はキャンバス内の y=3〜140、x=63〜530。Play の上端はキャンバス内 y≈160 | `05-en-menu-600x900.png`、`06-ja-menu-600x900.png` |

いずれもロゴの上端は欠けず(1 行目の上に 3 px の余白が残る)、メニューのボタンや左下のバージョン表示と重ならない。コンソールに `pageerror` はない(音楽ファイルの `ERR_ABORTED` は従来どおりで、今回の変更とは無関係)。

## 未確認(オーナーの作業)
- デスクトップ版の表示(この環境では起動できない)。`Unit.dp.scl(6)` はデスクトップでも 6 なので、Web 版と同じ 474x162 px の表示になる想定
- 縦長画面での表示(`BackgroundFragment` の縦長分岐)。Web 版では 4:3 固定のため到達できない。計算上は 79x27 の画像が 4.29 倍(339x116 px)になり、上端から 15 px 下に描かれるので欠けない
- 実機の Chrome と Firefox での見え方(ヘッドレスの SwiftShader で確認した)
