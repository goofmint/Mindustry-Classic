// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.I18NBundle;
import com.badlogic.gdx.utils.JsonReader;
import io.anuke.mindustry.resource.CodeItem;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.resource.Recipes;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.mindustry.world.blocks.WeaponBlocks;
import io.anuke.mindustry.world.blocks.types.production.ItemPowerGenerator;
import io.anuke.ucore.core.Core;
import java.lang.reflect.Field;
import java.util.Locale;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * バランスの机上計算(Task 5.3、Issue #29 の合格基準 1〜4)。値はすべて実際のブロック定義、Recipes、Balance、maps.json から読む。
 * 構成は Issue の参考構成に合わせる。
 * <ul>
 * <li>A: 初期配置のまま、何もしない</li>
 * <li>B: 初期配置 + シニアエンジニア 2 台(左右の合流点のコンベアと置き換える。残高が足りるたびに 1 台ずつ置く)</li>
 * <li>C: 初期配置 + CodeRabbit 1 台(中央の合流点のコンベアと置き換える)と、その電力</li>
 * </ul>
 */
public class BalanceModelTest{
	/** World.setDefaultBlocks() の人間のエンジニアの数(左右それぞれ。左右の 4 台ずつが、それぞれの合流点に流れ込む) */
	private static final int ENGINEERS_PER_SIDE = 4;
	private static final int SIDES = 2;
	/** World.setDefaultBlocks() で、中央の家電屋 1 台が燃料を配る発電機の数(ユニットテスト用と、合流点の CodeRabbit 用。砲台用の 2 組は別) */
	private static final int GENERATORS_PER_DRILL = 2;

	/** 合格基準 1: 何もしなくても残高が 0 を下回らない時間(分) */
	private static final double IDLE_MINUTES = 3.0;
	/** 合格基準 2: 構成 B が目標に届く時間の範囲(分) */
	private static final double CLEAR_MIN_MINUTES = 10.0, CLEAR_MAX_MINUTES = 15.0;
	/** 合格基準 3: 構成 C が B を上回っているかを比べる時点(分) */
	private static final double COMPARE_MINUTES = 5.0;
	/** 推移を計算する長さ(分) */
	private static final double HORIZON_MINUTES = 30.0;

	@BeforeClass
	public static void loadBundle(){
		//ブロックの定義は名前を Core.bundle から引くので、先に英語の文言を読み込む(作業ディレクトリは core)
		I18NBundle.setExceptionOnMissingKey(false);
		Core.bundle = I18NBundle.createBundle(new FileHandle("assets/bundles/bundle"), Locale.ROOT);
	}

	/** protected な設定値を、親クラスまでさかのぼって読む */
	private static Object field(Object owner, String name){
		for(Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()){
			try{
				Field field = type.getDeclaredField(name);
				field.setAccessible(true);
				return field.get(owner);
			}catch(NoSuchFieldException e){
				//親クラスを探す
			}catch(IllegalAccessException e){
				throw new AssertionError(e);
			}
		}
		throw new AssertionError("no field " + name + " in " + owner.getClass());
	}

	private static float floatField(Object owner, String name){
		return ((Number)field(owner, name)).floatValue();
	}

	private static int intField(Object owner, String name){
		return ((Number)field(owner, name)).intValue();
	}

	/** 建設費(credit) */
	private static int cost(Block block){
		for(ItemStack stack : Recipes.getByResult(block).requirements){
			if(stack.item == Item.credit) return stack.amount;
		}
		throw new AssertionError("no credit cost for " + block.name);
	}

	/** maps.json の 1 マップ目(prototype)の目標得点 */
	private static int prototypeTarget(){
		return new JsonReader().parse(new FileHandle("assets/maps/maps.json")).get("maps").get(0).getInt("targetScore");
	}

	/** 机上計算に使う値をまとめて読む */
	private static final class Model{
		final int[] humanStages = (int[])field(ProductionBlocks.humanEngineer, "stageChoices");
		final double sideSupply = ENGINEERS_PER_SIDE * BalanceModel.perMinute(floatField(ProductionBlocks.humanEngineer, "time"));

		final Block senior = ProductionBlocks.seniorEngineer;
		final int seniorGain = intField(senior, "stages");
		final double seniorRate = BalanceModel.perMinute(floatField(senior, "processTime"));

		final Block rabbit = WeaponBlocks.coderabbit;
		final int rabbitGain = intField(rabbit, "stages");
		final double rabbitRate = BalanceModel.perMinute(floatField(rabbit, "processTime"));

		/** 発電機 1 台が 1 分あたりに作る電力。家電屋 1 台の燃料を GENERATORS_PER_DRILL 台で分け、燃料 1 個で powerOutput × itemDuration 作る */
		final ItemPowerGenerator generator = (ItemPowerGenerator)ProductionBlocks.coalgenerator;
		final double generatorPower = BalanceModel.perMinute(floatField(ProductionBlocks.coaldrill, "time")) / GENERATORS_PER_DRILL
				* generator.powerOutput * generator.itemDuration;

		/** 初期配置のコアの手前にあるユニットテスト。上がる段数と、1 分あたりの処理個数(処理時間と電力の小さいほう) */
		final Block inlet = ProductionBlocks.unitTest;
		final int inletGain = intField(inlet, "stages");
		final double inletRate = Math.min(BalanceModel.perMinute(floatField(inlet, "processTime")),
				BalanceModel.powerLimited(generatorPower, floatField(inlet, "powerPerItem")));
		/** 中央の合流点に置いた CodeRabbit が、もう 1 台の発電機の電力で処理できる 1 分あたりの個数(迎撃に使う電力は含めない) */
		final double rabbitPowerRate = BalanceModel.powerLimited(generatorPower, floatField(rabbit, "powerPerItem"));

		final double initial = Balance.INITIAL_CREDIT;
		final double target = prototypeTarget();

		/** A: 何もしない。全員のコードがそのままコアの手前の施設を通って納品される */
		BalanceModel.Run idle(){
			BalanceModel.Pipeline pipeline = reviewers -> new BalanceModel.Flow(0.0, Math.min(SIDES * sideSupply, inletRate));
			return BalanceModel.simulate(initial, 0.0, 0, pipeline, 0.0, rawScore(), target, HORIZON_MINUTES);
		}

		/**
		 * B: シニアエンジニアを左右の合流点に 1 台ずつ。置いていない側の未レビューのコードは途切れずに中央の合流点を埋めるので、
		 * 下流の処理枠はそちらが先に使い、レビュー済みのコードは残りの枠だけ流れる(ブラウザで 1 台だけ置いたとき、残高が減り続けた)
		 */
		BalanceModel.Run seniors(){
			double perSide = Math.min(seniorRate, sideSupply);
			BalanceModel.Pipeline pipeline = reviewers -> {
				double raw = Math.min((SIDES - reviewers) * sideSupply, inletRate);
				double reviewed = Math.min(reviewers * perSide, inletRate - raw);
				return new BalanceModel.Flow(reviewed, raw);
			};
			return BalanceModel.simulate(initial, cost(senior), SIDES, pipeline,
					BalanceModel.expectedScore(humanStages, seniorGain + inletGain), rawScore(), target, HORIZON_MINUTES);
		}

		/** C: CodeRabbit を中央の合流点に 1 台。全員のコードが CodeRabbit を通る */
		BalanceModel.Run rabbit(){
			double rate = Math.min(Math.min(SIDES * sideSupply, rabbitRate), Math.min(rabbitPowerRate, inletRate));
			BalanceModel.Pipeline pipeline = reviewers -> reviewers == 0
					? new BalanceModel.Flow(0.0, Math.min(SIDES * sideSupply, inletRate))
					: new BalanceModel.Flow(rate, 0.0);
			return BalanceModel.simulate(initial, cost(rabbit), 1, pipeline,
					BalanceModel.expectedScore(humanStages, rabbitGain + inletGain), rawScore(), target, HORIZON_MINUTES);
		}

		double rawScore(){
			return BalanceModel.expectedScore(humanStages, inletGain);
		}

		/** 1 分あたりの期待得点(定常状態) */
		double idleScorePerMinute(){
			return Math.min(SIDES * sideSupply, inletRate) * rawScore();
		}

		double seniorScorePerMinute(){
			return Math.min(SIDES * Math.min(seniorRate, sideSupply), inletRate)
					* BalanceModel.expectedScore(humanStages, seniorGain + inletGain);
		}

		double rabbitScorePerMinute(){
			return Math.min(Math.min(SIDES * sideSupply, rabbitRate), Math.min(rabbitPowerRate, inletRate))
					* BalanceModel.expectedScore(humanStages, rabbitGain + inletGain);
		}
	}

	private static String minutes(int step){
		return step < 0 ? "never" : String.format(Locale.ROOT, "%.1f min", step * BalanceModel.STEP_SECONDS / 60.0);
	}

	private static void report(String name, BalanceModel.Run run, double perMinute, double quality){
		System.out.println(String.format(Locale.ROOT,
				"[BalanceModel] %s: %.1f/min, multiplier %.2f, balance 1/3/5 min = %.0f / %.0f / %.0f, bankrupt %s, target %s",
				name, perMinute, Score.multiplierFor((float)quality), run.at(1), run.at(3), run.at(5),
				minutes(run.bankruptAt), minutes(run.reachedAt)));
	}

	@Test
	public void expectedScoreUsesQualityTimesImportance(){
		//出発点 {2, 3}、上昇なし: 品質 (-2 + -1) / 2 = -1.5、重要度 (1 + 2) / 2 = 1.5 → -2.25
		int[] stages = {2, 3};
		double importance = (CodeItem.IMPORTANCE[0] + CodeItem.IMPORTANCE[1]) / 2.0;
		double quality = (CodeItem.QUALITY[2] + CodeItem.QUALITY[3]) / 2.0;
		assertEquals(quality * importance, BalanceModel.expectedScore(stages, 0), 1e-9);
		//段階は MAX_STAGE で止まる
		assertEquals(CodeItem.QUALITY[CodeItem.MAX_STAGE] * importance, BalanceModel.expectedScore(new int[]{CodeItem.MAX_STAGE}, 3), 1e-9);
	}

	@Test
	public void simulateBuysReviewersWhenAffordableAndStopsOnBankruptcy(){
		//残高 100、費用 80 のレビュー施設を 1 台だけ置き、レビュー済み 60 個/分 × 1 点 → 1 秒に 1 点増える
		BalanceModel.Pipeline pipeline = reviewers -> new BalanceModel.Flow(reviewers * 60.0, 0.0);
		BalanceModel.Run run = BalanceModel.simulate(100.0, 80.0, 1, pipeline, 1.0, 0.0, 30.0, 1.0);
		assertEquals(20.0, run.balance[0], 1e-9);
		assertEquals(10, run.reachedAt);
		assertEquals(-1, run.bankruptAt);

		//未レビュー 60 個/分 × -1 点、残高 5 → 6 秒目に 0 を下回って止まる
		BalanceModel.Pipeline rawOnly = reviewers -> new BalanceModel.Flow(0.0, 60.0);
		BalanceModel.Run broke = BalanceModel.simulate(5.0, 0.0, 0, rawOnly, 0.0, -1.0, 30.0, 1.0);
		assertEquals(6, broke.bankruptAt);
		assertEquals(broke.balance[6], broke.balance[60], 1e-9);
	}

	@Test
	public void acceptanceCriteria(){
		Model model = new Model();
		BalanceModel.Run idle = model.idle(), seniors = model.seniors(), rabbit = model.rabbit();

		System.out.println(String.format(Locale.ROOT, "[BalanceModel] initial %.0f, target %.0f", model.initial, model.target));
		report("A idle", idle, model.idleScorePerMinute(), BalanceModel.expectedQuality(model.humanStages, model.inletGain));
		report("B 2 seniors", seniors, model.seniorScorePerMinute(),
				BalanceModel.expectedQuality(model.humanStages, model.seniorGain + model.inletGain));
		report("C CodeRabbit", rabbit, model.rabbitScorePerMinute(),
				BalanceModel.expectedQuality(model.humanStages, model.rabbitGain + model.inletGain));

		//基準 1: 何もしなくても 3 分間は破産しない
		assertTrue("A goes bankrupt at " + minutes(idle.bankruptAt),
				idle.bankruptAt < 0 || idle.bankruptAt * BalanceModel.STEP_SECONDS >= IDLE_MINUTES * 60.0);

		//基準 2: シニア 2 台で、目標に 10〜15 分で届く
		assertTrue("B reaches the target at " + minutes(seniors.reachedAt), seniors.reachedAt >= 0
				&& seniors.reachedAt * BalanceModel.STEP_SECONDS >= CLEAR_MIN_MINUTES * 60.0
				&& seniors.reachedAt * BalanceModel.STEP_SECONDS <= CLEAR_MAX_MINUTES * 60.0);

		//基準 3: CodeRabbit のほうが早く届き、5 分時点の残高が上回る
		assertTrue("C reaches the target at " + minutes(rabbit.reachedAt) + ", B at " + minutes(seniors.reachedAt),
				rabbit.reachedAt >= 0 && rabbit.reachedAt < seniors.reachedAt);
		assertTrue("C at 5 min " + rabbit.at(COMPARE_MINUTES) + " vs B " + seniors.at(COMPARE_MINUTES),
				rabbit.at(COMPARE_MINUTES) > seniors.at(COMPARE_MINUTES));

		//敵の倍率: 品質の高い構成ほど敵が少ない
		float idleMultiplier = Score.multiplierFor((float)BalanceModel.expectedQuality(model.humanStages, model.inletGain));
		float seniorMultiplier = Score.multiplierFor((float)BalanceModel.expectedQuality(model.humanStages, model.seniorGain + model.inletGain));
		float rabbitMultiplier = Score.multiplierFor((float)BalanceModel.expectedQuality(model.humanStages, model.rabbitGain + model.inletGain));
		assertTrue(rabbitMultiplier <= seniorMultiplier && seniorMultiplier <= idleMultiplier);
	}
}
