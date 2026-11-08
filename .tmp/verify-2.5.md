# Task 2.5 verification (Issue #11)

- Date: 2026-11-08
- Branch: `issue-11-names` (stacked on `issue-10-ja-bundle`)
- JDK 17, Gradle 8.14, libGDX 1.13.1

## Target

- The kept recipes: `Recipes.java` has 63 recipes. The design (§2.2 Recipes) deletes 16 of them (steelwall, diriumwall, diriumwalllarge, sorter, smelter, crucible, coalpurifier, titaniumpurifier, oilrefinery, stoneformer, lavasmelter, weaponFactory, stonedrill, irondrill, titaniumdrill, omnidrill), so **47 blocks** are kept.
- Plus `core` (no recipe, but it is placed on every map; the game metaphor calls it the production environment), so **48 blocks** got a name and a description.
- Keys use the constructor string, not the Java field: `poweredconveyor` (field `pulseconveyor`), `conveyortunnel` (`tunnel`), `waveturret` (`teslaturret`), `titancannon` (`titanturret`), `door-large` (`largedoor`), `steelwall-large`, `titaniumwall-large`, `nuclearreactor` (`nuclearReactor`).
- Pipeline order follows the conveyor speed: `conveyor` 0.02 = 通常, `steelconveyor` 0.04 = 高速, `poweredconveyor` 0.09 = 超高速.

## Mapping table

Names: requirements 「名称と言語」 and 「施設」. English names are decided here. Parentheses are half-width in both languages.
Descriptions (`block.<ID>.description`) are inserted right after `block.<ID>.name` at the same position in both files.

