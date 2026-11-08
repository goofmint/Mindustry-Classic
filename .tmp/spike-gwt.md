# スパイク記録: Task 1.5 GWT 2.11.0 への移行(html のコンパイル)

- ブランチ: `issue-5-gwt`(`issue-4-libgdx13` の上)
- 実行環境: JDK 17(Homebrew `openjdk@17` 17.0.21)、Gradle 8.14(ラッパー)、`org.docstr:gwt-gradle-plugin:1.1.29`
- 結果: `./gradlew :html:dist` 成功。GWT コンパイル 21.2 秒、`:html:dist` 全体で約 24 秒(`clean` 込み、`localWorkers 10`、1 パーミュテーション)

## 1. 前提確認(1.2〜1.4 の成果)

| 項目 | 状態 |
| --- | --- |
| モジュール | `settings.gradle` は `desktop`、`html`、`core` のみ |
| Gradle | ラッパーは `gradle-8.14-bin.zip` |
| GWT プラグイン | `org.docstr:gwt-gradle-plugin:1.1.29`(ID `gwt`) |
| libGDX / gdx-ai | `gdxVersion = '1.13.1'`、`aiVersion = '1.8.2'` |
| controllers / websockets | ソースとビルドスクリプトに参照なし(`GdxDefinition.gwt.xml` の `controllers-gwt` の `inherits` のみ残っていた。本タスクで削除) |
| `HtmlLauncher` | `WebsocketClient` の登録なし |

## 2. 依存とモジュール設定の変更

| ファイル | 変更 |
| --- | --- |
| ルート `build.gradle` の `project(":html")` | `implementation "com.google.jsinterop:jsinterop-annotations:2.0.2:sources"` を追加。html の依存はルートに書かれているため、`html/build.gradle` ではなくここに置いた(tasks.md の記述は `html/build.gradle` だが場所だけ異なる) |
| `html/build.gradle` | `gwtVersion='2.8.0'` → `'2.11.0'` |
| `html/src/io/anuke/mindustry/GdxDefinition.gwt.xml` | `<inherits name='com.badlogic.gdx.controllers.controllers-gwt' />` を削除。gwt バックエンド、gdx-ai、`Mindustry`、`uCore`、エントリポイント、`gdx.assetpath`、`rename-to="html"` はそのまま。DOCTYPE はもともと無いので追加していない |
| `html/src/io/anuke/mindustry/GdxDefinitionSuperdev.gwt.xml` | `<inherits name='com.badlogic.gdx.physics.box2d.box2d-gwt' />` を削除(`grep -rni box2d core/src html/src` でこの行以外にヒットなし。Box2D 依存もない)。リンカー設定は触っていない |
| `core/src/Mindustry.gwt.xml` | `gdx.reflect.include` から `io.anuke.mindustry.net.Packets`、`net.Packet`、`net.Streamable` の 3 行だけ削除。`net` パッケージのソース自体は `<source path="io/anuke/mindustry" />` の範囲なので引き続きコンパイル対象 |
| `core/src/uCore.gwt.xml` | 変更なし |

html の依存(確認済み): `gdx-backend-gwt:1.13.1`、`gdx:1.13.1:sources`、`gdx-backend-gwt:1.13.1:sources`、`gdx-ai:1.8.2:sources`、`jsinterop-annotations:2.0.2:sources`。GWT コンパイラのクラスパスには、プラグインが足す `jsinterop-annotations-2.0.0.jar`(gwt-user 2.11.0 の推移依存)も載っていたが、競合エラーは出ていない。

### GWT プラグイン 1.1.29 の設定項目(`--info` の起動コマンドで確認)

```
com.google.gwt.dev.Compiler -nogenerateJsInteropExports -logLevel INFO
  -war html/build/gwt/out -workDir html/build/gwt/work -localWorkers 10
  -XdisableCastChecking -strict io.anuke.mindustry.GdxDefinition
JVM: -Xms1G -Xmx2G -Dgwt.persistentunitcachedir=html/build/gwt/cache
```

