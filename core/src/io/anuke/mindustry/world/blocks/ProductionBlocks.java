package io.anuke.mindustry.world.blocks;

import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.resource.CodeItem;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.defense.CoreBlock;
import io.anuke.mindustry.world.blocks.types.production.*;

public class ProductionBlocks{
	public static final Block
	
	core = new CoreBlock("core"){},
	
	pump = new Pump("pump"){},
	
	fluxpump = new Pump("fluxpump"){
		{
			pumpAmount = 3f;
		}
	},
	
	coaldrill = new Drill("coaldrill"){
		{
			resource = Blocks.coal;
			result = Item.igpu;
			time = 6;
		}
	},
	
	uraniumdrill = new Drill("uraniumdrill"){
		{
			resource = Blocks.uranium;
			result = Item.pcie;
			time = 7;
		}
	},
	
	coalgenerator = new ItemPowerGenerator("coalgenerator"){
		{
			generateItem = Item.igpu;
			powerOutput = 0.04f;
			powerCapacity = 40f;
		}
	},
	thermalgenerator = new LiquidPowerGenerator("thermalgenerator"){
		{
			generateLiquid = Liquid.lava;
			maxLiquidGenerate = 0.5f;
			powerPerLiquid = 0.08f;
			powerCapacity = 40f;
			generateEffect = Fx.redgeneratespark;
		}
	},
	combustiongenerator = new LiquidPowerGenerator("combustiongenerator"){
		{
			generateLiquid = Liquid.oil;
			maxLiquidGenerate = 0.4f;
			powerPerLiquid = 0.12f;
			powerCapacity = 40f;
		}
	},
	rtgenerator = new ItemPowerGenerator("rtgenerator"){
		{
			generateItem = Item.pcie;
			powerCapacity = 40f;
			powerOutput = 0.03f;
			itemDuration = 240f;
		}
	},
	nuclearReactor = new NuclearReactor("nuclearreactor"){
		{
			width = 3;
			height = 3;
			health = 600;
			breaktime *= 2.3f;
		}
	},
	humanEngineer = new CodeSource("human-engineer"){
		{
			stageChoices = new int[]{2, 3};
			time = 6;
		}
	},
	aiAgent1 = new PoweredCodeSource("ai-agent-1"){
		{
			stageChoices = new int[]{0, 1};
			time = 2;
			powerPerItem = 0.3f;
		}
	},
	aiAgent2 = new PoweredCodeSource("ai-agent-2"){
		{
			stageChoices = new int[]{1, 3};
			time = 2;
			powerPerItem = 0.4f;
		}
	},
	aiAgent3 = new PoweredCodeSource("ai-agent-3"){
		{
			stageChoices = new int[]{3};
			time = 2;
			powerPerItem = 0.5f;
		}
	},
	seniorEngineer = new QualityGate("senior-engineer"){
		{
			passedBit = CodeItem.REVIEW;
			stages = 1;
			processTime = 5;
		}
	},
	unitTest = new PoweredQualityGate("unit-test"){
		{
			passedBit = CodeItem.TEST;
			stages = 1;
			processTime = 2;
			powerPerItem = 0.3f;
		}
	},
	e2eTest = new PoweredQualityGate("e2e-test"){
		{
			passedBit = CodeItem.TEST;
			stages = 1;
			processTime = 4;
			powerPerItem = 0.5f;
		}
	},
	jenkins = new PoweredQualityGate("jenkins"){
		{
			passedBit = CodeItem.CI;
			stages = 1;
			processTime = 3;
			powerPerItem = 0.4f;
		}
	},
	githubActions = new PoweredQualityGate("github-actions"){
		{
			passedBit = CodeItem.CI;
			stages = 1;
			processTime = 2;
			powerPerItem = 0.4f;
		}
	},
	humanEngineer = new CodeSource("human-engineer"){
		{
			stageChoices = new int[]{2, 3};
			time = 6;
		}
	},
	aiAgent1 = new PoweredCodeSource("ai-agent-1"){
		{
			stageChoices = new int[]{0, 1};
			time = 2;
			powerPerItem = 0.3f;
		}
	},
	aiAgent2 = new PoweredCodeSource("ai-agent-2"){
		{
			stageChoices = new int[]{1, 3};
			time = 2;
			powerPerItem = 0.4f;
		}
	},
	aiAgent3 = new PoweredCodeSource("ai-agent-3"){
		{
			stageChoices = new int[]{3};
			time = 2;
			powerPerItem = 0.5f;
		}
	},
	seniorEngineer = new QualityGate("senior-engineer"){
		{
			passedBit = CodeItem.REVIEW;
			stages = 1;
			processTime = 5;
		}
	},
	unitTest = new PoweredQualityGate("unit-test"){
		{
			passedBit = CodeItem.TEST;
			stages = 1;
			processTime = 2;
			powerPerItem = 0.3f;
		}
	},
	e2eTest = new PoweredQualityGate("e2e-test"){
		{
			passedBit = CodeItem.TEST;
			stages = 1;
			processTime = 4;
			powerPerItem = 0.5f;
		}
	},
	jenkins = new PoweredQualityGate("jenkins"){
		{
			passedBit = CodeItem.CI;
			stages = 1;
			processTime = 3;
			powerPerItem = 0.4f;
		}
	},
	githubActions = new PoweredQualityGate("github-actions"){
		{
			passedBit = CodeItem.CI;
			stages = 1;
			processTime = 2;
			powerPerItem = 0.4f;
		}
	};
}
