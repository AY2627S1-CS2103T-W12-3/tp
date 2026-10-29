package coordimate.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.LongNode;

import coordimate.commons.exceptions.IllegalValueException;
import coordimate.model.CoordiMate;
import coordimate.model.ReadOnlyCoordiMate;
import coordimate.model.event.Event;
import coordimate.model.person.Name;
import coordimate.model.person.Person;
import coordimate.model.person.exceptions.DuplicateMemberIdException;
import coordimate.model.tag.Tag;
import coordimate.model.tag.exceptions.DuplicateTagException;

/**
 * An Immutable CoordiMate that is serializable to JSON format.
 */
@JsonRootName(value = "coordimate")
class JsonSerializableCoordiMate {

    public static final String MESSAGE_DUPLICATE_PERSON = "Persons list contains duplicate person(s).";
    public static final String MESSAGE_DUPLICATE_MEMBER_ID = "Persons list contains duplicate member IDs.";
    public static final String MESSAGE_INVALID_NEXT_MEMBER_ID = "Next member ID must be an integer from 1 to "
            + (Integer.MAX_VALUE + 1L) + " and greater than every saved member ID.";
    public static final String MESSAGE_MEMBER_IDS_EXHAUSTED =
            "No more member IDs are available for contacts without IDs.";
    public static final String MESSAGE_DUPLICATE_EVENT = "Events list contains duplicate event name(s).";
    public static final String MESSAGE_DUPLICATE_TAG = "Tags list contains duplicate tag name(s).";
    public static final String MESSAGE_UNKNOWN_MEMBER = "Event members must refer to existing contacts.";

    private static final Pattern LEGACY_WHITESPACE_RUN = Pattern.compile("\\p{javaWhitespace}+");

    private final List<JsonAdaptedPerson> persons = new ArrayList<>();
    private final List<JsonAdaptedEvent> events = new ArrayList<>();
    private final List<JsonAdaptedTag> tags = new ArrayList<>();
    private final boolean hasTagData;
    private final JsonNode nextMemberId;

    /**
     * Constructs a {@code JsonSerializableCoordiMate} with the given people and optional events.
     */
    @JsonCreator
    public JsonSerializableCoordiMate(@JsonProperty("persons") List<JsonAdaptedPerson> persons,
            @JsonProperty("events") List<JsonAdaptedEvent> events,
            @JsonProperty("tags") List<JsonAdaptedTag> tags,
            @JsonProperty("nextMemberId") JsonNode nextMemberId) {
        if (persons == null) {
            throw new IllegalArgumentException("Persons list must be present.");
        }
        this.persons.addAll(persons);
        if (events != null) {
            this.events.addAll(events);
        }
        if (tags != null) {
            this.tags.addAll(tags);
        }
        hasTagData = tags != null;
        this.nextMemberId = nextMemberId;
    }

    /**
     * Constructs a legacy store without saved ID allocation state.
     */
    public JsonSerializableCoordiMate(List<JsonAdaptedPerson> persons, List<JsonAdaptedEvent> events,
            List<JsonAdaptedTag> tags) {
        this(persons, events, tags, null);
    }

    /**
     * Constructs a serializable CoordiMate without an explicit tag list.
     */
    public JsonSerializableCoordiMate(List<JsonAdaptedPerson> persons, List<JsonAdaptedEvent> events) {
        this(persons, events, null);
    }

    /**
     * Converts a given {@code ReadOnlyCoordiMate} into this class for Jackson use.
     *
     * @param source future changes to this will not affect the created {@code JsonSerializableCoordiMate}.
     */
    public JsonSerializableCoordiMate(ReadOnlyCoordiMate source) {
        nextMemberId = LongNode.valueOf(source.getNextMemberId());
        persons.addAll(source.getPersonList().stream().map(JsonAdaptedPerson::new).collect(Collectors.toList()));
        events.addAll(source.getEventList().stream().map(JsonAdaptedEvent::new).collect(Collectors.toList()));
        tags.addAll(source.getTagList().stream().map(JsonAdaptedTag::new).collect(Collectors.toList()));
        hasTagData = true;
    }

    /**
     * Removes legacy events using year zero and returns their names for the startup notification.
     */
    List<String> removeYearZeroEvents() {
        List<String> removedNames = new ArrayList<>();
        events.removeIf(event -> {
            if (event != null && event.hasYearZeroEventDate()) {
                removedNames.add(event.getName() == null ? "(unnamed event)" : event.getName());
                return true;
            }
            return false;
        });
        return removedNames;
    }

    /** Validates statuses before cleanup can remove an event with an invalid archive value. */
    void validateEventArchiveStatuses() throws IllegalValueException {
        for (JsonAdaptedEvent event : events) {
            if (event != null) {
                event.validateArchived();
            }
        }
    }

    /**
     * Removes year-zero dismissal history from events that remain after cleanup.
     * Returns true if any history was removed.
     */
    boolean tryRemoveYearZeroReminderStarts() {
        boolean hasRemovedHistory = false;
        for (JsonAdaptedEvent event : events) {
            if (event != null && event.tryRemoveYearZeroReminderStarts()) {
                hasRemovedHistory = true;
            }
        }
        return hasRemovedHistory;
    }

