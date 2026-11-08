package io.anuke.mindustry.resource;

import com.badlogic.gdx.utils.ObjectMap;

/**
 * コードのアイテム。品質の段階(stage)× 通過状態(passed のビット)× 重要度(importance)のうち、
 * 出発点から到達できる組み合わせだけを Item として登録し、属性と Item を相互に引く。
 * 静的初期化では Item に触らない(Item の静的初期化から registerAll() が呼ばれるため)。
 */
public final class CodeItem{
	/** 品質の段階。配列の添字が段階で、値が得点に使う品質値 */
	public static final float[] QUALITY = {-5f, -3f, -2f, -1f, 0.5f, 0.8f, 1f};
	public static final int MAX_STAGE = QUALITY.length - 1;

	/** 通過状態のビット */
	public static final int REVIEW = 1, TEST = 2, CI = 4;

	/** 重要度。値がそのまま倍率 */
	public static final int[] IMPORTANCE = {1, 2};

	/** passed の取りうる値の数(REVIEW | TEST | CI の 3 ビット) */
	private static final int PASSED_COUNT = 8;
	/** 出発点の段階(この段階まで、通過状態は全未) */
	private static final int MAX_START_STAGE = 3;

	/** 施設を通したときの遷移。{通過ビット, 上がる段階数}。シニア(REVIEW+1)、CodeRabbit(REVIEW+2)、テスト(TEST+1)、CI(CI+1) */
	private static final int[][] TRANSITIONS = {{REVIEW, 1}, {REVIEW, 2}, {TEST, 1}, {CI, 1}};

	/** [stage][passed][importance - 1] */
	private static final Item[][][] forward = new Item[QUALITY.length][PASSED_COUNT][IMPORTANCE.length];
	private static final ObjectMap<Item, CodeAttr> reverse = new ObjectMap<>();
	private static boolean registered = false;

	private CodeItem(){
	}

	/** 到達可能な組み合わせだけを Item として登録する。Item の静的初期化の末尾から呼ばれる。2 回目以降は何もしない */
	public static void registerAll(){
		//Item のクラス初期化を先に済ませる。Item の初期化中にここへ来た場合は、すでに初期化中なのでそのまま戻る
		Item.getAllItems();
		if(registered) return;

		boolean[][] reachable = new boolean[QUALITY.length][PASSED_COUNT];
		int[] queueStage = new int[QUALITY.length * PASSED_COUNT];
		int[] queuePassed = new int[QUALITY.length * PASSED_COUNT];
		int head = 0, tail = 0;

		for(int stage = 0; stage <= MAX_START_STAGE; stage++){
			reachable[stage][0] = true;
			queueStage[tail] = stage;
			queuePassed[tail] = 0;
			tail++;
		}

		while(head < tail){
			int stage = queueStage[head];
			int passed = queuePassed[head];
			head++;

			for(int[] transition : TRANSITIONS){
				int bit = transition[0];
				if((passed & bit) != 0) continue;

				int nextStage = Math.min(stage + transition[1], MAX_STAGE);
				int nextPassed = passed | bit;
				if(!reachable[nextStage][nextPassed]){
					reachable[nextStage][nextPassed] = true;
					queueStage[tail] = nextStage;
					queuePassed[tail] = nextPassed;
					tail++;
				}
			}
		}

		//正準順: 重要度 → 通過状態 → 段階の昇順
		for(int importance : IMPORTANCE){
			for(int passed = 0; passed < PASSED_COUNT; passed++){
				for(int stage = 0; stage <= MAX_STAGE; stage++){
					if(!reachable[stage][passed]) continue;

					Item item = new Item("code-q" + stage + "-p" + passed + "-i" + importance);
					forward[stage][passed][importance - 1] = item;
					reverse.put(item, new CodeAttr(stage, passed, importance));
				}
			}
		}

		registered = true;
	}

	/** 属性から Item を引く。範囲外や登録されていない組み合わせなら IllegalArgumentException */
	public static Item get(int stage, int passed, int importance){
		Item.getAllItems();

		if(stage < 0 || stage > MAX_STAGE || passed < 0 || passed >= PASSED_COUNT
				|| importance < 1 || importance > IMPORTANCE.length){
			throw new IllegalArgumentException("Code item attributes out of range: stage=" + stage
					+ ", passed=" + passed + ", importance=" + importance);
		}

		Item item = forward[stage][passed][importance - 1];
		if(item == null){
			throw new IllegalArgumentException("Code item not registered: stage=" + stage
					+ ", passed=" + passed + ", importance=" + importance);
		}
		return item;
	}

	/** Item からの逆引き。コードでないアイテム(燃料、credit、旧アイテム)や null なら null */
	public static CodeAttr attrOf(Item item){
		Item.getAllItems();

		if(item == null) return null;
		return reverse.get(item);
	}

	/** 得点 = 品質 × 重要度。コードでないアイテムは 0 */
	public static float score(Item item){
		CodeAttr attr = attrOf(item);
		if(attr == null) return 0f;
		return QUALITY[attr.stage] * IMPORTANCE[attr.importance - 1];
	}

	/** 施設を通したあとの Item。passedBit が既に立っていれば同じ Item を返す(品質は変えない) */
	public static Item advance(Item item, int passedBit, int stages){
		CodeAttr attr = attrOf(item);
		if(attr == null){
			throw new IllegalArgumentException("Not a code item: " + (item == null ? "null" : item.name));
		}
		if(passedBit != REVIEW && passedBit != TEST && passedBit != CI){
			throw new IllegalArgumentException("passedBit must be exactly one of REVIEW, TEST, CI: " + passedBit);
		}
		if(stages < 0){
			throw new IllegalArgumentException("stages must not be negative: " + stages);
		}

		if((attr.passed & passedBit) != 0) return item;

		return get(Math.min(attr.stage + stages, MAX_STAGE), attr.passed | passedBit, attr.importance);
	}

	/** コードの属性。Item ごとに 1 個だけ作るので、== で比べられる */
	public static final class CodeAttr{
		public final int stage, passed, importance;

		private CodeAttr(int stage, int passed, int importance){
			this.stage = stage;
			this.passed = passed;
			this.importance = importance;
		}
	}
}
