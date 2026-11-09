// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.ucore.util;

import com.badlogic.gdx.Input.Keys;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InputKeyCodesTest{

    @Test
    public void keyCodesMatchLibgdx(){
        //Inputs.keyTap compares Input.code with the backend key code
        assertEquals(Keys.ESCAPE, Input.ESCAPE.code);
        assertEquals(Keys.END, Input.END.code);
        assertEquals(Keys.INSERT, Input.INSERT.code);
        assertEquals(Keys.F1, Input.F1.code);
        assertEquals(Keys.F2, Input.F2.code);
        assertEquals(Keys.F3, Input.F3.code);
        assertEquals(Keys.F4, Input.F4.code);
        assertEquals(Keys.F5, Input.F5.code);
        assertEquals(Keys.F6, Input.F6.code);
        assertEquals(Keys.F7, Input.F7.code);
        assertEquals(Keys.F8, Input.F8.code);
        assertEquals(Keys.F9, Input.F9.code);
        assertEquals(Keys.F10, Input.F10.code);
        assertEquals(Keys.F11, Input.F11.code);
        assertEquals(Keys.F12, Input.F12.code);
    }

    @Test
    public void ordinalsStayStable(){
        //ordinals are what keybind settings store, so reordering or inserting constants breaks saved bindings
        assertEquals(183, Input.values().length);
        assertEquals(136, Input.ESCAPE.ordinal());
        assertEquals(137, Input.END.ordinal());
        assertEquals(138, Input.INSERT.ordinal());
        assertEquals(170, Input.F1.ordinal());
        assertEquals(181, Input.F12.ordinal());
        assertEquals(182, Input.UNSET.ordinal());
    }

    @Test
    public void lookupByCodeFindsEscapeNotFunctionKey(){
        assertEquals(Input.ESCAPE, Input.findByType(Input.Type.key, Keys.ESCAPE, false));
        assertEquals(Input.F1, Input.findByType(Input.Type.key, Keys.F1, false));
    }
}
