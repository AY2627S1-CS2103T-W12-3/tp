package coordimate.logic.commands;

import static coordimate.model.Model.PREDICATE_SHOW_ALL_PERSONS;
import static java.util.Objects.requireNonNull;

import coordimate.model.Model;

/**
 * Lists all active contacts and events, or all archived ones, removing any search, filter or members view.
 */
public class ListCommand extends Command {

    public static final String COMMAND_WORD = "list";
    public static final String ARCHIVED_OPTION = "archived";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Lists all contacts and events, or only "
            + "archived ones.\n"
            + "Parameters: [" + ARCHIVED_OPTION + "]\n"
            + "Example: " + COMMAND_WORD + " " + ARCHIVED_OPTION;

    public static final String MESSAGE_SUCCESS = "Listed all contacts.";
    public static final String MESSAGE_ARCHIVED_SUCCESS = "Listed archived contacts and events. "
            + "Use list to return to active contacts and events.";

    private final boolean showArchived;

    /**
     * Creates a command that lists active contacts and events.
     */
    public ListCommand() {
        this(false);
    }

    /**
     * Creates a command that lists archived contacts and events if {@code showArchived} is true,
     * or active ones otherwise.
     */
    public ListCommand(boolean showArchived) {
        this.showArchived = showArchived;
    }

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.setShowingArchived(showArchived);
        model.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
        return new CommandResult(showArchived ? MESSAGE_ARCHIVED_SUCCESS : MESSAGE_SUCCESS,
                CommandResult.View.CONTACTS);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ListCommand otherCommand && showArchived == otherCommand.showArchived;
    }

    @Override
    public int hashCode() {
        return Boolean.hashCode(showArchived);
    }
}
