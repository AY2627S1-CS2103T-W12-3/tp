package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
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
    public static final String MESSAGE_NONE_ASSIGNED = "No new members could be assigned to %s. No changes were made.";
    public static final String MESSAGE_SKIPPED_ON_LEAVE =
            " Skipped %d contact(s) on leave for the entire event: %s.";

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
        }
        List<Name> newMembers = new ArrayList<>();
        List<String> onLeave = new ArrayList<>();
        int alreadyAssigned = 0;
        for (Person contact : contacts) {
            if (target.hasMember(contact.getName())) {
                alreadyAssigned++;
            } else if (contact.isOnLeave(target.getEndTime().getDate())) {
                onLeave.add(contact.getName() + " (ID: " + contact.getMemberId().orElseThrow().getValue() + ")");
            } else {
                newMembers.add(contact.getName());
            }
        }
        if (newMembers.isEmpty() && onLeave.isEmpty()) {
            throw new CommandException(String.format(MESSAGE_ALL_ALREADY_ASSIGNED, target.getName()));
        }
        String message = newMembers.isEmpty() ? String.format(MESSAGE_NONE_ASSIGNED, target.getName())
                : String.format(MESSAGE_SUCCESS, newMembers.size(), target.getName());
        if (alreadyAssigned > 0) {
            message += String.format(MESSAGE_SOME_ALREADY_ASSIGNED, alreadyAssigned);
        }
        if (!onLeave.isEmpty()) {
            message += String.format(MESSAGE_SKIPPED_ON_LEAVE, onLeave.size(), String.join(", ", onLeave));
        }
        if (newMembers.isEmpty()) {
            throw new CommandException(message);
        }
        List<Name> members = new ArrayList<>(target.getMembers());
        members.addAll(newMembers);
        model.setEvent(target, target.withMembersAndAttendance(members, target.getAttendanceRecord()));
        return new CommandResult(message);
    }
}
