package coordimate.model;

import coordimate.commons.core.GuiSettings;

/**
 * Unmodifiable view of user prefs.
 */
public interface ReadOnlyUserPrefs {

    /**
     * Returns the user's window size and position settings.
     */
    GuiSettings getGuiSettings();

}
