package coordimate.commons.core;

import java.awt.Point;
import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import coordimate.commons.util.ToStringBuilder;

/**
 * A Serializable class that contains the GUI settings.
 * Guarantees: immutable.
 */
public class GuiSettings implements Serializable {

    private static final double DEFAULT_HEIGHT = 850;
    private static final double DEFAULT_WIDTH = 1200;
    private static final boolean DEFAULT_MAXIMIZED = true;

    private final double windowWidth;
    private final double windowHeight;
    private final Point windowCoordinates;
    // Keeps the saved JSON key unchanged so existing preference files still load.
    @JsonProperty("windowMaximized")
    private final boolean isWindowMaximized;

    /**
     * Constructs a {@code GuiSettings} with the default height, width and position.
     */
    public GuiSettings() {
        windowWidth = DEFAULT_WIDTH;
        windowHeight = DEFAULT_HEIGHT;
        windowCoordinates = null; // null represent no coordinates
        isWindowMaximized = DEFAULT_MAXIMIZED;
    }

    /**
     * Constructs a {@code GuiSettings} with the specified height, width and position.
     */
    public GuiSettings(double windowWidth, double windowHeight, int xPosition, int yPosition) {
        this(windowWidth, windowHeight, xPosition, yPosition, DEFAULT_MAXIMIZED);
    }

    /**
     * Constructs a {@code GuiSettings} with the specified size, position and maximized state.
     */
    public GuiSettings(double windowWidth, double windowHeight, int xPosition, int yPosition,
                       boolean isWindowMaximized) {
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        windowCoordinates = new Point(xPosition, yPosition);
        this.isWindowMaximized = isWindowMaximized;
    }

    public double getWindowWidth() {
        return windowWidth;
    }

    public double getWindowHeight() {
        return windowHeight;
    }

    public Point getWindowCoordinates() {
        return windowCoordinates != null ? new Point(windowCoordinates) : null;
    }

    public boolean isWindowMaximized() {
        return isWindowMaximized;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof GuiSettings otherGuiSettings)) {
            return false;
        }

        return windowWidth == otherGuiSettings.windowWidth
                && windowHeight == otherGuiSettings.windowHeight
                && Objects.equals(windowCoordinates, otherGuiSettings.windowCoordinates)
                && isWindowMaximized == otherGuiSettings.isWindowMaximized;
    }

    @Override
    public int hashCode() {
        return Objects.hash(windowWidth, windowHeight, windowCoordinates, isWindowMaximized);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("windowWidth", windowWidth)
                .add("windowHeight", windowHeight)
                .add("windowCoordinates", windowCoordinates)
                .add("isWindowMaximized", isWindowMaximized)
                .toString();
    }
}
