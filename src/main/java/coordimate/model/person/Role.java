package coordimate.model.person;

import static coordimate.commons.util.AppUtil.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Represents a contact's responsibility or relationship to the CCA.
 */
public class Role {

    public static final String MESSAGE_CONSTRAINTS = "Invalid role. Example: r/Logistics Lead";
    private static final int MAX_LENGTH = 50;

    private final String value;

    /**
     * Constructs a {@code Role} from a valid value, trimming surrounding whitespace.
     */
    public Role(String role) {
        requireNonNull(role);
        checkArgument(isValidRole(role), MESSAGE_CONSTRAINTS);
        value = role.strip();
    }

    public String getValue() {
        return value;
    }

    /**
     * Returns true if the value is a non-blank role of at most 50 characters.
     */
    public static boolean isValidRole(String test) {
        requireNonNull(test);
        String trimmedRole = test.strip();
        int length = trimmedRole.codePointCount(0, trimmedRole.length());
        return length > 0 && length <= MAX_LENGTH
                && trimmedRole.codePoints().anyMatch(Character::isLetterOrDigit)
                && test.codePoints().allMatch(Role::isAllowedCharacter);
    }

    private static boolean isAllowedCharacter(int codePoint) {
        return Character.isLetterOrDigit(codePoint) || codePoint == ' ' || codePoint == '-'
                || codePoint == '/';
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
        return other instanceof Role otherRole && value.equals(otherRole.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
