package io.anuke.mindustry.game;

import io.anuke.mindustry.resource.CodeItem;
import io.anuke.mindustry.resource.CodeItem.CodeAttr;
import io.anuke.mindustry.resource.Item;

/**
 * 残高、目標、破産、敵の倍率をまとめる。残高は Inventory の Item.credit の個数そのもので、
 * このクラスはその読み書きをする薄いラッパー。
 * 端数(fraction)と品質の履歴は保存しない。ロード直後の倍率は 1.0 から始まる。
 */
public class Score{
	/** 目標得点。0 は未設定(reachedTarget() は常に false) */
	public int target;
	/** 敵の出現数に掛ける倍率。1.0 が基準 */
	public float qualityMultiplier = 1f;

	private final Inventory inventory;
	/** 残高にまだ反映していない得点の端数(符号つき、絶対値は 1 未満) */
	private float fraction;
	/** 直近のコード納品の品質のリングバッファ */
	private final float[] recentQualities = new float[Balance.QUALITY_HISTORY];
	private int recentHead;
	private int recentCount;
	private float recentQualitySum;
	/** 納品で残高が 0 を下回ったら立つ。reset() まで下ろさない */
	private boolean bankrupt;

	public Score(Inventory inventory){
		this.inventory = inventory;
	}

	/** 残高 */
	public int balance(){
		return inventory.getAmount(Item.credit);
	}

	/**
	 * 納品した得点を残高に足す(負もある)。端数は fraction に溜め、整数になった分だけ反映する。
	 * 残高が 0 を下回ったら 0 にして破産を立てる。コードの納品なら品質の履歴と倍率を更新する。
	 */
	public void deliver(Item item, float score){
		fraction += score;
		int whole = (int)(fraction + Math.copySign(Balance.FRACTION_EPSILON, fraction));
		fraction -= whole;

		if(whole != 0){
			int[] items = inventory.getItems();
			int result = items[Item.credit.id] + whole;
			if(result < 0){
				result = 0;
				bankrupt = true;
			}
			items[Item.credit.id] = result;
		}

		CodeAttr attr = CodeItem.attrOf(item);
		if(attr != null){
			addQuality(CodeItem.QUALITY[attr.stage]);
			qualityMultiplier = multiplierFor(recentQualitySum / recentCount);
		}
	}

	/** 目標が設定されていて、残高が目標以上 */
	public boolean reachedTarget(){
		return target > 0 && balance() >= target;
	}

	/** 納品で残高が 0 を下回ったことがある(建設の消費では立たない) */
	public boolean bankrupt(){
		return bankrupt;
	}

	/** 残高を initial にし、端数、履歴、破産、倍率を初期状態に戻す。target は変えない */
	public void reset(int initial){
		inventory.getItems()[Item.credit.id] = initial;
		fraction = 0f;
		recentHead = 0;
		recentCount = 0;
		recentQualitySum = 0f;
		bankrupt = false;
		qualityMultiplier = 1f;
	}

	/** 平均品質 q から敵の倍率を求める */
	static float multiplierFor(float q){
		float m = 1f - q * Balance.MULTIPLIER_SLOPE;
		return Math.max(Balance.MULTIPLIER_MIN, Math.min(Balance.MULTIPLIER_MAX, m));
	}

	private void addQuality(float quality){
		recentQualities[recentHead] = quality;
		recentHead = (recentHead + 1) % recentQualities.length;
		if(recentCount < recentQualities.length) recentCount++;

		//足し引きの誤差が溜まらないよう、毎回合計を取り直す(最大 20 個)
		float sum = 0f;
		for(int i = 0; i < recentCount; i++) sum += recentQualities[i];
		recentQualitySum = sum;
	}
}
