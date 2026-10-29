package coordimate.model.event;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Optional;

/**
 * Retains whether an event date has a specified time of day.
 */
public final class EventTime {
    public static final String MESSAGE_EMPTY = "Event time must not be empty. Example: st/17-09-2026 16:30";
    public static final String MESSAGE_CONSTRAINTS =
            "Invalid event time. Use dd-MM-yyyy with a year from 0001 to 9999 "
            + "and an optional time in HH:mm format. "
            + "Example: st/17-09-2026 16:30";

    // Date-only and timed values share the fixed-width dd-MM-uuuu prefix.
    private static final int DATE_TEXT_LENGTH = 10;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd-MM-uuuu HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);

    private final String value;

    /**
     * Creates an event time from a valid trimmed date with an optional time of day.
     * Preserves date-only values without assigning them a time.
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
     * Returns true if the value is a valid date with an optional time in the required format.
     */
    public static boolean isValidTime(String value) {
        requireNonNull(value);
        try {
            if (value.matches("[0-9]{2}-[0-9]{2}-[0-9]{4}")) {
                return LocalDate.parse(value, DATE_FORMAT).getYear() > 0;
            } else if (value.matches("[0-9]{2}-[0-9]{2}-[0-9]{4} [0-9]{2}:[0-9]{2}")) {
                return LocalDateTime.parse(value, TIME_FORMAT).getYear() > 0;
            } else {
                return false;
            }
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Returns the date, whether or not a time of day is specified.
     */
    public LocalDate getDate() {
        return LocalDate.parse(value.substring(0, DATE_TEXT_LENGTH), DATE_FORMAT);
    }

    /**
     * Returns the specified date and time, or empty for a date-only value.
     */
    public Optional<LocalDateTime> getDateTime() {
        return value.length() > DATE_TEXT_LENGTH
                ? Optional.of(LocalDateTime.parse(value, TIME_FORMAT)) : Optional.empty();
    }

    /**
     * Returns true if this date is later, or if the dates match and this specified time is later.
     * Compares times on matching dates only when both values specify a time of day.
     */
    public boolean isAfter(EventTime other) {
        requireNonNull(other);
        LocalDate date = getDate();
        LocalDate otherDate = other.getDate();
        if (!date.equals(otherDate)) {
            return date.isAfter(otherDate);
        }
        return value.length() > DATE_TEXT_LENGTH && other.value.length() > DATE_TEXT_LENGTH
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
