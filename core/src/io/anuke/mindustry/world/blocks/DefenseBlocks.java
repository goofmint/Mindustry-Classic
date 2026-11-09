package io.anuke.mindustry.world.blocks;

import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.Wall;
import io.anuke.mindustry.world.blocks.types.defense.*;

public class DefenseBlocks{
	static final int wallHealthMultiplier = 4;
	
	public static final Block

	stonewall = new Wall("stonewall"){{
		health = 40*wallHealthMultiplier;
	}},

	ironwall = new Wall("ironwall"){{
		health = 80*wallHealthMultiplier;
	}},

	titaniumwall = new Wall("titaniumwall"){{
		health = 150*wallHealthMultiplier;
	}},
	steelwalllarge = new Wall("steelwall-large"){{
		health = 110*4*wallHealthMultiplier;
		width = height = 2;
	}},
	titaniumwalllarge = new Wall("titaniumwall-large"){{
		health = 150*4*wallHealthMultiplier;
		width = height = 2;
	}},
	titaniumshieldwall = new ShieldedWallBlock("titaniumshieldwall"){{
		health = 150*wallHealthMultiplier;
	}},

	repairturret = new RepairTurret("repairturret"){
		{
			range = 30;
			reload = 20f;
			health = 60;
			powerUsed = 0.08f;
		}
	},

	megarepairturret = new RepairTurret("megarepairturret"){
		{
			range = 44;
      		reload = 12f;
			health = 90;
			powerUsed = 0.13f;
		}
	},

	shieldgenerator = new ShieldBlock("shieldgenerator"){
		{
			health = 100*wallHealthMultiplier;
		}
	},
	door = new Door("door"){{
		health = 90*wallHealthMultiplier;
	}},
	largedoor = new Door("door-large"){{
		openfx = Fx.dooropenlarge;
		closefx = Fx.doorcloselarge;
		health = 90*4*wallHealthMultiplier;
		width = height = 2;
	}};
}
