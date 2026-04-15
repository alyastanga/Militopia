package com.militopia.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.militopia.utils.HoverListener;

/**
 * Central factory for creating consistently styled TextButtons.
 *
 * Every button produced here uses the {@code "militopia-btn"} NinePatch style
 * and includes a {@link HoverListener} for scale + cursor feedback.
 *
 * To swap the underlying drawable (e.g. to a different 9-patch), edit
 * the {@code "militopia-btn"} style definition in
 * {@link com.militopia.MilitopiaGame#create()}.
 */
public final class ButtonFactory {

    /** The skin style name used for all standard game buttons. */
    public static final String DEFAULT_STYLE = "militopia-btn";

    private ButtonFactory() {} // utility class

    /**
     * Creates a styled TextButton with hover feedback.
     *
     * @param text button label
     * @param skin the game skin (must contain {@value #DEFAULT_STYLE})
     * @return a ready-to-use TextButton
     */
    public static TextButton create(String text, Skin skin) {
        return create(text, skin, DEFAULT_STYLE);
    }

    /**
     * Creates a TextButton with an explicit style name and hover feedback.
     *
     * @param text      button label
     * @param skin      the game skin
     * @param styleName name of the TextButtonStyle in the skin
     * @return a ready-to-use TextButton
     */
    public static TextButton create(String text, Skin skin, String styleName) {
        TextButton btn = new TextButton(text, skin, styleName);
        btn.setTransform(true);
        btn.addListener(new HoverListener());
        return btn;
    }
}
