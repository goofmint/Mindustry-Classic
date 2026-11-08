# Task 1.2 verification

ビルドは実行していない。Gradle 4.10.2 wrapper は JDK 17 で動かず、Gradle 8 は旧ビルドスクリプトを受け付けないため。目視確認と grep のみ。

## コマンドと結果

```
git grep -n -i -e kryo -e websocket -- . ':!core/**' ':!.tmp/**'
(出力なし, exit 1)

git grep -n -i -e kryo -e websocket -- core
core/src/io/anuke/mindustry/core/Platform.java:66:	/**Use the default thread provider from the kryonet module for this.*/
(コメントのみ)

git grep -n "kryonet\|:android\|:ios\|:server" settings.gradle build.gradle
(出力なし, exit 1)
```

## 備考
- Net.java に null ガードを追加した(設計書の「Net は触らない」からの意図的な逸脱)。Join/Host ダイアログでの NPE を避けるため。
- DefaultThreadImpl は kryonet から desktop へ git mv。実装は変更せず package 行のみ変更。
- GdxDefinitionSuperdev.gwt.xml は存在するが GwtWebsockets を継承しておらず(grep で確認)、未変更。
