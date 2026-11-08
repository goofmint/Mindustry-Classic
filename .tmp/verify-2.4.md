# Task 2.4 verification (Issue #10)

- Date: 2026-11-08
- Branch: `issue-10-ja-bundle` (stacked on `issue-9-fontswitch`)
- JDK 17, Gradle 8.14, libGDX 1.13.1

## Key counts

| | keys |
| --- | --- |
| `bundle.properties` upstream (master) | 553 (552 lines in `wc -l`; see below) |
| `bundle.properties` at the start of this task | 555 (`text.locale.en` / `text.locale.ja` were added in Task 2.3) |
| new English keys added in this task | 9 |
| `bundle.properties` now | 564 |
| excluded (`tools/bundle_exclude_keys.txt`) | 153 |
| `bundle_ja.properties` now | 411 (= 564 - 153) |

553 vs 552: the upstream file has no newline after the last line (`block.enemyspawn.name=enemyspawn`), so `wc -l` counts 552 newlines for 553 keys. The file still ends without a newline; the inserts in this task kept it that way.

### Exclusions by section (`tools/bundle_exclude_keys.txt`)

Every section comment says these keys are deleted together with the English keys in Task 3.11.

| section | keys | contents |
| --- | --- | --- |
| blocks | 33 | name / fulldescription of the 17 blocks with keys among the 18 removed in Phase 3 (steelwall, duriumwall, compositewall (name only), duriumwall-large, sorter, smelter, crucible, coalpurifier, titaniumpurifier, oilrefinery, stoneformer, lavasmelter, stonedrill, irondrill, titaniumdrill, omnidrill, weaponfactory). `siliconextractor` has no keys |
| items | 6 | `item.stone/iron/steel/titanium/dirium/sand.name`. `item.coal.name` and `item.uranium.name` are kept and translated (Task 2.5 renames them to iGPU / PCIe) |
| tutorial | 47 | the 44 `tutorial.*.text` bodies, `text.tutorial.back`, `text.tutorial.next` (only `Tutorial.java`), `map.tutorial.name` (map deleted with the tutorial; `visible: false`). The menu button `text.tutorial` is kept |
| multiplayer | 67 | keys referenced only from `HostDialog`, `JoinDialog`, `TraceDialog`, `BansDialog`, `AdminsDialog`, `PlayerListFragment` (hidden unless `Net.active()`), `NetClient`, `NetServer`, plus unreferenced multiplayer keys (`text.public`, `text.server.port`, `text.nohost`, ...) and the dynamic `text.server.kicked.*`. Kept: `text.joingame` (menu), `text.hostserver` / `text.host` (pause menu), shared keys (`text.ok`, `text.cancel`, `text.confirm`, `text.save.map`, `text.save.wave`, ...) |

Not excluded on purpose: floor block names, `block.steelwall-large.*`, all 44 `text.blocks.*`, `upgrade.*`, `text.purchased` / `text.upgrades` / `text.weapons` (not in the four planned sections; can be dropped later with the weapon factory).

## Translation

- `core/assets/bundles/bundle_ja.properties`: UTF-8 without BOM, flat `key=value`, same order as `bundle.properties`, 3 header comment lines
- Labels and stats use plain form / noun endings; dialogs and messages use です・ます. Japanese sentences use full-width `！` `？`; `…` for "..."
- Block names and descriptions follow the upstream meaning (AI SDLC names are Task 2.5). Weapon turrets are 砲台 (the design glossary term); repair turrets are 修復砲台
- `{n}` arguments, `\n`, color tags and literal `[[` are kept. No `'`, no `{{`, no `???` in any value (`Bundles.has()` treats `???` as missing)
- `text.blocks.*`: no half-width `:` in stat names (the code appends `: value`); `extrainfo` keeps `[accent]` and the trailing `:`; `repairssecond` = 修復回数/秒
- All stat names produced by the existing `getStats()` (`list.add("[xxx]Name: ")`) map to an existing `text.blocks.*` key (checked with grep; no missing key)

## Hardcoded English converted (menu -> settings path)

| file | before | after |
| --- | --- | --- |
| `MenuFragment` | desktop Play button `"$play"` (no such key, showed `play`) | `"$text.play"` |
| `MenuFragment` | version line `Version.buildName` (= `"build " + N` when numeric) | `Bundles.format("text.version.build", N)` when `Version.build != -1`; otherwise the raw `build` value from `version.properties` as before |
| `KeybindDialog` | `"Rebind"` x2 | `text.keybind.rebind` (new) |
| `KeybindDialog` | `"Reset to Defaults"` | existing `text.settings.reset` (same English text) |
| `KeybindDialog` | `"Press an axis or key..."` / `"Press a key..."` | `text.keybind.press.axis` / `text.keybind.press.key` (new) |
| `KeybindDialog` | device label `"Keyboard"` / `InputDevice.name` | `text.keybind.device.<DeviceType name>` -> `text.keybind.device.keyboard` (new) |
| `KeybindDialog` | non-axis binding name `Strings.capitalize(key)` | `Strings.capitalize(Bundles.get("keybind."+key+".name", key))`, the same lookup the axis branch already used (fallback only when the key is absent) |
| `LevelDialog` | custom map `"Delete"` | existing `"$text.delete"` |

