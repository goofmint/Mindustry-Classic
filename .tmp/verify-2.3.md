# Task 2.3 verification (Issue #9)

- Date: 2026-11-08
- Branch: `issue-9-fontswitch` (stacked on `issue-8-font`)
- JDK 17, Gradle 8.14, libGDX 1.13.1

## Prerequisites (checked before the change)
- `core/assets/ui/misaki.fnt` / `misaki.png` exist, `uiskin.json` has `japanese` (misaki.fnt, markupEnabled true, scale 2)
- `Vars.locales` = `en`, `ja`; `core/assets/bundles/` has only `bundle.properties` and `bundle_ja.properties`

## Changes
### `core/assets/ui/uiskin.json`
- Added `japanese-chat: { file: misaki.fnt, markupEnabled: false, scale: 2 }`. It plays the same role for misaki as `default-font-chat` does for square, so `[` typed in the chat field is not parsed as markup
- Removed the `korean` entry; `git rm core/assets/ui/korean.fnt core/assets/ui/korean.png`
- `git grep -n -i korean` outside `.tmp/` is empty

### `core/src/io/anuke/mindustry/core/UI.java` (`loadSkin()`)
`loadSkin()` runs inside the `SceneModule` constructor, so `Vars.ui` is still null. The locale comes from `this.getLocale()`. `Settings` is already loaded at that point because `BundleLoader.load()` runs before `new UI()`. `getLanguage()` returns `ja` for both `ja` and `ja_JP`.

When the language is `ja`:
1. Keeps references to the square `default-font` and `default-font-chat` objects
2. `skin.add("default-font", japanese, BitmapFont.class)` and `skin.add("default-font-chat", japaneseChat, BitmapFont.class)`
3. `replaceStyleFonts()` walks `skin.getAll(<StyleClass>.class)` and replaces a font field only when it is the same object (`==`) as the old square font (-> `japanese`) or the old square chat font (-> `japanese-chat`). It never matches by name, so `title` stays `title.fnt`. Style classes handled (all of them are registered in uiskin.json and have a font field):
   - `TextButton.TextButtonStyle.font`
   - `ImageTextButton.ImageTextButtonStyle.font`
   - `CheckBox.CheckBoxStyle.font`
   - `Window.WindowStyle.titleFont`
   - `Label.LabelStyle.font` (`title` style untouched because its font is `title`)
   - `TextField.TextFieldStyle.font` (`default-font-chat` -> `japanese-chat`) and `.messageFont` (`default-font` -> `japanese`)
   - `List.ListStyle.font`
   - Not handled: `SelectBox.SelectBoxStyle` has a font field but is not registered in the skin. `TextTooltipStyle` and `KeybindDialogStyle` have no font field
4. Scale: `jscale = max(1, round(Vars.fontscale * 4))`, applied with `setScale(jscale)` and `setUseIntegerPositions(false)` to `japanese`, `japanese-chat` and the `Core.font` instance. `Vars.fontscale = max(dp(1)/2, 0.5)`, and dp is 1 on desktop/WebGL, so jscale is 2 there: a 16 px glyph box with a 14 px cap height, the same result as the 2.2 check. A 2x-density device gets fontscale 1 and jscale 4
5. The square `down += dp(4)` / `lineHeight -= dp(2)` adjustment is NOT applied to the misaki fonts. misaki.fnt has lineHeight 8, so at scale 2 lineHeight is 16. Adding dp(4) to `down` would make the line step 12 px for a 14 px tall glyph, and wrapped lines would overlap. (This is a deviation from the plan, which said to keep the adjustment unless it visibly breaks.)
6. `Core.font` = a separate misaki `BitmapFont`: `new BitmapFont(new BitmapFontData(japanese.getData().fontFile, false), japanese.getRegions(), false)`, markupEnabled true. Because it receives the page regions, it shares the texture with `japanese` and does not own it. Reasons:
   - `Core.font` must contain the Japanese glyphs. `Label.checkFallback()` (ucore) looks every character up in the static `Core.font`. When a glyph is missing, it switches the label to the first skin font that has the glyph, and with the current keys `ObjectMap` iteration finds `japanese-chat` (markup off) first. I checked this with a small program against gdx-1.13.1. If `Core.font` stayed square, every label with Japanese text would show `[accent]`-style markup literally
   - `Core.font` must NOT be the UI font object. `Draw.tscl()` sets the scale of `Core.font` directly, and `Renderer.drawPlayerNames()` calls `Draw.tscl(fontscale)` every frame in game, which would shrink the UI misaki font to 0.5 (4 px glyphs)
   - `SceneModule.loadContext()` sets `Core.font` only when it is null, so the explicit assignment in `loadSkin()` wins
