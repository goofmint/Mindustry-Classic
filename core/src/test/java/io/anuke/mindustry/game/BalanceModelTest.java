// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.I18NBundle;
import com.badlogic.gdx.utils.JsonReader;
import io.anuke.mindustry.entities.enemies.EnemyType;
import io.anuke.mindustry.entities.enemies.EnemyTypes;
import io.anuke.mindustry.resource.CodeItem;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.resource.Recipes;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.mindustry.world.blocks.WeaponBlocks;
import io.anuke.mindustry.world.blocks.types.defense.LaserTurret;
import io.anuke.mindustry.world.blocks.types.defense.PowerTurret;
import io.anuke.mindustry.world.blocks.types.production.ItemPowerGenerator;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.entities.BaseBulletType;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
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

	/** Issue #58 の合格基準: 何もしなくても、初期配置の砲台でこの wave まで守りきる */
	private static final int DEFENDED_WAVES = 5;
	/** World.setDefaultBlocks() の初期配置の砲台の台数 */
	private static final int DEFAULT_TURRETS = 2;
	/** World.setDefaultBlocks() で、砲台を置くタイルのコアからのずれ (x±2, y+1) */
	private static final int TURRET_OFFSET_X = 2, TURRET_OFFSET_Y = 1;
	/** World.setDefaultBlocks() で、砲台ごとの発電機 1 台に燃料を配る家電屋の数(発電機 1 台に 1 台) */
	private static final int TURRET_DRILLS_PER_GENERATOR = 1;
	/** maps.json の prototype の敵の出現点の数(マップのロード時に「1 spawns」とログに出る) */
	private static final int PROTOTYPE_LANES = 1;
	/** タイル 1 枚の大きさ(Vars.tilesize。Vars を読み込むと描画の初期化が要るので、ここに書く) */
	private static final double TILE_SIZE = 8.0;
	/** EnemyType.move() の定数: 速さは speed + 0.04 × tier、射程は range + 5 × tier、コアから (射程 - 18) の距離で止まる */
	private static final double SPEED_PER_TIER = 0.04, RANGE_PER_TIER = 5.0, STOP_INSIDE_RANGE = 18.0;
	/** EnemyType.updateShooting() と Enemy.shoot() の定数: 発射の間隔は reload / max(tier / 1.5, 1)、弾のダメージは damage × tier */
	private static final double RELOAD_TIER_DIVISOR = 1.5;

	@BeforeClass
	public static void loadBundle(){
		//ブロックの定義は名前を Core.bundle から引くので、先に英語の文言を読み込む(作業ディレクトリは core)
		I18NBundle.setExceptionOnMissingKey(false);
		Core.bundle = I18NBundle.createBundle(new FileHandle("assets/bundles/bundle"), Locale.ROOT);
	}

	/** World.setDefaultBlocks() の初期配置の砲台(Issue #58 で WeaponBlocks.turret から変えた)。ブロックの定義は文言を読み込んでから作るので、メソッドで返す */
	private static Block defaultTurret(){
		return WeaponBlocks.laserturret;
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

	/** 防衛の机上計算に使う値。砲台、発電機、敵、コアの値はブロックと敵の定義から読む */
	private static final class Defense{
		final double idleMultiplier;
		final double coreHealth = ProductionBlocks.core.health;
		/** 砲台用の発電機 1 台が 1 秒に作る電力(砲台 1 台に発電機 1 台) */
		final double turretPower;

		Defense(Model model){
			idleMultiplier = Score.multiplierFor((float)BalanceModel.expectedQuality(model.humanStages, model.inletGain));
			ItemPowerGenerator generator = model.generator;
			//家電屋は time 秒に 1 個の燃料を出し、発電機は燃料 1 個で powerOutput × itemDuration の電力を作る
			turretPower = TURRET_DRILLS_PER_GENERATOR / floatField(ProductionBlocks.coaldrill, "time")
					* generator.powerOutput * generator.itemDuration;
		}

		/** 砲台 1 台の撃ち方。レーザーの砲台は弾を使わず、自分の damage をそのまま与える */
		BalanceModel.Turret turret(Block block){
			PowerTurret turret = (PowerTurret)block;
			double damage = turret instanceof LaserTurret ? intField(turret, "damage") : ((BaseBulletType<?>)field(turret, "bullet")).damage;
			return new BalanceModel.Turret(damage, intField(turret, "shots"), floatField(turret, "reload"),
					turret.powerUsed, turret.powerCapacity, turretPower);
		}

		/** 敵がコアの手前で止まる距離 */
		static double stopDistance(EnemyType type, int tier){
			return floatField(type, "range") + RANGE_PER_TIER * tier - STOP_INSIDE_RANGE;
		}

		/** コアから stop の距離で止まった敵に届く砲台の数。止まる方向を 1 度ずつ変え、いちばん少ない方向の台数を返す */
		static int turretsReaching(double range, double stop){
			int fewest = DEFAULT_TURRETS;
			for(int degree = 0; degree < 360; degree++){
				double x = stop * Math.cos(Math.toRadians(degree)), y = stop * Math.sin(Math.toRadians(degree));
				int reaching = 0;
				for(int side : new int[]{-1, 1}){
					if(Math.hypot(x - side * TURRET_OFFSET_X * TILE_SIZE, y - TURRET_OFFSET_Y * TILE_SIZE) <= range) reaching++;
				}
				fewest = Math.min(fewest, reaching);
			}
			return fewest;
		}

		/** wave に出る敵の、何もしない構成の倍率を掛けて切り上げた数(多いほうに丸める) */
		int count(EnemySpawn spawn, int wave){
			return (int)Math.ceil(spawn.baseAmount(wave, 0, Difficulty.normal.enemyScaling) * idleMultiplier) * PROTOTYPE_LANES;
		}

		/**
		 * wave の敵の一覧と、それぞれを倒しきらなければならない時間。
		 * <ul>
		 * <li>コアの手前で止まって撃つ敵: 期限は、その wave の敵がそろってコアを撃って壊すまでの時間</li>
		 * <li>止まらずにぶつかる敵(blast): 射程に入ってから砲台に着くまでの時間(射程 / 速さ)</li>
		 * <li>ヒーラー: コアを撃たないが、ほかの敵より内側で止まるので砲台が先に狙う(テストで確かめる)。期限はほかの止まる敵と同じにする。
		 * 回復する相手がまだ傷ついていないので、回復は数えない</li>
		 * </ul>
		 */
		List<BalanceModel.Target> wave(int wave, Block turret){
			double range = floatField(turret, "range");
			double coreDps = 0.0;
			for(EnemySpawn spawn : WaveCreator.getSpawns()){
				EnemyType type = spawn.type;
				int tier = spawn.tier(wave, 0);
				if(type == EnemyTypes.healer || !(boolean)field(type, "stopNearCore")) continue;
				BaseBulletType<?> bullet = (BaseBulletType<?>)field(type, "bullet");
				double reload = floatField(type, "reload") / Math.max(tier / RELOAD_TIER_DIVISOR, 1.0);
				coreDps += count(spawn, wave) * bullet.damage * tier * BalanceModel.FRAMES_PER_SECOND / reload;
			}

			List<BalanceModel.Target> targets = new ArrayList<>();
			for(EnemySpawn spawn : WaveCreator.getSpawns()){
				EnemyType type = spawn.type;
				int tier = spawn.tier(wave, 0);
				double health = intField(type, "health") * tier;
				double speed = (floatField(type, "speed") + SPEED_PER_TIER * tier) * BalanceModel.FRAMES_PER_SECOND;
				double deadline;
				if(!(boolean)field(type, "stopNearCore")){
					deadline = range / speed;
				}else if(coreDps == 0.0){
					deadline = Double.POSITIVE_INFINITY;
				}else{
					deadline = coreHealth / coreDps;
				}
				for(int n = 0; n < count(spawn, wave); n++){
					targets.add(new BalanceModel.Target(type.name, health, deadline));
				}
			}
			return targets;
		}

		/** wave の敵すべてに届く砲台の数。止まる敵は止まる位置で数え、ぶつかる敵は手前の 1 台だけが届くとする */
		int reaching(int wave, Block turret){
			double range = floatField(turret, "range");
			int reaching = DEFAULT_TURRETS;
			for(EnemySpawn spawn : WaveCreator.getSpawns()){
				if(count(spawn, wave) == 0) continue;
				reaching = Math.min(reaching, (boolean)field(spawn.type, "stopNearCore")
						? turretsReaching(range, stopDistance(spawn.type, spawn.tier(wave, 0))) : 1);
			}
			return reaching;
		}

		/** wave にヒーラーがいれば、ほかの止まる敵より内側で止まる(砲台が先に狙う) */
		boolean healerStopsInside(int wave){
			double healer = Double.POSITIVE_INFINITY, others = Double.POSITIVE_INFINITY;
			for(EnemySpawn spawn : WaveCreator.getSpawns()){
				if(count(spawn, wave) == 0 || !(boolean)field(spawn.type, "stopNearCore")) continue;
				double stop = stopDistance(spawn.type, spawn.tier(wave, 0));
				if(spawn.type == EnemyTypes.healer){
					healer = Math.min(healer, stop);
				}else{
					others = Math.min(others, stop);
				}
			}
			return healer == Double.POSITIVE_INFINITY || healer < others;
		}

		double margin(int wave, Block turret){
			return BalanceModel.defenseMargin(wave(wave, turret), turret(turret), reaching(wave, turret));
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
	public void defenseMarginKillsInDeadlineOrder(){
		//1 秒に 1 回 10 ダメージ、電力は足りる: 50 を 5 秒で倒し(期限 6)、次の 30 を 8 秒目に倒す(期限 10) → 余裕は min(1, 2) = 1
		BalanceModel.Turret steady = new BalanceModel.Turret(10.0, 1, BalanceModel.FRAMES_PER_SECOND, 0.1, 1.0, 1.0);
		List<BalanceModel.Target> targets = new ArrayList<>();
		targets.add(new BalanceModel.Target("late", 30.0, 10.0));
		targets.add(new BalanceModel.Target("early", 50.0, 6.0));
		assertEquals(1.0, BalanceModel.defenseMargin(targets, steady, 1), 1e-9);
		//2 台で等分すると 2.5 秒と 4 秒 → 余裕は min(3.5, 6) = 3.5。届く砲台が無ければ守れない
		assertEquals(3.5, BalanceModel.defenseMargin(targets, steady, 2), 1e-9);
		assertEquals(Double.NEGATIVE_INFINITY, BalanceModel.defenseMargin(targets, steady, 0), 0.0);
	}

	@Test
	public void turretSlowsDownWhenItsPowerRunsOut(){
		//15 フレームに 1 回(4 回/秒)3 ダメージ、1 回 0.1、満タン 1、発電 0.2/秒: 電力は 1 秒に 0.2 ずつ減り、5 秒で 60 ダメージを与えて空になる
		BalanceModel.Turret turret = new BalanceModel.Turret(3.0, 1, 15.0, 0.1, 1.0, 0.2);
		assertEquals(12.0, turret.fullDps, 1e-9);
		assertEquals(60.0, turret.burst, 1e-9);
		//そのあとは発電に合わせて 2 回/秒(6 ダメージ/秒)
		assertEquals(6.0, turret.sustainedDps, 1e-9);
		assertEquals(5.0, turret.timeToDeal(60.0), 1e-9);
		assertEquals(7.0, turret.timeToDeal(72.0), 1e-9);
	}

	/** Issue #58: 何もしなくても、初期配置の砲台 2 台で wave 1〜5 を守りきる。比べるために、元の砲台(リンター)とファザーの結果も出す */
	@Test
	public void defaultDefenseHoldsFirstWaves(){
		Defense defense = new Defense(new Model());
		Block turret = defaultTurret();
		Block[] compared = {WeaponBlocks.turret, WeaponBlocks.machineturret, turret};
		for(Block block : compared){
			BalanceModel.Turret stats = defense.turret(block);
			System.out.println(String.format(Locale.ROOT, "[BalanceModel] %s: range %.0f, health %d, dps %.1f until %.0f damage, then %.1f",
					block.name, floatField(block, "range"), block.health, stats.fullDps, stats.burst, stats.sustainedDps));
		}
		for(int wave = 1; wave <= DEFENDED_WAVES; wave++){
			StringBuilder line = new StringBuilder();
			double health = 0.0;
			for(BalanceModel.Target target : defense.wave(wave, turret)){
				line.append(target.name).append(' ');
				health += target.health;
			}
			line.append(String.format(Locale.ROOT, "(health %.0f)", health));
			for(Block block : compared){
				line.append(String.format(Locale.ROOT, " / %s: %d reach, margin %.1f s", block.name, defense.reaching(wave, block), defense.margin(wave, block)));
			}
			System.out.println("[BalanceModel] wave " + wave + ": " + line);

			double margin = defense.margin(wave, turret);
			assertTrue("healer stops outside other enemies in wave " + wave, defense.healerStopsInside(wave));
			assertTrue("wave " + wave + " is not defended: margin " + margin, margin >= 0.0);
		}
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
