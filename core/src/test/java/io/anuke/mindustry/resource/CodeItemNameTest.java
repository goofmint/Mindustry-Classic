package io.anuke.mindustry.resource;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.I18NBundle;
import io.anuke.mindustry.resource.CodeItem.CodeAttr;
import io.anuke.ucore.core.Core;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/** コードアイテムの表示名(基本名 + 通過状態の接尾辞) */
public class CodeItemNameTest{
	private static I18NBundle english, japanese;
	private static final String BASE = "item.code.q4.i2.name";
	private static final String REVIEW = "text.code.passed.review", TEST = "text.code.passed.test", CI = "text.code.passed.ci";

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

	@Test
	public void notPassedHasOnlyBaseName(){
		assertArrayEquals(new String[]{"item.code.q0.i1.name"}, CodeItem.nameKeys(0, 0, 1));
	}

	@Test
	public void singlePassAddsOneSuffix(){
		assertArrayEquals(new String[]{BASE, REVIEW}, CodeItem.nameKeys(4, CodeItem.REVIEW, 2));
		assertArrayEquals(new String[]{BASE, TEST}, CodeItem.nameKeys(4, CodeItem.TEST, 2));
		assertArrayEquals(new String[]{BASE, CI}, CodeItem.nameKeys(4, CodeItem.CI, 2));
	}

	/** 接尾辞は通った順によらず review → test → ci の順 */
	@Test
	public void suffixesFollowFixedOrder(){
		assertArrayEquals(new String[]{BASE, REVIEW, CI}, CodeItem.nameKeys(4, CodeItem.CI | CodeItem.REVIEW, 2));
		assertArrayEquals(new String[]{BASE, TEST, CI}, CodeItem.nameKeys(4, CodeItem.TEST | CodeItem.CI, 2));
		assertArrayEquals(new String[]{BASE, REVIEW, TEST, CI},
				CodeItem.nameKeys(4, CodeItem.REVIEW | CodeItem.TEST | CodeItem.CI, 2));
	}

	@Test
	public void localizedNameJoinsTranslationsWithOneSpace(){
		Item item = CodeItem.get(6, CodeItem.REVIEW | CodeItem.TEST | CodeItem.CI, 2);
		assertEquals(english.get("item.code.q6.i2.name") + " " + english.get(REVIEW) + " " + english.get(TEST) + " "
				+ english.get(CI), item.localizedName());
		assertEquals(item.localizedName(), item.toString());

		Core.bundle = japanese;
		assertEquals(japanese.get("item.code.q0.i1.name"), CodeItem.get(0, 0, 1).localizedName());
	}

	/** 登録した 70 種すべてに、両言語で訳がある */
	@Test
	public void everyCodeItemHasNameInBothLanguages(){
		for(I18NBundle bundle : new I18NBundle[]{english, japanese}){
			Core.bundle = bundle;
			for(Item item : Item.getAllItems()){
				CodeAttr attr = CodeItem.attrOf(item);
				if(attr == null) continue;
				assertFalse(item.name, item.localizedName().contains("???"));
				assertEquals(item.name, CodeItem.displayName(attr.stage, attr.passed, attr.importance), item.localizedName());
			}
		}
	}
}
