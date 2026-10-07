package coordimate.model.person;

import static coordimate.commons.util.AppUtil.checkArgument;
import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * Represents a contact's birthday in day-month-year format.
 */
public class Birthday {

    public static final String MESSAGE_CONSTRAINTS = "Invalid birthday. Use dd-MM-yyyy, e.g. b/18-06-2004";
    private static final String DATE_REGEX = "[0-9]{2}-[0-9]{2}-[0-9]{4}";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    private final String value;

    /**
     * Constructs a {@code Birthday} from a valid past or present date.
     */
    public Birthday(String birthday) {
        requireNonNull(birthday);
        checkArgument(isValidBirthday(birthday), MESSAGE_CONSTRAINTS);
        value = birthday;
    }

    public String getValue() {
        return value;
    }

    /**
     * Returns true if the date has the required format and is not in the future.
     */
    public static boolean isValidBirthday(String test) {
        requireNonNull(test);
        if (!test.matches(DATE_REGEX)) {
            return false;
        }

        try {
            LocalDate date = LocalDate.parse(test, DATE_FORMAT);
            return date.getYear() > 0 && !date.isAfter(LocalDate.now());
        } catch (DateTimeParseException e) {
            return false;
        }
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
        return other instanceof Birthday otherBirthday && value.equals(otherBirthday.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