| 設定 | 結果 |
| --- | --- |
| `gwtVersion='2.11.0'` | 反映(`gwt-dev-2.11.0`、`gwt-user-2.11.0`、`gwt-codeserver-2.11.0` が解決) |
| `maxHeapSize="2G"` / `minHeapSize="1G"` | 反映(`-Xmx2G -Xms1G`)。OOM は出ず 4G への引き上げは不要 |
| `compiler { strict = true; disableCastChecking = true }` | 反映(`-strict`、`-XdisableCastChecking`) |
| `src`、`modules`、`devModules`、`project.webAppDirName` | エラーなし。`src` はクラスパス先頭に `html/src`、`webapp` は `dist` で `build/dist` にコピーされる |
| `sourceLevel` | 指定なし(GWT 既定の auto)で通った。`sourceCompatibility = 1.8` もそのまま |

## 3. コンパイルエラーと修正

| # | メッセージ | 原因 | 修正 | 影響 |
| --- | --- | --- | --- | --- |
| 1 | `HtmlLauncher.java:74: final変数widthに値を代入することはできません`(`height` も同様、`:html:compileJava`) | 1.13.1 の `GwtApplicationConfiguration.width`/`height` は `public final int` | `new GwtApplicationConfiguration(WIDTH, HEIGHT)` と後の `config.width = 2000; config.height = 2000;` を、`new GwtApplicationConfiguration(2000, 2000)` の 1 行にまとめた | 本家でも最終的に 2000×2000 を設定していたので値は同じ。`WIDTH`/`HEIGHT`(800×600)は `scaleCanvas()` の縦横比計算で引き続き使う |

これ以外のエラーは GWT コンパイル(`-strict`)でも出なかった。

- `DataInputStream`/`DataOutputStream`: libGDX GWT バックエンドのエミュレーションでコンパイルが通った(エラーなし)
- `HtmlLauncher` の他の API(`getPreloaderCallback()` の `PreloaderCallback.update(PreloaderState)`/`error(String)`、`getConfig()`、`createApplicationListener()`、`setLoadingListener`、`getRootPanel()`、`setLogLevel(LOG_NONE)`)は 1.13.1 でそのままコンパイルできた。`Platform` の上書き(`DateTimeFormat`/`NumberFormat` による書式化、`Random` + `Base64Coder` による UUID)も変更なし
- `Vector2.angle()` 関連のエラーなし(1.4 で対応済み)。GWT の警告に出る `Vector2.angle()` 等の deprecated は、gwtref が生成するリフレクションキャッシュ(`IReflectionCacheGenerated.java`)が全メソッドを参照するためで、ゲームのコードではない
- 警告: リフレクションキャッシュ生成時の deprecated 参照警告のみ(`SpriteBatch.defaultVertexDataType`、`Matrix4.tmp`、`Tree.getNodes()`、`JsonValue.size()` など)。動作に影響なし

## 4. dist の確認

`dist` の `dependsOn: [clean, compileGwt]` は今回の 3 回の実行ではいずれも `clean` → `compileGwt` → `dist` の順で動き、出力は空にならなかった。順序は Gradle の保証ではないが、問題が出ていないので `mustRunAfter` は追加していない(既存の挙動のまま)。

`html/war` は libGDX の preloader ジェネレーターが `../core/assets` を `war/assets` へコピーする際に作られる。`.gitignore` に `/html/war/` が既にあるので追加不要。`dist` の `copy from "war"` はこの資産を `build/dist/assets` へ運ぶ役割で、失敗しない。

`html/build/dist` の中身:

- `index.html`、`styles.css`、`soundmanager2-setup.js`、`soundmanager2-jsmin.js`(`webapp` から)
- `html/html.nocache.js`(6.8KB)、`html/370E157B442DAF3A41B5D343C55624D0.cache.js`(3.5MB)、`html/logo.png`、`html/clear.cache.gif`
- `WEB-INF/`(`web.xml`、`deploy/html/symbolMaps` など。配信には不要だが既存の挙動のまま)
- `assets/`(16MB): `assets.txt`、`maps/`(23 枚 + `maps.json`)、`sprites/`(`sprites.atlas`、`sprites.png` ほか)、`ui/`(`uiskin.json`、`uiskin.atlas`、`square`/`korean`/`title` の `.fnt`/`.png`)、`bundles/`(12 言語)、`sounds/`(26 ファイル)、`music/`(6 曲)、`shaders/`、`cursors/`
  - 1.13.1 の preloader はファイル名にハッシュを付ける(例 `ui/uiskin-ce02d37a....json`)。元の名前との対応は `assets.txt` に書かれ、実行時は元の名前で読める
  - `assets/version-....properties`、`assets/mindustry-maps/` も含まれる。`core/assets` にあるローカルの生成物(どちらも `.gitignore` 対象)がそのままコピーされたもの。既存の挙動
