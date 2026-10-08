package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import coordimate.commons.core.index.Index;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.Model;
import coordimate.model.event.AttendanceStatus;
import coordimate.model.event.Event;
import coordimate.model.person.Name;

/**
 * Removes contacts from the displayed contact list from the members of the event identified by its name,
 * ignoring case. Their attendance for the event is removed too; other members keep their order and attendance.
 */
public class UnassignCommand extends EventMembersCommand {
    public static final String COMMAND_WORD = "unassign";
    public static final String MESSAGE_USAGE = "unassign evn/EVENT_NAME c/CONTACT_INDEX [MORE_CONTACT_INDEXES]...";
    public static final String MESSAGE_SUCCESS = "Removed %d member(s) from %s.";
    public static final String MESSAGE_SOME_NOT_ASSIGNED = " %d contact(s) were not assigned.";
    public static final String MESSAGE_NONE_ASSIGNED =
            "None of the specified contacts are assigned to %s. No changes were made.";
    public static final String MESSAGE_SAVE_ERROR = "Unassignment could not be saved. No changes were made.";

    /**
     * Creates a removal of the contacts at the given displayed indexes from the named event.
     */
    public UnassignCommand(String eventName, List<Index> contactIndexes) {
        super(eventName, contactIndexes);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        Event target = findEvent(model);
        Set<Name> selected = resolveContacts(model);

        long removedCount = selected.stream().filter(target::hasMember).count();
        if (removedCount == 0) {
            throw new CommandException(String.format(MESSAGE_NONE_ASSIGNED, target.getName()));
        }
        List<Name> remainingMembers = target.getMembers().stream()
                .filter(member -> !selected.contains(member))
                .toList();
        Map<Name, AttendanceStatus> remainingAttendance = new HashMap<>(target.getAttendanceRecord());
        remainingAttendance.keySet().removeAll(selected);
        model.setEvent(target, new Event(target.getName(), target.getStartTime(), target.getEndTime(),
                remainingMembers, remainingAttendance));

        long notAssigned = selected.size() - removedCount;
        String message = String.format(MESSAGE_SUCCESS, removedCount, target.getName());
        if (notAssigned > 0) {
            message += String.format(MESSAGE_SOME_NOT_ASSIGNED, notAssigned);
        }
        return new CommandResult(message);
    }
}
