// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.input;

import com.badlogic.gdx.Input.Keys;
import io.anuke.ucore.util.Input;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DefaultKeybindsTest{

    @Test
    public void consoleDefaultIsF2InBrowserAndGraveOnDesktop(){
        assertEquals(Input.F2, DefaultKeybinds.consoleDefault(true));
        assertEquals(Input.GRAVE, DefaultKeybinds.consoleDefault(false));
    }

    @Test
    public void storedGraveIsReplacedOnlyInBrowser(){
        assertEquals(Input.F2, DefaultKeybinds.migrateConsole(true, Input.GRAVE));
        assertEquals(Input.GRAVE, DefaultKeybinds.migrateConsole(false, Input.GRAVE));
    }

    @Test
    public void functionKeyCodesMatchLibgdx(){
        //Inputs.keyTap compares Input.code with the backend key code, so F2 only fires if these match
        assertEquals(Keys.F1, Input.F1.code);
        assertEquals(Keys.F2, Input.F2.code);
        assertEquals(Keys.F12, Input.F12.code);
    }

    @Test
    public void otherStoredBindingsAreKept(){
        assertEquals(Input.F3, DefaultKeybinds.migrateConsole(true, Input.F3));
        assertEquals(Input.UNSET, DefaultKeybinds.migrateConsole(true, Input.UNSET));
        assertEquals(Input.F2, DefaultKeybinds.migrateConsole(false, Input.F2));
    }
}
