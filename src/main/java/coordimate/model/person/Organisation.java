package coordimate.model.person;

import static coordimate.commons.util.AppUtil.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Represents the organisation a contact belongs to.
 */
public class Organisation {

    public static final String MESSAGE_CONSTRAINTS = "Invalid organisation. Example: o/NUS Student Affairs";
    private static final int MAX_LENGTH = 80;

    private final String value;

    /**
     * Constructs an {@code Organisation} from a valid value, trimming surrounding whitespace.
     */
    public Organisation(String organisation) {
        requireNonNull(organisation);
        checkArgument(isValidOrganisation(organisation), MESSAGE_CONSTRAINTS);
        value = organisation.strip();
    }

    public String getValue() {
        return value;
    }

    /**
     * Returns true if the value is a non-blank organisation of at most 80 characters.
     */
    public static boolean isValidOrganisation(String test) {
        requireNonNull(test);
        String trimmedOrganisation = test.strip();
        int length = trimmedOrganisation.codePointCount(0, trimmedOrganisation.length());
        return length > 0 && length <= MAX_LENGTH
                && trimmedOrganisation.codePoints().anyMatch(Character::isLetterOrDigit)
                && test.codePoints().allMatch(Organisation::isAllowedCharacter);
    }

    private static boolean isAllowedCharacter(int codePoint) {
        return Character.isLetterOrDigit(codePoint) || codePoint == ' ' || codePoint == '-'
                || codePoint == '&';
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
        return other instanceof Organisation otherOrganisation && value.equals(otherOrganisation.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
