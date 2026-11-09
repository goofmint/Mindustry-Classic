// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.game;

/**
 * ゲームバランスの調整用定数。インスタンスは作らない。
 * 後続のタスクで値を調整するときはここだけを書き換える。
 */
public final class Balance{
	/** 新しいゲームを始めたときの残高(credit)の初期値 */
	public static final int INITIAL_CREDIT = 150;

	/** 敵の倍率の計算に使う、直近のコード納品の個数 */
	public static final int QUALITY_HISTORY = 20;

	/** 敵の倍率の傾き。multiplier = 1 - 平均品質 × この値 */
	public static final float MULTIPLIER_SLOPE = 0.3f;

	/** 敵の倍率の下限(平均品質 1 のとき) */
	public static final float MULTIPLIER_MIN = 0.7f;

	/** 敵の倍率の上限(平均品質 -5 のとき) */
	public static final float MULTIPLIER_MAX = 2.5f;

	/** 得点の端数を整数に切り出すときの許容誤差。float の足し算の誤差で 0.8 × 5 が 3.9999 になって 1 点損をするのを防ぐ */
	public static final float FRACTION_EPSILON = 1e-4f;

	private Balance(){
	}
}