- New `keybind.*` keys (only those in `DefaultKeybinds`): `chat_history_prev`, `chat_history_next`, `chat_scroll` (after `keybind.chat.name`), `toggle_menus` (after `keybind.rotate.name`). `cursor_x` / `cursor_y` no longer exist after Task 1.4, so they were not added
- `Inputs.java` was not edited. Its `"Keyboard"` is created in a static initializer that can run before `Core.bundle` exists, and `Controller N` is already gone (Task 1.4). The name is no longer displayed: the dialog reads the key from the device type instead
- No English key was deleted

## Checker (`tools/check_bundles.py`)

Clean run:
```
$ python3 tools/check_bundles.py
en keys: 564, excluded: 153, expected in ja: 411, ja keys: 411
OK
(exit 0)
```

Forced failures (copies in the scratchpad, each exit 1):
```
--- one key removed (text.settings.language)
error: missing in .../ja_missing.properties: text.settings.language
1 problem(s) found
--- {0} changed to {1} in text.wave
error: .../ja_args.properties:77: text.wave format arguments differ: en [0], ja [1]
--- text.ok / text.open swapped
error: .../ja_order.properties:62: key order differs from core/assets/bundles/bundle.properties: expected text.ok, found text.open
--- text.ok duplicated
error: .../ja_dup.properties:415: duplicate key text.ok (first at line 62)
--- exclusion entry block.nonexistent.name
error: .../excl_bad.txt:170: excluded key block.nonexistent.name does not exist in core/assets/bundles/bundle.properties
--- missing file
error: .../nope.properties: file not found
--- invalid UTF-8
error: .../bad.properties: invalid UTF-8 (...)
```
During the work it also caught two real omissions (`text.hostserver`, `text.host`) before the first commit of the translation.

CI: **not wired in this task.** `.travis.yml` and the CI files were not edited (CI is GitHub Actions on another branch). The orchestrator adds `python3 tools/check_bundles.py` to `.github/workflows/build.yml` later.

## Font regeneration

```
python3 tools/collect_chars.py      -> 772 code points (was 445)
python3 tools/bdf2fnt.py ...        -> 758 glyphs (was 431), page 256x256, lineHeight 8, base 7
python3 tools/check_fnt.py ...      -> 758 chars, alpha [0,255], no overlaps, no stray pixels
```
- The 14 missing code points are all from the always-added kana ranges (U+3094-3098, U+3099/309A, U+309F, U+30A0, U+30F7-30FA, U+30FF); none of them is used in the bundles
- Every character in `bundle_ja.properties` values is in `misaki.fnt` (checked with a script); no text had to be rephrased
- Re-running `bdf2fnt.py` gives byte-identical `misaki.fnt` / `misaki.png`

## Build

```
./gradlew :core:compileJava :desktop:compileJava   -> exit 0 (only existing deprecation/unchecked notes)
./gradlew :html:dist                                -> BUILD SUCCESSFUL in 22s
html/build/dist/assets/assets.txt lists bundle.properties, bundle_ja.properties, ui/misaki.fnt, ui/misaki.png
```
`html/build/dist` is left in place. `core/assets/version.properties` is not committed.

## Pending: orchestrator browser check (locale ja)

- [ ] Menu: プレイ / エディター / 設定 / このゲームについて / 寄付; the version line at the bottom left (the local build shows `Mindustry classic / custom build`: `custom build` is the raw value from `version.properties`, written by `build.gradle` when no `buildversion` is given, so it stays English. Only `build N` is translated)
- [ ] Play menu: 新しいゲーム / ゲームに参加 / チュートリアル / ロード
- [ ] Level select: title, ゲームモード / 難易度 labels and buttons, "?" mode help, map names, ハイスコア line
- [ ] Settings: ゲーム / グラフィック / サウンド / 操作 / 言語 sub-screens, slider values (`{0} 秒`), 初期設定に戻す
- [ ] Language dialog and the restart message
- [ ] Controls dialog: title キー設定, キーボード, binding names, 変更 buttons, 初期設定に戻す, the "キーを押してください…" popup (desktop only; the browser build may not show the controls entry)
- [ ] Block info dialog ("?"): description wrapping, ブロックの詳細情報 and the stat lines
- [ ] HUD: 次のウェーブまで N (wider than the English text; the label has minWidth 126 and grows)
- [ ] Button widths: long labels such as このゲームについて (9 chars) in the 200 px menu buttons

Known English left: tutorial texts (excluded, deleted in Phase 3), multiplayer screens (excluded), `custom build`, key names shown by `Input.toString()` (W, Mouse Left, ...).

## Uncertain translations

- `text.blocks.powerdraindamage` 電力消費/ダメージ, `text.blocks.lasertilerange` レーザーの届くタイル数, `text.blocks.inaccuracy` 弾のばらつき
- `setting.difficulty.insane/purge` 超むずかしい / 地獄
- Static wall blocks (`sandblock` 砂の塊, `grassblock` 草の塊, ...) and `block.blockpart` ブロックの一部
- `text.save.export/import` 書き出す / 取り込む (LoadDialog buttons)
