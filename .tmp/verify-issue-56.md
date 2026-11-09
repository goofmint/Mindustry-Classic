# Issue #56 検証記録: ブラウザ版の TextField への日本語入力(IME)

## 概要
- 日時: 2026-11-09
- ブランチ: `issue-56-ime`(`issue-57-wrap` から分岐)
- 範囲: 計画の Phase 1(ソースから分かる事実の整理)と、実機 IME での確認手順の準備。IME 対応の実装はしていない。コードの変更はない
- 結論(静的): libGDX 1.13.1 の GWT 入力は `keydown`/`keypress`/`keyup` だけを受け取り、`compositionstart/update/end`、`beforeinput`/`input`、`paste` を登録していない。また、ページに編集可能な DOM 要素がない。このため、**実機の IME でも日本語の確定文字列は TextField に届かない可能性が高い**。ただし実機での確認はまだで、下の手順をオーナーが行う必要がある

## 入力の経路

```
ブラウザのキーイベント(document、バブリング段階)
  └ DefaultGwtInput.handleEvent()            ※ keydown / keypress / keyup だけ
      ├ keydown  → keyForCode(keyCode) → InputProcessor.keyDown(code)
      │            (BACKSPACE のときだけ keyTyped('\b') も送る)
      ├ keypress → (char) charCode      → InputProcessor.keyTyped(c)
      └ keyup    → InputProcessor.keyUp(code)(TAB のときだけ keyTyped('\t'))
  └ InputMultiplexer → Scene(ucore)の keyTyped
      └ TextField.keyTyped(c)
          ├ onlyFontChars(既定 true)のとき style.font.getData().hasGlyph(c) が false の文字は捨てる
          ├ UI.showTextInput() のフィルター: 12 文字まで(セーブ名、名前変更)
          └ 描画: TextField のフォント(日本語ロケールでは美咲、英語では square)
```

## ソースから確定した事実
参照: `gdx-backend-gwt-1.13.1-sources.jar` と `gdx-1.13.1-sources.jar`(Gradle キャッシュから、リポジトリ外に展開して読んだ)。行番号は 1.13.1 のソースのもの。

1. `HtmlLauncher` は `createInput` を上書きしていない。`GwtApplication.createInput()`(496〜498 行)が `new DefaultGwtInput(canvas, config)` を返す
2. `DefaultGwtInput.hookEvents()`(623〜639 行)が登録するのは、canvas と document の `mousedown`/`mouseup`/`mousemove`、canvas のホイール、**document の `keydown`/`keyup`/`keypress`**、window の `blur`、canvas の `touch*` だけ。`compositionstart`/`compositionupdate`/`compositionend`、`beforeinput`/`input`、`paste` は登録していない(ソース全体を `grep` しても出てこない)
3. 文字は `keypress` の `charCode` から作る(753〜762 行: `char c = (char)e.getCharCode(); processor.keyTyped(c)`)。IME の変換中の `keydown` は `keyCode` 229 になるが、`keyForCode` の `default` は `Keys.UNKNOWN`(1128〜1129 行)なので、ゲームには「不明なキー」として届くだけ
4. キーイベントを処理するのは `hasFocus` が true のときだけ。`hasFocus` は canvas の `mousedown` で true、canvas の外のクリックで false になる(649〜660 行)。DOM のフォーカスとは無関係
5. ページには `<input>`/`<textarea>`/`contenteditable` がなく、`document.activeElement` は `BODY` のまま(下の Playwright の観測)。ブラウザが IME を有効にする対象(編集可能な要素)がない
6. `setOnscreenKeyboardVisible` は空実装(308〜313 行)。`openTextInputField` も空
7. `GwtClipboard.getContents()` は内部のフィールド `content` を返すだけで(38〜40 行)、OS のクリップボードを読まない。`setContents` で入れた文字列しか貼り付けられない
8. `TextField` は `onlyFontChars` が既定で true で、`style.font.getData().hasGlyph(c)` が false の文字を捨てる。`BitmapFontData.hasGlyph` は `missingGlyph`(`id=0` のグリフ)があれば常に true を返す
   - `square.fnt` は `char id=0` を持つので、英語ロケールではどの文字も受け付ける(表示は代替グリフになる)
   - `misaki.fnt` は `id=0` を持たず、`tools/chars_ja.txt` の 815 文字だけを収録している。日本語ロケールでは、**文字が届いても、この 815 文字以外の漢字は捨てられる**
9. `Platform.addDialog(field)` は既定では何もせず、`HtmlLauncher` の `Platform` も上書きしていない(ネイティブの入力ダイアログに逃がす経路はない)

