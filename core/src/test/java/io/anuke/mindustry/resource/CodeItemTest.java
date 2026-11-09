package io.anuke.mindustry.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.anuke.mindustry.resource.CodeItem.CodeAttr;
import org.junit.Test;

public class CodeItemTest{
    private static final float EPS = 1e-6f;
    private static final Item[] NON_CODE = {
        Item.credit, Item.igpu, Item.pcie
    };

    private static int codeItemCount(){
        int count = 0;
        for(Item item : Item.getAllItems()){
            if(CodeItem.attrOf(item) != null) count++;
        }
        return count;
    }

    @Test
    public void registersExactly70CodeItems(){
        assertEquals(70, codeItemCount());
    }

    @Test
    public void namesMatchAttributes(){
        for(Item item : Item.getAllItems()){
            CodeAttr attr = CodeItem.attrOf(item);
            if(attr == null) continue;
            assertTrue(item.name, item.name.matches("code-q[0-6]-p[0-7]-i[12]"));
            assertEquals(item.name, "code-q" + attr.stage + "-p" + attr.passed + "-i" + attr.importance, item.name);
        }
    }

    @Test
    public void stageRangesPerPassed(){
        int[] min = {0, 1, 1, 2, 1, 2, 2, 3};
        int[] max = {3, 5, 4, 6, 4, 6, 5, 6};
        for(int importance = 1; importance <= 2; importance++){
            for(int passed = 0; passed < 8; passed++){
                for(int stage = 0; stage <= CodeItem.MAX_STAGE; stage++){
                    boolean expected = stage >= min[passed] && stage <= max[passed];
                    boolean registered;
                    try{
                        CodeItem.get(stage, passed, importance);
                        registered = true;
                    }catch(IllegalArgumentException e){
                        registered = false;
                    }
                    assertEquals("stage=" + stage + " passed=" + passed + " importance=" + importance, expected, registered);
                }
            }
        }
    }

    @Test
    public void advanceClampsToMaxStage(){
        for(int importance = 1; importance <= 2; importance++){
            Item item = CodeItem.get(5, CodeItem.TEST | CodeItem.CI, importance);
            Item next = CodeItem.advance(item, CodeItem.REVIEW, 2);
            assertSame(CodeItem.get(6, 7, importance), next);
            CodeAttr attr = CodeItem.attrOf(next);
            assertEquals(6, attr.stage);
            assertEquals(7, attr.passed);
            assertEquals(importance, attr.importance);
        }
    }

    @Test
    public void advanceWithSetBitReturnsSameInstance(){
        Item reviewed = CodeItem.get(2, CodeItem.REVIEW, 1);
        assertSame(reviewed, CodeItem.advance(reviewed, CodeItem.REVIEW, 1));
        assertSame(reviewed, CodeItem.advance(reviewed, CodeItem.REVIEW, 2));
    }

    @Test
    public void advanceSetsBitAndAddsStages(){
        Item start = CodeItem.get(0, 0, 2);
        assertSame(CodeItem.get(1, CodeItem.TEST, 2), CodeItem.advance(start, CodeItem.TEST, 1));
        assertSame(CodeItem.get(2, CodeItem.REVIEW, 2), CodeItem.advance(start, CodeItem.REVIEW, 2));
    }

    @Test
    public void roundTrip(){
        for(Item item : Item.getAllItems()){
            CodeAttr attr = CodeItem.attrOf(item);
            if(attr == null) continue;
            Item back = CodeItem.get(attr.stage, attr.passed, attr.importance);
            assertSame(item.name, item, back);
            CodeAttr again = CodeItem.attrOf(back);
            assertSame(item.name, attr, again);
            assertEquals(attr.stage, again.stage);
            assertEquals(attr.passed, again.passed);
            assertEquals(attr.importance, again.importance);
        }
    }

    @Test
    public void scoreValues(){
        assertEquals(-5f, CodeItem.score(CodeItem.get(0, 0, 1)), EPS);
        assertEquals(-10f, CodeItem.score(CodeItem.get(0, 0, 2)), EPS);
        assertEquals(2f, CodeItem.score(CodeItem.get(6, 7, 2)), EPS);
        for(Item item : NON_CODE){
            assertEquals(item.name, 0f, CodeItem.score(item), EPS);
        }
    }

    @Test
    public void attrOfNonCodeIsNull(){
        assertNull(CodeItem.attrOf(null));
        for(Item item : NON_CODE){
            assertNull(item.name, CodeItem.attrOf(item));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void getUnregisteredThrows(){
        CodeItem.get(0, CodeItem.REVIEW, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getStageOutOfRangeThrows(){
        CodeItem.get(7, 0, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getImportanceOutOfRangeThrows(){
        CodeItem.get(0, 0, 3);
    }

    @Test
    public void advanceRejectsNonCodeItems(){
        for(Item item : NON_CODE){
            try{
                CodeItem.advance(item, CodeItem.REVIEW, 1);
                fail("expected IllegalArgumentException for " + item.name);
            }catch(IllegalArgumentException expected){
                //ok
            }
        }
    }

    @Test
    public void advanceRejectsInvalidBit(){
        Item item = CodeItem.get(0, 0, 1);
        int[] invalid = {0, 3, 8, CodeItem.REVIEW | CodeItem.CI};
        for(int bit : invalid){
            try{
                CodeItem.advance(item, bit, 1);
                fail("expected IllegalArgumentException for bit " + bit);
            }catch(IllegalArgumentException expected){
                //ok
            }
        }
    }

    @Test
    public void advanceRejectsStagesOutsideTransitions(){
        Item item = CodeItem.get(0, 0, 1);
        int[][] invalid = {{CodeItem.REVIEW, 0}, {CodeItem.REVIEW, 3}, {CodeItem.TEST, 2}, {CodeItem.CI, 2}, {CodeItem.CI, -1}};
        for(int[] pair : invalid){
            try{
                CodeItem.advance(item, pair[0], pair[1]);
                fail("expected IllegalArgumentException for bit " + pair[0] + " stages " + pair[1]);
            }catch(IllegalArgumentException expected){
                assertTrue(expected.getMessage(), expected.getMessage().contains("transition"));
            }
        }
    }

    @Test
    public void registerAllIsIdempotent(){
        int before = Item.getAllItems().size;
        CodeItem.registerAll();
        CodeItem.registerAll();
        assertEquals(before, Item.getAllItems().size);
        assertEquals(70, codeItemCount());
    }

    @Test
    public void codeItemsAreNotNull(){
        assertNotNull(CodeItem.get(0, 0, 1));
    }
}
