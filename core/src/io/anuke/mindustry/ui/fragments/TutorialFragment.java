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
import io.anuke.ucore.scene.ui.Label;
import io.anuke.ucore.scene.ui.layout.Cell;
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
	/** 本文の幅(dp)。狭い画面では画面幅の 48% を上限にする(4:3 の 853×640 で左上の HUD の目標と残高に重ならない幅)。
	 * ブラウザ版は起動後に canvas が縮むので、幅は毎フレーム見直す */
	private static final float PANEL_WIDTH_DP = 460f;
	private static final float PANEL_MAX_FRACTION = 0.48f;
	/** パネルの内側の余白(dp)。640 の高さで右下のブロックパネルに届かないよう詰めている */
	private static final float PANEL_MARGIN_INNER = 8f;
	/** 画面の右端からの余白(dp)。上部中央に置くと左上の HUD(目標と残高)に 1280 幅でも重なるので、右上に寄せる */
	private static final float PANEL_MARGIN_DP = 8f;
	/** 画面上端からの余白(dp)。HUD の「一時停止中」ラベル(上部中央)の下に出す */
	private static final float TOP_PAD_DP = 52f;
	/** 本文の最小の高さ(dp)。手順ごとに本文の行数が違っても、ボタンの位置が動かないようにする。
	 * 英語の本文は狭い幅でも 6 行に収まる長さにしてあり、640 の高さで右下のレシピ説明(選択中に出る)の上に収まる */
	private static final float BODY_MIN_HEIGHT_DP = 108f;
	private static final float BUTTON_WIDTH = 120f;
	private static final float BUTTON_HEIGHT = 40f;

	private Table buttons;
	/** ボタン列を作った時点の手順。手順が変わったら作り直す */
	private TutorialStep builtFor;
	/** 本文の幅を最後に反映した値 */
	private float lastWidth = -1f;

	@Override
	public void build(){
		TutorialController tutorial = control.tutorial();

		new table(){{
			atop();
			aright();
			touchable(Touchable.childrenOnly);
			visible(() -> !state.is(State.menu) && tutorial.active());

			Table pane = new Table("pane");
			pane.setTouchable(Touchable.childrenOnly);
			pane.margin(PANEL_MARGIN_INNER);

			Cell<Label> titleCell = pane.labelWrap(() -> title(tutorial)).width(panelWidth()).left();
			titleCell.get().setTouchable(Touchable.disabled);
			pane.row();
			Cell<Label> bodyCell = pane.labelWrap(() -> body(tutorial)).width(panelWidth())
					.minHeight(Unit.dp.scl(BODY_MIN_HEIGHT_DP)).top().left().padTop(2f);
			bodyCell.get().setTouchable(Touchable.disabled);
			pane.row();

			buttons = new Table();
			buttons.setTouchable(Touchable.childrenOnly);
			pane.add(buttons).right().padTop(4f);

			pane.update(() -> {
				float width = panelWidth();
				if(width != lastWidth){
					lastWidth = width;
					titleCell.width(width);
					bodyCell.width(width);
					pane.invalidateHierarchy();
				}
				TutorialStep step = tutorial.step();
				if(step != builtFor){
					builtFor = step;
					rebuildButtons(tutorial, step);
				}
			});

			add(pane).padTop(Unit.dp.scl(TOP_PAD_DP)).padRight(Unit.dp.scl(PANEL_MARGIN_DP));
		}}.end();
	}

	/** 本文の幅。固定値と、画面幅の一定割合の小さいほう */
	private static float panelWidth(){
		return Math.min(Unit.dp.scl(PANEL_WIDTH_DP), Gdx.graphics.getWidth() * PANEL_MAX_FRACTION);
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
				//手順 1〜5 の途中で自分で解除していることがある。動いているときに「解除する」と案内すると止めてしまう
				return state.is(State.paused)
						? Bundles.format("tutorial.unpause", keyName("pause"))
						: Bundles.get("tutorial.unpause.running");
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
