package io.kostenarov.SolarSystem.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.kostenarov.SolarSystem.SimulationSettings;
import io.kostenarov.SolarSystem.physics.CelestialBody;
import io.kostenarov.SolarSystem.physics.OrbitInfo;

/**
 * The controls over the simulation: a top bar (menu, body list, settings, elapsed time), a bottom
 * bar (pause, speed, integrator, display toggles) and an info panel for the selected body.
 */
public class SimulationHud {
    private static final double MIN_TIME_SCALE = 360;
    private static final double MAX_TIME_SCALE = 30 * 86400;
    private static final float INFO_REFRESH_SECONDS = 0.1f;

    public interface Listener extends SettingsWindow.Listener {
        /** A body was picked from the body list: select it and follow it. */
        void onBodyPicked(CelestialBody body);

        void onFollowToggled(CelestialBody body);

        boolean isFollowing(CelestialBody body);

        /** The info panel was closed. */
        void onSelectionCleared();

        /** "Menu" was clicked: leave the simulation and go back to the start menu. */
        void onMenuRequested();
    }

    public enum IntegratorMode {
        VERLET("Verlet"),
        EULER("Euler"),
        BOTH("Verlet + Euler");

        private final String label;

        IntegratorMode(String label) {
            this.label = label;
        }
    }

    private final Stage stage;
    private final Skin skin;
    private final Listener listener;
    private final SimulationSettings settings;
    private final Array<CelestialBody> bodies;

    private final TextButton bodiesButton;
    private final ScrollPane bodiesPanel;
    private final Label clockLabel;
    private final TextButton pauseButton;
    private final Label speedLabel;
    private final TextButton integratorButton;
    private final Label comparisonLegend;
    private final SettingsWindow settingsWindow;

    private final Window infoWindow;
    private final Image infoSwatch;
    private final Label infoType;
    private final Table infoRows;
    private final TextButton followButton;
    private CelestialBody infoBody;
    private float infoRefreshTimer;

    private boolean paused = false;
    private boolean displayingMoonTrails = true;
    private boolean displayingStars = true;
    private IntegratorMode integratorMode = IntegratorMode.VERLET;

