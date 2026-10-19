package io.kostenarov.SolarSystem.Classes;

import com.badlogic.gdx.Preferences;

/** User-adjustable simulation and display settings, kept between runs in libGDX preferences. */
public class SimulationSettings {
    public static final String PREFERENCES_NAME = "io.kostenarov.SolarSystem.settings";

    public static final double DEFAULT_TIME_SCALE = 30 * 60 * 60; // 30 simulated hours per real second
    public static final double DEFAULT_INTEGRATION_STEP = 5 * 60;
    public static final int DEFAULT_TRAIL_LENGTH = 100000;
    public static final float DEFAULT_BODY_SIZE_SCALE = 1 / 400f;
    public static final float DEFAULT_MIN_BODY_PIXELS = 3f;

    /** Simulated seconds per real second. */
    public double timeScale = DEFAULT_TIME_SCALE;
    /** Longest single integration step in simulated seconds; smaller is more accurate but slower. */
    public double integrationStep = DEFAULT_INTEGRATION_STEP;
    /** Trail points kept per body. */
    public int trailLength = DEFAULT_TRAIL_LENGTH;
    /** Drawn radius = real radius * this. */
    public float bodySizeScale = DEFAULT_BODY_SIZE_SCALE;
    /** Bodies are never drawn smaller than this many pixels. */
    public float minBodyPixels = DEFAULT_MIN_BODY_PIXELS;

    public void resetToDefaults() {
        timeScale = DEFAULT_TIME_SCALE;
        integrationStep = DEFAULT_INTEGRATION_STEP;
        trailLength = DEFAULT_TRAIL_LENGTH;
        bodySizeScale = DEFAULT_BODY_SIZE_SCALE;
        minBodyPixels = DEFAULT_MIN_BODY_PIXELS;
    }

    public void load(Preferences prefs) {
        timeScale = prefs.getFloat("timeScale", (float) DEFAULT_TIME_SCALE);
        integrationStep = prefs.getFloat("integrationStep", (float) DEFAULT_INTEGRATION_STEP);
        trailLength = prefs.getInteger("trailLength", DEFAULT_TRAIL_LENGTH);
        bodySizeScale = prefs.getFloat("bodySizeScale", DEFAULT_BODY_SIZE_SCALE);
        minBodyPixels = prefs.getFloat("minBodyPixels", DEFAULT_MIN_BODY_PIXELS);
    }

    public void save(Preferences prefs) {
        prefs.putFloat("timeScale", (float) timeScale);
        prefs.putFloat("integrationStep", (float) integrationStep);
        prefs.putInteger("trailLength", trailLength);
        prefs.putFloat("bodySizeScale", bodySizeScale);
        prefs.putFloat("minBodyPixels", minBodyPixels);
        prefs.flush();
    }
}
