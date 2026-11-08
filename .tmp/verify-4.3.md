# Task 4.3 検証記録: ブロックの詳細説明と getStats(Issue #25)

## 変更内容

### 統計行(`abd0503`)
- 見出しは本家の流儀で英語直書き(`showBlockInfo()` が `text.blocks.<小文字化した見出し>` で訳す)。値側の単位とカテゴリ名は `getStats()` の中で `Bundles.get()` で訳す。
- `CodeSource`(人間のエンジニア、AI 3 種): `Drill` の Capacity / Seconds/item に加え、`[iteminfo]Start Quality: -2, -1`(`stageChoices` を `CodeItem.QUALITY` の値にしたもの)、`[iteminfo]Importance: 1, 2`、コロン無しの `[iteminfo]` + `text.blocks.codevein`(Generator の爆発警告と同じ書き方)。AI は `PoweredCodeSource` の Power Capacity / Power/item がそのまま続く。
- `GateLogic.addStats(list, passedBit, stages, processTime)` を足し、`QualityGate` と `CodeRabbitBlock` の両方から呼ぶ。Category(値は `text.code.category.review/test/ci`)、Quality Gain、Seconds/item の 3 行。
- `CodeRabbitBlock`: `PowerTurret` の射程、ダメージ、Power Capacity(1 行だけ)、Power/shot の後ろに、上の 3 行と Power/item。
- `CodeItem.bitKey(passedBit)`: 通過ビットを `review` / `test` / `ci` に変える。カテゴリ名と、下のアイテム名の接尾辞で共用。
- `PowerBooster` の ` tiles`、`LiquidPowerGenerator` の ` power/liquid` と ` liquid/s` を `text.blocks.unit.*` に置き換えた。見出し(`Power/Liquid`、`Max liquid/second:`)は変えていない(`NuclearReactor` がこの見出しで親の行を消すため)。
- 新しいキー(両言語、`text.blocks.qualitygain` の直後): `text.blocks.category`、`startquality`、`importance`、`codevein`、`unit.tiles`、`unit.powerliquid`、`unit.liquidsecond`、`text.code.category.review/test/ci`。
- ja の値側の単位は、既存の統計見出し(`電力/液体` など)に合わせて「電力」にした。詳細説明は 2.5 の `description` に合わせて「トークン」。統計見出しの「電力」をトークンに揃えるかは本タスクの範囲外として触っていない。

#### プランとの違い
- `QualityGate` は `Block` を継承していて `PowerBlock` ではないので、シニアエンジニアには最初から Power Capacity 行が無い。NuclearReactor 式の行の削除は要らなかった(テストで行が無いことを確認)。電力を使うテスト / CI は `PoweredQualityGate` が Power Capacity と Power/item を出す(既存)。
- Quality Gain の行は 4.3 の前から `QualityGate` / `CodeRabbitBlock` にあり、`text.blocks.qualitygain` も両言語にあった。共通化のため行の順を Category → Quality Gain → Seconds/item に揃えた。
- `coderabbit` の弾 `BulletType.coderabbitShot` のダメージは 10 で、設計値と一致。バランス値は変えていない。

### コードアイテムの表示名(`8de5776`)
- `CodeItem` は `Item` を継承していないので、`registerAll()` で作る `Item` を private な `CodeItemType extends Item` にし、`localizedName()` を上書きした。名前 `code-q*-p*-i*`、登録順、ID、アイコンは変えていない(既存の CodeItem / ItemIcons のテストがそのまま通る)。
- `CodeItem.nameKeys(stage, passed, importance)`: 先頭が `item.code.q<stage>.i<importance>.name`、続いて立っているビットの `text.code.passed.*` を review → test → ci の固定順。`displayName()` が各キーを訳して半角空白 1 個でつなぐ。キャッシュはしない。
- 基本名 14 キー(`item.pcie.name` の直後):

