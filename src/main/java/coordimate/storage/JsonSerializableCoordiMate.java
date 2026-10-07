package coordimate.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

import coordimate.commons.exceptions.IllegalValueException;
import coordimate.model.CoordiMate;
import coordimate.model.ReadOnlyCoordiMate;
import coordimate.model.event.Event;
import coordimate.model.person.Name;
import coordimate.model.person.Person;
import coordimate.model.tag.Tag;
import coordimate.model.tag.exceptions.DuplicateTagException;

/**
 * An Immutable CoordiMate that is serializable to JSON format.
 */
@JsonRootName(value = "coordimate")
class JsonSerializableCoordiMate {

    public static final String MESSAGE_DUPLICATE_PERSON = "Persons list contains duplicate person(s).";
    public static final String MESSAGE_DUPLICATE_EVENT = "Events list contains duplicate event name(s).";
    public static final String MESSAGE_DUPLICATE_TAG = "Tags list contains duplicate tag name(s).";
    public static final String MESSAGE_UNKNOWN_MEMBER = "Event members must refer to existing contacts.";

    private final List<JsonAdaptedPerson> persons = new ArrayList<>();
    private final List<JsonAdaptedEvent> events = new ArrayList<>();
    private final List<JsonAdaptedTag> tags = new ArrayList<>();

    /**
     * Constructs a {@code JsonSerializableCoordiMate} with the given people and optional events.
     */
    @JsonCreator
    public JsonSerializableCoordiMate(@JsonProperty("persons") List<JsonAdaptedPerson> persons,
            @JsonProperty("events") List<JsonAdaptedEvent> events,
            @JsonProperty("tags") List<JsonAdaptedTag> tags) {
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
        persons.addAll(source.getPersonList().stream().map(JsonAdaptedPerson::new).collect(Collectors.toList()));
        events.addAll(source.getEventList().stream().map(JsonAdaptedEvent::new).collect(Collectors.toList()));
        tags.addAll(source.getTagList().stream().map(JsonAdaptedTag::new).collect(Collectors.toList()));
    }

    /**
     * Converts this CoordiMate into the model's {@code CoordiMate} object.
     *
     * @throws IllegalValueException if there were any data constraints violated.
     */
    public CoordiMate toModelType() throws IllegalValueException {
        CoordiMate coordiMate = new CoordiMate();
        for (JsonAdaptedPerson jsonAdaptedPerson : persons) {
            if (jsonAdaptedPerson == null) {
                throw new IllegalValueException("Persons list must not contain null entries.");
            }
            Person person = jsonAdaptedPerson.toModelType();
            if (coordiMate.hasPerson(person)) {
                throw new IllegalValueException(MESSAGE_DUPLICATE_PERSON);
            }
            coordiMate.addPerson(person);
        }
        List<Tag> modelTags = new ArrayList<>();
        for (JsonAdaptedTag jsonAdaptedTag : tags) {
            if (jsonAdaptedTag == null) {
                throw new IllegalValueException("Tags list must not contain null entries.");
            }
            modelTags.add(jsonAdaptedTag.toModelType());
        }
        try {
            coordiMate.setTags(modelTags);
        } catch (DuplicateTagException duplicateTagException) {
            throw new IllegalValueException(MESSAGE_DUPLICATE_TAG);
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

}
