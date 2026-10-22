package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import coordimate.logic.Messages;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.Model;
import coordimate.model.event.Event;

/**
 * Archives or unarchives the event whose name matches the supplied name, ignoring case.
 * An archived event keeps its details, members and attendance, but is hidden from the event list
 * and produces no reminders.
 */
public class ArchiveEventCommand extends Command {
    public static final String COMMAND_WORD = "archiveevent";
    public static final String UNARCHIVE_COMMAND_WORD = "unarchiveevent";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Archives an event, hiding it from the "
            + "event list and reminders.\n"
            + "Parameters: evn/EVENT_NAME\n"
            + "Example: " + COMMAND_WORD + " evn/Final Concert";
    public static final String MESSAGE_UNARCHIVE_USAGE = UNARCHIVE_COMMAND_WORD + ": Restores an archived "
            + "event to the event list.\n"
            + "Parameters: evn/EVENT_NAME\n"
            + "Example: " + UNARCHIVE_COMMAND_WORD + " evn/Final Concert";
    public static final String MESSAGE_ARCHIVE_SUCCESS = "Archived Event %s.";
    public static final String MESSAGE_UNARCHIVE_SUCCESS = "Unarchived Event %s.";
    public static final String MESSAGE_ALREADY_ARCHIVED = "Event %s is already archived. No changes were made.";
    public static final String MESSAGE_NOT_ARCHIVED = "Event %s is not archived. No changes were made.";
    public static final String MESSAGE_EVENT_NOT_FOUND = Messages.MESSAGE_EVENT_NOT_FOUND;
    public static final String MESSAGE_SAVE_ERROR = "Event archive status could not be saved. No changes were made.";

    private final String eventName;
    private final boolean archive;

    /**
     * Creates a command that archives the named event if {@code archive} is true, or unarchives it otherwise.
     */
    public ArchiveEventCommand(String eventName, boolean archive) {
        this.eventName = requireNonNull(eventName).strip();
        this.archive = archive;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        Event target = model.getCoordiMate().findEvent(eventName)
                .orElseThrow(() -> new CommandException(String.format(MESSAGE_EVENT_NOT_FOUND, eventName)));
        if (target.isArchived() == archive) {
            throw new CommandException(String.format(archive ? MESSAGE_ALREADY_ARCHIVED : MESSAGE_NOT_ARCHIVED,
                    target.getName()));
        }
        model.setEvent(target, target.withArchived(archive));
        return new CommandResult(String.format(archive ? MESSAGE_ARCHIVE_SUCCESS : MESSAGE_UNARCHIVE_SUCCESS,
                target.getName()));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ArchiveEventCommand otherCommand && eventName.equals(otherCommand.eventName)
                && archive == otherCommand.archive;
    }

    @Override
    public int hashCode() {
        return eventName.hashCode() * 31 + Boolean.hashCode(archive);
    }
}
