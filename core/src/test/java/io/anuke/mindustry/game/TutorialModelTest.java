// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.game;

import io.anuke.mindustry.game.TutorialModel.Ids;
import io.anuke.mindustry.game.TutorialModel.Layout;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** TutorialModel の単体テスト。ブロックは任意の id、座標はコアからの相対タイルで、libGDX に依存しない */
public class TutorialModelTest{
	private static final int CONVEYOR = 1, UNIT_TEST = 2, CODE_RABBIT = 3, AIR = 0, OTHER = 9;
	private static final int COST = 100;
	/** flipBase が false の初期配置: 合流コンベアは (0, -4) */
	private static final int MERGE_DY = TutorialModel.mergeOffsetY(false);

	private TutorialModel model;

	@Before
	public void setUp(){
		model = new TutorialModel(new Ids(CONVEYOR, UNIT_TEST, CODE_RABBIT, AIR, COST));
	}

	private static Layout defaultBase(){
		return new Layout(CONVEYOR, UNIT_TEST, CONVEYOR, CONVEYOR);
	}

	/** 手順 3(合流コンベアの撤去)まで進める */
	private void beginAtBreak(){
		model.begin(false, defaultBase());
		model.next(CONVEYOR);
		model.next(CONVEYOR);
		assertEquals(TutorialStep.breakMerge, model.step());
	}

	@Test
	public void inactiveUntilBegin(){
		assertFalse(model.active());
		assertNull(model.step());
		assertFalse(model.ownsPause());
	}

	@Test
	public void nextWalksTheExplanationSteps(){
		model.begin(false, defaultBase());
		assertEquals(TutorialStep.goal, model.step());
		model.next(CONVEYOR);
		assertEquals(TutorialStep.pipeline, model.step());
		model.next(CONVEYOR);
		assertEquals(TutorialStep.breakMerge, model.step());
		//手順 3 以降は「次へ」で進まない
		model.next(CONVEYOR);
		assertEquals(TutorialStep.breakMerge, model.step());
	}

	@Test
	public void fullRunToDone(){
		beginAtBreak();
		model.broke(0, MERGE_DY, CONVEYOR);
		assertEquals(TutorialStep.selectCodeRabbit, model.step());
		model.selected(CODE_RABBIT);
		assertEquals(TutorialStep.placeCodeRabbit, model.step());
		model.placed(0, MERGE_DY, CODE_RABBIT);
		assertEquals(TutorialStep.unpause, model.step());
		model.delivered(1.5f);
		assertEquals(TutorialStep.done, model.step());
		assertTrue(model.active());
	}

	@Test
	public void breakOnAnotherTileIsIgnored(){
		beginAtBreak();
		model.broke(1, MERGE_DY, CONVEYOR);
		model.broke(0, MERGE_DY + 1, UNIT_TEST);
		assertEquals(TutorialStep.breakMerge, model.step());
	}

	@Test
	public void breakOfAnotherBlockOnMergeIsIgnored(){
		beginAtBreak();
		model.broke(0, MERGE_DY, OTHER);
		assertEquals(TutorialStep.breakMerge, model.step());
	}

	@Test
	public void duplicateBreakNotificationIsIgnored(){
		beginAtBreak();
		model.broke(0, MERGE_DY, CONVEYOR);
		model.broke(0, MERGE_DY, CONVEYOR);
		assertEquals(TutorialStep.selectCodeRabbit, model.step());
	}

	@Test
	public void placingAnotherBlockOrAnotherTileIsIgnored(){
		beginAtBreak();
		model.broke(0, MERGE_DY, CONVEYOR);
		model.selected(CODE_RABBIT);
		model.placed(0, MERGE_DY, OTHER);
		assertEquals(TutorialStep.placeCodeRabbit, model.step());
		model.placed(2, MERGE_DY, CODE_RABBIT);
		assertEquals(TutorialStep.placeCodeRabbit, model.step());
		model.placed(0, MERGE_DY, CODE_RABBIT);
		assertEquals(TutorialStep.unpause, model.step());
		//重複した配置の通知
		model.placed(0, MERGE_DY, CODE_RABBIT);
		assertEquals(TutorialStep.unpause, model.step());
	}

	@Test
	public void placingBeforeSelectionWasObservedStillAdvances(){
		beginAtBreak();
		model.broke(0, MERGE_DY, CONVEYOR);
		model.placed(0, MERGE_DY, CODE_RABBIT);
		assertEquals(TutorialStep.unpause, model.step());
	}

	@Test
	public void selectingAnotherRecipeIsIgnoredAndDeselectingGoesBack(){
		beginAtBreak();
		model.broke(0, MERGE_DY, CONVEYOR);
		model.selected(OTHER);
		model.selected(TutorialModel.NO_BLOCK);
		assertEquals(TutorialStep.selectCodeRabbit, model.step());
		model.selected(CODE_RABBIT);
		assertEquals(TutorialStep.placeCodeRabbit, model.step());
		model.selected(TutorialModel.NO_BLOCK);
		assertEquals(TutorialStep.selectCodeRabbit, model.step());
	}

	@Test
	public void enteringBreakStepSkipsAheadWhenMergeIsAlreadyEmpty(){
		model.begin(false, defaultBase());
		model.next(CONVEYOR);
		//手順 1〜2 の間に合流コンベアを壊していた
		model.next(AIR);
		assertEquals(TutorialStep.selectCodeRabbit, model.step());
	}

