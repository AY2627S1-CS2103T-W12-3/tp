package coordimate.model.tag;

import static coordimate.commons.util.AppUtil.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Represents a Tag in the CoordiMate.
 * Guarantees: immutable; name is valid as declared in {@link #isValidTagName(String)}
 */
public class Tag {

    public static final String MESSAGE_CONSTRAINTS = "Tag names should be alphanumeric with no spaces.";
    public static final String MESSAGE_LENGTH_CONSTRAINTS = "Tag names should not exceed 30 characters.";
    public static final String VALIDATION_REGEX = "\\p{Alnum}+";
    public static final int MAX_LENGTH = 30;

    private final String tagName;

    /**
     * Constructs a {@code Tag}.
     *
     * @param tagName A valid tag name.
     */
    public Tag(String tagName) {
        requireNonNull(tagName);
        checkArgument(tagName.matches(VALIDATION_REGEX), MESSAGE_CONSTRAINTS);
        checkArgument(tagName.length() <= MAX_LENGTH, MESSAGE_LENGTH_CONSTRAINTS);
        this.tagName = tagName;
    }

    public String getTagName() {
        return tagName;
    }

    /**
     * Returns true if both tags have the same name, ignoring case.
     */
    public boolean isSameTag(Tag otherTag) {
        if (otherTag == this) {
            return true;
        }

        return otherTag != null && tagName.equalsIgnoreCase(otherTag.tagName);
    }

    /**
     * Returns true if a given string is a valid tag name.
     */
    public static boolean isValidTagName(String test) {
        return test.matches(VALIDATION_REGEX) && test.length() <= MAX_LENGTH;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Tag otherTag)) {
            return false;
        }

        return tagName.equals(otherTag.tagName);
    }

    @Override
    public int hashCode() {
        return tagName.hashCode();
    }

    /**
     * Formats state as text for viewing.
     */
    @Override
    public String toString() {
        return '[' + tagName + ']';
    }

}
