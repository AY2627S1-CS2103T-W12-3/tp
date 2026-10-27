package coordimate.model.person;

import static coordimate.commons.util.AppUtil.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Represents a Person's phone number in the CoordiMate.
 * Guarantees: immutable; is valid as declared in {@link #isValidPhone(String)}
 */
public class Phone {

    public static final int MIN_DIGITS = 7;
    public static final int MAX_DIGITS = 15;
    public static final String MESSAGE_CONSTRAINTS = "Invalid phone number. Use " + MIN_DIGITS + " to " + MAX_DIGITS
            + " digits, optionally starting with + or (+country code), with optional spaces, hyphens, and "
            + "matching brackets around digits. Example: p/+6591234567";

    /** Digits, spaces and hyphens inside a pair of brackets, with at least one digit. */
    private static final String BRACKETED_DIGITS = "\\([0-9 -]*[0-9][0-9 -]*\\)";

    /**
     * An optional {@code +} or bracketed country code such as {@code (+65)} at the start, followed by digits,
     * spaces, hyphens and pairs of brackets that each enclose at least one digit and are not nested.
     */
    private static final String VALIDATION_REGEX = "(?:\\+|\\(\\+[0-9 -]*[0-9][0-9 -]*\\))?"
            + "(?:[0-9 -]|" + BRACKETED_DIGITS + ")+";

    /** The format accepted before brackets had to match, so that saved numbers from older versions still load. */
    private static final String PREVIOUS_VALIDATION_REGEX = "\\+?[0-9() -]+";
    private static final String LEGACY_VALIDATION_REGEX = " *[0-9]{3,} *";
    private final String value;

    /**
     * Constructs a {@code Phone}.
     *
     * @param phone A valid phone number.
     */
    public Phone(String phone) {
        requireNonNull(phone);
        checkArgument(isValidPhone(phone), MESSAGE_CONSTRAINTS);
        value = phone.strip();
    }

    public String getValue() {
        return value;
    }

    /**
     * Returns the phone number without formatting separators for duplicate checks.
     */
    public String getNormalizedValue() {
        return value.replace(" ", "").replace("-", "").replace("(", "").replace(")", "");
    }

    /**
     * Returns true for a current phone number, or for a number that an older version accepted: a digit-only number
     * of at least 3 digits, or 7 to 15 digits with brackets that need not match.
     */
    public static boolean isValidPhone(String test) {
        requireNonNull(test);
        return test.matches(LEGACY_VALIDATION_REGEX) || isValidInputPhone(test)
                || matchesWithDigitCount(test, PREVIOUS_VALIDATION_REGEX);
    }

    /** Returns true if a phone number entered through a command meets the current 7–15-digit rule. */
    public static boolean isValidInputPhone(String test) {
        requireNonNull(test);
        return matchesWithDigitCount(test, VALIDATION_REGEX);
    }

    private static boolean matchesWithDigitCount(String test, String regex) {
        String trimmedPhone = test.strip();
        long digitCount = trimmedPhone.chars().filter(character -> character >= '0' && character <= '9').count();
        return test.codePoints().allMatch(Phone::isAllowedCharacter)
                && trimmedPhone.matches(regex) && digitCount >= MIN_DIGITS && digitCount <= MAX_DIGITS;
    }

    private static boolean isAllowedCharacter(int codePoint) {
        return (codePoint >= '0' && codePoint <= '9') || codePoint == '+' || codePoint == ' '
                || codePoint == '-' || codePoint == '(' || codePoint == ')';
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

        // instanceof handles nulls
        if (!(other instanceof Phone otherPhone)) {
            return false;
        }

        return value.equals(otherPhone.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

}
