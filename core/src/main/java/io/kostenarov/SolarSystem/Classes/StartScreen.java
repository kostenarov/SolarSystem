package io.kostenarov.SolarSystem.Classes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Consumer;

public class StartScreen {
    private final Stage stage;
    private final Table root;
    private final Skin skin;

    private SelectBox<String> buildBox;

    public StartScreen(Stage stage, Runnable onStart, Runnable onLoadExample, Runnable onExit) {
        this(stage, onStart, Collections.<String>emptyList(), sel -> {
            if (onLoadExample != null) onLoadExample.run();
        }, onExit);
    }

    public StartScreen(Stage stage, Runnable onStart, java.util.List<String> builds, Consumer<String> onLoadExample, Runnable onExit) {
        this.stage = stage;

        skin = new Skin();
        BitmapFont font = new BitmapFont();
        skin.add("default-font", font);

        Pixmap pix = new Pixmap(1,1, Pixmap.Format.RGBA8888);
        pix.setColor(1f, 1f, 1f, 1f);
        pix.fill();
        Texture pxTex = new Texture(pix);
        pix.dispose();
        TextureRegionDrawable plain = new TextureRegionDrawable(pxTex);

        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.up = plain.tint(Color.DARK_GRAY);
        btnStyle.down = plain.tint(Color.CLEAR_WHITE);
        btnStyle.over = plain.tint(Color.GRAY);
        btnStyle.font = font;
        skin.add("default", btnStyle);

        Label.LabelStyle lblStyle = new Label.LabelStyle(font, Color.WHITE);
        skin.add("default", lblStyle);

        List.ListStyle listStyle = new List.ListStyle();
        listStyle.font = font;
        listStyle.fontColorSelected = Color.WHITE;
        listStyle.fontColorUnselected = Color.LIGHT_GRAY;
        listStyle.selection = plain.tint(Color.DARK_GRAY);
        listStyle.background = plain.tint(Color.DARK_GRAY);
        listStyle.over = plain.tint(Color.GRAY);

        ScrollPane.ScrollPaneStyle spStyle = new ScrollPane.ScrollPaneStyle();
        spStyle.background = plain.tint(Color.DARK_GRAY);

        SelectBox.SelectBoxStyle sbStyle = new SelectBox.SelectBoxStyle();
        sbStyle.font = font;
        sbStyle.fontColor = Color.WHITE;
        sbStyle.background = plain.tint(Color.DARK_GRAY);
        sbStyle.listStyle = listStyle;
        sbStyle.scrollStyle = spStyle;
        skin.add("default", sbStyle);

        root = new Table();
        root.setFillParent(true);

        Table bg = new Table(skin);
        bg.setFillParent(true);

        Label title = new Label("Solar System Visualiser", skin);
        title.setFontScale(2f);

        TextButton startBtn = new TextButton("Start", skin);
        TextButton loadBtn = new TextButton("Load Example", skin);
        TextButton exitBtn = new TextButton("Exit", skin);

        buildBox = new SelectBox<>(skin);
        if (builds != null && !builds.isEmpty()) {
            buildBox.setItems(builds.toArray(new String[0]));
        } else {
            java.util.List<String> auto = findBuildsInSaves();
            if (!auto.isEmpty()) {
                buildBox.setItems(auto.toArray(new String[0]));
            } else {
                buildBox.setItems(new String[] {"Default"});
            }
        }
        buildBox.setSelectedIndex(0);

        startBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hide();
                if (onStart != null) onStart.run();
            }
        });

        loadBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hide();
                if (onLoadExample != null) {
                    String selected = buildBox.getSelected();
                    onLoadExample.accept(selected);
                }
            }
        });

        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (onExit != null) onExit.run();
            }
        });

        root.add(title).padBottom(40f).row();
        root.add(startBtn).width(320f).height(56f).pad(8f).row();
        root.add(buildBox).width(320f).height(56f).pad(8f).row();
        root.add(loadBtn).width(320f).height(56f).pad(8f).row();
        root.add(exitBtn).width(320f).height(56f).pad(8f).row();

        bg.add(root).center();

        stage.addActor(bg);
        root.setName("startScreenRoot");
    }

    public void hide() {
        if (stage.getRoot().findActor("startScreenRoot") != null) {
            stage.getRoot().findActor("startScreenRoot").remove();
        }
        skin.dispose();
    }

    public void selectBuild(String nameOrPath) {
        if (nameOrPath == null || buildBox == null) return;
        String target = nameOrPath;
        int idx = target.lastIndexOf(File.separatorChar);
        if (idx >= 0) target = target.substring(idx + 1);
        idx = target.lastIndexOf('/');
        if (idx >= 0) target = target.substring(idx + 1);

        String[] items = buildBox.getItems().toArray(String.class);
        for (int i = 0; i < items.length; i++) {
            if (items[i].equals(target) || items[i].equals(nameOrPath) || nameOrPath.endsWith(items[i])) {
                buildBox.setSelectedIndex(i);
                return;
            }
        }
    }

    private java.util.List<String> findBuildsInSaves() {
        java.util.List<String> found = new ArrayList<>();
        try {
            File savesDir = new File("saves");
            if (savesDir.exists() && savesDir.isDirectory()) {
                File[] files = savesDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
                if (files != null) {
                    for (File f : files) {
                        found.add(f.getName());
                    }
                }
            }
        } catch (SecurityException se) {
        }
        return found;
    }
}
