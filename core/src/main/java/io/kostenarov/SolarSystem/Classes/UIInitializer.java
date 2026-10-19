package io.kostenarov.SolarSystem.Classes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class UIInitializer {

    private Stage stage;
    private Skin skin;
    private TextButton toggleButton;
    private ImageButton pauseButton;
    private Table dropdownContainer;
    private ScrollPane scrollPane;
    private Table bodyTable;
    private Listener listener;
    private TextButton settingsButton;
    private TextButton editorButton;
    private SettingsWindow settingsWindow;
    private SystemEditorWindow editorWindow;
    private Texture pauseTexture;
    private Texture bottomMenuTexture;
    private Image bottomMenuImage; // new field
    private CheckBox displayMoonTrailsCheckbox;
    private CheckBox displayStarsCheckbox;
    private TextButton integratorButton;

    private boolean isPaused = false;
    private boolean isDisplayingMoonTrails = true;
    private boolean isDisplayingStars = true;
    private IntegratorMode integratorMode = IntegratorMode.VERLET;

    public interface BodySelectionListener {
        void onBodySelected(CelestialBody body);
    }

    public interface Listener extends BodySelectionListener, SettingsWindow.Listener, SystemEditorWindow.Listener {
        BodyConfig[] getCurrentSystem();
        String getCurrentSystemName();
    }

    public enum IntegratorMode {
        VERLET("Verlet"),
        EULER("Euler"),
        BOTH("Both (solid = Verlet, outline = Euler)");

        private final String label;

        IntegratorMode(String label) {
            this.label = label;
        }
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

    public UIInitializer(Array<CelestialBody> bodies, SimulationSettings settings, Listener listener) {
        this.listener = listener;
        stage = new Stage(new ScreenViewport());
        skin = createBasicSkin();
        clearTextFocusOnOutsideClick();
        pauseTexture = createPauseButtonTexture();
        bottomMenuTexture = initBottomMenu();
        createDisplayMoonTrailsCheckbox();
        createDisplayStarsCheckbox();
        createIntegratorButton();
        initializeUi();
        createWindows(settings);
        updateBodyList(bodies);
    }

    // Without this a text field keeps keyboard focus after clicking elsewhere, which also
    // keeps the camera keys disabled
    private void clearTextFocusOnOutsideClick() {
        stage.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (!(event.getTarget() instanceof TextField)) stage.setKeyboardFocus(null);
                return false;
            }
        });
    }

    private void createWindows(SimulationSettings settings) {
        settingsWindow = new SettingsWindow(skin, settings, listener);
        settingsWindow.setVisible(false);
        stage.addActor(settingsWindow);

        editorWindow = new SystemEditorWindow(skin, listener);
        editorWindow.setVisible(false);
        stage.addActor(editorWindow);

        settingsButton = new TextButton(" Settings ", skin);
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
        stage.addActor(settingsButton);

        editorButton = new TextButton(" Edit System ", skin);
        editorButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (editorWindow.isVisible()) {
                    editorWindow.setVisible(false);
                } else {
                    editorWindow.open(listener.getCurrentSystem(), listener.getCurrentSystemName());
                }
            }
        });
        stage.addActor(editorButton);

        positionTopButtons(Gdx.graphics.getHeight());
    }

    // "Select Body", "Settings" and "Edit System" side by side in the top-left corner
    private void positionTopButtons(int height) {
        float y = height - toggleButton.getHeight() - 10;
        toggleButton.setPosition(10, y);
        settingsButton.setPosition(toggleButton.getX() + toggleButton.getWidth() + 10, y);
        editorButton.setPosition(settingsButton.getX() + settingsButton.getWidth() + 10, y);
    }

    /** True while a text field has keyboard focus, so key presses are typing, not camera controls. */
    public boolean isTyping() {
        return stage.getKeyboardFocus() instanceof TextField;
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
        checkBoxStyle.checkboxOn = sized(skin, Color.WHITE, 12, 12); // visible when checked
        checkBoxStyle.checkboxOff = sized(skin, Color.DARK_GRAY, 12, 12); // visible when unchecked
        checkBoxStyle.font = skin.getFont("default");
        checkBoxStyle.fontColor = Color.WHITE;
        skin.add("default", checkBoxStyle);

        Slider.SliderStyle sliderStyle = new Slider.SliderStyle();
        sliderStyle.background = sized(skin, new Color(0.3f, 0.3f, 0.3f, 1f), 0, 4);
        sliderStyle.knob = sized(skin, Color.LIGHT_GRAY, 10, 16);
        sliderStyle.knobOver = sized(skin, Color.WHITE, 10, 16);
        sliderStyle.knobDown = sized(skin, Color.GRAY, 10, 16);
        skin.add("default-horizontal", sliderStyle);

        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = skin.getFont("default");
        textFieldStyle.fontColor = Color.WHITE;
        textFieldStyle.background = padded(skin.newDrawable("white", new Color(0.18f, 0.18f, 0.18f, 1f)), 5, 3);
        textFieldStyle.focusedBackground = padded(skin.newDrawable("white", new Color(0.25f, 0.25f, 0.3f, 1f)), 5, 3);
        textFieldStyle.cursor = sized(skin, Color.WHITE, 1, 0);
        textFieldStyle.selection = skin.newDrawable("white", new Color(0.3f, 0.45f, 0.8f, 1f));
        skin.add("default", textFieldStyle);

        List.ListStyle listStyle = new List.ListStyle();
        listStyle.font = skin.getFont("default");
        listStyle.fontColorSelected = Color.WHITE;
        listStyle.fontColorUnselected = Color.LIGHT_GRAY;
        listStyle.selection = padded(skin.newDrawable("white", new Color(0.3f, 0.45f, 0.8f, 1f)), 5, 2);
        listStyle.over = padded(skin.newDrawable("white", new Color(0.25f, 0.25f, 0.25f, 1f)), 5, 2);
        skin.add("default", listStyle);

        SelectBox.SelectBoxStyle selectBoxStyle = new SelectBox.SelectBoxStyle();
        selectBoxStyle.font = skin.getFont("default");
        selectBoxStyle.fontColor = Color.WHITE;
        selectBoxStyle.background = padded(skin.newDrawable("white", Color.DARK_GRAY), 6, 3);
        selectBoxStyle.backgroundOver = padded(skin.newDrawable("white", Color.GRAY), 6, 3);
        selectBoxStyle.scrollStyle = scrollPaneStyle;
        selectBoxStyle.listStyle = listStyle;
        skin.add("default", selectBoxStyle);

        Window.WindowStyle windowStyle = new Window.WindowStyle();
        windowStyle.titleFont = skin.getFont("default");
        windowStyle.titleFontColor = Color.WHITE;
        Drawable windowBackground = skin.newDrawable("white", new Color(0.08f, 0.08f, 0.1f, 0.95f));
        windowBackground.setLeftWidth(10);
        windowBackground.setRightWidth(10);
        windowBackground.setTopHeight(28);
        windowBackground.setBottomHeight(10);
        windowStyle.background = windowBackground;
        skin.add("default", windowStyle);

        return skin;
    }

    // A 1x1 white drawable has no size or padding of its own; these give it some
    private static Drawable sized(Skin skin, Color color, float minWidth, float minHeight) {
        Drawable drawable = skin.newDrawable("white", color);
        drawable.setMinWidth(minWidth);
        drawable.setMinHeight(minHeight);
        return drawable;
    }

    private static Drawable padded(Drawable drawable, float horizontal, float vertical) {
        drawable.setLeftWidth(horizontal);
        drawable.setRightWidth(horizontal);
        drawable.setTopHeight(vertical);
        drawable.setBottomHeight(vertical);
        return drawable;
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
        pixmap.setColor(new Color(100 / 255f, 100 / 255f, 100 / 255f, 0.5f));
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private void createDisplayMoonTrailsCheckbox() {
        displayMoonTrailsCheckbox = new CheckBox(" Display Moon Trails", skin);
        displayMoonTrailsCheckbox.setPosition(10, moonTrailsCheckboxY(Gdx.graphics.getHeight()));
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
        displayStarsCheckbox = new CheckBox(" Display Stars", skin);
        displayStarsCheckbox.setPosition(10, starsCheckboxY(Gdx.graphics.getHeight()));
        displayStarsCheckbox.setChecked(true);
        displayStarsCheckbox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                isDisplayingStars = displayStarsCheckbox.isChecked();
            }
        });
        stage.addActor(displayStarsCheckbox);
    }

    // Cycles Verlet -> Euler -> Both
    private void createIntegratorButton() {
        integratorButton = new TextButton(integratorButtonText(), skin);
        integratorButton.pad(4, 8, 4, 8);
        integratorButton.setPosition(10, integratorButtonY(Gdx.graphics.getHeight()));
        integratorButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                IntegratorMode[] modes = IntegratorMode.values();
                integratorMode = modes[(integratorMode.ordinal() + 1) % modes.length];
                integratorButton.setText(integratorButtonText());
                integratorButton.pack();
            }
        });
        integratorButton.pack();
        stage.addActor(integratorButton);
    }

    private String integratorButtonText() {
        return "Integrator: " + integratorMode.label;
    }

    // Checkboxes sit just above the bottom menu bar
    private static float moonTrailsCheckboxY(int screenHeight) {
        return screenHeight / 6 + 20;
    }

    private static float starsCheckboxY(int screenHeight) {
        return screenHeight / 6 + 50;
    }

    private static float integratorButtonY(int screenHeight) {
        return screenHeight / 6 + 80;
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

        positionTopButtons(height);
        dropdownContainer.pack();
        float newY = height - toggleButton.getHeight() - dropdownContainer.getPrefHeight() - 20;
        dropdownContainer.setPosition(10, newY);

        // update bottom menu size to stretch across screen
        if (bottomMenuImage != null) {
            bottomMenuImage.setPosition(0, 0);
            bottomMenuImage.setSize(width, (float) height / 6.5f);
        }

        displayMoonTrailsCheckbox.setPosition(10, moonTrailsCheckboxY(height));
        displayStarsCheckbox.setPosition(10, starsCheckboxY(height));
        integratorButton.setPosition(10, integratorButtonY(height));

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
    public IntegratorMode getIntegratorMode() { return integratorMode; }
}
