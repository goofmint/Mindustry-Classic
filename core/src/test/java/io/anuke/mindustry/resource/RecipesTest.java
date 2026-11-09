package io.anuke.mindustry.resource;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.I18NBundle;
import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.mindustry.world.Block;
import io.anuke.ucore.core.Core;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class RecipesTest {

	/** ブロックと武器の定義は名前を Core.bundle から引くので、テストの前に英語の文言を読み込む(作業ディレクトリは core) */
	@BeforeClass
	public static void loadBundle(){
		I18NBundle.setExceptionOnMissingKey(false);
		Core.bundle = I18NBundle.createBundle(new FileHandle("assets/bundles/bundle"), Locale.ROOT);
	}

	@Test
	public void recipeCountIs47(){
		assertEquals(47, Recipes.all().size);
	}

	@Test
	public void everyRecipeCostsOnlyCredit(){
		for(Recipe recipe : Recipes.all()){
			assertEquals(recipe.result.name, 1, recipe.requirements.length);
			assertSame(recipe.result.name, Item.credit, recipe.requirements[0].item);
			assertTrue(recipe.result.name, recipe.requirements[0].amount > 0);
		}
	}

	@Test
	public void noResultBlockAppearsTwice(){
		ObjectSet<Block> seen = new ObjectSet<>();
		for(Recipe recipe : Recipes.all()){
			assertTrue(recipe.result.name, seen.add(recipe.result));
		}
	}

	@Test
	public void upgradeRecipesAreEmpty(){
		assertTrue(!UpgradeRecipes.getAllRecipes().hasNext());
		assertEquals(0, UpgradeRecipes.get(Weapon.triblaster).length);
	}
}
