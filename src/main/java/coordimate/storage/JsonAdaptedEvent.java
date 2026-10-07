package coordimate.storage;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import coordimate.commons.exceptions.IllegalValueException;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.person.Name;

/**
 * Stores optional times as text so date-only events survive a round trip.
 * Stores members as contact names; files saved before members existed load with no members.
 */
class JsonAdaptedEvent {
    public static final String MESSAGE_NULL_MEMBER = "Event members must not contain null entries.";

    private final String name;
    private final String startTime;
    private final String endTime;
    private final List<String> members = new ArrayList<>();

    /**
     * Creates a JSON event record with the given name, time strings and member names.
     */
    @JsonCreator
    public JsonAdaptedEvent(@JsonProperty("name") String name,
            @JsonProperty("startTime") String startTime, @JsonProperty("endTime") String endTime,
            @JsonProperty("members") List<String> members) {
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        if (members != null) {
            this.members.addAll(members);
        }
    }

    /**
     * Creates a JSON event record with no members.
     */
    public JsonAdaptedEvent(String name, String startTime, String endTime) {
        this(name, startTime, endTime, null);
    }

    /**
     * Copies an event's name, time representations and member names into a JSON event record.
     */
    public JsonAdaptedEvent(Event source) {
        this(source.getName(), source.getStartTime().toString(), source.getEndTime().toString(),
                source.getMembers().stream().map(Name::toString).toList());
    }

    /**
     * Returns an event reconstructed from this record using the model's validation rules.
     *
     * @throws IllegalValueException If required fields are missing or violate event constraints.
     */
    public Event toModelType() throws IllegalValueException {
        if (name == null || startTime == null || endTime == null) {
            throw new IllegalValueException("Event name, start time and end time must be present.");
        }
        if (members.contains(null)) {
            throw new IllegalValueException(MESSAGE_NULL_MEMBER);
        }
        try {
            List<Name> memberNames = members.stream().map(Name::new).toList();
            return new Event(name, new EventTime(startTime), new EventTime(endTime), memberNames);
        } catch (IllegalArgumentException e) {
            throw new IllegalValueException(e.getMessage());
        }
    }
}