7. Other locales: same loop as before for `default-font` and `default-font-chat` (only the `korean` argument was removed)

### `core/src/io/anuke/mindustry/ui/fragments/BlocksFragment.java` (`updateRecipe`)
- Name label `width(120f)` -> `width(160f)`. 「人間のエンジニア」 is 8 full-width characters, 8 x 16 px = 128 px at scale 2
- Health label `width(200)` -> `width(240)`
- The icon (40) and the info button (40x44) are unchanged

## Caveats
- Disposal: after the swap, the two square fonts are no longer referenced by the skin, so `Skin.dispose()` does not dispose them in `ja`. The `Core.font` misaki instance is not in the skin either; its texture belongs to `japanese`. Disposal is left as it was
- In-world text drawn through `Draw.text` (player names `Draw.tscl(0.125)`, the respawn Fx `0.25`, block debug) uses absolute scales tuned for square. With misaki these came out about 1/3 of the square size. Fixed in the PR review follow-up: `UI.worldTextScale()` multiplies the square scale by the ratio of the unscaled cap heights (square 20 / misaki 7), used by `Renderer.drawPlayerNames()` and the `Fx.respawn` text. The block/enemy debug text is left as is
- Labels with an absolute font scale: fixed in the follow-up below (`UI.labelScale()`)
- `FileChooser.updateFiles()` measures the path with `Core.font`. Once in game, `Draw.tscl(fontscale)` changes that font's scale, so the measurement can differ from the UI font. Minor
- `setUseIntegerPositions(false)` is kept for misaki. Check whether the dots look crisp or show half-pixel blur
- In English mode, the Language dialog's 「日本語」 label still goes through `Label.checkFallback()`. With the new key it may now pick `japanese-chat` instead of `japanese`. The text has no markup, so the display should be the same

## Build results
```
./gradlew :core:compileJava :desktop:compileJava   -> exit 0 (only existing deprecation/unchecked notes)
./gradlew :html:dist                                -> BUILD SUCCESSFUL in 22s (only existing GWT deprecation warnings)
html/build/dist/assets/ui: misaki-*.fnt / misaki-*.png present, no korean files
```

## How to switch
Settings -> Language -> 日本語 -> restart (reload the page in the browser build). Language "default" follows `Locale.getDefault()`, so a system locale of `ja` also switches.

## Visual checklist (for the orchestrator, browser build `html/build/dist`)
- [ ] Menu: buttons and labels use misaki at 2x; the logo (`title` font) is unchanged
- [ ] Settings dialog: tab/row labels, sliders, check boxes, the window title font
- [ ] Language dialog: 日本語 / English buttons; switch back to English and confirm square returns after restart
- [ ] Level select dialog: map names, buttons, wrapped text
- [ ] In-game HUD: wave label (scale 0.75, may be blurry), pause/respawn labels, place menu (block list, item table numbers)
- [ ] Place menu description panel: a long name (e.g. 人間のエンジニア) fits in 160 px next to the icon and the "?" button; the health line fits in 240 px; the panel does not overflow the screen
- [ ] Block info dialog ("?"): wrapped description at width 600, no overlapping lines
- [ ] Chat input (multiplayer only): typing `[` shows the character and does not start markup; chat messages render in misaki
- [ ] Line spacing of wrapped multi-line text (no overlap, no excessive gaps)
- [ ] Dot crispness (`setUseIntegerPositions(false)`)
- [ ] In-world text size (player names, respawn text): expected to be small, note it
- [ ] English mode: no visual change compared with before this branch

