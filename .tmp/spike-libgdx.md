# Task 1.4 スパイク記録: libGDX 1.13.1 への移行(core のコンパイル)

ブランチ: `issue-4-libgdx13`(`issue-3-gradle8` から分岐)
結論: JDK 17 で `./gradlew :core:compileJava` と `./gradlew :desktop:compileJava` が `BUILD SUCCESSFUL`。`./gradlew :desktop:run` は当初、arm64 の Mac で libGDX と無関係の理由(java-discord-rpc の JNA 4.4.0 に arm64 ネイティブが無い)で起動直後に落ちた。オーケストレーターの判断で Discord RPC を desktop から削除し(第 3 節)、その後の起動では 45 秒間例外なしで動き続けた。画面での確認はユーザーの手動確認待ち(第 6 節)。

## 1. 前提

| 項目 | 値 |
|---|---|
| JAVA_HOME | `/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`(17.0.21、aarch64) |
| Gradle | 8.14(ラッパー、Task 1.3) |
| `./gradlew help` | 変更前・変更後とも `BUILD SUCCESSFUL` |
| libGDX | 1.9.8 → **1.13.1**(`gdxVersion`) |
| gdx-ai | 1.8.1 → **1.8.2**(`aiVersion`) |
| gdx-tools(buildscript、`pack` 用) | 1.9.8 → **1.13.1**。`./gradlew help` で `core/build.gradle` の `import com.badlogic.gdx.tools.texturepacker.TexturePacker` が解決されることを確認(`pack` は実行していない) |
| LWJGL(lwjgl3 バックエンド経由) | 3.3.3(`natives-macos-arm64` を含む) |
| `sourceCompatibility` | 1.8 のまま(Task 1.5 へ持ち越し。第 3 節) |

依存解決(`./gradlew :core:dependencies --configuration compileClasspath | grep gdx`):

```
+--- com.badlogicgames.gdx:gdx:1.13.1
|    \--- com.badlogicgames.gdx:gdx-jnigen-loader:2.5.2
\--- com.badlogicgames.gdx:gdx-ai:1.8.2
     \--- com.badlogicgames.gdx:gdx:1.9.8 -> 1.13.1 (*)
```

gdx-ai 1.8.2 の POM は gdx 1.9.8 を要求するが、Gradle は 1.13.1 を選んでいる。

gdx-controllers の依存はすべて外した(ルート `build.gradle` から以下を削除)。
- core: `api "com.badlogicgames.gdx:gdx-controllers:$gdxVersion"`
- desktop: `implementation "com.badlogicgames.gdx:gdx-controllers-lwjgl3:$gdxVersion"`
- html: `gdx-controllers:$gdxVersion:sources`、`gdx-controllers-gwt:$gdxVersion`、`gdx-controllers-gwt:$gdxVersion:sources`

`gdx-platform:$gdxVersion:natives-desktop` は残した。

`java-discord-rpc`(`com.github.MinnDevelopment:java-discord-rpc:v2.0.1`)は jitpack から解決できる(`discord-rpc-release:v3.3.0` と `jna:4.4.0` を引く)。ただし実行時に arm64 で落ちる(第 5 節)。

## 2. 旧 API → 新 API 一覧

### 2.1 libGDX の API 変更への対応(コンパイルエラーの修正)

