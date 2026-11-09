# Issue #59 検証記録: ESCAPE / END / INSERT のキーコードを libGDX に合わせる

## 概要
- 日時: 2026-11-09
- ブランチ: `issue-59-keys`(`issue-56-ime` から分岐)
- 結論: ブラウザ版で Esc を押すとゲーム中にポーズメニューが開き、F1 では開かない。操作設定の Menu の行は「Escape」と表示される
- 証跡: `.tmp/verify-issue-59/`(PNG 4 枚)

## 変更
- `Input`(ucore): `ESCAPE` を 131 から 111、`END` を 132 から 123、`INSERT` を 133 から 124 に直した(libGDX 1.13.1 の `Keys` の値)。宣言の順序は変えていない。保存値は ordinal なので、保存済みの設定は壊れない。移行処理は入れていない
- F1〜F12 は Issue #55 で 131〜142 に直済みのため触っていない
- 111・123・124 を使うキーボードの項目は他にない(`grep` で確認。`BUTTON_MODE(110)` は別の種類のキー)
- `InputKeyCodesTest`(3 件): `ESCAPE`/`END`/`INSERT`/`F1`〜`F12` のコードが `Keys` と一致すること、ordinal の不変条件(全 183 個、`ESCAPE`/`END`/`INSERT` が 136/137/138、`F1`/`F12` が 170/181、`UNSET` が 182)、`findByType` が 111 に `ESCAPE`、131 に `F1` を返すこと

## 環境
- macOS(arm64)、OpenJDK 17、Gradle 8.14(wrapper)
- Playwright + Chromium(ヘッドレス)、ビューポート 1280×800
- `html/build/dist` を配信。標準の `http.server` は接続リセットで読み込みが崩れたため、待ち行列を広げた `ThreadingHTTPServer` で配信した

## ビルドとテスト
- `./gradlew :core:test`: 成功(`InputKeyCodesTest` 3 件を含む)
- `./gradlew :html:dist`: 成功

## ブラウザでの確認
新しいブラウザコンテキスト(ローカルストレージが空)で行った。

| # | 手順 | 結果 | 証跡 |
|---|---|---|---|
| 1 | Settings → Controls | Menu の行が「ESCAPE」(Issue #55 の時点では「F1」) | `01-controls-menu-escape.png` |
| 2 | Play → New game → prototype、マップ読み込み後 | ゲーム画面 | `02-game-before.png` |
| 3 | F1 | 何も開かない | `03-after-f1.png` |
| 4 | 続けて Esc | ポーズメニュー(Back / Settings / Save Game ほか)が開き、「PAUSED」が出る | `04-after-esc-pause-menu.png` |

## 未確認(オーナーの作業)
- デスクトップ版での Esc の動作(表示環境がないので未実施)
- 実機の Chrome と Firefox での Esc。ブラウザによっては Esc がフルスクリーンの解除などに使われる
- 旧版で F1 を Menu に再バインドして保存した設定は、F1 のまま残る(移行処理は入れない方針)
