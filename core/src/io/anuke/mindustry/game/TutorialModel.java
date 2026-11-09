// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.game;

/**
 * チュートリアルの手順の遷移モデル(Issue #78)。libGDX にも world にも依存しない。
 * 座標はコアから見た相対タイル、ブロックは Block.id で受け取る。現在の手順に合わない入力と重複した通知は無視する。
 * 手順に入るときは合流タイルの状態を見て、すでに済んでいる手順を飛ばす。
 * 一時停止の所有(自分が設定した一時停止かどうか)もここで持つ。
 */
public final class TutorialModel{
	/** World.setDefaultBlocks() の初期配置: 中央の合流コンベアはコアから縦に 4 タイル、その手前のユニットテストは 3 タイル */
	public static final int MERGE_DISTANCE = 4;
	public static final int GATE_DISTANCE = 3;
	/** 左右のコードが合流点へ入るコンベアは、合流点の横 1 タイル */
	public static final int SIDE_DISTANCE = 1;
	/** 中央のエンジニア 2 台はコアから横に 2、縦に 2 タイル */
	public static final int ENGINEER_DX = 2;
	public static final int ENGINEER_DISTANCE = 2;
	/** タイルが無い、または何も選んでいないことを表すブロック id */
	public static final int NO_BLOCK = -1;

	/** 判定に使うブロックの id と、CodeRabbit の価格(残高の警告に使う) */
	public static final class Ids{
		public final int conveyor, unitTest, codeRabbit, air, codeRabbitCost;

		public Ids(int conveyor, int unitTest, int codeRabbit, int air, int codeRabbitCost){
			this.conveyor = conveyor;
			this.unitTest = unitTest;
			this.codeRabbit = codeRabbit;
			this.air = air;
			this.codeRabbitCost = codeRabbitCost;
		}
	}

	/** 手順に入る時点の初期配置: 合流タイル、その手前(ユニットテスト)、合流タイルの左右のブロック id */
	public static final class Layout{
		public final int merge, gate, left, right;

		public Layout(int merge, int gate, int left, int right){
			this.merge = merge;
			this.gate = gate;
			this.left = left;
			this.right = right;
		}
	}

	private final Ids ids;
	/** 現在の手順。null はセッションが無い */
	private TutorialStep step;
	private boolean flipBase;
	private boolean ownsPause;

	public TutorialModel(Ids ids){
		this.ids = ids;
	}

	/** 合流コンベアのコアからの縦の相対位置。flipBase で上下が反転する */
	public static int mergeOffsetY(boolean flipBase){
		return flipBase ? MERGE_DISTANCE : -MERGE_DISTANCE;
	}

	/** ユニットテストのコアからの縦の相対位置 */
	public static int gateOffsetY(boolean flipBase){
		return flipBase ? GATE_DISTANCE : -GATE_DISTANCE;
	}

	/** 中央のエンジニアのコアからの縦の相対位置 */
	public static int engineerOffsetY(boolean flipBase){
		return flipBase ? ENGINEER_DISTANCE : -ENGINEER_DISTANCE;
	}

	/** セッションを始める。一時停止の所有を記録し、初期配置を検証して最初の手順に入る */
	public void begin(boolean flipBase, Layout layout){
		this.flipBase = flipBase;
		ownsPause = true;
		if(layout.gate != ids.unitTest || layout.left != ids.conveyor || layout.right != ids.conveyor
				|| stepForMerge(layout.merge) == TutorialStep.unguidable){
			step = TutorialStep.unguidable;
		}else{
			step = TutorialStep.goal;
		}
	}

	public boolean active(){
		return step != null;
	}

	/** 現在の手順。セッションが無ければ null */
	public TutorialStep step(){
		return step;
	}

	public boolean flipBase(){
		return flipBase;
	}

	/** 「次へ」。説明の手順だけ進む。手順 3 に入るときは合流タイルの現在のブロックで先の手順へ飛ばす */
	public void next(int mergeBlock){
		if(step == TutorialStep.goal){
			step = TutorialStep.pipeline;
		}else if(step == TutorialStep.pipeline){
			step = stepForMerge(mergeBlock);
		}
	}

	/** プレイヤーが (dx, dy) のブロック block を撤去した */
	public void broke(int dx, int dy, int block){
		if(step == TutorialStep.breakMerge && isMerge(dx, dy) && block == ids.conveyor){
			step = TutorialStep.selectCodeRabbit;
		}
	}

	/** プレイヤーが (dx, dy) に block を配置した。選択の手順を観測する前に置かれた場合も進める */
	public void placed(int dx, int dy, int block){
		if((step == TutorialStep.selectCodeRabbit || step == TutorialStep.placeCodeRabbit)
				&& isMerge(dx, dy) && block == ids.codeRabbit){
			step = TutorialStep.unpause;
		}
	}

	/** 現在選択中のレシピの結果ブロック(無ければ NO_BLOCK)。毎フレーム呼ばれる */
	public void selected(int block){
		if(step == TutorialStep.selectCodeRabbit && block == ids.codeRabbit){
			step = TutorialStep.placeCodeRabbit;
		}else if(step == TutorialStep.placeCodeRabbit && block != ids.codeRabbit){
			//選択を外したら、選び直す手順に戻す
			step = TutorialStep.selectCodeRabbit;
		}
	}

	/** コアに納品した得点。プラスの納品だけが手順 6 を完了する */
	public void delivered(float score){
		if(step == TutorialStep.unpause && score > 0f){
			step = TutorialStep.done;
		}
	}

	/** 状態が playing になった。以後、一時停止の所有は無い */
	public void playing(){
		ownsPause = false;
	}

	public boolean ownsPause(){
		return ownsPause;
	}

	/** スキップや終了のときにゲームを再開するか: 今 paused で、かつ自分が設定した一時停止のときだけ */
	public boolean shouldResume(boolean paused){
		return paused && ownsPause;
	}

	/** セッションを終える(スキップ、閉じる、リセット、menu、ゲームオーバー、ネットワーク開始) */
	public void invalidate(){
		step = null;
		ownsPause = false;
	}

	/** 手順 4 と 5 で、残高が CodeRabbit の価格に足りない */
	public boolean lowBalance(int balance){
		return (step == TutorialStep.selectCodeRabbit || step == TutorialStep.placeCodeRabbit)
				&& balance < ids.codeRabbitCost;
	}

	private boolean isMerge(int dx, int dy){
		return dx == 0 && dy == mergeOffsetY(flipBase);
	}

	/** 合流タイルの現在のブロックから、手順 3 以降のどこに入るかを決める */
	private TutorialStep stepForMerge(int mergeBlock){
		if(mergeBlock == ids.conveyor) return TutorialStep.breakMerge;
		if(mergeBlock == ids.air) return TutorialStep.selectCodeRabbit;
		if(mergeBlock == ids.codeRabbit) return TutorialStep.unpause;
		return TutorialStep.unguidable;
	}
}
