package io.kostenarov.SolarSystem.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;

/**
 * The skin shared by every screen, built from the atlas and fonts in assets/ui. The atlas has no
 * nine-patch borders of its own, so they are defined here.
 */
public final class UiSkin {
    public static final Color ACCENT = new Color(49 / 255f, 169 / 255f, 244 / 255f, 1f);
    public static final Color TEXT = new Color(0.93f, 0.94f, 0.96f, 1f);
    public static final Color MUTED_TEXT = new Color(0.66f, 0.70f, 0.78f, 1f);
    public static final Color ERROR_TEXT = new Color(1f, 0.47f, 0.47f, 1f);
    public static final Color OK_TEXT = new Color(0.55f, 0.9f, 0.6f, 1f);
    /** Translucent strip colour for the bars over the simulation. */
    public static final Color BAR = new Color(0.07f, 0.08f, 0.11f, 0.85f);

    private UiSkin() {
    }

    /**
     * Makes clicking anywhere outside a text field take keyboard focus away from it. Without this a
     * text field keeps focus after clicking elsewhere, which also keeps the camera keys disabled.
     */
    public static void clearTextFocusOnOutsideClick(final Stage stage) {
        stage.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (!(event.getTarget() instanceof TextField)) stage.setKeyboardFocus(null);
                return false;
            }
        });
    }

    public static Skin create() {
        TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("ui/uiskin.atlas"));
        Skin skin = new Skin(atlas);

        BitmapFont font = font(atlas, "font");
        BitmapFont small = font(atlas, "font-list");
        BitmapFont subtitle = font(atlas, "font-subtitle");
        BitmapFont title = font(atlas, "font-window");
        skin.add("default", font);
        skin.add("small", small);
        skin.add("subtitle", subtitle);
        skin.add("title", title);

        // Plain colour drawables used by custom widgets
        skin.add("bar", skin.newDrawable("white", BAR), Drawable.class);
        skin.add("list-item-up", skin.newDrawable("white", new Color(0, 0, 0, 0)), Drawable.class);
        skin.add("list-item-over", skin.newDrawable("white", new Color(1, 1, 1, 0.08f)), Drawable.class);
        skin.add("list-item-down", skin.newDrawable("white", new Color(ACCENT.r, ACCENT.g, ACCENT.b, 0.35f)), Drawable.class);

        // Labels
        skin.add("default", new Label.LabelStyle(font, TEXT));
        skin.add("small", new Label.LabelStyle(small, MUTED_TEXT));
        skin.add("subtitle", new Label.LabelStyle(subtitle, TEXT));
        skin.add("title", new Label.LabelStyle(title, TEXT));

        // Buttons: slate with a blue outline on hover and press
        TextButton.TextButtonStyle button = new TextButton.TextButtonStyle();
        button.up = nine(atlas, "button-normal", 6, 7, 6, 8, 14, 6);
        button.over = nine(atlas, "button-normal-over", 6, 7, 6, 8, 14, 6);
        button.down = nine(atlas, "button-normal-pressed", 6, 7, 6, 8, 14, 6);
        button.checked = button.down;
        button.font = font;
        button.fontColor = TEXT;
        button.disabledFontColor = MUTED_TEXT;
        skin.add("default", button);

        // Flat blue button for the main action on a screen
        TextButton.TextButtonStyle accent = new TextButton.TextButtonStyle(button);
        accent.up = padded(skin.newDrawable("white", ACCENT), 14, 7);
        accent.over = padded(skin.newDrawable("white", ACCENT.cpy().lerp(Color.WHITE, 0.15f)), 14, 7);
        accent.down = padded(skin.newDrawable("white", ACCENT.cpy().lerp(Color.BLACK, 0.2f)), 14, 7);
        accent.checked = null;
        accent.fontColor = Color.WHITE;
        skin.add("accent", accent);

        // Text-only button for the bars and title rows
        TextButton.TextButtonStyle flat = new TextButton.TextButtonStyle();
        flat.up = padded(skin.newDrawable("white", new Color(0, 0, 0, 0)), 10, 6);
        flat.over = padded(skin.newDrawable("white", new Color(1, 1, 1, 0.1f)), 10, 6);
        flat.down = padded(skin.newDrawable("white", new Color(ACCENT.r, ACCENT.g, ACCENT.b, 0.4f)), 10, 6);
        flat.checked = flat.down;
        flat.font = font;
        flat.fontColor = TEXT;
        skin.add("flat", flat);

        ScrollPane.ScrollPaneStyle scroll = new ScrollPane.ScrollPaneStyle();
        scroll.vScrollKnob = nine(atlas, "scrollbar", 3, 4, 3, 4, 0, 0);
        scroll.vScrollKnob.setMinWidth(8);
        skin.add("default", scroll);

        // A scroll pane with the slate list background
        ScrollPane.ScrollPaneStyle panel = new ScrollPane.ScrollPaneStyle(scroll);
        panel.background = nine(atlas, "list", 4, 6, 4, 6, 4, 4);
        skin.add("panel", panel);

        CheckBox.CheckBoxStyle checkBox = new CheckBox.CheckBoxStyle();
        checkBox.checkboxOff = skin.getDrawable("checkbox");
        checkBox.checkboxOn = skin.getDrawable("checkbox-selected");
        checkBox.checkboxOver = skin.getDrawable("checkbox-over");
        checkBox.checkboxOnOver = skin.getDrawable("checkbox-selected-over");
        checkBox.font = font;
        checkBox.fontColor = TEXT;
        skin.add("default", checkBox);

        Slider.SliderStyle slider = new Slider.SliderStyle();
        slider.background = nine(atlas, "slider", 4, 4, 4, 4, 0, 0);
        slider.knob = skin.getDrawable("slider-knob");
        slider.knobOver = skin.getDrawable("slider-knob-over");
        slider.knobDown = skin.getDrawable("slider-knob-over");
        skin.add("default-horizontal", slider);

        TextField.TextFieldStyle textField = new TextField.TextFieldStyle();
        textField.font = font;
        textField.fontColor = TEXT;
        textField.messageFontColor = MUTED_TEXT;
        textField.background = nine(atlas, "textfield", 6, 6, 6, 6, 8, 4);
        textField.focusedBackground = nine(atlas, "textfield-selected", 6, 6, 6, 6, 8, 4);
        textField.cursor = skin.newDrawable("white", TEXT);
        textField.cursor.setMinWidth(1);
        textField.selection = skin.newDrawable("white", new Color(ACCENT.r, ACCENT.g, ACCENT.b, 0.5f));
        skin.add("default", textField);

        List.ListStyle list = new List.ListStyle();
        list.font = font;
        list.fontColorSelected = Color.WHITE;
        list.fontColorUnselected = TEXT;
        list.selection = padded(skin.newDrawable("white", new Color(ACCENT.r, ACCENT.g, ACCENT.b, 0.55f)), 8, 4);
        list.over = padded(skin.newDrawable("white", new Color(1, 1, 1, 0.08f)), 8, 4);
        list.down = list.selection;
        skin.add("default", list);

        SelectBox.SelectBoxStyle selectBox = new SelectBox.SelectBoxStyle();
        selectBox.font = font;
        selectBox.fontColor = TEXT;
        selectBox.background = nine(atlas, "select-box", 6, 20, 6, 12, 10, 5);
        selectBox.backgroundOver = nine(atlas, "select-box-open", 6, 20, 6, 12, 10, 5);
        selectBox.backgroundOpen = selectBox.backgroundOver;
        selectBox.scrollStyle = panel;
        selectBox.listStyle = list;
        skin.add("default", selectBox);

        Window.WindowStyle window = new Window.WindowStyle();
        window.titleFont = subtitle;
        window.titleFontColor = Color.WHITE;
        window.background = nine(atlas, "window", 6, 6, 38, 6, 12, 0);
        window.background.setTopHeight(40);
        window.background.setBottomHeight(12);
        skin.add("default", window);

        return skin;
    }

    private static BitmapFont font(TextureAtlas atlas, String name) {
        BitmapFont font = new BitmapFont(Gdx.files.internal("ui/" + name + ".fnt"), atlas.findRegion(name));
        font.setUseIntegerPositions(true);
        return font;
    }

    /**
     * A nine-patch from an atlas region.
     * @param left,right,top,bottom border sizes in the region that don't stretch
     * @param padX,padY space between the drawable's edges and its content
     */
    private static Drawable nine(TextureAtlas atlas, String region, int left, int right, int top, int bottom, float padX, float padY) {
        NinePatchDrawable drawable = new NinePatchDrawable(new NinePatch(atlas.findRegion(region), left, right, top, bottom));
        drawable.setLeftWidth(left + padX);
        drawable.setRightWidth(right + padX);
        drawable.setTopHeight(top + padY);
        drawable.setBottomHeight(bottom + padY);
        return drawable;
    }

    private static Drawable padded(Drawable drawable, float horizontal, float vertical) {
        drawable.setLeftWidth(horizontal);
        drawable.setRightWidth(horizontal);
        drawable.setTopHeight(vertical);
        drawable.setBottomHeight(vertical);
        return drawable;
    }
}
