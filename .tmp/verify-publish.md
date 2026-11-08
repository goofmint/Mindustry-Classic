# Task 5.4 検証記録: 公開(Issue #30)

確認日: 2026-11-09。ブランチ `issue-30-publish`(Task 1.x〜5.1 の上)。Task 5.3(バランス調整)は別ブランチで並行しているため、README には調整で変わる数値を書いていない。

## 変更内容

### CI と Gradle(`999e00d`)
- `.github/workflows/build.yml`: 既存の `build` ジョブ(単体テスト、`:html:dist`、`html-dist` の artifact)はそのまま残し、最後に `actions/upload-pages-artifact@v3`(`path: html/build/dist`)を足した。出力の確認に `test -f html/build/dist/.nojekyll` を足した。
- 新しい `deploy` ジョブ: `needs: build`、`if: github.event_name == 'push' && github.ref == 'refs/heads/main'`、`permissions: pages: write / id-token: write`、`environment: github-pages`(URL は `deploy-pages` の `page_url`)、`concurrency: group: pages, cancel-in-progress: false`。手順は `actions/configure-pages@v5` → `actions/deploy-pages@v4`(`enablement` は使わない。後述)。pull request と手動実行ではデプロイしない。
- `html/build.gradle` の `dist`: コピーの後に `build/dist/.nojekyll`(空ファイル)を書く。ローカルの成果物と Pages の成果物を同じにするため、ワークフローではなく Gradle で作った。Pages の Actions 方式では Jekyll は動かないので実害の回避ではなく念のため。なお `upload-artifact@v7` は既定で隠しファイルを含めないので、`html-dist` の artifact には `.nojekyll` が入らない(Pages 用の artifact には入る)。

#### 計画からの逸脱: `gh-pages` ブランチではなく Pages の Actions 方式
タスクの文言と CodeRabbit の計画は「`gh-pages` ブランチに push して Deploy from a branch」だった。`GITHUB_TOKEN` で push したコミットは Pages のビルドを起動しないため、その方式は PAT のシークレットが要る。公式の Pages Actions 方式(`upload-pages-artifact` → `deploy-pages`)はシークレットが不要なので、こちらにした。`gh-pages` ブランチは作られない。

#### 確認したが直さなかったもの
- `clean` と `compileGwt` の順序: `./gradlew :html:dist --dry-run` で `:html:clean` → `:core:compileJava` → `:html:addSource` → `:html:compileJava` → `:html:processResources` → `:html:compileGwt` → `:html:dist` の順。現状は壊れていないので `mustRunAfter` は足していない(順序の制約が無く、Gradle のタスク名順に頼っている点は残る)。
- `core/assets/version.properties`: ルートの `writeVersion()` を `desktop/build.gradle` の `dist` タスクの設定ブロックが呼ぶので、設定フェーズで毎回生成される(`org.gradle.configureondemand=false`)。`:html:dist` だけを実行しても `compileGwt` より前に生成され、成果物に `assets/version-<hash>.properties` として入っていることを確認した。変更なし。
- ルート相対 URL: `html/webapp/index.html` は `styles.css`、`soundmanager2-*.js`、`html/html.nocache.js` をすべて相対パスで読む。`HtmlLauncher` のロゴは `GWT.getModuleBaseURL()` 基準。`styles.css` に `url()` は無い。変更なし。

### README(`1e9072e`)
日本語を先、英語の要約を後にした。公開 URL `https://goofmint.github.io/Mindustry-Classic/`、ゲームの流れ(実装済みの範囲。数値は書いていない)、`DefaultKeybinds` の既定値に合わせた操作表(設定の「操作」で変更可、タッチは保証しない)、ブラウザ版の制約(マルチプレイとマップエディタなし、セーブはローカルストレージ、自動保存は既定で無効 = `Control` の `save-N-autosave` の既定値が `!gwt`)、JDK 17 と `./gradlew :html:dist` + HTTP サーバのビルド手順、`desktop` は開発用、CI と公開、ライセンスとクレジット(GPL-3.0 継承、本家へのリンク、美咲フォント、フォークの主な変更点、対応するソースはこのリポジトリ)。本家の「no longer maintained」告知は「本家リポジトリの告知」という見出しの下に引用で残した。ビルドバッジは残した。

