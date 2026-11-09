package io.anuke.mindustry.ui.dialogs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.ui.Links;
import io.anuke.mindustry.ui.Links.LinkEntry;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.ui.ScrollPane;
import io.anuke.ucore.scene.ui.layout.Table;

import static io.anuke.mindustry.Vars.ui;

public class AboutDialog extends FloatingDialog {
    /**Sections of the credits dialog, top to bottom. Each value starts with its own heading.*/
    private static final String[] creditKeys = {"text.credits.coderabbit", "text.about", "text.credits.license", "text.credits.font"};

    public AboutDialog(){
        super("$text.about.button");

        addCloseButton();

        float h = 80f;
        float w = 600f;

        Table in = new Table();
        ScrollPane pane = new ScrollPane(in, "clear");

        for(LinkEntry link : Links.getLinks()){
            Table table = new Table("button");
            table.margin(0);
            table.table(img -> {
                img.addImage("white").height(h - 5).width(40f).color(link.color);
                img.row();
                img.addImage("white").height(5).width(40f).color(link.color.cpy().mul(0.8f, 0.8f, 0.8f, 1f));
            }).expandY();

            table.table(i -> {
                i.background("button");
                i.addImage(link.icon).size(14*3f);
            }).size(h-5, h);

            table.table(inset -> {
                inset.add("[accent]"+link.title).growX().left();
                inset.row();
                inset.labelWrap(link.description).width(w - 100f).color(Color.LIGHT_GRAY).growX();
            }).padLeft(8);

            table.addImageButton("icon-link", 14*3, () -> {
                if(!Gdx.net.openURI(link.link)){
                    ui.showError("$text.linkfail");
                    Gdx.app.getClipboard().setContents(link.link);
                }
            }).size(h-5, h);

            in.add(table).size(w, h).padTop(5).row();
        }

        shown(() -> Timers.run(1f, () -> Core.scene.setScrollFocus(pane)));

        content().add(pane).growX();

        buttons().addButton("$text.credits", this::showCredits).size(200f, 64f);
    }

    private void showCredits(){
        float w = 600f;

        FloatingDialog dialog = new FloatingDialog("$text.credits");
        dialog.addCloseButton();

        //opaque panel so the text stays readable over the menu behind the dialog
        Table in = new Table("pane");
        in.margin(12f);
        for(String key : creditKeys){
            in.labelWrap("$" + key).width(w).left().padBottom(20f).row();
        }
        ScrollPane pane = new ScrollPane(in, "clear");

        dialog.shown(() -> Timers.run(1f, () -> Core.scene.setScrollFocus(pane)));
        dialog.content().add(pane).growX();
        //the credits are long, so hide this dialog while they are open and come back to it afterwards
        dialog.hidden(this::show);
        hide();
        dialog.show();
    }
}
