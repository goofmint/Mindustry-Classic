# Issue #78 検証記録: チュートリアルが必要

## 概要
- 日時: 2026-11-09
- ブランチ: `issue-78-guide`(`origin/main` の `c7ac683` から分岐)
- 内容: Prototype マップで実際に操作しながら進む、非モーダルのチュートリアルを追加した。CodeRabbit の計画(3 フェーズ)に沿って実装し、英語と日本語のブラウザ版で通しの動作を確認した
- 環境: macOS 27.2(arm64)、OpenJDK 17、Gradle wrapper、Playwright + ヘッドレス Chromium(`--use-angle=swiftshader`)。ビューポートは 1280×800 と 1024×640(ゲームの canvas は 4:3 なので、実際の描画は 1066×800 と 853×640)
- デスクトップ版はディスプレイが無いので起動していない(`./gradlew :desktop:compileJava` でコンパイルだけ確認)

## 変更点

### Phase 1: 手順モデル、イベント、コントローラ
- `core/src/io/anuke/mindustry/game/TutorialStep.java`(新規): 目的 → 生産ライン → 合流コンベアの撤去 → CodeRabbit の選択 → 配置 → 一時停止の解除とプラスの納品 → 完了、の 7 手順と「案内不能」
- `core/src/io/anuke/mindustry/game/TutorialModel.java`(新規): libGDX にも world にも依存しない遷移モデル。コア相対座標と `Block.id` で入力を受け、合わない入力と重複通知は無視する。手順 3 に入るときは合流タイルの状態で先へ飛ばす(空なら手順 4、CodeRabbit 済みなら手順 6)。初期配置(合流点のコンベア、手前のユニットテスト、左右のコンベア)が合わなければ案内不能。一時停止の所有(`ownsPause` / `shouldResume`)と残高の警告(`lowBalance`)もここで持つ
- `core/src/io/anuke/mindustry/game/TutorialController.java`(新規): `Control` が 1 回だけ生成し、`Events.on` も生成時に 1 回だけ登録する。`ResetEvent`、`GameOverEvent`、menu への遷移、`Net.active()` でセッションを無効化する。`begin()` は `logic.play()` の直後に `StateChangeEvent` のハンドラの外で呼ばれ、初期配置を検証してから `state.set(State.paused)` して所有を記録する。完了とスキップで `tutorialdone` を `Settings.putBool` + `Settings.save()`。合流タイルなどは毎回 `world` から計算する
- `core/src/io/anuke/mindustry/game/EventType.java`: `PlaceEvent(int x, int y, int block, int rotation)`、`BreakEvent(int x, int y, int block)`、新規 `DeliverEvent(int item, float score)`
- `core/src/io/anuke/mindustry/input/InputHandler.java`: `placeBlock` / `breakBlock` の `!Net.client()` 側で発火。`DesktopInput` と `AndroidInput` の撤去と配置はすべてこの 2 つを通るので、他の経路は変えていない。マップ生成とセーブ読み込みは `Placement` と `setBlock` を直接呼ぶので発火しない
- `core/src/io/anuke/mindustry/world/blocks/types/defense/CoreBlock.java`: `state.score.deliver` の直後に `DeliverEvent` を発火
- `core/src/io/anuke/mindustry/core/Control.java`: `tutorialdone` / `tutorialprompted` の既定値、`TutorialController` の生成と `tutorial()`、`startTutorial()`(Waves、Normal、Prototype を `playMap()` と同じ流れで開き、同じタスク内の `logic.play()` の直後に `begin()`)、毎フレームの `tutorial.update()`
- `core/src/test/java/io/anuke/mindustry/game/TutorialModelTest.java`(新規、22 件)

### Phase 2: 表示とメニュー
- `core/src/io/anuke/mindustry/ui/fragments/TutorialFragment.java`(新規): 画面右上の非モーダルなパネル。外側と背景は `Touchable.childrenOnly`、ラベルは `Touchable.disabled` なので、本文の下のタイルにもブロックを置ける。「次へ」「スキップ」「閉じる」だけ押せる。本文は毎フレーム `Bundles.format` で組み立て、キー名は `KeybindDialog.inputName(KeyBinds.get(...))` で現在のバインドから取る。手順 4〜5 で残高が CodeRabbit の価格(レシピから取る)に足りなければ警告を足す
- `core/src/io/anuke/mindustry/core/UI.java`: `tutorialfrag` を `init()` で構築
- `core/src/io/anuke/mindustry/core/Renderer.java`: `drawOverlay()` から `drawTutorialHighlights()`。手順 3 と 5 は合流タイル、手順 2 はエンジニア 2 台、ユニットテスト、コアを点滅するアウトラインで示す。設定中ブロックのアウトラインと同じ `Lines.square`
- `core/src/io/anuke/ucore/scene/ui/KeybindDialog.java`: `inputName()` を `public static` に
- `core/src/io/anuke/mindustry/ui/fragments/MenuFragment.java`: non-mobile の Play メニューに「チュートリアル」(`icon-tutorial`)
- `core/src/io/anuke/mindustry/ui/dialogs/LevelDialog.java`: 初めて開いたとき `ui.showConfirm` で案内し、どちらを選んでも `tutorialprompted` を保存

