package coordimate.model;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

import coordimate.commons.core.GuiSettings;

/**
 * Represents User's preferences.
 */
public class UserPrefs implements ReadOnlyUserPrefs {

    private GuiSettings guiSettings = new GuiSettings();
    private boolean hasSeenTutorial;

    /**
     * Creates a {@code UserPrefs} with default values.
     */
    public UserPrefs() {}

    /**
     * Creates a {@code UserPrefs} with the prefs in {@code userPrefs}.
     */
    public UserPrefs(ReadOnlyUserPrefs userPrefs) {
        requireNonNull(userPrefs);
        setGuiSettings(userPrefs.getGuiSettings());
        setHasSeenTutorial(userPrefs.hasSeenTutorial());
    }

    @Override
    public GuiSettings getGuiSettings() {
        return guiSettings;
    }

    /**
     * Replaces the user's window settings with the given non-null settings.
     */
    public void setGuiSettings(GuiSettings guiSettings) {
        requireNonNull(guiSettings);
        this.guiSettings = guiSettings;
    }

    @Override
    public boolean hasSeenTutorial() {
        return hasSeenTutorial;
    }

    /** Records whether the first-launch tutorial has been shown. */
    public void setHasSeenTutorial(boolean hasSeenTutorial) {
        this.hasSeenTutorial = hasSeenTutorial;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof UserPrefs otherUserPrefs)) {
            return false;
        }

        return guiSettings.equals(otherUserPrefs.guiSettings)
                && hasSeenTutorial == otherUserPrefs.hasSeenTutorial;
    }

    @Override
    public int hashCode() {
        return Objects.hash(guiSettings, hasSeenTutorial);
    }

    @Override
    public String toString() {
        return "Gui Settings : " + guiSettings + ", Tutorial shown : " + hasSeenTutorial;
    }

}