| ファイル:行 | 旧 | 新 | 備考 |
|---|---|---|---|
| `core/src/io/anuke/mindustry/core/Control.java:48` | `Gdx.input.setCatchBackKey(true)` | `Gdx.input.setCatchKey(Keys.BACK, true)` | 1.9.10 で置き換え |
| `core/src/io/anuke/mindustry/core/Control.java:21` | `import io.anuke.ucore.modules.*;` のみ | `import io.anuke.ucore.modules.Module;` を追加 | JDK 9 以降の `java.lang.Module` と曖昧になるため(libGDX ではなく JDK 17 起因)。この 1 件で `@Override`・`delta()`・`Mindustry.module(...)` の連鎖エラー 7 件も消えた |
| `core/src/io/anuke/ucore/core/Inputs.java:18` | `InputAdapter.scrolled(int amount)`: `scroll = -amount` | `scrolled(float amountX, float amountY)`: `scroll = -(int)Math.signum(amountY)` | `amountX` は無視。`int` の契約(-1/0/1)を守る。`return false` とフレーム末尾のリセットは変更なし |
| `core/src/io/anuke/ucore/scene/Scene.java:409` | `scrolled(int amount)` | `scrolled(float amountX, float amountY)`。先頭で `int amount = (int)Math.signum(amountY);` | `InputEvent.setScrollAmount(amount)`・`scrollFocus`・handled の扱いは変更なし。`InputListener.scrolled(InputEvent, float, float, int)` を受ける側(ScrollPane、Window、KeybindDialog)は無変更 |
| `core/src/io/anuke/ucore/graphics/DummyBatch.java:35` | `setColor(float color)`(`Batch` の実装) | `setPackedColor(float color)` | `Batch.setColor(float)` が `setPackedColor(float)` に改名 |
| `core/src/io/anuke/ucore/graphics/CacheBatch.java:82-83` | `setColor(float color){ cache.setColor(color); }` | `setPackedColor(float color){ cache.setPackedColor(color); }` | `SpriteCache` 側も同じ改名 |
| `core/src/io/anuke/ucore/graphics/CacheDrawBatch.java:42` | `setColor(float color)` | `setPackedColor(float color)` | |
| `core/src/io/anuke/ucore/graphics/Draw.java:75` | `batch.setColor(NumberUtils.intBitsToFloat(color))` | `batch.setPackedColor(...)` | |
| `core/src/io/anuke/ucore/scene/style/SpriteDrawable.java:53,70` | `batch.setColor(batchColor)`(float) | `batch.setPackedColor(batchColor)` | |
| `core/src/io/anuke/ucore/scene/style/TiledDrawable.java:99` | `batch.setColor(batchColor)`(float) | `batch.setPackedColor(batchColor)` | |
| `core/src/io/anuke/ucore/scene/Skin.java:228` | `((AtlasRegion)region).splits` | `((AtlasRegion)region).findValue("split")` | 1.9.13 から `AtlasRegion` の `splits`/`pads` フィールドが `names`/`values` + `findValue()` に変わった |
| `core/src/io/anuke/ucore/scene/Skin.java:231` | `((AtlasRegion)region).pads` | `((AtlasRegion)region).findValue("pad")` | |
| `core/src/io/anuke/ucore/scene/Skin.java:276` | `region.splits != null` | `region.findValue("split") != null` | |

コンパイルエラーは 1 回目で 22 件(上記と連鎖分)、修正後の 2 回目で 0 件。`DesktopLauncher.java`(Lwjgl3 の設定 API、`setWindowIcon`)、`Atlas.java`、`Pixmaps.java`、`Base64Coder`、`Gdx.net`、`Array`、`Texture`/`Pixmap` のコンストラクタはエラーにならず、無変更。`touchCancelled` の追加も不要だった(`Scene` などは `InputAdapter` 系を継承しているか、インターフェースの既定実装で済んでいる)。

### 2.2 `Vector2.angle()` → `angleDeg()`

1.13.1 でも `Vector2.angle()` は非推奨として残っていて、中身は `angleDeg()` と同じ(反時計回り、度、0〜360)。コンパイルエラーではないが、設計 §10 の方針どおり、受け手が `com.badlogic.gdx.math.Vector2` の 3 か所だけ直した。

| ファイル:行 | 旧 | 新 |
|---|---|---|
| `core/src/io/anuke/mindustry/entities/Player.java:211` | `movement.angle()` | `movement.angleDeg()` |
| `core/src/io/anuke/mindustry/entities/enemies/EnemyType.java:147` | `enemy.velocity.angle()` | `enemy.velocity.angleDeg()` |
| `core/src/io/anuke/ucore/entities/BulletEntity.java:95`(`BulletEntity.angle()` の実装) | `velocity.angle()` | `velocity.angleDeg()` |

設計書の「28 か所(5 ファイル)」の内訳(変更前の `git grep "\.angle()"`): Vector2 が上の 3 か所、残り 25 か所は独自メソッド `BulletEntity.angle()` の呼び出し `b.angle()`(`BulletType.java` 23、`BaseBulletType.java` 2)。こちらは libGDX の API ではないので変えていない。`Angles.angle(x, y, x2, y2)`(独自、4 か所)も対象外。

### 2.3 gdx-controllers の削除(ゲームパッド対応を切る)

