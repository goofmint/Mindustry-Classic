package io.anuke.mindustry.game;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EnemySpawnTest{

    @Test
    public void multiplierOneKeepsBaseForAnyRoll(){
        for(float roll : new float[]{0f, 0.25f, 0.5f, 0.999f}){
            assertEquals(5, EnemySpawn.applyMultiplier(5, 1f, roll));
        }
    }

    @Test
    public void multiplierTwoDoubles(){
        assertEquals(6, EnemySpawn.applyMultiplier(3, 2f, 0f));
        assertEquals(6, EnemySpawn.applyMultiplier(3, 2f, 0.999f));
    }

    @Test
    public void fractionCarriesWhenRollIsBelowFraction(){
        //3 * 1.5 = 4.5: 小数 0.5 に対し roll < 0.5 のときだけ切り上げる
        assertEquals(5, EnemySpawn.applyMultiplier(3, 1.5f, 0f));
        assertEquals(5, EnemySpawn.applyMultiplier(3, 1.5f, 0.49f));
        assertEquals(4, EnemySpawn.applyMultiplier(3, 1.5f, 0.5f));
        assertEquals(4, EnemySpawn.applyMultiplier(3, 1.5f, 0.99f));
    }

    @Test
    public void lowMultiplierReduces(){
        //10 * 0.7f は float でもちょうど 7.0f になるので、端数が無く抽選に依らず 7
        assertEquals(7, EnemySpawn.applyMultiplier(10, 0.7f, 0.999f));
        assertEquals(7, EnemySpawn.applyMultiplier(10, 0.7f, 0f));
        //3 * 0.7 = 2.1: 2 か 3
        assertEquals(2, EnemySpawn.applyMultiplier(3, 0.7f, 0.99f));
        assertEquals(3, EnemySpawn.applyMultiplier(3, 0.7f, 0f));
    }

    @Test
    public void zeroBaseStaysZero(){
        assertEquals(0, EnemySpawn.applyMultiplier(0, 2.5f, 0f));
        assertEquals(0, EnemySpawn.applyMultiplier(0, 0.7f, 0.5f));
    }
}
