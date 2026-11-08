package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.BlockBar;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.PowerBlock.PowerEntity;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Strings;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * コードを 1 個受け取り、{@link #processTime} 秒のあと品質を上げて隣に出す施設(シニアエンジニア、テスト、CI)。
 * 変換は {@link GateLogic} が行う。処理中のコードは {@link GateEntity#current} に持ち、entity.items は使わない。
 * ただし隣が受け取れなかった出力は、{@link #offloadNear} の仕様どおり entity.items に入るので、{@link #tryDump} で吐き出す。
 * 出し切るまで次のコードは受けない。
 * <p>
 * このクラスは電力を使わない(シニアエンジニア用)。{@link io.anuke.mindustry.world.blocks.types.PowerAcceptor}
 * を実装しないので、発電機やパワーレーザーのレーザーを受け止めない。{@code Generator.laserTarget()} は
 * 最初に当たった PowerAcceptor でレーザーを止めるため、電力を使わないブロックが PowerAcceptor だと、
 * その先へ電力が届かなくなる。電力(トークン)を使う施設は {@link PoweredQualityGate} を使う。
 */
public class QualityGate extends Block{
	protected final int timerDump = timers++;

	/** 通過ビット({@code CodeItem.REVIEW}、{@code TEST}、{@code CI} のどれか 1 つ) */
	protected int passedBit;
	/** 上がる品質の段階数 */
	protected int stages = 1;
	/** 1 個の処理時間(秒) */
	protected float processTime;

	public QualityGate(String name){
		super(name);
		update = true;
		solid = true;
	}

	/** 1 個あたりの消費電力。このクラスは電力を使わないので 0。{@link PoweredQualityGate} が上書きする */
	protected float powerPerItem(){
		return 0f;
	}

	@Override
	public void init(){
		bars.add(new BlockBar(Color.GREEN, true, tile -> {
			GateEntity entity = tile.entity();
			return entity.current == null ? -1f : entity.progress;
		}));
	}

	@Override
	public void getStats(Array<String> list){
		super.getStats(list);
		list.add("[craftinfo]Seconds/item: " + Strings.toFixed(processTime, 1));
		list.add("[craftinfo]Quality Gain: " + stages);
	}

	@Override
	public void update(Tile tile){
		GateEntity entity = tile.entity();

		Item result = GateLogic.update(entity, passedBit, stages, processTime, powerPerItem(), Timers.delta());
		if(result != null){
			offloadNear(tile, result);
		}

		if(entity.timer.get(timerDump, 5) && entity.totalItems() > 0){
			tryDump(tile);
		}
	}

	@Override
	public boolean acceptItem(Item item, Tile tile, Tile source){
		GateEntity entity = tile.entity();
		return GateLogic.accepts(entity, item, entity.totalItems() > 0);
	}

	@Override
	public void handleItem(Item item, Tile tile, Tile source){
		if(source == tile){
			//offloadNear で隣が受け取れなかった出力。entity.items に入れ、tryDump で出す
			super.handleItem(item, tile, source);
			return;
		}

		GateEntity entity = tile.entity();
		entity.current = item;
		entity.progress = 0f;
	}

	@Override
	public void draw(Tile tile){
		super.draw(tile);

		GateEntity entity = tile.entity();
		if(entity.current != null){
			Draw.rect(entity.current.region, tile.worldx(), tile.worldy(), 4f, 4f);
		}
	}

	@Override
	public TileEntity getEntity(){
		return new GateEntity();
	}

	/** 電力は {@link PoweredQualityGate} だけが使う。このクラスでは power は 0 のまま(保存形式は共通) */
	public static class GateEntity extends PowerEntity implements GateLogic.GateState{
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
