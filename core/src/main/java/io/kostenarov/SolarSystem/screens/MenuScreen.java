package io.kostenarov.SolarSystem.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.kostenarov.SolarSystem.Main;
import io.kostenarov.SolarSystem.render.StarField;
import io.kostenarov.SolarSystem.system.BodyConfig;
import io.kostenarov.SolarSystem.system.SystemFiles;
import io.kostenarov.SolarSystem.ui.SettingsWindow;
import io.kostenarov.SolarSystem.ui.SystemEditorWindow;
import io.kostenarov.SolarSystem.ui.UiSkin;

/**
 * Shown before the simulation: pick a saved system to run, create or edit one in the system
 * editor, or change the settings.
 */
public class MenuScreen extends ScreenAdapter implements SystemEditorWindow.Listener, SettingsWindow.Listener {
    private static final Color ERROR_COLOR = new Color(1f, 0.45f, 0.45f, 1f);
    private static final Color INFO_COLOR = new Color(0.75f, 0.75f, 0.75f, 1f);

    private final Main game;
    private final Skin skin;
    private final Stage stage;
    private final StarField starField = new StarField();
    private final OrthographicCamera backgroundCamera = new OrthographicCamera();

    private final List<String> savesList;
    private final Label statusLabel;
    private final SettingsWindow settingsWindow;
    private final SystemEditorWindow editorWindow;

    public MenuScreen(Main game) {
        this.game = game;
        this.skin = game.getSkin();
        stage = new Stage(new ScreenViewport());
        UiSkin.clearTextFocusOnOutsideClick(stage);

        Label title = new Label("Solar System Simulator", skin);
        title.setFontScale(2f);
        Label subtitle = new Label("Pick a system to simulate, or build your own.", skin);
        subtitle.setColor(INFO_COLOR);

        savesList = new List<>(skin);
        // Double-click a save to start it straight away
        savesList.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (getTapCount() == 2) startSelected();
            }
        });
        ScrollPane savesScroll = new ScrollPane(savesList, skin);
        savesScroll.setFadeScrollBars(false);
        savesScroll.setScrollingDisabled(true, false);

        Table buttons = new Table();
        buttons.defaults().width(220).height(34).padBottom(8);
        buttons.add(button("Start simulation", this::startSelected)).row();
        buttons.add(button("Edit selected system", this::editSelected)).row();
        buttons.add(button("Create new system", this::createNew)).row();
        buttons.add(button("Settings", this::openSettings)).padTop(16).row();
        buttons.add(button("Quit", () -> Gdx.app.exit())).row();

        statusLabel = new Label("", skin);
        statusLabel.setWrap(true);

        Label savesHeading = new Label("Saved systems", skin);
        Label folderHint = new Label("Saves are kept in " + SystemFiles.savesFolderPath(), skin);
        folderHint.setColor(INFO_COLOR);
        folderHint.setFontScale(0.85f);
        folderHint.setWrap(true);

        Table panel = new Table(skin);
        panel.setBackground(skin.newDrawable("white", new Color(0.08f, 0.08f, 0.1f, 0.9f)));
        panel.pad(24);
        panel.defaults().left();
        panel.add(title).colspan(2).padBottom(4).row();
        panel.add(subtitle).colspan(2).padBottom(20).row();
        panel.add(savesHeading).padBottom(6);
        panel.add().row();
        panel.add(savesScroll).width(320).height(260).top();
        panel.add(buttons).top().padLeft(20).row();
        panel.add(folderHint).colspan(2).width(560).padTop(8).row();
        panel.add(statusLabel).colspan(2).width(560).padTop(8);

        Table root = new Table();
        root.setFillParent(true);
        root.add(panel);
        stage.addActor(root);

        settingsWindow = new SettingsWindow(skin, game.getSettings(), this);
        settingsWindow.setVisible(false);
        stage.addActor(settingsWindow);

        editorWindow = new SystemEditorWindow(skin, this);
        editorWindow.setVisible(false);
        stage.addActor(editorWindow);

        refreshSaves(null);
    }

    private void refreshSaves(String select) {
        Array<String> saves = SystemFiles.listSaves();
        savesList.setItems(saves);
        if (select != null && saves.contains(select, false)) {
            savesList.setSelected(select);
        }
        if (saves.size == 0) {
            setStatus("No saved systems yet. Use Create new system to make one.", false);
        }
    }

    private void startSelected() {
        BodyConfig[] configs = readSelected();
        if (configs == null) return;
        String problem = SystemFiles.validate(configs);
        if (problem != null) {
            setStatus(savesList.getSelected() + " can't be simulated: " + problem + ". Open it in the editor to fix it.", true);
            return;
        }
        game.startSimulation(configs, savesList.getSelected());
    }

    private void editSelected() {
        BodyConfig[] configs = readSelected();
        if (configs == null) return;
        editorWindow.open(configs, savesList.getSelected());
    }

    private void createNew() {
        editorWindow.openNew();
    }

    private BodyConfig[] readSelected() {
        String name = savesList.getSelected();
        if (name == null) {
            setStatus("Select a saved system first, or create a new one.", true);
            return null;
        }
        try {
            return SystemFiles.read(name);
        } catch (Exception e) {
            setStatus("Could not read " + name + ": " + e.getMessage(), true);
            return null;
        }
    }

    private void openSettings() {
        settingsWindow.refresh();
        settingsWindow.setPosition((stage.getWidth() - settingsWindow.getWidth()) / 2,
            (stage.getHeight() - settingsWindow.getHeight()) / 2);
        settingsWindow.setVisible(true);
        settingsWindow.toFront();
    }

    @Override
    public void onSettingsClosed() {
        game.saveSettings();
    }

    @Override
    public void onSystemApplied(BodyConfig[] configs, String name) {
        game.startSimulation(configs, name);
    }

    @Override
    public void onSystemSaved(String name) {
        refreshSaves(name);
        setStatus("Saved " + name + ".", false);
    }

    private TextButton button(String text, final Runnable action) {
        TextButton button = new TextButton(text, skin);
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        });
        return button;
    }

    private void setStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.setColor(error ? ERROR_COLOR : INFO_COLOR);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Slow drift across the background stars
        backgroundCamera.position.x += delta * 20f;
        starField.render(backgroundCamera);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        stage.getViewport().update(width, height, true);
        backgroundCamera.setToOrtho(false, width, height);
    }

    @Override
    public void dispose() {
        stage.dispose();
        starField.dispose();
    }
}
