# Task 4.2 検証記録: HUD と配置メニュー(Issue #24)

## 変更内容

### 残高と目標の表示(`63e6d2e`)
- bundle: `text.score.status` を両言語の `text.enemies.single` の直後に足した。en `Target {0} / Balance {1}`、ja `目標 {0} / 残高 {1}`。`text.credits` と `text.level.highscore` は意味が違うので使っていない。`item.credit.name` は既存(en `credits`、ja `得点`)。
- `BlocksFragment.updateItems()`: inventory 全 ID の走査と奇偶の改行をやめ、`icon-credit` の画像と `state.score.balance()` の数量を 1 行だけ出す。残高 0 でも行を出す(`0` と表示)。無限資源モードの早期 return と `itemtable` の可視条件(レシピ選択中と無限資源モードで隠す)は変えていない。
  - アイコンは `Draw.region("icon-credit")` を直接使う。プランの「画像が無ければ blank」は採らない(フォールバック禁止。`icon-credit` は Task 3.3 から `Control` の起動時検査の対象)。
- 数値の書式: 既存の `format(int)` を `static`(パッケージ内可視)にし、元の `amount > 99999999 ? "inf" : format(amount)` を `static String formatAmount(int)` に切り出した。中身は変えていない。資源欄と HUD の両方がこれを使う。
- `HudFragment`: 左上の内側テーブルで、波表ラッパー(`visible(() -> shown)`)の直後、`infolabel` の前に `scorelabel` の行を足した。
  - supplier 形式で毎フレーム `Bundles.format("text.score.status", target, balance)` を組み立てる。値は `state.score.target` と `state.score.balance()` を毎回読む。
  - 文字の大きさは波表の見出しや資源欄の数量と同じ `UI.labelScale(fontscale*1.5f)`。セルは `left()`。
  - `shown` の可視条件を持たないので、折り畳みで隠れない。`toggleMenus()` で `infolabel` と同じく、折り畳み時に `wavetable.getHeight()` だけ上へ動かし、再表示時に戻す(`Actions.call(() -> shown = false)` は `infolabel` 側にあるので重ねていない)。
  - `formatAmount()` は `1.5[gray]k` のように色を付けて終わることがあるので、HUD では値の後ろに `[]` を付けて色を戻す(`scoreValue()`)。付けないと後ろの「 / Balance」まで灰色になる。

### 配置メニュー(`d73030e`)
- `Section` の宣言順: `production, crafting, distribution, power, weapon, defense`(enum 名は変えていない。`icon-<name>` と `sectionbutton<name>` の参照はそのまま)。タブは 3 個ごとに改行するので、1 行目が「生成 / 品質 / 搬送」、2 行目が「電力 / 迎撃 / 防御」。先頭の `production` が初期タブ。
- `Recipes`: 下の一覧の所属と順に並べ替えた。費用と `desktopOnly`(door、largedoor)は変えていない(57 件のまま)。リストもセクション順に並べ直した。
- アイコン: 6 枚の `core/assets-raw/sprites/ui/icons/icon-<section>.png`(10×10、白一色)を見た。
  - production は下向きのドリルの刃、crafting は枠の中の菱形(精錬炉)で、「コード生成」「品質ゲート」を表していないので、同名で差し替えた。
  - production(ターミナルのプロンプト `>_`)と crafting(チェックマーク):
    ```
    production   crafting
    ..........   ..........
    ..........   ..........
    .##.......   ........#.
    ..##......   .......##.
    ...##.....   .#....##..
    ..##......   .##..##...
    .##.......   ..####....
    .....####.   ...##.....
    .....####.   ..........
    ..........   ..........
    ```
  - distribution(コンベア)、power(稲妻)、weapon(砲台)、defense(盾)は分類と合っているので残した。
  - PNG は一度きりの Python スクリプト(標準ライブラリ、RGBA、白 `#FFFFFF` 不透明 / 透明)で書き、`./gradlew :core:pack` で再パックした。`sprites.atlas` は変化なし(同じ 10×10 なので配置が同じ)、`sprites.png` だけ変わった。パック後の `sprites.png` の `icon-production`(919,483)と `icon-crafting`(835,469)の領域の alpha が新しい PNG と一致することを確認した。同名ファイルは `assets-raw/sprites` 内に 1 つずつだけ。
  - ツールチップは足していない。