- `soundmanager2`: 1.13.1 の GWT バックエンドは Web Audio を使い、soundmanager2 は使わない。ただしスクリプトは `webapp` にあるので 404 にはならない(`curl` で 200)。起動を壊していないので残した

## 5. 配信

```
cd html/build/dist && python3 -m http.server 8765 >/tmp/dist-server.log 2>&1 &
```

- URL: http://localhost:8765/index.html
- 確認: `index.html`、`html/html.nocache.js`、`assets/assets.txt`、`soundmanager2-jsmin.js` がすべて `HTTP/1.0 200 OK`
- 起動時のサーバー PID: 50625(オーケストレーターの確認用に起動したままにしている)

## 6. ブラウザでの確認(オーケストレーターが実施、2026-11-08 09:10〜09:18Z)

方法: Playwright 1.61(Node)+ 同梱 Chromium 1228(ヘッドレス、SwiftShader による WebGL)。`html/build/dist` を `python3 -m http.server 8765` で配信し、`http://localhost:8765/index.html` を開いて操作とスクリーンショットを自動化した。スクリプトはリポジトリ外(セッションの scratchpad)に置き、コミットしていない。

| 項目 | 結果 |
| --- | --- |
| 初回ロード(`load` イベント → preloader 消失・canvas 表示) | 約 0.7〜1.5 秒(localhost、5 回実行) |
| コンソールエラー / pageerror | 0 件(WebGL の `GPU stall due to ReadPixels` 警告のみ。SwiftShader 由来) |
| 失敗したリクエスト | `assets/music/*.mp3` が `ERR_ABORTED`(ヘッドレスの自動再生制限。index.html には影響なし) |
| メニュー表示 | OK(Play / Editor / Settings / About / Donate、ロゴ、バージョン表記) |
| Play → New Game → レベル選択 | OK(Waves / Normal、マップ一覧とサムネイル、ハイスコア表示) |
| caldera(256×256、3 レーン)開始 | OK(コア、初期設備、HUD「WAVE 1 / WAVE IN 112」、資源 52 石) |
| HUD の再生ボタンでウェーブ開始 | OK(「WAVE 2 / 3 ENEMIES」表示) |
| 敵の出現と移動 | OK。開始 10 秒で敵 3 体、65 秒でコア付近に到達し砲台が迎撃(残り 2 体)。経路計算は機能している |
| 経路計算の秒数 | 計測コードによる厳密値は未計測。ウェーブ開始から 65 秒時点で敵がコア付近(砲台の射程内)に到達し迎撃されていたことで代える。コアそのものへの到達時刻は記録していない(Task 1.5 の完了条件をこの形に変更済み)。厳密な計測は Task 5.2 で行う |
| GWT でのホイール量、`scaleCanvas()` の表示 | 未確認(ヘッドレスで操作していない)。Task 5.2 へ |

観察メモ: レベル選択ダイアログの背後にメニューのボタン文字がうっすら透けて見える(ダイアログ背景の半透明)。本家由来かどうか未確認。見た目だけで動作に影響なし。

## 7. 気づいたが直していないリスク

- `Net.connect`/`Net.pingHost`: `clientProvider` が未設定なので、呼ばれると `NullPointerException` になる。`HtmlLauncher` の `canJoinGame()` は referrer が `https` でも itch.io でもなければ `true` を返すため、`http://localhost` から開くと参加ボタンが出る可能性がある。押すと落ちるかもしれない(未確認)
- `SaveSlot.delete()`(`Saves.java:181`): GWT のファイル(LocalStorage)上での削除の動作は未確認
- `DataInputStream.readLong`(`Save12`/`Save15`/`SaveFileVersion` の保存時刻): GWT では `long` がエミュレーションされるので値は正しいはずだが、セーブ/ロードの往復は未確認。セーブ形式は触っていない
- GWT プラグイン 1.1.29 は Gradle 9 非対応(1.3 の記録どおり)
- `dist` の `clean` と `compileGwt` の実行順は Gradle の保証外(今回は問題なし)
