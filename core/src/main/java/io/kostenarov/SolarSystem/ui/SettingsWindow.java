package io.kostenarov.SolarSystem.ui;

import io.kostenarov.SolarSystem.SimulationSettings;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;

/** Sliders for {@link SimulationSettings}. Changes apply immediately; closing the window saves them. */
public class SettingsWindow extends Window {

    public interface Listener {
        void onSettingsClosed();
    }

    // Each slider runs 0..1 and maps onto its setting; most map logarithmically because the
    // useful ranges cover several orders of magnitude
    private abstract static class SettingRow {
        Slider slider;
        Label valueLabel;

        abstract float toSlider();
        abstract void fromSlider(float t);
        abstract String format();
    }

    private final SimulationSettings settings;
    private final Array<SettingRow> rows = new Array<>();

    public SettingsWindow(Skin skin, SimulationSettings settings, Listener listener) {
        super("Settings", skin);
        this.settings = settings;
        defaults().pad(4);
        setResizable(false);

        addRow(skin, "Simulation speed", "How much simulated time passes per real second", new SettingRow() {
            float toSlider() { return toLog(settings.timeScale, 360, 30 * 86400); }
            void fromSlider(float t) { settings.timeScale = fromLog(t, 360, 30 * 86400); }
            String format() { return Format.duration(settings.timeScale) + " / s"; }
        });
        addRow(skin, "Integration step", "Smaller is more accurate but slower; large steps break Euler first", new SettingRow() {
            float toSlider() { return toLog(settings.integrationStep, 10, 2 * 86400); }
            void fromSlider(float t) { settings.integrationStep = fromLog(t, 10, 2 * 86400); }
            String format() { return Format.duration(settings.integrationStep); }
        });
        addRow(skin, "Trail length", "Points of trail kept per body", new SettingRow() {
            float toSlider() { return toLog(settings.trailLength, 10, 100000); }
            void fromSlider(float t) { settings.trailLength = (int) Math.round(fromLog(t, 10, 100000)); }
            String format() { return settings.trailLength + " points"; }
        });
        addRow(skin, "Body size", "Drawn radius as a multiple of the real radius (x 1 is true size)", new SettingRow() {
            float toSlider() { return toLog(settings.bodySizeScale, 1, 1000); }
            void fromSlider(float t) { settings.bodySizeScale = (float) fromLog(t, 1, 1000); }
            String format() { return "x " + Format.f("%.3g", settings.bodySizeScale); }
        });
        addRow(skin, "Minimum body size", "Bodies are never drawn smaller than this", new SettingRow() {
            float toSlider() { return (settings.minBodyPixels - 1) / 19f; }
            void fromSlider(float t) { settings.minBodyPixels = Math.round(1 + t * 19); }
            String format() { return (int) settings.minBodyPixels + " px"; }
        });

        TextButton defaultsButton = new TextButton("Reset to defaults", skin);
        defaultsButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                settings.resetToDefaults();
                refresh();
            }
        });
        TextButton closeButton = new TextButton("Done", skin, "accent");
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                setVisible(false);
                listener.onSettingsClosed();
            }
        });
        row().padTop(10);
        add(defaultsButton).left();
        add(closeButton).right().colspan(2);

        refresh();
        pack();
    }

    private void addRow(Skin skin, String name, String hint, final SettingRow row) {
        row.slider = new Slider(0, 1, 0.001f, false, skin);
        row.valueLabel = new Label("", skin);
        row.slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                row.fromSlider(row.slider.getValue());
                row.valueLabel.setText(row.format());
            }
        });

        Label hintLabel = new Label(hint, skin, "small");

        row();
        add(new Label(name, skin)).left();
        add(row.slider).width(220);
        add(row.valueLabel).width(130).left();
        row();
        add(hintLabel).colspan(3).left().padTop(0).padBottom(6);
        rows.add(row);
    }

    /** Moves the sliders to match the current settings. */
    public void refresh() {
        for (SettingRow row : rows) {
            row.slider.setProgrammaticChangeEvents(false);
            row.slider.setValue(row.toSlider());
            row.slider.setProgrammaticChangeEvents(true);
            row.valueLabel.setText(row.format());
        }
    }

    private static float toLog(double value, double min, double max) {
        return (float) (Math.log(value / min) / Math.log(max / min));
    }

    private static double fromLog(float t, double min, double max) {
        return min * Math.pow(max / min, t);
    }

}
