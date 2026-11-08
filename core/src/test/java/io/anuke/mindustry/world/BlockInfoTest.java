package io.anuke.mindustry.world;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.I18NBundle;
import io.anuke.mindustry.resource.Recipe;
import io.anuke.mindustry.resource.Recipes;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.mindustry.world.blocks.WeaponBlocks;
import io.anuke.ucore.core.Core;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** ブロック情報ダイアログ(BlocksFragment.showBlockInfo())に出す統計行と詳細説明の検査。対象は配置メニューの全ブロックと core */
public class BlockInfoTest {
	private static I18NBundle english, japanese;

	/** ブロックの定義は名前を Core.bundle から引くので、テストの前に英語の文言を読み込む(作業ディレクトリは core) */
	@BeforeClass
	public static void loadBundles(){
		I18NBundle.setExceptionOnMissingKey(false);
		english = I18NBundle.createBundle(new FileHandle("assets/bundles/bundle"), Locale.ROOT);
		japanese = I18NBundle.createBundle(new FileHandle("assets/bundles/bundle"), new Locale("ja"));
		Core.bundle = english;
	}

	@After
	public void restoreEnglish(){
		Core.bundle = english;
	}

	private static Array<Block> targets(){
		Array<Block> blocks = new Array<>();
		for(Recipe recipe : Recipes.all()){
			blocks.add(recipe.result);
		}
		blocks.add(ProductionBlocks.core);
		return blocks;
	}

	/** showBlockInfo() と同じ規則で、見出し(最初の : まで)を text.blocks.* で訳す */
	private static String translate(String line){
		if(!line.contains(":")) return line;
		String color = line.substring(0, line.indexOf("]") + 1);
		String first = line.substring(color.length(), line.indexOf(":")).replace("/", "").replace(" ", "").toLowerCase();
		return color + Core.bundle.get("text.blocks." + first) + line.substring(line.indexOf(":"));
	}

	private static Array<String> stats(Block block){
		Array<String> list = new Array<>();
		block.getStats(list);
		return list;
	}

	@Test
	public void targetsAre57RecipesAndCore(){
		assertEquals(58, targets().size);
	}

	@Test
	public void statLinesHaveTranslationsInBothLanguages(){
		for(I18NBundle bundle : new I18NBundle[]{english, japanese}){
			Core.bundle = bundle;
			for(Block block : targets()){
				for(String line : stats(block)){
					String shown = translate(line);
					assertFalse(block.name + ": " + shown, shown.contains("???"));
				}
			}
		}
	}

	/** 日本語では、色タグ、サイズの「1x1」、英字の固有名(アイテム名、液体名、CI)を除くと、英字が残らない */
	@Test
	public void japaneseStatLinesHaveNoEnglish(){
		Core.bundle = japanese;
		for(Block block : targets()){
			for(String line : stats(block)){
				String shown = translate(line).replaceAll("\\[[^\\]]*\\]", "").replaceAll("\\d+x\\d+", "")
						.replace("iGPU", "").replace("PCIe", "").replace("dGPU", "").replace("vGPU", "").replace("CI", "");
				assertFalse(block.name + ": " + shown, shown.matches(".*[A-Za-z].*"));
			}
		}
	}

	@Test
	public void seniorEngineerHasNoPowerLine(){
		for(String line : stats(ProductionBlocks.seniorEngineer)){
			assertFalse(line, line.startsWith("[powerinfo]"));
		}
	}

	@Test
	public void codeRabbitHasOnePowerCapacityAndGateStats(){
		int capacity = 0;
		boolean category = false, gain = false, seconds = false, powerItem = false;
		for(String line : stats(WeaponBlocks.coderabbit)){
			if(line.startsWith("[powerinfo]Power Capacity: ")) capacity++;
			if(line.equals("[craftinfo]Category: " + english.get("text.code.category.review"))) category = true;
			if(line.equals("[craftinfo]Quality Gain: 2")) gain = true;
			if(line.startsWith("[craftinfo]Seconds/item: ")) seconds = true;
			if(line.startsWith("[powerinfo]Power/item: ")) powerItem = true;
		}
		assertEquals(1, capacity);
		assertTrue(category && gain && seconds && powerItem);
	}

	@Test
	public void codeSourceShowsStartQualityAndImportance(){
		Array<String> human = stats(ProductionBlocks.humanEngineer);
		assertTrue(human.toString(), human.contains("[iteminfo]Start Quality: -2, -1", false));
		assertTrue(human.toString(), human.contains("[iteminfo]Importance: 1, 2", false));
		assertTrue(human.toString(), human.contains("[iteminfo]" + english.get("text.blocks.codevein"), false));

		Array<String> agent = stats(ProductionBlocks.aiAgent1);
		assertTrue(agent.toString(), agent.contains("[iteminfo]Start Quality: -5, -3", false));
	}
}
