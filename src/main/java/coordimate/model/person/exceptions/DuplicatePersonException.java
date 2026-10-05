package coordimate.model.person.exceptions;

/**
 * Signals that the operation will result in duplicate Persons (Persons are considered duplicates if they have the same
 * identity).
 */
public class DuplicatePersonException extends RuntimeException {
    /**
     * Creates an error indicating that a person with the same identity already exists.
     */
    public DuplicatePersonException() {
        super("Operation would result in duplicate persons");
    }
}
