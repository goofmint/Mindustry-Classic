![Imgur](https://i.imgur.com/w4N0yhv.png)

# AI SDLC Classic(Mindustry Classic 改造版)

[![build](https://github.com/goofmint/Mindustry-Classic/actions/workflows/build.yml/badge.svg)](https://github.com/goofmint/Mindustry-Classic/actions/workflows/build.yml)

AI を使ったソフトウェア開発の流れ(AI SDLC)を、タワーディフェンスで疑似体験するブラウザゲームです。[Anuken/Mindustry-Classic](https://github.com/Anuken/Mindustry-Classic) をフォークして改造しています。

**遊ぶ**: https://goofmint.github.io/Mindustry-Classic/

インストールは要りません。PC のブラウザで開くと、そのまま遊べます。

## ゲームの概要

コードを書く設備と、品質を上げる施設をパイプラインでつなぎ、できたコードを本番環境に納品して得点を稼ぎます。

1. コード鉱脈の上に、コードを書く設備(人間のエンジニア、AI コーディングエージェント)を置きます。品質と重要度がランダムなコードが出てきます。
2. パイプライン(コンベア)でコードを運びます。PR(ルーター)はコードをためて、隣の施設に配ります。
3. レビュー(シニアエンジニア、CodeRabbit)、テスト(ユニットテスト、E2E テスト)、CI(Jenkins、GitHub Actions)の施設を通すと、コードの品質が上がります。同じ種類の施設をもう一度通しても、品質は上がりません。
4. コードを本番環境(コア)に納品すると、品質 × 重要度の得点が残高に入ります。品質がマイナスのコードは残高を減らします。
5. 低品質のコードを納品するほど、次のウェーブのバグ(敵)が増えます。バグは砲台などで迎え撃ちます。CodeRabbit はレビューしながらバグも撃ちます。
6. 残高が目標得点に届けば勝ちです。本番環境が壊されるか、残高がゼロを下回ると負けです。

AI エージェント、CodeRabbit、テスト、CI の施設はトークン(電力)を使います。トークンは発電施設で作ります。

マップは「プロトタイプ」「スプリント」「リリース」の 3 つです。ゲームモードは「ウェーブ」「サンドボックス」「フリービルド」から選べます。サンドボックスでは資源が無限のため、勝敗の判定はありません。

表示言語は日本語と英語です。設定の「言語」で切り替えます。

## 操作方法

既定のキー割り当てです。設定の「操作」で変更できます。

| 操作 | キー |
|---|---|
| 移動 | W / A / S / D |
| ダッシュ | 左 Shift |
| ブロックを置く / 撃つ | 左クリック |
| ブロックを撤去する | 右クリック |
| ブロックの向きを変える | R / E、またはマウスホイール |
| ズーム | 左 Ctrl を押しながらマウスホイール |
| 一時停止 | Space |
| メニュー | Esc |
| 画面のメニューの表示を切り替える | C |

マウスとキーボードでの操作を前提にしています。タッチ操作(スマートフォン、タブレット)は動作を保証しません。

## ブラウザ版の制約

- マルチプレイ(ゲームに参加、サーバーを立てる)は使えません。
- マップエディタはありません。
- セーブデータは、ブラウザのローカルストレージに保存されます。ブラウザのサイトデータを消すと、セーブデータも消えます。別のブラウザや別の端末には引き継がれません。
- 自動保存は、既定で無効です。セーブは、ゲーム中のメニューから手動で行ってください。

## ビルド

配布するのはブラウザ版(HTML/GWT)だけです。`desktop` モジュールは開発時の動作確認とツールの実行に使うもので、配布しません。

### 必要なもの

- JDK 17(JRE ではなく `javac` を含む JDK)。Gradle はラッパー(`./gradlew`)が取得するので、インストールは要りません。
- 初回の実行時はネットワーク接続(Gradle と依存ライブラリをダウンロードします)。

JDK 17 の入れ方の例:

- Homebrew(macOS): `brew install openjdk@17` のあと、
  `export JAVA_HOME=$(brew --prefix openjdk@17)/libexec/openjdk.jdk/Contents/Home` と `export PATH="$JAVA_HOME/bin:$PATH"`
- SDKMAN: `sdk list java` で 17.x の Temurin を選んでインストール
- Eclipse Temurin のインストーラ: https://adoptium.net/

`java -version` と `javac -version` が 17 を表示することを確かめてください。

### コマンド

リポジトリのルートで実行します。Windows では `./gradlew` の代わりに `gradlew.bat` を使います。

- `./gradlew :html:dist`: ブラウザ版を `html/build/dist` に出力します。`file://` では動かないので、HTTP サーバで配信して開きます。

  ```sh
  cd html/build/dist
  python3 -m http.server 8765
  # http://localhost:8765/index.html を開く
  ```

- `./gradlew :core:test`: 単体テストを実行します。
- `./gradlew :desktop:run`: デスクトップ版を起動します。開発用で、GUI が必要です。
- `./gradlew pack`: `core/assets-raw/sprites` の画像を `core/assets/sprites/sprites.atlas` と `sprites.png` にまとめます。この 2 ファイルは Git で管理しているので、通常のビルドでは不要です。

ツールチェーンは JDK 17 / Gradle 8.14(ラッパー)/ libGDX 1.13.1 / GWT 2.11.0 です。旧バージョンからの移行の記録は [`docs/UPGRADE.md`](docs/UPGRADE.md) にあります。

### CI と公開

GitHub Actions(`.github/workflows/build.yml`)が、push と pull request のたびに単体テストと `:html:dist` を実行し、`html/build/dist` を `html-dist` という artifact として保存します。`main` ブランチへの push では、同じ成果物を GitHub Pages に配置します。

## ライセンスとクレジット

このリポジトリは、Anuken さんの [Mindustry Classic](https://github.com/Anuken/Mindustry-Classic) のフォークです。本家の GPL-3.0 を引き継いでいます。ライセンスの全文は [`LICENSE`](LICENSE) にあります。

- 元のゲーム: [Anuken/Mindustry-Classic](https://github.com/Anuken/Mindustry-Classic)(制作 Anuken)。元のゲームのクレジットは、ゲーム内の「ゲーム情報」にあります。
- 日本語フォント: 門真なむ (Num Kadoma) さんの「美咲フォント」(美咲ゴシック第2、2021-05-05 版、配布元 https://littlelimit.net/misaki.htm)。取得元と変換手順は [`tools/README.md`](tools/README.md) にあります。

このフォークでの主な変更点:

- ゲーム内容を AI SDLC 向けに作り変えました(コードのアイテム、コードを書く設備、品質を上げる施設、得点による勝敗、3 つのマップ)。
- 表示言語を日本語と英語の 2 つにし、日本語の文言と日本語フォントを足しました。
- Android、iOS、サーバーのモジュールを削除し、ブラウザ版だけを配布するようにしました。
- ビルド環境を JDK 17 / Gradle 8.14 / libGDX 1.13.1 / GWT 2.11.0 に更新しました。

このフォークで変更・追加したソースファイルには、その旨を 1 行のコメントで記しています。公開しているゲームに対応するソースコードは、このリポジトリ(https://github.com/goofmint/Mindustry-Classic)にあります。

### 本家リポジトリの告知(Notice from the upstream repository)

以下は本家 Anuken/Mindustry-Classic の README にある告知です。本家についての告知で、このフォークについてのものではありません。

> This repository is **no longer being maintained.** No features will be implemented, no PR will be accepted, no bugs will be fixed and no additional releases will be made.
> If you find an issue and know how to fix it, feel free to fork this repo.

## English summary

A browser tower-defense game about the AI software development lifecycle (AI SDLC), built as a fork of [Anuken/Mindustry-Classic](https://github.com/Anuken/Mindustry-Classic).

- **Play**: https://goofmint.github.io/Mindustry-Classic/ (desktop browser; mouse and keyboard)
- **Gameplay**: place code-writing facilities on code veins, carry the code on pipelines through review, test and CI facilities to raise its quality, and deliver it to the production core. Each delivery adds quality × importance to your balance. Low-quality deliveries spawn more bugs (enemies). You win when the balance reaches the map's target; you lose if the core is destroyed or the balance drops below zero.
- **Controls** (defaults, rebindable in Settings → Controls): WASD move, Left Shift dash, left click place/shoot, right click remove, R/E or mouse wheel rotate, Left Ctrl + wheel zoom, Space pause, Esc menu, C toggle the HUD menus.
- **Browser limitations**: no multiplayer and no map editor. Saves are kept in the browser's local storage. Autosave is off by default.
- **Build**: JDK 17, then `./gradlew :html:dist`; serve `html/build/dist` over HTTP (it does not work over `file://`). The `desktop` module is for development only and is not distributed. Toolchain: JDK 17 / Gradle 8.14 (wrapper) / libGDX 1.13.1 / GWT 2.11.0; see [`docs/UPGRADE.md`](docs/UPGRADE.md).
- **CI**: `.github/workflows/build.yml` runs the tests and `:html:dist` on every push and pull request, and deploys the output to GitHub Pages on pushes to `main`.
- **License**: GPL-3.0, inherited from the upstream project ([`LICENSE`](LICENSE)). The Japanese text uses the Misaki font by Num Kadoma. Files changed or added in this fork carry a one-line modification notice. The corresponding source is this repository.
