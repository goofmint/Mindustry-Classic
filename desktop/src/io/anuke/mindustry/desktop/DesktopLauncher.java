// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import io.anuke.mindustry.Mindustry;
import io.anuke.mindustry.core.Platform;

public class DesktopLauncher {
	
	public static void main (String[] arg) {
		
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setTitle("Mindustry Classic");
		config.setMaximized(true);
		config.setWindowedMode(960, 540);
		config.setWindowIcon("sprites/icon.png");

        Platform.instance = new DesktopPlatform(arg);


		try {
			new Lwjgl3Application(new Mindustry(), config);
		}catch (Throwable e){
		    CrashHandler.handle(e);
		}
	}
}
