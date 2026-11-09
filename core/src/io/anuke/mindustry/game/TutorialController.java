// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.game;

import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.game.EventType.BreakEvent;
import io.anuke.mindustry.game.EventType.DeliverEvent;
import io.anuke.mindustry.game.EventType.GameOverEvent;
import io.anuke.mindustry.game.EventType.PlaceEvent;
import io.anuke.mindustry.game.EventType.ResetEvent;
import io.anuke.mindustry.game.EventType.StateChangeEvent;
import io.anuke.mindustry.game.TutorialModel.Ids;
import io.anuke.mindustry.game.TutorialModel.Layout;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.resource.Recipe;
import io.anuke.mindustry.resource.Recipes;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.DistributionBlocks;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.mindustry.world.blocks.WeaponBlocks;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.core.Settings;

import static io.anuke.mindustry.Vars.*;

/**
 * チュートリアルのセッション管理(Issue #78)。Control が 1 回だけ生成し、イベントのリスナーもそのとき 1 回だけ登録する
 * (Events にリスナーを外す API が無い)。リスナーはセッションが有効なときだけモデルに通知する。
 * 合流タイルなどの位置は毎回 world から計算し、Tile や画面座標は保持しない。
 */
public class TutorialController{
	/** 完了またはスキップしたら true(Settings) */
	public static final String DONE_SETTING = "tutorialdone";
	/** 初回の New Game でチュートリアルを案内したら true(Settings) */
	public static final String PROMPTED_SETTING = "tutorialprompted";
	/** チュートリアルを行うマップ */
	public static final String MAP_NAME = "prototype";

	private final TutorialModel model;
	private final int codeRabbitCost;

	public TutorialController(){
		Recipe recipe = Recipes.getByResult(WeaponBlocks.coderabbit);
		if(recipe == null) throw new IllegalStateException("CodeRabbit has no recipe");
		int cost = -1;
		for(ItemStack stack : recipe.requirements){
			if(stack.item == Item.credit) cost = stack.amount;
		}
		if(cost < 0) throw new IllegalStateException("CodeRabbit recipe has no credit cost");
		codeRabbitCost = cost;

		model = new TutorialModel(new Ids(DistributionBlocks.conveyor.id, ProductionBlocks.unitTest.id,
				WeaponBlocks.coderabbit.id, Blocks.air.id, cost));

		//begin() より前の ResetEvent は、前のセッションを終えるだけ(開始処理の logic.reset() がこれにあたる)
		Events.on(ResetEvent.class, model::invalidate);
		Events.on(GameOverEvent.class, model::invalidate);
		Events.on(StateChangeEvent.class, (from, to) -> {
			if(to == State.menu){
				model.invalidate();
			}else if(to == State.playing){
				model.playing();
			}
		});
		Events.on(BreakEvent.class, (x, y, block) -> {
			Tile core = world.getCore();
			if(!model.active() || core == null) return;
			model.broke(x - core.x, y - core.y, block);
		});
		Events.on(PlaceEvent.class, (x, y, block, rotation) -> {
			Tile core = world.getCore();
			if(!model.active() || core == null) return;
			model.placed(x - core.x, y - core.y, block);
		});
		Events.on(DeliverEvent.class, (item, score) -> {
			if(!model.active()) return;
			model.delivered(score);
			if(model.step() == TutorialStep.done) saveDone();
		});
	}

	/**
	 * セッションを始める。logic.play() の直後、StateChangeEvent のハンドラの外で呼ぶ
	 * (GameState.set() は状態を代入する前にイベントを発火するので、ハンドラ内で状態を変えてはいけない)。
	 * 初期配置を検証してから、ゲームを一時停止してその所有を記録する。
	 */
	public void begin(){
		Tile core = world.getCore();
		if(core == null) throw new IllegalStateException("Tutorial started without a core");
		boolean flip = world.getMap().flipBase;
		int mergeY = core.y + TutorialModel.mergeOffsetY(flip);
		model.begin(flip, new Layout(
				blockAt(core.x, mergeY),
				blockAt(core.x, core.y + TutorialModel.gateOffsetY(flip)),
				blockAt(core.x - TutorialModel.SIDE_DISTANCE, mergeY),
				blockAt(core.x + TutorialModel.SIDE_DISTANCE, mergeY)));
		state.set(State.paused);
	}

	/** 毎フレーム(menu 以外)。ネットワークが始まったらセッションを終え、選択中のレシピをモデルに渡す */
	public void update(){
		if(!model.active()) return;
		if(Net.active()){
			model.invalidate();
			return;
		}
		Recipe recipe = control.input().recipe;
		model.selected(recipe == null ? TutorialModel.NO_BLOCK : recipe.result.id);
	}

	public boolean active(){
		return model.active();
	}

	/** 現在の手順。セッションが無ければ null */
	public TutorialStep step(){
		return model.step();
	}

	/** 手順 4 と 5 で、残高が CodeRabbit の価格に足りない */
	public boolean lowBalance(){
		return model.lowBalance(state.score.balance());
	}

	public int codeRabbitCost(){
		return codeRabbitCost;
	}

	/** 「次へ」 */
	public void next(){
		Tile merge = mergeTile();
		model.next(merge == null ? TutorialModel.NO_BLOCK : merge.block().id);
	}

	/** 「スキップ」: 完了として記録し、セッションを終える */
	public void skip(){
		saveDone();
		end();
	}

	/** 「閉じる」(完了または案内不能) */
	public void close(){
		end();
	}

	/** 合流コンベアのタイル。コアが無ければ null */
	public Tile mergeTile(){
		Tile core = world.getCore();
		return core == null ? null : world.tile(core.x, core.y + TutorialModel.mergeOffsetY(world.getMap().flipBase));
	}

	/** 合流点の手前のユニットテストのタイル */
	public Tile gateTile(){
		Tile core = world.getCore();
		return core == null ? null : world.tile(core.x, core.y + TutorialModel.gateOffsetY(world.getMap().flipBase));
	}

	/** 中央のエンジニアのタイル。side は -1(左)か 1(右) */
	public Tile engineerTile(int side){
		Tile core = world.getCore();
		return core == null ? null : world.tile(core.x + side * TutorialModel.ENGINEER_DX,
				core.y + TutorialModel.engineerOffsetY(world.getMap().flipBase));
	}

	/** セッションを終える。自分が設定した一時停止がまだ続いていれば再開する */
	private void end(){
		boolean resume = model.shouldResume(state.is(State.paused));
		model.invalidate();
		if(resume) state.set(State.playing);
	}

	private void saveDone(){
		Settings.putBool(DONE_SETTING, true);
		Settings.save();
	}

	private static int blockAt(int x, int y){
		Tile tile = world.tile(x, y);
		return tile == null ? TutorialModel.NO_BLOCK : tile.block().id;
	}
}
