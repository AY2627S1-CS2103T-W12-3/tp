package coordimate.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import coordimate.logic.commands.CommandResult.View;

public class CommandResultTest {
    @Test
    public void equals() {
        CommandResult commandResult = new CommandResult("feedback");

        assertEquals(commandResult, new CommandResult("feedback"));
        assertEquals(commandResult, new CommandResult("feedback", false, false));
        assertEquals(commandResult, new CommandResult("feedback", View.NONE));
        assertEquals(commandResult, new CommandResult("feedback", false, false, View.NONE));
        assertTrue(commandResult.equals(commandResult));
        assertFalse(commandResult.equals(null));
        assertFalse(commandResult.equals(0.5f));

        assertNotEquals(commandResult, new CommandResult("different"));
        assertNotEquals(commandResult, new CommandResult("feedback", true, false));
        assertNotEquals(commandResult, new CommandResult("feedback", false, true));
        assertNotEquals(commandResult, new CommandResult("feedback", View.TAGS));
        assertNotEquals(commandResult, new CommandResult("feedback", View.CONTACTS));
        assertNotEquals(commandResult, new CommandResult("feedback", View.EVENTS));
        assertNotEquals(commandResult, CommandResult.warning("feedback"));
    }

    @Test
    public void hashcode() {
        CommandResult commandResult = new CommandResult("feedback");

        assertEquals(commandResult.hashCode(), new CommandResult("feedback", View.NONE).hashCode());
        assertNotEquals(commandResult.hashCode(), new CommandResult("different").hashCode());
        assertNotEquals(commandResult.hashCode(), new CommandResult("feedback", true, false).hashCode());
        assertNotEquals(commandResult.hashCode(), new CommandResult("feedback", false, true).hashCode());
        assertNotEquals(commandResult.hashCode(), new CommandResult("feedback", View.TAGS).hashCode());
        assertNotEquals(commandResult.hashCode(), new CommandResult("feedback", View.CONTACTS).hashCode());
        assertNotEquals(commandResult.hashCode(), new CommandResult("feedback", View.EVENTS).hashCode());
        assertNotEquals(commandResult.hashCode(), CommandResult.warning("feedback").hashCode());
    }

    @Test
    public void toStringMethod() {
        CommandResult commandResult = new CommandResult("feedback", View.EVENTS);
        String expected = CommandResult.class.getCanonicalName() + "{feedbackToUser="
                + commandResult.getFeedbackToUser() + ", showHelp=" + commandResult.shouldShowHelp()
                + ", exit=" + commandResult.shouldExit() + ", viewToShow=" + commandResult.getViewToShow()
                + ", warning=" + commandResult.isWarning() + "}";
        assertEquals(expected, commandResult.toString());
    }

    @Test
    public void warning_marksUnsavedResultWithoutOtherActions() {
        CommandResult result = CommandResult.warning("Not saved");
        assertEquals("Not saved", result.getFeedbackToUser());
        assertTrue(result.isWarning());
        assertFalse(new CommandResult("Saved").isWarning());
        assertFalse(result.shouldShowHelp());
        assertFalse(result.shouldExit());
        assertEquals(View.NONE, result.getViewToShow());
        assertEquals(result, CommandResult.warning("Not saved"));
    }

    @Test
    public void getViewToShow_returnsRequestedViewOrNone() {
        assertEquals(View.NONE, new CommandResult("feedback").getViewToShow());
        assertEquals(View.TAGS, new CommandResult("feedback", View.TAGS).getViewToShow());
        assertEquals(View.CONTACTS, new CommandResult("feedback", View.CONTACTS).getViewToShow());
        assertEquals(View.EVENTS, new CommandResult("feedback", View.EVENTS).getViewToShow());
    }

    @Test
    public void constructor_nullView_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new CommandResult("feedback", (View) null));
    }
}
