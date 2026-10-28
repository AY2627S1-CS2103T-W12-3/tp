package coordimate.ui;

import java.awt.Point;

import coordimate.commons.core.GuiSettings;
import javafx.geometry.Rectangle2D;

/**
 * Calculates window bounds that fit within a screen's usable area.
 */
final class WindowBounds {

    static final double PREFERRED_MIN_WIDTH = 1200;
    static final double PREFERRED_MIN_HEIGHT = 800;

    private WindowBounds() {
    }

    /**
     * Returns the saved window bounds clamped to {@code screenBounds}.
     * A window without saved coordinates is centred on the screen.
     */
    static Rectangle2D fitToScreen(GuiSettings guiSettings, Rectangle2D screenBounds) {
        double minWidth = Math.min(PREFERRED_MIN_WIDTH, screenBounds.getWidth());
        double minHeight = Math.min(PREFERRED_MIN_HEIGHT, screenBounds.getHeight());
        double width = Math.clamp(guiSettings.getWindowWidth(), minWidth, screenBounds.getWidth());
        double height = Math.clamp(guiSettings.getWindowHeight(), minHeight, screenBounds.getHeight());

        Point coordinates = guiSettings.getWindowCoordinates();
        double x = coordinates == null
                ? screenBounds.getMinX() + (screenBounds.getWidth() - width) / 2
                : Math.clamp(coordinates.getX(), screenBounds.getMinX(), screenBounds.getMaxX() - width);
        double y = coordinates == null
                ? screenBounds.getMinY() + (screenBounds.getHeight() - height) / 2
                : Math.clamp(coordinates.getY(), screenBounds.getMinY(), screenBounds.getMaxY() - height);
        return new Rectangle2D(x, y, width, height);
    }

    /**
     * Moves normal window bounds from one screen to the same relative position on another screen.
     * The size is retained when possible and reduced only when the target screen is smaller.
     */
    static Rectangle2D moveToScreen(Rectangle2D windowBounds, Rectangle2D sourceScreen,
                                    Rectangle2D targetScreen) {
        double translatedX = targetScreen.getMinX() + windowBounds.getMinX() - sourceScreen.getMinX();
        double translatedY = targetScreen.getMinY() + windowBounds.getMinY() - sourceScreen.getMinY();
        GuiSettings translated = new GuiSettings(windowBounds.getWidth(), windowBounds.getHeight(),
                (int) translatedX, (int) translatedY);
        return fitToScreen(translated, targetScreen);
    }

    /**
     * Returns whether the fitted bounds differ from any saved size or position.
     */
    static boolean wereAdjusted(GuiSettings guiSettings, Rectangle2D fittedBounds) {
        if (guiSettings.getWindowWidth() != fittedBounds.getWidth()) {
            return true;
        }
        if (guiSettings.getWindowHeight() != fittedBounds.getHeight()) {
            return true;
        }
        Point coordinates = guiSettings.getWindowCoordinates();
        if (coordinates == null) {
            return false;
        }
        return coordinates.getX() != fittedBounds.getMinX()
                || coordinates.getY() != fittedBounds.getMinY();
    }
}