## Orchestrator check 1 (browser, Playwright + Chromium, locale=ja)
- OK: menu, settings, language dialog, level select, place panel (name 160 px / health 240 px), Japanese switching itself
- NG (before the fix below): in-game HUD. The wave label (`HudFragment`, `.scale(fontscale*1.5f)`) and the item counts above the place menu (`BlocksFragment.updateItems()`, `setFontScale(fontscale*1.5f)`) are tiny and unreadable. The cause is the absolute scale 0.75 applied to the 8 px misaki font

## Follow-up fix: absolute label scales
- Added `UI.labelScale(float squareScale)` (static), backed by `private static float japaneseScale`. `loadSkin()` sets `japaneseScale = jscale` in the ja branch, and the field stays 0 otherwise
  - other locales: returns `squareScale` unchanged, so the square look is exactly as before. The argument is the absolute scale the caller used to pass, which also keeps the 0.75 of the paused/respawn labels independent of `fontscale` as before
  - ja: ignores `squareScale` and returns `jscale` (an integer, the same as normal text, 2 at fontscale 0.5). The wave/item labels are therefore the same size as normal text in ja, not 1.5x. A non-integer or smaller scale makes the dot font unreadable
  - Deviation from the suggested `labelScale(mult)` returning `fontscale * mult`: the paused/respawn labels use a fixed 0.75, which is not `fontscale * 1.5` on 2x-density devices (fontscale 1). Passing the absolute square scale keeps every non-ja value identical
- Replaced exactly 4 call sites:
  - `HudFragment` wave label: `.scale(fontscale*1.5f)` -> `.scale(UI.labelScale(fontscale*1.5f))`
  - `HudFragment` paused label: `.scale(0.75f)` -> `.scale(UI.labelScale(0.75f))`
  - `HudFragment` respawn label: `.scale(0.75f)` -> `.scale(UI.labelScale(0.75f))`
  - `BlocksFragment.updateItems()` item count: `setFontScale(fontscale*1.5f)` -> `setFontScale(UI.labelScale(fontscale*1.5f))`
- Changed in the PR review follow-up: `Renderer.drawPlayerNames` and the `Fx` respawn text go through `UI.worldTextScale()` (see Caveats). The block/enemy debug text still uses the square scale; not checked visually (player names need a second player, respawn needs the player to die)
- `./gradlew :core:compileJava :desktop:compileJava` -> exit 0; `./gradlew :html:dist` -> BUILD SUCCESSFUL in 22s (output in `html/build/dist`)
- To check: in-game wave label, item counts, pause label (single player, pause) and respawn label are readable in ja and unchanged in en


## オーケストレーターによる再確認(HUD 修正後、2026-11-08 10:25Z ごろ)

- Playwright + Chromium(ヘッドレス)で `html/build/dist`(コミット 233b7e2 のビルド)を開き、設定 → 言語 → 日本語 → 再読み込みののち caldera を開始した。
- ja: HUD のウェーブ表示(「Wave 1 / Wave in 113」)、配置メニュー上部の資源個数(46)、一時停止表示(「< Paused >」)がいずれも通常文と同じ大きさ(美咲 scale 2)で読める。にじみなし。
- en: 同じ画面を英語で開き、ウェーブ表示・資源個数が従来どおり square フォント 0.75 倍で描かれることを確認(見た目の変化なし)。
- 日本語ラベルの折り返し(複数行)とチャット入力の `[` は、本家文言の日本語化(Task 2.4)以降で文言が入ってから確認する。
- コンソールの `Failed to load resource`(接続リセット)は、確認中に静的サーバーを再起動したことによる音楽ファイルの読み込み中断で、アプリの例外ではない。