    /**
     * Converts this CoordiMate into the model's {@code CoordiMate} object.
     *
     * @throws IllegalValueException if there were any data constraints violated.
     */
    public CoordiMate toModelType() throws IllegalValueException {
        List<Person> modelPersons = new ArrayList<>();
        long minimumNextId = 1;
        for (JsonAdaptedPerson jsonAdaptedPerson : persons) {
            if (jsonAdaptedPerson == null) {
                throw new IllegalValueException("Persons list must not contain null entries.");
            }
            Person person = jsonAdaptedPerson.toModelType();
            modelPersons.add(person);
            if (person.getMemberId().isPresent()) {
                minimumNextId = Math.max(minimumNextId, person.getMemberId().orElseThrow().getValue() + 1L);
            }
        }
        CoordiMate coordiMate = new CoordiMate(resolveNextMemberId(minimumNextId));
        List<Tag> modelTags = new ArrayList<>();
        for (JsonAdaptedTag jsonAdaptedTag : tags) {
            if (jsonAdaptedTag == null) {
                throw new IllegalValueException("Tags list must not contain null entries.");
            }
            modelTags.add(jsonAdaptedTag.toModelType());
        }
        if (hasTagData) {
            try {
                coordiMate.setTags(modelTags);
            } catch (DuplicateTagException duplicateTagException) {
                throw new IllegalValueException(MESSAGE_DUPLICATE_TAG);
            }
        }
        for (Person person : modelPersons) {
            if (coordiMate.hasPerson(person)) {
                throw new IllegalValueException(MESSAGE_DUPLICATE_PERSON);
            }
            try {
                coordiMate.addPerson(person);
            } catch (DuplicateMemberIdException e) {
                throw new IllegalValueException(MESSAGE_DUPLICATE_MEMBER_ID);
            } catch (IllegalStateException e) {
                throw new IllegalValueException(MESSAGE_MEMBER_IDS_EXHAUSTED);
            }
        }
        Set<Name> contactNames = coordiMate.getPersonList().stream().map(Person::getName).collect(Collectors.toSet());
        for (JsonAdaptedEvent jsonAdaptedEvent : events) {
            if (jsonAdaptedEvent == null) {
                throw new IllegalValueException("Events list must not contain null entries.");
            }
            Event event = jsonAdaptedEvent.toModelType();
            if (coordiMate.hasEvent(event)) {
                throw new IllegalValueException(MESSAGE_DUPLICATE_EVENT);
            }
            if (!contactNames.containsAll(event.getMembers())) {
                throw new IllegalValueException(MESSAGE_UNKNOWN_MEMBER);
            }
            coordiMate.addEvent(event);
        }
        return coordiMate;
    }

    /**
     * Removes legacy events whose names contain embedded command parameters, preserving file order.
     * The caller must validate the remaining data before saving or reporting these deletions.
     */
    List<String> removeEventsWithEmbeddedParameters() {
        List<String> removedNames = new ArrayList<>();
        events.removeIf(event -> {
            if (event != null && event.hasEmbeddedParameterInName()) {
                removedNames.add(event.getName());
                return true;
            }
            return false;
        });
        return removedNames;
    }

    /**
     * Preserves events that were distinct under the old whitespace rules by giving collisions unique suffixes.
     * Names already present anywhere in the file are reserved before choosing replacements.
     */
    List<EventNameCleanupResult.Rename> renameEventsWithCollidingNames() throws IllegalValueException {
        List<Event> modelEvents = new ArrayList<>();
        Set<String> legacyNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> reservedNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (JsonAdaptedEvent savedEvent : events) {
            if (savedEvent == null) {
                throw new IllegalValueException("Events list must not contain null entries.");
            }
            Event event = savedEvent.toModelType();
            String legacyName = LEGACY_WHITESPACE_RUN.matcher(savedEvent.getName().strip()).replaceAll(" ");
            if (!legacyNames.add(legacyName)) {
                throw new IllegalValueException(MESSAGE_DUPLICATE_EVENT);
            }
            modelEvents.add(event);
            reservedNames.add(event.getName());
        }

        List<EventNameCleanupResult.Rename> renamedEvents = new ArrayList<>();
        Set<String> seenNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (int i = 0; i < modelEvents.size(); i++) {
            Event event = modelEvents.get(i);
            if (seenNames.add(event.getName())) {
                continue;
            }
            String newName = reserveUniqueEventName(event.getName(), reservedNames);
            renamedEvents.add(new EventNameCleanupResult.Rename(events.get(i).getName(), newName));
            events.set(i, new JsonAdaptedEvent(event.withDetails(newName, event.getStartTime(), event.getEndTime())));
        }
        return renamedEvents;
    }

    /**
     * Reserves an unused numbered name, truncating its stem to fit the event name limit.
     */
    private static String reserveUniqueEventName(String name, Set<String> reservedNames) {
        for (int number = 2; ; number++) {
            String suffix = " (" + number + ")";
            int stemLength = Math.min(name.codePointCount(0, name.length()), Event.MAX_NAME_LENGTH - suffix.length());
            String candidate = name.substring(0, name.offsetByCodePoints(0, stemLength)).stripTrailing() + suffix;
            if (reservedNames.add(candidate)) {
                return candidate;
            }
        }
    }

    /**
     * Returns the saved allocation counter, rejecting counters that could reuse a member ID.
     */
    private long resolveNextMemberId(long minimumNextId) throws IllegalValueException {
        if (nextMemberId == null || nextMemberId.isNull()) {
            return minimumNextId;
        }
        if (!nextMemberId.isIntegralNumber() || !nextMemberId.canConvertToLong()
                || nextMemberId.longValue() < minimumNextId || nextMemberId.longValue() > Integer.MAX_VALUE + 1L) {
            throw new IllegalValueException(MESSAGE_INVALID_NEXT_MEMBER_ID);
        }
        return nextMemberId.longValue();
    }

}
