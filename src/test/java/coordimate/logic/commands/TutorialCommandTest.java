package coordimate.logic.commands;

import static coordimate.logic.commands.CommandTestUtil.assertCommandSuccess;
import static coordimate.logic.commands.TutorialCommand.SHOWING_TUTORIAL_MESSAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import coordimate.model.Model;
import coordimate.model.ModelManager;

public class TutorialCommandTest {
    private Model model = new ModelManager();
    private Model expectedModel = new ModelManager();

    @Test
    public void execute_tutorial_success() {
        CommandResult expectedCommandResult = CommandResult.showingTutorial(SHOWING_TUTORIAL_MESSAGE);
        assertCommandSuccess(new TutorialCommand(), model, expectedCommandResult, expectedModel);
    }

    @Test
    public void execute_tutorial_requestsTutorialOnly() {
        CommandResult result = new TutorialCommand().execute(model);
        assertTrue(result.shouldShowTutorial());
        assertFalse(result.shouldShowHelp());
        assertFalse(result.shouldExit());
        assertEquals(CommandResult.View.NONE, result.getViewToShow());
    }
}
