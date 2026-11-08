# Task 4.1 検証記録: マップ 3 枚

## 作ったもの
- `tools/gen_maps.py`(numpy + Pillow): 固定シードで `core/assets/maps/prototype.png`(id 0、seed 1001)、`sprint.png`(id 1、seed 2002)、`release.png`(id 2、seed 3003)を書く。256×256、非インターレースの RGB。`--check A.png ...` で既存 PNG だけを検査する。生成時も書き出し前と書き出し後に同じ検査をかけ、規則を破ったマップは書かない。
  - 手順: ノイズで床(stone/grass/blackstone/sand) → ノイズの岩壁と縁の壁 → 液体の池(水は中心を deepwater、sprint は蛇行する川) → 資源の塊(壁を上書き) → 敵出現点からコアまで 4 か所で曲がる幅 5 の通路を削る → コア周り半径 14 と出現点周り半径 4 の壁・液体を消す、開始基地の範囲を素の床に戻す、出現点から 9 未満の資源を床に戻す → 緑と赤を置く。
  - 2 回続けて生成し、3 枚の md5 が一致することを確認した(決定的)。
- 開始基地の範囲: `World.setDefaultBlocks()`(flipBase なし)はワールド座標でコアの x-3〜x+3、y-5〜y+1 を使う。PNG は y を反転して読む(`pixmap.getPixel(x, height-1-y)`)ので PNG 座標では x-3〜x+3、y-1〜y+5。検査はこれに 2 マスの余白を足した 11×11 の箱で、壁、液体、鉱石床、赤を禁止する。
- 歩ける床: `Tile.passable()` で壁と液体の床(water、deepwater、lava、oil は `solid = true`)は通れないので、それ以外を歩ける床として 4 近傍 BFS。3×3 の空き(周囲 8 マスも歩ける)でも BFS した。
- `maps.json`: id 0 prototype(targetScore 1000)、1 sprint(1500)、2 release(2000)。`visible: true`、`oreGen: true`、`backgroundColor: "646464"`(`LevelDialog` のカード背景で使う)、`flipBase` は省略(既定 false)。
- 本家 PNG 21 枚を削除(登録済み 12 枚 + 未登録の arena、blankmap、canyon、delta、oilrush、pit、test1〜3)。tasks.md の「22 枚」は Task 3.11 で消した tutorial.png を含む数。
- bundle: 両言語から旧 `map.<name>.name` 12 件を消し、3 マップの `name` と `description` を同じ位置に足した。prototype の説明はチュートリアルの代わりに、コード鉱脈に人間のエンジニア → パイプライン → 本番環境、途中のレビュー/テスト/CI、低品質で得点が減ること、CodeRabbit のレビューと迎撃を書いた。用語は bundle の既存名(pipeline/パイプライン、production/本番環境)に合わせた。
- `Map.localizedDescription()`: `Bundles.get("map."+name+".description")`。キーが無いときは Bundles の欠けたキーの表示(`???キー???`)がそのまま出る(フォールバックしない)。
- `LevelDialog`: 描画前に id 順に並べ替え、カードのハイスコアの下に `[lightgray]` の折り返しラベルで説明を出す。カスタムマップには説明キーが無いので出さない。クリック動作とカスタムマップの削除ボタンは変えていない。

## 生成スクリプトの検査結果(`python3 -I tools/gen_maps.py --check core/assets/maps/{prototype,sprint,release}.png`、終了コード 0)
全マップ共通: 256×256、色はすべて ColorMapper の色、紫なし、緑 1 点、資源 6 色あり、開始基地の範囲に壁/液体/鉱石/赤なし、赤から 9 マス未満に資源なし、出現点は縁から 6 マス以上内側。

| | prototype | sprint | release |
|---|---|---|---|
| 緑(PNG 座標) | (70,150) | (128,206) | (128,124) |
| 赤 | 1: (226,44) | 2: (36,30) (220,34) | 4: (22,22) (234,24) (232,232) (24,234) |
| 開始基地の箱 x0,x1,y0,y1 | 65,75,147,157 | 123,133,203,213 | 123,133,121,131 |
| コード鉱脈 c3a490 | 739 | 593 | 553 |
| iGPU 161616 | 247 | 247 | 161 |
| PCIe 83bc58 | 197 | 198 | 158 |
| 水 506eb4(+deepwater) | 761 (+196) | 1333 (+1104) | 146 (+46) |
| 石油 292929 | 230 | 278 | 127 |
| 溶岩 ed5334 | 114 | 238 | 642 |
| titanium(飾り) | 105 | 101 | 101 |
| 壁 | 6634 | 19897 | 26243 |
| 素の床 | 56311 | 41544 | 37354 |
| BFS 赤→緑 | OK(3×3 も OK) | 2 点とも OK(3×3 も OK) | 4 点とも OK(3×3 も OK) |
| コアから届く鉱脈/iGPU/PCIe | 739/247/197(全部) | 381/171/148 | 553/97/158 |
| コアから届く液体に隣接するマス(水/石油/溶岩) | 119/64/15 | 388/49/62 | 33/49/113 |

- 「届く」は壁と液体を越えずに歩いて行ける数。sprint は川の向こう側、release は壁に囲まれた一部の鉱脈に歩いては届かない。
- 床は stone/grass/blackstone/sand が中心なので、`oreGen: true` のノイズ鉱脈も上に乗る(ゲーム内で増える分は PNG の数に入っていない)。

## bundle とフォント
- `python3 -I tools/check_bundles.py`: OK(en 545、除外 67、ja 478)
- 美咲フォントを `tools/README.md` の手順で作り直した。`collect_chars.py`: 816 コードポイント(増: 両 四 増 川 暗 迫 途 減 隅、減: 旧マップ名だけで使っていた 塞 山 島 巻 没 渦 漠 焦 穴 迷 陥)。`bdf2fnt.py`: 802 グリフ、256×256。`check_fnt.py`: 重なり・はみ出しなし。BDF に無い 14 字は従来どおり常に入れるかな範囲の字(ゔ ゕ ゖ など)で、文言には使っていない。

## テストとビルド
- `DefaultMapsTest` を追加(maps.json が 3 件で id 0〜2、名前、targetScore、visible/oreGen/flipBase/custom。各 PNG が存在し 256×256。maps ディレクトリの PNG が登録済みの 3 枚だけ。en/ja の bundle に name と description がある)。
- `./gradlew :core:test --rerun-tasks`: 成功(100 件、失敗 0、エラー 0。前回 96 件 + 4 件)
- `./gradlew :html:dist`: 成功。`html/build/dist/assets/assets.txt` の maps は prototype、sprint、release の PNG と maps.json だけ。`html/build/dist` を残してある。

## 未確認(ブラウザ確認は別途)
- レベル選択に 3 枚だけが id 順に出ること、説明ラベルの折り返しと高さ(日本語は 1 行 10 字ほど)、英日の切り替え。
- 説明ラベルは既定の文字サイズ。カードの他のラベルが `UI.labelScale()` を使っていないので合わせた。小さい倍率にすると square は 6px 前後、美咲は整数倍でないと潰れるため。長すぎるようなら文言を削る。
- ゲーム開始後の見た目: 開始基地が壁に切られないこと、敵がコアまで来ること、ドリル/ポンプが資源の床と液体の床で動くこと、256×256 の経路計算時間(Task 5.2)。
- 資源量、出現点数、targetScore の釣り合い(Task 5.3。`tools/gen_maps.py` の `MAPS` を直して再生成する)。
