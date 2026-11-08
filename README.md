![Imgur](https://i.imgur.com/w4N0yhv.png)

This repository is a fork of [Anuken/Mindustry-Classic](https://github.com/Anuken/Mindustry-Classic) and is being turned into a browser game. The GPL-3.0 license of the upstream project is inherited; the full license text is in the root [`LICENSE`](LICENSE) file.

An old, classic version of the pixelated sandbox tower defense game known as [Mindustry](https://github.com/Anuken/Mindustry).

### Distribution and toolchain of this fork

- Distribution is the browser (HTML/GWT) build only (ブラウザ版のみ配布). The planned output is `html/build/dist` (unverified yet).
- The `desktop` module is for development-time verification and for running tools. It is NOT distributed (decision date: 2026-11-08; 開発用で配布しない).
- Target toolchain, all UNVERIFIED targets to be confirmed in later tasks: JDK 17 / Gradle 8.14 / libGDX 1.13.1 / GWT 2.11.

### Notice from the upstream repository

This repository is **no longer being maintained.** No features will be implemented, no PR will be accepted, no bugs will be fixed and no additional releases will be made.
If you find an issue and know how to fix it, feel free to fork this repo.

### Building (legacy upstream instructions, to be replaced in Task 1.6)

First, make sure you have Java 8 and JDK 8 installed. Open a terminal in the root directory, and run the following commands:


**_Windows_**

_Running:_ `gradlew.bat desktop:run`  
_Building:_ `gradlew.bat desktop:dist`


**_Linux_**

_Running:_ `./gradlew desktop:run`  
_Building:_ `./gradlew desktop:dist`

Gradle may take up to several minutes to download files. Be patient. <br>
After building, the output .JAR file should be in the output JAR file should be in `/desktop/build/libs/desktop-release.jar.`
