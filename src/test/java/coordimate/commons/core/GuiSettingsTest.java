package coordimate.commons.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

public class GuiSettingsTest {
    @Test
    public void toStringMethod() {
        GuiSettings guiSettings = new GuiSettings();
        String expected = GuiSettings.class.getCanonicalName() + "{windowWidth=" + guiSettings.getWindowWidth()
                + ", windowHeight=" + guiSettings.getWindowHeight() + ", windowCoordinates="
                + guiSettings.getWindowCoordinates() + ", windowMaximized=" + guiSettings.isWindowMaximized() + "}";
        assertEquals(expected, guiSettings.toString());
    }

    @Test
    public void equalsAndHashCode_differentMaximizedState_notEqual() {
        GuiSettings maximized = new GuiSettings(900, 700, 10, 20, true);
        GuiSettings sameSettings = new GuiSettings(900, 700, 10, 20, true);
        GuiSettings notMaximized = new GuiSettings(900, 700, 10, 20, false);
        GuiSettings differentCoordinates = new GuiSettings(900, 700, 20, 10, true);

        assertEquals(maximized, sameSettings);
        assertEquals(maximized.hashCode(), sameSettings.hashCode());
        assertNotEquals(maximized, notMaximized);
        assertNotEquals(maximized, differentCoordinates);
    }
}
