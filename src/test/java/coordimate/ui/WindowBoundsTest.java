package coordimate.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import coordimate.commons.core.GuiSettings;
import javafx.geometry.Rectangle2D;

public class WindowBoundsTest {

    private static final Rectangle2D SMALL_SCREEN = new Rectangle2D(0, 20, 800, 580);
    private static final Rectangle2D SECOND_SCREEN = new Rectangle2D(1920, 0, 1280, 720);

    @Test
    public void fitToScreen_oversizedWindow_fillsAvailableScreen() {
        GuiSettings guiSettings = new GuiSettings(1470, 860, 0, 38);

        assertEquals(SMALL_SCREEN, WindowBounds.fitToScreen(guiSettings, SMALL_SCREEN));
    }

    @Test
    public void fitToScreen_offScreenCoordinates_movesWindowInsideScreen() {
        GuiSettings guiSettings = new GuiSettings(1000, 650, -2000, 2000);

        Rectangle2D expected = new Rectangle2D(1920, 70, 1000, 650);
        assertEquals(expected, WindowBounds.fitToScreen(guiSettings, SECOND_SCREEN));
    }

    @Test
    public void fitToScreen_noSavedCoordinates_centresWindow() {
        GuiSettings guiSettings = new GuiSettings();

        Rectangle2D expected = new Rectangle2D(1960, 0, 1200, 720);
        assertEquals(expected, WindowBounds.fitToScreen(guiSettings, SECOND_SCREEN));
    }

    @Test
    public void fitToScreen_windowBelowPreferredMinimum_enlargesWindow() {
        GuiSettings guiSettings = new GuiSettings(400, 300, 2000, 50);

        Rectangle2D expected = new Rectangle2D(2000, 50, 830, 650);
        assertEquals(expected, WindowBounds.fitToScreen(guiSettings, SECOND_SCREEN));
    }

    @Test
    public void moveToScreen_normalBounds_preservesSizeAndRelativePosition() {
        Rectangle2D sourceScreen = new Rectangle2D(0, 20, 1920, 1040);
        Rectangle2D targetScreen = new Rectangle2D(1920, 40, 1600, 900);
        Rectangle2D windowBounds = new Rectangle2D(120, 100, 1000, 700);

        Rectangle2D expected = new Rectangle2D(2040, 120, 1000, 700);
        assertEquals(expected, WindowBounds.moveToScreen(windowBounds, sourceScreen, targetScreen));
    }

    @Test
    public void moveToScreen_smallerTarget_clampsSizeAndPosition() {
        Rectangle2D sourceScreen = new Rectangle2D(0, 0, 1920, 1080);
        Rectangle2D targetScreen = new Rectangle2D(-1280, 30, 1280, 720);
        Rectangle2D windowBounds = new Rectangle2D(1000, 500, 1400, 900);

        assertEquals(targetScreen, WindowBounds.moveToScreen(windowBounds, sourceScreen, targetScreen));
    }

    @Test
    public void wereAdjusted_sameSavedBounds_returnsFalse() {
        GuiSettings guiSettings = new GuiSettings(900, 650, 1920, 20);
        Rectangle2D fittedBounds = new Rectangle2D(1920, 20, 900, 650);

        assertFalse(WindowBounds.wereAdjusted(guiSettings, fittedBounds));
    }

    @Test
    public void wereAdjusted_noSavedCoordinatesAndSameSize_returnsFalse() {
        GuiSettings guiSettings = new GuiSettings();
        Rectangle2D fittedBounds = new Rectangle2D(100, 50,
                guiSettings.getWindowWidth(), guiSettings.getWindowHeight());

        assertFalse(WindowBounds.wereAdjusted(guiSettings, fittedBounds));
    }

    @Test
    public void wereAdjusted_differentWidth_returnsTrue() {
        GuiSettings guiSettings = new GuiSettings(900, 650, 1920, 20);
        Rectangle2D fittedBounds = new Rectangle2D(1920, 20, 830, 650);

        assertTrue(WindowBounds.wereAdjusted(guiSettings, fittedBounds));
    }

    @Test
    public void wereAdjusted_differentHeight_returnsTrue() {
        GuiSettings guiSettings = new GuiSettings(900, 700, 1920, 20);
        Rectangle2D fittedBounds = new Rectangle2D(1920, 20, 900, 650);

        assertTrue(WindowBounds.wereAdjusted(guiSettings, fittedBounds));
    }

    @Test
    public void wereAdjusted_differentXCoordinate_returnsTrue() {
        GuiSettings guiSettings = new GuiSettings(900, 650, 2000, 20);
        Rectangle2D fittedBounds = new Rectangle2D(1920, 20, 900, 650);

        assertTrue(WindowBounds.wereAdjusted(guiSettings, fittedBounds));
    }

    @Test
    public void wereAdjusted_differentYCoordinate_returnsTrue() {
        GuiSettings guiSettings = new GuiSettings(900, 650, 1920, 50);
        Rectangle2D fittedBounds = new Rectangle2D(1920, 20, 900, 650);

        assertTrue(WindowBounds.wereAdjusted(guiSettings, fittedBounds));
    }
}
