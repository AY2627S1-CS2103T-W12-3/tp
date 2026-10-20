package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import coordimate.logic.commands.CommandResult.View;
import coordimate.model.Model;

/**
 * Requests that the UI show one of the main views.
 */
public class SwitchCommand extends Command {

    public static final String COMMAND_WORD = "switch";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Switches to a view.\n"
            + "Parameters: contacts | events | tags\n"
            + "Example: " + COMMAND_WORD + " events";
    public static final String MESSAGE_SUCCESS = "Switched to %s view.";

    private final View view;

    /** Creates a command to show the specified view. */
    public SwitchCommand(View view) {
        this.view = requireNonNull(view);
        if (view == View.NONE) {
            throw new IllegalArgumentException("SwitchCommand requires a view");
        }
    }

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        return new CommandResult(String.format(MESSAGE_SUCCESS, view.getDisplayName()), view);
    }
}
