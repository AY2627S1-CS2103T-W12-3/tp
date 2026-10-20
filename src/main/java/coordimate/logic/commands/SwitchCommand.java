package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

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

    /** The available views. */
    public enum View {
        CONTACTS("Contacts"), EVENTS("Events"), TAGS("Tags");

        private final String displayName;

        View(String displayName) {
            this.displayName = displayName;
        }
    }

    private final View view;

    public SwitchCommand(View view) {
        this.view = requireNonNull(view);
    }

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        return new CommandResult(String.format(MESSAGE_SUCCESS, view.displayName), false, false,
                view == View.TAGS, view == View.CONTACTS, view == View.EVENTS);
    }
}
