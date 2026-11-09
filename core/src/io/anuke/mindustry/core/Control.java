// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.core;

import com.badlogic.gdx.*;
import com.badlogic.gdx.Input.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.core.GameState.*;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.input.*;
import io.anuke.mindustry.io.*;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.resource.*;
import io.anuke.mindustry.world.*;
import io.anuke.ucore.*;
import io.anuke.ucore.core.*;
import io.anuke.ucore.core.Inputs.*;
import io.anuke.ucore.entities.*;
import io.anuke.ucore.modules.*;
import io.anuke.ucore.modules.Module;
import io.anuke.ucore.scene.ui.layout.*;
import io.anuke.ucore.util.*;

import static io.anuke.mindustry.Vars.*;

/**Control module.
 * Handles all input, saving, keybinds and keybinds.
 * Should <i>not</i> handle any game-critical state.
 * This class is not created in the headless server.*/
public class Control extends Module{
	private UpgradeInventory upgrades = new UpgradeInventory();
	private boolean hiscore = false;
	private boolean bestscore = false;

	private boolean wasPaused = false;

	private Saves saves;

	private float respawntime;
	private InputHandler input;

    private Throwable error;

	public Control(){
		saves = new Saves();

		Gdx.input.setCatchKey(Keys.BACK, true);

		if(mobile){
			input = new AndroidInput();
		}else{
			input = new DesktopInput();
		}

		Inputs.addProcessor(input);

		Effects.setShakeFalloff(10000f);

		Core.atlas = new Atlas("sprites.atlas");

		//Atlas.findRegion() は無い領域に error 画像を返すので、init() の前に領域の有無で検査して止める
		Array<String> missingIcons = Item.missingIcons(Core.atlas::hasRegion);
		if(missingIcons.size > 0){
			throw new RuntimeException("Missing item icons: " + missingIcons.toString(", "));
		}

		for(Item item : Item.getAllItems()){
			item.init();
		}

		Array<String> nullRegions = new Array<>();
		for(Item item : Item.getAllItems()){
			if(item.region == null){
				nullRegions.add("icon-" + item.name);
			}
		}
		if(nullRegions.size > 0){
			throw new RuntimeException("Missing item icons: " + nullRegions.toString(", "));
		}

		Sounds.load("shoot.mp3", "place.mp3", "explosion.mp3", "enemyshoot.mp3",
				"corexplode.mp3", "break.mp3", "spawn.mp3", "flame.mp3", "die.mp3",
				"respawn.mp3", "purchase.mp3", "flame2.mp3", "bigshot.mp3", "laser.mp3", "lasershot.mp3",
				"ping.mp3", "tesla.mp3", "waveend.mp3", "railgun.mp3", "blast.mp3", "bang2.mp3");

		Sounds.setFalloff(9000f);

        Musics.load("1.mp3", "2.mp3", "3.mp3", "4.mp3", "5.mp3", "6.mp3");

        DefaultKeybinds.load();

		for(int i = 0; i < saveSlots; i ++){
			Settings.defaults("save-" + i + "-autosave", !gwt);
			Settings.defaults("save-" + i + "-name", "untitled");
			Settings.defaults("save-" + i + "-data", "empty");
		}

		Settings.defaultList(
			"ip", "localhost",
			"port", port+"",
			"name", mobile || gwt ? "player" : UCore.getProperty("user.name"),
			"servers", "mindustry.kr:6570|||",
			"color", Color.rgba8888(playerColors[8]),
			"lastVersion", "3.2",
			"lastBuild", 0
		);

		KeyBinds.load();

		for(Map map : world.maps().list()){
			Settings.defaults("hiscore" + map.name, 0);
			Settings.defaults("bestscore" + map.name, 0);
		}

		player = new Player();
		player.name = Settings.getString("name");
		player.isAndroid = mobile;
		player.color.set(Settings.getInt("color"));
		player.isLocal = true;

		saves.load();

		Events.on(StateChangeEvent.class, (from, to) -> {
			if((from == State.playing && to == State.menu) || (from == State.menu && to != State.menu)){
				Timers.runTask(5f, Platform.instance::updateRPC);
			}
		});

		Events.on(PlayEvent.class, () -> {
			renderer.clearTiles();

			player.set(world.getSpawnX(), world.getSpawnY());

			Core.camera.position.set(player.x, player.y, 0);

			ui.hudfrag.updateItems();

			state.set(State.playing);
		});

		Events.on(ResetEvent.class, () -> {
			upgrades.reset();
			player.weaponLeft = player.weaponRight = Weapon.blaster;

			player.add();
			player.heal();

			respawntime = -1;
			hiscore = false;
			bestscore = false;

			ui.hudfrag.updateItems();
			ui.hudfrag.updateWeapons();
			ui.hudfrag.fadeRespawn(false);
		});

		Events.on(WaveEvent.class, () -> {
			Sounds.play("spawn");

			int last = Settings.getInt("hiscore" + world.getMap().name, 0);

			if(state.wave > last && !state.mode.infiniteResources && !state.mode.disableWaveTimer){
				Settings.putInt("hiscore" + world.getMap().name, state.wave);
				Settings.save();
				hiscore = true;
			}

			Platform.instance.updateRPC();
		});

		Events.on(GameOverEvent.class, () -> {
			int balance = state.score.balance();
			if(!state.mode.infiniteResources && !state.mode.disableWaveTimer
					&& balance > Settings.getInt("bestscore" + world.getMap().name, 0)){
				Settings.putInt("bestscore" + world.getMap().name, balance);
				Settings.save();
				bestscore = true;
			}

			//目標到達の勝ちではコアは壊れていないので、爆発を出さない
			if(!state.won){
				Effects.shake(5, 6, Core.camera.position.x, Core.camera.position.y);
				Sounds.play("corexplode");
				for(int i = 0; i < 16; i ++){
					Timers.run(i*2, ()-> Effects.effect(Fx.explosion, world.getCore().worldx()+Mathf.range(40), world.getCore().worldy()+Mathf.range(40)));
				}
				Effects.effect(Fx.coreexplosion, world.getCore().worldx(), world.getCore().worldy());
			}

			ui.restart.show();

			Timers.runTask(30f, () -> state.set(State.menu));
		});
	}

