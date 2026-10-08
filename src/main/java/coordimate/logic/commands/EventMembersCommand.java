package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

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
 * Changes the members of the event identified by its name, ignoring case, using contacts chosen by their
 * indexes in the displayed contact list.
 */
public abstract class EventMembersCommand extends Command {
    public static final String MESSAGE_EVENT_NOT_FOUND = "Event %s does not exist.";
    public static final String MESSAGE_CONTACT_NOT_FOUND = "Contact %d does not exist in the displayed list.";

    private final String eventName;
    private final List<Index> contactIndexes;

    /**
     * Creates a command for the named event and the contacts at the given displayed indexes.
     */
    protected EventMembersCommand(String eventName, List<Index> contactIndexes) {
        this.eventName = requireNonNull(eventName).strip();
        this.contactIndexes = List.copyOf(contactIndexes);
    }

    /**
     * Returns the saved event whose name matches this command's event name, ignoring case.
     *
     * @throws CommandException If no saved event has that name.
     */
    protected Event findEvent(Model model) throws CommandException {
        return model.getCoordiMate().getEventList().stream()
                .filter(event -> event.getName().equalsIgnoreCase(eventName)).findFirst()
                .orElseThrow(() -> new CommandException(String.format(MESSAGE_EVENT_NOT_FOUND, eventName)));
    }

    /**
     * Returns the names of the contacts at this command's indexes in the displayed contact list, in the order
     * given, with repeated contacts counted once. Every index is checked before any name is returned.
     *
     * @throws CommandException If an index is outside the displayed contact list.
     */
    protected Set<Name> resolveContacts(Model model) throws CommandException {
        List<Person> displayedContacts = model.getFilteredPersonList();
        Set<Name> selected = new LinkedHashSet<>();
        for (Index index : contactIndexes) {
            if (index.getZeroBased() >= displayedContacts.size()) {
                throw new CommandException(String.format(MESSAGE_CONTACT_NOT_FOUND, index.getOneBased()));
            }
            selected.add(displayedContacts.get(index.getZeroBased()).getName());
        }
        return selected;
    }

    @Override
    public boolean equals(Object other) {
        return other != null && other.getClass() == getClass()
                && eventName.equals(((EventMembersCommand) other).eventName)
                && contactIndexes.equals(((EventMembersCommand) other).contactIndexes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), eventName, contactIndexes.stream().map(Index::getZeroBased).toList());
    }
}
