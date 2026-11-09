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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.BeforeClass;
import org.junit.Test;

/** Task 4.4 / Issue #79: About のリンク、クレジットと製品名の bundle キー、Web 版の HTML シェル、メニューのロゴの確認(作業ディレクトリは core) */
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

    private static final String MENU_LOGO = "assets-raw/sprites/ui/logotext.png";
    private static final String ATLAS = "assets/sprites/sprites.atlas";
    /** tools/gen_logo.py の MENU_MAX_WIDTH / MENU_MAX_HEIGHT と同じ(旧ロゴは 89x21、6 倍表示のため) */
    private static final int MENU_LOGO_MAX_WIDTH = 96;
    private static final int MENU_LOGO_MAX_HEIGHT = 28;

    /** Issue #79: メニューのロゴ(gen_logo.py が書き出す元画像)は透明背景で、6 倍表示に収まる大きさ */
    @Test
    public void menuLogoSourceIsSmallWithTransparentCorners() throws IOException{
        BufferedImage image = ImageIO.read(new FileHandle(MENU_LOGO).file());
        assertTrue(image.getColorModel().hasAlpha());
        assertTrue("width " + image.getWidth(), image.getWidth() <= MENU_LOGO_MAX_WIDTH);
        assertTrue("height " + image.getHeight(), image.getHeight() <= MENU_LOGO_MAX_HEIGHT);
        int right = image.getWidth() - 1, bottom = image.getHeight() - 1;
        assertEquals(0, image.getRGB(0, 0) >>> 24);
        assertEquals(0, image.getRGB(right, 0) >>> 24);
        assertEquals(0, image.getRGB(0, bottom) >>> 24);
        assertEquals(0, image.getRGB(right, bottom) >>> 24);
    }

    /** Issue #79: sprites.atlas の logotext は元画像と同じ大きさで回転しておらず、ページの中身も元画像と一致する */
    @Test
    public void menuLogoIsPackedUnchanged() throws IOException{
        BufferedImage source = ImageIO.read(new FileHandle(MENU_LOGO).file());
        String[] lines = new FileHandle(ATLAS).readString("UTF-8").split("\\r?\\n");
        assertEquals("sprites.png", lines[1]);
        int pages = 0, entries = 0;
        Map<String, String> region = new HashMap<>();
        for(int i = 0; i < lines.length; i++){
            if(lines[i].endsWith(".png")) pages++;
            if(!lines[i].equals("logotext")) continue;
            entries++;
            for(i++; i < lines.length && lines[i].startsWith("  "); i++){
                String[] kv = lines[i].trim().split(": ", 2);
                region.put(kv[0], kv[1]);
            }
        }
        assertEquals(1, pages);
        assertEquals(1, entries);
        assertEquals("false", region.get("rotate"));
        assertEquals("0, 0", region.get("offset"));
        assertEquals(source.getWidth() + ", " + source.getHeight(), region.get("size"));
        assertEquals(region.get("size"), region.get("orig"));

        String[] xy = region.get("xy").split(", ");
        int x0 = Integer.parseInt(xy[0]), y0 = Integer.parseInt(xy[1]);
        BufferedImage page = ImageIO.read(new FileHandle("assets/sprites/sprites.png").file());
        for(int y = 0; y < source.getHeight(); y++){
            for(int x = 0; x < source.getWidth(); x++){
                int expected = source.getRGB(x, y), actual = page.getRGB(x0 + x, y0 + y);
                String at = "pixel " + x + "," + y;
                if(expected >>> 24 == 0){
                    assertEquals(at, 0, actual >>> 24);
                }else{
                    assertEquals(at, expected, actual);
                }
            }
        }
    }
}
