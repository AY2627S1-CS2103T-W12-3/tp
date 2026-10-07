package coordimate.model;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import coordimate.commons.util.ToStringBuilder;
import coordimate.model.event.Event;
import coordimate.model.person.Name;
import coordimate.model.person.Person;
import coordimate.model.person.UniquePersonList;
import coordimate.model.tag.Tag;
import coordimate.model.tag.UniqueTagList;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Wraps all data at the CoordiMate level.
 * Duplicates are not allowed (by .isSamePerson comparison).
 */
public class CoordiMate implements ReadOnlyCoordiMate {

    private static final List<Tag> DEFAULT_TAGS = List.of(
            new Tag("EXCO"),
            new Tag("Sponsor"),
            new Tag("UniversityStaff"),
            new Tag("Logistics"));

    private final UniquePersonList persons = new UniquePersonList();
    private final UniqueTagList tags = new UniqueTagList();
    private final ObservableList<Event> events = FXCollections.observableArrayList();
    private final ObservableList<Event> unmodifiableEvents = FXCollections.unmodifiableObservableList(events);

    /**
     * Creates an empty contact and event store.
     */
    public CoordiMate() {
        tags.setTags(DEFAULT_TAGS);
    }

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
     * Replaces the saved tags. An empty replacement restores the initial default tags.
     */
    public void setTags(List<Tag> tags) {
        requireNonNull(tags);
        this.tags.setTags(tags.isEmpty() ? DEFAULT_TAGS : tags);
    }

    /**
     * Resets the existing data of this {@code CoordiMate} with {@code newData}.
     */
    public void resetData(ReadOnlyCoordiMate newData) {
        requireNonNull(newData);

        setPersons(newData.getPersonList());
        setTags(newData.getTagList());
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

    //// tag-level operations

    /**
     * Returns true if a tag with the same name exists, ignoring case.
     */
    public boolean hasTag(Tag tag) {
        requireNonNull(tag);
        return tags.contains(tag);
    }

    /**
     * Adds a tag whose name must not already exist, ignoring case.
     */
    public void addTag(Tag tag) {
        tags.add(tag);
    }

    /**
     * Replaces {@code target} with {@code editedTag} in the saved tag list and on every contact.
     * {@code target} must exist, and {@code editedTag} must not already exist.
     */
    public void setTag(Tag target, Tag editedTag) {
        requireNonNull(target);
        requireNonNull(editedTag);
        if (!hasTag(target)) {
            throw new IllegalArgumentException("Tag does not exist.");
        }
        if (hasTag(editedTag)) {
            throw new IllegalArgumentException("Tag already exists.");
        }

        List<Tag> editedTags = tags.asUnmodifiableObservableList().stream()
                .map(tag -> tag.isSameTag(target) ? editedTag : tag)
                .toList();
        List<Person> editedPersons = persons.asUnmodifiableObservableList().stream()
                .map(person -> replaceTag(person, target, editedTag))
                .toList();
        tags.setTags(editedTags);
        persons.setPersons(editedPersons);
    }

    @Override
    public ObservableList<Tag> getTagList() {
        return tags.asUnmodifiableObservableList();
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

    /**
     * Returns a copy of {@code person} with {@code target} replaced by {@code editedTag}.
     */
    private Person replaceTag(Person person, Tag target, Tag editedTag) {
        if (person.getTags().stream().noneMatch(tag -> tag.isSameTag(target))) {
            return person;
        }
        Set<Tag> editedTags = person.getTags().stream()
                .map(tag -> tag.isSameTag(target) ? editedTag : tag)
                .collect(Collectors.toSet());
        return new Person(person.getName(), person.getPhone(), person.getEmail(), person.getAddress(), editedTags);
    }

    //// util methods

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("persons", persons)
                .add("tags", tags)
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

        return persons.equals(otherCoordiMate.persons)
                && tags.equals(otherCoordiMate.tags)
                && events.equals(otherCoordiMate.events);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(persons, tags, events);
    }
}
