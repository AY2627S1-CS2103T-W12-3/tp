package coordimate.storage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import coordimate.commons.exceptions.IllegalValueException;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;

/**
 * Stores optional times as text so date-only events survive a round trip.
 */
class JsonAdaptedEvent {
    private final String name;
    private final String startTime;
    private final String endTime;

    /**
     * Creates a JSON event record with the given name and time strings.
     */
    @JsonCreator
    public JsonAdaptedEvent(@JsonProperty("name") String name,
            @JsonProperty("startTime") String startTime, @JsonProperty("endTime") String endTime) {
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Copies an event's name and time representations into a JSON event record.
     */
    public JsonAdaptedEvent(Event source) {
        this(source.getName(), source.getStartTime().toString(), source.getEndTime().toString());
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
        try {
            return new Event(name, new EventTime(startTime), new EventTime(endTime));
        } catch (IllegalArgumentException e) {
            throw new IllegalValueException(e.getMessage());
        }
    }
}
