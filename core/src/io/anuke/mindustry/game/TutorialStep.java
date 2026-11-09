// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.game;

/** チュートリアルの手順。宣言順に進む(Issue #78)。unguidable は初期配置が想定と違うときだけ使い、番号を持たない */
public enum TutorialStep{
	/** 目的: 残高と目標、品質×重要度、初期状態では残高が減ること */
	goal,
	/** 生産ライン: エンジニア → ユニットテスト → 本番環境(コア) */
	pipeline,
	/** 合流コンベアを撤去する */
	breakMerge,
	/** CodeRabbit を選択する */
	selectCodeRabbit,
	/** CodeRabbit を合流タイルに配置する */
	placeCodeRabbit,
	/** 一時停止を解除して、最初のプラスの納品を見る */
	unpause,
	/** 完了: 勝敗条件、ウェーブ、ブロック情報の見方 */
	done,
	/** 案内不能: 初期配置が想定と違う */
	unguidable;

	/** 番号つきの手順の数(goal〜done) */
	public static final int NUMBERED = done.ordinal() + 1;

	/** 表示用の 1 始まりの番号。unguidable は 0 */
	public int number(){
		return this == unguidable ? 0 : ordinal() + 1;
	}
}
