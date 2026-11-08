# Issue #54 検証記録: ブラウザ版のセーブ一覧の日時が 1969 年になる

- ブランチ: `issue-54-savedate`(`issue-30-publish` から分岐)
- 確認日時: 2026-11-09 01:00〜01:10 JST(タイムゾーン Asia/Tokyo, UTC+9)
- ブラウザ: Playwright 1.61.1 同梱のヘッドレス Chromium(SwiftShader)、1280×800、毎回新しいコンテキスト
- 配信: `html/build/dist` を `python3 -m http.server 8766 --bind 127.0.0.1`

## 原因

libGDX 1.13.1 の GWT エミュレーション `java/io/DataInputStream.readLong()`(`gdx-backend-gwt-1.13.1-sources.jar`)は次の実装になっている。

```java
long a = readInt();
long b = readInt() & 0x0ffffffff;   // int リテラル(-1)とのAND。マスクにならない
return (a << 32) | b;
```

`0x0ffffffff` は `int` の -1 なので、下位ワードのビット 31 が 1 のとき `b` が負の `long` に符号拡張され、上位 32 ビットが全部 1 になる。結果は負の値(1970 年より前)になる。libGDX PR #7590 で修正済み。

書き込み(`DataOutputStream.writeLong`)は正しい。実際に保存されたバイト列を確かめた(下記)。

## 修正前の再現(修正前のビルド = `issue-30-publish` の `html/build/dist`)

1. 新しいゲーム(プロトタイプ、ウェーブ)→ メニュー → セーブ → 新規セーブ「slotA」
2. セーブ画面の表示: `Last saved: Fri, 19 Dec 1969 03:38:58`(`verify-issue-54/01-before-fix-1969.png`)
3. localStorage `io.anuke.moment:save-0-datas` の先頭 `AAAADwAAAaG7521uAAAA…` をデコード
   - バージョン 15、タイムスタンプのバイト列 `00 00 01 a1 bb e7 6d 6e` = 1794153868654(2026-11-09 01:04:28 JST、正しい)
   - 下位ワード `0xbbe76d6e` のビット 31 は **1**
   - 上記のエミュレーションで読むと `0xffffffffbbe76d6e` = -1142461074 ms = 1969-12-19 03:38 JST。表示と一致

現在時刻(2026-11-09 01:04 JST, `System.currentTimeMillis()` = 1794153785910)の下位ワードはビット 31 が 1。約 13 日後(1142543818 ms 後)にビット 31 が 0 に変わり、その後約 24.8 日はこのバグが出ない。**今日はブラウザで修正の効果を確認できる状態だった。**

## 修正

- `SaveFileVersion` に `protected static long readTimestamp(DataInputStream)` を追加。`readInt()` 2 回で上位ワード(符号付き)と下位ワード(`& 0xffffffffL`)を組み立てる
- `SaveFileVersion.getData()`(Save12〜14 が継承)と `Save15.getData()` のタイムスタンプ読み取りだけを置き換えた
- 書き込み処理、各バージョンの `read()`(フルロード、値は捨てている。8 バイトは正しく消費する)、表示処理(`LoadDialog` / `HtmlLauncher`)は変更していない。セーブ形式も同じ

## 修正後の確認(修正後に `./gradlew :html:dist` で再ビルド、新しいコンテキスト)

| 確認 | 結果 |
|---|---|
| 新規セーブ「fixed」を作成した直後のセーブ一覧 | `Last saved: Mon, 09 Nov 2026 01:07:14`(保存時刻 01:07 JST と一致)。下位ワード `0xbbe9f448`、ビット 31 は 1(`02-after-fix-save-list.png`) |
| ページ再読み込み後、タイトル → プレイ → ゲームをロード の一覧 | 同じく `Mon, 09 Nov 2026 01:07:14`(`03-after-fix-reload-load-list.png`) |
| そのスロットをロード | ウェーブ 1、同じマップと配置でゲームに戻った(`04-after-fix-loaded.png`) |

## 単体テスト

`core/src/test/java/io/anuke/mindustry/io/SaveTimestampTest.java`: `DataOutputStream.writeLong` で書いた値を `readTimestamp` で読み、下位ワードのビット 31 が 1 の値、0 の値、0 / -1 / `Long.MAX_VALUE` / `Long.MIN_VALUE` で元の値に戻ること、後続の `int` がずれないことを確認する。JVM での動作の確認であり、GWT のバグそのものは単体テストでは再現できない(GWT の確認は上のブラウザ確認で行った)。`./gradlew :core:test` 成功。

## 確認していないこと

- **デスクトップ版**: このセッションにはディスプレイが無く `:desktop:run` が起動しない(`.tmp/verify-desktop.md` 参照)。JVM の `readInt()` は標準動作なので、結果は従来の `readLong()` と同じはず(単体テストで確認)
- **修正前のビルドで作ったセーブを修正後のビルドで表示する確認**: 修正前のセーブは別のブラウザコンテキストに作ったため持ち越していない。書き込み側は変更しておらず、保存済みのバイト列(上のデコード結果)は正しいので、修正後のビルドでは正しい日時になるはず
