![Imgur](https://i.imgur.com/w4N0yhv.png)

This repository is a fork of [Anuken/Mindustry-Classic](https://github.com/Anuken/Mindustry-Classic) and is being turned into a browser game. The GPL-3.0 license of the upstream project is inherited; the full license text is in the root [`LICENSE`](LICENSE) file.

An old, classic version of the pixelated sandbox tower defense game known as [Mindustry](https://github.com/Anuken/Mindustry).

### Distribution and toolchain of this fork

[![build](https://github.com/goofmint/Mindustry-Classic/actions/workflows/build.yml/badge.svg)](https://github.com/goofmint/Mindustry-Classic/actions/workflows/build.yml)

- Distribution is the browser (HTML/GWT) build only (ブラウザ版のみ配布). The output is `html/build/dist`.
- The `desktop` module is for development-time verification and for running tools. It is NOT distributed (decision date: 2026-11-08; 開発用で配布しない).
- Verified toolchain: JDK 17 / Gradle 8.14 (wrapper) / libGDX 1.13.1 / GWT 2.11.0. The old versions and the migration notes are in [`docs/UPGRADE.md`](docs/UPGRADE.md).

### Notice from the upstream repository

This repository is **no longer being maintained.** No features will be implemented, no PR will be accepted, no bugs will be fixed and no additional releases will be made.
If you find an issue and know how to fix it, feel free to fork this repo.

### Building

#### Requirements

- JDK 17 (a JDK with `javac`, not only a JRE). Gradle itself is downloaded by the wrapper (`./gradlew`), so it does not need to be installed.
- Network access on the first run (Gradle and Maven dependencies are downloaded).

Ways to install JDK 17:

- Homebrew (macOS): `brew install openjdk@17`, then
  `export JAVA_HOME=$(brew --prefix openjdk@17)/libexec/openjdk.jdk/Contents/Home` and `export PATH="$JAVA_HOME/bin:$PATH"`
- SDKMAN: `sdk list java` and install a 17.x Temurin build
- Eclipse Temurin installers: https://adoptium.net/

Check with `java -version` (it must print version 17) and `javac -version`.

The Gradle wrapper prefers `JAVA_HOME` over `PATH`, so a stale or invalid `JAVA_HOME` makes `./gradlew` fail even when `java` is 17. Point it at the JDK 17 installation (for example `export JAVA_HOME=$(/usr/libexec/java_home -v 17)` on macOS, or the Homebrew path above) and confirm the JVM the wrapper actually uses with `./gradlew --version`: the `Launcher JVM:` line must show 17, and the `Daemon JVM:` line (a version, or a path when no JDK is specified) must also point at the JDK 17 installation. If it does not, check that `org.gradle.java.home` in `gradle.properties` or `~/.gradle/gradle.properties` is not pointing at another JDK.

#### Commands

Run all commands in the repository root. On Windows use `.\gradlew.bat` instead of `./gradlew`.

- `./gradlew :desktop:run`: starts the desktop version. For development only; it needs a GUI. The working directory is `core/assets`, and the macOS `-XstartOnFirstThread` flag is handled by the task.
- `./gradlew :html:dist`: builds the browser version into `html/build/dist`. It does not work over `file://`; serve the directory and open `index.html`, for example:
  `cd html/build/dist && python3 -m http.server 8765`, then open http://localhost:8765/index.html.
  The browser build has no multiplayer (Join).
- `./gradlew pack`: packs the sprites in `core/assets-raw/sprites` into `core/assets/sprites/sprites.atlas` and `sprites.png`. These two files are tracked in git, so run it after changing the raw sprites and commit the result. It is not needed for a normal build. Running it on the current tree already rewrites both files (the atlas header spacing and the PNG bytes change; the sprite layout does not), so commit them only together with a real sprite change.

CI (`.github/workflows/build.yml`) runs `./gradlew :core:compileJava :desktop:compileJava :html:dist` on JDK 17 and uploads `html/build/dist` as the `html-dist` artifact.
