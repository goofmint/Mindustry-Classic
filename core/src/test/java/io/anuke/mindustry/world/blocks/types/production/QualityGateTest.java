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
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.mindustry.world.blocks.types.production.QualityGate.GateEntity;
import io.anuke.ucore.core.Core;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** passedBit などの protected な設定値を読むため、QualityGate と同じパッケージに置く */
public class QualityGateTest {

	/** ブロックの定義は名前を Core.bundle から引くので、テストの前に英語の文言を読み込む(作業ディレクトリは core) */
	@BeforeClass
	public static void loadBundle(){
		I18NBundle.setExceptionOnMissingKey(false);
		Core.bundle = I18NBundle.createBundle(new FileHandle("assets/bundles/bundle"), Locale.ROOT);
	}

	private static final Block[] GATES = {
		ProductionBlocks.seniorEngineer, ProductionBlocks.unitTest, ProductionBlocks.e2eTest,
		ProductionBlocks.jenkins, ProductionBlocks.githubActions
	};
	private static final String[] NAMES = {"senior-engineer", "unit-test", "e2e-test", "jenkins", "github-actions"};
	private static final int[] BITS = {CodeItem.REVIEW, CodeItem.TEST, CodeItem.TEST, CodeItem.CI, CodeItem.CI};
	private static final float[] TIMES = {5f, 2f, 4f, 3f, 2f};
	private static final float[] POWERS = {0f, 0.3f, 0.5f, 0.4f, 0.4f};
	private static final int[] COSTS = {80, 30, 45, 60, 80};

	/** ワールドに登録せず、エンティティだけを持つタイルを作る */
	private static Tile gateTile(QualityGate gate){
		Tile tile = new Tile(0, 0);
		GateEntity entity = (GateEntity)gate.getEntity();
		entity.items = new int[Item.getAllItems().size];
		tile.entity = entity;
		return tile;
	}

	@Test
	public void definitionsMatchDesign(){
		for(int i = 0; i < GATES.length; i++){
			QualityGate gate = (QualityGate)GATES[i];
			assertEquals(NAMES[i], gate.name);
			assertEquals(gate.name, BITS[i], gate.passedBit);
			assertEquals(gate.name, 1, gate.stages);
			assertEquals(gate.name, TIMES[i], gate.processTime, 0f);
			assertEquals(gate.name, POWERS[i], gate.powerPerItem, 0f);
			assertEquals(gate.name, 1, gate.width);
			assertEquals(gate.name, 1, gate.height);
		}
	}

	@Test
	public void recipesAreInCraftingAndCostCredit(){
		for(int i = 0; i < GATES.length; i++){
			Recipe recipe = Recipes.getByResult(GATES[i]);
			assertNotNull(GATES[i].name, recipe);
			assertSame(Section.crafting, recipe.section);
			assertEquals(1, recipe.requirements.length);
			assertSame(Item.credit, recipe.requirements[0].item);
			assertEquals(COSTS[i], recipe.requirements[0].amount);
		}
	}

	/** Tile はブロック ID を byte で持つので、末尾に足した ID が 127 を超えると置けなくなる */
	@Test
	public void idsFitInTileByte(){
		for(Block block : GATES){
			assertSame(block.name, block, Block.getByID((byte)block.id));
		}
		assertTrue(Block.getAllBlocks().size <= 127);
	}

	@Test
	public void takesOneCodeAtATime(){
		QualityGate gate = (QualityGate)ProductionBlocks.unitTest;
		Tile tile = gateTile(gate);
		Tile conveyor = new Tile(1, 0);
		Item code = CodeItem.get(1, 0, 1);

		assertFalse(gate.acceptItem(Item.coal, tile, conveyor));
		assertTrue(gate.acceptItem(code, tile, conveyor));

		gate.handleItem(code, tile, conveyor);
		GateEntity entity = tile.entity();
		assertSame(code, entity.current);
		assertEquals(0, entity.totalItems());
		assertFalse(gate.acceptItem(CodeItem.get(2, 0, 1), tile, conveyor));
	}

	@Test
	public void unsentOutputIsKeptAndBlocksInput(){
		QualityGate gate = (QualityGate)ProductionBlocks.unitTest;
		Tile tile = gateTile(gate);
		Tile conveyor = new Tile(1, 0);
		Item done = CodeItem.get(2, CodeItem.TEST, 1);

		//offloadNear が隣に出せなかったときは、自分自身を source にして handleItem を呼ぶ
		gate.handleItem(done, tile, tile);
		GateEntity entity = tile.entity();
		assertNull(entity.current);
		assertEquals(1, entity.getItem(done));
		assertFalse(gate.acceptItem(CodeItem.get(1, 0, 1), tile, conveyor));
	}

	@Test
	public void onlyPoweredGatesAcceptPower(){
		QualityGate senior = (QualityGate)ProductionBlocks.seniorEngineer;
		QualityGate unit = (QualityGate)ProductionBlocks.unitTest;

		assertFalse(senior.acceptsPower(gateTile(senior)));
		assertTrue(unit.acceptsPower(gateTile(unit)));
	}

	@Test
	public void entitySavesCurrentAndProgress() throws IOException{
		GateEntity saved = new GateEntity();
		saved.power = 3.5f;
		saved.current = CodeItem.get(3, CodeItem.CI, 2);
		saved.progress = 0.25f;

		GateEntity loaded = roundTrip(saved);
		assertEquals(3.5f, loaded.power, 0f);
		assertSame(saved.current, loaded.current);
		assertEquals(0.25f, loaded.progress, 0f);
	}

	@Test
	public void entitySavesEmptyGate() throws IOException{
		GateEntity saved = new GateEntity();
		saved.power = 1f;

		GateEntity loaded = roundTrip(saved);
		assertNull(loaded.current);
		assertEquals(0f, loaded.progress, 0f);
		assertEquals(1f, loaded.power, 0f);
	}

	private static GateEntity roundTrip(GateEntity saved) throws IOException{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		saved.write(new DataOutputStream(bytes));
		assertEquals(4 + 1 + 4, bytes.size());

		GateEntity loaded = new GateEntity();
		loaded.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
		return loaded;
	}
}
