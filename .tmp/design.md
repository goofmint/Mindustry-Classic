# 詳細設計書 - AI SDLCゲーム(Mindustry Classic改造)

2026-11-08 作成。`.tmp/requirements.md` を入力とし、土台コード(Anuken/Mindustry-Classic)を読んで確認した事実にもとづいて書く。コードを読んで確かめた箇所は「確認済み」、まだ動かして確かめていない箇所は「未検証」と書き分ける。

---

## 1. アーキテクチャ概要

### 1.1 システム構成図

本家のモジュール構成(`Mindustry.init()` で `Logic`、`World`、`Control`、`Renderer`、`UI`、`Net*` を登録。確認済み)は変えない。改造は「データ定義の差し替え」と「少数のロジック差し込み」に閉じる。

```
┌─────────────────────────────────────────────────────────────────────┐
│ html (GWT)  HtmlLauncher → Mindustry(ModuleCore)                     │
│   変更なし(表示タイトルとロゴのみ差し替え)                             │
├─────────────────────────────────────────────────────────────────────┤
│ core                                                                 │
│                                                                      │
│  [データ定義層]  ← 今回の改造の中心                                   │
│   resource/Item.java          … 8種 → コード70種+燃料2種(iGPU,PCIe)   │
│   resource/CodeItem.java (新)  … 品質/通過状態/重要度 ⇄ Item の対応表   │
│   resource/Recipes.java        … 建設レシピを全面差し替え(費用=得点)   │
│   world/blocks/*Blocks.java    … 使わないブロックを削除、新ブロック追加 │
│   io/BlockLoader.java          … 旧ID表(セーブ互換)を新ブロック表に更新 │
│   entities/enemies/EnemyTypes  … target型を除外                       │
│   assets/bundles/bundle*.properties … en + ja                        │
│   assets-raw/sprites/**        … 新アイコン(生成)、再パック             │
│   assets/maps/*.png, maps.json … 256×256 を3枚                        │
│                                                                      │
│  [ロジック差し込み層]                                                 │
│   world/blocks/types/production/CodeSource.java (新)  … Drill派生     │
│   world/blocks/types/production/QualityGate.java (新) … 品質変換施設   │
│   world/blocks/types/defense/CodeRabbitBlock.java (新)… 変換+迎撃     │
│   world/blocks/types/defense/CoreBlock.java   … 納品 → 残高加算        │
│   game/Score.java (新)         … 残高・目標・勝敗判定・敵倍率           │
│   game/EnemySpawn.java         … evaluate() に品質倍率を掛ける          │
│   core/Logic.java              … 勝利/残高負けの判定を追加              │
│   ui/dialogs/RestartDialog     … 勝ち/負けの文言出し分け               │
│   ui/fragments/BlocksFragment  … 資源欄を「残高」1行表示に変更          │
│                                                                      │
│  [そのまま使う層]                                                     │
│   Conveyor / Router / Junction / Tunnel / Teleporter                  │
│   Turret / PowerTurret / RepairTurret / ShieldBlock / Wall / Door     │
│   Generator 系 / Pump / Conduit / Pathfind / Renderer / Save15        │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 技術スタック

本家の構成(確認済み)と、今回の更新先を並べる。更新先は 2026-11-08 の検討時点の判断で、Phase 1 の作業で確定する。

| 項目 | 本家(確認済み) | 更新先 | 更新先の根拠 |
| --- | --- | --- | --- |
| JDK | Java 8(`sourceCompatibility = 1.8`) | **Java 17**(LTS) | libGDX 1.13 以降は Java 17 でビルドされる。GWT 2.10 以降は Java 17 上での実行を試験済み |
| ゲーム基盤 | libGDX 1.9.8、gdx-ai 1.8.1 | **libGDX 1.13.1**、gdx-ai 1.8.2 | 1.13.0 は GWT の SpriteBatch に不具合があり 1.13.1 で修正。1.14.x は Phase 1 で試して問題なければ採用 |
| ブラウザ変換 | GWT 2.8.0、gwt-gradle-plugin 0.6(de.richsource) | **GWT 2.11.0**、gdx-liftoff が使う GWT Gradle プラグイン | libGDX 1.13 の GWT バックエンドが GWT 2.11.0 前提。`jsinterop-annotations:2.0.2:sources` の追加が要る |
| ビルド | Gradle 4.10.2 | **Gradle 8.14** | gdx-liftoff 1.14.0.2 の既定。Gradle 9 系は GWT プラグインの対応が追いついていない報告あり |
| 画像パック | gdx-tools TexturePacker。`gradle pack` | 同じ(gdx-tools を 1.13.1 に揃える) | `core/build.gradle` |
| 多言語 | libGDX `I18NBundle` | 同じ | `io/BundleLoader.java` |
| 配布物 | `html/build/dist`(`gradle :html:dist`) | 同じ | `html/build.gradle` |

更新に伴って削るモジュール: `android`、`ios`、`kryonet`、`server`。`settings.gradle` は `core`、`desktop`、`html` の3つにする。ネットワーク対戦は要件外で、HTML 版は本家でも非対応(確認済み)なので、`desktop` の `KryoClient/KryoServer`、`html` の `WebsocketClient`、`gwt-websockets` 依存を外し、`Net` のプロバイダは未設定のままにする(`Net.active()` が false を返すだけで、他のコードは動く。`Net.java` 確認済み)。ゲームパッド対応(`Inputs.useControllers`、`XboxBinds`)は gdx-controllers 2.x で API が変わるため、Phase 1 で切る。

ビルドは未検証。作業環境から Gradle/Maven のサーバーに届かなかった(要件定義の記載どおり)。更新作業は時間を区切った試行(スパイク)として進め、壊れた箇所を記録してから本修正に入る(§10、タスクリスト Phase 1)。

日本語表示のフォント(確認済み): UI スキンは `square.fnt`(300 字)と `korean.fnt`(2447 字、BMFont 形式)を持つが、`korean` はフォントのスケール調整に名前が出るだけで、言語によって切り替える処理は無い。GWT では FreeType が使えないため、日本語も `.fnt` + `.png`(AngelCode 形式)で用意する。フォントは既存の 8×8 ドット日本語フォント「美咲フォント」(littlelimit.net、2021-05-05 版。利用・改変・再配布が無制限、表記義務なし。確認済み)を使い、配布物(TTF または BDF)から Hiero か小さな変換スクリプトで `.fnt` 形式に変換するだけで、文字を作る作業は無い。`UI.loadSkin()` でロケールが `ja` のとき `default-font` をこのフォントに差し替える。`Draw.text()` は `Core.font` を使う(確認済み)ので、こちらも同じフォントを指す。

---

## 2. コンポーネント設計

### 2.1 コンポーネント一覧

| コンポーネント | 責務 | 依存 | 新規/変更 |
| --- | --- | --- | --- |
| `Item` | アイテム定義。コード70種と燃料2種を静的に生成 | `Bundles`、`Draw` | 変更 |
| `CodeItem` | コードアイテムの属性(品質段階、通過状態、重要度)と `Item` の相互変換 | `Item` | 新規 |
| `CodeSource` | コードを生む設備(人間のエンジニア、AIエージェント3段階)。鉱脈の上でだけ動く | `Drill`、`CodeItem`、`PowerAcceptor` | 新規 |
| `QualityGate` | 品質を上げる施設(シニア、ユニットテスト、E2E、Jenkins、GitHub Actions)。1個受けて変換して出す | `PowerBlock`、`CodeItem` | 新規 |
| `CodeRabbitBlock` | レビュー変換と迎撃を1ブロックで行う | `PowerTurret`、`QualityGate` の変換ロジック | 新規 |
| `CoreBlock` | 納品受付。得点を `Score` に渡す | `Score`、`CodeItem` | 変更 |
| `Score` | 残高、目標、勝敗判定、品質倍率の保持と計算 | `GameState` | 新規 |
| `Recipes` | 建設費(得点)の表 | `Item.score`、各 `*Blocks` | 変更 |
| `EnemySpawn` | 出現数に品質倍率を掛ける | `Score` | 変更 |
| `Logic` | 勝利と残高負けの判定、イベント発火 | `Score`、`EventType` | 変更 |
| `BlockLoader` | ブロック名⇄旧ID表を新構成に合わせる | 各 `*Blocks` | 変更 |
| `RestartDialog` / `HudFragment` / `BlocksFragment` | 勝敗表示、残高表示 | `Score`、`Bundles` | 変更 |
| `IconGenerator`(ツール) | 14枚の基本画像にドットを重ねて70枚のアイコンPNGを作る。ビルド前に1回実行 | Java標準の `javax.imageio` のみ | 新規(desktop側ツール) |
| `bundle.properties` / `bundle_ja.properties` | 英語と日本語の文言 | なし | 変更/新規 |
| マップ3枚 | 256×256 PNG。コード鉱脈(鉄鉱石色 `c3a490`)、iGPU在庫(石炭色 `161616`)、PCIe在庫(ウラン色 `83bc58`) | `ColorMapper` | 新規 |

### 2.2 各コンポーネントの詳細

#### Item(変更)

- **目的**: コードと燃料を、本家と同じ「種類IDだけのアイテム」として表す。
- **確認済みの制約**:
  - `TileEntity.items` は `int[Item.getAllItems().size]`。アイテム数が増えると、全ブロックの entity が持つ配列が同じ分だけ大きくなる。72種なら `int[72]`。256×256 マップで設備が数千個あっても数MB以下。
  - `Conveyor.ItemPos.toInt()` はアイテムIDを `(byte)itemid` で保存し、`toLong()` で `byte itemid` として読む。符号付きなので **127 まで**しか安全でない。72種なら問題ない。
  - `Save15` はインベントリと entity のアイテムIDを `writeByte` で書く。同じく127まで。
  - `Block.tryDump()` は `for(Item item : Item.getAllItems())` で全種類を走査する。72種への増加は許容範囲だが、ルーター(PR)が多いと走査コストが増える。§8 に記載。
- **定義方針**: 静的フィールドの列挙ではなく、`static` 初期化子でループ生成する。ID順は「燃料2種 → コード70種」とし、燃料のIDを0と1に固定する(アイコン名の衝突を避け、デバッグ時に見分けやすい)。

```java
public class Item{
    private static final Array<Item> items = new Array<>();

