# ツールチェーン更新の記録

JDK 8 時代の構成を、JDK 17 / Gradle 8.14 / libGDX 1.13.1 / GWT 2.11.0 に上げたときの変更点をまとめる。元の記録は Task 1.3〜1.5 のスパイク(`.tmp/spike-gradle.md`、`.tmp/spike-libgdx.md`、`.tmp/spike-gwt.md`)。ビルド手順は [`README.md`](../README.md) を参照。

## バージョン一覧

| 項目 | 旧 | 新 |
|---|---|---|
| JDK | 8 | 17 |
| Gradle | 4.10.2 | 8.14 |
| libGDX | 1.9.8 | 1.13.1 |
| gdx-ai | 1.8.1 | 1.8.2 |
| GWT | 2.8.0 | 2.11.0 |
| GWT Gradle プラグイン | `de.richsource.gradle.plugins:gwt-gradle-plugin:0.6` | `org.docstr:gwt-gradle-plugin:1.1.29` |
| gdx-tools(`pack` 用) | 1.9.8 | 1.13.1 |

モジュールは `core`、`desktop`、`html` の 3 つ。

## 1. Gradle(Task 1.3)

### ラッパー

`gradle-wrapper.properties` の `distributionUrl` を `gradle-4.10.2-bin.zip` から `gradle-8.14-bin.zip` に変更した。旧ラッパー(4.10.2)は JDK 17 で起動できないため、先にビルドスクリプトを直し、スタンドアロンの Gradle 8.14 で `gradle wrapper --gradle-version 8.14` を実行して `gradlew`、`gradlew.bat`、`gradle-wrapper.jar` を再生成した。

### API の置き換え

| 場所 | 旧 | 新 |
|---|---|---|
| 全モジュールの依存宣言 | `compile` | `core` は `api`、`desktop` と `html` は `implementation` |
| `core` | `apply plugin: "java"` | `apply plugin: "java-library"` |
| `desktop` の `run`、`debug` | `main = project.mainClassName` | `mainClass.set(project.mainClassName)` |
| `desktop` の `dist` | `configurations.compile.collect {...}` | `configurations.runtimeClasspath.collect {...}` |
| `html` の `distZip` | `archiveName appName + "-html5.zip"` | `archiveFileName.set(appName + "-html5.zip")`(出力名は同じ) |
| `html` の `distZip` | `destinationDir(file("build/dist-zipped"))` | `destinationDirectory.set(file("build/dist-zipped"))` |

### GWT プラグインの出自

`org.docstr:gwt-gradle-plugin:1.1.29`(Maven Central、プラグイン ID は `gwt` のまま)。gdx-liftoff 1.14.0.2 の生成物が使っている座標とバージョンとして CodeRabbit の計画に記載されたもので、gdx-liftoff の生成物そのものは再確認していない。1.x 系は Gradle 9 非対応で、gdx-liftoff 1.14.0.3 は 2.2.x 系に移行している。

## 2. libGDX(Task 1.4)

### API の変更と対応

| 場所 | 旧 | 新 | 備考 |
|---|---|---|---|
| `Control.java` | `Gdx.input.setCatchBackKey(true)` | `Gdx.input.setCatchKey(Keys.BACK, true)` | |
| `Control.java` | `import io.anuke.ucore.modules.*;` のみ | `import io.anuke.ucore.modules.Module;` を追加 | JDK 9 以降の `java.lang.Module` と曖昧になるため(libGDX ではなく JDK 17 起因)。`sourceCompatibility = 1.8` のままで `--release` 未指定のため JDK 17 のクラスライブラリが見える |
| `Inputs.java`、`Scene.java` | `scrolled(int amount)` | `scrolled(float amountX, float amountY)` | `Math.signum(amountY)` で -1/0/1 にして、既存の呼び出し側の整数前提を保った。トラックパッドでは 1.9.8 より敏感に感じる可能性がある(未確認) |
| `DummyBatch`、`CacheBatch`、`CacheDrawBatch`、`Draw`、`SpriteDrawable`、`TiledDrawable` | `setColor(float)`(packed color) | `setPackedColor(float)` | `Batch.setColor(float)` と `SpriteCache` の同名メソッドの改名 |
| `Skin.java` | `AtlasRegion.splits`、`pads` | `findValue("split")`、`findValue("pad")` | 1.9.13 から `names`/`values` + `findValue()` に変更 |
| `Player`、`EnemyType`、`BulletEntity` | `Vector2.angle()` | `Vector2.angleDeg()` | 受け手が libGDX の `Vector2` の 3 か所のみ。`BulletEntity.angle()`(独自メソッド)の呼び出し 25 か所は対象外 |

