package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.Model;
import coordimate.model.event.AttendanceStatus;
import coordimate.model.event.Event;
import coordimate.model.person.Name;

/**
 * Records whether a member assigned to an event was present or absent.
 */
public class MarkAttendanceCommand extends Command {

    public static final String COMMAND_WORD = "markattendance";
    public static final String MESSAGE_USAGE = "markattendance evn/EVENT_NAME mem/MEMBER_NAME att/STATUS";
    public static final String MESSAGE_SUCCESS = "Marked %s as %s for Event %s.";
    public static final String MESSAGE_ALREADY_MARKED =
            "%s is already marked as %s for Event %s. No changes were made.";
    public static final String MESSAGE_EVENT_NOT_FOUND = "Error! Event %s does not exist!";
    public static final String MESSAGE_MEMBER_NOT_ASSIGNED = "Error! %s is not assigned to event %s!";
    public static final String MESSAGE_SAVE_ERROR = "Attendance could not be saved. No changes were made.";

    private final String eventName;
    private final Name memberName;
    private final AttendanceStatus status;

    /**
     * Creates a command that marks {@code memberName}'s attendance for the named event.
     */
    public MarkAttendanceCommand(String eventName, Name memberName, AttendanceStatus status) {
        this.eventName = requireNonNull(eventName).strip();
        this.memberName = requireNonNull(memberName);
        this.status = requireNonNull(status);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        Event target = model.getCoordiMate().getEventList().stream()
                .filter(event -> event.getName().equalsIgnoreCase(eventName)).findFirst()
                .orElseThrow(() -> new CommandException(String.format(MESSAGE_EVENT_NOT_FOUND, eventName)));

        if (!target.hasMember(memberName)) {
            throw new CommandException(
                    String.format(MESSAGE_MEMBER_NOT_ASSIGNED, memberName, target.getName()));
        }

        if (target.getAttendance(memberName).map(status::equals).orElse(false)) {
            return new CommandResult(
                    String.format(MESSAGE_ALREADY_MARKED, memberName, status, target.getName()));
        }

        Event updated = target.withAttendance(memberName, status);
        model.setEvent(target, updated);

        return new CommandResult(String.format(MESSAGE_SUCCESS, memberName, status, target.getName()));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof MarkAttendanceCommand otherCommand)) {
            return false;
        }
        return eventName.equalsIgnoreCase(otherCommand.eventName) && memberName.equals(otherCommand.memberName)
                && status == otherCommand.status;
    }
}