    public SimulationHud(Skin skin, String systemName, Array<CelestialBody> bodies, SimulationSettings settings, Listener listener) {
        this.skin = skin;
        this.listener = listener;
        this.settings = settings;
        this.bodies = bodies;
        stage = new Stage(new ScreenViewport());
        UiSkin.clearTextFocusOnOutsideClick(stage);

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // ---- Top bar ----
        bodiesButton = new TextButton("Bodies", skin, "flat");
        TextButton settingsButton = new TextButton("Settings", skin, "flat");
        clockLabel = new Label("", skin);
        clockLabel.setColor(UiSkin.MUTED_TEXT);
        Label titleLabel = new Label(systemName, skin, "subtitle");

        Table top = new Table();
        top.setBackground(skin.getDrawable("bar"));
        top.pad(4, 6, 4, 12);
        top.add(button("Menu", "flat", listener::onMenuRequested));
        top.add(bodiesButton);
        top.add(settingsButton);
        top.add(titleLabel).expandX().center();
        top.add(clockLabel).width(170).right();
        clockLabel.setAlignment(Align.right);
        root.add(top).growX().row();

        // ---- Middle: body list on the left, info panel on the right ----
        Table bodyTable = new Table();
        bodyTable.top().left();
        for (CelestialBody body : bodies) {
            bodyTable.add(bodyRow(body)).growX().row();
        }
        bodiesPanel = new ScrollPane(bodyTable, skin, "panel");
        bodiesPanel.setFadeScrollBars(false);
        bodiesPanel.setScrollingDisabled(true, false);
        bodiesPanel.setVisible(false);
        bodiesButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                bodiesPanel.setVisible(bodiesButton.isChecked());
            }
        });

        infoSwatch = new Image(skin.newDrawable("white"));
        infoType = new Label("", skin);
        infoType.setColor(UiSkin.MUTED_TEXT);
        infoRows = new Table();
        infoRows.defaults().left().padBottom(3);
        followButton = new TextButton("Follow", skin, "accent");
        followButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (infoBody != null) listener.onFollowToggled(infoBody);
            }
        });

        infoWindow = new Window("", skin);
        infoWindow.setMovable(false);
        infoWindow.getTitleTable().add(button("X", "flat", listener::onSelectionCleared)).padRight(-8);
        Table header = new Table();
        header.add(infoSwatch).size(14).padRight(8);
        header.add(infoType).left().expandX();
        infoWindow.add(header).growX().padBottom(8).row();
        infoWindow.add(infoRows).growX().row();
        infoWindow.add(followButton).growX().padTop(10);
        infoWindow.setVisible(false);

        comparisonLegend = new Label("Solid: Verlet     Outline: Euler", skin, "small");
        comparisonLegend.setVisible(false);

        Table middle = new Table();
        middle.pad(10);
        middle.add(bodiesPanel).top().left().width(250).maxHeight(440);
        middle.add().expand();
        middle.add(infoWindow).top().right().width(350).row();
        middle.add(comparisonLegend).colspan(3).left().bottom();
        root.add(middle).grow().row();

        // ---- Bottom bar ----
        pauseButton = button("Pause", "default", this::togglePause);
        speedLabel = new Label("", skin);
        speedLabel.setAlignment(Align.center);
        integratorButton = new TextButton("", skin);
        integratorButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                IntegratorMode[] modes = IntegratorMode.values();
                integratorMode = modes[(integratorMode.ordinal() + 1) % modes.length];
                updateIntegratorText();
            }
        });
        updateIntegratorText();

        CheckBox starsBox = checkBox(" Stars", true);
        starsBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                displayingStars = starsBox.isChecked();
            }
        });
        CheckBox moonTrailsBox = checkBox(" Moon trails", true);
        moonTrailsBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                displayingMoonTrails = moonTrailsBox.isChecked();
            }
        });

        Label hint = new Label("Click a body for details  |  Drag to pan  |  Wheel to zoom  |  Space pause  |  F follow  |  R reset", skin, "small");
        hint.setEllipsis(true);
        hint.setAlignment(Align.right);

        Table bottom = new Table();
        bottom.setBackground(skin.getDrawable("bar"));
        bottom.pad(6, 10, 6, 12);
        bottom.defaults().padRight(6);
        bottom.add(pauseButton).width(84);
        bottom.add(button("<<", "default", () -> scaleTime(0.5)));
        bottom.add(speedLabel).width(110);
        bottom.add(button(">>", "default", () -> scaleTime(2)));
        bottom.add(integratorButton).padLeft(14);
        bottom.add(starsBox).padLeft(14);
        bottom.add(moonTrailsBox).padLeft(8);
        bottom.add(hint).expandX().fillX().minWidth(0).padLeft(14);
        root.add(bottom).growX();

        // ---- Settings window, over everything ----
        settingsWindow = new SettingsWindow(skin, settings, listener);
        settingsWindow.setVisible(false);
        stage.addActor(settingsWindow);
        settingsButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (settingsWindow.isVisible()) {
                    settingsWindow.setVisible(false);
                    listener.onSettingsClosed();
                    return;
                }
                settingsWindow.refresh();
                settingsWindow.setPosition((stage.getWidth() - settingsWindow.getWidth()) / 2,
                    (stage.getHeight() - settingsWindow.getHeight()) / 2);
                settingsWindow.setVisible(true);
                settingsWindow.toFront();
            }
        });
    }

    // One clickable row in the body list: colour, name, and what it is
    private Table bodyRow(final CelestialBody body) {
        final Table row = new Table(skin);
        row.setBackground("list-item-up");
        row.pad(4, 8, 4, 8);
        row.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        row.add(new Image(skin.newDrawable("white", body.getColor()))).size(10).padRight(8);
        row.add(new Label((body.isMoon ? "   " : "") + body.getName(), skin)).left().expandX();
        Label kind = new Label(body.isMoon ? "moon" : "", skin, "small");
        row.add(kind).right();
        row.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                listener.onBodyPicked(body);
                bodiesButton.setChecked(false);
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (pointer == -1) row.setBackground("list-item-over");
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (pointer == -1) row.setBackground("list-item-up");
            }
        });
        return row;
    }

    // ---- Info panel ----

    /** Shows the info panel for a body, or hides it for null. */
    public void showInfo(CelestialBody body) {
        infoBody = body;
        infoWindow.setVisible(body != null);
        if (body == null) return;
        infoWindow.getTitleLabel().setText(body.getName());
        infoSwatch.setColor(body.getColor().r, body.getColor().g, body.getColor().b, 1f);
        infoRefreshTimer = 0;
        refreshInfo();
    }

    public CelestialBody getInfoBody() {
        return infoBody;
    }

    private void refreshInfo() {
        CelestialBody body = infoBody;
        CelestialBody reference = referenceFor(body);

        String type;
        if (body.isMoon && reference != null) type = "Moon of " + reference.getName();
        else if (reference == null) type = "Heaviest body in the system";
        else type = "Orbits " + reference.getName();
        infoType.setText(type);

        infoRows.clearChildren();
        addInfoRow("Mass", Format.mass(body.getMass()));
        addInfoRow("Radius", Format.radius(body.getRadius()));
        if (reference != null) {
            OrbitInfo orbit = OrbitInfo.of(body, reference);
            addInfoRow("Distance", Format.distance(orbit.distance));
            addInfoRow("Speed", Format.speed(orbit.speed));
            if (orbit.bound) {
                addInfoRow("Orbital period", Format.duration(orbit.period));
                addInfoRow("Eccentricity", Format.f("%.3f", orbit.eccentricity));
                addInfoRow("Semi-major axis", Format.distance(orbit.semiMajorAxis));
            } else {
                addInfoRow("Orbit", "Escaping " + reference.getName());
            }
        } else {
            addInfoRow("Speed", Format.speed(body.getVelocity().len()));
        }
        if (body.hasRings()) addInfoRow("Rings", "Yes");

        followButton.setText(listener.isFollowing(body) ? "Stop following" : "Follow");
    }

    private void addInfoRow(String name, String value) {
        infoRows.add(new Label(name, skin, "small")).width(105);
        infoRows.add(new Label(value, skin)).left().expandX().row();
    }

    /** What the body orbits: a moon's parent, otherwise the heaviest body heavier than it. */
    private CelestialBody referenceFor(CelestialBody body) {
        if (body.orbitParent != null) return body.orbitParent;
        CelestialBody heaviest = null;
        for (CelestialBody b : bodies) {
            if (b == body || b.isMoon) continue;
            if (heaviest == null || b.getMass() > heaviest.getMass()) heaviest = b;
        }
        return heaviest != null && heaviest.getMass() > body.getMass() ? heaviest : null;
    }

    // ---- Bottom bar actions ----

    public void togglePause() {
        paused = !paused;
        pauseButton.setText(paused ? "Play" : "Pause");
    }

    private void scaleTime(double factor) {
        settings.timeScale = Math.max(MIN_TIME_SCALE, Math.min(MAX_TIME_SCALE, settings.timeScale * factor));
    }

    private void updateIntegratorText() {
        integratorButton.setText("Integrator: " + integratorMode.label);
        comparisonLegend.setVisible(integratorMode == IntegratorMode.BOTH);
    }

    // ---- Per-frame ----

    /** Updates the live labels and draws the HUD. */
    public void render(float delta, double elapsedSeconds) {
        clockLabel.setText("Elapsed: " + Format.elapsed(elapsedSeconds));
        speedLabel.setText(Format.duration(settings.timeScale) + " / s");

        if (infoBody != null) {
            infoRefreshTimer -= delta;
            if (infoRefreshTimer <= 0) {
                infoRefreshTimer = INFO_REFRESH_SECONDS;
                refreshInfo();
            }
        }

        stage.act(delta);
        stage.draw();
    }

    /** True while a text field has keyboard focus, so key presses are typing, not camera controls. */
    public boolean isTyping() {
        return stage.getKeyboardFocus() instanceof TextField;
    }

    public boolean isUiInFocus(float screenX, float screenY) {
        return stage.hit(screenX, Gdx.graphics.getHeight() - screenY, true) != null;
    }

    public boolean isOverScrollableArea(float screenX, float screenY) {
        Actor hit = stage.hit(screenX, Gdx.graphics.getHeight() - screenY, true);
        return hit != null && hit.isDescendantOf(bodiesPanel);
    }

    public Stage getStage() {
        return stage;
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public void dispose() {
        stage.dispose();
    }

    private TextButton button(String text, String style, final Runnable action) {
        TextButton button = new TextButton(text, skin, style);
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        });
        return button;
    }

    private CheckBox checkBox(String text, boolean checked) {
        CheckBox box = new CheckBox(text, skin);
        box.setChecked(checked);
        return box;
    }

    public boolean isPaused() { return paused; }
    public boolean isDisplayingMoonTrails() { return displayingMoonTrails; }
    public boolean isDisplayingStars() { return displayingStars; }
    public IntegratorMode getIntegratorMode() { return integratorMode; }
}
