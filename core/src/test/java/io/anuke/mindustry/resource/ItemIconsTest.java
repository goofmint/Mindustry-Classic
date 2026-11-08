// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.resource;

import static org.junit.Assert.assertEquals;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.ucore.function.Predicate;
import org.junit.Test;

public class ItemIconsTest{

    private static ObjectSet<String> allIcons(){
        ObjectSet<String> regions = new ObjectSet<>();
        for(Item item : Item.getAllItems()){
            regions.add("icon-" + item.name);
        }
        return regions;
    }

    private static Predicate<String> contains(final ObjectSet<String> regions){
        return new Predicate<String>(){
            @Override
            public boolean test(String name){
                return regions.contains(name);
            }
        };
    }

    @Test
    public void noneMissing(){
        assertEquals(0, Item.missingIcons(contains(allIcons())).size);
    }

    @Test
    public void listsAllMissing(){
        ObjectSet<String> regions = allIcons();
        String first = "icon-" + CodeItem.get(0, 0, 1).name;
        String second = "icon-" + Item.credit.name;
        regions.remove(first);
        regions.remove(second);

        Array<String> missing = Item.missingIcons(contains(regions));
        assertEquals(2, missing.size);
        //登録順(credit が先)
        assertEquals(second, missing.get(0));
        assertEquals(first, missing.get(1));
    }
}
