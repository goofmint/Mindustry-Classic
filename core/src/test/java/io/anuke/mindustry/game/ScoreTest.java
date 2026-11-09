// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.anuke.mindustry.resource.CodeItem;
import io.anuke.mindustry.resource.CodeItem.CodeAttr;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.ItemStack;
import org.junit.Before;
import org.junit.Test;

public class ScoreTest{
    private static final float DELTA = 1e-5f;

    private Inventory inventory;
    private Score score;

    @Before
    public void setUp(){
        //clearItems() は Vars.debug を読むので使わない
        inventory = new Inventory();
        score = new Score(inventory);
        score.reset(Balance.INITIAL_CREDIT);
    }

    /** 指定した段階(品質)の、到達可能なコードアイテムを 1 つ探す */
    private static Item codeWithStage(int stage){
        for(Item item : Item.getAllItems()){
            CodeAttr attr = CodeItem.attrOf(item);
            if(attr != null && attr.stage == stage) return item;
        }
        throw new AssertionError("no code item with stage " + stage);
    }

    @Test
    public void resetSetsInitialBalance(){
        assertEquals(Balance.INITIAL_CREDIT, score.balance());
        assertEquals(Balance.INITIAL_CREDIT, inventory.getAmount(Item.credit));
    }

    @Test
    public void halfTwiceAddsOne(){
        score.deliver(Item.igpu, 0.5f);
        assertEquals(Balance.INITIAL_CREDIT, score.balance());
        score.deliver(Item.igpu, 0.5f);
        assertEquals(Balance.INITIAL_CREDIT + 1, score.balance());
    }

    @Test
    public void exactOneAddsOne(){
        score.deliver(Item.igpu, 1f);
        assertEquals(Balance.INITIAL_CREDIT + 1, score.balance());
    }

    @Test
    public void pointEightFiveTimesAddsFourAtFifth(){
        for(int i = 0; i < 4; i++) score.deliver(Item.igpu, 0.8f);
        assertEquals(Balance.INITIAL_CREDIT + 3, score.balance());
        score.deliver(Item.igpu, 0.8f);
        assertEquals(Balance.INITIAL_CREDIT + 4, score.balance());
    }

    @Test
    public void negativeIsAppliedImmediately(){
        score.deliver(Item.igpu, -5f);
        assertEquals(Balance.INITIAL_CREDIT - 5, score.balance());
        assertFalse(score.bankrupt());
    }

    @Test
    public void deliverSetsUpdatedFlag(){
        inventory.setUpdated(false);
        score.deliver(Item.igpu, 2f);
        assertTrue(inventory.isUpdated());
    }

    @Test
    public void deliveryBelowZeroBankrupts(){
        score.reset(3);
        score.deliver(Item.igpu, -5f);
        assertEquals(0, score.balance());
        assertTrue(score.bankrupt());
    }

    @Test
    public void bankruptPersistsAndDeliveriesContinue(){
        score.reset(1);
        score.deliver(Item.igpu, -2f);
        assertTrue(score.bankrupt());
        score.deliver(Item.igpu, 10f);
        assertEquals(10, score.balance());
        assertTrue(score.bankrupt());
    }

    @Test
    public void landingExactlyOnZeroIsNotBankrupt(){
        score.reset(5);
        score.deliver(Item.igpu, -5f);
        assertEquals(0, score.balance());
        assertFalse(score.bankrupt());
    }

    @Test
    public void constructionCannotBankrupt(){
        score.reset(10);
        inventory.removeItem(new ItemStack(Item.credit, 50));
        assertEquals(0, score.balance());
        assertFalse(score.bankrupt());
    }

    @Test
    public void multiplierBoundaries(){
        assertEquals(0.7f, Score.multiplierFor(1f), DELTA);
        assertEquals(1.0f, Score.multiplierFor(0f), DELTA);
        assertEquals(2.5f, Score.multiplierFor(-5f), DELTA);
        assertEquals(0.7f, Score.multiplierFor(3f), DELTA);
        assertEquals(2.5f, Score.multiplierFor(-10f), DELTA);
        assertEquals(1.3f, Score.multiplierFor(-1f), DELTA);
    }

    @Test
    public void emptyHistoryMultiplierIsOne(){
        assertEquals(1f, score.qualityMultiplier, DELTA);
    }

    @Test
    public void nonCodeDeliveryDoesNotChangeMultiplier(){
        score.deliver(Item.igpu, 3f);
        score.deliver(Item.credit, -1f);
        assertEquals(1f, score.qualityMultiplier, DELTA);
    }

    @Test
    public void codeDeliveryUpdatesMultiplier(){
        Item worst = codeWithStage(0);
        score.deliver(worst, CodeItem.score(worst));
        assertEquals(2.5f, score.qualityMultiplier, DELTA);
    }

    @Test
    public void negativeCodeItemLowersBalance(){
        Item worst = codeWithStage(0);
        float itemScore = CodeItem.score(worst);
        assertTrue(itemScore < 0);
        score.deliver(worst, itemScore);
        assertEquals(Balance.INITIAL_CREDIT + (int)itemScore, score.balance());
    }

    @Test
    public void historyDropsOldest(){
        Item worst = codeWithStage(0); //q = -5
        Item best = codeWithStage(CodeItem.MAX_STAGE); //q = 1
        score.reset(100000);

        score.deliver(worst, 0f);
        for(int i = 0; i < Balance.QUALITY_HISTORY - 1; i++) score.deliver(best, 0f);
        //平均 (-5 + 19) / 20 = 0.7 → 1 - 0.21 = 0.79
        assertEquals(0.79f, score.qualityMultiplier, DELTA);

        //21 個目で最古の -5 が抜け、20 個すべて 1 → 0.7
        score.deliver(best, 0f);
        assertEquals(0.7f, score.qualityMultiplier, DELTA);
    }

    @Test
    public void resetRestoresEverything(){
        Item worst = codeWithStage(0);
        score.reset(1);
        score.deliver(worst, -2f);
        score.deliver(Item.igpu, 0.5f);
        assertTrue(score.bankrupt());
        score.target = 500;

        score.reset(Balance.INITIAL_CREDIT);
        assertEquals(Balance.INITIAL_CREDIT, score.balance());
        assertFalse(score.bankrupt());
        assertEquals(1f, score.qualityMultiplier, DELTA);
        assertEquals(500, score.target);

        //端数も消えている: 0.5 を 1 回足しても増えない
        score.deliver(Item.igpu, 0.5f);
        assertEquals(Balance.INITIAL_CREDIT, score.balance());
    }

    @Test
    public void reachedTarget(){
        score.target = 0;
        assertFalse(score.reachedTarget());

        score.target = Balance.INITIAL_CREDIT + 1;
        assertFalse(score.reachedTarget());
        score.deliver(Item.igpu, 1f);
        assertTrue(score.reachedTarget());
    }

    @Test
    public void codeItemsExist(){
        assertNotNull(codeWithStage(0));
        assertNotNull(codeWithStage(CodeItem.MAX_STAGE));
    }
}