    public static final Item igpu = new Item("igpu");   // id 0
    public static final Item pcie = new Item("pcie");   // id 1
    // id 2 以降は CodeItem.registerAll() が生成する

    public final int id;
    public final String name;
    public TextureRegion region;
    // 既存フィールド(explosiveness, flammability)は残す。使わないが、参照箇所があるため
}
```

- **削除するもの**: `stone`、`iron`、`coal`、`steel`、`titanium`、`dirium`、`uranium`、`sand` の静的フィールド。参照箇所は確認済みで10ファイル(`Inventory`、`Tutorial`、`Recipes`、`UpgradeRecipes`、`Blocks`、`ProductionBlocks`、`Conveyor`、`Sorter`、`NuclearReactor`、`WeaponBlocks`)。`Conveyor.draw()` の `acceptItem(Item.stone, ...)` はアニメーション判定用なので、`Item.igpu` に置き換える(アイテム種を見ていないため挙動は変わらない)。

#### CodeItem(新規)

- **目的**: 品質7段階 × 通過状態8通り × 重要度2種類のうち、実際に作られる **70パターン** だけを `Item` として登録し、属性 ⇄ `Item` を O(1) で引く。
- **70パターンの根拠**: 出発点の品質は -5、-3、-2、-1 の4段階(通過状態は全未)。各施設を通るごとに段階が上がり、通過状態のビットが立つ。到達可能な(品質, 通過状態)の組を列挙すると 35 通りで、重要度2種類を掛けて 70。列挙は `registerAll()` で、出発点からの到達探索で機械的に行う(手書きの表にしない)。
- **公開インターフェース**:

```java
public final class CodeItem{
    /** 品質の段階。配列の添字が段階で、値が得点に使う品質値 */
    public static final float[] QUALITY = {-5f, -3f, -2f, -1f, 0.5f, 0.8f, 1f};
    public static final int MAX_STAGE = QUALITY.length - 1;

