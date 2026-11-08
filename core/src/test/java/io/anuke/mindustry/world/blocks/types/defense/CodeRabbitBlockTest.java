package io.anuke.mindustry.world.blocks.types.defense;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.I18NBundle;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.resource.CodeItem;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Recipe;
import io.anuke.mindustry.resource.Recipes;
import io.anuke.mindustry.resource.Section;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.WeaponBlocks;
import io.anuke.mindustry.world.blocks.types.PowerAcceptor;
import io.anuke.mindustry.world.blocks.types.defense.CodeRabbitBlock.CodeRabbitEntity;
import io.anuke.mindustry.world.blocks.types.production.GateLogic;
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

/** range などの protected な設定値を読むため、CodeRabbitBlock と同じパッケージに置く */
public class CodeRabbitBlockTest {

	/** ブロックの定義は名前を Core.bundle から引くので、テストの前に英語の文言を読み込む(作業ディレクトリは core) */
	@BeforeClass
	public static void loadBundle(){
		I18NBundle.setExceptionOnMissingKey(false);
		Core.bundle = I18NBundle.createBundle(new FileHandle("assets/bundles/bundle"), Locale.ROOT);
	}

	private static CodeRabbitBlock block(){
		return (CodeRabbitBlock)WeaponBlocks.coderabbit;
	}

	/** ワールドに登録せず、エンティティだけを持つタイルを作る */
	private static Tile rabbitTile(){
		Tile tile = new Tile(0, 0);
		CodeRabbitEntity entity = (CodeRabbitEntity)block().getEntity();
		entity.items = new int[Item.getAllItems().size];
		tile.entity = entity;
		return tile;
	}

	@Test
	public void definitionMatchesDesign(){
		CodeRabbitBlock rabbit = block();
		assertEquals("coderabbit", rabbit.name);
		assertEquals(60f, rabbit.range, 0f);
		assertEquals(4f, rabbit.reload, 0f);
		assertEquals(0.2f, rabbit.powerUsed, 0f);
		assertEquals(110, rabbit.health);
		assertSame(BulletType.coderabbitShot, rabbit.bullet);
		assertEquals(10, rabbit.bullet.damage);
		assertEquals(CodeItem.REVIEW, rabbit.passedBit);
		assertEquals(2, rabbit.stages);
		assertEquals(3f, rabbit.processTime, 0f);
		assertEquals(0.6f, rabbit.powerPerItem, 0f);
	}

	@Test
	public void isPowerTurretAndEntityIsGateState(){
		assertTrue(block() instanceof PowerAcceptor);
		assertTrue(block() instanceof PowerTurret);
		assertTrue(block().getEntity() instanceof GateLogic.GateState);
	}

	/** Tile はブロック ID を byte で持つので、ID が 127 を超えると置けなくなる */
	@Test
	public void idFitsInTileByte(){
		assertSame(block(), Block.getByID((byte)block().id));
		assertTrue(Block.getAllBlocks().size <= 127);
	}

	@Test
	public void recipeIsInCraftingAndCosts100(){
		Recipe recipe = Recipes.getByResult(block());
		assertNotNull(recipe);
		assertSame(Section.crafting, recipe.section);
		assertEquals(1, recipe.requirements.length);
		assertSame(Item.credit, recipe.requirements[0].item);
		assertEquals(100, recipe.requirements[0].amount);
	}

	@Test
	public void takesOneCodeAtATime(){
		CodeRabbitBlock rabbit = block();
		Tile tile = rabbitTile();
		Tile conveyor = new Tile(1, 0);
		Item code = CodeItem.get(1, 0, 1);

		assertFalse(rabbit.acceptItem(Item.igpu, tile, conveyor));
		assertFalse(rabbit.acceptItem(Item.credit, tile, conveyor));
		assertTrue(rabbit.acceptItem(code, tile, conveyor));

		rabbit.handleItem(code, tile, conveyor);
		CodeRabbitEntity entity = tile.entity();
		assertSame(code, entity.current);
		assertEquals(0, entity.totalItems());
		assertFalse(rabbit.acceptItem(CodeItem.get(2, 0, 1), tile, conveyor));
	}

	@Test
	public void unsentOutputIsKeptAndBlocksInput(){
		CodeRabbitBlock rabbit = block();
		Tile tile = rabbitTile();
		Tile conveyor = new Tile(1, 0);
		Item done = CodeItem.get(3, CodeItem.REVIEW, 1);

		//offloadNear が隣に出せなかったときは、自分自身を source にして handleItem を呼ぶ
		rabbit.handleItem(done, tile, tile);
		CodeRabbitEntity entity = tile.entity();
		assertNull(entity.current);
		assertEquals(1, entity.getItem(done));
		assertFalse(rabbit.acceptItem(CodeItem.get(1, 0, 1), tile, conveyor));
	}

	/** 3 秒(180 フレーム)で REVIEW を通し品質を 2 段階上げ、電力 0.6 を使う。電力が 0.6 未満なら進まない */
	@Test
	public void reviewRaisesQualityByTwoUsingSharedPower(){
		CodeRabbitBlock rabbit = block();
		CodeRabbitEntity entity = new CodeRabbitEntity();
		Item code = CodeItem.get(1, 0, 1);

		entity.current = code;
		entity.power = 0.5f;
		assertNull(GateLogic.update(entity, rabbit.passedBit, rabbit.stages, rabbit.processTime, rabbit.powerPerItem, 180f));
		assertEquals(0f, entity.progress, 0f);

		entity.power = 1f;
		Item result = GateLogic.update(entity, rabbit.passedBit, rabbit.stages, rabbit.processTime, rabbit.powerPerItem, 180f);
		assertSame(CodeItem.advance(code, CodeItem.REVIEW, 2), result);
		assertEquals(0.4f, entity.power, 0.0001f);
		assertNull(entity.current);
	}

	@Test
	public void entitySavesPowerCurrentAndProgress() throws IOException{
		CodeRabbitEntity saved = new CodeRabbitEntity();
		saved.power = 5.5f;
		saved.current = CodeItem.get(2, CodeItem.TEST, 1);
		saved.progress = 0.75f;

		CodeRabbitEntity loaded = roundTrip(saved);
		assertEquals(5.5f, loaded.power, 0f);
		assertSame(saved.current, loaded.current);
		assertEquals(0.75f, loaded.progress, 0f);
	}

	@Test
	public void entitySavesEmptyTurret() throws IOException{
		CodeRabbitEntity saved = new CodeRabbitEntity();
		saved.power = 1f;

		CodeRabbitEntity loaded = roundTrip(saved);
		assertNull(loaded.current);
		assertEquals(0f, loaded.progress, 0f);
		assertEquals(1f, loaded.power, 0f);
	}

	private static CodeRabbitEntity roundTrip(CodeRabbitEntity saved) throws IOException{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		saved.write(new DataOutputStream(bytes));
		assertEquals(4 + 1 + 4, bytes.size());

		CodeRabbitEntity loaded = new CodeRabbitEntity();
		loaded.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
		return loaded;
	}
}
