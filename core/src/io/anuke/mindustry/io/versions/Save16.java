// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.io.versions;

/**
 * Save15 と同じ書式で、番号だけ 16 にしたセーブ形式。
 * Task 3.11 でブロックとアイテムの ID を詰め直し、Task 4.1 でマップを prototype / sprint / release(ID 0〜2)に
 * 入れ替えたため、本家の Save15 ファイルは別のマップとして開かれるか、ID のずれたブロックを復元してしまう。
 * 番号を変えることで、旧ファイルは isSaveValid() で弾かれてセーブ一覧に出ない。移行処理は持たない。
 */
public class Save16 extends Save15 {

    public Save16(){
        super(16);
    }
}