    /** 通過状態のビット */
    public static final int REVIEW = 1, TEST = 2, CI = 4;

    /** 重要度。値がそのまま倍率 */
    public static final int[] IMPORTANCE = {1, 2};

    /** 到達可能な組み合わせだけを Item として登録する。Item の静的初期化の直後に1回呼ぶ */
    public static void registerAll();

    /** 属性から Item を引く。登録されていない組み合わせなら例外 */
    public static Item get(int stage, int passed, int importance);

    /** Item からの逆引き。コードでないアイテム(燃料)なら null */
    public static CodeAttr attrOf(Item item);

    /** 得点 = 品質 × 重要度 */
    public static float score(Item item);

    /** 施設を通したあとの Item。passedBit が既に立っていれば同じ Item を返す(品質は変えない) */
    public static Item advance(Item item, int passedBit, int stages);

    public static final class CodeAttr{
        public final int stage, passed, importance;
    }
}
```

- **Item の命名規則**: `code-q<stage>-p<passed>-i<importance>`。例: `code-q0-p0-i1`(品質-5、未通過、重要度1)。アイコン領域名は `icon-` + この名前(`Item.init()` が `icon-<name>` を引く。確認済み)。
- **`advance()` の仕様**: `passedBit` が既に立っている場合は入力をそのまま返す。立っていない場合、`stage + stages` を `MAX_STAGE` で上限クリップし、`passed | passedBit` を立てた Item を返す。この結果は `registerAll()` の到達探索で必ず登録済みになる(同じ規則で探索するため)。

#### CodeSource(新規、`Drill` を継承)

- **目的**: コード鉱脈(床 `Blocks.iron`、そのまま流用)の上で、一定時間ごとにコードを1個出す。
- **本家 `Drill` との差(確認済みの本家仕様)**: `Drill.update()` は `tile.floor() == resource` のとき `timerDrill` 周期で `offloadNear(tile, result)` する。`result` が固定の1種なので、`CodeSource` は `update()` を上書きし、出すたびに `result` をランダムに決める。
- **トークン消費**: AIエージェントだけ `PowerAcceptor` を実装し、1個出すごとに `powerPerItem` を消費する。足りなければ出さない。人間のエンジニアは消費しない。実装は `PowerBlock` と同じ `PowerEntity` を使う(`Generator.laserTarget()` が `instanceof PowerAcceptor` で配電先を探す。確認済み)。
- **設定値**:

```java
public class CodeSource extends Drill implements PowerAcceptor{
    protected int[] stageChoices;      // 出発点の品質段階の候補。例: 人間 {2,3}(-2,-1)
    protected float powerPerItem = 0f; // 0 なら電力不要(人間)
    protected float powerCapacity = 10f;
    // time(秒/個)、capacity は Drill のものを使う
}
```

| ブロック名 | 表示名(ja) | stageChoices | time(秒/個) | powerPerItem | 費用 |
| --- | --- | --- | --- | --- | --- |
| `human-engineer` | 人間のエンジニア | {2,3} = -2,-1 | 6 | 0 | 50 |
| `ai-agent-1` | AIコーディングエージェント | {0,1} = -5,-3 | 2 | 0.3 | 20 |
| `ai-agent-2` | 同(中間) | {1,3} = -3,-1 | 2 | 0.4 | 30 |
| `ai-agent-3` | 同(最上位) | {3} = -1 | 2 | 0.5 | 40 |

重要度は毎回 1 か 2 を等確率で決める。time と powerPerItem は仮の値で、遊びながら調整する(要件定義の「費用は未決定」に対応)。「3段階に強化」は、上位ブロックを同じ場所に置き替えられる形で実現する(`canReplace()` で `other instanceof CodeSource` を返す。本家 `Turret.canReplace()` と同じ手法。確認済み)。

#### QualityGate(新規、`PowerBlock` を継承)

- **目的**: コードを1個受け取り、処理時間のあと、品質を上げて隣に出す。
- **本家の参考実装**: `Smelter`(確認済み)。入力を `entity.items` に溜め、`timerCraft` 周期で1個消費して `offloadNear(tile, result)` する。`QualityGate` との違いは、入力が70種、出力が入力に依存する点。
- **内部状態**: `GateEntity extends PowerEntity` に `Item current`(処理中のコード、null なら空)と `float progress` を持つ。`entity.items` は使わない(70種の配列を走査する必要をなくすため)。`write/read` で `current` の ID と `progress` を保存する(`Save15` が `tile.entity.write(stream)` を呼ぶ。確認済み)。
- **受け入れ条件**: `acceptItem()` は「`CodeItem.attrOf(item) != null` かつ `current == null`」のとき true。燃料は受けない。
- **処理**: `update()` で、`current != null` かつ(電力不要 or `power >= powerPerItem`)なら `progress += delta / processTime`。`progress >= 1` で `powerPerItem` を引き、`CodeItem.advance(current, passedBit, stages)` を `offloadNear()` で出し、`current = null`。出せない(隣が満杯)なら `offloadNear()` の仕様どおり自分の `items` に入るので、そのあと `tryDump()` で吐き出す。
- **同じカテゴリの再通過**: `acceptItem()` では弾かない。処理時間とトークンは使い、`advance()` が同じ Item を返す(要件定義どおり)。
- **設定値**:

| ブロック名 | 表示名(ja) | passedBit | stages | processTime(秒) | powerPerItem | 費用 |
| --- | --- | --- | --- | --- | --- | --- |
| `senior-engineer` | シニアエンジニア | REVIEW | 1 | 5 | 0 | 80 |
| `unit-test` | ユニットテスト | TEST | 1 | 2 | 0.3 | 30 |
| `e2e-test` | E2Eテスト | TEST | 1 | 4 | 0.5 | 45 |
| `jenkins` | Jenkins | CI | 1 | 3 | 0.4 | 60 |
| `github-actions` | GitHub Actions | CI | 1 | 2 | 0.4 | 80 |

- **表示**: `bars` に処理進捗(`progress`)のバーを足す。`draw()` で処理中は `current.region` をブロック中央に小さく描く(`Sorter.draw()` の手法。確認済み)。これで「いま何を処理しているか」が見える。

#### CodeRabbitBlock(新規、`PowerTurret` を継承)

- **目的**: 1ブロックでレビュー変換(2段階上、REVIEW ビット)と迎撃を行う。
- **構成**: `PowerTurret` を継承して迎撃を得る。変換ロジックは `QualityGate` と重複させず、`QualityGate` の変換部分を静的ヘルパー `GateLogic.update(tile, entity, ...)` に切り出して両方から呼ぶ。`PowerTurretEntity` を継承した `CodeRabbitEntity` に `current`、`progress` を足す。
- **電力の共有**: 迎撃の `powerUsed`(1発分)と変換の `powerPerItem` は、同じ `entity.power` から引く。どちらかが枯れると止まる。「トークンを食うが強い」という体感を狙う。
- **`acceptItem()`**: `PowerTurret.acceptItem()` は常に false(確認済み)。上書きして `QualityGate` と同じ条件にする。
- **設定値**: 本家 `laserturret` の性能(射程60、リロード4、ダメージ10、`powerUsed` 0.2。確認済み)を借り、`LaserTurret` ではなく `PowerTurret` + `BulletType` で撃つ。processTime 3秒、stages 2、powerPerItem 0.6。費用 100。
- **見た目**: 砲塔画像は本家 `laserturret.png` を流用し、色を変えた `coderabbit.png` を用意する(ブロック画像は `Turret.draw()` が `"block"` を、`drawLayer()` が `name()` を描く。確認済み)。

#### CoreBlock(変更)

- **確認済みの本家仕様**: `handleItem()` で `state.inventory.addItem(item, 1)`。`acceptItem()` は常に true。
- **変更**: `handleItem()` を次に置き換える。

```java
@Override
public void handleItem(Item item, Tile tile, Tile source){
    if(Net.client() && Net.active()) return;
    float score = CodeItem.score(item);   // 燃料なら 0 を返す仕様にする
    state.score.deliver(item, score);
    Effects.effect(score >= 0 ? Fx.deliverGood : Fx.deliverBad, tile.drawx(), tile.drawy());
}
```

燃料(iGPU、PCIe)をコアに流しても得点 0 で消える。「燃料を捨てる」という使い道は想定しないが、詰まりを避けるため受けはする。

#### Score(新規、`GameState` に `public final Score score = new Score()` として持つ)

- **目的**: 残高、目標、勝敗、敵倍率を1か所にまとめる。
- **本家の `Inventory` との関係**: 建設費のチェックと消費は `InputHandler.tryPlaceBlock()`、`Placement.validPlace()`、`Placement.breakBlock()`、`BlocksFragment` が `state.inventory.hasItems(recipe.requirements)` と `removeItem(stack)` で行う(確認済み、4か所)。これらを書き換えるより、**残高を `Inventory` の1アイテム分として表現する**ほうが変更が少ない。そこで `Item.igpu` の ID 0 と別に、得点専用の擬似アイテム `Item.credit`(ID 0、アイコンは得点マーク)を置き、`Recipes` の要件はすべて `stack(Item.credit, 費用)` とする。`Score` は `state.inventory.getAmount(Item.credit)` を残高として読み書きする薄いラッパーにする。

  この決定で、ID順は「credit(0) → igpu(1) → pcie(2) → コード70種(3〜72)」の73種になる。

- **公開インターフェース**:

```java
public class Score{
    public int target;                 // 目標得点。マップごとに持てるよう Map に targetScore フィールドを足す
    public float qualityMultiplier;    // 敵の出現数に掛ける倍率。1.0 が基準
    private float recentQualitySum;    // 直近の納品の品質の移動平均に使う
    private int recentCount;