	public void triggerInputUpdate(){
	}

	public void setError(Throwable error){
		this.error = error;
	}

	public UpgradeInventory upgrades() {
		return upgrades;
	}

	public Saves getSaves(){
		return saves;
	}

	public InputHandler input(){
		return input;
	}

	public void playMap(Map map){
		ui.loadfrag.show();
		saves.resetSave();

		Timers.runTask(10, () -> {
			logic.reset();
			world.loadMap(map);
			logic.play();
		});

		Timers.runTask(18, () -> ui.loadfrag.hide());
	}

	public boolean isHighScore(){
		return hiscore;
	}

	public boolean isBestScore(){
		return bestscore;
	}

	public float getRespawnTime(){
		return respawntime;
	}

	public void setRespawnTime(float respawntime){
		this.respawntime = respawntime;
	}

	@Override
	public void dispose(){
		Platform.instance.onGameExit();
		Net.dispose();
	}

	@Override
	public void pause(){
		wasPaused = state.is(State.paused);
		if(state.is(State.playing)) state.set(State.paused);
	}

	@Override
	public void resume(){
		if(state.is(State.paused) && !wasPaused){
            state.set(State.playing);
		}
	}

	@Override
	public void init(){
		Timers.run(1f, Musics::shuffleAll);

		Entities.initPhysics();
		Entities.collisions().setCollider(tilesize, world::solid);

		Platform.instance.updateRPC();
	}

	@Override
	public void update(){

		if(error != null){
			throw new RuntimeException(error);
		}

        if(Inputs.keyTap("console")){
			console = !console;
		}

        saves.update();

		if(state.inventory.isUpdated() && (Timers.get("updateItems", 8) || state.is(State.paused))){
			ui.hudfrag.updateItems();
			state.inventory.setUpdated(false);
		}

		if(!state.is(State.menu)){
			input.update();

			if(Inputs.keyTap("pause") && !ui.restart.isShown() && (state.is(State.paused) || state.is(State.playing))){
                state.set(state.is(State.playing) ? State.paused : State.playing);
			}

			if(Inputs.keyTap("menu")){
				if(state.is(State.paused)){
					ui.paused.hide();
                    state.set(State.playing);
				}else if (!ui.restart.isShown()){
					if(ui.chatfrag.chatOpen()) {
						ui.chatfrag.hide();
					}else{
						ui.paused.show();
                        state.set(State.paused);
					}
				}
			}

			if(!state.is(State.paused) || Net.active()){
				Entities.update(effectGroup);

				if(respawntime > 0){

					respawntime -= delta();

					if(respawntime <= 0){
						player.set(world.getSpawnX(), world.getSpawnY());
						player.heal();
						player.add();
						Effects.sound("respawn");
						ui.hudfrag.fadeRespawn(false);
					}
				}
			}
		}else{
			if(!state.is(State.paused) || Net.active()){
				Timers.update();
			}
		}
	}
}