| stage | 品質 | en (i1 / i2) | ja (i1 / i2) |
| --- | --- | --- | --- |
| 0 | -5 | broken code / broken critical code | 壊れたコード / 壊れた重要コード |
| 1 | -3 | buggy code / buggy critical code | バグだらけのコード / バグだらけの重要コード |
| 2 | -2 | rough code / rough critical code | 荒いコード / 荒い重要コード |
| 3 | -1 | mediocre code / mediocre critical code | 平凡なコード / 平凡な重要コード |
| 4 | 0.5 | decent code / decent critical code | まずまずのコード / まずまずの重要コード |
| 5 | 0.8 | good code / good critical code | 良いコード / 良い重要コード |
| 6 | 1 | excellent code / excellent critical code | 優秀なコード / 優秀な重要コード |

- 接尾辞(`text.code.category.ci` の直後): en `(reviewed)` `(tested)` `(CI passed)`、ja `(レビュー済)` `(テスト済)` `(CI済)`。括弧は 2.5 の取り決めどおり半角。例: `優秀な重要コード (レビュー済) (テスト済) (CI済)`。

### 詳細説明(`3410389`)
- 対象: 4.2 後の `Recipes` 57 件の結果ブロックと `core` の計 58 個。床、鉱脈、`air`、`blockpart`、出現マーカーは対象外。
- 新規 11 個: human-engineer、ai-agent-1、ai-agent-2、ai-agent-3、senior-engineer、coderabbit、unit-test、e2e-test、jenkins、github-actions、core
- 書き直し 47 個(本家の石、鉄、石炭、ウラン、弾薬、ドリル、コンベアなどの説明を AI SDLC の文脈に):
  - production: coaldrill、uraniumdrill、pump、fluxpump
  - distribution: conveyor、steelconveyor、poweredconveyor、router、junction、conveyortunnel、conduit、pulseconduit、liquidrouter、liquidjunction、teleporter
  - power: coalgenerator、thermalgenerator、combustiongenerator、rtgenerator、nuclearreactor、powerlaser、powerlasercorner、powerlaserrouter、powerbooster
  - weapon: turret、doubleturret、machineturret、shotgunturret、flameturret、sniperturret、laserturret、mortarturret、waveturret、plasmaturret、chainturret、titancannon
  - defense: stonewall、ironwall、titaniumwall、steelwall-large、titaniumwall-large、door、door-large、titaniumshieldwall、shieldgenerator、repairturret、megarepairturret
- 内容の要点:
  - コード生成: コード鉱脈の上でだけ動く。人間はトークン不要で遅く品質 -2/-1、AI はトークンを使い速いが品質が低い(-5/-3、-3/-1、-1)。上位の AI をそのまま上に置いて強化。
  - 品質の施設: レビュー、テスト、CI は順不同。同じカテゴリの再通過では品質が上がらないが、処理時間(とトークン)は使う。同じカテゴリの施設の組(シニアと CodeRabbit、ユニットと E2E、Jenkins と GitHub Actions)を明記。
  - CodeRabbit: 品質 2 段階のレビュー(シニアの倍で速い)と迎撃を 1 ブロックで行い、同じトークン残量を分け合う。迎撃が優先で、少なくなるとレビューが待ち、尽きると迎撃も止まる(`CodeRabbitBlock` のコメントと実装に合わせた)。
  - core: 品質×重要度で残高が増減、マイナス品質は減る、コード以外は 0、最近の品質が低いほどバグが増える、壊されるとゲームオーバー。
  - PR は品質を変えずに配る(送り返すこともある)。バイパスは 2 つ 1 組で逆向き、出口は入口の後ろ 3 タイル以内(`TunnelConveyor.maxdist`)。API連携は同色、トークンを使う。
  - 発電: iGPU(ローカルGPU)、dGPU(オンプレミス)、vGPU(国内クラウド)、PCIe(海外クラウド、AIデータセンター)。AIデータセンターは水冷が切れると過熱して爆発。調達は iGPU / PCIe の在庫の床。
  - 迎撃の 12 種はすべて `PowerTurret` / `LaserTurret` になったので、弾薬の記述を消してトークンで撃つと書いた。
