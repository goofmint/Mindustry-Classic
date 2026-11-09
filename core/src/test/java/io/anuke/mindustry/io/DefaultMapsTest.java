// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.I18NBundle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import io.anuke.mindustry.world.Map;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.junit.Test;

/** core/assets/maps/maps.json の 3 マップ(Task 4.1)と、その PNG と bundle のキーの確認 */
public class DefaultMapsTest{
    private static final String[] NAMES = {"prototype", "sprint", "release"};
    private static final int[] TARGETS = {300, 450, 600};
    private static final int SIZE = 256;

    private Array<Map> readMaps(){
        //Maps と同じ設定(Color は 16 進文字列)で読む
        Json json = new Json();
        json.setElementType(Maps.ArrayContainer.class, "maps", Map.class);
        json.setSerializer(Color.class, new Json.ReadOnlySerializer<Color>(){
            @Override
            public Color read(Json json, JsonValue jsonData, Class type){
                return Color.valueOf(jsonData.asString());
            }
        });
        return json.fromJson(Maps.ArrayContainer.class, new FileHandle("assets/maps/maps.json")).maps;
    }

    @Test
    public void threeMapsWithIdsNamesAndTargets(){
        Array<Map> maps = readMaps();
        assertEquals(NAMES.length, maps.size);
        for(int i = 0; i < NAMES.length; i++){
            Map map = maps.get(i);
            assertEquals(i, map.id);
            assertEquals(NAMES[i], map.name);
            assertEquals(TARGETS[i], map.targetScore);
            assertTrue(map.visible);
            assertTrue(map.oreGen);
            assertFalse(map.flipBase);
            assertFalse(map.custom);
        }
    }

    @Test
    public void everyMapPngExistsAndIs256Square() throws IOException{
        for(Map map : readMaps()){
            FileHandle file = new FileHandle("assets/maps/" + map.name + ".png");
            assertTrue(file.path(), file.exists());
            BufferedImage image = ImageIO.read(file.file());
            assertNotNull(file.path(), image);
            assertEquals(file.path(), SIZE, image.getWidth());
            assertEquals(file.path(), SIZE, image.getHeight());
        }
    }

    @Test
    public void onlyRegisteredPngsAreInTheMapDirectory(){
        FileHandle[] pngs = new FileHandle("assets/maps").list(".png");
        assertEquals(NAMES.length, pngs.length);
        for(FileHandle png : pngs){
            assertNotNull(png.name(), find(readMaps(), png.nameWithoutExtension()));
        }
    }

    @Test
    public void bundlesHaveNameAndDescription(){
        for(Locale locale : new Locale[]{Locale.ROOT, new Locale("ja")}){
            I18NBundle bundle = I18NBundle.createBundle(new FileHandle("assets/bundles/bundle"), locale);
            for(String name : NAMES){
                for(String key : new String[]{"map." + name + ".name", "map." + name + ".description"}){
                    assertFalse(locale + " " + key, bundle.get(key).contains("???"));
                }
            }
        }
    }

    private Map find(Array<Map> maps, String name){
        for(Map map : maps){
            if(map.name.equals(name)) return map;
        }
        return null;
    }
}
