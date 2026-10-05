package coordimate.model.event;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

/**
 * An event's identity is its trimmed, case-sensitive name.
 */
public final class Event {
    public static final String MESSAGE_EMPTY_NAME = "Event name must not be empty.";
    public static final String MESSAGE_INVALID_TIME_ORDER = "Start time must not be after end time.";

    private final String name;
    private final EventTime startTime;
    private final EventTime endTime;

    /**
     * Trims the name without changing internal spacing or capitalization.
     */
    public Event(String name, EventTime startTime, EventTime endTime) {
        this.name = requireNonNull(name).strip();
        if (this.name.isEmpty()) {
            throw new IllegalArgumentException(MESSAGE_EMPTY_NAME);
        }
        this.startTime = requireNonNull(startTime);
        this.endTime = requireNonNull(endTime);
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException(MESSAGE_INVALID_TIME_ORDER);
        }
    }

    public String getName() {
        return name;
    }

    public EventTime getStartTime() {
        return startTime;
    }

    public EventTime getEndTime() {
        return endTime;
    }

    /**
     * Timings do not affect event identity.
     */
    public boolean isSameEvent(Event other) {
        return other != null && name.equals(other.name);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Event otherEvent && name.equals(otherEvent.name)
                && startTime.equals(otherEvent.startTime) && endTime.equals(otherEvent.endTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, startTime, endTime);
    }
}
