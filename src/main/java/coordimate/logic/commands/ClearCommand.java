package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import coordimate.model.CoordiMate;
import coordimate.model.Model;

/**
 * Clears the CoordiMate.
 */
public class ClearCommand extends Command {

    public static final String COMMAND_WORD = "clear";
    public static final String MESSAGE_SUCCESS = "CoordiMate has been cleared!";


    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.setCoordiMate(new CoordiMate());
        return new CommandResult(MESSAGE_SUCCESS);
    }
}