| # | ID | ja | en | description (ja) | description (en) |
| --- | --- | --- | --- | --- | --- |
| 1 | `turret` | リンター | linter | 近づく敵を撃つ、いちばん手軽な防衛。 | The simplest defense, shooting at approaching enemies. |
| 2 | `doubleturret` | 型チェッカー | type checker | 2門で撃ち、リンターより多くの敵を止める。 | Fires from two barrels to stop more enemies than a linter. |
| 3 | `machineturret` | ファジング | fuzzer | すばやい連射で、次々に来る敵を削る。 | Rapid fire that wears down enemies as they keep coming. |
| 4 | `shotgunturret` | ペネトレーションテスト | penetration test | 一度に広く撃ち、近くの敵をまとめて叩く。 | Fires a spread of shots that hits several nearby enemies at once. |
| 5 | `flameturret` | ホットフィックス | hotfix | 射程は短いが、目の前の敵をすばやく焼き払う。 | Short range, but quickly burns down enemies right in front of it. |
| 6 | `sniperturret` | セキュリティ監査 | security audit | 遠くの敵を、重い一撃で狙い撃つ。 | Picks off distant enemies with heavy single shots. |
| 7 | `mortarturret` | レートリミッター | rate limiter | 遠くの敵の群れを、範囲攻撃で抑え込む。 | Holds back distant groups of enemies with area attacks. |
| 8 | `laserturret` | アラート | alert | トークンを使い、レーザーで敵をすばやく捉える。 | Uses tokens to catch enemies quickly with a laser. |
| 9 | `waveturret` | オブザーバビリティ | observability | トークンを使い、連鎖する電撃で周りの敵をまとめて捉える。 | Uses tokens to hit nearby enemies with chaining lightning. |
| 10 | `plasmaturret` | ロールバック | rollback | 高熱のプラズマを浴びせ、近くの敵を一気に片付ける。 | Sprays hot plasma to clear out nearby enemies in one go. |
| 11 | `chainturret` | 回帰テストスイート | regression test suite | 重い連射で、大きな敵も止め続ける。 | Heavy rapid fire that keeps even large enemies in check. |
| 12 | `titancannon` | AIセキュリティエージェント | AI security agent | 最強の砲撃で、ボス級の敵にも立ち向かう。 | The strongest cannon, able to take on boss-level enemies. |
| 13 | `repairturret` | AIによる自動修正 | AI auto-fix | トークンを使い、近くの壊れた施設を直す。 | Uses tokens to repair damaged blocks nearby. |
| 14 | `megarepairturret` | AIによる自動修正(高性能) | AI auto-fix (pro) | トークンを使い、より広い範囲の施設をすばやく直す。 | Uses tokens to repair blocks faster over a wider area. |
| 15 | `shieldgenerator` | ファイアウォール | firewall | トークンを使ってシールドを張り、敵の弾を防ぐ。 | Uses tokens to raise a shield that blocks enemy bullets. |
| 16 | `stonewall` | 基本ルール | basic rules | 敵の進路をふさぐ、安価で基本的な防御ルール。 | A cheap, basic rule set that blocks the enemy's path. |
| 17 | `ironwall` | OWASPルール | OWASP rules | 定番のセキュリティ基準にならい、基本ルールより固く守る。 | Follows a well-known security standard and holds longer than basic rules. |
| 18 | `titaniumwall` | カスタムルール | custom rules | 自社向けに作り込んだ、頑丈な防御ルール。 | Sturdy rules tailored to your own system. |
| 19 | `steelwall-large` | マネージドルール | managed rules | 専門家が保守する、広い範囲を守る防御ルール。 | Rules maintained by specialists that cover a wide area. |
| 20 | `titaniumwall-large` | エンタープライズWAF | enterprise WAF | 広い範囲を固く守る、大規模向けの防御。 | Heavy-duty protection for large systems that covers a wide area. |
| 21 | `door` | フィーチャーフラグ | feature flag | 開け閉めを切り替えて、通り道を開いたり閉じたりする。 | Toggle it to open or close the path. |
| 22 | `door-large` | メンテナンスモード | maintenance mode | 広い通り道を、まとめて開け閉めする。 | Opens or closes a wide path at once. |
| 23 | `titaniumshieldwall` | AIシールド | AI shield | トークンを使って、受けた攻撃を打ち消す壁。 | A wall that spends tokens to cancel out incoming damage. |
| 24 | `conveyor` | パイプライン(通常) | pipeline (normal) | アイテムを次の施設へ流す、基本のパイプライン。 | The basic pipeline that moves items to the next block. |
| 25 | `steelconveyor` | パイプライン(高速) | pipeline (fast) | 通常の倍の速さでアイテムを流す。 | Moves items twice as fast as the normal pipeline. |
| 26 | `poweredconveyor` | パイプライン(超高速) | pipeline (ultra) | いちばん速くアイテムを流すパイプライン。 | The fastest pipeline for moving items. |
| 27 | `router` | PR | PR | アイテムを受け取り、隣の施設に振り分ける。 | Takes in items and distributes them to neighboring blocks. |
| 28 | `junction` | クロスポイント | crosspoint | 交差する2本のパイプラインを、混ぜずに通す。 | Lets two crossing pipelines pass through without mixing. |
| 29 | `conveyortunnel` | バイパス | bypass | 地下をくぐり、間にある施設を飛び越えて運ぶ。 | Carries items underground, skipping over the blocks in between. |
| 30 | `teleporter` | API連携 | API link | トークンを使い、同じ色の連携先へアイテムを瞬時に送る。 | Uses tokens to send items instantly to linked blocks of the same color. |
| 31 | `coalgenerator` | ローカルGPU | local GPU | 手元のGPUで、燃料からトークンを作る。 | Generates tokens from fuel on a local GPU. |
| 32 | `thermalgenerator` | オンプレミス | on-premise | 自社の設備で、dGPUからトークンを作る。 | Generates tokens from dGPU on your own hardware. |
| 33 | `combustiongenerator` | 国内クラウド | domestic cloud | 国内のクラウドで、vGPUからトークンを作る。 | Generates tokens from vGPU in a domestic cloud. |
| 34 | `rtgenerator` | 海外クラウド | overseas cloud | 海外のクラウドで、燃料から長くトークンを作り続ける。 | Keeps generating tokens from fuel for a long time in an overseas cloud. |
| 35 | `nuclearreactor` | AIデータセンター | AI data center | 冷却水を絶やさずに、燃料から大量のトークンを作る。 | Generates a large amount of tokens from fuel; keep it supplied with cooling water. |
| 36 | `powerbooster` | AIゲートウェイ | AI gateway | 周囲の施設にトークンを配る。 | Distributes tokens to blocks around it. |
| 37 | `powerlaser` | プライベート接続 | private link | 向いている方向の施設へトークンを送る。 | Sends tokens to the block it faces. |
| 38 | `powerlasercorner` | プロキシ | proxy | トークンを中継し、2方向へ送る。 | Relays tokens in two directions. |
| 39 | `powerlaserrouter` | ロードバランサー | load balancer | トークンを中継し、3方向へ振り分ける。 | Relays tokens and splits them in three directions. |
| 40 | `coaldrill` | 家電屋 | electronics store | 在庫の床に置き、発電機の燃料を仕入れる。 | Place it on a stock floor to procure fuel for generators. |
| 41 | `uraniumdrill` | ベンダー | vendor | 在庫の床に置き、上位の発電機向けの燃料を仕入れる。 | Place it on a stock floor to procure fuel for high-end generators. |
| 42 | `pump` | ポンプ | pump | 液体の床に置き、水、vGPU、dGPUをくみ上げる。 | Place it on a liquid floor to pump up water, vGPU or dGPU. |
| 43 | `fluxpump` | フラックスポンプ | flux pump | ポンプより多くの液体をくみ上げる。 | Pumps more liquid than a pump. |
| 44 | `conduit` | 導管 | conduit | 液体を運ぶ管。 | A pipe that moves liquids. |
| 45 | `pulseconduit` | パルス導管 | pulse conduit | 導管より多くの液体を速く運ぶ。 | Moves more liquid, faster than a conduit. |
| 46 | `liquidrouter` | 液体ルーター | liquid router | 液体を受け取り、隣に振り分ける。 | Takes in liquids and distributes them to neighboring blocks. |
| 47 | `liquidjunction` | 液体ジャンクション | liquid junction | 交差する2本の導管を、混ぜずに通す。 | Lets two crossing conduits pass through without mixing. |
| 48 | `core` | 本番環境 | production | 守るべき本番環境で、壊されるとゲームオーバー。 | The production environment you must protect; losing it ends the game. |

