package io.kostenarov.SolarSystem.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/** The skin shared by every screen, built in code from a single white pixel and the default font. */
public final class UiSkin {

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
}
