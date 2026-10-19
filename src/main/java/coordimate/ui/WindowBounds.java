package coordimate.ui;

import java.awt.Point;

import coordimate.commons.core.GuiSettings;
import javafx.geometry.Rectangle2D;

/**
 * Calculates window bounds that fit within a screen's usable area.
 */
final class WindowBounds {

    static final double PREFERRED_MIN_WIDTH = 830;
    static final double PREFERRED_MIN_HEIGHT = 650;

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
}
