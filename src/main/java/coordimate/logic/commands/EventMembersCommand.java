package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import coordimate.logic.Messages;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.Model;
import coordimate.model.event.Event;
import coordimate.model.person.MemberId;
import coordimate.model.person.Name;
import coordimate.model.person.Person;

/**
 * Changes the members of the event identified by its name, ignoring case, using permanent member IDs.
 */
public abstract class EventMembersCommand extends Command {
    public static final String MESSAGE_EVENT_NOT_FOUND = Messages.MESSAGE_EVENT_NOT_FOUND;
    public static final String MESSAGE_UNKNOWN_MEMBER_ID = Messages.MESSAGE_MEMBER_ID_NOT_FOUND;
    public static final String MESSAGE_EVENT_ARCHIVED = "Event %1$s is archived, so its members cannot be "
            + "changed. Unarchive it first with: unarchiveevent evn/%1$s";

    private final String eventName;
    private final List<MemberId> memberIds;

    /**
     * Creates a command for the named event and the contacts with the given permanent IDs.
     */
    protected EventMembersCommand(String eventName, List<MemberId> memberIds) {
        this.eventName = requireNonNull(eventName).strip();
        this.memberIds = List.copyOf(memberIds);
    }

    /**
     * Returns the saved event whose name matches this command's event name, ignoring case.
     * Archived events are rejected, since their members are kept as a historical record.
     *
     * @throws CommandException If no saved event has that name, or the event is archived.
     */
    protected Event findEvent(Model model) throws CommandException {
        Event event = findEvent(model, eventName);
        if (event.isArchived()) {
            throw new CommandException(String.format(MESSAGE_EVENT_ARCHIVED, event.getName()));
        }
        return event;
    }

    /**
     * Returns the saved event whose name matches {@code eventName}, ignoring case.
     *
     * @throws CommandException If no saved event has that name.
     */
    static Event findEvent(Model model, String eventName) throws CommandException {
        return model.getCoordiMate().findEvent(eventName)
                .orElseThrow(() -> new CommandException(String.format(MESSAGE_EVENT_NOT_FOUND, eventName)));
    }

    /**
     * Resolves IDs across all saved contacts, retaining command order and counting repeated contacts once.
     * Every ID is checked before any name is returned, so an unknown ID cannot cause a partial update.
     *
     * @throws CommandException If a member ID does not exist.
     */
    protected Set<Name> resolveContacts(Model model) throws CommandException {
        Set<Name> selected = new LinkedHashSet<>();
        for (Person person : resolvePersons(model)) {
            selected.add(person.getName());
        }
        return selected;
    }

    /**
     * Resolves IDs to saved contacts in command order, counting repeated contacts once.
     * Every ID is checked before any contact is returned.
     *
     * @throws CommandException If a member ID does not exist.
     */
    protected Set<Person> resolvePersons(Model model) throws CommandException {
        Set<Person> selected = new LinkedHashSet<>();
        for (MemberId memberId : memberIds) {
            selected.add(MemberIdResolver.resolve(model, memberId));
        }
        return selected;
    }

    @Override
    public boolean equals(Object other) {
        return other != null && other.getClass() == getClass()
                && eventName.equals(((EventMembersCommand) other).eventName)
                && memberIds.equals(((EventMembersCommand) other).memberIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), eventName, memberIds);
    }
}
