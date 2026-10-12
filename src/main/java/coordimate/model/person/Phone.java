package coordimate.model.person;

import static coordimate.commons.util.AppUtil.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Represents a Person's phone number in the CoordiMate.
 * Guarantees: immutable; is valid as declared in {@link #isValidPhone(String)}
 */
public class Phone {


    public static final String MESSAGE_CONSTRAINTS = "Invalid phone number. Example: p/+6591234567";
    private static final String VALIDATION_REGEX = "\\+?[0-9() -]+";
    private static final String LEGACY_VALIDATION_REGEX = "[0-9]{3,}";
    private static final int MIN_DIGITS = 7;
    private static final int MAX_DIGITS = 15;
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

    /** Returns true for a current phone number or a previously valid digit-only phone number. */
    public static boolean isValidPhone(String test) {
        requireNonNull(test);
        return test.matches(LEGACY_VALIDATION_REGEX) || isValidInputPhone(test);
    }

    /** Returns true if a phone number entered through a command meets the current 7–15-digit rule. */
    public static boolean isValidInputPhone(String test) {
        requireNonNull(test);
        String trimmedPhone = test.strip();
        long digitCount = trimmedPhone.chars().filter(character -> character >= '0' && character <= '9').count();
        return test.codePoints().allMatch(Phone::isAllowedCharacter)
                && trimmedPhone.matches(VALIDATION_REGEX) && digitCount >= MIN_DIGITS && digitCount <= MAX_DIGITS;
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
