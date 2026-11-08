package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.resource.CodeItem;
import io.anuke.mindustry.resource.Item;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Strings;

/**
 * コードの品質を上げる施設の変換部分。{@link QualityGate} と、Task 3.8 の CodeRabbitBlock(PowerTurret の
 * サブクラス)の両方から呼ぶ。状態は {@link GateState} を実装したエンティティが持ち、このクラスは状態を持たない。
 * 出力の受け渡し(offloadNear や tryDump)はしない。呼び出し側のブロックが行う。
 */
public final class GateLogic{

	private GateLogic(){
	}

	/** 施設のエンティティが実装する状態。処理中のコード(空なら null)、進捗(0〜1)、電力 */
	public interface GateState{
		Item getCurrent();

		void setCurrent(Item item);

		float getProgress();

		void setProgress(float progress);

		float getPower();

		void setPower(float power);
	}

	/**
	 * 処理を進める。処理中のコードがあり、電力が足りている(または電力不要の)ときだけ進捗を足す。電力が足りなければ進捗は止まる。
	 * 進捗が 1 に届いたら powerPerItem を引き、{@link CodeItem#advance} の結果を返して、処理中のコードと進捗を空にする。
	 *
	 * @param processTime 1 個の処理時間(秒)
	 * @param powerPerItem 1 個あたりの消費電力。0 なら電力を使わない
	 * @param delta このフレームの経過時間(1/60 秒単位。ブロックは Timers.delta() を渡す)
	 * @return 処理が終わったコード。終わっていなければ null
	 */
	public static Item update(GateState state, int passedBit, int stages, float processTime, float powerPerItem, float delta){
		Item current = state.getCurrent();
		if(current == null) return null;

		if(powerPerItem > 0f && state.getPower() < powerPerItem) return null;

		float progress = state.getProgress() + delta / (60f * processTime);
		state.setProgress(progress);

		if(progress < 1f) return null;

		state.setPower(state.getPower() - powerPerItem);
		state.setCurrent(null);
		state.setProgress(0f);
		return CodeItem.advance(current, passedBit, stages);
	}

	/**
	 * ブロック情報ダイアログの統計行(カテゴリ、品質の上昇、処理秒数)を足す。{@link QualityGate} と CodeRabbitBlock が使う。
	 * 見出しは英語で書き、BlocksFragment.showBlockInfo() が text.blocks.* で訳す。カテゴリ名は値なので、ここで訳す。
	 */
	public static void addStats(Array<String> list, int passedBit, int stages, float processTime){
		list.add("[craftinfo]Category: " + Bundles.get("text.code.category." + CodeItem.bitKey(passedBit)));
		list.add("[craftinfo]Quality Gain: " + stages);
		list.add("[craftinfo]Seconds/item: " + Strings.toFixed(processTime, 1));
	}

	/**
	 * コードを受け入れられるか。コード以外(燃料、credit など)は受けない。処理中のコードがあるときと、
	 * 出し切れていない出力が残っているときも受けない(施設の中にあるコードは常に 1 個まで)。
	 */
	public static boolean accepts(GateState state, Item item, boolean hasPendingOutput){
		return CodeItem.attrOf(item) != null && state.getCurrent() == null && !hasPendingOutput;
	}
}
