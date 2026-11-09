// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.I18NBundle;
import io.anuke.mindustry.resource.CodeItem;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Recipe;
import io.anuke.mindustry.resource.Recipes;
import io.anuke.mindustry.resource.Section;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.mindustry.world.blocks.types.PowerAcceptor;
import io.anuke.ucore.core.Core;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** stageChoices などの protected な設定値を読むため、CodeSource と同じパッケージに置く */
public class CodeSourceTest {

	/** ブロックの定義は名前を Core.bundle から引くので、テストの前に英語の文言を読み込む(作業ディレクトリは core) */
	@BeforeClass
	public static void loadBundle(){
		I18NBundle.setExceptionOnMissingKey(false);
		Core.bundle = I18NBundle.createBundle(new FileHandle("assets/bundles/bundle"), Locale.ROOT);
	}

	private static final Block[] SOURCES = {
		ProductionBlocks.humanEngineer, ProductionBlocks.aiAgent1, ProductionBlocks.aiAgent2, ProductionBlocks.aiAgent3
	};

	@Test
	public void definitionsMatchDesign(){
		CodeSource human = (CodeSource)ProductionBlocks.humanEngineer;
		PoweredCodeSource ai1 = (PoweredCodeSource)ProductionBlocks.aiAgent1;
		PoweredCodeSource ai2 = (PoweredCodeSource)ProductionBlocks.aiAgent2;
		PoweredCodeSource ai3 = (PoweredCodeSource)ProductionBlocks.aiAgent3;

		assertEquals("human-engineer", human.name);
		assertEquals("ai-agent-1", ai1.name);
		assertEquals("ai-agent-2", ai2.name);
		assertEquals("ai-agent-3", ai3.name);

		assertArrayEquals(new int[]{2, 3}, human.stageChoices);
		assertArrayEquals(new int[]{0, 1}, ai1.stageChoices);
		assertArrayEquals(new int[]{1, 3}, ai2.stageChoices);
		assertArrayEquals(new int[]{3}, ai3.stageChoices);

		assertEquals(6f, human.time, 0f);
		assertEquals(2f, ai1.time, 0f);
		assertEquals(2f, ai2.time, 0f);
		assertEquals(2f, ai3.time, 0f);

		assertEquals(0.3f, ai1.powerPerItem, 0f);
		assertEquals(0.4f, ai2.powerPerItem, 0f);
		assertEquals(0.5f, ai3.powerPerItem, 0f);
	}

	@Test
	public void onlyAiAgentsAcceptPower(){
		assertFalse(ProductionBlocks.humanEngineer instanceof PowerAcceptor);
		assertTrue(ProductionBlocks.aiAgent1 instanceof PowerAcceptor);
		assertTrue(ProductionBlocks.aiAgent2 instanceof PowerAcceptor);
		assertTrue(ProductionBlocks.aiAgent3 instanceof PowerAcceptor);
	}

	@Test
	public void everyStageChoiceGivesRegisteredCode(){
		for(Block block : SOURCES){
			for(int stage : ((CodeSource)block).stageChoices){
				for(int importance : CodeItem.IMPORTANCE){
					Item item = CodeItem.get(stage, 0, importance);
					assertNotNull(block.name, item);
					assertNotNull(block.name, CodeItem.attrOf(item));
				}
			}
		}
	}

	@Test
	public void recipesCostCredit(){
		int[] costs = {50, 20, 30, 40};
		for(int i = 0; i < SOURCES.length; i++){
			Recipe recipe = Recipes.getByResult(SOURCES[i]);
			assertNotNull(SOURCES[i].name, recipe);
			assertSame(Section.production, recipe.section);
			assertEquals(1, recipe.requirements.length);
			assertSame(Item.credit, recipe.requirements[0].item);
			assertEquals(costs[i], recipe.requirements[0].amount);
		}
	}

	@Test
	public void codeSourcesReplaceEachOther(){
		for(Block a : SOURCES){
			for(Block b : SOURCES){
				assertTrue(a.name + " <- " + b.name, a.canReplace(b));
			}
			assertFalse(a.canReplace(ProductionBlocks.coaldrill));
		}
	}

	/** Tile はブロック ID を byte で持つので、末尾に足した ID が 127 を超えると置けなくなる */
	@Test
	public void idsFitInTileByte(){
		for(Block block : SOURCES){
			assertSame(block.name, block, Block.getByID((byte)block.id));
		}
	}

	@Test
	public void veinIsIronOreFloorOnly(){
		CodeSource human = (CodeSource)ProductionBlocks.humanEngineer;
		Tile vein = new Tile(0, 0, Blocks.iron);
		Tile other = new Tile(0, 0, Blocks.stone);

		assertTrue(human.isVein(vein));
		assertFalse(human.isLayer(vein));
		assertFalse(human.isVein(other));
		assertTrue(human.isLayer(other));
	}
}
