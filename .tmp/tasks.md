# タスクリスト - AI SDLCゲーム(Mindustry Classic改造)

2026-11-08 作成。`.tmp/design.md` にもとづく。優先順位は、(1) ビルド基盤の更新(GWT と JDK)、(2) 多言語化と日本語対応、(3) ゲーム内容、の順。

## 概要

- 総タスク数: 30
- 推定作業時間: 約 95〜120 時間(Phase 1 のスパイク結果で ±20 時間ぶれる)
- 優先度: Phase 1 と Phase 2 が高。Phase 3 以降は Phase 1 の完了が前提

## タスク一覧

GitHub Issues: https://github.com/goofmint/Mindustry-Classic/issues (2026-11-08 に全タスクを Issue 化。マイルストーンは Phase ごと、ラベルは `phase-1`〜`phase-5`)

### Phase 1: ビルド基盤の更新(JDK 17 / Gradle 8 / libGDX 1.13.1 / GWT 2.11)

#### Task 1.1: 現状の記録と作業ブランチ (Issue #1)

- [x] 作業ブランチを切る(運用変更: Issue ごとに `issue-<番号>-<短い名前>` ブランチを切り PR で `main` に戻す(既定ブランチは 2026-11-08 に master から main へ改名)。単一の `modernize` ブランチは使わない。Issue #1 は `issue-1-baseline`)
- [x] JDK 17 と Gradle 8.14 の有無を確認し、無ければ導入する(`sdkman` か Homebrew)。結果: Gradle 8.14 は SDKMAN、JDK 17 は Homebrew `openjdk@17`(keg-only、`JAVA_HOME` 指定で使う)。詳細は `.tmp/verify-1.1.md`
- [x] 本家 README の「保守終了」の記載を確認し、改造版の README 草稿に「本家からのフォーク、GPL-3.0 継承」の1段落を書く
- [x] README 草稿に「公開物はブラウザ版のみ。`desktop` モジュールは開発時の動作確認とツール実行用で、配布しない」と明記する(2026-11-08 決定)
- **完了条件**: ブランチがあり、`java -version` が 17 を返す
- **依存**: なし
- **推定時間**: 1h

#### Task 1.2: モジュールの整理 (Issue #2)

- [x] `settings.gradle` を `core`、`desktop`、`html` に絞る
- [x] `android/`、`ios/`、`kryonet/`、`server/` ディレクトリを削除する
- [x] ルート `build.gradle` から Android、RoboVM、kryonet 関連の `buildscript` 依存と `project(":android")`、`project(":ios")` ブロックを消す
- [x] `desktop/DesktopLauncher.java` から `KryoClient`、`KryoServer` の登録と import を消す
- [x] `html/HtmlLauncher.java` から `WebsocketClient` の登録を消し、`WebsocketClient.java` を削除する。`html/build.gradle` と `GdxDefinition.gwt.xml` から `gwt-websockets` を外す
- [x] `desktop/build.gradle` の packr 関連タスク(`packrCmd`、`fixMac`、`fixWindows32`、`packrZip`、`clearOut`)を消す
- **完了条件**: `git grep -i kryo` と `git grep -i websocket` が core 以外でヒットしない
- **依存**: 1.1
- **推定時間**: 2h

#### Task 1.3: Gradle 8 への移行(スパイク、時間を区切る) (Issue #3)

- [x] `gradle/wrapper/gradle-wrapper.properties` を 8.14 にする
- [x] ルート `build.gradle` の `compile` を `implementation`/`api` に、`archiveName`/`destinationDir` を `archiveFileName`/`destinationDirectory` に直す
- [x] `desktop/build.gradle` の `task run(type: JavaExec)` を `mainClass.set(...)` 形式に直す
- [x] `html/build.gradle` の `de.richsource.gradle.plugins:gwt-gradle-plugin:0.6` を、gdx-liftoff が生成する `html/build.gradle` と同じ GWT プラグインに差し替える(gdx-liftoff 1.14.0.2 の生成物を参照して書き写す。プラグインの座標とバージョンはこの時点で確定して記録する)
- [x] `./gradlew :core:compileJava` が設定段階のエラーを出さずに進むところまで持っていく(コンパイルエラーは次のタスク)
- **完了条件**: `./gradlew help` と `./gradlew :core:compileJava --dry-run` が通る。壊れた箇所の一覧を `.tmp/spike-gradle.md` に残す
- **依存**: 1.2
- **推定時間**: 4h(上限。超えたら中断して報告)

#### Task 1.4: libGDX 1.13.1 への移行(core のコンパイル) (Issue #4)

- [x] `gdxVersion` を 1.13.1、`aiVersion` を 1.8.2 にする。gdx-tools も 1.13.1 に揃える
- [x] `Vector2.angle()` 28 か所(5 ファイル)を `angleDeg()` に直す。`angleRad()` はそのまま(実際に Vector2 だったのは 3 か所。残り 25 か所は独自の `BulletEntity.angle()` で対象外。`.tmp/spike-libgdx.md` 2.2)
- [x] gdx-controllers の参照(`Inputs.java`、`XboxBinds.java`、`Input.java`、`KeybindDialog.java`、`Control.java`)を消し、`Inputs.useControllers()` と `DeviceType.controller` の分岐を取り除く。`gdx-controllers` の依存を外す
- [x] 残りのコンパイルエラーを 1 件ずつ直す。直した API 差分を `.tmp/spike-libgdx.md` に「旧 API → 新 API」で記録する
- [x] `./gradlew :core:compileJava` と `./gradlew :desktop:compileJava` を通す
- **完了条件**: `./gradlew :desktop:run` でメニューが表示され、既存マップでウェーブが始まる(本家のゲームがそのまま動く)
- **依存**: 1.3
- **推定時間**: 6h(上限 8h)

#### Task 1.5: GWT 2.11.0 への移行(html のコンパイル) (Issue #5)

- [x] `html/build.gradle` に `com.google.jsinterop:jsinterop-annotations:2.0.2:sources` を足す
- [x] `GdxDefinition.gwt.xml` と `GdxDefinitionSuperdev.gwt.xml` の `inherits` を 1.13.1 のバックエンドに合わせる(`gdx-controllers-gwt` の `inherits` を外す)
- [x] `core/src/Mindustry.gwt.xml` の `gdx.reflect.include` から `net.Packets`、`net.Packet`、`net.Streamable` を外す(ネットワークを切ったため)
- [x] GWT コンパイルで出るエラー(エミュレーション外の JRE API、`java.io.DataInputStream` 周りなど)を 1 件ずつ直す。`.tmp/spike-gwt.md` に記録する
- [x] `./gradlew :html:dist` を通し、`html/build/dist` をローカル HTTP サーバーで開いて起動を確認する(Playwright + Chromium で確認。caldera で 1 ウェーブ。詳細は `.tmp/spike-gwt.md` §6)
- **完了条件**: ブラウザで本家のゲームが起動し、既存マップで 1 ウェーブ遊べる。初回ロード時間を記録する。経路計算の秒数は、Task 1.5 では「ウェーブ開始から敵がコア付近(砲台の射程内)に到達するまでの秒数」で代える。観察では 65 秒時点でコア付近への到達と迎撃を確認した(コアそのものへの到達時刻は未計測)。計測コードを入れた厳密な値は Task 5.2 で記録する(2026-11-08 にローカルレビューと PR レビューの指摘を受けて条件を明示的に変更)
- **依存**: 1.4
- **推定時間**: 6h(上限 8h)

#### Task 1.6: CI とビルド手順の固定 (Issue #6)

- [ ] `.github/workflows/build.yml` を作り、JDK 17 で `./gradlew :html:dist` を回して成果物をアップロードする(ファイルは作成済み。GitHub 上での実行は PR を開いた後に確認する。`.tmp/verify-1.6.md`)
- [x] `README.md` に JDK 17 の導入、`./gradlew :desktop:run`、`./gradlew :html:dist`、`./gradlew pack` の手順を書く
- [x] 1.3〜1.5 の `.tmp/spike-*.md` の内容を README か `docs/UPGRADE.md` にまとめる
- **完了条件**: CI が緑。README の手順どおりに新しい環境でビルドできる
- **依存**: 1.5
- **推定時間**: 2h

### Phase 2: 多言語化と日本語対応

#### Task 2.1: 言語の構成を en と ja に絞る (Issue #7)

- [ ] `core/assets/bundles/` から `bundle_de`、`es`、`fr`、`in_ID`、`ita`、`ko`、`pl`、`pt_BR`、`ru`、`tk`、`uk_UA` を削除する
- [ ] `Vars.locales` を `{new Locale("en"), new Locale("ja")}` にする
- [ ] `bundle.properties` に `text.locale.en=English`、`text.locale.ja=日本語` を足し、`LanguageDialog` のボタン表示を `Platform.getLocaleName()` から `Bundles.get("text.locale." + loc)` に変える
- [ ] `BundleLoader.getLocale()` の既定値(`"default"` = OS のロケール)が `ja` のとき `bundle_ja` を読むことをデスクトップで確認する
- **完了条件**: 言語ダイアログに「English / 日本語」の2つだけが出る
- **依存**: 1.4
- **推定時間**: 1h

#### Task 2.2: 美咲フォントを libGDX の形式に変換する (Issue #8)

既存の 8×8 ドット日本語フォント「美咲フォント」(littlelimit.net、2021-05-05 版)を使う。文字を描く作業は無い。libGDX の `BitmapFont` は AngelCode 形式(`.fnt` + `.png`)しか読まないので、配布物(TTF、BDF、PNG)からその形式へ変換するだけ。ライセンスは利用・複製・配布・改変が無制限で表記義務なし(配布ページの font.htm で確認済み)。

- [x] `misaki_ttf_2021-05-05.zip`(ビットマップ埋め込み TTF)と `misaki_bdf_2021-05-05.zip` を取得し、`tools/fonts/misaki/` に置く。同梱のライセンス文を一緒に入れる
- [x] `tools/collect_chars.py`(標準ライブラリのみ)を書く。`bundle_ja.properties` の文字と、ASCII 全印字文字、ひらがな、カタカナ、よく使う記号を集めて `tools/chars_ja.txt` に出す(全収録文字 7,000 字超を入れずに済ませるため)
- [ ] Hiero(gdx-tools に同梱、`java -cp gdx-tools.jar com.badlogic.gdx.tools.hiero.Hiero`)で `misaki_gothic_2nd.ttf` をサイズ 8、アンチエイリアスなし、`chars_ja.txt` の文字だけで `misaki.fnt` + `misaki.png` に書き出す(見送り: Hiero は GUI で自動実行できないため、下の BDF 変換を使った)
- [x] Hiero の描画がにじむ、または埋め込みビットマップを拾わない場合は、BDF から `.fnt` を書く変換スクリプト(`tools/bdf2fnt.py`、標準ライブラリのみ)に切り替える。BDF はドットの並びそのものなので、原寸で確実に出せる
- [x] `core/assets/ui/uiskin.json` の `BitmapFont` に `japanese: { file: misaki.fnt, markupEnabled: true, scale: ... }` を足す。`square.fnt`(本家の英字フォント)と並べて高さが揃う `scale` を決める(ブラウザ描画で比較し scale 2 を採用。詳細は `.tmp/verify-2.2.md`)
- [x] 文字を足したとき(`bundle_ja` を増やしたとき)の再変換手順を `tools/README.md` に書く
- **完了条件**: `misaki.fnt` と `misaki.png` がリポジトリにあり、デスクトップ版で「設計書」「レビュー済」が原寸のドットで(にじまずに)描かれる
- **依存**: 2.1
- **推定時間**: 2h

#### Task 2.3: ロケールによるフォント切り替え (Issue #9)

- [x] `UI.loadSkin()` で、`getLocale().getLanguage()` が `ja` のとき `skin` の `default-font` と `default-font-chat` を `japanese` に差し替える(`Skin.add("default-font", font)` で上書き)。`Core.font` も同じフォントを指すようにする(実装: `default-font-chat` は `japanese-chat`。`Core.font` は `Draw.tscl()` で UI が縮むのを避けるため美咲の別インスタンス。詳細は verify-2.3.md)
- [x] `title` フォント(`title.fnt`)はロゴ用なので触らない。タイトル画面の日本語はラベルで出す
- [x] 既存の `korean` フォントと `korean.fnt/png` を削除する(未使用、確認済み)
- [x] 日本語で UI のレイアウト崩れ(ボタン幅、折り返し)を確認し、`BlocksFragment` の説明欄(`width(120f)`、`width(200)`)を日本語の長さに合わせて調整する(ブラウザで確認。HUD の固定倍率ラベル 4 か所を `UI.labelScale` 経由に変更して解消。詳細は `.tmp/verify-2.3.md`)
- **完了条件**: 言語を日本語にして再起動すると、メニュー、設定、配置メニュー、ブロック情報が日本語フォントで表示される
- **依存**: 2.2
- **推定時間**: 3h

#### Task 2.4: 本家文言の日本語化(bundle_ja.properties) (Issue #10)

- [x] `bundle.properties` の 552 行のうち、残す予定のキー(削除予定のブロック・アイテム・チュートリアル・マルチプレイの文言を除く)を洗い出す
- [x] `bundle_ja.properties` を作り、残すキーをすべて日本語にする。キーはフラット構造のまま、`bundle.properties` と同じ順序で並べる
- [x] `text.blocks.*`(ブロック情報の項目名、44 キー)を日本語にする
- [ ] `tools/check_bundles.py` を書く。en と ja のキー集合の差分を出し、差分があれば終了コード 1 にする。CI に組み込む
  - (2.4 時点) スクリプトは作成済み(除外リストは `tools/bundle_exclude_keys.txt`)。CI は別ブランチの GitHub Actions に移るため、`.github/workflows/build.yml` への組み込みは後で行う。それまで未完了のまま残す。詳細は `.tmp/verify-2.4.md`
- **完了条件**: `check_bundles.py` が差分 0。デスクトップで日本語に切り替えて、メニューから設定まで英語が残っていない
- **依存**: 2.3
- **推定時間**: 4h

#### Task 2.5: 名称の割り当て(本家ブロックの AI SDLC 名) (Issue #11)

- [x] 要件定義の対応表(砲台 12、修復 2、シールド、壁 5、ドア 2、シールド壁、搬送 5、発電 5、配線 4、液体の名称)を `bundle_ja.properties` の `block.<name>.name` に書く
- [x] 同じ対応表の英語名を決めて `bundle.properties` に書く(要件定義の「英語名は日本語が決まってから」に対応。ここで決める)
- [x] `item.igpu.name`、`item.pcie.name`、`liquid.oil.name=vGPU`、`liquid.lava.name=dGPU` を両言語に書く(Item の実体は Phase 3 で足すが、キーは先に用意する)
- [x] 各ブロックの `block.<name>.description`(短い説明)を AI SDLC の文脈で書き直す。`fulldescription` は Phase 4 で書く
- **完了条件**: 配置メニューのすべてのブロックが、両言語で AI SDLC の名前になっている
- **依存**: 2.4
- **推定時間**: 4h

### Phase 3: ゲーム内容の実装(コードアイテムと施設)

#### Task 3.1: 単体テストの土台 (Issue #12)

- [x] `core/build.gradle` に JUnit 4 を `testImplementation` で足し、`core/src/test/java` を `sourceSets.test` にする。GWT の `source path` に入らないことを確認する
- [x] 空のテストを 1 つ置いて `./gradlew :core:test` が通ることを確認する。CI に組み込む
- **完了条件**: `./gradlew :core:test` が緑
- **依存**: 1.6
- **推定時間**: 1h

#### Task 3.2: Item と CodeItem (Issue #13)

- [ ] `Item` の静的フィールドを `credit`、`igpu`、`pcie` に置き換える(旧 8 種は残したままにして、この時点ではまだ消さない)
- [ ] `CodeItem` を設計書 §2.2 のインターフェースで実装する。`registerAll()` は出発点 4 段階からの到達探索
- [ ] `Item` の静的初期化の末尾で `CodeItem.registerAll()` を呼ぶ
- [ ] テスト: 登録数が 70、`advance()` の上限クリップと再通過の恒等、`attrOf(get(a)) == a`、`score()` の値
- [ ] 起動時検査: `Item.getAllItems().size > 127` で `RuntimeException`
- **完了条件**: テストが緑。`./gradlew :desktop:run` が起動する(アイコン未作成のため警告は許容)
- **依存**: 3.1
- **推定時間**: 3h

#### Task 3.3: アイコン生成ツール (Issue #14)

- [ ] `core/assets-raw/sprites/code-base/q<0-6>-i<1-2>.png`(8×8、14 枚)を作る。色は設計書の表、形は丸と星
- [ ] `desktop/src/.../tools/IconGenerator.java` を書く(`javax.imageio` のみ)。右下 (5,7) 白、(6,7) 黒、(7,7) 緑のドットを通過状態に応じて重ね、`icon-code-q*-p*-i*.png` を 70 枚出す
- [ ] `icon-credit.png`、`icon-igpu.png`、`icon-pcie.png` を作る(本家 `icon-coal`、`icon-uranium` を流用してよい)
- [ ] `desktop/build.gradle` に `genIcons` タスクを足し、`./gradlew :desktop:genIcons pack` で `sprites.atlas` を再生成する
- [ ] 起動時検査: 全 Item の `region != null`。欠けていれば名前を列挙して止める
- **完了条件**: 生成物がリポジトリに入り、起動時検査が通る
- **依存**: 3.2
- **推定時間**: 3h

#### Task 3.4: Score と残高 (Issue #15)

- [ ] `Score` を設計書 §2.2 のインターフェースで実装し、`GameState` に `score` と `won` を足す
- [ ] `Inventory.clearItems()` の `addItem(Item.stone, 40)` を `addItem(Item.credit, Balance.INITIAL_CREDIT)` に変える。`Balance.java` を作る
- [ ] `Map` に `targetScore` を足し、`maps.json` の読み込みで入ることを確認する
- [ ] テスト: 端数の繰り越し、納品での 0 割れで `bankrupt()`、建設では立たない、倍率の境界値
- **完了条件**: テストが緑。HUD の既存アイテム欄に credit が 150 と出る
- **依存**: 3.2
- **推定時間**: 3h

#### Task 3.5: Recipes の差し替え (Issue #16)

- [ ] `Recipes` を要件定義の費用表で書き直す。すべて `stack(Item.credit, n)`
- [ ] 削るレシピ(`steelwall`、`diriumwall`、`diriumwalllarge`、`sorter`、`smelter`、`crucible`、`coalpurifier`、`titaniumpurifier`、`oilrefinery`、`stoneformer`、`lavasmelter`、`weaponFactory`、`stonedrill`、`irondrill`、`titaniumdrill`、`omnidrill`)を外す
- [ ] `UpgradeRecipes` を空にする
- [ ] 砲台 12 種を `PowerTurret` ベースに変える(`ammo` を外し、`powerUsed` を設定。本家の `Turret` 定義を `PowerTurret` に置き換える。`LaserTurret` と `waveturret` は元から電力)
- **完了条件**: デスクトップで、残高を使ってブロックが置け、置くと残高が減る。弾薬なしで砲台が電力で撃つ
- **依存**: 3.4
- **推定時間**: 4h

#### Task 3.6: CodeSource(コードを生む設備) (Issue #17)

- [ ] `CodeSource extends Drill implements PowerAcceptor` を実装する
- [ ] `ProductionBlocks` に `human-engineer`、`ai-agent-1/2/3` を定義する(設計書の表の値)
- [ ] `canReplace()` で `CodeSource` 同士の置き替えを許す
- [ ] 8×8 のブロック画像を 4 枚作る(仮絵でよい)
- [ ] 両言語の `block.<name>.name` と `description` を足す
- **完了条件**: 鉄鉱石の床(コード鉱脈)の上に置くとコードが出て、コンベアで運べる。鉱脈の外では `cross` が出る。AI エージェントは電力が無いと止まる
- **依存**: 3.5
- **推定時間**: 3h

#### Task 3.7: QualityGate(品質を上げる施設) (Issue #18)

- [ ] `QualityGate extends PowerBlock` と `GateEntity`(`current`、`progress`、`write/read`)を実装する。変換部分は `GateLogic` に切り出す
- [ ] `ProductionBlocks` に `senior-engineer`、`unit-test`、`e2e-test`、`jenkins`、`github-actions` を定義する
- [ ] 進捗バーと、処理中アイテムの中央描画
- [ ] ブロック画像 5 枚(仮絵)、両言語の文言
- **完了条件**: コードが 1 個ずつ入り、処理後に色とドットが変わって出る。同じ施設を 2 回通しても色が変わらない。電力が無いと止まる(シニア以外)
- **依存**: 3.6
- **推定時間**: 4h

#### Task 3.8: CodeRabbitBlock(変換と迎撃) (Issue #19)

- [ ] `CodeRabbitBlock extends PowerTurret` と `CodeRabbitEntity` を実装し、`GateLogic` を呼ぶ
- [ ] `acceptItem()` を上書きして `QualityGate` と同じ条件にする
- [ ] `WeaponBlocks` に `coderabbit` を定義する(射程 60、リロード 4、stages 2、processTime 3 秒)
- [ ] 砲塔画像 `coderabbit.png`(`laserturret.png` の色替え)、両言語の文言
- **完了条件**: 敵を撃ちつつコードを変換する。電力が尽きると両方止まる
- **依存**: 3.7
- **推定時間**: 3h

#### Task 3.9: CoreBlock と勝敗 (Issue #20)

- [ ] `CoreBlock.handleItem()` を `Score.deliver()` 呼び出しに変える。`Fx.deliverGood/deliverBad` を `Fx` に足す
- [ ] `Logic.update()` に目標到達と残高割れの判定を足す
- [ ] `Control` の `GameOverEvent` ハンドラで `won` のとき爆発演出を出さない
- [ ] `RestartDialog` を勝敗で出し分け、`bestscore<map>` を保存する。文言 `text.victory`、`text.delivered` を両言語に足す
- **完了条件**: 目標に届くと勝ちダイアログ、納品で残高が 0 を下回ると負けダイアログ、コア破壊は従来どおり負け
- **依存**: 3.8
- **推定時間**: 3h

#### Task 3.10: 敵の出現倍率 (Issue #21)

- [ ] `EnemySpawn.evaluate()` の戻り値に `state.score.qualityMultiplier` を掛け、端数を `Mathf.chance()` で切り上げる
- [ ] `WaveCreator` に `target` 型が無いことを確認する(確認済み、変更なし)
- [ ] デバッグ用に HUD へ倍率を出す(`Settings` の `fps` 表示と同じ条件で出す)
- **完了条件**: 品質 -5 を流し続けると次のウェーブの敵が目に見えて増え、品質 1 を流すと減る
- **依存**: 3.9
- **推定時間**: 2h

#### Task 3.11: 旧ブロックと旧アイテムの削除 (Issue #22)

- [ ] `Tutorial` と `tutorial.png`、`MenuFragment` のチュートリアルボタン、`World.loadMap()` の `tutorial` 分岐を削除する
- [ ] `DefenseBlocks`、`ProductionBlocks`、`DistributionBlocks` から削除対象 18 ブロックの定義を消す。`Sorter`、`LiquidCrafter`、`Smelter`、`Omnidrill`、`LiquidDrill`、`WeaponFactory` クラスを消す
- [ ] `Item` の旧 8 種を消し、残った参照(`Conveyor.draw()` の `Item.stone` → `Item.igpu`、`NuclearReactor.generateItem` → `Item.pcie`、`coaldrill`/`uraniumdrill` の `result`)を直す
- [ ] `BlockLoader.defaultMap` と `Save12〜14`、`getByOldID()` を消し、`SaveIO.versionArray` を `Save15` だけにする。起動時検査 `Block.getAllBlocks().size > 127` を足す
- [ ] `World.setDefaultBlocks()` を `human-engineer` と `turret` に置き換える
- [ ] 両言語の `bundle` から削除したブロック・アイテムのキーを消し、`check_bundles.py` を通す
- **完了条件**: `git grep "Item\.stone"` などがヒットしない。起動時検査が通る(ブロック数 88 前後)。セーブとロードで施設の処理中アイテムと残高が戻る
- **依存**: 3.10
- **推定時間**: 4h

### Phase 4: マップ、UI、文言の仕上げ

#### Task 4.1: マップ 3 枚 (Issue #23)

- [ ] 256×256 の PNG を 3 枚描く(`ColorMapper` の色表に従う。コード鉱脈 `c3a490`、iGPU `161616`、PCIe `83bc58`、水 `506eb4`、石油 `292929`、溶岩 `ed5334`、プレイヤー `00ff00`、敵出現 `ff0000`)
- [ ] `maps.json` に 3 枚を登録し、`targetScore` を仮に 1000 / 1500 / 2000 とする。`oreGen: true`
- [ ] 本家マップ(22 枚)を `maps.json` と `core/assets/maps/` から外す
- [ ] 両言語の `map.<name>.name` と `map.<name>.description` を書く
- **完了条件**: レベル選択に 3 枚だけ出て、各マップに鉱脈と燃料の床、液体の床がある
- **依存**: 3.11
- **推定時間**: 4h

#### Task 4.2: HUD と配置メニューの表示 (Issue #24)

- [ ] `BlocksFragment.updateItems()` を credit 1 行の表示に変える
- [ ] `HudFragment` に「目標 / 残高」のラベルを足す
- [ ] 配置メニューの `Section` アイコンと並び順を見直す(`crafting` に品質施設、`production` にコード生成と調達、`power` に発電と配線)
- **完了条件**: HUD で残高と目標が常に見え、配置メニューの分類が説明なしで分かる
- **依存**: 3.11
- **推定時間**: 2h

#### Task 4.3: ブロックの詳細説明と getStats (Issue #25)

- [ ] 新ブロック 10 種の `getStats()` を本家の流儀(`"[craftinfo]Quality Gain: "` など英語直書き)で書き、対応する `text.blocks.*` を両言語に足す
- [ ] 全ブロックの `block.<name>.fulldescription` を AI SDLC の文脈で両言語に書く(CodeRabbit の説明はここで価値を伝える)
- [ ] コードアイテムの表示名: `CodeItem` 側で `localizedName()` を組み立てる(品質 × 重要度 14 通りの `item.code.q<stage>.i<imp>.name` と、通過状態の接尾辞 `text.code.passed.review/test/ci`)
- **完了条件**: ブロック情報ダイアログに英語の直書きが残らず、`check_bundles.py` が差分 0
- **依存**: 4.2
- **推定時間**: 4h

#### Task 4.4: タイトル、About、ブランディング (Issue #26)

- [ ] `html/webapp/index.html` のタイトル、`logo.png`、`styles.css` の配色を差し替える
- [ ] `AboutDialog` に本家クレジット、GPL-3.0、美咲フォントの出典(表記義務は無いが礼儀として)、CodeRabbit の説明を両言語で入れる
- [ ] メニューから Discord、寄付、エディタ(GWT では非対応表示)のボタンを外す
- **完了条件**: タイトル画面に CodeRabbit 以外の外部リンクが無い
- **依存**: 4.1
- **推定時間**: 2h

### Phase 5: 検証と公開

#### Task 5.1: デスクトップでの通し確認 (Issue #27)

- [ ] 設計書 §7.2 のチェックリスト 1〜10 を実施し、結果を `.tmp/verify-desktop.md` に残す
- [ ] 見つかった不具合を直す(別タスクに切るほど大きければ報告)
- **完了条件**: チェックリスト 10 項目すべてに「OK」が付く
- **依存**: 4.4
- **推定時間**: 4h

#### Task 5.2: ブラウザでの通し確認 (Issue #28)

- [ ] `./gradlew :html:dist` を回し、ローカル HTTP サーバーで §7.2 のチェックリストを再実施する
- [ ] 256×256 マップの初回ロード時間、経路計算が終わるまでの秒数、1 ウェーブ目の FPS を記録する
- [ ] 日本語フォントの描画(にじみ、欠け)を確認する
- [ ] 重ければ設計書 §8.2 の対策(マップ縮小、`Router.tryDump` の最適化)を判断して報告する
- **完了条件**: ブラウザで最初から勝利までの 1 プレイが通る。計測値が `.tmp/verify-web.md` にある
- **依存**: 5.1
- **推定時間**: 4h

#### Task 5.3: バランス調整 (Issue #29)

- [ ] 3 マップを通しで遊び、費用、処理時間、電力、倍率の係数、初期資金、目標得点を `Balance.java` と `maps.json` で調整する
- [ ] 「CodeRabbit を置くと楽になる」が 5 分以内に体感できるかを確認する(イベントでの利用場面を想定)
- [ ] 調整した値と理由を `docs/BALANCE.md` に残す
- **完了条件**: 初見の人が 1 マップ目を 10〜15 分でクリアできる
- **依存**: 5.2
- **推定時間**: 6h

#### Task 5.4: 公開 (Issue #30)

- [ ] GitHub Pages への配置を CI に足す(`:html:dist` の成果物を `gh-pages` に置く)
- [ ] `README.md` を仕上げる(遊び方、ビルド、ライセンス、本家へのクレジット)
- [ ] `LICENSE` が GPL-3.0 のままであることを確認し、変更したファイルの著作権表記を整える
- **完了条件**: 公開 URL でブラウザから遊べる
- **依存**: 5.3
- **推定時間**: 2h

## 実装順序

1. **Phase 1 を最初に、順番どおりに**(1.1 → 1.6)。1.3〜1.5 はスパイクで、上限時間を超えたら中断して報告する。ここが通らなければ以降の作業はすべて止まる
2. **Phase 2 は 1.4 が通った時点で着手できる**(2.1 以降はデスクトップで確認できる)。1.5 の GWT 作業と並行できる
3. **Phase 3 は 1.6 と 2.5 の後**。3.2 → 3.3 → 3.4 → 3.5 は直列。3.6 → 3.7 → 3.8 も直列(前の施設でコードの流れを確認してから次へ)
4. **Phase 4 の 4.1(マップ)は 3.6 の後なら並行できる**(マップ描きはコードに依存しない)
5. **Phase 5 は直列**

```
1.1 → 1.2 → 1.3 → 1.4 → 1.5 → 1.6 ─────────────┐
                     └→ 2.1 → 2.2 → 2.3 → 2.4 → 2.5 ┤
                                                    ▼
                                      3.1 → 3.2 → 3.3 → 3.4 → 3.5 → 3.6 → 3.7 → 3.8 → 3.9 → 3.10 → 3.11
                                                                      └→ 4.1(並行可)          │
                                                                                             ▼
                                                                                   4.2 → 4.3 → 4.4 → 5.1 → 5.2 → 5.3 → 5.4
```

**クリティカルパス**: 1.3 → 1.4 → 1.5(ビルド基盤)。ここで libGDX や GWT の非互換が想定より多いと、全体が後ろにずれる。1.4 で直す API 差分の目安は、`Vector2.angle()` が 28 か所、gdx-controllers が 5 ファイル(確認済み)。それ以外は、やってみないと分からない。

## リスクと対策

- **libGDX 1.9.8 → 1.13.1 の非互換が多い**: 1.4 を 8 時間で区切り、超えたら「libGDX は 1.9.8 のまま、GWT だけ 2.8.2 に上げる」縮退案を提示する。GWT 2.8.2 は JDK 11 までの実行なら動く報告があるため、JDK 17 は諦めることになる
- **GWT の JRE エミュレーションに無い API を本家が使っている**: `Save15` の `DataInputStream/DataOutputStream` は本家の HTML 版でも動いていたので問題ないはず。`java.util.Locale` の扱いと `String.format` は要注意。1.5 で洗い出す
- **GWT Gradle プラグインのバージョン**: 今回の調査では gdx-liftoff が使うプラグインの座標とバージョンを確定できなかった。1.3 で gdx-liftoff 1.14.0.2 の生成物を見て確定し、`.tmp/spike-gradle.md` に記録する
- **日本語フォントの変換結果がにじむ**: 美咲フォントは 8×8 の原寸で使う前提。Hiero の出力がぼやけるときは BDF からの直接変換に切り替える(Task 2.2)。テクスチャは使用文字に絞れば 512×512 程度に収まる見込み
- **アイテム 73 種による `tryDump` の走査コスト**: 設計書 §8.2 のとおり、初期実装では触らず、5.2 の計測で判断する
- **ブロック ID 127 上限**: 88 個で運用。起動時検査で超過を止める
- **セーブ互換**: 旧セーブ(`Save12〜14`)は切る。新規公開なので影響なし
- **要件定義の「複雑さは求めない」との乖離**: 施設と砲台の種類が多い(ブロック 88 個)。5.3 のバランス調整で、最初のマップでは配置メニューに出すブロックを絞る案(`Recipe.desktopOnly` と同じ仕組みでマップごとの表示制限)を検討する

## 注意事項

- 各タスクはコミット単位で完結させる。コミットメッセージにタスク番号を入れる(例: `[1.4] libGDX 1.13.1 へ更新`)
- 動いている機能は変えない。タスクに書かれた範囲だけを直し、「ついでに」の修正はしない
- フォールバック値は入れない。設定やアイコンが無ければ起動時検査で止める
- 言語ファイルはフラット構造を守り、en と ja に同時に足す。`check_bundles.py` を各コミット前に回す
- 数値(費用、時間、電力、倍率)はすべて `Balance.java` と `maps.json` に置き、各所に直書きしない
- 不明点は実装前に確認する。特に 1.3〜1.5 のスパイクで想定外の非互換が出たら、直す前に報告する

## 実装開始ガイド

1. このタスクリストに従って順次実装を進めてください
2. 各タスクの開始時に、このファイルのチェックボックスを進捗管理に使ってください(このセッションでは TodoWrite ツールが使えないため)
3. 完了時はチェックを付け、完了条件を満たした証拠(コマンドの出力、スクリーンショットの場所)を `.tmp/verify-*.md` に残してください
4. 問題発生時は速やかに報告してください。特に Phase 1 の上限時間超過は、縮退案の判断が要るため必ず報告してください