| ファイル | 変更 |
|---|---|
| `core/src/io/anuke/ucore/core/Inputs.java` | controllers の import、`useControllers` とそのメソッド、`loadControllers()`(接続・切断リスナー、軸・POV・ボタンのデバッグログ)、`findBy()`、`update()` の軸・POV のスナップショット、`keyDown`/`keyTap`/`keyRelease` のコントローラー分岐、`getAxis` のコントローラー分岐、`getControllerType()`、`ControllerType`、`InputDevice` のコントローラー用フィールドとコンストラクタ、`DeviceType.controller` を削除。コントローラー専用だった `debug`/`setDebug()` も削除(呼び出し元なし)。共有マルチプレクサ、キー離しの記録、マウスボタンの押下エッジ判定、キーボードの `Input.SCROLL` 軸は残した。デバイス一覧はキーボードのみ |
| `core/src/io/anuke/ucore/util/Input.java` | `PovDirection` 依存、`pov`/`direction` フィールド、POV 用コンストラクタ、`findPOV()` を削除。コントローラー定数 21 個は **名前と位置をそのまま残し**(コード `-1`、`Type.controller`、軸フラグは元のまま)、予約枠とした。保存済みキー設定は `ordinal()` で保存されるため。確認: `MOUSE_LEFT`=21、`SCROLL`=26、`UNSET`=182、要素数 183(変更前と同じ) |
| `core/src/io/anuke/ucore/util/XboxBinds.java` | **削除** |
| `core/src/io/anuke/ucore/scene/ui/KeybindDialog.java` | コントローラーのリスナー、import、`canRebindController()`、コントローラー種別の表示、コメントアウトされたコントローラー参照 2 か所を削除。`openDialog()` の `if(section.device.type == DeviceType.keyboard)` は常に真になるので外した(中身は同じ)。再割り当て、スクロールの軸割り当て、Escape、リセット、デバイス切り替えの矢印は残した |
| `core/src/io/anuke/mindustry/core/Control.java` | `Inputs.useControllers(!gwt)`、仮想カーソル(`controlx`/`controly`/`controlling`)、合成タッチイベント、デッドゾーン判定、`setCursorCatched` の毎フレーム呼び出し、`InputProxy` の生成と `update()` での差し替え、`showCursor()` を削除。`triggerInputUpdate()` は Logic から呼ばれるので空のメソッドとして残した |
| `core/src/io/anuke/ucore/util/InputProxy.java` | **削除**(他の参照なしを確認) |
| `core/src/io/anuke/mindustry/core/UI.java` | `controller-cursor` の描画ブロックを削除(`showCursor()` が false の場合と同じ) |
| `core/src/io/anuke/mindustry/input/GestureHandler.java` | `pan()` の `showCursor()` 判定を削除(false の場合の式に簡約)。未使用になった `Inputs` の import を削除 |
| `core/src/io/anuke/mindustry/input/PlaceMode.java:239` | `mobile && !Gdx.input.isTouched(0) && !control.showCursor()` → `mobile && !Gdx.input.isTouched(0)` |
| `core/src/io/anuke/mindustry/input/DefaultKeybinds.java` | `DeviceType.controller` の既定割り当てを削除 |
| `core/src/io/anuke/mindustry/input/DesktopInput.java` | ズーム条件からコントローラー分岐を削除(Ctrl + スクロールのズームとスクロール回転は同じ)。未使用の import 2 つを削除。ローカル変数 `showCursor`(手のカーソル)はそのまま |

## 3. 判断事項

