package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.Model;
import coordimate.model.event.Event;

/**
 * Creates an event unless its name is already in use.
 */
public class AddEventCommand extends Command {
    public static final String COMMAND_WORD = "addevent";
    public static final String MESSAGE_USAGE = "addevent evn/EVENT_NAME st/START_TIME et/END_TIME";
    public static final String MESSAGE_SUCCESS = "Created Event %s. Start Time: %s. End Time: %s.";
    public static final String MESSAGE_DUPLICATE_EVENT = "An event with this name already exists.";
    public static final String MESSAGE_SAVE_ERROR = "Contact could not be saved. No changes were made.";
    public static final String MESSAGE_LOAD_ERROR =
            "Contact data could not be loaded. Please check the local data file.";

    private final Event toAdd;

    public AddEventCommand(Event event) {
        toAdd = requireNonNull(event);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        if (model.hasEvent(toAdd)) {
            throw new CommandException(MESSAGE_DUPLICATE_EVENT);
        }
        model.addEvent(toAdd);
        return new CommandResult(String.format(MESSAGE_SUCCESS,
                toAdd.getName(), toAdd.getStartTime(), toAdd.getEndTime()));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AddEventCommand otherCommand && toAdd.equals(otherCommand.toAdd);
    }

    @Override
    public int hashCode() {
        return toAdd.hashCode();
    }
}