Rules applied to the descriptions:

- One sentence, the role in the AI SDLC metaphor. No `{n}`, no color markup, no `???`
- The 10 turrets that currently use item ammo (turret, doubleturret, machineturret, shotgunturret, flameturret, sniperturret, mortarturret, plasmaturret, chainturret, titancannon) do not mention ammo, fuel or tokens (requirements: all turrets will fire with tokens later)
- Only blocks that already use power in the code mention トークン/tokens: laserturret, waveturret, repairturret, megarepairturret, shieldgenerator, titaniumshieldwall (`ShieldedWallBlock` drains power per damage), teleporter (`powerPerItem`), generators, power wiring
- No unimplemented behaviour (code quality, review, interception, delivery scoring). Items are called アイテム/items, not code
- Procurement blocks and `coalgenerator` / `rtgenerator` / `nuclearreactor` say 燃料/fuel, not iGPU/PCIe, because `item.coal.name` / `item.uranium.name` still read 石炭/ウラン until Task 3.11. The liquid generators and the pump name vGPU/dGPU because `liquid.oil/lava.name` are renamed in this task
- `block.<ID>.description` is read into `Block.description` (`Block.java:98`) but no UI displays it today (`BlocksFragment` shows the name, cost, health and the `?` button for `fulldescription`). The text is prepared for later UI use; it cannot be seen in game yet

