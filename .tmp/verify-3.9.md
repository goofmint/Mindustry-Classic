# Task 3.9 検証記録(CoreBlock と勝敗、Issue #20)

ブランチ: `issue-20-core`(`issue-19-coderabbit` の上に積んだ)

## 変えたこと

- `Fx.java`: `shockwaveSmall` の後に `deliverGood`(白から緑へ広がる輪、12 フレーム)と `deliverBad`(白から赤 `SCARLET`)を足した
- `CoreBlock.handleItem()`: `Net.server() || !Net.active()` の条件はそのまま。`state.inventory.addItem(item, 1)` をやめ、`CodeItem.score(item)` を `state.score.deliver(item, score)` に渡し、得点が 0 以上なら `Fx.deliverGood`、負なら `Fx.deliverBad` をコアの中心に出す。燃料と credit は得点 0 で受け取って消える(インベントリに入らない)。`acceptItem()` は `true` のまま
- `Logic`: パッケージ内の `enum Outcome { none, won, bankrupt }` と `static Outcome evaluateOutcome(GameState)` を足し、`update()` のコア破壊の判定の直後で `!state.gameOver` のときに呼ぶ。`none` 以外なら `gameOver = true`、`won = (outcome == won)` にして `GameOverEvent` を発火する
- `Control`:
  - `GameOverEvent` ハンドラ: 先に `bestscore<map>` を保存する(`infiniteResources` でも `disableWaveTimer` でもなく、残高が保存値より大きいとき。`hiscore` と同じ条件)。保存したら `bestscore` フラグを立てる。`state.won` のときは揺れ、音、爆発を出さない。`ui.restart.show()` と 30 フレーム後の `state.set(State.menu)` は勝ち負けとも同じ
  - `Settings.defaults("bestscore" + map.name, 0)` を `hiscore` の defaults の隣に足した。`ResetEvent` で `bestscore = false`。`isBestScore()` を足した
  - 勝ちの専用演出(design.md の `Fx.victory`)は作っていない(最小の変更にとどめた)
- `RestartDialog.shown()`: タイトルを `won` → `$text.victory`、`score.bankrupt()` → `$text.bankrupt`、それ以外 → `$text.gameover` にする。本文はハイスコア(従来)、自己ベスト(`$text.bestscore`、新規)、`text.delivered`(残高)、到達ウェーブ(従来)の順。メニューボタンは従来どおり
- 文言(両言語、`text.gameover` の直後): `text.victory`、`text.bankrupt`、`text.delivered`、`text.bestscore`。`python3 -I tools/check_bundles.py` → OK(ja 487 キー)
- 美咲フォント: 「己」が新しい字だったので作り直した(818 字。欠ける 14 字は以前と同じ)

## 判定の順番

1. コア破壊(従来の判定。`won = false`)
2. 目標到達: `!infiniteResources` かつ `score.reachedTarget()`(`target > 0 && balance >= target`)
3. 破産: `!infiniteResources` かつ `score.bankrupt()`

- サンドボックスは `inventory.fill()` で credit が満タンになり、どの目標もすぐ超えるので、得点では勝ち負けを判定しない
- `targetScore <= 0` のマップは得点で勝てないが、破産では負ける
- 同じフレームで目標到達と破産の両方が成り立つときは勝ち

## テスト

`./gradlew :core:test`: 88 件すべて成功(既存 81 件 + `LogicOutcomeTest` 6 件 + `ScoreTest` 1 件)。

- `LogicOutcomeTest`: 目標未達で破産なし → none、目標到達 → won、破産 → bankrupt、目標 0 は勝たないが破産はする、両方成り立てば won、サンドボックスは目標到達でも破産でも none
- `ScoreTest.negativeCodeItemLowersBalance`: 品質の段階 0 のコードを `CodeItem.score()` の得点で納品すると残高が減る

## 実行

- `./gradlew :desktop:run`: GLFW のモニター取得で NullPointerException(`glfwGetMonitorPos`)。画面がスリープしている環境の問題で、これまでのタスクと同じ。起動後に残ったプロセスは止めた
- `./gradlew :html:dist`: 成功。`html/build/dist` に出力を残した

## ブラウザで確かめること(未確認)

- **勝ち**: 組み込みのマップはすべて `targetScore` が 0 なので、Task 4.1 でマップを足すまで勝ちのダイアログは見られない。足したあと、残高が目標以上になると「目標達成！」のダイアログが出て、コアが爆発しないこと
- **破産**: 品質の低いコード(段階 0〜3)をレビューを通さずにコアへ流し、残高が 0 を下回ると「残高がゼロを下回りました」のダイアログが出ること。納品した得点(0)と到達ウェーブが出ること
- **コア破壊**: 従来どおり爆発して「コアが破壊されました。」のダイアログが出ること
- 納品のたびにコアの上に緑(得点 0 以上)か赤(負)の輪が出ること
- ウェーブモードで 1 回目のゲームオーバーでは残高が 0 より大きければ「自己ベスト更新！」が出ること(既定値が 0 のため)

## 気になる点

- 破産の負けでもコアの爆発演出が出る(依頼の計画どおり、`won` でなければ従来の演出)
- マルチプレイのサーバーでは、得点による終了で `NetEvents.handleGameOver()` を呼んでいない(コア破壊の判定だけが呼ぶ)。Web 版はマルチプレイに対応していないので触っていない
