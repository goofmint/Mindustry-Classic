# Task 2.1 verification (Issue #7)

- Date: 2026-11-08
- Branch: `issue-7-locales` (stacked on `issue-5-gwt`)
- JDK 17 (`/opt/homebrew/opt/openjdk@17/...`), `./gradlew` (Gradle 8.14)

## Build
```
./gradlew :core:compileJava :desktop:compileJava   -> PASS (only existing deprecation notes)
./gradlew :desktop:dist                            -> FAIL (not caused by this task)
  Entry META-INF/INDEX.LIST is a duplicate but no duplicate handling strategy has been set.
```
The `dist` failure is the known duplicate-entry jar problem; no jar was produced, so the jar launch method was not usable.

## Launch attempt (window)
Ran `DesktopLauncher` directly with the `:desktop` runtime classpath (obtained via a throwaway init script), cwd `core/assets`,
`java -XstartOnFirstThread -Duser.home=/tmp/mc-home-{ja,en} -Duser.language={ja,en} -Duser.country={JP,US} -cp <classpath> io.anuke.mindustry.desktop.DesktopLauncher`.
Preferences were isolated by `-Duser.home` (real settings untouched).
Result in both cases (before any `Got locale` line was logged):
```
java.lang.NullPointerException
	at org.lwjgl.glfw.GLFW.nglfwGetMonitorPos(GLFW.java:1432)
	at com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration.toLwjgl3Monitor(...)
	at io.anuke.mindustry.desktop.DesktopLauncher.main(DesktopLauncher.java:22)
```
GLFW found no monitor (the agent shell has no display session). So the game window could not be started here; `:desktop:run` was not tried because it would hit the same GLFW failure.

## Substitute check (no window): same loading call as BundleLoader
`I18NBundle.createBundle(FileHandle("core/assets/bundles/bundle"), locale)` with gdx 1.13.1, run from a throwaway Java file:
```
Got locale: ja_JP, bundle locale: 'ja'  ja=日本語 play=Play
Got locale: ja,    bundle locale: 'ja'
Got locale: en_US, bundle locale: ''    (root bundle)
Got locale: en,    bundle locale: ''
Got locale: fr,    bundle locale: ''    (old saved value falls back to root/English)
```
`play=Play` under ja confirms keys missing in `bundle_ja.properties` fall back to the root bundle.
This is not the in-game log. The in-game line is now `Got locale: {locale}, bundle locale: {Core.bundle.getLocale()}` (BundleLoader) and must be read on a real run.

## PENDING (orchestrator / user, needs a display)
- Run the desktop app with `-Duser.language=ja -Duser.country=JP` and check the log line shows `ja_JP` / `ja`; with `en`/`US`, bundle locale empty.
- Visual check of the Language dialog: exactly two buttons, "English" first, second button labelled with the `text.locale.ja` string, no `???`.
  Japanese glyphs may render as boxes until Tasks 2.2/2.3 add a Japanese font.

## Known behaviors
- Japanese glyphs are not rendered yet (no Japanese font).
- With `locale=default` and OS locale `ja_JP`, the dialog's exact-match check (`ui.getLocale().equals(loc)`) does not match `ja`, so `en` (or nothing) may look selected although the `ja` bundle is loaded.
- Old saved values (e.g. `fr`) load the root (English) bundle; none of the buttons is checked.
- `Platform.getLocaleName()` and DesktopPlatform's override are left in place (unused by the dialog now).
- The Korean font references (`uiskin.json`, `UI.java`) are untouched (Task 2.3).


## オーケストレーターによる追加確認(ブラウザ版、2026-11-08 09:40Z ごろ)

- このブランチで `./gradlew :html:dist` を再実行し、`html/build/dist/assets/bundles/` に `bundle.properties` と `bundle_ja.properties` の 2 ファイルだけが含まれることを確認した(ハッシュ付きファイル名)。
- Playwright + Chromium(ヘッドレス)で `http://localhost:8765/index.html` を開き、Settings → Language を操作した。
  - ボタンは 2 つだけ。1 つ目のラベルは「ENGLISH」。
  - 2 つ目のボタンは存在し、`???` は表示されない。ラベル「日本語」は現在の `square.fnt` に字形が無いため空白(短い横線)として描かれる。字形の表示は Task 2.2 / 2.3 の範囲。
- ゲーム内ログ(`Got locale: …, bundle locale: …`)は、デスクトップをディスプレイ付きで起動できなかったため未取得。代わりに libGDX 1.13.1 の `I18NBundle.createBundle` を同じ引数で直接呼び、`ja_JP` → `ja`、`en_US` → ルート、`fr` → ルートになることを確認した(上記の表)。
