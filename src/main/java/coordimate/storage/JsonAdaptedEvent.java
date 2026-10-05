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

    @JsonCreator
    public JsonAdaptedEvent(@JsonProperty("name") String name,
            @JsonProperty("startTime") String startTime, @JsonProperty("endTime") String endTime) {
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public JsonAdaptedEvent(Event source) {
        this(source.getName(), source.getStartTime().toString(), source.getEndTime().toString());
    }

    /**
     * Applies the same constraints to stored events as to command input.
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
