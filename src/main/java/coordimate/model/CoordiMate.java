package coordimate.model;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import coordimate.commons.util.ToStringBuilder;
import coordimate.model.event.AttendanceStatus;
import coordimate.model.event.Event;
import coordimate.model.event.MemberNameConflictException;
import coordimate.model.person.MemberId;
import coordimate.model.person.Name;
import coordimate.model.person.Person;
import coordimate.model.person.UniquePersonList;
import coordimate.model.person.exceptions.DuplicatePersonException;
import coordimate.model.tag.Tag;
import coordimate.model.tag.UniqueTagList;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Wraps all data at the CoordiMate level.
 * Contacts have unique member IDs as well as distinct names, phone numbers, and email addresses.
 */
public class CoordiMate implements ReadOnlyCoordiMate {

    private static final List<Tag> DEFAULT_TAGS = List.of(
            new Tag("EXCO"),
            new Tag("Sponsor"),
            new Tag("UniversityStaff"),
            new Tag("Logistics"));

    private final UniquePersonList persons = new UniquePersonList();
    private long nextMemberId = 1;
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
     * Assigns missing IDs after reserving the supplied IDs, retaining the allocation state.
     */
    public void setPersons(List<Person> persons) {
        requireNonNull(persons);
        UniquePersonList replacement = new UniquePersonList();
        replacement.setPersons(persons);
        long candidateNextId = nextMemberId;
        for (Person person : persons) {
            if (person.getMemberId().isPresent()) {
                candidateNextId = Math.max(candidateNextId, person.getMemberId().orElseThrow().getValue() + 1L);
            }
        }
        List<Person> identifiedPersons = new ArrayList<>();
        for (Person person : persons) {
            if (person.getMemberId().isEmpty()) {
                person = person.withMemberId(createMemberId(candidateNextId));
                candidateNextId++;
            }
            identifiedPersons.add(person);
        }
        replacement.setPersons(identifiedPersons);
        this.persons.setPersons(replacement);
        nextMemberId = candidateNextId;
        registerTagsFromPersons(identifiedPersons);
    }

    /**
     * Replaces the saved tags, including with an empty list.
     */
    public void setTags(List<Tag> tags) {
        requireNonNull(tags);
        this.tags.setTags(tags);
        registerTagsFromPersons(persons.asUnmodifiableObservableList());
    }

