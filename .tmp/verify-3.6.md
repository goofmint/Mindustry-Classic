# Task 3.6 検証記録(CodeSource、Issue #17)

ブランチ: `issue-17-codesource`(`issue-16-recipes` の上に積んだ)

## 変えたこと

- `CodeSource extends Drill`(新規): コード鉱脈(床が `Blocks.iron` と完全一致)の上で、`time` 秒ごとにコードを 1 個出す。`PowerAcceptor` は実装しない
  - 段階は `stageChoices` から、重要度は `CodeItem.IMPORTANCE`(1, 2)から `Mathf.random` で等確率に選び、`CodeItem.get(stage, 0, importance)` を `offloadNear()` で出す。`Effects.effect(drillEffect, ...)` も出す
  - 生産の条件は順に「鉱脈の上」「`entity.totalItems() < capacity`(Drill の既定 5)」「`canProduce(entity)`(基底は常に true)」。全部満たしたときだけ `timer.get(timerDrill, 60 * time)` を評価するので、条件を満たさないフレームはタイマーをリセットしない
  - `tryDump()` は Drill と同じく `timerDump` 30 フレームごとに常に呼ぶ
  - `isVein(Tile)` を `update()` と `isLayer()` で共有。鉱脈の外では `isLayer()` が true になり、Drill の `drawLayer()` が `cross` を描く
  - `canReplace(other)` は `other instanceof CodeSource`(上位ブロックへ同じ場所で置き替え)。費用の返金や中身の引き継ぎはしない
  - `resource` と `result` は null にし、使わない
- `PoweredCodeSource extends CodeSource implements PowerAcceptor`(新規): AI エージェント用
  - `powerPerItem`、`powerCapacity = 10f`。`getEntity()` は `PowerBlock.PowerEntity`(電力の保存と読み込みはその `write/read`)
  - `canProduce()` は `power >= powerPerItem`、`onProduced()` で `powerPerItem` を引く
  - `acceptsPower()` / `addPower()` / `setPower()` は `PowerTurret` と同じ(`addPower()` は受け取った量を返す)
  - 黄色の電力バーを `bars` に足した。`getStats()` に `Power Capacity` と `Power/item` を足した(`text.blocks.powercapacity` / `poweritem` は両言語に既にある。`Seconds/item` と `Capacity` は Drill の分がそのまま出る)
- 設計書 §2.2 との差: 設計書は `CodeSource` 1 クラスが `PowerAcceptor` を実装する案。`Generator.laserTarget()` は最初に当たった `PowerAcceptor` でレーザーを止めるので、電力を使わない人間のエンジニアが `PowerAcceptor` だとその先へ電力が届かない。そのため 2 クラスに分けた(Javadoc にも書いた)
- `ProductionBlocks.java` の末尾(`weaponFactory` の後)に 4 種を足した。既存ブロックの ID はずれない。ブロック総数は 96 から 100。`health` は既存のドリルと同じく既定値(40)のまま

| フィールド | 登録名 | クラス | stageChoices | time | powerPerItem | 費用(credit) |
| --- | --- | --- | --- | --- | --- | --- |
| humanEngineer | human-engineer | CodeSource | {2,3} | 6 | - | 50 |
| aiAgent1 | ai-agent-1 | PoweredCodeSource | {0,1} | 2 | 0.3 | 20 |
| aiAgent2 | ai-agent-2 | PoweredCodeSource | {1,3} | 2 | 0.4 | 30 |
| aiAgent3 | ai-agent-3 | PoweredCodeSource | {3} | 2 | 0.5 | 40 |

- `Recipes.java`: `production` 区分の `uraniumdrill` の後に、人間 → AI-1 → AI-2 → AI-3 の順で足した。レシピは 47 件から 51 件
- ブロック画像: `core/assets-raw/sprites/blocks/` に 8×8 の仮絵 `human-engineer.png`(橙)、`ai-agent-1.png`(水色)、`ai-agent-2.png`(青)、`ai-agent-3.png`(紫)。Python 標準ライブラリで作った。`./gradlew :core:pack` で `sprites.atlas` / `sprites.png` を作り直した
- 文言: 両言語の `uraniumdrill` の後に `block.<name>.name` と `.description` を 4 種分足した。`python3 -I tools/check_bundles.py` → OK(ja 470 キー)
- 美咲フォント: 説明文の「良」が新しい字だったので、`tools/README.md` の手順で `chars_ja.txt` と `core/assets/ui/misaki.fnt` / `misaki.png` を作り直した(`check_fnt.py` で検査済み)

## テスト

`./gradlew :core:test`: 55 件すべて成功(既存 48 件 + `CodeSourceTest` 7 件)。`RecipesTest` のレシピ数を 51 に直した。

`core/src/test/java/io/anuke/mindustry/world/blocks/types/production/CodeSourceTest.java`(protected な設定値を読むため `CodeSource` と同じパッケージに置いた):

- 4 種の登録名、`stageChoices`、`time`、`powerPerItem` が設計書の表どおり
- 人間は `PowerAcceptor` でなく、AI 3 種は `PowerAcceptor`
- 全 `stageChoices` × 重要度 1, 2 の `CodeItem.get(stage, 0, importance)` が登録済みのコード
- 4 種のレシピが `production` 区分で、credit 50/20/30/40 の 1 項目
- 4 種が互いに `canReplace()` でき、`coaldrill` は置き替えられない
- ブロック ID が `Tile` の byte に収まる(`Block.getByID((byte)id)` で自分に戻る)
- `Blocks.iron` の床は鉱脈(`isLayer()` false)、`Blocks.stone` の床は鉱脈でない(`isLayer()` true)

## 起動確認

`./gradlew :desktop:run` を約 45 秒動かして終了した。ログは `Got locale: ja, bundle locale: ja`、`Total blocks loaded: 100` のみで、例外もエラーも出ていない。

`./gradlew :html:dist` 成功。出力は `html/build/dist` に残した。

## ブラウザ版で目で確かめること(未確認)

- 鉄鉱石の床(コード鉱脈)に置くとコードが出て、コンベアで運べる
- 鉱脈の外に置くと `cross` が点滅し、何も出ない
- AI エージェントは電力が無いと止まり、発電機のレーザーで電力が入ると動く。電力バーが出る
- 人間のエンジニアが発電機のレーザーを止めない(後ろの AI エージェントへ電力が届く)
- 置くと credit が 50/20/30/40 減る
- 置いてある CodeSource の上に別の CodeSource を置き替えられる

## 既知の制限

- 電力切れや満杯で止まっている間もタイマーの基準時刻は進む(`Timer.get()` は前回発火からの経過時間で判定する)。そのため、止まっていた時間が `time` 秒を超えていれば、条件が戻った最初のフレームですぐ 1 個出る。進捗を凍結する作りにはしていない
- マルチプレイヤーの同期は考えていない(生成するコードは各クライアントの乱数で決まる)
- 置き替え時に元のブロックの費用は返らず、溜まっていたコードと電力も引き継がない
- 本家の `Omnidrill.canReplace()` は `other instanceof Drill` なので、理屈の上では omnidrill で CodeSource を置き替えられる(omnidrill はレシピから外してあるので実際には置けない)
