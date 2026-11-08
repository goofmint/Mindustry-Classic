package io.anuke.mindustry.ui.dialogs;

import io.anuke.mindustry.core.GameState.State;
import io.anuke.ucore.scene.ui.Dialog;
import io.anuke.ucore.util.Bundles;

import static io.anuke.mindustry.Vars.*;

public class RestartDialog extends Dialog {
    
    public RestartDialog(){
        super("$text.gameover", "dialog");

        shown(() -> {
            content().clearChildren();
            if(state.won){
                getTitleLabel().setText("$text.victory");
            }else if(state.score.bankrupt()){
                getTitleLabel().setText("$text.bankrupt");
            }else{
                getTitleLabel().setText("$text.gameover");
            }
            if(control.isHighScore()){
                content().add("$text.highscore").pad(6);
                content().row();
            }
            if(control.isBestScore()){
                content().add("$text.bestscore").pad(6);
                content().row();
            }
            content().add(Bundles.format("text.delivered", state.score.balance())).pad(6).colspan(2);
            content().row();
            content().add("$text.lasted").pad(12).get();
            content().add("[accent]" + state.wave);
            pack();
        });

        getButtonTable().addButton("$text.menu", ()-> {
            hide();
            state.set(State.menu);
            logic.reset();
        }).size(130f, 60f);
    }
}
