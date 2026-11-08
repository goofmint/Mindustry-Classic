# Task 3.5 検証記録(Recipes の差し替え、Issue #16)

ブランチ: `issue-16-recipes`(`issue-15-score` の上に積んだ)

## 変えたこと

- `Recipes.java`: レシピを 63 件から 47 件に減らした。残した全レシピの費用を `stack(Item.credit, n)` の 1 つだけにした(n は要件定義の費用表の値)。登録順と `Section` は元のまま。`door` と `largedoor` の `setDesktop()` も残した
  - 外したレシピ(16 件): `steelwall`、`diriumwall`(ブロック ID は `duriumwall`)、`diriumwalllarge`、`sorter`、`smelter`、`crucible`、`coalpurifier`、`titaniumpurifier`、`oilrefinery`、`stoneformer`、`lavasmelter`、`weaponFactory`、`stonedrill`、`irondrill`、`titaniumdrill`、`omnidrill`
  - この結果、`crafting` 区分のレシピは 0 件になった。ブロック選択の UI には空のタブが出る。Task 3.6〜3.8 で施設を足すと埋まる
- `UpgradeRecipes.java`: 武器 5 種の項目を外し、空の `ObjectMap` にした。`get()` は空配列を返す。クラスと公開メソッドは残した
- `WeaponBlocks.java`: 弾薬で撃っていた砲台 10 種を、`teslaturret` と同じ匿名の `PowerTurret` サブクラスに変えた。`ammo` と `ammoMultiplier` を外し、`powerUsed` を足した。ほかの値、`doubleturret` と `chainturret` の `shoot()` 上書き、登録名(`waveturret`、`titancannon`)、並び順は変えていない。`teslaturret` は既定値 0.5 を明示した。`laserturret` は変えていない
  - `powerCapacity` は既定値 20 のまま。数値の調整は Task 5.3

## powerUsed(1 発あたりの電力)

| フィールド | 登録名 | powerUsed |
| --- | --- | --- |
| turret | turret | 0.1 |
| doubleturret | doubleturret | 0.1 |
| machineturret | machineturret | 0.1 |
| shotgunturret | shotgunturret | 0.3 |
| flameturret | flameturret | 0.08 |
| sniperturret | sniperturret | 0.6 |
| mortarturret | mortarturret | 0.8 |
| laserturret | laserturret | 0.2(変更なし、LaserTurret) |
| teslaturret | waveturret | 0.5(既定値を明示) |
| plasmaturret | plasmaturret | 0.1 |
| chainturret | chainturret | 0.25 |
| titanturret | titancannon | 1.5 |

`PowerTurret.acceptItem()` は常に false なので、砲台はアイテムを受け取らない。`Turret.getStats()` と `Turret.update()` は `ammo != null` で守られているので、`ammo == null` でも動く。

## テスト

`./gradlew :core:compileJava :core:test`: 48 件すべて成功(既存 44 件 + `RecipesTest` 4 件)

`core/src/test/java/io/anuke/mindustry/resource/RecipesTest.java`:

- レシピが 47 件
- 全レシピの費用が 1 項目だけで、アイテムが `Item.credit`、個数が正
- 同じ結果ブロックが 2 回出てこない
- `UpgradeRecipes` が空で、`get()` が空配列を返す

ブロックと武器のコンストラクタは名前を `Core.bundle` から引く。`Core.bundle` が null だと NullPointerException、キーが無いと MissingResourceException で静的初期化が落ちる。そのため `@BeforeClass` で `I18NBundle.setExceptionOnMissingKey(false)`(ゲーム本体の `BundleLoader` と同じ)をしてから `assets/bundles/bundle` を読み込んだ。グラフィックスには触らない。

## 起動確認

`./gradlew :desktop:run` を約 45 秒動かして終了した。ログは `Got locale: ja, bundle locale: ja`、`Total blocks loaded: 96` のみで、例外もエラーも出ていない(`BlockLoader` と `Recipes` の静的初期化は通る)。

`./gradlew :html:dist` 成功。出力は `html/build/dist` に残した。画面での確認(残高で置けて減ること、砲台が電力で撃つこと)はブラウザ版で行う。

## 既知の状態(このタスクでは直さない)

- チュートリアルと `World.setDefaultBlocks()` は、まだ `stonedrill` と `doubleturret` を置く(Task 3.11 で直す)。初期配置の `doubleturret` は電力が無いと撃たない
- 古いセーブにある `WeaponFactory` は、`UpgradeRecipes` が空なので武器を無料で渡す
- 古いセーブの砲台の弾薬は無視される。`TurretEntity` は `int` の弾薬、`PowerTurretEntity` は `float` の電力を書く。どちらも 4 バイトなのでずれは起きないが、弾薬の値のビット列がそのまま電力として読まれる(例: 弾薬 400 はほぼ 0 の電力になる)
- `PowerTurret.getStats()` は `Power/shot` を小数 1 桁で出すため、`flameturret` の 0.08 は `0.1` と表示される
- `WeaponBlocks.java` の `Item` と `Turret` の import は使われなくなったが、最小変更の方針で残した
