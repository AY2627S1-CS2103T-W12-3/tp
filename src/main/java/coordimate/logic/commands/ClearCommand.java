package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import coordimate.logic.Messages;
import coordimate.model.CoordiMate;
import coordimate.model.Model;

/**
 * Clears the CoordiMate.
 */
public class ClearCommand extends Command {

    public static final String COMMAND_WORD = "clear";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Clears CoordiMate.\n"
            + "Parameters: none\n"
            + "Example: " + COMMAND_WORD;
    public static final String MESSAGE_SUCCESS = "CoordiMate has been cleared!";
    public static final String MESSAGE_SAVE_ERROR = "CoordiMate could not be cleared. No changes were made.";
    public static final String MESSAGE_LOAD_ERROR =
            Messages.MESSAGE_LOAD_ERROR;

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.setCoordiMate(new CoordiMate());
        return new CommandResult(MESSAGE_SUCCESS);
    }
}
