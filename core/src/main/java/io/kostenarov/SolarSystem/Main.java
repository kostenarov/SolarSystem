package io.kostenarov.SolarSystem;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import io.kostenarov.SolarSystem.screens.MenuScreen;
import io.kostenarov.SolarSystem.screens.SimulationScreen;
import io.kostenarov.SolarSystem.system.BodyConfig;
import io.kostenarov.SolarSystem.ui.UiSkin;

/** Starts on the menu, where a system is chosen or built, then switches to the simulation. */
public class Main extends Game {
    private final SimulationSettings settings = new SimulationSettings();
    private Skin skin;

    private interface ScreenFactory {
        Screen create();
    }

    @Override
    public void create() {
        settings.load(preferences());
        skin = UiSkin.create();
        setScreen(new MenuScreen(this));
    }

    public Skin getSkin() {
        return skin;
    }

    public SimulationSettings getSettings() {
        return settings;
    }

    public void saveSettings() {
        settings.save(preferences());
    }

    public void showMenu() {
        switchTo(() -> new MenuScreen(this));
    }

    public void startSimulation(final BodyConfig[] configs, final String name) {
        switchTo(() -> new SimulationScreen(this, configs, name));
    }

    // Screens switch when one of their own buttons is clicked, in the middle of their render.
    // Waiting for the next frame means the old screen is never disposed while it is still drawing.
    private void switchTo(final ScreenFactory factory) {
        Gdx.app.postRunnable(() -> {
            Screen old = getScreen();
            setScreen(factory.create());
            if (old != null) old.dispose();
        });
    }

    private Preferences preferences() {
        return Gdx.app.getPreferences(SimulationSettings.PREFERENCES_NAME);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (getScreen() != null) getScreen().dispose();
        saveSettings();
        skin.dispose();
    }
}