- **`sourceCompatibility` は 1.8 のまま**: Task 1.3 のスパイクで「1.4 で 17 へ」と書かれていたが、今回の Task 1.4 のチェックリストと指示に含まれず、コンパイルにも不要だったため変えなかった。JDK 17 の javac で `-source 8 -target 8` のまま JDK 17 のクラスライブラリに対してコンパイルされる(`--release` 未指定)。このため `java.lang.Module` が見え、`Control.java` の曖昧エラーが出た。**オーケストレーターの判断: 1.8 のまま。GWT 2.11 で使える言語レベルが確定する Task 1.5 で判断する(1.5 へ持ち越し)**
- **スクロール量の扱い**: 1.13.1 の lwjgl3 バックエンドは `scrolled(amountX, amountY)` に float(トラックパッドでは小数)を渡す。既存の利用側(ズーム、回転、チャット、マップエディタ、ScrollPane)は整数 ±1 を前提にしているので、符号だけを取って -1/0/1 にした。1.9.8 の lwjgl3 バックエンドも 1 イベントにつき ±1 を渡していたので、マウスホイールでは挙動は同じ。トラックパッドの小さな量も ±1 になるため、1.9.8 より敏感に感じる可能性はある(未確認)。**オーケストレーターの判断: 変更不要。手動確認項目として残す(第 6 節)**
- **`Input` のコントローラー定数を予約枠として残した**: 消すと後ろの定数の `ordinal()` がずれ、既存のキー設定(`keybind-default-keyboard-*`)が別のキーに化ける
- **`DeviceType.controller` を消した影響**: `KeyBinds` は `DeviceType.values()` を回すだけなので無変更でよい(`KeyBinds.java` は触っていない)。保存済みの `keybind-default-controller-*` は読まれなくなるだけ。`<section>-last-device-type` が 1 以上でも `Mathf.clamp` で 0(キーボード)になる
- **非推奨 API はそのまま**: `-Xlint:deprecation` で 35 件の警告(第 6 節)。いずれも 1.13.1 で動作は同じなので、最小変更の方針により直していない
- **Discord RPC を desktop から削除(オーケストレーターの判断、案 (b))**: 理由は、desktop は開発用モジュールで配布しないこと、設計書 Task 4.4 で Discord 系リンクを削除する予定であること、arm64 で起動できない害の方が大きいこと。変更内容:
  - ルート `build.gradle` の `project(":desktop")` から `implementation 'com.github.MinnDevelopment:java-discord-rpc:v2.0.1'` を削除(`jna:4.4.0`、`discord-rpc-release:v3.3.0` も依存から消えた)
  - `desktop/src/io/anuke/mindustry/desktop/DesktopPlatform.java`: `club.minnced.discord.rpc.*` の import、`useDiscord`(Discord 専用)と `applicationId` のフィールド、コンストラクタの `Discord_Initialize`、`updateRPC()` の Presence 組み立てと `Discord_UpdatePresence`、`onGameExit()` の `Discord_Shutdown` を削除。`updateRPC()`/`onGameExit()` は中身が Discord 処理だけだったので空のオーバーライドとして残した。未使用になった import(`GameState.State`、`Net`、`UCore`、`Strings`)も削除
  - core の `Platform.java`(`hasDiscord()` など)、メニューの Discord ボタン、`DiscordDialog` は触っていない(Task 4.4 の範囲)

## 4. 1.5 への引き継ぎ

- `html/src/io/anuke/mindustry/GdxDefinition.gwt.xml:5` の `<inherits name='com.badlogic.gdx.controllers.controllers-gwt' />` が残っている。依存を外したので GWT コンパイルでは解決できない。Task 1.5 で外す
- html の依存の状態: `gdx-backend-gwt:1.13.1`、`gdx:1.13.1:sources`、`gdx-backend-gwt:1.13.1:sources`、`gdx-ai:1.8.2:sources` のみ。controllers 系はすべて削除済み。GWT 2.11.0 と `jsinterop-annotations` は未追加
- GWT の `scrolled` の経路: GWT バックエンド 1.13.1 も `InputProcessor.scrolled(float, float)` を呼ぶ。`Inputs` と `Scene` は対応済みだが、ブラウザのホイール量(ピクセル単位の大きな値の可能性)は符号だけを使うので問題ないはず。未確認
- `Inputs`/`Input` に残した `Input.Type.controller` と予約定数は GWT でもそのままコンパイルできるはず(controllers のクラスを参照していない)
- `*.gwt.xml` に `XboxBinds`/`InputProxy`/controllers への参照は、上記の `inherits` 以外に無いことを確認した

## 5. 未解決・報告事項

- **(解決済み: 第 3 節で Discord RPC を削除)`:desktop:run` が arm64 Mac で起動直後に落ちる(libGDX 移行とは無関係)**: `DesktopPlatform` のコンストラクタが `DiscordRPC.INSTANCE` を触った時点で、JNA 4.4.0 のディスパッチライブラリ(`i386,x86_64` のみ)を読めずに `java.lang.UnsatisfiedLinkError` になる(`at io.anuke.mindustry.desktop.DesktopPlatform.<init>(DesktopPlatform.java:37)`)。依存とこのコードは変更前と同じで、arm64 環境に固有。x86_64 の JDK や Intel Mac、Windows、Linux x64 では起きないはず(未確認)。対処案(要判断): (a) `useDiscord` の条件に `os.arch` を加えて arm64 では無効にする、(b) `java-discord-rpc` を外す、(c) JNA を 5.x に上げる(ただし `discord-rpc-release` のネイティブ自体が arm64 非対応の可能性が高い)。オーケストレーターが (b) を選び、対応した
- 残置(今回は触らないと決めたもの): `Vars.controllerMin`(`AndroidInput` が使用)、`Vars.baseControllerSpeed`(未使用になった)、設定画面の感度スライダー(`sensitivity`)、`controller-cursor` のスプライトとアトラスの項目、`uiskin.json` の `controllerColor`(`KeybindDialogStyle.controllerColor` フィールドも、スキン読み込みのため残した)、`Input.Type.controller`
- `desktop` の `dist`(fat jar)の重複エントリは未確認(Task 1.3 からの引き継ぎのまま)

