# Task 3.1 検証記録

## 変更
- `core/build.gradle`: `sourceSets.main.java.exclude 'test/**'`、`sourceSets.test.java.srcDirs = ["src/test/java"]`、`testImplementation 'junit:junit:4.13.2'`、`test { useJUnit() }`
- `core/src/test/java/io/anuke/mindustry/SmokeTest.java`(libGDX・Vars 非依存、`assertEquals(4, 2 + 2)`)
- `.github/workflows/build.yml`: `Unit tests` ステップ(`./gradlew :core:test --no-daemon`)を `Build`(`:html:dist`)の前に追加

## 結果
- ベースライン: `./gradlew :html:dist`(JDK 17、Gradle 8.14)成功
- `./gradlew :core:clean :core:test`: BUILD SUCCESSFUL。`core/build/test-results/test/TEST-io.anuke.mindustry.SmokeTest.xml` は tests=1 skipped=0 failures=0 errors=0
- ソースセット比較(一時 init script、非コミット): main は 425 ファイルで SmokeTest・`core/src/test/` 配下を含まない。test は `[SmokeTest.java]` のみ
- `./gradlew :core:jar` 後の `jar tf core/build/libs/*.jar | grep -ic smoke` は 0
- `.gwt.xml` 2 ファイルは未変更。`core/src/test` に `.gwt.xml` は無い。`html/build.gradle` の `addSource` は `core` の `allJava.srcDirs`(= ディレクトリ `core/src`)をそのまま compileClasspath に足すので、`sourceSets.main.java.exclude 'test/**'` はここには効かない(exclude が除くのは main のソースファイルの集合であって、ディレクトリではない)。テストが GWT のソースに入らない理由は、`Mindustry.gwt.xml` / `uCore.gwt.xml` の `source path` が `io/anuke/mindustry` と `io/anuke/ucore` で、テストは `core/src/test/java` 配下にあり、どちらの path にも含まれないため。`./gradlew :html:dist` は変更後も成功(43s)
- CI: PyYAML でワークフローを読み込めることを確認。GitHub 上の実行は PR 作成後にオーケストレーターが確認する
