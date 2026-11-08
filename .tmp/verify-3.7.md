# Task 3.7 検証記録(QualityGate、Issue #18)

ブランチ: `issue-18-gate`(`issue-17-codesource` の上に積んだ)

## 変えたこと

- `GateLogic`(新規、`world/blocks/types/production/GateLogic.java`): 変換部分の静的ヘルパー。Task 3.8 の CodeRabbitBlock からも呼ぶ
  - 状態の interface `GateLogic.GateState`: `getCurrent()/setCurrent(Item)`、`getProgress()/setProgress(float)`、`getPower()/setPower(float)`
  - `update(state, passedBit, stages, processTime, powerPerItem, delta)`: 処理中のコードがあり、`powerPerItem == 0` か `power >= powerPerItem` のときだけ `progress += delta / (60 * processTime)`。電力不足の間は進捗が止まる。`progress >= 1` で電力を引き、`CodeItem.advance()` の結果を返して `current = null`、`progress = 0`。それ以外は null
  - `delta` を引数にした理由: `Timers.delta()` が `Gdx.graphics` を読むので、単体テストで呼べない。ブロック側が `Timers.delta()` を渡す
  - `accepts(state, item, hasPendingOutput)`: `CodeItem.attrOf(item) != null && current == null && !hasPendingOutput`。燃料や credit は受けない
  - 出力の受け渡し(`offloadNear` / `tryDump`)はしない
- `QualityGate extends PowerBlock`(新規)と `GateEntity extends PowerEntity implements GateLogic.GateState`(`current`、`progress`)
  - `acceptItem()`: `GateLogic.accepts(entity, item, entity.totalItems() > 0)`。施設の中のコードは常に 1 個まで
  - `handleItem()`: `source == tile`(`offloadNear` が隣に出せず自分に戻したとき)は `entity.items` に入れる(出し待ち)。それ以外は `current` に入れ、`progress = 0`
  - `update()`: `GateLogic.update(..., Timers.delta())` の結果を `offloadNear()` で出す。5 フレームごとに、出し待ちがあれば `tryDump()`
  - `acceptsPower()`: `powerPerItem > 0` のときだけ PowerBlock の判定。シニアは電力を受けない
  - `init()`: `powerPerItem == 0` なら PowerBlock のコンストラクタが最後に足した電力バーを外す。緑の進捗バーを足す(処理中でなければ -1 を返し、`Renderer` が描かない)
  - `draw()`: 処理中のコードのアイコンを中央に 4×4 で描く(Conveyor と同じ大きさ)
  - `getStats()`: `Seconds/item`、`Power/item`、`Quality Gain`。`text.blocks.qualitygain` を両言語に足した(品質の上昇 / Quality Gain)
  - 保存: `GateEntity.write/read` は `super`(電力 float)の後に `current` の ID(byte、空は -1)と `progress`(float)。読み込みで -1 か範囲外なら `current = null`
  - 1×1、蓄電容量 10(PowerBlock の既定)。ネット同期、撤去時の返金は扱わない
- `ProductionBlocks.java` の末尾(`aiAgent3` の後)に 5 種。既存ブロックの ID はずれない。ブロック総数は 100 から 105

| フィールド | 登録名 | passedBit | stages | processTime(秒) | powerPerItem | 費用(credit) |
| --- | --- | --- | --- | --- | --- | --- |
| seniorEngineer | senior-engineer | REVIEW | 1 | 5 | 0 | 80 |
| unitTest | unit-test | TEST | 1 | 2 | 0.3 | 30 |
| e2eTest | e2e-test | TEST | 1 | 4 | 0.5 | 45 |
| jenkins | jenkins | CI | 1 | 3 | 0.4 | 60 |
| githubActions | github-actions | CI | 1 | 2 | 0.4 | 80 |

- `Recipes.java`: `crafting` 区分に 5 種(AI エージェントの後)。レシピは 51 件から 56 件
- ブロック画像: 8×8 の仮絵 `senior-engineer.png`(紫)、`unit-test.png`(緑)、`e2e-test.png`(青緑)、`jenkins.png`(赤茶)、`github-actions.png`(青)。Python 標準ライブラリで作り、`./gradlew :core:pack` でアトラスを作り直した
- 文言: 両言語の `ai-agent-3` の後に `block.<name>.name` と `.description` を 5 種分。`python3 -I tools/check_bundles.py` → OK(ja 481 キー)
- 美咲フォント: 「早」「昇」「段」「階」が新しい字だったので、`tools/README.md` の手順で作り直した(816 字、`check_fnt.py` で検査済み。欠ける 14 字は以前と同じ BDF に無い仮名)

## テスト

