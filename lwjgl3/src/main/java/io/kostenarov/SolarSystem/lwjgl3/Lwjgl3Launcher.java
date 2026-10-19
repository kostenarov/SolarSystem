package io.kostenarov.SolarSystem.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import io.kostenarov.SolarSystem.Main;

public class Lwjgl3Launcher {
    public static void main (String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Solar System Simulation");
        config.setWindowIcon("textures/deathstar.png");
        config.setWindowedMode(1280, 720);
        config.useVsync(true);
        config.setForegroundFPS(120);
        new Lwjgl3Application(new Main(), config);
    }
}
