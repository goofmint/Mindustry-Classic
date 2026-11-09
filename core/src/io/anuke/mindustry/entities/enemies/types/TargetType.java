// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.entities.enemies.types;

import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.entities.enemies.EnemyType;
import io.anuke.mindustry.entities.enemies.EnemyTypes;
import io.anuke.ucore.core.Timers;

public class TargetType extends EnemyType {
	
	public TargetType(){
		super("targetenemy");

		speed = 0f;
		health = 40;
		shootsound = null;
	}
	
	@Override
	public void move(Enemy enemy){

	}
	
	@Override
	public void shoot(Enemy enemy){
		//do nothing
	}
	
	@Override
	public void removed(Enemy enemy){
		//don't call enemy death since this is only a target
	}
	
	@Override
	public void draw(Enemy enemy){
		super.draw(enemy);
	}
	
	@Override
	public void onDeath(Enemy enemy, boolean force){
		super.onDeath(enemy, force);
		Timers.run(100f, ()->{
			new Enemy(EnemyTypes.target).set(enemy.x, enemy.y).add();
		});
	}

	@Override
	public boolean isCalculating(Enemy enemy){
		return false;
	}
}
