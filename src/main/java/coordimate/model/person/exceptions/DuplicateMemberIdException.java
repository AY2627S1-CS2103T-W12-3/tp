package coordimate.model.person.exceptions;

/**
 * Signals that distinct contacts cannot share a permanent member ID.
 */
public class DuplicateMemberIdException extends RuntimeException {
    public DuplicateMemberIdException() {
        super("A member with this ID already exists.");
    }
}
