package coordimate.model;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Objects;

import coordimate.commons.util.ToStringBuilder;
import coordimate.model.event.Event;
import coordimate.model.person.Name;
import coordimate.model.person.Person;
import coordimate.model.person.UniquePersonList;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Wraps all data at the CoordiMate level.
 * Duplicates are not allowed (by .isSamePerson comparison).
 */
public class CoordiMate implements ReadOnlyCoordiMate {

    private final UniquePersonList persons = new UniquePersonList();
    private final ObservableList<Event> events = FXCollections.observableArrayList();
    private final ObservableList<Event> unmodifiableEvents = FXCollections.unmodifiableObservableList(events);

    /**
     * Creates an empty contact and event store.
     */
    public CoordiMate() {}

    /**
     * Creates a CoordiMate using the people and events in {@code toBeCopied}.
     */
    public CoordiMate(ReadOnlyCoordiMate toBeCopied) {
        this();
        resetData(toBeCopied);
    }

    //// list overwrite operations

    /**
     * Replaces the contents of the person list with {@code persons}.
     * {@code persons} must not contain duplicate persons.
     */
    public void setPersons(List<Person> persons) {
        this.persons.setPersons(persons);
    }

    /**
     * Resets the existing data of this {@code CoordiMate} with {@code newData}.
     */
    public void resetData(ReadOnlyCoordiMate newData) {
        requireNonNull(newData);

        setPersons(newData.getPersonList());
        setEvents(newData.getEventList());
    }

    /**
     * Rejects duplicate names before replacing the event list.
     */
    public void setEvents(List<Event> events) {
        requireNonNull(events);
        for (int i = 0; i < events.size(); i++) {
            requireNonNull(events.get(i));
            for (int j = 0; j < i; j++) {
                if (events.get(i).isSameEvent(events.get(j))) {
                    throw new IllegalArgumentException("An event with this name already exists.");
                }
            }
        }
        this.events.setAll(events);
    }

    /**
     * Returns true if an event with the same name exists, regardless of its timings.
     */
    public boolean hasEvent(Event event) {
        requireNonNull(event);
        return events.stream().anyMatch(event::isSameEvent);
    }

    /**
     * Rejects an event if its name is already in use.
     */
    public void addEvent(Event event) {
        if (hasEvent(event)) {
            throw new IllegalArgumentException("An event with this name already exists.");
        }
        events.add(event);
    }

    /**
     * Replaces the target in place after checking the new name for duplicates.
     */
    public void setEvent(Event target, Event editedEvent) {
        requireNonNull(target);
        requireNonNull(editedEvent);
        int index = events.indexOf(target);
        if (index < 0) {
            throw new IllegalArgumentException("Event does not exist.");
        }
        for (int i = 0; i < events.size(); i++) {
            if (i != index && editedEvent.isSameEvent(events.get(i))) {
                throw new IllegalArgumentException("An event with this name already exists.");
            }
        }
        events.set(index, editedEvent);
    }

    @Override
    public ObservableList<Event> getEventList() {
        return unmodifiableEvents;
    }

    /**
     * Removes an existing event and notifies observers of the event list.
     */
    public void removeEvent(Event target) {
        requireNonNull(target);
        if (!events.remove(target)) {
            throw new IllegalArgumentException("Event does not exist.");
        }
    }

    //// person-level operations

    /**
     * Returns true if a person with the same identity as {@code person} exists in the CoordiMate.
     */
    public boolean hasPerson(Person person) {
        requireNonNull(person);
        return persons.contains(person);
    }

    /**
     * Adds a person to the CoordiMate.
     * The person must not already exist in the CoordiMate.
     */
    public void addPerson(Person p) {
        persons.add(p);
    }

    /**
     * Replaces the given person {@code target} in the list with {@code editedPerson}.
     * {@code target} must exist in the CoordiMate.
     * The person identity of {@code editedPerson} must not be the same as another existing person in the CoordiMate.
     * Events that list {@code target} as a member are updated to the edited name.
     */
    public void setPerson(Person target, Person editedPerson) {
        requireNonNull(editedPerson);

        persons.setPerson(target, editedPerson);
        if (!target.getName().equals(editedPerson.getName())) {
            replaceMemberInEvents(target.getName(), editedPerson.getName());
        }
    }

    /**
     * Removes {@code key} from this {@code CoordiMate}.
     * {@code key} must exist in the CoordiMate.
     * {@code key} is also removed from the members of every event.
     */
    public void removePerson(Person key) {
        persons.remove(key);
        replaceMemberInEvents(key.getName(), null);
    }

    /**
     * Replaces {@code oldName} with {@code newName} in every event that lists it as a member,
     * keeping each member's position. Removes {@code oldName} instead if {@code newName} is null.
     */
    private void replaceMemberInEvents(Name oldName, Name newName) {
        for (int i = 0; i < events.size(); i++) {
            Event event = events.get(i);
            if (!event.hasMember(oldName)) {
                continue;
            }
            List<Name> members = event.getMembers().stream()
                    .map(member -> member.equals(oldName) ? newName : member)
                    .filter(Objects::nonNull)
                    .toList();
            events.set(i, new Event(event.getName(), event.getStartTime(), event.getEndTime(), members));
        }
    }

    //// util methods

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("persons", persons)
                .add("events", events)
                .toString();
    }

    @Override
    public ObservableList<Person> getPersonList() {
        return persons.asUnmodifiableObservableList();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof CoordiMate otherCoordiMate)) {
            return false;
        }

        return persons.equals(otherCoordiMate.persons) && events.equals(otherCoordiMate.events);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(persons, events);
    }
}
