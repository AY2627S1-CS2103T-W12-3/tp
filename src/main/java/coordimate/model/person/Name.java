package coordimate.model.person;

import static coordimate.commons.util.AppUtil.checkArgument;
import static java.util.Objects.requireNonNull;

import coordimate.commons.util.StringUtil;

/**
 * Represents a Person's name in the CoordiMate.
 * Surrounding spaces are removed and each run of internal spaces is collapsed to one, so names that differ
 * only in spacing are stored identically.
 * Guarantees: immutable; is valid as declared in {@link #isValidName(String)}
 */
public class Name {

    public static final int MAX_LENGTH = 80;
    public static final String MESSAGE_CONSTRAINTS = "Invalid name. Use 1 to " + MAX_LENGTH
            + " characters: letters, spaces, hyphens, apostrophes, and periods, with at least one letter. "
            + "Example: n/Aisha Tan";

    private final String fullName;

    /**
     * Constructs a {@code Name}.
     *
     * @param name A valid name.
     */
    public Name(String name) {
        requireNonNull(name);
        checkArgument(isValidName(name), MESSAGE_CONSTRAINTS);
        fullName = StringUtil.normalizeWhitespace(name);
    }

    public String getFullName() {
        return fullName;
    }

    /**
     * Returns true if a given string is a valid name. The length limit applies after spacing is normalised.
     */
    public static boolean isValidName(String test) {
        requireNonNull(test);
        String trimmedName = StringUtil.normalizeWhitespace(test);
        int length = trimmedName.codePointCount(0, trimmedName.length());
        return length > 0 && length <= MAX_LENGTH
                && trimmedName.codePoints().anyMatch(Character::isLetter)
                && test.codePoints().allMatch(Name::isAllowedCharacter);
    }

    private static boolean isAllowedCharacter(int codePoint) {
        return Character.isLetter(codePoint) || codePoint == ' ' || codePoint == '-'
                || codePoint == '\'' || codePoint == '.';
    }


    @Override
    public String toString() {
        return fullName;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Name otherName)) {
            return false;
        }

        return fullName.equals(otherName.fullName);
    }

    @Override
    public int hashCode() {
        return fullName.hashCode();
    }

}
