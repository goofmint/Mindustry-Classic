# Task 3.11 検証記録: 旧ブロックと旧アイテムの削除

## 削除したもの
- `game/Tutorial.java`、`core/assets/maps/tutorial.png`、`maps.json` の id 12(tutorial)。
  - `Maps` は `IntMap<Map>` に id で入れ、`lastID = max(id)` を新規カスタムマップの採番に使うだけ。tutorial は末尾の id 12 だったので、残り 0〜11 は連番のまま。
- `MenuFragment` のチュートリアルボタン(デスクトップの 4 列目と、モバイルの再生ダイアログ)。デスクトップは 3 列になったので、下段の `colspan(4)` を `colspan(3)` に、モバイルは Join を全幅(`width(bw).colspan(2)`)にした。
- `Control`、`Logic`、`Renderer`、`World`、`InputHandler`、`BlocksFragment`、`HudFragment`、`PausedDialog` のチュートリアル分岐。`TargetType.draw()` は強調表示(`showTarget()`)だけ消し、`EnemyTypes.target` と再出現は残した。`icon-tutorial` のアトラス項目は残した(再パックしない)。
- ブロック 18 個: steelwall、duriumwall、compositewall、duriumwall-large、sorter、smelter、crucible、coalpurifier、titaniumpurifier、oilrefinery、stoneformer、lavasmelter、siliconextractor、stonedrill、irondrill、titaniumdrill、omnidrill、weaponfactory。
- クラス: `Sorter`、`LiquidCrafter`、`Smelter`、`Omnidrill`、`LiquidDrill`、`WeaponFactory`。`Difficulty` の `instanceof Smelter` を外した。
- `Item` の stone、iron、coal、steel、titanium、dirium、uranium、sand。登録は credit 0、igpu 1、pcie 2、コード 3〜72 の 73 件。
- `Save12`〜`Save14`、`BlockLoader.defaultMap`/`blockmap`/`getByOldID()`。`SaveIO.versionArray` は `Save15` だけ。
- bundle: 英語 89 キー(ブロック 33、アイテム 8、チュートリアル 48 = `tutorial.*` 44 + `text.tutorial`、`text.tutorial.back/next`、`map.tutorial.name`)、日本語 3 キー(`text.tutorial`、`item.coal.name`、`item.uranium.name`)。`tools/bundle_exclude_keys.txt` から該当 86 行を消した。

## 置き換え
- `Conveyor.draw()` の `Item.stone` → `Item.igpu`。`NuclearReactor` の既定燃料と `nuclearReactor`/`rtgenerator` → `Item.pcie`、`coalgenerator` → `Item.igpu`。`coaldrill.result` → igpu、`uraniumdrill.result` → pcie。
- 床 `coal` の drops → igpu×1、`uranium` → pcie×1。stone、blackstone、iron、titanium の床と rock、icerock、blackrock の drops は削除(`Block.drops` の既定は null)。
- `Drill.update()`/`isLayer()` は `resource.drops` が null なら床の同一性だけで判定する(null 同士を一致とみなさない)。
- `World.setDefaultBlocks()`: stonedrill 8 か所 → `ProductionBlocks.humanEngineer`、doubleturret 2 か所 → `WeaponBlocks.turret`。`set()` の床は `Blocks.iron`(コード鉱脈)。コンベアの位置、反転、回転は変えていない。
- `BlockLoader.load()` は全ブロックの `init()` の後に `checkRegistrationLimit(Block.getAllBlocks().size)` を呼ぶ。127 を超えると登録数と上限を含む `RuntimeException`(`Item.checkRegistrationLimit` と同じ形)。`blockClasses` のアンカーは `DefenseBlocks.compositewall` → `stonewall`。

## 確認結果
- `git grep -E "Item\.(stone|iron|coal|steel|titanium|dirium|uranium|sand)\b"`: 0 件
- `git grep -E "\b(Tutorial|Sorter|SorterEntity|Smelter|LiquidCrafter|Omnidrill|LiquidDrill|WeaponFactory|Save12|Save13|Save14|getByOldID)\b" -- core desktop html tools README.md`: 0 件
- `git grep -i tutorial -- core/src core/assets/bundles core/assets/maps desktop html`: 0 件(アトラスの `icon-tutorial` は対象外)
- 削除ブロック名(stonedrill 等)を `core/src`、bundle、`tools` で検索: 0 件
- `docs/UPGRADE.md` 86 行目に `Save12` の名前が残る。Phase 1 当時の GWT 検証の記録なので変えていない。
- `python3 -I tools/check_bundles.py`: OK(en 551、除外 67、ja 484)
- `./gradlew :core:test --rerun-tasks`: 成功(96 件、失敗 0)。`BlockLoaderTest` を追加(127 で通る、128 で件数と上限入りの例外、実登録数が上限内)。`ItemTest` の総数を 81 → 73、`CodeItemTest` の非コード一覧、`GateLogicTest`/`QualityGateTest` の `Item.coal` → `Item.igpu` を直した。
- 実ブロック数: **88**(`BlockLoaderTest.realRegistrationIsWithinLimit` が `BlockLoader.load()` と同じクラスを初期化して数えた値)。設計の見積もり 88 と一致。
- `./gradlew :html:dist`: 成功(`html/build/dist` を残してある)。

## 互換性
- 変更前の `Save15` ファイルは非対応。ブロック ID とアイテム ID が詰め直されたため(例: 旧 stone の ID 0 は credit として読まれる)。移行処理は作っていない。Save12〜14 のファイルは `versions.get()` が null になり、`isSaveValid()` は false、`load()` は例外になる(既存の不正セーブと同じ扱い)。

## 未確認(ブラウザ/デスクトップでの手動確認が要る)
- 起動ログの `Total blocks loaded: 88` と、起動時検査が実行時に通ること(テストでは同じ数を確認済み)。
- 新しいゲームで human-engineer 8 個と turret 2 個が置かれ、下の床がコード鉱脈になること。
- 品質ゲートの処理中にセーブし、ロード後に処理中アイテム、進捗、credit 残高が戻ること。
- メニューの 3 列/モバイル再生ダイアログのボタン配置の見た目。