    /**
     * Resets the existing data of this {@code CoordiMate} with {@code newData}.
     */
    public void resetData(ReadOnlyCoordiMate newData) {
        requireNonNull(newData);

        CoordiMate replacement = new CoordiMate();
        long incomingNextId = newData.getNextMemberId();
        if (incomingNextId < 1 || incomingNextId > Integer.MAX_VALUE + 1L) {
            throw new IllegalArgumentException("Invalid next member ID.");
        }
        replacement.nextMemberId = incomingNextId;
        replacement.setEvents(newData.getEventList());
        replacement.setPersons(newData.getPersonList());
        replacement.setTags(newData.getTagList());

        // Events are replaced first: replacing the persons makes filtered person lists re-check their
        // predicates, and predicates such as EventMembersPredicate must then see the new events.
        setEvents(replacement.getEventList());
        persons.setPersons(replacement.persons);
        nextMemberId = replacement.nextMemberId;
        tags.setTags(replacement.getTagList());
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
     * Removes {@code target} from the saved tag list and from every contact that uses it.
     */
    public void removeTag(Tag target) {
        requireNonNull(target);
        if (!hasTag(target)) {
            throw new IllegalArgumentException("Tag does not exist.");
        }

        List<Tag> remainingTags = tags.asUnmodifiableObservableList().stream()
                .filter(tag -> !tag.isSameTag(target))
                .toList();
        List<Person> updatedPersons = persons.asUnmodifiableObservableList().stream()
                .map(person -> removeTagFromPerson(person, target))
                .toList();
        tags.setTags(remainingTags);
        persons.setPersons(updatedPersons);
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
     * Assigns an ID if absent, or reserves the supplied ID for a restored contact.
     */
    public void addPerson(Person p) {
        requireNonNull(p);
        if (persons.contains(p)) {
            throw new DuplicatePersonException();
        }
        if (p.getMemberId().isEmpty()) {
            p = p.withMemberId(createMemberId(nextMemberId));
        }
        persons.add(p);
        nextMemberId = Math.max(nextMemberId, p.getMemberId().orElseThrow().getValue() + 1L);
        registerTagsFromPersons(List.of(p));
    }

    @Override
    public long getNextMemberId() {
        return nextMemberId;
    }

    private static MemberId createMemberId(long value) {
        if (value > Integer.MAX_VALUE) {
            throw new IllegalStateException("No more member IDs are available.");
        }
        return new MemberId((int) value);
    }

    /**
     * Replaces the given person {@code target} in the list with {@code editedPerson}.
     * {@code target} must exist in the CoordiMate.
     * The person identity of {@code editedPerson} must not be the same as another existing person in the CoordiMate.
     * Events that list {@code target} as a member are updated to the edited name.
     */
    public void setPerson(Person target, Person editedPerson) {
        requireNonNull(target);
        requireNonNull(editedPerson);

        List<Event> updatedEvents = null;
        if (!target.getName().equals(editedPerson.getName())) {
            updatedEvents = getEventsWithReplacedMember(target.getName(), editedPerson.getName());
        }
        persons.setPerson(target, editedPerson);
        registerTagsFromPersons(List.of(editedPerson));
        if (updatedEvents != null) {
            events.setAll(updatedEvents);
        }
    }

    /**
     * Removes {@code key} from this {@code CoordiMate}.
     * {@code key} must exist in the CoordiMate.
     * {@code key} is also removed from the members of every event.
     */
    public void removePerson(Person key) {
        List<Event> updatedEvents = getEventsWithReplacedMember(key.getName(), null);
        persons.remove(key);
        events.setAll(updatedEvents);
    }

    /**
     * Prepares event rosters with {@code oldName} replaced by {@code newName}, keeping each member's position
     * and moving {@code oldName}'s attendance to {@code newName}.
     * Removes {@code oldName} and its attendance instead if {@code newName} is null.
     */
    private List<Event> getEventsWithReplacedMember(Name oldName, Name newName) {
        List<Event> updatedEvents = new ArrayList<>();
        for (Event event : events) {
            if (!event.hasMember(oldName)) {
                updatedEvents.add(event);
                continue;
            }
            if (newName != null && event.hasMember(newName)) {
                throw new MemberNameConflictException();
            }
            List<Name> members = event.getMembers().stream()
                    .map(member -> member.equals(oldName) ? newName : member)
                    .filter(Objects::nonNull)
                    .toList();
            Map<Name, AttendanceStatus> attendance = new HashMap<>(event.getAttendanceRecord());
            AttendanceStatus oldStatus = attendance.remove(oldName);
            if (newName != null && oldStatus != null) {
                attendance.put(newName, oldStatus);
            }
            updatedEvents.add(event.withMembersAndAttendance(members, attendance));
        }
        return updatedEvents;
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
        return new Person(person.getMemberId().orElse(null), person.getName(), person.getPhone(), person.getEmail(),
                person.getRole(),
                person.getBirthday().orElse(null), person.getAddress().orElse(null),
                person.getOrganisation().orElse(null), person.getNote().orElse(null), editedTags);
    }

    /**
     * Returns a copy of {@code person} without {@code target}, or the original person if it does not use the tag.
     */
    private Person removeTagFromPerson(Person person, Tag target) {
        if (person.getTags().stream().noneMatch(tag -> tag.isSameTag(target))) {
            return person;
        }
        Set<Tag> remainingTags = person.getTags().stream()
                .filter(tag -> !tag.isSameTag(target))
                .collect(Collectors.toSet());
        return new Person(person.getMemberId().orElse(null), person.getName(), person.getPhone(), person.getEmail(),
                person.getRole(),
                person.getBirthday().orElse(null), person.getAddress().orElse(null),
                person.getOrganisation().orElse(null), person.getNote().orElse(null), remainingTags);
    }

    /**
     * Registers tags used by {@code personsToRegister} that are not already in the saved tag list.
     * Existing tag order and capitalization are preserved; missing tags are appended alphabetically.
     */
    private void registerTagsFromPersons(List<Person> personsToRegister) {
        personsToRegister.stream()
                .flatMap(person -> person.getTags().stream())
                .sorted(Comparator.comparing(Tag::getTagName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Tag::getTagName))
                .forEach(tag -> {
                    if (!hasTag(tag)) {
                        tags.add(tag);
                    }
                });
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
                && nextMemberId == otherCoordiMate.nextMemberId
                && tags.equals(otherCoordiMate.tags)
                && events.equals(otherCoordiMate.events);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(persons, nextMemberId, tags, events);
    }
}
