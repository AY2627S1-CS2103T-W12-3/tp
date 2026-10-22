package coordimate.logic.commands;

import coordimate.model.Model;

/**
 * Opens the guided tutorial from its first step.
 */
public class TutorialCommand extends Command {

    public static final String COMMAND_WORD = "tutorial";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Opens the guided tutorial.\n"
            + "Parameters: none\n"
            + "Example: " + COMMAND_WORD;

    public static final String SHOWING_TUTORIAL_MESSAGE = "Opened tutorial.";

    @Override
    public CommandResult execute(Model model) {
        return CommandResult.showingTutorial(SHOWING_TUTORIAL_MESSAGE);
    }
}
