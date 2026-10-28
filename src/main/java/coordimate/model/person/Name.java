package coordimate.model.person;

import static coordimate.commons.util.AppUtil.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.regex.Pattern;

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
            + " characters: letters, spaces, hyphens, apostrophes, periods, and s/o or d/o, "
            + "with at least one letter. Example: n/Aisha Tan";

    /**
     * Matches {@code s/o} ("son of") or {@code d/o} ("daughter of") as a separate word, ignoring case.
     * These appear in Singapore names, so they are allowed in names and are not read as command parameters.
     */
    public static final String RELATIONSHIP_WORD_REGEX = "(?i:(?<!\\S)[sd]/o(?!\\S))";

    private static final Pattern RELATIONSHIP_WORD = Pattern.compile(RELATIONSHIP_WORD_REGEX);

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
     * A {@code /} is allowed only within the words {@code s/o} and {@code d/o}, and the name needs a letter
     * outside those words.
     */
    public static boolean isValidName(String test) {
        requireNonNull(test);
        String trimmedName = StringUtil.normalizeWhitespace(test);
        int length = trimmedName.codePointCount(0, trimmedName.length());
        String withoutRelationshipWords = RELATIONSHIP_WORD.matcher(test).replaceAll("");
        return length > 0 && length <= MAX_LENGTH
                && withoutRelationshipWords.codePoints().anyMatch(Character::isLetter)
                && withoutRelationshipWords.codePoints().allMatch(Name::isAllowedCharacter);
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
