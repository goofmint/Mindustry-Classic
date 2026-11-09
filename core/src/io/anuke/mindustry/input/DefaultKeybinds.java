// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.input;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import io.anuke.ucore.core.Inputs.Axis;
import io.anuke.ucore.core.KeyBinds;
import io.anuke.mindustry.Vars;
import io.anuke.ucore.util.Input;

public class DefaultKeybinds {

    public static void load(){
        KeyBinds.defaults(
            "move_x", new Axis(Input.A, Input.D),
            "move_y", new Axis(Input.S, Input.W),
            "select", Input.MOUSE_LEFT,
            "break", Input.MOUSE_RIGHT,
            "shoot", Input.MOUSE_LEFT,
            "zoom_hold", Input.CONTROL_LEFT,
            "zoom", new Axis(Input.SCROLL),
            "menu", Gdx.app.getType() == ApplicationType.Android ? Input.BACK : Input.ESCAPE,
            "pause", Input.SPACE,
            "dash", Input.SHIFT_LEFT,
            "rotate_alt", new Axis(Input.R, Input.E),
            "rotate", new Axis(Input.SCROLL),
            "toggle_menus", Input.C,
            "block_info", Input.CONTROL_LEFT,
            "player_list", Input.TAB,
            "chat", Input.ENTER,
            "chat_history_prev", Input.UP,
            "chat_history_next", Input.DOWN,
            "chat_scroll", new Axis(Input.SCROLL),
            "console", consoleDefault(Vars.gwt),
            "weapon_1", Input.NUM_1,
            "weapon_2", Input.NUM_2,
            "weapon_3", Input.NUM_3,
            "weapon_4", Input.NUM_4,
            "weapon_5", Input.NUM_5,
            "weapon_6", Input.NUM_6
        );
    }

    /**Default key of the in-game console. The libGDX GWT backend maps the grave key (keyCode 192) to UNKNOWN,
     * so the browser build uses F2 instead. Desktop keeps the grave key.*/
    public static Input consoleDefault(boolean gwt){
        return gwt ? Input.F2 : Input.GRAVE;
    }

    /**Console binding to use after loading the stored value. On GWT a stored GRAVE can never fire
     * (and cannot be chosen in the rebind dialog), so it is replaced with the browser default.
     * Every other stored value is kept as is.*/
    public static Input migrateConsole(boolean gwt, Input stored){
        return gwt && stored == Input.GRAVE ? consoleDefault(true) : stored;
    }
}