    public int balance();                        // 残高
    public void deliver(Item item, float score); // 残高に加算(負もあり)、倍率を更新、イベント発火
    public boolean reachedTarget();              // balance() >= target
    public boolean bankrupt();                   // 納品で 0 を下回った(建設では下回れない)
    public void reset(int initial);              // 初期資金 150 をセット
}
```

- **残高の負値**: `Inventory.removeItem()` は負なら 0 に丸める(確認済み)。納品での減算は `Score.deliver()` が `items[credit.id] += (int)score` を直接扱い、0 を下回ったら `bankrupt = true` を立ててから 0 に丸める。「納品で残高が0を下回ると負け」はこのフラグで判定する。
- **得点の整数化**: 品質 0.5 や 0.8 は 1個では整数にならない。`deliver()` 内で `float` の端数を `fraction` に溜め、1.0 を超えた分だけ整数で残高に反映する(切り捨てで損をしないようにする)。
- **敵倍率の式(初期値、調整前提)**: 直近 20 個の納品の平均品質 `q`(範囲 -5〜1)から、`multiplier = clamp(1.0 - q * 0.3, 0.7, 2.5)`。q = 1 なら 0.7、q = 0 なら 1.0、q = -5 なら 2.5。納品が無いときは 1.0。

#### Recipes(変更)

- 要件定義の費用表をそのまま `stack(Item.credit, n)` で記述する。削るレシピは本家の `steelwall`、`diriumwall`、`diriumwalllarge`、`sorter`、`smelter`、`crucible`、`coalpurifier`、`titaniumpurifier`、`oilrefinery`、`stoneformer`、`lavasmelter`、`weaponFactory`、`stonedrill`、`irondrill`、`titaniumdrill`、`omnidrill`。
- `Section` の分類は本家の6つ(weapon、production、distribution、power、defense、crafting)をそのまま使う。`crafting` には品質を上げる施設を入れる。アイコンは `icon-<section>` が UI スキンにあり(確認済み)、画像の差し替えは任意。

#### EnemySpawn(変更)

- **確認済みの本家仕様**: `evaluate(wave, lane)` が出現数を返す。`Logic.runWave()` がその回数だけ `Enemy` を生成する。
- **変更**: `evaluate()` の戻り値に `state.score.qualityMultiplier` を掛けて `Math.round()` する。どの敵が増えるかは、既に本家が種類ごとに `EnemySpawn` を持つため、全種類に同じ倍率が掛かる。要件の「どの敵が増えるかはランダム」は、倍率を掛けたあとの端数を確率で切り上げる(`Mathf.chance(frac)`)ことで実現する。
- `WaveCreator` から `EnemyTypes.target` は元々使われていない(確認済み)。`WorldGenerator` が紫色の床で `target` を置く処理(確認済み)はマップに紫を使わなければ動かないので、コードは触らない。

#### Logic(変更)

- **確認済みの本家仕様**: `update()` でコアのブロックが `ProductionBlocks.core` でなくなったら `gameOver = true` にして `GameOverEvent` を発火。勝利判定は無い。
- **変更**: 同じ場所に2つの判定を足す。

```java
if(!state.gameOver && state.score.reachedTarget()){
    state.gameOver = true; state.won = true;
    Events.fire(GameOverEvent.class);
}
if(!state.gameOver && state.score.bankrupt()){
    state.gameOver = true; state.won = false;
    Events.fire(GameOverEvent.class);
}
```

`GameState` に `public boolean won` を足す。`Control` の `GameOverEvent` ハンドラ(コア爆発の演出。確認済み)は、`won` のとき爆発を出さず、別の演出(`Fx.victory`)に分岐する。

#### RestartDialog(変更)

- **確認済みの本家仕様**: 「The core was destroyed.」と「You lasted until wave N」を表示。
- **変更**: `shown()` 内で `state.won` を見て、タイトルを `$text.victory` / `$text.gameover`、本文を「納品した得点」「到達ウェーブ」に切り替える。ハイスコアの保存は `Control` の `WaveEvent` ハンドラが `hiscore<map>` に wave を保存している(確認済み)。これは残し、加えて `bestscore<map>` に最高残高を保存する。

#### BlocksFragment / HudFragment(変更)

- **確認済みの本家仕様**: `BlocksFragment.updateItems()` が `state.inventory` の全アイテムを2列で並べる。
- **変更**: `updateItems()` を「credit だけを1行で表示」に変える。燃料とコードはインベントリに入らない(コアが得点に変えるため)ので、残高表示だけで足りる。`HudFragment.addWaveTable()` の下に `目標 {target} / 残高 {balance}` のラベルを1行足す。

#### BlockLoader(変更)

- **確認済みの本家仕様**: `defaultMap` がブロック名→旧IDの表を持ち、`load()` で `Block.getAllBlocks()` を初期化する。`blockClasses` の配列は各 `*Blocks` クラスの静的初期化を走らせるためのもの。
- **変更**: 新ブロックの定義クラスを `blockClasses` に足す。`defaultMap` は `getByOldID()` 経由で `Save12〜14` の読み込みにだけ使われる(確認済み)。旧セーブ互換は不要なので、`Save12〜14` と `defaultMap` を削り、`SaveIO.versionArray` を `Save15` だけにする。
- **ブロックIDの上限(確認済み)**: `Tile` は床とブロックのIDを `byte` に詰めて `short` に保存し、`Block.getByID(getWallID())` で `byte` を `int` に戻す。128 以上は負になって `Array.get()` が落ちる。**上限は 127**。本家は 95 ブロック(`defaultMap` の最大ID 94 + 1)。今回の増減は次のとおり。

| 区分 | 増減 | 内訳 |
| --- | --- | --- |
| 削除 | -18 | steelwall、duriumwall、compositewall、duriumwall-large、sorter、smelter、crucible、coalpurifier、titaniumpurifier、oilrefinery、stoneformer、lavasmelter、siliconextractor、stonedrill、irondrill、titaniumdrill、omnidrill、weaponfactory |
| 追加 | +11 | human-engineer、ai-agent-1/2/3、senior-engineer、coderabbit、unit-test、e2e-test、jenkins、github-actions、credit用の床は不要 |
| 結果 | 95 - 18 + 11 = **88** | 上限 127 に対して余裕あり |

床ブロック(stone、grass、ice 等 27 個)はマップ描画に使うので残す。

#### IconGenerator(新規、desktop モジュールのツール)

- **目的**: 要件定義の「14枚の基本画像にドットを重ねて、プログラムで作る」を実装する。
- **入出力**: 入力は `core/assets-raw/sprites/code-base/q<stage>-i<importance>.png`(8×8、14枚。本家の `icon-*.png` が 8×8。確認済み)。出力は `core/assets-raw/sprites/icon-code-q<stage>-p<passed>-i<importance>.png`(70枚)。
- **ドットの位置**: 右下 3×3 の領域に、レビュー(白)、テスト(黒)、CI(緑)の 1px ドットを、それぞれ (5,7)、(6,7)、(7,7) に置く。重なる心配はない。
- **実行**: `desktop` モジュールに `tools/IconGenerator.java` を置き、`gradle :desktop:genIcons` タスクで実行してから `gradle pack` を実行する。生成物はリポジトリに入れる(ビルド環境に依存させない)。
- **色の設計**(品質を色で表す。赤→青のグラデーション):

| stage | 品質 | 色 |
| --- | --- | --- |
| 0 | -5 | `e0342c`(赤) |
| 1 | -3 | `e8702a`(橙) |
| 2 | -2 | `e8b02a`(黄) |
| 3 | -1 | `b8c832`(黄緑) |
| 4 | 0.5 | `5ec8a0`(青緑) |
| 5 | 0.8 | `3a9ee0`(空色) |
| 6 | 1 | `2a5ae8`(青) |

形は重要度1が丸、2が星(要件定義どおり)。

---

## 3. データフロー

### 3.1 データフロー図

```
[コード鉱脈 (床 iron)]
   │ CodeSource.update(): time秒ごとに
   │   stage ← random(stageChoices), importance ← random{1,2}
   │   item ← CodeItem.get(stage, 0, importance)
   │   offloadNear(tile, item)
   ▼