### 変更告知(`53a9961`)
- `LICENSE`: `git diff --name-status 92e28d8 HEAD -- LICENSE` の出力は空(変更なし)。`92e28d8`(Anuken、2021-03-24「pre-archive readme changes」)は本家の最後のコミットで、このフォーク最初のコミット `d487a23`(`.tmp/` の文書を追加)の親。
- 対象: `git diff --name-only --diff-filter=AM 92e28d8 HEAD -- core/src desktop/src html/src '*.gradle' 'tools/*.py'` の 109 ファイル(Java 94、GWT モジュール XML 3、Gradle 5、Python 7。テストの Java も含む)。削除したファイル(17)は対象外。
- 告知の文言: `Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).`(Java と Gradle は `//`、XML は `<!-- -->`、Python は `#`)。
- 位置: 既存のヘッダが無いファイルは先頭。Apache-2.0 ヘッダ(`Copyright 2011 See AUTHORS file.`)を持つ `ucore/scene/Scene.java`、`Skin.java`、`style/SpriteDrawable.java`、`style/TiledDrawable.java` はヘッダの閉じ `*/` の直後に足し、ヘッダは変えていない。XML は `<?xml?>` 宣言(と `<!DOCTYPE>`)の後、Python は shebang の後。
- 触っていないもの: `LICENSE`、`soundmanager2*.js`、`html/webapp/*`、bundle、`AboutDialog` のクレジット文(`AboutDialog.java` にはコメント 1 行だけ足した)。`.travis.yml` は存在しない。
- **所有者に確認してほしいこと**: 変更者・著作権者の名前は git の作者名 `Atsushi Nakatsugawa` を使った。別の名義(会社名など)にする場合は一括置換で直せる。

## ローカルでのサブパスの確認
Pages のプロジェクト URL(`/Mindustry-Classic/`)と同じ条件にするため、`html/build/dist` を一時ディレクトリの `Mindustry-Classic/` にコピーし、親ディレクトリを `python3 -m http.server 8766 --bind 127.0.0.1` で配信して、Playwright(Chromium headless、SwiftShader)で `http://127.0.0.1:8766/Mindustry-Classic/index.html` を開いた。

- メニュー(Play / Settings / About)が表示された: `.tmp/verify-5.4/01-subpath-menu.png`
- コンソール(`.tmp/verify-5.4/01-subpath-console.log`): エラー 0。`requestfailed` は音楽の mp3 5 件(`net::ERR_ABORTED`。これまでの検証と同じく、ヘッドレスでの音楽の読み込み中断)だけ。
- サーバのログ(`.tmp/verify-5.4/01-subpath-server-requests.log`): 104 件がすべて `/Mindustry-Classic/` 以下で、200 が 77 件、304 が 27 件。404 とサブパス外へのリクエストは 0 件。
- ゲームの開始、セーブと再読み込みはこのタスクでは確認していない(Task 5.1 の記録で確認済みの範囲と同じビルド構成)。

## テスト
- `./gradlew :core:test :desktop:compileJava`: 成功(119 件、失敗 0)
- `./gradlew :html:dist`: 成功。`html/build/dist/.nojekyll` が出る
- `python3 -I tools/check_bundles.py`: OK

## 所有者の手作業(残り)
1. (必須。マージの前に行う)GitHub の Settings → Pages → Build and deployment → Source を「GitHub Actions」にする。2026-11-09 時点で `gh api repos/goofmint/Mindustry-Classic/pages` は 404(Pages 未設定)。`configure-pages` の `enablement: true` は `GITHUB_TOKEN` では権限不足で失敗する(action の定義でも別トークンが要ると明記されている)ため、ワークフローでは使っていない。設定前にマージすると初回の `deploy` ジョブは失敗するので、設定後に Re-run する。
2. このブランチを `main` にマージする → `build` の後に `deploy` ジョブが動く。
3. デプロイの完了後、`https://goofmint.github.io/Mindustry-Classic/` を開き、メニューの表示、マップの開始、ウェーブの進行、セーブと再読み込みを確認する。
4. Settings → Environments → `github-pages` の配置ブランチの制限は既定で `main` のみ。変えなくてよい。

## 未完了
- 完了条件「公開 URL でブラウザから遊べる」は、初回のデプロイまで確認できない。`.tmp/tasks.md` の Task 5.4 は README と LICENSE の 2 項目だけチェックし、CI / Pages の項目は公開 URL での確認まで未チェックのままにした。