`./gradlew :core:test`: 71 件すべて成功(既存 55 件 + `GateLogicTest` 8 件 + `QualityGateTest` 8 件)。`RecipesTest` のレシピ数を 56 に直した。

- `GateLogicTest`(libGDX に依存しない状態のスタブ): 空なら何もしない / 2 秒 = 120 フレームで delta 1 × 60 回なら進捗 0.5 / 電力不足で進捗も電力も止まる / 電力 0 の施設は 300 フレーム(float 誤差で 301)で終わる / 完了で電力を引き、`current` と `progress` を空にする / 同じカテゴリを 2 回通すと同じ Item が返り、電力は使う / 段階は最大 6 で止まる / `accepts()` が燃料、credit、処理中、出し待ちを拒む
- `QualityGateTest`: 5 種の設定値と 1×1 / `crafting` 区分で credit 80/30/45/60/80 / ID が byte に収まり総数 127 以下 / 1 個受けたら次を拒む / `source == tile` の出し待ちは `entity.items` に入り、次の入力を拒む / シニアだけ `acceptsPower()` が false / `GateEntity` の保存と読み込み(処理中と空の両方、9 バイト)

## 起動確認

`./gradlew :desktop:run` を約 45 秒動かして終了した。ログは `Got locale: ja, bundle locale: ja`、`Total blocks loaded: 105` のみで、例外もエラーも出ていない。

`./gradlew :html:dist` 成功。出力は `html/build/dist` に残した。

## ブラウザ版で目で確かめること(未確認)

- 配置パネルの crafting タブに 5 種が並び、置くと credit が 80/30/45/60/80 減る
- コンベアで運んだコードが 1 個ずつ入り、処理中は中央にアイコンが出て、緑の進捗バーが伸びる
- 処理後、色とドット(通過状態)が変わったコードが隣のコンベアへ出る
- 同じカテゴリの施設を 2 回通すと、2 回目は色が変わらない(時間と電力は使う)
- 電力の要る 4 種は電力が無いと止まり(進捗バーが止まる)、発電機のレーザーで電力が入ると再開する。シニアは電力バーが無く、電力なしで動く
- 出口が詰まると施設の中に 1 個溜まり、それ以上受けない。詰まりが解けると出る
- 処理中に保存して読み込むと、処理中のコードと進捗が戻る

## 追記: QualityGate と PoweredQualityGate への分割

最初の実装は `QualityGate extends PowerBlock` の 1 クラスで、シニアエンジニアも `PowerAcceptor` になっていた。`Generator.laserTarget()` は最初に当たった `PowerAcceptor` でレーザーを止めるので、シニアを発電機のレーザーの途中に置くと、その先へ電力が届かない(シニアの `acceptsPower()` は false なので、電力はシニアにも入らない)。Task 3.6 の `CodeSource` / `PoweredCodeSource` と同じ方針で、次のように分けた(設計書 §2.2 の「`PowerBlock` を継承」とは異なる)。

- `QualityGate extends Block`(シニアエンジニア用): `PowerAcceptor` を実装しない。`powerPerItem()` は常に 0 を返す。電力バーは無く、`getStats()` に電力の行を出さない。`update = true`、`solid = true`
- `PoweredQualityGate extends QualityGate implements PowerAcceptor`(テストと CI の 4 種): `powerPerItem`、`powerCapacity = 10f`、黄色の電力バー。`acceptsPower()` / `addPower()` / `setPower()` は `PoweredCodeSource` と同じ形。`getStats()` に `Power Capacity` と `Power/item` を出す
- `GateEntity` は両方とも `PowerEntity` を継承したまま。`QualityGate` では `power` は 0 のままで、保存形式(電力 float、ID byte、進捗 float)は共通
- `GateLogic` は変えていない。ブロック ID と登録順も変わらない
- テスト: `QualityGateTest` を「シニアは `PowerAcceptor` でなく、他 4 種は `PoweredQualityGate`(`PowerAcceptor`)」に直し、`addPower()` が容量 10 まで受けて受け入れ量を返すテストを足した。`./gradlew :core:test` 72 件すべて成功
- `./gradlew :html:dist` を作り直し、出力を `html/build/dist` に残した
- `./gradlew :desktop:run` は 2 回とも、ゲームのコードに入る前のウィンドウ作成(`GLFW.glfwGetMonitorPos`、`Lwjgl3ApplicationConfiguration.toLwjgl3Monitor`)で NullPointerException になった。主モニターが取れていない環境側の問題とみられ、分割の前(同じセッション)の起動は成功している
- ブラウザで追加で確かめること: シニアを発電機のレーザーの途中に置いても、後ろの施設へ電力が届く

## 既知の制限と懸念

- 撤去しても処理中のコードと出し待ちは返らない。マルチプレイヤーの同期は考えていない
