package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.CodeItem;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Mathf;

/**
 * コードを生む設備。コード鉱脈(床 {@link Blocks#iron})の上でだけ、{@code time} 秒ごとにコードを 1 個出す。
 * 出すコードの品質段階は {@link #stageChoices} から、重要度は 1 か 2 から、毎回等確率で決める。
 * <p>
 * このクラスは電力を使わない(人間のエンジニア用)。{@link io.anuke.mindustry.world.blocks.types.PowerAcceptor}
 * を実装しないので、発電機やパワーレーザーのレーザーを受け止めない。
 * {@code Generator.laserTarget()} は最初に当たった PowerAcceptor でレーザーを止めるため、
 * 電力を使わないブロックが PowerAcceptor だと、その先へ電力が届かなくなる。
 * 電力(トークン)を使う AI エージェントは {@link PoweredCodeSource} を使う。
 */
public class CodeSource extends Drill{
	/** 出発点の品質段階の候補({@link CodeItem#QUALITY} の添字) */
	protected int[] stageChoices;

	public CodeSource(String name){
		super(name);
		resource = null;
		result = null;
	}

	/** コード鉱脈(床が {@link Blocks#iron})の上か */
	protected boolean isVein(Tile tile){
		return tile.floor() == Blocks.iron;
	}

	/** 鉱脈と容量以外の、生産の追加条件。電力を使うサブクラスが上書きする */
	protected boolean canProduce(TileEntity entity){
		return true;
	}

	/** コードを 1 個出したときに呼ばれる。電力を使うサブクラスが上書きする */
	protected void onProduced(TileEntity entity){
	}

	@Override
	public void getStats(Array<String> list){
		super.getStats(list);

		StringBuilder quality = new StringBuilder();
		for(int stage : stageChoices){
			if(quality.length() > 0) quality.append(", ");
			float value = CodeItem.QUALITY[stage];
			quality.append(value == (int)value ? Integer.toString((int)value) : Float.toString(value));
		}
		list.add("[iteminfo]Start Quality: " + quality);

		StringBuilder importance = new StringBuilder();
		for(int value : CodeItem.IMPORTANCE){
			if(importance.length() > 0) importance.append(", ");
			importance.append(value);
		}
		list.add("[iteminfo]Importance: " + importance);

		//コロンの無い行は showBlockInfo() が訳さないので、訳した文をそのまま入れる(Generator の爆発警告と同じ)
		list.add("[iteminfo]" + Bundles.get("text.blocks.codevein"));
	}

	@Override
	public void update(Tile tile){
		TileEntity entity = tile.entity;

		if(isVein(tile) && entity.totalItems() < capacity && canProduce(entity)
				&& entity.timer.get(timerDrill, 60 * time)){
			int stage = stageChoices[Mathf.random(stageChoices.length - 1)];
			int importance = CodeItem.IMPORTANCE[Mathf.random(CodeItem.IMPORTANCE.length - 1)];
			onProduced(entity);
			Item produced = CodeItem.get(stage, 0, importance);
			offloadNear(tile, produced);
			Effects.effect(drillEffect, tile.worldx(), tile.worldy());
		}

		if(entity.timer.get(timerDump, 30)){
			tryDump(tile);
		}
	}

	@Override
	public boolean canReplace(Block other){
		return other instanceof CodeSource;
	}

	@Override
	public boolean isLayer(Tile tile){
		return !isVein(tile);
	}
}
