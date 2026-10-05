package coordimate.logic.parser.exceptions;

import coordimate.commons.exceptions.IllegalValueException;

/**
 * Represents a parse error encountered by a parser.
 */
public class ParseException extends IllegalValueException {

    /**
     * Creates a parse error with the given message.
     */
    public ParseException(String message) {
        super(message);
    }

    /**
     * Creates a parse error with the given message and underlying cause.
     */
    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
