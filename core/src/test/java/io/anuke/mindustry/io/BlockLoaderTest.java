// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.io;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.I18NBundle;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.*;
import io.anuke.ucore.core.Core;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Locale;

public class BlockLoaderTest{

    /** ブロックの定義は名前を Core.bundle から引くので、テストの前に英語の文言を読み込む(作業ディレクトリは core) */
    @BeforeClass
    public static void loadBundle(){
        I18NBundle.setExceptionOnMissingKey(false);
        Core.bundle = I18NBundle.createBundle(new FileHandle("assets/bundles/bundle"), Locale.ROOT);
    }

    @Test
    public void registrationLimitAllows127(){
        BlockLoader.checkRegistrationLimit(127);
    }

    @Test
    public void registrationLimitRejects128(){
        try{
            BlockLoader.checkRegistrationLimit(128);
            fail("expected RuntimeException");
        }catch(RuntimeException e){
            assertTrue(e.getMessage(), e.getMessage().contains("128"));
            assertTrue(e.getMessage(), e.getMessage().contains("127"));
        }
    }

    @Test
    public void realRegistrationIsWithinLimit(){
        //BlockLoader.load() と同じクラスを初期化して、全ブロックを登録させる
        Block[] blockClasses = {
            Blocks.air,
            DefenseBlocks.stonewall,
            DistributionBlocks.conduit,
            ProductionBlocks.coaldrill,
            WeaponBlocks.chainturret,
            SpecialBlocks.enemySpawn
        };
        System.out.println("Total blocks registered: " + Block.getAllBlocks().size);
        BlockLoader.checkRegistrationLimit(Block.getAllBlocks().size);
    }
}
