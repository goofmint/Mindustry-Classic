// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.ui;

import com.badlogic.gdx.graphics.Color;
import io.anuke.ucore.util.Bundles;

public class Links {
    private static final LinkEntry[] links = {
        new LinkEntry("coderabbit", "https://coderabbit.ai", Color.valueOf("ff570a"), "icon-check"),
    };

    public static LinkEntry[] getLinks(){
        return links;
    }

    public static class LinkEntry{
        public final String name, title, description, link, icon;
        public final Color color;

        public LinkEntry(String name, String link, Color color, String icon) {
            this.name = name;
            this.icon = icon;
            this.color = color;
            this.title = Bundles.getNotNull("text.link." + name + ".name");
            this.description = Bundles.getNotNull("text.link." + name +".description");
            this.link = link;
        }
    }
}
