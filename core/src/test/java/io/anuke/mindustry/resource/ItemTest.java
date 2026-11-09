// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class ItemTest{

    @Test
    public void fixedIdsComeFirst(){
        assertEquals(0, Item.credit.id);
        assertEquals(1, Item.igpu.id);
        assertEquals(2, Item.pcie.id);
    }

    @Test
    public void totalItemCount(){
        //credit, igpu, pcie + コード 70 種
        assertEquals(73, Item.getAllItems().size);
    }

    @Test
    public void idsMatchIndices(){
        for(int i = 0; i < Item.getAllItems().size; i++){
            assertEquals(i, Item.getAllItems().get(i).id);
        }
    }

    @Test
    public void registrationLimitAllows127(){
        Item.checkRegistrationLimit(127);
    }

    @Test
    public void registrationLimitRejects128(){
        try{
            Item.checkRegistrationLimit(128);
            fail("expected RuntimeException");
        }catch(RuntimeException e){
            assertTrue(e.getMessage(), e.getMessage().contains("128"));
            assertTrue(e.getMessage(), e.getMessage().contains("127"));
        }
    }

    @Test
    public void realRegistrationIsWithinLimit(){
        Item.checkRegistrationLimit();
    }
}
