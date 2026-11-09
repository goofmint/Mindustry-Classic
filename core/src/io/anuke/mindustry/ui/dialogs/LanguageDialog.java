// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.ui.dialogs;

import io.anuke.ucore.core.Settings;
import io.anuke.ucore.scene.ui.ButtonGroup;
import io.anuke.ucore.scene.ui.ScrollPane;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Log;

import java.util.Locale;

import static io.anuke.mindustry.Vars.locales;
import static io.anuke.mindustry.Vars.ui;

public class LanguageDialog extends FloatingDialog{

    public LanguageDialog(){
        super("$text.settings.language");
        addCloseButton();
        setup();
    }

    /**
     * 選択表示に使うロケール。設定が default で OS のロケールが ja_JP のような場合も、
     * バンドルは言語だけで選ばれるので、言語が一致する対応ロケールを返す。
     * 対応していない言語のときは基底バンドル(英語、locales[0])が読まれているので、それを返す。
     */
    private Locale selectedLocale(){
        String language = ui.getLocale().getLanguage();
        for(Locale loc : locales){
            if(loc.getLanguage().equals(language)) return loc;
        }
        return locales[0];
    }

    private void setup(){
        Table langs = new Table();
        langs.marginRight(24f).marginLeft(24f);
        ScrollPane pane = new ScrollPane(langs, "clear");
        pane.setFadeScrollBars(false);

        ButtonGroup<TextButton> group = new ButtonGroup<>();

        for(Locale loc : locales){
            TextButton button = new TextButton(Bundles.get("text.locale." + loc), "toggle");
            button.setChecked(loc.equals(selectedLocale()));
            button.clicked(() -> {
                if(ui.getLocale().equals(loc)) return;
                Settings.putString("locale", loc.toString());
                Settings.save();
                Log.info("Setting locale: {0}", loc.toString());
                ui.showInfo("$text.language.restart");
            });
            langs.add(button).group(group).update(t -> {
                t.setChecked(loc.equals(selectedLocale()));
            }).size(400f, 60f).row();
        }

        content().add(pane);
    }
}
