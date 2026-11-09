// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.input;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.game.EventType.BreakEvent;
import io.anuke.mindustry.game.EventType.PlaceEvent;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetEvents;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.resource.Recipe;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Placement;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;

public abstract class InputHandler extends InputAdapter{
	public float breaktime = 0;
	public Recipe recipe;
	public int rotation;
	public PlaceMode placeMode = mobile ? PlaceMode.cursor : PlaceMode.hold;
	public PlaceMode breakMode = mobile ? PlaceMode.none : PlaceMode.holdDelete;
	public PlaceMode lastPlaceMode = placeMode;
	public PlaceMode lastBreakMode = breakMode;

	public abstract void update();
	public abstract float getCursorX();
	public abstract float getCursorY();
	public abstract float getCursorEndX();
	public abstract float getCursorEndY();
	public int getBlockX(){ return Mathf.sclb(Graphics.world(getCursorX(), getCursorY()).x, tilesize, round2()); }
	public int getBlockY(){ return Mathf.sclb(Graphics.world(getCursorX(), getCursorY()).y, tilesize, round2()); }
	public int getBlockEndX(){ return Mathf.sclb(Graphics.world(getCursorEndX(), getCursorEndY()).x, tilesize, round2()); }
	public int getBlockEndY(){ return Mathf.sclb(Graphics.world(getCursorEndX(), getCursorEndY()).y, tilesize, round2()); }
	public void resetCursor(){}
	public boolean drawPlace(){ return true; }
	
	public boolean onConfigurable(){
		Tile tile = world.tile(getBlockX(), getBlockY());
		return tile != null && (tile.block().isConfigurable(tile) || (tile.isLinked() && tile.getLinked().block().isConfigurable(tile)));
	}
	
	public boolean cursorNear(){
		return Vector2.dst(player.x, player.y, getBlockX() * tilesize, getBlockY() * tilesize) <= placerange ||
				state.mode.infiniteResources;
	}
	
	public boolean tryPlaceBlock(int x, int y, boolean sound){
		if(recipe != null && 
				validPlace(x, y, recipe.result) && !ui.hasMouse() && cursorNear() &&
				state.inventory.hasItems(recipe.requirements)){
			
			placeBlock(x, y, recipe.result, rotation, true, sound);
			
			for(ItemStack stack : recipe.requirements){
				state.inventory.removeItem(stack);
			}

			return true;
		}
		return false;
	}
	
	public boolean tryDeleteBlock(int x, int y, boolean sound){
		if(cursorNear() && validBreak(x, y)){
			breakBlock(x, y, sound);
			return true;
		}
		return false;
	}
	
	public boolean round2(){
		return !(recipe != null && recipe.result.isMultiblock() && recipe.result.height % 2 == 0);
	}
	
	public boolean validPlace(int x, int y, Block type){
		return Placement.validPlace(x, y, type);
	}
	
	public boolean validBreak(int x, int y){
		return Placement.validBreak(x, y);
	}
	
	public void placeBlock(int x, int y, Block result, int rotation, boolean effects, boolean sound){
		if(!Net.client()){ //is server or singleplayer
			Placement.placeBlock(x, y, result, rotation, effects, sound);
		}

		if(Net.active()){
			NetEvents.handlePlace(x, y, result, rotation);
		}

		if(!Net.client()){
			Tile tile = world.tile(x, y);
			if(tile != null) result.placed(tile);
			//確定したプレイヤーの配置(オフラインまたはホスト)。マップ生成とセーブ読み込みは Placement を直接呼ぶのでここを通らない
			Events.fire(PlaceEvent.class, x, y, result.id, rotation);
		}
	}

	public void breakBlock(int x, int y, boolean sound){
		if(!Net.client()){
			Block broken = Placement.breakBlock(x, y, true, sound);
			//確定したプレイヤーの撤去(オフラインまたはホスト)。タイルが無ければ null で、何も壊していない
			if(broken != null) Events.fire(BreakEvent.class, x, y, broken.id);
		}

		if(Net.active()){
			NetEvents.handleBreak(x, y);
		}
	}
}