- テスト:
  - `RecipesTest.sectionOrderStartsWithProduction`: `Section.values()` が上の順。
  - `RecipesTest.everySectionHasRecipe`: 全セクションにレシピがあり、`desktopOnly` を除いても 1 件以上ある。
  - `CodeRabbitBlockTest.recipeIsInWeaponAndCosts100` → `recipeIsInCraftingAndCosts100`(所属を `crafting` に)。

## セクションごとのレシピ(タブ順、括弧は費用)
- production(8): human-engineer 人間のエンジニア(50)、ai-agent-1(20)、ai-agent-2(30)、ai-agent-3(40)、coaldrill 家電屋(65)、uraniumdrill ベンダー(80)、pump(10)、fluxpump(15)
- crafting(6): senior-engineer シニアエンジニア(80)、coderabbit CodeRabbit(100)、unit-test ユニットテスト(30)、e2e-test E2Eテスト(45)、jenkins(60)、github-actions(80)
- distribution(11): conveyor(1)、steelconveyor(1)、pulseconveyor(1)、router(2)、junction(2)、tunnel(2)、conduit(1)、pulseconduit(2)、liquidrouter(2)、liquidjunction(2)、teleporter API連携(70)
- power(9): coalgenerator ローカルGPU(50)、thermalgenerator オンプレミス(60)、combustiongenerator 国内クラウド(50)、rtgenerator 海外クラウド(40)、nuclearreactor AIデータセンター(130)、powerlaser プライベート接続(6)、powerlasercorner プロキシ(8)、powerlaserrouter ロードバランサー(10)、powerbooster AIゲートウェイ(16)
- weapon(12): turret、doubleturret、machineturret、shotgunturret、flameturret、sniperturret、laserturret、mortarturret、teslaturret、plasmaturret、chainturret、titanturret(費用・順は従来どおり)
- defense(11): stonewall、ironwall、titaniumwall、steelwalllarge、titaniumwalllarge、door(desktopOnly)、largedoor(desktopOnly)、titaniumshieldwall、shieldgenerator ファイアウォール(60)、repairturret(30)、megarepairturret(50)
- モバイル(`desktopOnly` 除外)でも defense は 9 件で、空のタブはない。

## bundle とフォント
- `python3 -I tools/check_bundles.py`: OK(en 546、除外 67、ja 479)
- 新しい日本語の字(目 標 残 高)はすべて `tools/chars_ja.txt` にあるので、フォントは作り直していない。
- 参考: `collect_chars.py` を走らせると `途` が 1 字消える(815 コードポイント)。Task 4.1 の説明文短縮(`fb60c05`)で `途` を使わなくなったが、フォントはその前の 816 字のまま。余分な字が 1 つあるだけで表示に害はないので、本タスクでは触らず `chars_ja.txt` は元に戻した。

## テストとビルド
- `./gradlew :core:test`: 成功(102 件、失敗 0、エラー 0。前回 100 件 + 2 件)
- `./gradlew :html:dist`: 成功。`html/build/dist` の `sprites-*.png` は `core/assets/sprites/sprites.png` と同一、両言語の bundle に `text.score.status` がある。`html/build/dist` を残してある。

## ブラウザ確認(Playwright、HTML ビルド)
- prototype を開始すると、波表の下に `TARGET 1000 / BALANCE 140` が出る。`C` で折り畳むと波表は消え、ラベルは上に詰まって見えたまま。再表示で元の位置に戻る。
- 資源欄は `icon-credit` と残高の 1 行。開始直後から納品で値が動き(140 → 135 → 138)、HUD のラベルと同じ値を示す。
- 配置メニューの初期タブは production(`>_`)。上段が生成 / 品質 / 搬送、下段が電力 / 迎撃 / 防御。crafting タブには senior-engineer、coderabbit、unit-test、e2e-test、jenkins、github-actions の 6 個、power タブには発電 5 個と配線 4 個が並ぶ。新しいアイコン 2 枚は 40px でも判読できる。
- 日本語では `目標 1000 / 残高 143` が同じ位置に出る。文字の大きさは既存の波表ラベル(`ウェーブ 1`)と同じで、英語より小さい(2.3 からの既知の見え方)。
- `k` / `mil` が付く値と、一時停止中の表示は確認していない(ヘッドレスでは Escape で一時停止にならなかった)。
