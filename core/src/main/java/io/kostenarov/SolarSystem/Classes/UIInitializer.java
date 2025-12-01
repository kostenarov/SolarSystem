package io.kostenarov.SolarSystem.Classes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import org.w3c.dom.Text;

public class UIInitializer {

    private Stage stage;
    private Skin skin;
    private TextButton toggleButton;
    private ImageButton pauseButton;
    private Table dropdownContainer;
    private ScrollPane scrollPane;
    private Table bodyTable;
    private Array<CelestialBody> bodies;
    private BodySelectionListener listener;
    private Texture pauseTexture;
    private Texture bottomMenuTexture;
    private Image bottomMenuImage; // new field
    private CheckBox displayMoonTrailsCheckbox;
    private CheckBox isDisplayingStarsCheckbox;

    private boolean isPaused = false;
    private boolean isDisplayingMoonTrails = true;
    private boolean isDisplayingStars = true;

    public interface BodySelectionListener {
        void onBodySelected(CelestialBody body);
    }

    private class BodyListItem extends Table {
        private final CelestialBody body;

        public BodyListItem(CelestialBody body, Skin skin, BodySelectionListener listener, Table container) {
            super(skin);
            this.body = body;
            this.pad(5);
            this.setBackground("list-item-up");
            this.addListener(createItemClickListener(body, listener, container));

            Label nameLabel = new Label(body.getName(), skin);
            this.add(nameLabel).expandX().fillX().left();

            Image colorBox = new Image(skin.newDrawable("white", body.getColor()));
            this.add(colorBox).size(15).padLeft(10).right();
            this.row();
            this.setWidth(200);
        }

