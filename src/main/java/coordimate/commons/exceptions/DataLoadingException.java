package coordimate.commons.exceptions;

/**
 * Represents an error during loading of data from a file.
 */
public class DataLoadingException extends Exception {
    /**
     * Creates a data-loading error with the underlying cause.
     */
    public DataLoadingException(Exception cause) {
        super(cause);
    }

}
