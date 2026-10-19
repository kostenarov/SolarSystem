package io.kostenarov.SolarSystem;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Game;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game implements ApplicationListener {
    @Override
    public void create() {
        setScreen(new FirstScreen());
    }
}