[パイプライン (Conveyor)] ── [PR (Router)] ── 分岐
   │                                           │
   ▼                                           ▼
[QualityGate / CodeRabbitBlock]           [別の QualityGate]
   │ acceptItem: コードで current==null なら受理
   │ update: progress が 1 に達したら
   │   next ← CodeItem.advance(current, passedBit, stages)
   │   offloadNear(tile, next)
   ▼
[コア (CoreBlock)]
   │ handleItem: score ← CodeItem.score(item)
   │   Score.deliver(item, score)
   │     ├ 残高 += score (端数は繰り越し)
   │     ├ 直近20個の平均品質を更新 → qualityMultiplier
   │     └ 0 を下回ったら bankrupt
   ▼
[Logic.update]
   ├ reachedTarget → won=true, GameOverEvent
   ├ bankrupt      → won=false, GameOverEvent
   └ runWave: EnemySpawn.evaluate × qualityMultiplier → 敵の数

[燃料の流れ(本家そのまま)]
[iGPU在庫 (床 coal)] → 家電屋 (Drill) → Conveyor → ローカルGPU (ItemPowerGenerator) → 配線 → 施設
[PCIe在庫 (床 uranium)] → ベンダー (Drill) → Conveyor → AIデータセンター (NuclearReactor) + 水 (Pump→Conduit)
[石油/溶岩の床] → ポンプ → 導管 → 国内クラウド / オンプレミス (LiquidPowerGenerator)
```

### 3.2 データ変換

- **入力データ形式**: `Item`(ID のみ)。コードの属性は `CodeItem.attrOf()` で ID から復元する。
- **処理過程**:
  - 生成: `(stage, 0, importance)` → Item
  - 変換: `advance(item, bit, n)` = `get(min(stage+n, 6), passed|bit, importance)`、ただし `passed & bit != 0` なら恒等
  - 納品: `score = QUALITY[stage] * IMPORTANCE[importance-1]`
- **出力データ形式**: 残高(`int`、`Inventory` の credit 枠)、倍率(`float`)、勝敗(`boolean won`)。
- **セーブ**: `Save15` の形式はそのまま使える。`GateEntity.write/read` で `current` の ID(`writeByte`、127 以下なので安全)と `progress`(`writeFloat`)を追加する。`Score` の残高は `Inventory` にあるので `Save15` が既に保存する。`qualityMultiplier` と `fraction` は保存しない(ロード直後は 1.0 から始まる。許容する)。

---

## 4. APIインターフェース

### 4.1 内部API

| 呼び出し元 | 呼び出し先 | 用途 |
| --- | --- | --- |
| `CodeSource.update()` | `CodeItem.get()` | 生成するコードを決める |
| `QualityGate.update()` / `CodeRabbitBlock.update()` | `CodeItem.advance()` | 変換後のコードを決める |
| `CoreBlock.handleItem()` | `CodeItem.score()`、`Score.deliver()` | 納品 |
| `EnemySpawn.evaluate()` | `state.score.qualityMultiplier` | 出現数の補正 |
| `Logic.update()` | `Score.reachedTarget()`、`Score.bankrupt()` | 勝敗 |
| `Logic.reset()` | `Score.reset(150)` | 初期資金(`Inventory.clearItems()` が `Item.stone` を 40 足している処理を置き換える。確認済み) |
| `HudFragment` / `BlocksFragment` / `RestartDialog` | `Score.balance()`、`Score.target` | 表示 |

本家の既存インターフェース(`acceptItem`、`handleItem`、`offloadNear`、`tryDump`、`PowerAcceptor`)は変えない。新ブロックはこれらに乗る。

### 4.2 外部API

なし。ブラウザ版は静的ファイルだけで動く。マルチプレイは本家の HTML 版でも非対応(`text.multiplayer.web` の文言。確認済み)なので、`Net*` 関連は触らない。

---

## 5. エラーハンドリング

### 5.1 エラー分類

- **未登録の (stage, passed, importance) を `CodeItem.get()` に渡した**: `IllegalArgumentException` を投げる。フォールバックで近い Item を返さない。`advance()` の探索と `registerAll()` の探索が同じ規則なら起きないので、起きたら実装ミスとして落とす。
- **`attrOf()` にコードでない Item を渡した**: `null` を返す。呼び出し側(`acceptItem`、`score`)は `null` を「燃料」として扱う。
- **アイコン領域が見つからない(`Draw.region("icon-code-...")`)**: libGDX の `TextureAtlas.findRegion()` は `null` を返し、描画時に `NullPointerException` になる。`Item.init()` の直後に全 Item の `region != null` を検査し、欠けていれば名前を列挙して `RuntimeException` で起動を止める(ビルド時のパック漏れを早く見つけるため)。
- **ブロック数が 127 を超えた**: `BlockLoader.load()` で `Block.getAllBlocks().size > 127` なら `RuntimeException`。現状 88 なので保険。
- **アイテム数が 127 を超えた**: 同様に `Item.getAllItems().size > 127` で止める。現状 73。
- **マップに床の色が無い(鉱脈ゼロ)**: ゲームとしては成立しないが、コードでは検出しない。マップ作成時のチェックリスト(§10)で対応する。

### 5.2 エラー通知

- 起動時の検査はログ(`Log.err`)と例外で止める。GWT ではブラウザのコンソールに出る。
- プレイ中の異常(到達不能な Item など)は例外で落とす。要件の「フォールバック禁止」に従う。

---

## 6. セキュリティ設計

### 6.1 認証・認可

なし。シングルプレイの静的コンテンツ。

### 6.2 データ保護

- セーブは `Settings`(GWT では `localStorage`、確認済み `SaveIO.saveToSlot()`)に Base64 で入る。個人情報は含まない。
- 本家の `Platform.getUUID()` が乱数の 8 バイトを `localStorage` に保存している(確認済み)。マルチプレイ用で、今回は使わないが、削除はしない(触らない方針)。
- GPL-3.0 の継承。`LICENSE` を残し、`AboutDialog` の文言に本家のクレジットと改変の旨を足す。

---

## 7. テスト戦略

### 7.1 単体テスト

本家にはテストが無い(確認済み、`src/test` なし)。純粋ロジックの `CodeItem` と `Score` だけ JUnit 4 を `core` に足す。GWT のコンパイル対象(`gwt.xml` の `source path`)にテストが入らないよう、`core/src/test` を `sourceSets.test` に置く。

- `CodeItem`: 到達可能な組み合わせが 70 であること。`advance()` が上限でクリップし、再通過で恒等であること。全 Item の `attrOf(get(a)) == a` が成り立つこと。
- `Score`: 端数の繰り越しが損をしないこと(0.5 を 2 回で 1 増える)。納品で 0 を下回ると `bankrupt()` が立つこと。建設(`removeItem`)では立たないこと。倍率の式の境界値。
- カバレッジ目標: この 2 クラスは 90% 以上。ブロック類は手動確認で代える。

### 7.2 統合テスト

デスクトップ版(`gradle :desktop:run`)で手動確認する。確認項目をチェックリストにする。

1. 人間のエンジニアとAIエージェントが鉱脈の上でだけコードを出す。鉱脈の外では `cross` の警告が出る(本家 `Drill.drawLayer()`、確認済み)。
2. コンベアで運んだコードが QualityGate に 1 個ずつ入り、処理後に色とドットが変わって出る。
3. 同じ施設を 2 回通しても色が変わらず、処理時間はかかる。
4. コアに入ると残高が増減する。品質 -5 のコードで減る。
5. 残高 0 を下回ると負けのダイアログ。目標に届くと勝ちのダイアログ。
6. 低品質を流し続けると次のウェーブの敵が増える。
7. 電力が切れると AI エージェント、テスト、CI、CodeRabbit が止まり、シニアと人間は止まらない。
8. CodeRabbit が敵を撃ちつつ変換もする。電力が尽きると両方止まる。
9. セーブしてロードしても、施設の処理中アイテムと残高が戻る。
10. 言語を日本語と英語で切り替えて、ブロック名、説明、勝敗の文言が切り替わる。
11. ブラウザ版(`gradle :html:dist` → ローカル HTTP サーバー)で 1〜10 を再確認する。特に 256×256 マップの初回経路計算にかかる秒数を記録する。

---

## 8. パフォーマンス最適化

### 8.1 想定される負荷

- **マップ**: 256×256 = 65,536 タイル。本家の既定マップも大半が 256×256(確認済み。`maze`、`fortress` など)なので、本家の HTML 版が動いていたサイズと同じ。
- **経路計算**: `Pathfind.update()` は 1 フレームあたり最大 5ms(`maxTime = 1000000 * 5` ナノ秒。確認済み)で、スポーン地点ごとに非同期に進める。マップサイズには依存するが、本家と同じ条件。
- **アイテム走査**: `Block.tryDump()` が全 Item(73 種)を走査する。`Router`(PR)が多数あると毎フレーム 73 × 4 方向のループになる。本家 8 種の約 9 倍。

### 8.2 最適化方針

- `QualityGate` と `CodeRabbitBlock` は `entity.items` を使わず `current` 1 個を持つので、`tryDump` の走査は出力が詰まったときだけ起きる。
- `Router` は本家のまま使うが、問題が出たら `Router.update()` の `tryDump(tile, dir, null)` を「`totalItems() > 0` のときだけ、非ゼロの ID から探す」小さな最適化で対応する。初期実装では触らない(動く機能は変えない方針)。
- アイコン 70 枚は 8×8 なので、アトラスのサイズは増えない。
- GWT 版で重い場合は、マップを 192×192 に縮める選択肢を残す。要件の「256×256 を 3 枚」は維持し、計測結果で判断する。

---

## 9. デプロイメント

### 9.1 デプロイ構成

- `gradle :html:dist` で `html/build/dist` に静的ファイル一式(`index.html`、`html/html.nocache.js`、`assets/`)が出る(確認済み)。
- 配布先は GitHub Pages か任意の静的ホスティング。`index.html` のタイトルと `logo.png` を差し替える。
- ビルド環境: JDK 17 と Gradle 8.14(wrapper)。`README` に JDK 17 の導入手順と `./gradlew :html:dist` の手順を書く。GitHub Actions で `:html:dist` を回し、成果物を GitHub Pages に置く。

### 9.2 設定管理

- ゲームバランスの数値(費用、処理時間、電力、倍率の式の係数、初期資金、目標得点)は Java の定数としてブロック定義に書く(本家の流儀。`Settings` や外部ファイルに逃がさない)。調整しやすいよう、`Balance.java` に定数をまとめ、各ブロック定義はそこを参照する。
- 目標得点はマップごとに変えられるよう `Map.targetScore`(`maps.json` の項目)に持つ。`Maps.loadMapFile()` は libGDX `Json` で読むので、フィールドを足すだけで読める(確認済み)。
- 言語ファイルは `bundle.properties`(英語)と `bundle_ja.properties`(日本語)の 2 つ。`Vars.locales` を `{en, ja}` にし、他 9 言語のファイルは削除する。`Platform.getLocaleName()` は `locale.toString()` を返す(確認済み)ので、言語ダイアログには `en`、`ja` と出る。表示名を `text.locale.<code>` キーで持ち、`LanguageDialog` で引くように 1 行変える。

---

## 10. 実装上の注意事項

- **最初にビルド基盤を更新する**: ゲーム内容に手を付ける前に、JDK 17、Gradle 8.14、libGDX 1.13.1、GWT 2.11.0 で `gradle :desktop:run` と `gradle :html:dist` が通る状態を作る(§1.2)。libGDX 1.9.8 → 1.13.1 の API 差分で壊れる箇所の目安(確認済み): `Vector2.angle()` の呼び出しが 5 ファイル 28 か所(1.13 では `angleDeg()`)、gdx-controllers の参照が 5 ファイル(切る)。ここが最大の未検証事項で、スパイクで壊れた箇所を洗い出してから直す。
- **言語ファイルの先行**: 日本語化はゲーム内容と独立に進められる。本家の文言(552 行、うちブロック・アイテム・液体名が 172 行)を先に日本語化し、新ブロックの文言はブロックを足すたびに両言語へ同時に足す。
- **削除より追加を先に**: 新 Item、新ブロック、新レシピを足して動作を見てから、旧 Item と旧ブロックを消す。消す順番は「レシピ → ブロック定義 → Item」。Item を先に消すとコンパイルエラーが 10 ファイルに広がる。
- **`Item` の ID 順を固定する**: `credit`、`igpu`、`pcie`、コード 70 種の順。セーブに ID が入るため、順序を変えるとセーブが壊れる。
- **`Tutorial` の扱い**: `Tutorial` は `Item.stone` と本家ブロックに依存する(確認済み)。本家のチュートリアルマップとともに削除し、`MenuFragment` のチュートリアルボタンも消す。AI SDLC 向けの導入は、最初のマップの説明文(`map.<name>.description`)とブロックの `fulldescription` で代える。
- **`WeaponFactory` と `UpgradeRecipes`**: プレイヤー機体の武器強化は `Item.iron` 等を要求する(確認済み)。`weaponFactory` ブロックを削除し、`UpgradeRecipes` は空にする(クラスは残す)。プレイヤーは初期武器だけで動く。
- **`Inventory.clearItems()`**: `addItem(Item.stone, 40)` を `addItem(Item.credit, Balance.INITIAL_CREDIT)` に変える。`debug` 時の 99999 埋めは残す。
- **`World.setDefaultBlocks()`**: コア周辺に `stonedrill` と `doubleturret` を置く(確認済み)。`human-engineer` と `turret`(リンター)に置き換え、鉱脈が無い場所でも動くよう `set()` の中で床を `Blocks.iron` にする(本家が `stonedrill` の下を `Blocks.stone` にしているのと同じ手法)。
- **アイコンの参照名**: `BlocksFragment` はブロックの配置メニューに `<name>-icon` があればそれを、無ければ `<name>` を使う(確認済み)。砲塔など本家の `-icon` 画像はそのまま使える。新ブロックは 8×8 の `<name>.png` だけ用意すれば足りる。
- **言語ファイルのキー**: フラット構造を守る(`block.<name>.name`、`block.<name>.description`、`block.<name>.fulldescription`、`item.<name>.name`、`liquid.<name>.name`、`map.<name>.name`)。コード 70 種の `item.code-q*-p*-i*.name` は、品質と重要度ごとの文言を 14 通り用意し、`CodeItem` が通過状態の接尾辞(例: 「(レビュー済)」)を `text.code.passed.review` 等のキーから組み立てて `localizedName()` を上書きする。70 行の手書きを避けつつ、翻訳ファイルの構造はフラットのまま。
- **`getStats()` の英語直書き**: 本家は `"[powerinfo]Power Capacity: "` のように英語をコードに書き、`BlocksFragment.showBlockInfo()` が `text.blocks.<小文字化した項目名>` で翻訳している(確認済み)。新ブロックの `getStats()` も同じ流儀で書き、対応する `text.blocks.*` キーを両言語に足す。
- **敵の名前**: `EnemyType.name` はスプライト名(`standard-t1` など)に使われる(確認済み)。表示名は使われていないので、敵の意味づけ(「障害・バグ」等)はマップ説明や about の文言で伝える。スプライト名は変えない。
- **やらないこと**: `Net*`、`mapeditor`、`Pathfind`、`Renderer`、`Conveyor` の内部ロジックは触らない。本家の `Sorter`、`LiquidCrafter`、`Smelter`、`Omnidrill`、`LiquidDrill` はクラスごと削除してよいが、`Drill`、`Pump`、`Generator` 系は残す。

### 未検証事項の引き継ぎ(要件定義から)

| 項目 | この設計での扱い |
| --- | --- |
| ビルド | §10 の最初の作業で確認する |
| 施設の動き | §7.2 のチェックリストで確認する |
| アイテム種類数の上限 127 | コードで確認済み(`Conveyor.ItemPos.toInt/toLong`、`Save15`)。73 種で運用。起動時検査を入れる |
| ブロック ID の上限 127 | コードで確認済み(`Tile.iSetBlock`、`Block.getByID`)。88 個で運用。起動時検査を入れる |
| GWT への影響 | §7.2 の 11 で確認する |
| マップの重さ | 本家既定マップと同サイズ。§7.2 の 11 で秒数を記録し、重ければ §8.2 の縮小案 |
