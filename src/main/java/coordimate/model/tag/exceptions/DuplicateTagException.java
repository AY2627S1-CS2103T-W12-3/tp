package coordimate.model.tag.exceptions;

/**
 * Signals that an operation would result in duplicate tags.
 */
public class DuplicateTagException extends RuntimeException {

    /**
     * Creates an error indicating that a tag with the same name already exists.
     */
    public DuplicateTagException() {
        super("Operation would result in duplicate tags");
    }
}
