package coordimate.model.person;

import static coordimate.commons.util.AppUtil.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Represents a Person's name in the CoordiMate.
 * Guarantees: immutable; is valid as declared in {@link #isValidName(String)}
 */
public class Name {

    public static final String MESSAGE_CONSTRAINTS = "Invalid name. Example: n/Aisha Tan";
    private static final int MAX_LENGTH = 80;

    private final String fullName;

    /**
     * Constructs a {@code Name}.
     *
     * @param name A valid name.
     */
    public Name(String name) {
        requireNonNull(name);
        checkArgument(isValidName(name), MESSAGE_CONSTRAINTS);
        fullName = name.strip();
    }

    public String getFullName() {
        return fullName;
    }

    /**
     * Returns true if a given string is a valid name.
     */
    public static boolean isValidName(String test) {
        requireNonNull(test);
        String trimmedName = test.strip();
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
