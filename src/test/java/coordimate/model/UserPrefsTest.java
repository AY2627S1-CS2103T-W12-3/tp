package coordimate.model;

import static coordimate.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class UserPrefsTest {

    @Test
    public void newUserPrefs_tutorialHasNotBeenSeen() {
        assertFalse(new UserPrefs().hasSeenTutorial());
    }

    @Test
    public void setGuiSettings_nullGuiSettings_throwsNullPointerException() {
        UserPrefs userPref = new UserPrefs();
        assertThrows(NullPointerException.class, () -> userPref.setGuiSettings(null));
    }

    @Test
    public void copyConstructor_preservesTutorialState() {
        UserPrefs original = new UserPrefs();
        original.setHasSeenTutorial(true);

        UserPrefs copy = new UserPrefs(original);

        assertTrue(copy.hasSeenTutorial());
        assertTrue(copy.equals(original));
    }

}
