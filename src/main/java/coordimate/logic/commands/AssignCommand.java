package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import coordimate.commons.core.index.Index;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.Model;
import coordimate.model.event.Event;
import coordimate.model.person.Name;
import coordimate.model.person.Person;

/**
 * Assigns contacts from the displayed contact list to the event identified by its name, ignoring case.
 */
public class AssignCommand extends Command {
    public static final String COMMAND_WORD = "assign";
    public static final String MESSAGE_USAGE = "assign evn/EVENT_NAME c/CONTACT_INDEX [MORE_CONTACT_INDEXES]...";
    public static final String MESSAGE_SUCCESS = "Assigned %d member(s) to %s.";
    public static final String MESSAGE_SOME_ALREADY_ASSIGNED = " %d contact(s) were already assigned.";
    public static final String MESSAGE_ALL_ALREADY_ASSIGNED =
            "All specified contacts are already assigned to %s. No changes were made.";
    public static final String MESSAGE_EVENT_NOT_FOUND = "Event %s does not exist.";
    public static final String MESSAGE_CONTACT_NOT_FOUND = "Contact %d does not exist in the displayed list.";
    public static final String MESSAGE_SAVE_ERROR = "Assignment could not be saved. No changes were made.";

    private final String eventName;
    private final List<Index> contactIndexes;

    /**
     * Creates an assignment of the contacts at the given displayed indexes to the named event.
     */
    public AssignCommand(String eventName, List<Index> contactIndexes) {
        this.eventName = requireNonNull(eventName).strip();
        this.contactIndexes = List.copyOf(contactIndexes);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        Event target = model.getCoordiMate().getEventList().stream()
                .filter(event -> event.getName().equalsIgnoreCase(eventName)).findFirst()
                .orElseThrow(() -> new CommandException(String.format(MESSAGE_EVENT_NOT_FOUND, eventName)));

        List<Person> displayedContacts = model.getFilteredPersonList();
        Set<Name> selected = new LinkedHashSet<>();
        for (Index index : contactIndexes) {
            if (index.getZeroBased() >= displayedContacts.size()) {
                throw new CommandException(String.format(MESSAGE_CONTACT_NOT_FOUND, index.getOneBased()));
            }
            selected.add(displayedContacts.get(index.getZeroBased()).getName());
        }

        List<Name> newMembers = selected.stream().filter(name -> !target.hasMember(name)).toList();
        if (newMembers.isEmpty()) {
            throw new CommandException(String.format(MESSAGE_ALL_ALREADY_ASSIGNED, target.getName()));
        }
        List<Name> members = new ArrayList<>(target.getMembers());
        members.addAll(newMembers);
        model.setEvent(target, new Event(target.getName(), target.getStartTime(), target.getEndTime(), members));

        int alreadyAssigned = selected.size() - newMembers.size();
        String message = String.format(MESSAGE_SUCCESS, newMembers.size(), target.getName());
        if (alreadyAssigned > 0) {
            message += String.format(MESSAGE_SOME_ALREADY_ASSIGNED, alreadyAssigned);
        }
        return new CommandResult(message);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AssignCommand otherCommand && eventName.equals(otherCommand.eventName)
                && contactIndexes.equals(otherCommand.contactIndexes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventName, contactIndexes.stream().map(Index::getZeroBased).toList());
    }
}