        private ClickListener createItemClickListener(final CelestialBody body, final BodySelectionListener listener, final Table container) {
            return new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    listener.onBodySelected(body);
                    container.setVisible(false);
                }

                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    if (pointer == -1) {
                        setBackground("list-item-over");
                    }
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    if (pointer == -1) {
                        setBackground("list-item-up");
                    }
                }

                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    setBackground("list-item-down");
                    return super.touchDown(event, x, y, pointer, button);
                }

                @Override
                public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                    setBackground("list-item-over");
                    super.touchUp(event, x, y, pointer, button);
                }
            };
        }
    }

    public UIInitializer(Array<CelestialBody> bodies, BodySelectionListener listener) {
        this.bodies = bodies;
        this.listener = listener;
        stage = new Stage(new ScreenViewport());
        skin = createBasicSkin();
        pauseTexture = createPauseButtonTexture();
        bottomMenuTexture = initBottomMenu();
        createDisplayMoonTrailsCheckbox();
        createDisplayStarsCheckbox();
        initializeUi();
        updateBodyList(bodies);
    }

    private Skin createBasicSkin() {
        Skin skin = new Skin();

        BitmapFont font = new BitmapFont();
        skin.add("default", font);

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));
        pixmap.dispose();

        skin.add("list-item-up", skin.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 1)), Drawable.class);
        skin.add("list-item-over", skin.newDrawable("white", Color.GRAY), Drawable.class);
        skin.add("list-item-down", skin.newDrawable("white", Color.LIGHT_GRAY), Drawable.class);

        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.up = skin.newDrawable("white", Color.DARK_GRAY);
        textButtonStyle.down = skin.newDrawable("white", Color.LIGHT_GRAY);
        textButtonStyle.over = skin.newDrawable("white", Color.GRAY);
        textButtonStyle.font = skin.getFont("default");
        textButtonStyle.fontColor = Color.WHITE;
        skin.add("default", textButtonStyle);

        ScrollPane.ScrollPaneStyle scrollPaneStyle = new ScrollPane.ScrollPaneStyle();
        scrollPaneStyle.background = skin.newDrawable("white", new Color(0.1f, 0.1f, 0.1f, 1));
        scrollPaneStyle.vScroll = skin.newDrawable("white", Color.GRAY);
        scrollPaneStyle.vScrollKnob = skin.newDrawable("white", Color.LIGHT_GRAY);
        skin.add("default", scrollPaneStyle);

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = skin.getFont("default");
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);

        CheckBox.CheckBoxStyle checkBoxStyle = new CheckBox.CheckBoxStyle();
        checkBoxStyle.checkboxOn = skin.newDrawable("white", Color.WHITE); // visible when checked
        checkBoxStyle.checkboxOff = skin.newDrawable("white", Color.DARK_GRAY); // visible when unchecked
        checkBoxStyle.font = skin.getFont("default");
        checkBoxStyle.fontColor = Color.WHITE;
        skin.add("default", checkBoxStyle);

        Slider.SliderStyle sliderStyle = new Slider.SliderStyle();
        sliderStyle.background = skin.newDrawable("white", new Color(0.15f, 0.15f, 0.15f, 1f));
        sliderStyle.knob = skin.newDrawable("white", Color.LIGHT_GRAY);
        sliderStyle.knobOver = skin.newDrawable("white", Color.WHITE);
        sliderStyle.knobDown = skin.newDrawable("white", Color.DARK_GRAY);
        skin.add("default-horizontal", sliderStyle);

        return skin;
    }

    private Texture createPauseButtonTexture() {
        Pixmap pixmap = new Pixmap(20, 20, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.RED);
        pixmap.fillRectangle(0, 0, 8, 20);
        pixmap.fillRectangle(12, 0, 8, 20);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private Texture initBottomMenu() {
        Pixmap pixmap = new Pixmap(Gdx.graphics.getWidth(), (int) (Gdx.graphics.getHeight() / 6.5), Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(100, 100, 100, 0.5f));
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private void createDisplayMoonTrailsCheckbox() {
        displayMoonTrailsCheckbox = new CheckBox(" Display Moon Trails", skin);
        displayMoonTrailsCheckbox.setPosition(10, Gdx.graphics.getHeight() / 6 + 20);
        displayMoonTrailsCheckbox.setChecked(true);
        displayMoonTrailsCheckbox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                isDisplayingMoonTrails = displayMoonTrailsCheckbox.isChecked();
            }
        });
        stage.addActor(displayMoonTrailsCheckbox);
    }

    private void createDisplayStarsCheckbox() {
        isDisplayingStarsCheckbox = new CheckBox(" Display Stars", skin);
        isDisplayingStarsCheckbox.setPosition(10, Gdx.graphics.getHeight() / 6 + 50);
        isDisplayingStarsCheckbox.setChecked(true);
        isDisplayingStarsCheckbox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                isDisplayingStars = isDisplayingStarsCheckbox.isChecked();
            }
        });
        stage.addActor(isDisplayingStarsCheckbox);
    }

    private void initializeUi() {
        bottomMenuImage = new Image(bottomMenuTexture);
        bottomMenuImage.setPosition(0, 0);
        bottomMenuImage.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight() / 6.5f);
        stage.addActor(bottomMenuImage);

        toggleButton = new TextButton("Select Body", skin);
        toggleButton.setPosition(10, Gdx.graphics.getHeight() - toggleButton.getHeight() - 10);
        toggleButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dropdownContainer.setVisible(!dropdownContainer.isVisible());
            }
        });
        stage.addActor(toggleButton);

        stage.addActor(new Image());

        pauseButton();


        bodyTable = new Table(skin);
        bodyTable.top().left();

        scrollPane = new ScrollPane(bodyTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        dropdownContainer = new Table();
        dropdownContainer.add(scrollPane).width(220).height(300).grow();
        float newY = Gdx.graphics.getHeight() - toggleButton.getHeight() - dropdownContainer.getPrefHeight() - 20;
        dropdownContainer.setPosition(10, newY);
        dropdownContainer.setVisible(false);
        stage.addActor(dropdownContainer);
    }

    public void updateBodyList(Array<CelestialBody> bodies) {
        if (bodies == null) return;

        bodyTable.clearChildren();

        for (CelestialBody body : bodies) {
            BodyListItem item = new BodyListItem(body, skin, listener, dropdownContainer);
            bodyTable.add(item).width(200).fillX().row();
        }
    }

    public Stage getStage() {
        return stage;
    }

    public ScrollPane getScrollPane() {
        return scrollPane;
    }

    public boolean isUiInFocus(float screenX, float screenY) {
        Actor hoveredActor = stage.hit(screenX, Gdx.graphics.getHeight() - screenY, true);
        return hoveredActor != null;
    }

    public boolean isOverScrollableArea(float screenX, float screenY) {
        Actor hoveredActor = stage.hit(screenX, Gdx.graphics.getHeight() - screenY, true);
        return hoveredActor == scrollPane || (hoveredActor != null && hoveredActor.isDescendantOf(scrollPane));
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);

        toggleButton.setPosition(10, height - toggleButton.getHeight() - 10);
        dropdownContainer.pack();
        float newY = height - toggleButton.getHeight() - dropdownContainer.getPrefHeight() - 20;
        dropdownContainer.setPosition(10, newY);

        // update bottom menu size to stretch across screen
        if (bottomMenuImage != null) {
            bottomMenuImage.setPosition(0, 0);
            bottomMenuImage.setSize(width, (float) height / 6.5f);
        }

        isDisplayingStarsCheckbox.setPosition(10, height / 6 + 50);
        displayMoonTrailsCheckbox.setPosition(10, height / 6 + 20);

        pauseButton.setPosition((width - pauseButton.getWidth()) / 2, (float) height / 7 - pauseButton.getHeight() - 10);
        //pauseButton.setSize(pauseButton.getWidth() - pauseButton.getWidth() / Gdx.graphics.getWidth(),
        //pauseButton.getHeight() - pauseButton.getHeight() / Gdx.graphics.getHeight());

    }

    public void pauseButton() {
        pauseButton = new ImageButton(new Image(pauseTexture).getDrawable());
        pauseButton.setPosition(Gdx.graphics.getWidth() - pauseButton.getWidth(), Gdx.graphics.getHeight() - pauseButton.getHeight() - 10);
        pauseButton.getStyle().imageUp = new Image(pauseTexture).getDrawable();
        pauseButton.getStyle().imageDown = new Image(pauseTexture).getDrawable();
        pauseButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                isPaused = !isPaused;
            }
        });
        stage.addActor(pauseButton);

    }

    public void dispose() {
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
        if (pauseTexture != null) pauseTexture.dispose();
        if (bottomMenuTexture != null) bottomMenuTexture.dispose();
    }

    public boolean isPaused() { return isPaused; }
    public boolean isDisplayingMoonTrails() { return isDisplayingMoonTrails; }
    public boolean isDisplayingStars() { return isDisplayingStars; }
}
