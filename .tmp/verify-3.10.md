# Task 3.10 検証記録: 敵の出現倍率

## 式
`EnemySpawn.evaluate(wave, lane)` は従来どおり base(max で上限済み)を求め、`applyMultiplier(base, state.score.qualityMultiplier, Mathf.random(1f))` を返す。

- scaled = base * multiplier
- result = (int)scaled(切り捨て)
- roll < (scaled - result) なら result++(端数の確率繰り上げ)
- 範囲外ウェーブ/spacing による `return 0` は倍率をかけない。倍率後の上限は設けない(調整は Task 5.3)。

## テスト(EnemySpawnTest)
- 倍率 1.0: 任意の roll で base のまま
- 倍率 2.0: 3 -> 6
- 倍率 1.5, base 3 (4.5): roll < 0.5 で 5、roll >= 0.5 で 4
- 倍率 0.7: 10 -> 7、3 -> roll 0.99 で 2、roll 0 で 3(`EnemySpawnTest.lowMultiplierReduces` は roll に関係なく 7 ちょうどを検証する。`applyMultiplier()` は切り上げで端数を扱うため、base 10 × 0.7 = 7 で端数が出ない)
- base 0: 常に 0

`WaveCreator` に `target` 型は無い(変更なし)。`Logic.runWave()` も変更なし。

## HUD
FPS 設定が ON のときだけ、`infolabel` に `Mult: x.xx` を追加表示する(英語固定のデバッグ表示)。

## ブラウザ確認(未実施、オーケストレータ待ち)
FPS 表示を ON にして、低品質コードを納品し、Mult が上がって次のウェーブの敵数が増えることを確認する。
