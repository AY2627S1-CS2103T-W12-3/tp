package coordimate.model.event;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * Retains whether an event date has a specified time of day.
 */
public final class EventTime {
    public static final String MESSAGE_EMPTY = "Start time and/or end time must not be empty!";
    public static final String MESSAGE_CONSTRAINTS = "Start time and/or end time are formatted as dd-MM-yyyy [HH:mm]. "
            + "Time of day is optional. Example: 17-09-2026 16:30";

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd-MM-uuuu HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);

    private final String value;

    /**
     * Validates trimmed input without assigning a time to date-only values.
     */
    public EventTime(String input) {
        requireNonNull(input);
        value = input.strip();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(MESSAGE_EMPTY);
        }
        if (!isValidTime(value)) {
            throw new IllegalArgumentException(MESSAGE_CONSTRAINTS);
        }
    }

    /**
     * Rejects impossible dates and times as well as incorrect formatting.
     */
    public static boolean isValidTime(String value) {
        requireNonNull(value);
        try {
            if (value.matches("[0-9]{2}-[0-9]{2}-[0-9]{4}")) {
                LocalDate.parse(value, DATE_FORMAT);
            } else if (value.matches("[0-9]{2}-[0-9]{2}-[0-9]{4} [0-9]{2}:[0-9]{2}")) {
                LocalDateTime.parse(value, TIME_FORMAT);
            } else {
                return false;
            }
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Compares dates first; on the same date, compares times only when both are specified.
     */
    public boolean isAfter(EventTime other) {
        requireNonNull(other);
        LocalDate date = LocalDate.parse(value.substring(0, 10), DATE_FORMAT);
        LocalDate otherDate = LocalDate.parse(other.value.substring(0, 10), DATE_FORMAT);
        if (!date.equals(otherDate)) {
            return date.isAfter(otherDate);
        }
        return value.length() > 10 && other.value.length() > 10
                && LocalDateTime.parse(value, TIME_FORMAT).isAfter(LocalDateTime.parse(other.value, TIME_FORMAT));
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof EventTime otherTime && value.equals(otherTime.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
