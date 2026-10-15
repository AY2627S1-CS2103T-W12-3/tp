package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

import coordimate.logic.Messages;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.Model;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;

/**
 * Edits the event identified by its name, ignoring case and retaining fields omitted by the user.
 */
public class EditEventCommand extends Command {
    public static final String COMMAND_WORD = "editevent";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Edits an event.\n"
            + "Parameters: evn/EVENT_NAME [nevn/NEW_EVENT_NAME] [st/NEW_START_TIME] [et/NEW_END_TIME]\n"
            + "At least one of nevn/, st/, or et/ is required.\n"
            + "Example: " + COMMAND_WORD + " evn/Final Concert st/08-08-2026 16:00";
    public static final String MESSAGE_SUCCESS = "Edited Event %s. Start Time: %s. End Time: %s.";
    public static final String MESSAGE_EVENT_NOT_FOUND = Messages.MESSAGE_EVENT_NOT_FOUND;
    public static final String MESSAGE_NOT_EDITED = "Provide at least one field to edit.\n" + MESSAGE_USAGE;
    public static final String MESSAGE_EMPTY_NEW_NAME = "New event name must not be empty. Example: nevn/Final Concert";
    public static final String MESSAGE_TIME_FORMAT =
            EventTime.MESSAGE_CONSTRAINTS;
    public static final String MESSAGE_DUPLICATE_EVENT =
            "This update conflicts with another saved event. No changes were made.";
    public static final String MESSAGE_SAVE_ERROR = "Event could not be saved. No changes were made.";

    private final String eventName;
    private final String newName;
    private final EventTime newStartTime;
    private final EventTime newEndTime;

    /**
     * Creates an edit; null optional fields retain their existing values.
     */
    public EditEventCommand(String eventName, String newName, EventTime newStartTime, EventTime newEndTime) {
        this.eventName = requireNonNull(eventName).strip();
        this.newName = newName == null ? null : newName.strip();
        this.newStartTime = newStartTime;
        this.newEndTime = newEndTime;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        validateEditRequest();
        Event target = findTargetEvent(model);
        Event editedEvent = createEditedEvent(target);
        if (!target.isSameEvent(editedEvent) && model.hasEvent(editedEvent)) {
            throw new CommandException(MESSAGE_DUPLICATE_EVENT);
        }

        model.setEvent(target, editedEvent);
        return new CommandResult(String.format(MESSAGE_SUCCESS,
                editedEvent.getName(), editedEvent.getStartTime(), editedEvent.getEndTime()));
    }

    private void validateEditRequest() throws CommandException {
        if (newName == null && newStartTime == null && newEndTime == null) {
            throw new CommandException(MESSAGE_NOT_EDITED);
        }
        if (newName != null && newName.isEmpty()) {
            throw new CommandException(MESSAGE_EMPTY_NEW_NAME);
        }
    }

    private Event findTargetEvent(Model model) throws CommandException {
        return model.getCoordiMate().getEventList().stream()
                .filter(event -> event.getName().equalsIgnoreCase(eventName)).findFirst()
                .orElseThrow(() -> new CommandException(String.format(MESSAGE_EVENT_NOT_FOUND, eventName)));
    }

    private Event createEditedEvent(Event target) throws CommandException {
        try {
            return new Event(newName == null ? target.getName() : newName,
                    newStartTime == null ? target.getStartTime() : newStartTime,
                    newEndTime == null ? target.getEndTime() : newEndTime, target.getMembers(),
                    target.getAttendanceRecord());
        } catch (IllegalArgumentException e) {
            throw new CommandException(e.getMessage(), e);
        }
    }

    /**
     * Returns true if this command gives a new name to the event named {@code name}, ignoring case.
     */
    public boolean renamesEvent(String name) {
        return newName != null && eventName.equalsIgnoreCase(name.strip());
    }

    /**
     * Returns the new event name, or null if this command keeps the existing name.
     */
    public String getNewName() {
        return newName;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof EditEventCommand otherCommand && eventName.equals(otherCommand.eventName)
                && Objects.equals(newName, otherCommand.newName)
                && Objects.equals(newStartTime, otherCommand.newStartTime)
                && Objects.equals(newEndTime, otherCommand.newEndTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventName, newName, newStartTime, newEndTime);
    }
}
