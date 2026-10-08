package coordimate.model.person;

import static coordimate.commons.util.AppUtil.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Represents a printable note about a contact.
 */
public class Note {

    public static final String MESSAGE_CONSTRAINTS = "Invalid note. Example: m/Handles venue bookings";
    private static final int MAX_LENGTH = 500;

    private final String value;

    /**
     * Constructs a {@code Note} from non-blank printable text.
     */
    public Note(String note) {
        requireNonNull(note);
        checkArgument(isValidNote(note), MESSAGE_CONSTRAINTS);
        value = note;
    }

    public String getValue() {
        return value;
    }

    /**
     * Returns true if the note has at most 500 printable Unicode code points.
     */
    public static boolean isValidNote(String test) {
        return PrintableText.isValid(test, MAX_LENGTH);
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
        return other instanceof Note otherNote && value.equals(otherNote.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
