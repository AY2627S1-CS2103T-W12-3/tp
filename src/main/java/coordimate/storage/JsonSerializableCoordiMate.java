package coordimate.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
