package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import coordimate.commons.core.index.Index;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.Model;
import coordimate.model.event.Event;
import coordimate.model.person.Name;

/**
 * Assigns contacts from the displayed contact list to the event identified by its name, ignoring case.
 */
public class AssignCommand extends EventMembersCommand {
    public static final String COMMAND_WORD = "assign";
    public static final String MESSAGE_USAGE = "assign evn/EVENT_NAME c/CONTACT_INDEX [MORE_CONTACT_INDEXES]...";
    public static final String MESSAGE_SUCCESS = "Assigned %d member(s) to %s.";
    public static final String MESSAGE_SOME_ALREADY_ASSIGNED = " %d contact(s) were already assigned.";
    public static final String MESSAGE_ALL_ALREADY_ASSIGNED =
            "All specified contacts are already assigned to %s. No changes were made.";
    public static final String MESSAGE_SAVE_ERROR = "Assignment could not be saved. No changes were made.";

    /**
     * Creates an assignment of the contacts at the given displayed indexes to the named event.
     */
    public AssignCommand(String eventName, List<Index> contactIndexes) {
        super(eventName, contactIndexes);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        Event target = findEvent(model);
        Set<Name> selected = resolveContacts(model);

        List<Name> newMembers = selected.stream().filter(name -> !target.hasMember(name)).toList();
        if (newMembers.isEmpty()) {
            throw new CommandException(String.format(MESSAGE_ALL_ALREADY_ASSIGNED, target.getName()));
        }
        List<Name> members = new ArrayList<>(target.getMembers());
        members.addAll(newMembers);
        model.setEvent(target, new Event(target.getName(), target.getStartTime(), target.getEndTime(), members,
                target.getAttendanceRecord()));

        int alreadyAssigned = selected.size() - newMembers.size();
        String message = String.format(MESSAGE_SUCCESS, newMembers.size(), target.getName());
        if (alreadyAssigned > 0) {
            message += String.format(MESSAGE_SOME_ALREADY_ASSIGNED, alreadyAssigned);
        }
        return new CommandResult(message);
    }
}