### Phase 3: 文言、フォント
- `core/assets/bundles/bundle.properties` / `bundle_ja.properties`: `text.tutorial*`(ボタン、題名、初回案内)と `tutorial.*`(手順 1〜8、手順 3 の補足、残高の警告)。`python3 -I tools/check_bundles.py` は OK
- `tools/chars_ja.txt`、`core/assets/ui/misaki.fnt`、`misaki.png`: `collect_chars.py` → `bdf2fnt.py` → `check_fnt.py` で再生成。845 コードポイント、831 字、256×256、重なりなし。不足は以前から無いかな範囲の文字(U+3094 など)だけで、追加した文言の字はすべてある

## 自動テストとビルド
- `./gradlew :core:test :desktop:compileJava`: 176 件すべて成功(既存 154 + `TutorialModelTest` 22。main の #79 を取り込む前は 174 件だった)
- `./gradlew :html:dist`: 成功。`Events.fire()` は GWT でも `ClassReflection` で `handle` を探すので、新しい `DeliverEvent` を含めてブラウザで実際に発火することを下の手順で確認した
- `core/assets/crash-report-*.txt` は出ていない

## ブラウザでの確認

### 手順
1. `html/build/dist` を `ThreadingHTTPServer` で 127.0.0.1:8768 に配信し、Playwright で操作した(操作スクリプト `load78.mjs` はリポジトリ外の作業用ディレクトリにあり、コミットしていない。左右クリック、キー、待ち、スクリーンショット、`localStorage` の読み書きができる)。証跡のスクリーンショットはこのリポジトリの `.tmp/verify-issue-78/` にコミットしてある(PNG のため CodeRabbit のレビュー対象からは除外される)。ブラウザは常に 1 つだけ動かした
2. 日本語は `localStorage` の `io.anuke.moment:locales=ja` を読み込み前に入れて切り替えた(GwtPreferences の保存形式)。`tutorialdone` と `tutorialprompted` は `io.anuke.moment:tutorialdoneb` / `tutorialpromptedb` を読んだ
3. 1280×800 の座標: Play (668,363) → チュートリアル (638,343)。パネルの「次へ」(970,212)、「スキップ」/「閉じる」(1093,212)(日本語は y=208)。合流コンベア (640,463) を右クリックで撤去、クラフトタブ (1055,490) → CodeRabbit (1027,600)、(640,463) を左クリックで配置、Space で解除
4. 1024×640 の座標: Play (512,283) → チュートリアル (510,262)、「次へ」(734,212)、合流コンベア (512,382)、クラフトタブ (820,330) → CodeRabbit (792,440)

### 結果

| # | 確認項目 | 英語 | 日本語 | 証跡 |
|---|---|---|---|---|
| 1 | Play メニューに「チュートリアル」が出る | OK | OK | `01`、`17` |
| 2 | 開始すると Prototype が一時停止で始まり、パネル(1/7)が右上に出る。左上の HUD、上部中央の「一時停止中」、右下のブロックパネルに重ならない | OK | OK | `02`、`18` |
| 3 | 手順 2 でエンジニア 2 台、ユニットテスト、コアが強調される | OK | OK(別の撮影) | `03` |
| 4 | 手順 3 で合流コンベアが強調され、右クリックで撤去すると手順 4 に進む | OK | OK | `04`、`20` |
| 5 | クラフトタブで CodeRabbit を選ぶと手順 5、強調タイルに置くと手順 6(残高 200 → 100) | OK | OK | `06`、`07`(日本語は `22` の前段で同じ流れ) |
| 6 | Space で解除し、最初のプラスの納品で手順 7(完了)。`tutorialdone=true` が保存される | OK(残高 102 で完了) | OK(残高 102) | `08`、`22` |
| 7 | 「閉じる」でパネルが消え、プレイヤーが解除した一時停止なのでゲームは動いたまま | OK | OK | `09` |
| 8 | パネルの本文の下(720,200)にパイプラインを置ける(残高 200 → 199、手順は 4 のまま) | OK | — | `10` |
| 9 | プレイヤーが解除して再び一時停止したあとのスキップ: パネルが消え、一時停止のまま。`tutorialdone=true` | OK | — | `11` |
| 10 | 所有が続いている状態のスキップ: ゲームが再開する(一時停止アイコンが再生中に変わり、タイマーが進む) | OK | — | `12` |
| 11 | 初めて New Game を開くと確認が手前に出る。キャンセルで戻り、`tutorialprompted=true`。もう一度開いても出ない。OK を押すと開始する | OK | OK(表示のみ) | `13`、`23` |
| 12 | HUD のメニュー → 終了 → 確認で menu に戻るとパネルが消え、もう一度始めると新しいセッション(1/7、一時停止)になる | OK | — | `15` |
| 13 | 1024×640 でも HUD、「一時停止中」、ブロックパネル、選択中に出るレシピ説明に重ならず、手順 1〜7 を通せる | OK | OK(手順 1) | `24`、`26`、`27` |
| 14 | ブラウザのコンソールに例外が無い(`pageerror` 0) | OK | OK | 各 run の `console.log` |

### 証跡(`.tmp/verify-issue-78/`、幅 640 に縮小)

| ファイル | 内容 |
|---|---|
| `01-en-playmenu.png` | Play メニューの「チュートリアル」 |
| `02-en-step1.png` 〜 `04-en-step3.png` | 手順 1〜3(一時停止、強調表示) |
| `06-en-step5.png`、`07-en-step6.png`、`08-en-step7.png` | CodeRabbit の選択、配置、完了 |
| `09-en-closed.png` | 「閉じる」のあと |
| `10-en-under-panel.png` | パネルの下にパイプラインを置いた(残高 199) |
| `11-en-skip-after-unpause.png` | 解除 → 再一時停止 → スキップ(一時停止のまま) |
| `12-en-skip-owned.png` | 所有中のスキップ(再開) |
| `13-en-prompt.png` | 初回の確認 |
| `15-en-pausemenu-over-panel.png` | HUD のメニューからの終了 |
| `17-ja-playmenu.png`、`18-ja-step1.png`、`20-ja-step3.png`、`22-ja-step7.png` | 日本語 |
| `23-ja-prompt.png` | 日本語の初回の確認 |
| `24-small-en-step1.png`、`26-small-en-step7.png`、`27-small-ja-step1.png` | 1024×640 |

## 計画との差分
- `PlaceEvent` / `BreakEvent` / `DeliverEvent` は `Block` や `Item` ではなく `Block.id` / `Item.id` を渡す。`Events.fire()` は引数の実行時クラスで `handle` を探すので、`Conveyor` などのサブクラスのインスタンスを `Block` 型の引数に渡すと見つからない。`BreakEvent` には撤去前のブロックの id を足した
- パネルは上部中央ではなく右上(上端から 52dp)に置いた。上部中央だと 1280 幅でも左上の目標と残高に重なり、右上の上端だと「一時停止中」に重なったため。幅は 460dp と画面幅の 48% の小さいほうを毎フレーム見直す(ブラウザ版は起動後に canvas が 4:3 に縮むので、build() 時の幅では 1024×640 で HUD に重なった)。本文には 6 行分の最小の高さを持たせ、手順が変わってもボタンが動かないようにした。英語の手順 1、2、3、5、7 は狭い幅でも 6 行に収まる長さに言い換えた(内容は同じ。手順 7 の「最初は砲台に任せていい」は英語では省き、日本語には残っている)
- 点滅は `Timers.time()` ではなく実時間(`TimeUtils.millis()`)で回す。一時停止中は `Timers` が進まないため
- `startTutorial()` は読み込み画面を同じタスクの中で閉じる。`playMap()` のように 8 フレーム後のタスクで閉じると、`begin()` の一時停止で `Timers` が止まり、読み込み画面が残る
- 初回の確認は `Timers.run(1f)` で 1 フレーム遅らせる。`LevelDialog` の `shown()` はダイアログが scene に載る前に呼ばれ、確認が後ろに隠れた(同じファイルの削除の確認と同じ対処)
- 手順 5 で選択を外すと手順 4 に戻す(計画には無い小さな追加。外したまま「置く」案内が残らないようにした)
- 合流タイルは `flipBase` の符号を使って計算するので、反転した基地でも配置が合えば案内できる。テストの「反転した初期配置」は、符号を誤って反対側を見た(何も無い)場合に案内不能になることを確認している
- Escape は一時停止中は「再開」になる既存の動作のため、チュートリアルの一時停止中に Escape を押すとゲームが動き出す(パネルは残り、所有は解除される)。menu への遷移は HUD のメニューボタンから確認した

## 未確認のこと
- デスクトップ版の実行(ディスプレイが無い。コンパイルのみ)
- チュートリアル中のセーブ読み込み(`SaveIO` → `logic.reset()` → `ResetEvent` で終わる経路。コードで確認、実機では未確認)
- キーバインドを変えたあとの表示の変化(既定の「マウス右」「マウス左」「Space」「L-Ctrl」が Controls 画面と同じ `inputName()` で出ることだけ確認)
- 残高が 100 未満のときの警告、案内不能の表示、ゲームオーバーによる終了、ホスト開始による終了(いずれも実機では未確認。`TutorialModelTest` で遷移だけ確認)
- 人が最初から最後まで操作した通しプレイ
- モバイル(Android)の入力。計画どおり対象外で、ボタンも non-mobile のメニューにだけ出す
