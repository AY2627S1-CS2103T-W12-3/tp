package coordimate.model.person.exceptions;

/**
 * Signals that distinct contacts cannot share a permanent member ID.
 */
public class DuplicateMemberIdException extends RuntimeException {
    /**
     * Creates an exception for a permanent ID already used by another saved contact.
     */
    public DuplicateMemberIdException() {
        super("A member with this ID already exists.");
    }
}
