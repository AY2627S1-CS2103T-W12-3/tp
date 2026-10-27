package coordimate.model.tag;

import static coordimate.commons.util.AppUtil.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.Collection;

/**
 * Represents a Tag in the CoordiMate.
 * Guarantees: immutable; name is valid as declared in {@link #isValidTagName(String)}
 */
public class Tag {

    public static final String MESSAGE_CONSTRAINTS =
            "Invalid tag name. Use letters and numbers without spaces. Example: t/Publicity";
    public static final String MESSAGE_LENGTH_CONSTRAINTS =
            "Tag name must not exceed 30 characters. Example: t/Publicity";
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
        checkArgument(isValidTagName(tagName), MESSAGE_CONSTRAINTS);
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
     * Returns the tag in {@code savedTags} with the same name as this tag, ignoring case,
     * or this tag if no saved tag matches.
     */
    public Tag findSavedTag(Collection<Tag> savedTags) {
        requireNonNull(savedTags);
        return savedTags.stream().filter(this::isSameTag).findFirst().orElse(this);
    }

    /**
     * Returns true if a given string is a valid tag name in the model, including names accepted by older versions.
     */
    public static boolean isValidTagName(String test) {
        requireNonNull(test);
        return test.matches(VALIDATION_REGEX);
    }

    /**
     * Returns true if a tag name entered through a command meets the current length limit.
     */
    public static boolean isValidInputTagName(String test) {
        requireNonNull(test);
        return isValidTagName(test) && test.length() <= MAX_LENGTH;
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
