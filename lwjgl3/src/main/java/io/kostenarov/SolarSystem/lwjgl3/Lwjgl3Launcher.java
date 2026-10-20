package io.kostenarov.SolarSystem.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import io.kostenarov.SolarSystem.Main;

/**
 * Options: {@code --system <save name>} skips the menu and simulates that save straight away;
 * {@code --select <body name>} also selects that body.
 */
public class Lwjgl3Launcher {
    public static void main (String[] arg) {
        String system = null, select = null;
        for (int i = 0; i + 1 < arg.length; i++) {
            if ("--system".equals(arg[i])) system = arg[++i];
            else if ("--select".equals(arg[i])) select = arg[++i];
        }

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Solar System Simulation");
        config.setWindowIcon("textures/deathstar.png");
        config.setWindowedMode(1280, 720);
        config.useVsync(true);
        config.setForegroundFPS(120);
        new Lwjgl3Application(new Main(system, select), config);
    }
}
