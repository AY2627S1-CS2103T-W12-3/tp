package coordimate.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class CommandResultTest {
    @Test
    public void equals() {
        CommandResult commandResult = new CommandResult("feedback");

        // same values -> returns true
        assertTrue(commandResult.equals(new CommandResult("feedback")));
        assertTrue(commandResult.equals(new CommandResult("feedback", false, false)));
        assertTrue(commandResult.equals(new CommandResult("feedback", false, false, false)));
        assertTrue(commandResult.equals(new CommandResult("feedback", false, false, false, false)));

        // same object -> returns true
        assertTrue(commandResult.equals(commandResult));

        // null -> returns false
        assertFalse(commandResult.equals(null));

        // different types -> returns false
        assertFalse(commandResult.equals(0.5f));

        // different feedbackToUser value -> returns false
        assertFalse(commandResult.equals(new CommandResult("different")));

        // different showHelp value -> returns false
        assertFalse(commandResult.equals(new CommandResult("feedback", true, false)));

        // different exit value -> returns false
        assertFalse(commandResult.equals(new CommandResult("feedback", false, true)));

        // different showTags value -> returns false
        assertFalse(commandResult.equals(new CommandResult("feedback", false, false, true)));

        // different showContacts value -> returns false
        assertFalse(commandResult.equals(new CommandResult("feedback", false, false, false, true)));
    }

    @Test
    public void hashcode() {
        CommandResult commandResult = new CommandResult("feedback");

        // same values -> returns same hashcode
        assertEquals(commandResult.hashCode(), new CommandResult("feedback").hashCode());

        // different feedbackToUser value -> returns different hashcode
        assertNotEquals(commandResult.hashCode(), new CommandResult("different").hashCode());

        // different showHelp value -> returns different hashcode
        assertNotEquals(commandResult.hashCode(), new CommandResult("feedback", true, false).hashCode());

        // different exit value -> returns different hashcode
        assertNotEquals(commandResult.hashCode(), new CommandResult("feedback", false, true).hashCode());

        // different showTags value -> returns different hashcode
        assertNotEquals(commandResult.hashCode(), new CommandResult("feedback", false, false, true).hashCode());

        // different showContacts value -> returns different hashcode
        assertNotEquals(commandResult.hashCode(), new CommandResult("feedback", false, false, false, true).hashCode());
    }

    @Test
    public void toStringMethod() {
        CommandResult commandResult = new CommandResult("feedback");
        String expected = CommandResult.class.getCanonicalName() + "{feedbackToUser="
                + commandResult.getFeedbackToUser() + ", showHelp=" + commandResult.shouldShowHelp()
                + ", exit=" + commandResult.shouldExit() + ", showTags=" + commandResult.shouldShowTags()
                + ", showContacts=" + commandResult.shouldShowContacts() + "}";
        assertEquals(expected, commandResult.toString());
    }

    @Test
    public void shouldShowTags_listTagsResult_returnsTrue() {
        CommandResult commandResult = new CommandResult("feedback", false, false, true);

        assertTrue(commandResult.shouldShowTags());
    }

    @Test
    public void shouldShowContacts_defaultFalseAndSetWhenRequested() {
        assertFalse(new CommandResult("feedback").shouldShowContacts());
        assertFalse(new CommandResult("feedback", false, false, true).shouldShowContacts());
        assertTrue(new CommandResult("feedback", false, false, false, true).shouldShowContacts());
    }
}
