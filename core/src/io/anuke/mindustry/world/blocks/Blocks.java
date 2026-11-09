package io.anuke.mindustry.world.blocks;

import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.*;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Mathf;

public class Blocks{
	public static final Block
	
	air = new Block("air"){
		//no drawing here
		public void drawCache(Tile tile){}
		
		//update floor blocks for effects, if needed
		public void draw(Tile tile){}
	},
	
	blockpart = new BlockPart(){

	},
	
	deepwater = new Floor("deepwater"){{
		variants = 0;
		solid = true;
		liquidDrop = Liquid.water;
		liquid = true;
	}},
	
	water = new Floor("water"){{
		variants = 0;
		solid = true;
		liquidDrop = Liquid.water;
		liquid = true;
	}},
	
	lava = new Floor("lava"){
		{
			variants = 0;
			solid = true;
			liquidDrop = Liquid.lava;
			liquid = true;
		}
		
		@Override
		public void update(Tile tile){
			if(Mathf.chance(0.001 * Timers.delta())){
				Effects.effect(Fx.lava, tile.worldx() + Mathf.range(5f), tile.worldy() + Mathf.range(5f));
			}
			
			if(Mathf.chance(0.002 * Timers.delta())){
				Effects.effect(Fx.lavabubble, tile.worldx() + Mathf.range(3f), tile.worldy() + Mathf.range(3f));
			}
		}
	},
	
	oil = new Floor("oil"){
		{
			variants = 0;
			solid = true;
			liquidDrop = Liquid.oil;
			liquid = true;
		}
		
		@Override
		public void update(Tile tile){
			if(Mathf.chance(0.0022 * Timers.delta())){
				Effects.effect(Fx.oilbubble, tile.worldx() + Mathf.range(2f), tile.worldy() + Mathf.range(2f));
			}
		}
	},
	
	stone = new Floor("stone"){{
		blends = block -> block != this && !(block instanceof Ore);
	}},
	
	blackstone = new Floor("blackstone"){},
	
	iron = new Ore("iron"){},
	
	coal = new Ore("coal"){{
		drops = new ItemStack(Item.igpu, 1);
	}},
	
	titanium = new Ore("titanium"){},
	
	uranium = new Ore("uranium"){{
		drops = new ItemStack(Item.pcie, 1);
	}},
	
	dirt = new Floor("dirt"){},
	
	sand = new Floor("sand"){},
	
	ice = new Floor("ice"){},
	
	snow = new Floor("snow"){},
	
	grass = new Floor("grass"){},
	
	sandblock = new StaticBlock("sandblock"){{
		solid = true;
		variants = 3;
	}},
	
	snowblock = new StaticBlock("snowblock"){{
		solid = true;
		variants = 3;
	}},
	
	stoneblock = new StaticBlock("stoneblock"){{
		solid = true;
		variants = 3;
	}},
	
	blackstoneblock = new StaticBlock("blackstoneblock"){{
		solid = true;
		variants = 3;
	}},
	
	grassblock = new StaticBlock("grassblock"){{
		solid = true;
		variants = 2;
	}},
					
	mossblock = new StaticBlock("mossblock"){{
		solid = true;
	}},
	
	shrub = new Rock("shrub"){

	},
	
	rock = new Rock("rock"){{
		variants = 2;
		varyShadow = true;
	}},
	
	icerock = new Rock("icerock"){{
		variants = 2;
		varyShadow = true;
	}},
	
	blackrock = new Rock("blackrock"){{
		variants = 1;
		varyShadow = true;
	}},
	
	dirtblock = new StaticBlock("dirtblock"){{
		solid = true;
	}};
}