最初のコンパイルエラーは 22 件(連鎖分を含む)で、上記の修正後に 0 件になった。gdx-ai 1.8.2 を使う 7 ファイルはそのままコンパイルでき、実行時にも例外は出なかった。gdx-ai 1.8.2 の POM は gdx 1.9.8 を要求するが、Gradle は 1.13.1 を選ぶ。

### gdx-controllers の削除(ゲームパッド対応を切る)

依存(core、desktop、html)をすべて外し、`XboxBinds` と `InputProxy` を削除した。`Inputs`、`Input`、`KeybindDialog`、`Control`、`UI`、`GestureHandler`、`PlaceMode`、`DefaultKeybinds`、`DesktopInput` からコントローラー関連の処理を削除した。デバイス一覧はキーボードのみになる。

`Input` のコントローラー定数 21 個は、名前と位置をそのまま残した予約枠にしている。保存済みキー設定は `ordinal()` で保存されるため、消すと後ろの定数がずれて別のキーに化ける。残置したもの: `Vars.controllerMin`、`Vars.baseControllerSpeed`、設定画面の感度スライダー、`controller-cursor` のスプライトとアトラスの項目、`uiskin.json` の `controllerColor`、`Input.Type.controller`。

### Discord RPC の削除

arm64 の Mac では、`DesktopPlatform` が `DiscordRPC.INSTANCE` を触った時点で、JNA 4.4.0 のネイティブ(`i386,x86_64` のみ)を読めず `UnsatisfiedLinkError` になった(libGDX 移行とは無関係)。オーケストレーターの判断で `java-discord-rpc` を desktop から削除した。削除後は arm64 Mac でも `:desktop:run` が 45 秒間例外なしで動いた。

## 3. GWT(Task 1.5)

### 依存とモジュール設定

| ファイル | 変更 |
|---|---|
| ルート `build.gradle` の `project(":html")` | `implementation "com.google.jsinterop:jsinterop-annotations:2.0.2:sources"` を追加 |
| `html/build.gradle` | `gwtVersion='2.8.0'` から `'2.11.0'` |
| `GdxDefinition.gwt.xml` | `com.badlogic.gdx.controllers.controllers-gwt` の `inherits` を削除 |
| `GdxDefinitionSuperdev.gwt.xml` | `com.badlogic.gdx.physics.box2d.box2d-gwt` の `inherits` を削除(Box2D への依存は他に無い) |
| `core/src/Mindustry.gwt.xml` | `gdx.reflect.include` から `net.Packets`、`net.Packet`、`net.Streamable` の 3 行を削除 |
| `HtmlLauncher.java` | 1.13.1 の `GwtApplicationConfiguration.width`/`height` が `final` になったため、`new GwtApplicationConfiguration(2000, 2000)` の 1 行にまとめた(値は従来の最終値と同じ) |

html の依存: `gdx-backend-gwt:1.13.1`、`gdx:1.13.1:sources`、`gdx-backend-gwt:1.13.1:sources`、`gdx-ai:1.8.2:sources`、`jsinterop-annotations:2.0.2:sources`。

### GWT プラグイン 1.1.29 の設定

`gwtVersion`、`maxHeapSize="2G"`/`minHeapSize="1G"`、`compiler { strict = true; disableCastChecking = true }`、`src`、`modules`、`devModules`、`webAppDirName` はそのまま反映された。OOM は出ていない。`sourceLevel` は未指定(GWT 既定の auto)で通り、`sourceCompatibility = 1.8` も変えていない。

### JRE エミュレーション

