// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import io.anuke.ucore.util.Bundles;

public class Map{
	public int id = -1;
	public String name;
	public boolean visible = true;
	public boolean flipBase = false;
	public boolean custom = false;
	public boolean oreGen = true;
	/** 目標得点。0 は未設定 */
	public int targetScore;
	public Color backgroundColor = Color.valueOf("646464");
	
	public transient Pixmap pixmap;
	public transient Texture texture;

	public Map(){}

	public String localized(){
		return Bundles.get("map."+name+".name", name);
	}

	/** map.<name>.description。キーが無いときは Bundles の欠けたキーの表示(???キー???)のまま返す */
	public String localizedDescription(){
		return Bundles.get("map."+name+".description");
	}
	
	public int getWidth(){
		return pixmap.getWidth();
	}
	
	public int getHeight(){
		return pixmap.getHeight();
	}
}