## 6. 検証結果

### コンパイル

- `./gradlew :core:compileJava`: `BUILD SUCCESSFUL`(JDK 17)
- `./gradlew :desktop:compileJava`: `BUILD SUCCESSFUL`(JDK 17)
- gdx-ai 1.8.2 の互換性: gdx-ai を使う 7 ファイル(`ai/Heuristics`、`OptimizedPathFinder`、`Pathfind`、`Raycaster`、`SmoothGraphPath`、`game/Difficulty`、`SpawnPoint`)はエラーなしでコンパイルでき、実行時にも例外は出なかった(経路計算の結果は画面で未確認)
- `-Xlint:deprecation` の警告 35 件(javac の表示は 36 行で、1 行は件数の集計):
  - `Vector2.setAngle(float)`: 17 件(`Lines.java` 13、`BulletEntity.java` 3、`Geometry.java` 1)
  - `Vector2.rotate(float)`: 10 件(`Angles.java` 6、`Translator.java` 2、`Pixmaps.java` 1、`ai/Pathfind.java` 1)
  - `ShaderProgram.begin()`/`end()`: 8 件(`Graphics.java` 6、`IndexedRenderer.java` 2)
- unchecked の注記は以前からのもの

### `./gradlew :desktop:run`

1 回目(コードそのまま): 起動直後に `UnsatisfiedLinkError`(第 5 節)。ゲームの初期化前に落ちた。

2 回目(検証用に一時的に `DesktopPlatform.useDiscord = false` にした。**コミットしていない、実行後に戻した**): バックグラウンドで起動し、45 秒待ってからログを確認し、プロセスを止めた。ログ全文(`> Task :desktop:run` 以降):

```
Got locale: ja_JP
Total blocks loaded: 96
Failed to find custom map directory.
```

- `Exception` / `Error` / `at io.anuke` の出現は 0 件。45 秒後もプロセス(`DesktopLauncher`)は動いていた
- `Failed to find custom map directory.` はカスタムマップのフォルダが無いときに `Maps.loadMaps()` が出す通常のログ
- アトラス(`sprites.atlas`)、スキン、サウンド、マップの読み込みは `Control`/`UI` のコンストラクタと `init()` で行われ、失敗すれば例外になるので、少なくともそこまでは通っている

3 回目(Discord RPC を削除してコミット対象にしたコードのまま。一時的な変更なし): JDK 17 で `./gradlew :desktop:compileJava` が `BUILD SUCCESSFUL`。`./gradlew :desktop:run` をバックグラウンドで起動し、45 秒後にログを確認してからプロセスを止めた。ログ全文(`> Task :desktop:run` 以降、Gradle の定型メッセージを除く):

```
Got locale: ja_JP
Total blocks loaded: 96
```

- `Exception` / `Error` / `at io.anuke` の出現は 0 件。45 秒後もプロセスは動いていた(`pgrep` で確認後に停止。Gradle は `ignoreExitValue` のため `BUILD SUCCESSFUL in 51s`)
- `Failed to find custom map directory.` が出なくなったのは、2 回目の実行で `Maps.loadMaps()` がカスタムマップの `maps.json` を作ったため(`Maps.java:84-86`)。異常ではない

**ユーザーの手動確認待ち(画面が見えないため未確認)**:
- メニューが表示される
- 既存マップを開いてウェーブが始まる
- プレイヤー・敵・弾の向き(`angleDeg()` の変更)
- Ctrl + スクロールのズーム、スクロールでの回転、チャットのスクロール、ScrollPane
- キー設定ダイアログ(デバイス表示が「Keyboard」だけ、再割り当て、スクロールの軸割り当て、リセット)
- ブロックの配置と撤去
- 9-patch の UI(ボタンや枠。`findValue("split")` の変更)の見た目
- トラックパッドでのズーム・スクロールの敏感さ(符号だけを使う変更の影響)
- 注意: Discord RPC 削除後は arm64 Mac でもそのまま起動する(3 回目の実行)
