package io.anuke.mindustry.world.blocks.types.defense;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.CodeItem;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.BlockBar;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.production.GateLogic;
import io.anuke.mindustry.world.blocks.types.production.QualityGate;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Strings;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * コードレビュー(REVIEW ビット、品質 2 段階上)と迎撃を 1 ブロックで行う砲塔。迎撃は {@link PowerTurret} のまま、
 * 変換は {@link QualityGate} と同じく {@link GateLogic} が行う。処理中のコードは {@link CodeRabbitEntity#current} に持ち、
 * 隣が受け取れなかった出力は entity.items に入れて {@link #tryDump} で吐き出す。出し切るまで次のコードは受けない。
 * <p>
 * 電力(トークン)は迎撃と変換で同じ entity.power を使う。迎撃は 1 発ごとに {@link #powerUsed}、変換は 1 個ごとに
 * {@link #powerPerItem} を引き、それぞれ自分の量が残っているかだけを見る。そのため powerUsed 以上 powerPerItem 未満の
 * 電力しか無いときは、迎撃は続き、変換は電力が貯まるまで待つ。毎フレーム迎撃を先に処理するので、撃つほうが優先される。
 */
public class CodeRabbitBlock extends PowerTurret{
	protected final int timerDump = timers++;

	/** 通過ビット */
	protected int passedBit = CodeItem.REVIEW;
	/** 上がる品質の段階数 */
	protected int stages = 2;
	/** 1 個の処理時間(秒) */
	protected float processTime = 3f;
	/** コード 1 個あたりの消費電力 */
	protected float powerPerItem = 0.6f;

	public CodeRabbitBlock(String name){
		super(name);
	}

	@Override
	public void init(){
		bars.add(new BlockBar(Color.GREEN, true, tile -> {
			CodeRabbitEntity entity = tile.entity();
			return entity.current == null ? -1f : entity.progress;
		}));
	}

	@Override
	public void getStats(Array<String> list){
		super.getStats(list);
		GateLogic.addStats(list, passedBit, stages, processTime);
		list.add("[powerinfo]Power/item: " + Strings.toFixed(powerPerItem, 1));
	}

	@Override
	public void update(Tile tile){
		//迎撃を先に処理する(電力が少ないときは撃つほうが優先)
		super.update(tile);

		CodeRabbitEntity entity = tile.entity();

		Item result = GateLogic.update(entity, passedBit, stages, processTime, powerPerItem, Timers.delta());
		if(result != null){
			offloadNear(tile, result);
		}

		if(entity.timer.get(timerDump, 5) && entity.totalItems() > 0){
			tryDump(tile);
		}
	}

	@Override
	public boolean acceptItem(Item item, Tile tile, Tile source){
		CodeRabbitEntity entity = tile.entity();
		return GateLogic.accepts(entity, item, entity.totalItems() > 0);
	}

	@Override
	public void handleItem(Item item, Tile tile, Tile source){
		if(source == tile){
			//offloadNear で隣が受け取れなかった出力。entity.items に入れ、tryDump で出す
			super.handleItem(item, tile, source);
			return;
		}

		CodeRabbitEntity entity = tile.entity();
		entity.current = item;
		entity.progress = 0f;
	}

	@Override
	public TileEntity getEntity(){
		return new CodeRabbitEntity();
	}

	public static class CodeRabbitEntity extends PowerTurretEntity implements GateLogic.GateState{
		/** 処理中のコード。空なら null */
		public Item current;
		/** 処理の進捗(0〜1) */
		public float progress;

		@Override
		public Item getCurrent(){
			return current;
		}

		@Override
		public void setCurrent(Item item){
			current = item;
		}

		@Override
		public float getProgress(){
			return progress;
		}

		@Override
		public void setProgress(float progress){
			this.progress = progress;
		}

		@Override
		public float getPower(){
			return power;
		}

		@Override
		public void setPower(float power){
			this.power = power;
		}

		@Override
		public void write(DataOutputStream stream) throws IOException{
			super.write(stream);
			stream.writeByte(current == null ? -1 : current.id);
			stream.writeFloat(progress);
		}

		@Override
		public void read(DataInputStream stream) throws IOException{
			super.read(stream);
			byte id = stream.readByte();
			current = (id < 0 || id >= Item.getAllItems().size) ? null : Item.getByID(id);
			progress = stream.readFloat();
		}
	}
}
