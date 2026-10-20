package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import coordimate.logic.Messages;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.Model;
import coordimate.model.event.Event;

/**
 * Creates an event unless its name is already in use.
 */
public class AddEventCommand extends Command {
    public static final String COMMAND_WORD = "addevent";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Creates an event.\n"
            + "Parameters: evn/EVENT_NAME st/START_TIME et/END_TIME\n"
            + "Example: " + COMMAND_WORD + " evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00";
    public static final String MESSAGE_SUCCESS = "Created Event %s. Start Time: %s. End Time: %s.";
    public static final String MESSAGE_DUPLICATE_EVENT =
            "An event with this name already exists. No changes were made.";
    public static final String MESSAGE_DUPLICATE_ARCHIVED_EVENT = "An archived event named %1$s already exists. "
            + "No changes were made. To restore it, use: unarchiveevent evn/%1$s";
    public static final String MESSAGE_SAVE_ERROR = "Event could not be saved. No changes were made.";
    public static final String MESSAGE_LOAD_ERROR =
            Messages.MESSAGE_LOAD_ERROR;

    private final Event toAdd;

    /**
     * Creates a command that adds the given event.
     */
    public AddEventCommand(Event event) {
        toAdd = requireNonNull(event);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        if (model.hasEvent(toAdd)) {
            Event existing = EventMembersCommand.findEvent(model, toAdd.getName());
            throw new CommandException(existing.isArchived()
                    ? String.format(MESSAGE_DUPLICATE_ARCHIVED_EVENT, existing.getName())
                    : MESSAGE_DUPLICATE_EVENT);
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
