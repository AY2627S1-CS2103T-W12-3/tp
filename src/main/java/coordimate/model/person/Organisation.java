package coordimate.model.person;

import static coordimate.commons.util.AppUtil.checkArgument;
import static java.util.Objects.requireNonNull;

import coordimate.commons.util.StringUtil;

/**
 * Represents the organization a contact belongs to.
 * Surrounding spaces are removed and each run of internal spaces is collapsed to one.
 */
public class Organisation {

    public static final int MAX_LENGTH = 80;
    public static final String MESSAGE_CONSTRAINTS = "Invalid organisation. Use 1 to " + MAX_LENGTH
            + " characters: letters, digits, spaces, and - & . , ' ( ) /, with at least one letter or digit. "
            + "Example: o/NUS Student Affairs";

    private final String value;

    /**
     * Constructs an {@code Organisation} from a valid value, trimming surrounding whitespace and collapsing
     * internal runs of spaces.
     */
    public Organisation(String organisation) {
        requireNonNull(organisation);
        checkArgument(isValidOrganisation(organisation), MESSAGE_CONSTRAINTS);
        value = StringUtil.normalizeWhitespace(organisation);
    }

    public String getValue() {
        return value;
    }

    /**
     * Returns true if the value is a non-blank organization of at most 80 characters.
     * The length limit applies after spacing is normalised.
     */
    public static boolean isValidOrganisation(String test) {
        requireNonNull(test);
        String trimmedOrganisation = StringUtil.normalizeWhitespace(test);
        int length = trimmedOrganisation.codePointCount(0, trimmedOrganisation.length());
        return length > 0 && length <= MAX_LENGTH
                && trimmedOrganisation.codePoints().anyMatch(Character::isLetterOrDigit)
                && test.codePoints().allMatch(Organisation::isAllowedCharacter);
    }

    private static boolean isAllowedCharacter(int codePoint) {
        return Character.isLetterOrDigit(codePoint) || " -&.,'()/".indexOf(codePoint) >= 0;
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
