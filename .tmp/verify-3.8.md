# Task 3.8 検証記録(CodeRabbitBlock、Issue #19)

ブランチ: `issue-19-coderabbit`(`issue-18-gate` の上に積んだ)

## 変えたこと

- `CodeRabbitBlock extends PowerTurret`(新規、`world/blocks/types/defense/CodeRabbitBlock.java`)
  - 設定値: `passedBit = CodeItem.REVIEW`、`stages = 2`、`processTime = 3f`(秒)、`powerPerItem = 0.6f`。`timerDump` を `timers` に足した
  - `CodeRabbitEntity extends PowerTurretEntity implements GateLogic.GateState`(`current`、`progress`。`getPower/setPower` は `power`)
  - `update()`: 先に `super.update()`(迎撃)、次に `GateLogic.update(entity, passedBit, stages, processTime, powerPerItem, Timers.delta())` の結果を `offloadNear()` で出す。5 フレームごとに、出し待ちがあれば `tryDump()`(QualityGate と同じ)
  - `acceptItem()`: `GateLogic.accepts(entity, item, entity.totalItems() > 0)`(PowerTurret の常に false を上書き)
  - `handleItem()`: `source == tile` は `entity.items` に入れる(出し待ち)、それ以外は `current` に入れて `progress = 0`
  - `init()`: 緑の進捗バーを足す(処理中でなければ -1 で描かない)。処理中アイテムの中央描画はしない(砲塔の画像が上に重なるため)
  - `getStats()`: PowerTurret の行に `Seconds/item`、`Power/item`、`Quality Gain` を足した(キーは 3.7 で追加済み)
  - 保存: `super`(電力 float)の後に `current` の ID(byte、空は -1)と `progress`(float)。計 9 バイト
- **電力の共有**: 迎撃(1 発 `powerUsed` 0.2)と変換(1 個 `powerPerItem` 0.6)は同じ `entity.power` から引く。それぞれ自分の量だけを見るので、電力が 0.2 以上 0.6 未満のときは迎撃が続き、変換は電力が貯まるまで待つ。毎フレーム迎撃を先に処理するので、撃つほうが優先される。電力が 0.2 未満になると両方止まる
- 弾: 既存の `BulletType` にダメージ 10 のものが無いため、`BulletType.coderabbitShot`(速さ 2.7、ダメージ 10、lifetime 40、`shot` と同じ線の描き方で色は白からオレンジ `ff570a`)を `beamlaser` の後(末尾)に足した。既存の弾の ID はずれない
- `WeaponBlocks.java` の末尾(`titanturret` の後)に `coderabbit`: `shootsound = "laser"`、射程 60、リロード 4、体力 110、`powerUsed = 0.2f`、弾 `coderabbitShot`。ブロック総数は 105 から 106(保存は名前で対応付けるので、後ろの `playerspawn`/`enemyspawn` の ID がずれても読み込みに影響しない)
- `Recipes.java`: `weapon` 区分の `titanturret` の後に `stack(Item.credit, 100)`。レシピは 56 件から 57 件(`RecipesTest` を 57 に直した)
- 砲塔画像 `core/assets-raw/sprites/blocks/coderabbit.png`(10×10): `laserturret.png` を PIL で色替え。紫の本体をオレンジ(色相 21°)、水色の部分を琥珀色(色相 40°)にし、灰色と透明はそのまま。`laserturret-icon.png` は無いのでアイコンは作っていない。`./gradlew :core:pack` でアトラスを作り直した
- 文言: 両言語の `titancannon` の後に `block.coderabbit.name=CodeRabbit` と `.description`。`python3 -I tools/check_bundles.py` → OK(ja 483 キー)
- 美咲フォント: 「迎」が新しい字だったので作り直した(817 字。欠ける 14 字は以前と同じ)

## テスト

`./gradlew :core:test`: 81 件すべて成功(既存 72 件 + `CodeRabbitBlockTest` 9 件)。

- 設定値(名前、射程、リロード、powerUsed、体力、弾とダメージ 10、passedBit、stages、processTime、powerPerItem)
- `PowerAcceptor` かつ `PowerTurret`、エンティティが `GateState`
- ID が byte に収まり総数 127 以下
- `weapon` 区分で credit 100
- 燃料と credit を拒み、コードを 1 個受けたら次を拒む
- 出し待ちは `entity.items` に入り、次の入力を拒む
- 電力 0.5 では進まず、電力 1 で 180 フレーム後に `CodeItem.advance(code, REVIEW, 2)` を返し、電力 0.6 を使う
- エンティティの保存と読み込み(処理中と空、9 バイト)

## 起動確認

- `./gradlew :html:dist` 成功。出力は `html/build/dist` に残した
- `./gradlew :desktop:run` はウィンドウ作成(`GLFW.glfwGetMonitorPos`)で NullPointerException(3.7 と同じ環境側の問題。ゲームのコードに入る前)

## ブラウザ版で目で確かめること(未確認)

- 配置パネルの weapon タブに CodeRabbit(オレンジの砲塔)が出て、置くと credit が 100 減る
- 電力を入れると敵を撃ち(オレンジの弾)、同時にコンベアで入れたコードを 3 秒ごとに変換する(緑の進捗バー)
- 変換後のコードは REVIEW を通り品質が 2 段階上がっている(色とドット)
- 電力が無いと迎撃も変換も止まり、電力が少ない(0.2〜0.6)間は迎撃だけ続く
- 出口が詰まると中に 1 個溜まり、それ以上受けない

## 既知の制限と懸念

- 弾の lifetime 40 × 速さ 2.7 = 108 で射程 60 より遠くまで飛ぶ(他の砲塔の弾と同じく、外れた弾は射程の外で消える)
- 撤去しても処理中のコードと出し待ちは返らない。マルチプレイヤーの同期は考えていない
