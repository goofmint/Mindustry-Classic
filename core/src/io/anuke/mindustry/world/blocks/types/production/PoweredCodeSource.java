package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.BlockBar;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.PowerAcceptor;
import io.anuke.mindustry.world.blocks.types.PowerBlock.PowerEntity;
import io.anuke.ucore.util.Strings;

/**
 * 電力(トークン)を使ってコードを生む設備(AI コーディングエージェント用)。
 * コードを 1 個出すたびに {@link #powerPerItem} を消費し、足りなければ出さない。
 * <p>
 * 設計書 §2.2 では CodeSource 1 クラスが PowerAcceptor を実装する案だったが、電力を使うのは AI だけなので
 * 電力を使わない {@link CodeSource} と、このサブクラスに分けた。1 クラスにすると人間のエンジニアも
 * PowerAcceptor になり、{@code Generator.laserTarget()} がそこでレーザーを止めてしまうため。
 * 電力は {@link PowerEntity} に持たせるので、保存と読み込みは PowerEntity の write/read で行われる。
 */
public class PoweredCodeSource extends CodeSource implements PowerAcceptor{
	/** コード 1 個あたりの消費電力 */
	protected float powerPerItem;
	protected float powerCapacity = 10f;

	public PoweredCodeSource(String name){
		super(name);

		bars.add(new BlockBar(Color.YELLOW, true, tile -> tile.<PowerEntity>entity().power / powerCapacity));
	}

	@Override
	public void getStats(Array<String> list){
		super.getStats(list);
		list.add("[powerinfo]Power Capacity: " + powerCapacity);
		list.add("[powerinfo]Power/item: " + Strings.toFixed(powerPerItem, 1));
	}

	@Override
	protected boolean canProduce(TileEntity entity){
		return ((PowerEntity)entity).power >= powerPerItem;
	}

	@Override
	protected void onProduced(TileEntity entity){
		((PowerEntity)entity).power -= powerPerItem;
	}

	@Override
	public boolean acceptsPower(Tile tile){
		PowerEntity entity = tile.entity();

		return entity.power + 0.001f <= powerCapacity;
	}

	@Override
	public float addPower(Tile tile, float amount){
		PowerEntity entity = tile.entity();

		float canAccept = Math.min(powerCapacity - entity.power, amount);

		entity.power += canAccept;

		return canAccept;
	}

	@Override
	public void setPower(Tile tile, float power){
		PowerEntity entity = tile.entity();
		entity.power = power;
	}

	@Override
	public TileEntity getEntity(){
		return new PowerEntity();
	}
}