## Playwright での観測(参考)
ヘッドレス Chromium で、ゲームの canvas をクリックした後に document へイベントロガー(下のスクリプトと同じもの)を付けて観測した。

| 操作 | document に届いたイベント |
|---|---|
| `keyboard.type('abc')` | 文字ごとに `keydown` → `keypress`(`charCode` 97〜99)→ `keyup`。`target` は `BODY` |
| `keyboard.insertText('日本語')` | **何も届かない**(`input` も `keypress` もない) |
| `keyboard.type('日')` | 何も届かない(US キー配列にない文字は `insertText` と同じ扱い) |

- `document.activeElement` は `BODY`(編集可能ではない)
- Task 5.2 で「設計書レビュー済v1」が「v1」だけになった現象(`.tmp/verify-web.md` の N2)は、事実 2 と 3 で説明できる。ASCII は `keypress` で届き、日本語は `keypress` が発生しないので届かない
- **これは Playwright の挙動で、実機の IME の結果ではない**

## 仮説(実機で確かめること)
- H1: 編集可能な DOM 要素がないので、ブラウザが IME を有効にしない、または composition が発生しない(ローマ字がそのまま `keypress` で入る)
- H2: composition は発生するが、確定文字列は `compositionend`/`input` でしか届かず、`keypress` が出ないのでゲームが受け取らない
- H3: 文字は `keyTyped` まで届くが、12 文字のフィルターや `hasGlyph`(事実 8)で捨てられる
- H4: 値は入るが、フォントにない字が代替グリフで表示される(英語ロケール)

静的には H1 か H2 の可能性が高い。日本語ロケールでは、美咲フォントにない文字がグリフによって拒否される(`TextField.keyTyped` は文字数フィルターより先に `hasGlyph` で拒否する)。文字数制限による拒否は、フォントにある文字でも起きうる。

## オーナーの手動確認の手順

### 準備
1. JDK 17 で `./gradlew :html:dist` を実行する
2. `html/build/dist` を HTTP で配信する(例: `cd html/build/dist && python3 -m http.server 8765`)。`http://localhost:8765/index.html` を開く
3. 対象: Chrome と Firefox(必須)、Safari か Edge(可能なら)。OS の日本語 IME を 1 種類以上(macOS の日本語入力、または Windows の Microsoft IME)
4. 開発者ツールのコンソールで、次のイベントロガーを実行する。`preventDefault` や伝播の停止はしないので、ゲームの動作は変わらない

```js
(() => {
  const types = ['keydown', 'keypress', 'keyup', 'beforeinput', 'input',
    'compositionstart', 'compositionupdate', 'compositionend', 'paste'];
  window.__imeLog = [];
  for (const t of types) {
    document.addEventListener(t, e => {
      const row = {
        type: e.type, key: e.key, code: e.code, keyCode: e.keyCode, charCode: e.charCode,
        isComposing: e.isComposing, data: e.data ?? null,
        target: e.target && e.target.nodeName, defaultPrevented: null,
      };
      window.__imeLog.push(row);
      // capture 段階では GWT(bubbling)の preventDefault がまだ走っていないので、
      // 配送が終わった後(setTimeout 0)に最終値を記録する
      setTimeout(() => {
        row.defaultPrevented = e.defaultPrevented;
        console.log(JSON.stringify(row));
      }, 0);
    }, true);
  }
  console.log('IME logger installed. document.activeElement =', document.activeElement.nodeName);
})();
```

5. 保存値の確認方法: IME の確定(Enter)だけではセーブ名は保存されない。各シナリオでは、入力欄を空にしてから入力し、ダイアログの保存ボタンで保存してから、開発者ツールの Application(Firefox は Storage)→ Local Storage で、そのスロットの `io.anuke.moment:save-<番号>-name` キー(libGDX の GWT 版 Preferences は「設定名:キー」の形で Local Storage に保存する。設定名は `Settings.load()` に渡している `io.anuke.moment`、キーは `Saves.java` の `save-<番号>-name`)の値を見る。前のシナリオの値(例: `abc`)と区別がつくよう、シナリオごとに別のスロットを使うか、ゲーム内の名前変更で別の名前にしてから保存前後の値を比べる(Local Storage のキーを直接消すと、ゲーム内に残った値が次の設定保存で書き戻されることがあるので、消さない)
6. 空欄のときの扱い: H1/H2 で文字が一切入らない場合、入力欄は空のままになる。`UI.showTextInput` は空欄では確認ボタンを無効にするので保存はできない。そのときは入力欄の表示と観測したイベントだけを記録し、結果表の「Local Storage の値」には「保存不可(空欄)」と書く。保存できたシナリオは上の手順のとおり値を見る

### 手順
各ブラウザで、英語と日本語の両方のロケールで行う。

