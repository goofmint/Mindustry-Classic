# Issue #55 検証記録: ブラウザ版のゲーム内コンソールを F2 で開く

## 概要
- 日時: 2026-11-09
- ブランチ: `issue-55-console`(`issue-28-web` から分岐)
- 結論: ブラウザ版で F2 を押すとゲーム内コンソールが開き、もう一度押すと閉じる。ゲーム中のコンソールには `Log` の出力(Task 5.2 の `[world]` と `[pathfind]` の行)が出る。保存済みのグレーブキーも起動時に F2 へ置き換わる
- 証跡: `.tmp/verify-issue-55/`(PNG 6 枚)

## 変更
- `DefaultKeybinds`: `console` の既定キーを `consoleDefault(Vars.gwt)` にした。ブラウザ版は F2、デスクトップ版はグレーブキーのまま。保存値の置き換え規則 `migrateConsole(gwt, stored)` も同じクラスに置いた(どちらも純粋関数)
- `Control` のコンストラクタ: `KeyBinds.load()` の直後に、既定セクションのキーボード用 `console` だけを `migrateConsole` の結果で上書きする。ブラウザ版で保存値がグレーブキーのときだけ F2 になる。起動時には保存しない
- `Input`(ucore): F1〜F12 のキーコードを libGDX 1.13.1 の値(131〜142)に直した。並び順(保存値の ordinal)は変えていない
- `README.md`: 操作方法の表とその下の注記、英語の概要に、コンソールのキー(ブラウザ版 F2、デスクトップ版はグレーブキー)と macOS の Fn の注意を足した
- `DefaultKeybindsTest`(4 件): 既定キーの選び方、置き換え規則、F キーのコードが libGDX と一致すること

## 計画からの逸脱: F キーのコードの修正
計画どおり既定キーを `Input.F2` にしただけでは、F2 を押してもコンソールは開かなかった(1 回目のビルドで確認)。

- `Input.F1`〜`F12` のコードは 244〜255 で、libGDX 1.13.1 の `Keys.F1`〜`F12`(131〜142)と違っていた。旧版の libGDX の値が残っていたもので、`Inputs.keyTap` は `Gdx.input.isKeyJustPressed(input.code)` で比べるので、F キーはどれも反応しなかった
- F キーは他のどこでも使っていない(`grep` で確認)。コードを直しても ordinal は変わらないので、保存値は壊れない
- 同じ比較で、`ESCAPE`(131)、`END`(132)、`INSERT`(133)も libGDX 1.13.1 の値(111、123、124)と違う。131〜133 は 1.13.1 では F1〜F3 なので、今回の修正後は `ESCAPE` と `F1`、`END` と `F2`、`INSERT` と `F3` が同じコードになる。動作への影響は次のとおり
  - `menu` の既定の `ESCAPE` は、修正の前から F1 で発火している(操作設定の画面でも Menu の行が「F1」と表示される。`06-controls-menu-f1.png`)。今回の変更で変わらない
  - 操作設定の取り込みダイアログで F2 を押すと、`Input.findByType` が先に定義された `END` を返す。表示は「F2」で、発火するキーも F2 なので、使う上では問題ない
- `ESCAPE` などの修正は `menu` の挙動を変えるので、本 Issue では行わない。別 Issue にするかはオーナーの判断に任せる

## 環境
- macOS(arm64)、OpenJDK 17(`/opt/homebrew/opt/openjdk@17`)、Gradle 8.14(wrapper)
- Playwright 1.61.1 + Chromium(ヘッドレス、SwiftShader)、ビューポート 1280×800
- `html/build/dist` を `python3 -m http.server 8766 --bind 127.0.0.1` で配信。接続リセットで読み込みが崩れた回はやり直した

## ビルドとテスト
- `./gradlew :core:test`: 成功(`DefaultKeybindsTest` 4 件を含む)
- `./gradlew :html:dist`: 成功

## ブラウザでの確認
新しいブラウザコンテキスト(ローカルストレージが空)で行った。

| # | 手順 | 結果 | 証跡 |
|---|---|---|---|
| 1 | メインメニューで F2 | 左上に診断情報、上部中央にログ欄が出る | `01-menu-f2-on.png` |
| 2 | 続けて F2 | コンソールが閉じる | `02-menu-f2-off.png` |
| 3 | Play → New game → prototype、マップ読み込み後に F2 | ログ欄に `[world] map prototype (256x256, 1 spawns) loaded in 61 ms` と `[pathfind] initial paths for 1 spawns done in 548 ms ...` が出る | `03-game-f2-console-log.png` |
| 4 | ローカルストレージの console の保存値をグレーブキー(ordinal 74)にして再読み込み、F2 | 保存値は 74 のまま、F2 でコンソールが開く | `04-stored-grave-f2-on.png` |
| 5 | 4 の状態で Settings → Controls | Console の行が「F2」 | `05-controls-console-f2.png` |
| 6 | 5 で Reset to Defaults | 保存値が 171(`Input.F2` の ordinal)になる | ローカルストレージの値で確認 |

- 保存キーについて: 計画は保存キーを `keybind-null-keyboard-console` としていたが、ブラウザ版では `Section.name` が null の文字列連結が `undefined` になり、実際のキーは `io.anuke.moment:keybind-undefined-keyboard-consolei` だった。4 はこのキーで行った(最初に `null` のキーで試した回は、ゲームが値を読まないので既定値の F2 になっていただけだった。その回は証跡にしていない)
- 起動直後のログ(ロケール、ブロック数など)は、コンソールのログ欄には出ない。計画の想定どおりの既知の制限

## 未確認(オーナーの作業)
- デスクトップ版でグレーブキーでコンソールが開くこと(表示環境がないので未実施)。既定値はコード上 `Input.GRAVE` のままで、置き換えはブラウザ版だけ
- 実機の Chrome と Firefox で F2 が届くこと。macOS ではメディアキーの設定によって Fn+F2 が必要
- `ESCAPE`/`END`/`INSERT` のキーコードのずれ(上記)を別 Issue にするか