- 削除: 3.11 で消えたブロックの残りとして `block.liquiditemjunction.name` と `.fulldescription` を両言語から消した(コードに該当ブロックが無い)。
- `\n` は段落の区切りにだけ使った。`[` と `{` は使っていない。en の「品質×重要度」は既定フォントを考えて `quality x importance`(ASCII の x)。

## bundle とフォント
- `python3 -I tools/check_bundles.py`: `en keys: 582, excluded: 67, expected in ja: 515, ja keys: 515` / `OK`(4.2 時点は en 546、ja 479。+36 = 統計 10 + 接尾辞 3 + 基本名 14 + 新規の詳細説明 11 − liquiditemjunction 2)。
- `tools/bundle_exclude_keys.txt`: 変更なし(67 件はすべて en に存在、新キーは除外していない)。
- フォント(`collect_chars.py` → `bdf2fnt.py` → `check_fnt.py`):
  - `8de5776`: 816 → 820 コードポイント。追加 優 凡 平 秀 荒、削除 途(4.2 の記録にあった未使用の 1 字)。
  - `3410389`: 820 → 818 コードポイント。追加 19 字(× 事 供 値 処 口 尽 巻 控 換 決 理 組 給 落 身 返 過 順)、削除 21 字(本家の説明だけで使っていた 傷 単 収 吸 場 崩 戦 所 掘 放 有 材 橋 火 炎 産 絞 製 造 鋼 非)。
  - 最終: グリフ 804 個、ページ 256×256。`check_fnt.py` は alpha [0, 255]、矩形の重なり・はみ出し無し。missing の 14 個(U+3094–309A、309F、30A0、30F7–30FA、30FF)は常に入れる仮名の範囲のうち BDF に無いもので、2.2 からの既知。本タスクの文に BDF に無い字は無い。
  - 見本画像で「優秀な重要コード (レビュー済)」「品質×重要度 巻き込んで過熱」を目視確認した。

## テスト
- 新規 `BlockInfoTest`(7 件): 対象 58 個、全統計行が両言語で `???` にならない(showBlockInfo と同じ規則で見出しを訳す)、日本語の統計行に英字が残らない(色タグ、`1x1`、iGPU / PCIe / dGPU / vGPU / CI を除く)、全対象の `fulldescription` が両言語のファイルにある(ja の I18NBundle は無いキーを英語から引くので、ファイルを直接読む)、シニアに電力行が無い、CodeRabbit の Power Capacity が 1 行でゲートの 4 行がある、人間 / AI-1 の出発品質と重要度。
- 新規 `CodeItemNameTest`(5 件): p=0 は基本名だけ、単一通過、固定順(REVIEW|CI → review, ci、全通過 → review, test, ci)、半角空白 1 個での連結と `toString()`、登録 70 種すべてに両言語の名前がある。
- `./gradlew :core:test`: 成功(114 件、失敗 0、エラー 0。4.2 時点の 102 件 + 12 件)。
- `./gradlew :html:dist`: 成功。`html/build/dist/assets` の `misaki-*.png` と `bundle_ja-*.properties` は `core/assets` のものと同一。`html/build/dist` を残してある。

## 確認していないこと
- ブラウザ / デスクトップでのブロック情報ダイアログの見た目(英語・日本語の両方、字欠け、行の折り返し、CodeRabbit の Power Capacity が 1 行、シニアに電力行が無いこと)。行の中身は上のテストで確認しただけ。
- コードアイテム名はアイテムの名前を出す UI(資源欄は 4.2 で残高 1 行にした)がほぼ無いので、画面では確認していない。テストで組み立てを確認しただけ。
- 日本語の詳細説明の長さ。最長は coderabbit と core(約 160 字)で、ダイアログ幅 600 で折り返す前提。
