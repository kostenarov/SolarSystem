    package io.kostenarov.SolarSystem.Classes;

import com.badlogic.gdx.Gdx;
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
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;


public class StartScreen {
    private final Stage stage;
    private final Table root;
    private final Skin skin;

    public StartScreen(Stage stage, Runnable onStart, Runnable onLoadExample, Runnable onExit) {
        this.stage = stage;

        // Minimal skin using a generated texture and default font
        skin = new Skin();
        BitmapFont font = new BitmapFont();
        skin.add("default-font", font);

        Pixmap pix = new Pixmap(1,1, Pixmap.Format.RGBA8888);
        pix.setColor(1f, 1f, 1f, 1f);
        pix.fill();
        Texture pxTex = new Texture(pix);
        pix.dispose();
        TextureRegionDrawable plain = new TextureRegionDrawable(pxTex);

        // Button style
        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.up = plain.tint(Color.DARK_GRAY);
        btnStyle.down = plain.tint(Color.DARK_GRAY);
        btnStyle.font = font;
        skin.add("default", btnStyle);

        Label.LabelStyle lblStyle = new Label.LabelStyle(font, Color.WHITE);
        skin.add("default", lblStyle);

        root = new Table();
        root.setFillParent(true);

        // No background set here — remove tinting of the underlying screen
        Table bg = new Table(skin);
        bg.setFillParent(true);

        Label title = new Label("Solar System Visualiser", skin);
        title.setFontScale(2f);

        TextButton startBtn = new TextButton("Start", skin);
        TextButton loadBtn = new TextButton("Load Example", skin);
        TextButton exitBtn = new TextButton("Exit", skin);

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
                if (onLoadExample != null) onLoadExample.run();
            }
        });

        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (onExit != null) onExit.run();
            }
        });

        // Layout
        root.add(title).padBottom(40f).row();
        root.add(startBtn).width(320f).height(56f).pad(8f).row();
        root.add(loadBtn).width(320f).height(56f).pad(8f).row();
        root.add(exitBtn).width(320f).height(56f).pad(8f).row();

        bg.add(root).center();

        stage.addActor(bg);
        // keep reference to background wrapper so we can remove it on hide
        root.setName("startScreenRoot");
    }

    public void hide() {
        // remove all actors added by this screen
        if (stage.getRoot().findActor("startScreenRoot") != null) {
            stage.getRoot().findActor("startScreenRoot").remove();
        }
        skin.dispose();
    }
}
