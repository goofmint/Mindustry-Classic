# Issue #52 検証記録: 一時停止系ダイアログの背景が暗転せず、背後の UI と重なって読めない(#53 の根本原因も同じ)

- ブランチ: `issue-52-dialogs`(`issue-53-saveconfirm` から分岐)
- 確認日時: 2026-11-09 JST
- ブラウザ: Playwright 同梱のヘッドレス Chromium、1280×800、新しいコンテキスト
- デスクトップ版: 未確認(ディスプレイが無く `:desktop:run` が起動しない)

## 原因

`core/src/io/anuke/ucore/scene/style/SpriteDrawable.java` の `draw()`(2 つのオーバーロードとも)が `sprite.getColor()` の参照をそのまま保存していた。続く `sprite.setColor(batch.getColor().mul(spriteColor))` で、`mul` が batch の内部 `Color` を書き換えるうえ、`spriteColor` は sprite 自身の色オブジェクトなので、「復元」の `sprite.setColor(spriteColor)` は何も戻さない。結果として、描画のたびに共有 sprite の tint に batch の色(フェード中の alpha)が掛かり続け、`dialogDim`(skin の `dialog` スタイルの `stageBackground`)の alpha が 0 に向かって減衰し、暗転が消える。

## 修正

`SpriteDrawable.draw()` の両オーバーロードで、tint を static の一時 `Color`(`savedTint`)へ値でコピーし、batch 色 × tint を別の一時 `Color`(`drawTint`)で計算して `sprite.setColor(drawTint)` で描画し、描画後に `sprite.setColor(savedTint)` で元の tint を戻す。`batch.getPackedColor()` の保存と `setPackedColor` の復元は従来どおり。他の drawable、skin、`Window`、各ダイアログは変更していない。AboutDialog のクレジットと RestartDialog の `pane` 背景(.tmp/verify-4.4.md)もそのまま。

## 単体テスト

`core/src/test/java/io/anuke/ucore/scene/style/SpriteDrawableTest.java` を追加した。`new Sprite()`(Texture なしで生成可能。`new Sprite(new TextureRegion())` は Texture が null で NPE になる)と、`java.lang.reflect.Proxy` による最小の fake `Batch`(色だけ保持し、他の呼び出しは無視)を使い、GL なしで動く。batch alpha 0 / 0.5 / 1 の各ケースで、両オーバーロードを 20 回描画し、sprite の tint の alpha(0.9)が変わらないことと、batch の packed 色が元に戻ることを確認する。

- 修正後: `./gradlew :core:test` 成功
- 修正前のコードに戻して実行: batch alpha 0 と 0.5 の 2 件が失敗(alpha 1 は mul が恒等のため通る)。回帰テストとして機能することを確認した

## ブラウザでの確認(`:html:dist` を再ビルド)

証跡は `.tmp/verify-issue-52/` にある。

| 確認 | 結果 | 画像 |
|---|---|---|
| プロトタイプ開始 → ポーズメニュー → Save game を 5 回連続で開閉し、5 回目を撮影 | ポーズメニューのボタンが十分に暗く、Save game の文字・Back が読める。5 回目でも暗転が薄くならない | `save-dialog-5th-open.png` |
| Save game → New save(名前入力ダイアログ) | 背後のセーブ画面(Back を含む)も暗くなっている | `save-newname-dim.png` |
| セーブ済みスロットをクリックして上書き確認 | 背後のセーブ一覧と SaveDialog の Back が暗転で隠れ、確認ダイアログの OK / Cancel が読める | `save-overwrite-confirm.png` |
| メインメニュー About → Credits | 背後のメニューが暗くなっている(本文は `pane` 背景のまま) | `about-credits.png` |

## #53 との関係

#53 の「Back の位置をクリックすると上書きされる」は、確認ダイアログの OK がセーブ画面の Back と同じ位置に重なり、背後が暗転しないため Back が見えたままになることが原因だった(`.tmp/verify-issue-53.md`)。この修正で背後のセーブ画面が暗転して Back が視覚的に隠れ、確認ダイアログが目立つため、誤クリックは起きにくくなる。ただし OK と Back の座標が重なる配置自体は変えていない(クリックの当たり判定はモーダルが先に受ける)。配置を変える修正はしていない。

## 確認していないこと

- デスクトップ版(ディスプレイが無い)
- `loadDim` と `LoadDialog` の `clear` ボタン(同じ `SpriteDrawable` を使う他の tint 付き drawable)を繰り返し表示した確認
- Android
