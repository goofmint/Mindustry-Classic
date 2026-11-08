# Task 1.3 スパイク記録: Gradle 8.14 への移行

ブランチ: `issue-3-gradle8`(`issue-2-modules` から分岐)
結論: JDK 17 で `./gradlew help` と `./gradlew :core:compileJava --dry-run` の両方が `BUILD SUCCESSFUL`。完了条件を満たした。所要は 4h の上限内。

## 1. 環境

| 項目 | 値 |
|---|---|
| JAVA_HOME | `/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home` |
| `java -version` | `openjdk version "17.0.21.0.3" 2026-10-24` (Homebrew, aarch64) |
| `javac -version` | `javac 17.0.21.0.3` |
| ラッパー再生成に使った Gradle | SDKMAN の Gradle 8.14(`gradle wrapper --gradle-version 8.14`) |
| ラッパー | Gradle 8.14(`gradle-8.14-bin.zip`、`distributionSha256Sum` なし) |

前提確認: `settings.gradle` は `include 'desktop', 'html', 'core'` のみ。android / ios / server / kryonet のディレクトリは存在しない(Task 1.2 で削除済み)。

## 2. 変更点(ファイル: 旧 → 新)

### ラッパー
- `gradle/wrapper/gradle-wrapper.properties`: `distributionUrl` を `gradle-4.10.2-bin.zip` → `gradle-8.14-bin.zip`。Gradle 8.14 の `wrapper` タスクにより `networkTimeout=10000`、`validateDistributionUrl=true` が追加された
- `gradlew`、`gradlew.bat`、`gradle/wrapper/gradle-wrapper.jar`: Gradle 8.14 で再生成

手順メモ: 先にビルドスクリプト(下記)を直してから、スタンドアロンの Gradle 8.14 で `gradle wrapper --gradle-version 8.14` を実行した。旧ラッパー(4.10.2)は JDK 17 で起動できないため。

### ルート `build.gradle`
- buildscript classpath: `de.richsource.gradle.plugins:gwt-gradle-plugin:0.6` → `org.docstr:gwt-gradle-plugin:1.1.29`(プラグイン ID は `gwt` のまま)
- `project(":core")`: `apply plugin: "java"` → `apply plugin: "java-library"`。`compile` → `api`(gdx、gdx-ai、gdx-controllers)
- `project(":desktop")`: `compile` → `implementation`(`project(":core")`、gdx-backend-lwjgl3、gdx-platform natives-desktop、gdx-controllers-lwjgl3、java-discord-rpc)
- `project(":html")`: `compile` → `implementation`(`project(":core")`、gdx-backend-gwt、gdx sources、gdx-backend-gwt sources、gdx-ai sources、gdx-controllers sources、gdx-controllers-gwt、gdx-controllers-gwt sources)
- リポジトリ(`jcenter()`、sonatype snapshots、jitpack)は変更なし。解決は失敗しなかった

### `desktop/build.gradle`
- `run`、`debug`: `main = project.mainClassName` → `mainClass.set(project.mainClassName)`
- `dist`: `configurations.compile.collect {...}` → `configurations.runtimeClasspath.collect {...}`
- `workingDir`、`-XstartOnFirstThread`、`appArgs`、`standardInput` はそのまま。`classesDir` / `baseName` / Jar の `version` は使われていなかった

### `html/build.gradle`
- `distZip`: `archiveName appName + "-html5.zip"` → `archiveFileName.set(appName + "-html5.zip")`(出力名は同じ)
- `distZip`: `destinationDir(file("build/dist-zipped"))` → `destinationDirectory.set(file("build/dist-zipped"))`
- `webAppDirName`、`addSource`、`dist`、`gwt { ... }` DSL、`draftWar` はそのまま

### `core/build.gradle`
- 変更なし。`pack` タスク(buildscript の gdx-tools 1.9.8 の TexturePacker)はそのまま。`sourceCompatibility = 1.8` もそのまま(Task 1.4 で 17 へ)

上記以外に設定段階を止める箇所はなかった。

## 3. 完了条件コマンドの結果

### `./gradlew help --warning-mode all`

```
Downloading https://services.gradle.org/distributions/gradle-8.14-bin.zip
.............10%...(略)...100%
Welcome to Gradle 8.14.
> Configure project :
  (非推奨警告、第 4 節)
> Configure project :core / :desktop / :html
  (非推奨警告、第 4 節)
> Task :help
...
BUILD SUCCESSFUL in 4s
1 actionable task: 1 executed
```

### `./gradlew :core:compileJava --dry-run --warning-mode all`

