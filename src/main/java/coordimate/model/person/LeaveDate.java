package coordimate.model.person;

import static coordimate.commons.util.AppUtil.checkArgument;
import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * Represents the last day of a contact's leave, in day-month-year format.
 * A contact is on leave up to and including this date. A date that has passed is kept, but no longer
 * marks the contact as on leave.
 */
public class LeaveDate {

    public static final String MESSAGE_CONSTRAINTS = "Invalid leave end date. Use a real date in dd-MM-yyyy "
            + "format that is not in the past. Example: lv/30-11-2026";
    private static final String DATE_REGEX = "[0-9]{2}-[0-9]{2}-[0-9]{4}";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    private final String value;
    private final LocalDate date;

    /**
     * Constructs a {@code LeaveDate} from a real date in dd-MM-yyyy format, which may be in the past.
     */
    public LeaveDate(String leaveDate) {
        requireNonNull(leaveDate);
        checkArgument(isValidLeaveDate(leaveDate), MESSAGE_CONSTRAINTS);
        value = leaveDate;
        date = LocalDate.parse(leaveDate, DATE_FORMAT);
    }

    public String getValue() {
        return value;
    }

    public LocalDate getDate() {
        return date;
    }

    /**
     * Returns true if {@code test} is a real date in dd-MM-yyyy format.
     * Past dates are valid so that saved leave that has ended can still be loaded.
     */
    public static boolean isValidLeaveDate(String test) {
        requireNonNull(test);
        if (!test.matches(DATE_REGEX)) {
            return false;
        }

        try {
            return LocalDate.parse(test, DATE_FORMAT).getYear() > 0;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Returns true if {@code test} is a valid leave end date that is not before {@code today}.
     * New leave entered through a command must meet this rule.
     */
    public static boolean isValidInputLeaveDate(String test, LocalDate today) {
        requireNonNull(test);
        requireNonNull(today);
        return isValidLeaveDate(test) && !LocalDate.parse(test, DATE_FORMAT).isBefore(today);
    }

    /**
     * Returns true if the leave covers {@code day}, that is, {@code day} is not after the last day of leave.
     */
    public boolean covers(LocalDate day) {
        requireNonNull(day);
        return !day.isAfter(date);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        return other instanceof LeaveDate otherLeaveDate && value.equals(otherLeaveDate.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
