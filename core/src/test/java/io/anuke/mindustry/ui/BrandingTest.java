// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.I18NBundle;
import io.anuke.mindustry.ui.Links.LinkEntry;
import io.anuke.ucore.core.Core;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.junit.BeforeClass;
import org.junit.Test;

/** Task 4.4: About のリンク、クレジットと製品名の bundle キー、Web 版の HTML シェルの確認(作業ディレクトリは core) */
public class BrandingTest{
    private static final String[] KEYS = {
        "text.gamename",
        "text.link.coderabbit.name",
        "text.link.coderabbit.description",
        "text.credits.coderabbit",
        "text.about",
        "text.credits.license",
        "text.credits.font",
    };
    private static final String[] REMOVED_LINKS = {"discord", "trello", "wiki", "itch.io", "google-play", "github", "dev-builds"};

    /** LinkEntry は説明文を Core.bundle から引くので、Links を読む前に英語の文言を読み込む */
    @BeforeClass
    public static void loadBundle(){
        I18NBundle.setExceptionOnMissingKey(false);
        Core.bundle = bundle(Locale.ROOT);
    }

    private static I18NBundle bundle(Locale locale){
        return I18NBundle.createBundle(new FileHandle("assets/bundles/bundle"), locale);
    }

    private static Locale[] locales(){
        return new Locale[]{Locale.ROOT, new Locale("ja")};
    }

    @Test
    public void onlyCodeRabbitLink(){
        LinkEntry[] links = Links.getLinks();
        assertEquals(1, links.length);
        assertEquals("coderabbit", links[0].name);
        assertEquals("https://coderabbit.ai", links[0].link);
        assertEquals("CodeRabbit", links[0].title);
    }

    @Test
    public void bundlesHaveBrandingKeys(){
        for(Locale locale : locales()){
            I18NBundle bundle = bundle(locale);
            for(String key : KEYS){
                assertFalse(locale + " " + key, bundle.get(key).contains("???"));
            }
        }
    }

    @Test
    public void oldLinkDescriptionsAreRemoved(){
        for(Locale locale : locales()){
            I18NBundle bundle = bundle(locale);
            for(String name : REMOVED_LINKS){
                String key = "text.link." + name + ".description";
                assertTrue(locale + " " + key, bundle.get(key).contains("???"));
            }
        }
    }

    @Test
    public void gameNameIsTheSameEverywhere(){
        String name = bundle(Locale.ROOT).get("text.gamename");
        assertEquals(name, bundle(new Locale("ja")).get("text.gamename"));
        String html = new FileHandle("../html/webapp/index.html").readString("UTF-8");
        assertTrue(html, html.contains("<title>" + name + "</title>"));
    }

    @Test
    public void webLogoIs610x124WithAlpha() throws IOException{
        BufferedImage image = ImageIO.read(new FileHandle("../html/webapp/html/logo.png").file());
        assertEquals(610, image.getWidth());
        assertEquals(124, image.getHeight());
        assertTrue(image.getColorModel().hasAlpha());
        //余白は透明
        assertEquals(0, image.getRGB(0, 0) >>> 24);
    }
}
