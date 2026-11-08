# Task 3.2 検証記録

## 変更
- `core/src/io/anuke/mindustry/resource/Item.java`: `credit`(id 0)、`igpu`(1)、`pcie`(2)を先頭に宣言。旧 8 種はその後に残す(Task 3.11 で削除)。`MAX_ITEMS = 127`、`checkRegistrationLimit()` / `checkRegistrationLimit(int)`(超過で `RuntimeException`、件数と上限を表示)。静的宣言の末尾で `CodeItem.registerAll()`。コンストラクタと `init()` は変更なし
- `core/src/io/anuke/mindustry/resource/CodeItem.java`(新規): 設計書 §2.2 の公開インターフェース。出発点(stage 0〜3、passed 0)から REVIEW+1、REVIEW+2、TEST+1、CI+1 の遷移で到達探索し、重要度 → 通過状態 → 段階の昇順で `code-q<stage>-p<passed>-i<importance>` を登録。正引きは `Item[stage][passed][importance-1]`、逆引きは `ObjectMap<Item, CodeAttr>`(Item ごとに 1 個の `CodeAttr`)。未登録・範囲外は `IllegalArgumentException`(フォールバックなし)。`advance()` は通過ビット以外の値、負の `stages`、コードでない Item で `IllegalArgumentException`
- `core/src/io/anuke/mindustry/Mindustry.java`: `init()` の最初の文で `Item.checkRegistrationLimit()`
- `bundle.properties` / `bundle_ja.properties`: `item.credit.name`(credits / 得点)を `item.igpu.name` の直前に追加。「得」「点」は `tools/chars_ja.txt` に収録済みのためフォント再生成は不要
- テスト: `core/src/test/java/io/anuke/mindustry/resource/CodeItemTest.java`(16 件)、`ItemTest.java`(6 件)

## 結果
- `./gradlew :core:compileJava :desktop:compileJava :core:test`: BUILD SUCCESSFUL。CodeItemTest 16 / ItemTest 6 / SmokeTest 1、計 23 件、failures=0 errors=0
- 登録数: コード 70 種、全体 81 種(credit・igpu・pcie + 旧 8 + コード 70)。最後の `code-q6-p7-i2` は id 80
- passed ごとの段階範囲(0:0–3、1:1–5、2/4:1–4、3/5:2–6、6:2–5、7:3–6)が両重要度で一致
- クラス初期化順: `CodeItem.registerAll()` を先に呼ぶ / `CodeItem.get()` を先に呼ぶ / `Item.credit` を先に触る、の 3 通りを一時プログラム(scratchpad、非コミット)で実行し、いずれも total=81
- `Item` の静的初期化は `Vars` や libGDX ネイティブに触れない(`Array` / `ObjectMap` のみ)。テストは libGDX 起動なしで通る
- `python3 -I tools/check_bundles.py`: OK(ja 462 キー)
- `./gradlew :desktop:run` は未実施。アイコン(`icon-credit`、`icon-code-*` など)が未作成で、Task 3.3 の起動時検査で落とす前提のため、起動確認は Task 3.3 で行う。GWT(`:html:dist`)も Task 3.3 でまとめて確認する
