// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.world.BlockBar;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.PowerAcceptor;
import io.anuke.ucore.util.Strings;

/**
 * 電力(トークン)を使ってコードの品質を上げる施設(テスト、CI)。コード 1 個の処理に {@link #powerPerItem} を使い、
 * 足りない間は処理が止まる。電力を使わない {@link QualityGate} と分けたのは、電力を使わないシニアエンジニアが
 * PowerAcceptor になって発電機のレーザーを止めないようにするため({@link PoweredCodeSource} と同じ理由)。
 */
public class PoweredQualityGate extends QualityGate implements PowerAcceptor{
	/** コード 1 個あたりの消費電力 */
	protected float powerPerItem;
	protected float powerCapacity = 10f;

	public PoweredQualityGate(String name){
		super(name);

		bars.add(new BlockBar(Color.YELLOW, true, tile -> tile.<GateEntity>entity().power / powerCapacity));
	}

	@Override
	protected float powerPerItem(){
		return powerPerItem;
	}

	@Override
	public void getStats(Array<String> list){
		super.getStats(list);
		list.add("[powerinfo]Power Capacity: " + powerCapacity);
		list.add("[powerinfo]Power/item: " + Strings.toFixed(powerPerItem, 1));
	}

	@Override
	public boolean acceptsPower(Tile tile){
		GateEntity entity = tile.entity();

		return entity.power + 0.001f <= powerCapacity;
	}

	@Override
	public float addPower(Tile tile, float amount){
		GateEntity entity = tile.entity();

		float canAccept = Math.min(powerCapacity - entity.power, amount);

		entity.power += canAccept;

		return canAccept;
	}

	@Override
	public void setPower(Tile tile, float power){
		GateEntity entity = tile.entity();
		entity.power = power;
	}
}
