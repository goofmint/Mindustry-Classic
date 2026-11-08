# Task 1.1 verification (Issue #1)

- Date: 2026-11-08
- Branch: `issue-1-baseline` (`modernize` was NOT created; per orchestrator instruction)
- Base commit: `d487a23`
- **No build was run.** `./gradlew` was not executed (wrapper 4.10.2 is incompatible with modern JDKs; Task 1.3).

## Toolchain (install method: SDKMAN, user-level, no sudo)

### Before install
```
$ java -version
openjdk version "27.0.1.0.3" 2026-10-24
OpenJDK Runtime Environment Homebrew (build 27.0.1.0.3)
OpenJDK 64-Bit Server VM Homebrew (build 27.0.1.0.3, mixed mode, sharing)
$ javac -version   (not captured separately before install; same JDK 27 -> after-install value below is unchanged)
$ gradle --version   (Homebrew /opt/homebrew/bin/gradle)
Gradle 9.8.2
```
`/usr/libexec/java_home -V`: no JDKs found.

### Install results
- `sdk install gradle 8.14`: SUCCESS. Installed and SDKMAN set it as the default Gradle (`sdk current`: gradle 8.14).
- `sdk install java 17.0.21-tem`: FAILED, `17.0.21-tem is not available` (invalid identifier). The listed Temurin 17 identifier is `17.0.21.0+9-tem`.
- `sdk install java 17.0.21.0+9-tem` (tried twice, zsh and bash): FAILED. Download completed, then:
  ```
  zip error: Nothing to do! (try: zip -qyr /Users/nakatsugawa/.sdkman/tmp/java-17.0.21.0+9-tem.zip . -i Java-17.0.21.0+9-tem)
  Stop! The archive was corrupt and has been removed! Please try installing again.
  ```
  (SDKMAN post-install hook could not find `Contents/Home` in the extracted archive.) No alternative package was tried.
- `~/.sdkman/candidates/java` does not exist. JDK 17 is NOT installed.

### After install
```
$ java -version
openjdk version "27.0.1.0.3" 2026-10-24
OpenJDK Runtime Environment Homebrew (build 27.0.1.0.3)
OpenJDK 64-Bit Server VM Homebrew (build 27.0.1.0.3, mixed mode, sharing)
$ javac -version
javac 27.0.1.0.3
$ gradle --version   (SDKMAN gradle 8.14)
Gradle 8.14
Build time:    2025-04-25 09:29:08 UTC
Revision:      34c560e3be961658a6fbcd7170ec2443a228b109
Kotlin:        2.0.21
Groovy:        3.0.24
Ant:           Apache Ant(TM) version 1.10.15 compiled on August 25 2024
Launcher JVM:  27.0.1.0.3 (Homebrew 27.0.1.0.3)
OS:            Mac OS X 27.2 aarch64
```
Result: Gradle 8.14 is available; it currently launches on JVM 27, NOT 17. The completion criterion "`java -version` returns 17" is NOT met.

## Pre-migration baseline (read from repo files, nothing executed)

- Gradle wrapper: 4.10.2 (`gradle/wrapper/gradle-wrapper.properties`)
- `sourceCompatibility = 1.8` (core, desktop, html, server, kryonet, ios; android uses `JavaVersion.VERSION_1_8`)
- libGDX `gdxVersion = '1.9.8'`, gdx-ai `aiVersion = '1.8.1'` (root `build.gradle`)
- GWT `gwtVersion='2.8.0'` (`html/build.gradle`), plugin `de.richsource.gradle.plugins:gwt-gradle-plugin:0.6` (root `build.gradle`)
- `settings.gradle`: `include 'desktop', 'html', 'core', 'android', 'kryonet', 'server', 'ios'`
- `.travis.yml`: `language: android`, `jdk: openjdk8`, Android components (android-26, google play services, m2 repositories, google_apis-google-26); script `./gradlew desktop:dist`; after_success runs `upload-build.sh` and `cleanup_builds.sh`.

## README
`README.md` updated: fork paragraph (GPL-3.0 inherited, `LICENSE`), distribution/toolchain section (targets marked unverified), upstream notice kept under "Notice from the upstream repository", legacy build instructions labelled as to be replaced in Task 1.6.
