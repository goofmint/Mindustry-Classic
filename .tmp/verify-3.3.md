# Task 3.3 検証記録

## 変更
- `tools/gen_code_base.py`(新規、Python 標準ライブラリのみ): `core/assets-raw/sprites/code-base/q<0-6>-i<1-2>.png`(8×8 RGBA、14 枚)と `icon-credit.png`(8×8 の金貨)を書く。色は設計書の表、i1 は丸、i2 は星。どちらも左上 7×7 に収め、最下段(y=7)は透明。塗りは単色(段階は色で読むので陰影なし)
- `core/assets-raw/sprites/icon-igpu.png`(`icon-coal.png` のコピー)、`icon-pcie.png`(`icon-uranium.png` のコピー)。`icon-coal` / `icon-uranium` は残す
- `desktop/src/io/anuke/mindustry/desktop/tools/IconGenerator.java`(新規): `javax.imageio` / `java.awt.image` のみ。`Item.getAllItems()` を `CodeItem.attrOf()` で絞り、基本画像を ARGB に写して (5,7) 白 `#FFFFFF`(REVIEW)、(6,7) 黒 `#000000`(TEST)、(7,7) 緑 `#3AC83A`(CI)を置く。既存の `icon-code-*.png` を先に消す。基本画像が無い・8×8 でない・ドット位置が透明でないときは、ファイル名付きで例外。書いた数が 70 でなければ例外。`Item.init()` や `Draw` は呼ばない
- `desktop/build.gradle`: `genIcons`(JavaExec、`classes` に依存、作業ディレクトリはリポジトリのルート)
- `core/build.gradle`: `pack.mustRunAfter ':desktop:genIcons'`
- `Item.missingIcons(Predicate<String>)`(ucore の `Predicate`、GWT 可): `icon-<name>` が無い Item の領域名を登録順に返す
- `Control` のコンストラクタ: `Core.atlas = new Atlas(...)` の直後、`item.init()` の前に `Item.missingIcons(Core.atlas::hasRegion)` で検査し、欠けがあれば `RuntimeException("Missing item icons: ...")`(全件列挙)。ucore の `Atlas.findRegion()` は無い領域に `error` 画像を返す(フォールバック)ので、`hasRegion` で先に止める必要がある。`init()` の後にも全 Item の `region != null` を同じ形式で検査
- 生成物: `icon-code-q*-p*-i*.png` 70 枚、`core/assets/sprites/sprites.atlas`、`sprites.png`(ページは引き続き `sprites.png` の 1 枚、1024×512)
- テスト: `core/src/test/java/io/anuke/mindustry/resource/ItemIconsTest.java`(集合ベースの述語で、欠けなし → 空、2 件欠け → 2 件とも登録順で列挙)

## 結果
- `./gradlew :desktop:genIcons :core:pack`: `IconGenerator: wrote 70 icons to core/assets-raw/sprites`。`icon-code-*.png` は 70 枚
- `sprites.atlas`: `icon-code-*` 70、`icon-credit`、`icon-igpu`、`icon-pcie`、旧 8 種(`icon-stone` 〜 `icon-sand`)あり。基本画像は `q0-i1` 〜 `q6-i2` の名前で入る(一部は同じ画素の `icon-code-q*-p0-*` のエイリアス)。領域名の重複なし。ヘッダーは gdx-tools 1.13.1 の書式(`size: 1024, 512` など)に変わった(Task 1.6 で観察済みの挙動)
- 再生成の再現性: genIcons + pack をやり直した結果の `sprites.atlas` / `sprites.png` はバイト単位で一致
- `./gradlew :core:test`: CodeItemTest 16 / ItemTest 6 / ItemIconsTest 2 / SmokeTest 1、計 25 件、failures=0 errors=0
- `./gradlew :desktop:run`(約 45 秒、バックグラウンド): ログは `Got locale: ja, bundle locale: ja`、`Total blocks loaded: 96` で例外なし。強制終了まで動作
- 否定テスト: `icon-credit.png` と `icon-code-q6-p7-i2.png` を一時的に退避して `:core:pack` → `:desktop:run` で `java.lang.RuntimeException: Missing item icons: icon-credit, icon-code-q6-p7-i2`(`Control.<init>`、Control.java:66)。PNG を戻して genIcons + pack し、atlas・png が退避前と一致することを確認、`:desktop:run` が再び例外なしで起動
- genIcons の否定テスト: `code-base/q0-i1.png` を退避すると `IOException: Base image not found: core/assets-raw/sprites/code-base/q0-i1.png` でビルド失敗。戻して再生成(70 枚)
- `./gradlew :html:dist`: BUILD SUCCESSFUL(GWT の `*.cache.js` が再生成されたことを確認)。ブラウザでの表示確認は未実施
