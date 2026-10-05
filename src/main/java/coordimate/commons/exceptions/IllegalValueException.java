package coordimate.commons.exceptions;

/**
 * Signals that some given data does not fulfill some constraints.
 */
public class IllegalValueException extends Exception {
    /**
     * Creates a constraint violation with the given message.
     *
     * @param message should contain relevant information on the failed constraint(s).
     */
    public IllegalValueException(String message) {
        super(message);
    }

    /**
     * Creates a constraint violation with the given message and cause.
     *
     * @param message should contain relevant information on the failed constraint(s).
     * @param cause of the main exception.
     */
    public IllegalValueException(String message, Throwable cause) {
        super(message, cause);
    }
}
