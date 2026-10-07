package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.Model;
import coordimate.model.event.Event;

/**
 * Deletes an event whose name matches the supplied name, ignoring case.
 */
public class DeleteEventCommand extends Command {
    public static final String COMMAND_WORD = "deleteevent";
    public static final String MESSAGE_USAGE = "deleteevent evn/EVENT_NAME";
    public static final String MESSAGE_SUCCESS = "Deleted Event %s.";
    public static final String MESSAGE_EVENT_NOT_FOUND = "Event %s does not exist.";
    public static final String MESSAGE_SAVE_ERROR = "Event could not be deleted. No changes were made.";

    private final String eventName;

    /**
     * Creates a deletion for the supplied event name, trimming surrounding whitespace.
     */
    public DeleteEventCommand(String eventName) {
        this.eventName = requireNonNull(eventName).strip();
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        Event target = model.getCoordiMate().getEventList().stream()
                .filter(event -> event.getName().equalsIgnoreCase(eventName)).findFirst()
                .orElseThrow(() -> new CommandException(String.format(MESSAGE_EVENT_NOT_FOUND, eventName)));
        model.deleteEvent(target);
        return new CommandResult(String.format(MESSAGE_SUCCESS, target.getName()));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof DeleteEventCommand otherCommand && eventName.equals(otherCommand.eventName);
    }

    @Override
    public int hashCode() {
        return eventName.hashCode();
    }
}
