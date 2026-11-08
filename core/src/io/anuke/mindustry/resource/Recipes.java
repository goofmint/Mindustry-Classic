package io.anuke.mindustry.resource;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.DefenseBlocks;
import io.anuke.mindustry.world.blocks.DistributionBlocks;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.mindustry.world.blocks.WeaponBlocks;

import static io.anuke.mindustry.resource.Section.*;

public class Recipes {
	private static final Array<Recipe> list = Array.with(
			new Recipe(defense, DefenseBlocks.stonewall, stack(Item.credit, 12)),
			new Recipe(defense, DefenseBlocks.ironwall, stack(Item.credit, 12)),
			new Recipe(defense, DefenseBlocks.titaniumwall, stack(Item.credit, 12)),
			new Recipe(defense, DefenseBlocks.steelwalllarge, stack(Item.credit, 48)),
			new Recipe(defense, DefenseBlocks.titaniumwalllarge, stack(Item.credit, 48)),
			new Recipe(defense, DefenseBlocks.door, stack(Item.credit, 15)).setDesktop(),
			new Recipe(defense, DefenseBlocks.largedoor, stack(Item.credit, 60)).setDesktop(),
			new Recipe(defense, DefenseBlocks.titaniumshieldwall, stack(Item.credit, 16)),

			new Recipe(distribution, DistributionBlocks.conveyor, stack(Item.credit, 1)),
			new Recipe(distribution, DistributionBlocks.steelconveyor, stack(Item.credit, 1)),
			new Recipe(distribution, DistributionBlocks.pulseconveyor, stack(Item.credit, 1)),
			new Recipe(distribution, DistributionBlocks.router, stack(Item.credit, 2)),
			new Recipe(distribution, DistributionBlocks.junction, stack(Item.credit, 2)),
			new Recipe(distribution, DistributionBlocks.tunnel, stack(Item.credit, 2)),
			new Recipe(distribution, DistributionBlocks.conduit, stack(Item.credit, 1)),
			new Recipe(distribution, DistributionBlocks.pulseconduit, stack(Item.credit, 2)),
			new Recipe(distribution, DistributionBlocks.liquidrouter, stack(Item.credit, 2)),
			new Recipe(distribution, DistributionBlocks.liquidjunction, stack(Item.credit, 2)),

			new Recipe(weapon, WeaponBlocks.turret, stack(Item.credit, 4)),
			new Recipe(weapon, WeaponBlocks.doubleturret, stack(Item.credit, 7)),
			new Recipe(weapon, WeaponBlocks.machineturret, stack(Item.credit, 18)),
			new Recipe(weapon, WeaponBlocks.shotgunturret, stack(Item.credit, 20)),
			new Recipe(weapon, WeaponBlocks.flameturret, stack(Item.credit, 21)),
			new Recipe(weapon, WeaponBlocks.sniperturret, stack(Item.credit, 25)),
			new Recipe(weapon, WeaponBlocks.laserturret, stack(Item.credit, 24)),
			new Recipe(weapon, WeaponBlocks.mortarturret, stack(Item.credit, 40)),
			new Recipe(weapon, WeaponBlocks.teslaturret, stack(Item.credit, 60)),
			new Recipe(weapon, WeaponBlocks.plasmaturret, stack(Item.credit, 45)),
			new Recipe(weapon, WeaponBlocks.chainturret, stack(Item.credit, 115)),
			new Recipe(weapon, WeaponBlocks.titanturret, stack(Item.credit, 175)),
			new Recipe(weapon, WeaponBlocks.coderabbit, stack(Item.credit, 100)),

			new Recipe(production, ProductionBlocks.coaldrill, stack(Item.credit, 65)),
			new Recipe(production, ProductionBlocks.uraniumdrill, stack(Item.credit, 80)),
			new Recipe(production, ProductionBlocks.humanEngineer, stack(Item.credit, 50)),
			new Recipe(production, ProductionBlocks.aiAgent1, stack(Item.credit, 20)),
			new Recipe(production, ProductionBlocks.aiAgent2, stack(Item.credit, 30)),
			new Recipe(production, ProductionBlocks.aiAgent3, stack(Item.credit, 40)),

			new Recipe(crafting, ProductionBlocks.seniorEngineer, stack(Item.credit, 80)),
			new Recipe(crafting, ProductionBlocks.unitTest, stack(Item.credit, 30)),
			new Recipe(crafting, ProductionBlocks.e2eTest, stack(Item.credit, 45)),
			new Recipe(crafting, ProductionBlocks.jenkins, stack(Item.credit, 60)),
			new Recipe(crafting, ProductionBlocks.githubActions, stack(Item.credit, 80)),

			new Recipe(power, ProductionBlocks.coalgenerator, stack(Item.credit, 50)),
			new Recipe(power, ProductionBlocks.thermalgenerator, stack(Item.credit, 60)),
			new Recipe(power, ProductionBlocks.combustiongenerator, stack(Item.credit, 50)),
			new Recipe(power, ProductionBlocks.rtgenerator, stack(Item.credit, 40)),
			new Recipe(power, ProductionBlocks.nuclearReactor, stack(Item.credit, 130)),
			new Recipe(power, DistributionBlocks.powerbooster, stack(Item.credit, 16)),
			new Recipe(power, DistributionBlocks.powerlaser, stack(Item.credit, 6)),
			new Recipe(power, DistributionBlocks.powerlasercorner, stack(Item.credit, 8)),
			new Recipe(power, DistributionBlocks.powerlaserrouter, stack(Item.credit, 10)),

			new Recipe(power, DefenseBlocks.shieldgenerator, stack(Item.credit, 60)),

			new Recipe(distribution, DistributionBlocks.teleporter, stack(Item.credit, 70)),

			new Recipe(power, DefenseBlocks.repairturret, stack(Item.credit, 30)),
			new Recipe(power, DefenseBlocks.megarepairturret, stack(Item.credit, 50)),

			new Recipe(production, ProductionBlocks.pump, stack(Item.credit, 10)),
			new Recipe(production, ProductionBlocks.fluxpump, stack(Item.credit, 15))
	);
	
	private static ItemStack stack(Item item, int amount){
		return new ItemStack(item, amount);
	}

	public static Array<Recipe> all(){
		return list;
	}

	public static Recipe getByResult(Block block){
		for(Recipe recipe : list){
			if(recipe.result == block){
				return recipe;
			}
		}
		return null;
	}
	
	public static Array<Recipe> getBy(Section section, Array<Recipe> r){
		for(Recipe recipe : list){
			if(recipe.section == section && !(Vars.mobile && recipe.desktopOnly))
				r.add(recipe);
		}
		
		return r;
	}
}
