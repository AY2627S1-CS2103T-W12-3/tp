package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.Model;
import coordimate.model.event.Event;
import coordimate.model.person.MemberId;
import coordimate.model.person.Name;
import coordimate.model.person.Person;

/**
 * Assigns contacts identified by permanent member IDs to the event identified by its name, ignoring case.
 */
public class AssignCommand extends EventMembersCommand {
    public static final String COMMAND_WORD = "assign";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Assigns contacts to an event.\n"
            + "Parameters: evn/EVENT_NAME c/MEMBER_ID [MORE_MEMBER_IDS]...\n"
            + "Example: " + COMMAND_WORD + " evn/Final Concert c/1 3";
    public static final String MESSAGE_SUCCESS = "Assigned %d member(s) to %s.";
    public static final String MESSAGE_SOME_ALREADY_ASSIGNED = " %d contact(s) were already assigned.";
    public static final String MESSAGE_ALL_ALREADY_ASSIGNED =
            "All specified contacts are already assigned to %s. No changes were made.";
    public static final String MESSAGE_SAVE_ERROR = "Assignment could not be saved. No changes were made.";
    public static final String MESSAGE_CONTACT_ARCHIVED = "%1$s is archived, so it cannot be assigned. "
            + "No changes were made. Unarchive it first with: unarchive %2$d";
    public static final String MESSAGE_CONTACT_ON_LEAVE = "%1$s is on leave until %2$s, "
            + "covering the entire event %3$s. No changes were made.";

    /**
     * Creates an assignment of the contacts with the given permanent member IDs to the named event.
     */
    public AssignCommand(String eventName, List<MemberId> memberIds) {
        super(eventName, memberIds);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        Event target = findEvent(model);
        Set<Person> contacts = resolvePersons(model);
        for (Person contact : contacts) {
            if (contact.isArchived()) {
                throw new CommandException(String.format(MESSAGE_CONTACT_ARCHIVED, contact.getName(),
                        contact.getMemberId().orElseThrow().getValue()));
            }
            if (!target.hasMember(contact.getName()) && contact.isOnLeave(target.getEndTime().getDate())) {
                throw new CommandException(String.format(MESSAGE_CONTACT_ON_LEAVE, contact.getName(),
                        contact.getLeaveDate().orElseThrow(), target.getName()));
            }
        }
        Set<Name> selected = new LinkedHashSet<>();
        contacts.forEach(contact -> selected.add(contact.getName()));

        List<Name> newMembers = selected.stream().filter(name -> !target.hasMember(name)).toList();
        if (newMembers.isEmpty()) {
            throw new CommandException(String.format(MESSAGE_ALL_ALREADY_ASSIGNED, target.getName()));
        }
        List<Name> members = new ArrayList<>(target.getMembers());
        members.addAll(newMembers);
        model.setEvent(target, target.withMembersAndAttendance(members, target.getAttendanceRecord()));

        int alreadyAssigned = selected.size() - newMembers.size();
        String message = String.format(MESSAGE_SUCCESS, newMembers.size(), target.getName());
        if (alreadyAssigned > 0) {
            message += String.format(MESSAGE_SOME_ALREADY_ASSIGNED, alreadyAssigned);
        }
        return new CommandResult(message);
    }
}