	@Test
	public void enteringBreakStepSkipsToUnpauseWhenCodeRabbitIsPlaced(){
		model.begin(false, defaultBase());
		model.next(CONVEYOR);
		model.next(CODE_RABBIT);
		assertEquals(TutorialStep.unpause, model.step());
	}

	@Test
	public void beginWithEmptyMergeOrPlacedCodeRabbitIsGuidable(){
		model.begin(false, new Layout(AIR, UNIT_TEST, CONVEYOR, CONVEYOR));
		assertEquals(TutorialStep.goal, model.step());
		model.next(AIR);
		model.next(AIR);
		assertEquals(TutorialStep.selectCodeRabbit, model.step());

		model.begin(false, new Layout(CODE_RABBIT, UNIT_TEST, CONVEYOR, CONVEYOR));
		model.next(CODE_RABBIT);
		model.next(CODE_RABBIT);
		assertEquals(TutorialStep.unpause, model.step());
	}

	@Test
	public void flippedBaseSampledOnTheWrongSideIsUnguidable(){
		//flipBase の基地は反対側にあるので、符号を間違えて取った位置には何も無い
		model.begin(false, new Layout(AIR, AIR, AIR, AIR));
		assertEquals(TutorialStep.unguidable, model.step());
		assertTrue(model.active());
	}

	@Test
	public void mergeOffsetFollowsFlipBase(){
		assertEquals(-TutorialModel.MERGE_DISTANCE, TutorialModel.mergeOffsetY(false));
		assertEquals(TutorialModel.MERGE_DISTANCE, TutorialModel.mergeOffsetY(true));
		assertEquals(-TutorialModel.GATE_DISTANCE, TutorialModel.gateOffsetY(false));
		assertEquals(TutorialModel.GATE_DISTANCE, TutorialModel.gateOffsetY(true));

		//反転した基地では、合流タイルの判定も反転した位置で行う
		model.begin(true, defaultBase());
		model.next(CONVEYOR);
		model.next(CONVEYOR);
		model.broke(0, -TutorialModel.MERGE_DISTANCE, CONVEYOR);
		assertEquals(TutorialStep.breakMerge, model.step());
		model.broke(0, TutorialModel.MERGE_DISTANCE, CONVEYOR);
		assertEquals(TutorialStep.selectCodeRabbit, model.step());
	}

	@Test
	public void unexpectedBaseIsUnguidable(){
		model.begin(false, new Layout(CONVEYOR, OTHER, CONVEYOR, CONVEYOR));
		assertEquals(TutorialStep.unguidable, model.step());
		model.begin(false, new Layout(CONVEYOR, UNIT_TEST, AIR, CONVEYOR));
		assertEquals(TutorialStep.unguidable, model.step());
		model.begin(false, new Layout(OTHER, UNIT_TEST, CONVEYOR, CONVEYOR));
		assertEquals(TutorialStep.unguidable, model.step());
		//案内不能では「次へ」も操作も効かない
		model.next(CONVEYOR);
		model.broke(0, MERGE_DY, CONVEYOR);
		assertEquals(TutorialStep.unguidable, model.step());
	}

	@Test
	public void zeroOrNegativeDeliveryDoesNotFinish(){
		beginAtBreak();
		model.broke(0, MERGE_DY, CONVEYOR);
		model.placed(0, MERGE_DY, CODE_RABBIT);
		model.delivered(0f);
		assertEquals(TutorialStep.unpause, model.step());
		model.delivered(-2f);
		assertEquals(TutorialStep.unpause, model.step());
		model.delivered(0.25f);
		assertEquals(TutorialStep.done, model.step());
	}

	@Test
	public void deliveryOutsideUnpauseStepIsIgnored(){
		beginAtBreak();
		model.delivered(3f);
		assertEquals(TutorialStep.breakMerge, model.step());
	}

	@Test
	public void skipAfterPlayerUnpausedDoesNotResume(){
		model.begin(false, defaultBase());
		assertTrue(model.ownsPause());
		model.playing();
		assertFalse(model.ownsPause());
		//プレイヤーがまた一時停止していても、それはチュートリアルの一時停止ではない
		assertFalse(model.shouldResume(true));
	}

	@Test
	public void skipWhileStillOwningPauseResumes(){
		model.begin(false, defaultBase());
		assertTrue(model.shouldResume(true));
		assertFalse(model.shouldResume(false));
	}

	@Test
	public void invalidateEndsTheSessionAndIgnoresLaterInput(){
		beginAtBreak();
		model.invalidate();
		assertFalse(model.active());
		assertFalse(model.ownsPause());
		model.broke(0, MERGE_DY, CONVEYOR);
		model.next(CONVEYOR);
		model.delivered(1f);
		assertNull(model.step());
	}

	@Test
	public void lowBalanceOnlyWhileSelectingOrPlacing(){
		beginAtBreak();
		assertFalse(model.lowBalance(COST - 1));
		model.broke(0, MERGE_DY, CONVEYOR);
		assertTrue(model.lowBalance(COST - 1));
		assertFalse(model.lowBalance(COST));
		model.selected(CODE_RABBIT);
		assertTrue(model.lowBalance(0));
		model.placed(0, MERGE_DY, CODE_RABBIT);
		assertFalse(model.lowBalance(0));
	}

	@Test
	public void stepNumbers(){
		assertEquals(7, TutorialStep.NUMBERED);
		assertEquals(1, TutorialStep.goal.number());
		assertEquals(7, TutorialStep.done.number());
		assertEquals(0, TutorialStep.unguidable.number());
	}
}