```
> Configure project :
> Configure project :core
> Configure project :desktop
> Configure project :html
:core:compileJava SKIPPED

BUILD SUCCESSFUL in 263ms
```

### 参考(記録のみ): `./gradlew :html:compileGwt --dry-run`

```
:core:compileJava SKIPPED
:html:addSource SKIPPED
:html:compileJava SKIPPED
:html:processResources SKIPPED
:html:compileGwt SKIPPED
BUILD SUCCESSFUL in 301ms
```

### 参考: 依存解決
- `./gradlew buildEnvironment`: classpath に `org.docstr:gwt-gradle-plugin:1.1.29` と `com.badlogicgames.gdx:gdx-tools:1.9.8` が解決された
- `./gradlew :core:dependencies --configuration compileClasspath`: gdx 1.9.8、gdx-ai 1.8.1(gdx 1.9.5 → 1.9.8)、gdx-controllers 1.9.8 が解決された

失敗の分類(環境 / プラグイン互換 / 旧 API): 完了条件のコマンドでは失敗なし。旧 API 起因で直したものは第 2 節の通り。

## 4. 非推奨警告(Gradle 8.14、`--warning-mode all`)

| 場所 | 警告 | 削除予定 | 原因 |
|---|---|---|---|
| `build.gradle:6`、`:68` | `maven { url "..." }` の空白代入構文 | Gradle 10 | ビルドスクリプト |
| `build.gradle:7` | `RepositoryHandler.jcenter()` | Gradle 9 | ビルドスクリプト |
| `build.gradle:87`(`apply plugin: "war"`) | `Convention` / `JavaPluginConvention` | Gradle 9 | GWT プラグイン 1.1.29 内部(war 適用時の処理と推定) |
| `core/build.gradle:2`、`desktop/build.gradle:3`、`html/build.gradle:57` | `JavaPluginConvention`(トップレベルの `sourceCompatibility = 1.8`) | Gradle 9 | ビルドスクリプト。`java { sourceCompatibility = ... }` に直せば消える(Task 1.4 で 17 にする際に対応) |
| `html/build.gradle:12` | `WarPluginConvention`(`project.webAppDirName = 'webapp'`) | Gradle 9 | ビルドスクリプト |
| `html/build.gradle:14` | `org.gradle.util.ConfigureUtil`(`compiler { ... }` ブロック) | Gradle 9 | GWT プラグイン 1.1.29 内部 |

いずれも Gradle 8.14 では動作を止めない。今回は最小変更の方針により直していない。

## 5. 補足・注意点

- `writeVersion()` の副作用: `desktop/build.gradle` の `dist` タスクの設定クロージャ内で `writeVersion()` が呼ばれるため、設定段階(`help` でも)で `core/assets/version.properties` が書き出される。実際に生成を確認した。`.gitignore` 対象なのでコミットされない
- `html` の `dist` は `dependsOn: [clean, compileGwt]` で、`clean` と `compileGwt` の実行順は保証されない(`mustRunAfter` がない)。既存の挙動のまま
- `webAppDirName` は Gradle 9 で削除予定(WarPluginConvention)
- GWT プラグインの出自: `org.docstr:gwt-gradle-plugin:1.1.29`(Maven Central、プラグイン ID `gwt`)。gdx-liftoff 1.14.0.2 の生成物が使っている座標とバージョンとして CodeRabbit の計画に記載されたもの。gdx-liftoff の生成物そのものは今回再確認していない。1.x 系は Gradle 9 非対応で、gdx-liftoff 1.14.0.3 は 2.2.x 系に移行している

## 6. 後続タスクへ回すもの

### Task 1.4
- Java のコンパイルエラー(今回は `compileJava` を実行していない)
- gdx-controllers の削除(依存はまだ残している)
- desktop の fat jar(`dist`)で `runtimeClasspath` の全 jar を `zipTree` で展開するため、`META-INF` などの重複エントリが出る可能性(`duplicatesStrategy` 未設定。Gradle 7 以降は重複があると失敗する)
- `sourceCompatibility = 1.8` → 17、および `java { }` ブロックへの移行
- gdx-tools 1.9.8(`pack`)の更新

### Task 1.5
- GWT 2.8.0 → 2.11.0、jsinterop-annotations の追加、superDev、Gretty
- GWT プラグイン 1.1.29 の Gradle 9 非対応(Gradle 9 に上げる時は 2.x への移行が必要)

## 7. 未検証

- GWT コンパイル(`:html:compileGwt` は dry-run のみ)
- desktop / html の実行(`run`、`dist`、`distZip`)
- `pack` タスクの実行
