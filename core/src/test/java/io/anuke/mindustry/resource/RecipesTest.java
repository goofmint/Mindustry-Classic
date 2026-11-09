// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.resource;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.I18NBundle;
import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.mindustry.world.Block;
import io.anuke.ucore.core.Core;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertArrayEquals;
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
	public void recipeCountIs57(){
		assertEquals(57, Recipes.all().size);
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

	/** 配置メニューのタブ順は Section の宣言順で決まる */
	@Test
	public void sectionOrderStartsWithProduction(){
		Section[] expected = {Section.production, Section.crafting, Section.distribution,
				Section.power, Section.weapon, Section.defense};
		assertArrayEquals(expected, Section.values());
	}

	/** どのタブも空にしない。モバイル(desktopOnly を除く)でも同じ */
	@Test
	public void everySectionHasRecipe(){
		for(Section section : Section.values()){
			int all = 0, withoutDesktopOnly = 0;
			for(Recipe recipe : Recipes.all()){
				if(recipe.section != section) continue;
				all++;
				if(!recipe.desktopOnly) withoutDesktopOnly++;
			}
			assertTrue(section.name(), all > 0);
			assertTrue(section.name(), withoutDesktopOnly > 0);
		}
	}

	@Test
	public void upgradeRecipesAreEmpty(){
		assertTrue(!UpgradeRecipes.getAllRecipes().hasNext());
		assertEquals(0, UpgradeRecipes.get(Weapon.triblaster).length);
	}
}