1. 基準(ASCII): ゲームを開始 → メニュー → セーブ → 新しいスロット。入力欄をクリックし、`abc` を入力して保存する。ロガーに `keydown`→`keypress`→`keyup` が出ること、Local Storage の値が `abc` になることを確認する。13 文字目が拒否されることも確認する。ここで失敗する場合は IME の確認より先に記録する
2. IME(ローマ字入力): IME をオンにして `nihongo` と打ち、変換して Enter で確定する。記録すること
   - 変換候補のウィンドウが出るか、出る場合はどこに出るか
   - `compositionstart/update/end` が出るか
   - `keydown` が `keyCode=229`(`key="Process"`)で届くか
   - `keypress` が出るか、出る場合の `charCode`
   - 入力欄にローマ字や記号がそのまま入らないか
   - 確定後の入力欄の表示。続けて保存ボタンで保存し、そのスロットの Local Storage の値
3. IME の取り消し: 未確定のまま Esc を押す。ゲームのメニューやダイアログが閉じないか、入力欄に何か入らないかを記録する
4. ひらがなのまま確定: 変換せずに Enter で確定する
5. 可能ならかな入力モードで 2 を繰り返す
6. 貼り付け: OS のクリップボードに「日本語」を入れて、入力欄で Cmd+V(Windows は Ctrl+V)。`paste` イベントの有無と入力欄の結果を記録する(事実 7 から、OS のクリップボードの内容は入らない見込み)
7. 名前変更: ロード画面の鉛筆ボタンで既存のセーブの名前を変え、1〜6 のうち必要なものを繰り返す

### 結果表(記入用)

| シナリオ | ブラウザ | ロケール | 観測した DOM イベント | 入力欄の表示 | Local Storage の値 | 判定(H1〜H4) |
|---|---|---|---|---|---|---|
| ASCII `abc` | | | | | | |
| IME 変換して確定 | | | | | | |
| IME 取り消し(Esc) | | | | | | |
| ひらがなで確定 | | | | | | |
| 貼り付け | | | | | | |

### 判定の目安
- `compositionstart` が 1 つも出ず、ローマ字が `keypress` で入る → H1
- `compositionend`/`input` に確定文字列が出るが、`keypress` が出ず入力欄に何も入らない → H2
- `keypress` に日本語の `charCode` が出るのに入力欄に入らない → H3 の候補。ただしロガーは document の capture 段階で記録しているだけで、GWT の `DefaultGwtInput` は DOM の `document.activeElement` とは別に自前の `hasFocus`(canvas のクリックで立ち、外をクリックすると落ちる)を見て、false のときはキー入力をゲームへ渡さない。`TextField` も scene2d のキーボードフォーカスを確認する。そのため `keypress` が出ても `TextField.keyTyped` に届いたとは限らず、`activeElement` が `BODY` のままでもゲーム側はフォーカスを持っていることがある。H3 と判定する前に次を確認する
  1. ゲーム側のフォーカスと `keyTyped` への到達: 同じ入力欄で、直前に ASCII を 1 文字打って入ること(入れば `hasFocus` と scene2d のフォーカスが揃っていて、`keyTyped` に届いている)。確認後にその 1 文字は消す。入力欄のカーソルが点滅していることも併せて見る
  2. 対象の `keypress` の `charCode` が日本語の文字であること
  3. そのときの入力欄の文字数
  - 1 が確認できなければ「未判定(到達を確認できず)」と記録する
  - 1 と 2 を満たし、文字数が 12 未満で入らない → H3(グリフによる拒否。日本語ロケールなら美咲フォントにない字)
  - 1 と 2 を満たし、文字数が 12 以上で入らない → `TextField.keyTyped` は文字数フィルターより先に `hasGlyph` で拒否するので、その文字が `tools/chars_ja.txt` にある(=フォントにある)ことを確認できた場合だけ「H3(文字数制限。`UI.showTextInput` のフィルターは 12 文字に達すると次の文字を拒否する)」とする。確認できない場合は「H3(文字数制限またはグリフ)」と両方を候補として記録する
  - 結果表の判定欄には上の区分をそのまま書く
- Local Storage には日本語が入っているが、表示が四角などになる → H4

## 後続の対応(実機の結果を見て判断)
- H1/H2 なら、IME 対応には入力バックエンドの変更が要る(例: 隠した `<input>` にフォーカスを移して `compositionend`/`input` を `keyTyped` に流す)。Issue #56 は確認だけの Issue なので、実装するなら別 Issue にする
- 日本語ロケールでは、美咲フォントに収録した 815 文字以外は入力できても捨てられる(事実 8)。セーブ名に任意の漢字を許すなら、フォントの収録文字の扱いも決める必要がある