`DataInputStream`/`DataOutputStream`(`Save12`/`Save15` など)は、libGDX の GWT バックエンドのエミュレーションでコンパイルが通り、`-strict` でも他のエラーは出なかった。`Platform` の上書き(`DateTimeFormat`/`NumberFormat`、`Random` + `Base64Coder` による UUID)は変更なし。`DataInputStream.readLong` の保存時刻など、セーブとロードの往復は未確認。

### `:html:dist` の結果

`./gradlew :html:dist` が成功した(GWT コンパイル 21.2 秒、全体で約 24 秒)。`html/build/dist` に `index.html`、`html/html.nocache.js`、`assets/` などができる。1.13.1 の preloader はアセットのファイル名にハッシュを付けるが、元の名前との対応は `assets.txt` にあり、実行時は元の名前で読める。

### ブラウザでの確認(2026-11-08、Playwright + ヘッドレス Chromium)

| 項目 | 結果 |
|---|---|
| 初回ロード | 約 0.7〜1.5 秒(localhost、5 回) |
| コンソールエラー | 0 件(WebGL の `GPU stall due to ReadPixels` 警告のみ) |
| メニュー、Play、レベル選択、caldera の開始 | OK |
| ウェーブの開始、敵の出現と移動 | OK。65 秒以内に敵がコア付近へ到達し、砲台が迎撃した |
| 失敗したリクエスト | `assets/music/*.mp3` が `ERR_ABORTED`(ヘッドレスの自動再生制限) |

経路計算の秒数の厳密な計測、GWT でのホイール量、`scaleCanvas()` の表示は未確認で、Task 5.2 で扱う。

## 4. 残っている非推奨警告(Gradle 8.14、`--warning-mode all`)

| 場所 | 警告 | 削除予定 |
|---|---|---|
| `build.gradle` の `maven { url "..." }` | 空白代入構文 | Gradle 10 |
| `build.gradle` の `jcenter()` | `RepositoryHandler.jcenter()` | Gradle 9 |
| `build.gradle` の `apply plugin: "war"` | `Convention`/`JavaPluginConvention`(GWT プラグイン内部と推定) | Gradle 9 |
| `core`、`desktop`、`html` の `sourceCompatibility = 1.8` | `JavaPluginConvention` | Gradle 9 |
| `html/build.gradle` の `project.webAppDirName` | `WarPluginConvention` | Gradle 9 |
| `html/build.gradle` の `compiler { ... }` | `org.gradle.util.ConfigureUtil`(GWT プラグイン内部) | Gradle 9 |

いずれも Gradle 8.14 では動作を止めない。コード側の `-Xlint:deprecation` 警告も 35 件ある(`Vector2.setAngle` 17、`Vector2.rotate` 10、`ShaderProgram.begin()`/`end()` 8)。動作は 1.13.1 でも同じなので直していない。

## 5. 既知の制限と未解決の項目

- GWT プラグイン 1.1.29 は Gradle 9 に対応していない。Gradle 9 に上げるときは 2.x 系への移行が必要。
- `desktop` の `dist`(fat jar)は、`runtimeClasspath` の jar を展開するため `META-INF` などが重複する可能性がある(`duplicatesStrategy` 未設定で、Gradle 7 以降は重複があると失敗する)。実行して確認していない。
- `html` の `dist` は `dependsOn: [clean, compileGwt]` で、実行順は Gradle の保証外。これまでの実行では `clean`、`compileGwt`、`dist` の順で動いた。
- `Net.connect`/`Net.pingHost` は `clientProvider` が未設定のため、呼ぶと `NullPointerException` になる。`HtmlLauncher` の `canJoinGame()` は referrer が `https` でも itch.io でもなければ `true` を返すので、`http://localhost` から開くと参加ボタンが出る可能性がある(押下は未確認)。
- `SaveSlot.delete()` の GWT(LocalStorage)での動作は未確認。
- `desktop` の目視確認(メニュー、ウェーブ、向き、ズームと回転、キー設定ダイアログ、9-patch の UI など)はユーザーの手動確認待ち。
- `core/assets/version.properties` はビルドの設定段階で書き出される(`.gitignore` 対象)。