## Items and liquids

| key | ja | en |
| --- | --- | --- |
| `item.igpu.name` (new, after the last `item.*` key) | iGPU | iGPU |
| `item.pcie.name` (new) | PCIe | PCIe |
| `liquid.oil.name` | vGPU (was 石油) | vGPU (was oil) |
| `liquid.lava.name` | dGPU (was 溶岩) | dGPU (was lava) |
| `liquid.water.name` | 水 (unchanged) | water (unchanged) |

`item.coal.name` / `item.uranium.name` are unchanged (Task 3.11). In English, `item.igpu/pcie.name` come after `item.sand.name` (excluded), so in Japanese they come after `item.uranium.name`; the checker confirms the order.

## Exceptions (not changed on purpose)

- The 16 deleted blocks above (their bundle keys are `steelwall`, `duriumwall`, `duriumwall-large`, `sorter`, ..., plus `compositewall`, which has no recipe): `.name` / `.fulldescription` as in Task 2.4 (English only; excluded from Japanese), no `.description` added
- Floor / ore block names (`block.oil.name`=石油, `block.lava.name`=溶岩, `block.coal.name`=石炭, `block.uranium.name`=ウラン鉱石, ...) stay as translated in Task 2.4. The liquid is vGPU/dGPU while the floor it comes from is still 石油/溶岩
- All `.fulldescription` values (Phase 4). They still describe the upstream behaviour (e.g. "uses stone as ammo")
- The turret stats still show the item ammo (`ammo: stone` etc. from `getStats()`), and the build cost still shows upstream items; both change in Phase 3
- `block.liquiditemjunction.name`, `block.playerspawn/enemyspawn.name`, `block.air/blockpart.name` (not placeable)
- No Java code changed

## Checker and key checks

```
$ python3 -I tools/check_bundles.py
en keys: 614, excluded: 153, expected in ja: 461, ja keys: 461
OK
(exit 0)
```

- 614 = 564 + 48 descriptions + 2 item keys; 461 = 411 + 50
- Duplicate keys (all non-comment `key=` lines, `sort | uniq -d`): en 0, ja 0
- All 48 `block.<ID>.name` and `block.<ID>.description` exist in both files. Values equal between ja and en: only `block.router.name` (`PR`)
- No value of the 96 keys contains `???`, `{` or `[`

## Font regeneration

New kanji in the names and descriptions were not in `misaki.fnt`, so the font was regenerated (`tools/README.md` steps 1-3):

- `collect_chars.py`: 811 code points (772 before this task). 42 added (仕倍備元叩固国型外家専屋帰広床庫張打払抑振捉携施査模浴海混監瞬社級絶衛越軽進道遠門飛), 3 removed because no value uses them any more (子 炉 装)
- `bdf2fnt.py`: 797 glyphs (758 before), page 256x256. The 14 "missing" code points are the kana-range code points the Misaki BDF has no glyph for (U+3094-309A, U+309F, U+30A0, U+30F7-30FA, U+30FF), none used in the bundles
- `check_fnt.py`: alpha [0, 255], all rects inside the page, no overlaps, no stray pixels (exit 0)
- Changed files: `tools/chars_ja.txt`, `core/assets/ui/misaki.fnt`, `core/assets/ui/misaki.png`

## Build

`./gradlew :core:compileJava :html:dist` (JDK 17): success. `html/build/dist/assets/bundles/bundle_ja-*.properties` contains the new keys. `html/build/dist` is left for the browser check; `core/assets/version.properties` is not modified.

## Pending (orchestrator)

- In-game check in the browser (ja and en): every block in the placement menu shows the AI SDLC name; long names such as AIセキュリティエージェント and 回帰テストスイート wrap inside the 160 px name label; the liquid name in the block inspect shows vGPU/dGPU
