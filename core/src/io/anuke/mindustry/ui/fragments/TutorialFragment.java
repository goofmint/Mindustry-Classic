// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.Gdx;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.game.Balance;
import io.anuke.mindustry.game.TutorialController;
import io.anuke.mindustry.game.TutorialStep;
import io.anuke.ucore.core.KeyBinds;
import io.anuke.ucore.scene.builders.table;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.KeybindDialog;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Bundles;

import static io.anuke.mindustry.Vars.*;

/**
 * チュートリアルの案内パネル(Issue #78)。画面右上に出す非モーダルのパネルで、本文の背景は touch 不可、
 * 「次へ」「スキップ」「閉じる」のボタンだけ touch 可能。パネルの下のタイルにもブロックを置ける。
 * 表示は毎フレーム TutorialController の状態から組み立て、セッションが無ければ隠す。
 */
public class TutorialFragment implements Fragment{
	/** パネルの幅(dp)。画面幅から左右の余白を引いた値を上限にする */
	private static final float PANEL_WIDTH_DP = 460f;
	/** 画面の右端からの余白(dp)。上部中央に置くと左上の HUD(目標と残高)に 1280 幅でも重なるので、右上に寄せる */
	private static final float PANEL_MARGIN_DP = 8f;
	/** 画面上端からの余白(dp)。HUD の「一時停止中」ラベル(上部中央)の下に出す */
	private static final float TOP_PAD_DP = 52f;
	/** 本文の最小の高さ(dp)。手順ごとに本文の行数が違っても、ボタンの位置が動かないようにする(英語の手順 1 の 6 行より 1 行多い) */
	private static final float BODY_MIN_HEIGHT_DP = 126f;
	private static final float BUTTON_WIDTH = 120f;
	private static final float BUTTON_HEIGHT = 44f;

	private Table buttons;
	/** ボタン列を作った時点の手順。手順が変わったら作り直す */
	private TutorialStep builtFor;

	@Override
	public void build(){
		TutorialController tutorial = control.tutorial();

		new table(){{
			atop();
			aright();
			touchable(Touchable.childrenOnly);
			visible(() -> !state.is(State.menu) && tutorial.active());

			float width = Math.min(Unit.dp.scl(PANEL_WIDTH_DP), Gdx.graphics.getWidth() - Unit.dp.scl(PANEL_MARGIN_DP) * 2f);

			Table pane = new Table("pane");
			pane.setTouchable(Touchable.childrenOnly);
			pane.margin(10f);

			pane.labelWrap(() -> title(tutorial)).width(width).left().get().setTouchable(Touchable.disabled);
			pane.row();
			pane.labelWrap(() -> body(tutorial)).width(width).minHeight(Unit.dp.scl(BODY_MIN_HEIGHT_DP)).top().left().padTop(4f)
					.get().setTouchable(Touchable.disabled);
			pane.row();

			buttons = new Table();
			buttons.setTouchable(Touchable.childrenOnly);
			pane.add(buttons).right().padTop(6f);

			pane.update(() -> {
				TutorialStep step = tutorial.step();
				if(step != builtFor){
					builtFor = step;
					rebuildButtons(tutorial, step);
				}
			});

			add(pane).padTop(Unit.dp.scl(TOP_PAD_DP)).padRight(Unit.dp.scl(PANEL_MARGIN_DP));
		}}.end();
	}

	private void rebuildButtons(TutorialController tutorial, TutorialStep step){
		buttons.clearChildren();
		if(step == null) return;
		buttons.defaults().size(BUTTON_WIDTH, BUTTON_HEIGHT).pad(2f);

		if(step == TutorialStep.goal || step == TutorialStep.pipeline){
			buttons.addButton("$text.tutorial.next", tutorial::next);
		}

		if(step == TutorialStep.done || step == TutorialStep.unguidable){
			buttons.addButton("$text.tutorial.close", tutorial::close);
		}else{
			buttons.addButton("$text.tutorial.skip", tutorial::skip);
		}
	}

	private static String title(TutorialController tutorial){
		TutorialStep step = tutorial.step();
		if(step == null) return "";
		if(step == TutorialStep.unguidable) return Bundles.get("text.tutorial");
		return Bundles.format("text.tutorial.title", String.valueOf(step.number()), String.valueOf(TutorialStep.NUMBERED));
	}

	private static String body(TutorialController tutorial){
		TutorialStep step = tutorial.step();
		if(step == null) return "";
		switch(step){
			case goal:
				return Bundles.format("tutorial.goal", String.valueOf(state.score.target), String.valueOf(Balance.INITIAL_CREDIT));
			case pipeline:
				return Bundles.get("tutorial.pipeline");
			case breakMerge:
				return Bundles.format("tutorial.break", keyName("break")) + "\n" + Bundles.format("tutorial.break.hint", keyName("break"));
			case selectCodeRabbit:
				return Bundles.format("tutorial.select", String.valueOf(tutorial.codeRabbitCost())) + lowBalance(tutorial);
			case placeCodeRabbit:
				return Bundles.format("tutorial.place", keyName("select")) + lowBalance(tutorial);
			case unpause:
				return Bundles.format("tutorial.unpause", keyName("pause"));
			case done:
				return Bundles.format("tutorial.done", keyName("block_info"), keyName("select"));
			case unguidable:
				return Bundles.get("tutorial.unguidable");
			default:
				throw new IllegalStateException("Unknown tutorial step: " + step);
		}
	}

	/** 手順 4 と 5 で残高が CodeRabbit の価格に足りないときの警告。足りていれば空 */
	private static String lowBalance(TutorialController tutorial){
		if(!tutorial.lowBalance()) return "";
		return "\n" + Bundles.format("tutorial.lowbalance", String.valueOf(tutorial.codeRabbitCost()));
	}

	/** 現在のキーバインドの表示名(KeybindDialog と同じ名前) */
	private static String keyName(String bind){
		return KeybindDialog.inputName(KeyBinds.get(bind));
	}
}
