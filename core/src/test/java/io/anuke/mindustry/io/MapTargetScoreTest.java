package io.anuke.mindustry.io;

import static org.junit.Assert.assertEquals;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import io.anuke.mindustry.world.Map;
import org.junit.Test;

/** maps.json の targetScore が libGDX Json で Map に入ることの確認(Maps と同じ設定で読む) */
public class MapTargetScoreTest{

    @Test
    public void targetScoreIsReadAndDefaultsToZero(){
        Json json = new Json();
        json.setElementType(Maps.ArrayContainer.class, "maps", Map.class);

        String text = "{\"maps\":[{\"id\":0,\"name\":\"a\",\"targetScore\":1000},{\"id\":1,\"name\":\"b\"}]}";
        Array<Map> maps = json.fromJson(Maps.ArrayContainer.class, text).maps;

        assertEquals(2, maps.size);
        assertEquals(1000, maps.get(0).targetScore);
        assertEquals(0, maps.get(1).targetScore);
    }
}
