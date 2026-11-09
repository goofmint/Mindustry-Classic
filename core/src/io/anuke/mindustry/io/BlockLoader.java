// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.io;

import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.*;
import io.anuke.ucore.util.Log;

public class BlockLoader {
    /** ブロックIDの上限。Tile が床とブロックのIDを符号付き byte に詰め、NetworkIO もブロックIDを byte で送るため、127 まで */
    public static final int MAX_BLOCKS = 127;

    public static void load(){

        Block[] blockClasses = {
            Blocks.air,
            DefenseBlocks.stonewall,
            DistributionBlocks.conduit,
            ProductionBlocks.coaldrill,
            WeaponBlocks.chainturret,
            SpecialBlocks.enemySpawn
            //add any new block sections here
        };

        for(Block block : Block.getAllBlocks()){
            block.init();
        }

        checkRegistrationLimit(Block.getAllBlocks().size);

        Log.info("Total blocks loaded: {0}", Block.getAllBlocks().size);
    }

    /** count が MAX_BLOCKS を超えていれば RuntimeException */
    static void checkRegistrationLimit(int count){
        if(count > MAX_BLOCKS){
            throw new RuntimeException("Too many blocks: " + count + " registered, limit is " + MAX_BLOCKS
                    + " (block IDs are stored as signed bytes in Tile and NetworkIO)");
        }
    }
}
