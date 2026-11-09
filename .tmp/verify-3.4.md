# Task 3.4 検証記録(Score と残高、Issue #15)

ブランチ: `issue-15-score`(`issue-14-icons` の上に積む)

## 実装内容

- `game/Balance.java`(新規): 定数のみ。`INITIAL_CREDIT = 150`、`QUALITY_HISTORY = 20`、`MULTIPLIER_SLOPE = 0.3f`、`MULTIPLIER_MIN = 0.7f`、`MULTIPLIER_MAX = 2.5f`、`FRACTION_EPSILON = 1e-4f`
- `game/Score.java`(新規): `Score(Inventory)`。`Vars` を参照しない
  - `balance()` は `inventory.getAmount(Item.credit)`
  - `deliver(item, score)`: `fraction += score`、`whole = (int)(fraction + copySign(EPS, fraction))`(0 方向へ切り捨て、誤差を許容)、`fraction -= whole`(符号つきの余りを残す)。`whole != 0` のとき `inventory.getItems()[credit.id]` に足す(HUD の更新フラグが立つ)。結果が負なら 0 にして `bankrupt = true`。ちょうど 0 は破産でない。破産後も納品は受け付け、フラグは `reset()` まで残る
  - コードアイテム(`CodeItem.attrOf != null`)だけ `QUALITY[stage]` を直近 20 個のリングバッファに入れ、`qualityMultiplier = multiplierFor(平均)` を更新。合計は毎回取り直す(最大 20 個、誤差の蓄積を避ける)
  - `multiplierFor(q) = clamp(1 - 0.3q, 0.7, 2.5)`(パッケージプライベート、static)
  - `reachedTarget()` は `target > 0 && balance() >= target`。`reset(initial)` は残高・端数・履歴・破産・倍率を戻し、`target` は触らない
  - `fraction` と履歴は保存しない(設計書 §4.1 のとおり、ロード直後は倍率 1.0)
- `GameState`: `public final Score score = new Score(inventory);`、`public boolean won;`
- `Inventory.clearItems()`: `addItem(Item.stone, 40)` → `addItem(Item.credit, Balance.INITIAL_CREDIT)`。debug の 99999 埋めは残す
- `Logic.reset()`: `state.won = false`、`state.score.reset(Balance.INITIAL_CREDIT)` を `clearItems()` の前に呼ぶ(`clearItems()` の値が最終値になる。ロード時は `reset()` → インベントリ復元の順なので、復元値は上書きされない)
- `Map.targetScore`(0 = 未設定)。`World.loadMap(Map, int)` で `state.score.target = map.targetScore`。`maps.json` は未変更(Task 4.1 で値を入れる)

## テスト(`./gradlew :core:test`)

全 44 件緑(failures 0、errors 0)

| クラス | 件数 |
|---|---|
| ScoreTest(新規) | 18 |
| MapTargetScoreTest(新規、`io` パッケージ。`Maps.ArrayContainer.maps` がパッケージプライベートのため) | 1 |
| CodeItemTest | 16 |
| ItemTest | 6 |
| ItemIconsTest | 2 |
| SmokeTest | 1 |

ScoreTest の内容: 0.5×2 で +1、1.0 ちょうどで +1、0.8×5 で 5 回目に計 +4、負の納品の即時反映、更新フラグ、0 割れで破産、破産の持続と納品継続、ちょうど 0 は破産でない、`removeItem` 超過は 0 で破産でない、倍率の境界値(1→0.7、0→1.0、-5→2.5、範囲外のクランプ、-1→1.3)、履歴が空なら 1.0、コード以外は倍率を変えない、コード納品で倍率更新、21 個目で最古が抜ける、`reset()` で全部戻り `target` は残る、`reachedTarget()`(target 0 は false、等号で true)

MapTargetScoreTest: `Maps` と同じ `setElementType` で `{"maps":[{"id":0,"name":"a","targetScore":1000},{"id":1,"name":"b"}]}` を読み、1000 と 0 を確認

## ビルドと起動

- `./gradlew :desktop:compileJava`: 成功
- `./gradlew :desktop:run`: 約 50 秒動かして終了。ログは `Got locale: ja, bundle locale: ja`、`Total blocks loaded: 96` のみで、例外なし。メニューまでの確認で、ゲーム開始(`Logic.reset()` / `World.loadMap()`)はこの起動では操作していない
- `./gradlew :html:dist`: BUILD SUCCESSFUL(`Math.copySign` を含め GWT コンパイル通過)。出力は残してある

## 既知の状態

- 初期の石 40 が無くなり credit 150 になったため、旧レシピ(石・鉄などを要求)は Task 3.5 でレシピを差し替えるまで建てられない。本家チュートリアル(コンベアに石が要る)も同じ理由でこの時点では進められないが、レシピは Task 3.5 で credit に替わり、チュートリアル自体は Task 3.11 で削除するので、石の補充は行わない(PR #46 のレビュー指摘への回答)
- HUD のアイテム欄に credit 150 が出ることはブラウザで確認する(未確認)
- `CoreBlock` の納品、勝敗イベント、`EnemySpawn` の倍率適用は後続タスク
