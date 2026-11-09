# Task 1.6 verification (Issue #6)

- Branch: `issue-6-ci` (stacked on `issue-7-locales`)
- JDK: Homebrew openjdk@17 (17.0.21)

## Files
- Added: `.github/workflows/build.yml`, `docs/UPGRADE.md`
- Changed: `README.md` (build section rewritten, badge, link to UPGRADE.md)
- Deleted: `.travis.yml` (JDK 8 / Android; referenced `upload-build.sh` and `cleanup_builds.sh`, which do not exist)

## Preconditions (confirmed)
Gradle 8.14 (wrapper), libGDX 1.13.1 / gdx-ai 1.8.2 (root `build.gradle`), GWT 2.11.0 (`html/build.gradle`), the three `.tmp/spike-*.md` files exist.

## Local checks
- Workflow YAML: `yaml.safe_load` (PyYAML) parses it. Triggers: push (main), pull_request (no filter), workflow_dispatch. Steps: checkout@v7, setup-java@v6 (temurin 17), setup-gradle@v6, build, verify, upload-artifact@v7. (PyYAML reads `on` as the key `True`; that is normal for YAML 1.1 and GitHub accepts `on`.)
- `./gradlew :html:dist --no-daemon` in the working tree: BUILD SUCCESSFUL in 25s; `html/build/dist/index.html` and `html/build/dist/html/html.nocache.js` exist.
- Cold-cache reproduction (fresh `git clone`, `git checkout issue-6-ci`, `GRADLE_USER_HOME=/tmp/gradle-fresh`):
  - Downloading the Gradle 8.14 distribution failed repeatedly: `services.gradle.org` redirects to a GitHub release URL that returned HTTP 504 for several minutes (also with curl, 6 tries). First attempt also hit the wrapper `networkTimeout=10000`. This is an external outage, not a repo problem.
  - Workaround: only the Gradle 8.14 distribution was copied from `~/.gradle/wrapper/dists` into the fresh `GRADLE_USER_HOME`. Everything else (plugins, libGDX, GWT, Maven artifacts; ~107 MB of caches) was downloaded fresh.
  - `./gradlew :html:dist`: BUILD SUCCESSFUL in 38s (6 tasks executed), both output files exist.
  - `./gradlew pack`: BUILD SUCCESSFUL in 684ms. `git status --short` afterwards:
    ```
     M core/assets/sprites/sprites.atlas
     M core/assets/sprites/sprites.png
    ```
    Atlas diff is only header spacing (`size: 1024,512` -> `size: 1024, 512`, `filter: Nearest,Nearest` -> `Nearest, Nearest`); the PNG differs in bytes (93925 -> 113952 bytes, both 1024x512 RGBA). Cause is the gdx-tools 1.9.8 -> 1.13.1 update (not investigated further). These changes were NOT committed; the README says to commit pack output only with a real sprite change.
- `/tmp/mc-fresh` and `/tmp/gradle-fresh` removed.

## Pending
- The CI run on GitHub (green check, `html-dist` artifact) can only be checked after the PR is opened.
- A cold-cache run that also downloads the Gradle distribution itself (needs services.gradle.org to work again).
- Whether the regenerated `sprites.png` looks identical in game was not checked.
