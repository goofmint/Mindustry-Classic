// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.game;

import io.anuke.mindustry.resource.CodeItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * バランス調整(Task 5.3)の机上計算。コードの流れを「1 分あたりの個数」で表し、期待得点と残高の推移を出す。
 * 値は呼び出し側(BalanceModelTest)が実際のブロック定義と Balance から読んで渡す。このクラスは数値を持たない。
 * 端数の切り捨て、搬送の遅れ、敵の攻撃による電力の消費は扱わない(定常状態の期待値)。
 * 防衛(Issue #58)は、砲台の与ダメージと、敵ごとの「倒しきらなければならない時間」の比較で見る。
 */
final class BalanceModel{
	/** 残高の推移を計算する刻み(秒) */
	static final double STEP_SECONDS = 1.0;
	/** 1 秒あたりのフレーム数。ブロックと敵の速さ、発射の間隔はフレーム単位で書かれている */
	static final double FRAMES_PER_SECOND = 60.0;

	private BalanceModel(){
	}

	/** 処理時間(秒)から、1 分あたりの処理個数 */
	static double perMinute(double seconds){
		return 60.0 / seconds;
	}

	/** 電力が 1 分あたり powerPerMinute 届くとき、1 個あたり powerPerItem を使う施設が処理できる 1 分あたりの個数 */
	static double powerLimited(double powerPerMinute, double powerPerItem){
		return powerPerMinute / powerPerItem;
	}

	/** 出発点の段階が stageChoices から等確率、重要度が IMPORTANCE から等確率のコードを gain 段階上げて納品したときの、1 個あたりの期待得点 */
	static double expectedScore(int[] stageChoices, int gain){
		double importance = 0.0;
		for(int value : CodeItem.IMPORTANCE) importance += value;
		importance /= CodeItem.IMPORTANCE.length;
		return expectedQuality(stageChoices, gain) * importance;
	}

	/** 同じ条件の、1 個あたりの期待品質(敵の倍率の計算に使う平均品質) */
	static double expectedQuality(int[] stageChoices, int gain){
		double sum = 0.0;
		for(int stage : stageChoices){
			sum += CodeItem.QUALITY[Math.min(stage + gain, CodeItem.MAX_STAGE)];
		}
		return sum / stageChoices.length;
	}

	/** ある時点のコードの流れ。reviewed はレビューを通ったコード、raw は通っていないコード(1 分あたりの個数) */
	static final class Flow{
		final double reviewed, raw;

		Flow(double reviewed, double raw){
			this.reviewed = reviewed;
			this.raw = raw;
		}
	}

	/** 構成ごとに、置いたレビュー施設の数から流れを決める */
	interface Pipeline{
		Flow flow(int reviewers);
	}

	/**
	 * 砲台 1 台の撃ち方(Issue #58)。reload フレームに 1 回、1 回に shots 発の damage を撃ち、1 回ごとに powerPerShot の電力を使う。
	 * 満タン(powerCapacity)の電力を使い切るまでは全速で撃ち、使い切ったあとは発電機が 1 秒に作る powerPerSecond の範囲で撃つ
	 */
	static final class Turret{
		/** 全速で撃つときの 1 秒あたりの与ダメージ */
		final double fullDps;
		/** 電力を使い切ったあとの 1 秒あたりの与ダメージ */
		final double sustainedDps;
		/** 満タンの電力を使い切るまでに与えるダメージ。発電が全速に追いつくなら正の無限大 */
		final double burst;

		Turret(double damage, int shots, double reload, double powerPerShot, double powerCapacity, double powerPerSecond){
			double fullRate = FRAMES_PER_SECOND / reload;
			fullDps = damage * shots * fullRate;
			sustainedDps = damage * shots * Math.min(fullRate, powerPerSecond / powerPerShot);
			double drain = fullRate * powerPerShot - powerPerSecond;
			burst = drain > 0.0 ? fullDps * powerCapacity / drain : Double.POSITIVE_INFINITY;
		}

		/** 合計 amount のダメージを与えるのにかかる時間(秒) */
		double timeToDeal(double amount){
			if(amount <= burst) return amount / fullDps;
			return burst / fullDps + (amount - burst) / sustainedDps;
		}
	}

	/** 防衛の計算に使う敵 1 体。deadline は、砲台の射程に入ってから倒しきらなければならない時間(秒) */
	static final class Target{
		final String name;
		final double health, deadline;

		Target(String name, double health, double deadline){
			this.name = name;
			this.health = health;
			this.deadline = deadline;
		}
	}

	/**
	 * 期限の早い敵から順に、届く砲台 turrets 台で 1 体ずつ倒したときの余裕(秒)。各砲台は倒すべき体力を等分して受け持つ。
	 * どの敵も期限までに倒せれば 0 以上、どれかが間に合わなければ負、届く砲台が無ければ負の無限大、敵がいなければ正の無限大
	 */
	static double defenseMargin(List<Target> targets, Turret turret, int turrets){
		List<Target> sorted = new ArrayList<>(targets);
		sorted.sort(Comparator.comparingDouble(target -> target.deadline));
		double dealt = 0.0, margin = Double.POSITIVE_INFINITY;
		for(Target target : sorted){
			if(turrets == 0) return Double.NEGATIVE_INFINITY;
			dealt += target.health;
			margin = Math.min(margin, target.deadline - turret.timeToDeal(dealt / turrets));
		}
		return margin;
	}

	/** 残高の推移の結果 */
	static final class Run{
		/** 秒ごとの残高(添字が秒) */
		final double[] balance;
		/** 破産した秒。破産しなければ -1 */
		final int bankruptAt;
		/** 目標に届いた秒。届かなければ -1 */
		final int reachedAt;

		Run(double[] balance, int bankruptAt, int reachedAt){
			this.balance = balance;
			this.bankruptAt = bankruptAt;
			this.reachedAt = reachedAt;
		}

		/** minutes 分後の残高 */
		double at(double minutes){
			return balance[(int)Math.round(minutes * 60.0 / STEP_SECONDS)];
		}
	}

	/**
	 * 残高の推移を計算する。開始時点から、残高が reviewerCost 以上になるたびにレビュー施設を 1 台ずつ(maxReviewers 台まで)置く。
	 * 1 分あたりの得点は、レビューを通ったコード × reviewedScore + 通っていないコード × rawScore。
	 * 納品で残高が 0 を下回ったら、その時点で止める(破産)。
	 */
	static Run simulate(double initial, double reviewerCost, int maxReviewers, Pipeline pipeline,
			double reviewedScore, double rawScore, double target, double minutes){
		int steps = (int)Math.round(minutes * 60.0 / STEP_SECONDS);
		double[] balance = new double[steps + 1];
		double current = initial;
		int reviewers = 0;
		int bankruptAt = -1, reachedAt = -1;

		for(int step = 0; step <= steps; step++){
			while(reviewers < maxReviewers && current >= reviewerCost){
				current -= reviewerCost;
				reviewers++;
			}
			balance[step] = current;

			if(bankruptAt < 0 && current < 0.0) bankruptAt = step;
			if(reachedAt < 0 && bankruptAt < 0 && current >= target) reachedAt = step;

			if(bankruptAt < 0){
				Flow flow = pipeline.flow(reviewers);
				current += (flow.reviewed * reviewedScore + flow.raw * rawScore) * STEP_SECONDS / 60.0;
			}
		}
		return new Run(balance, bankruptAt, reachedAt);
	}
}
