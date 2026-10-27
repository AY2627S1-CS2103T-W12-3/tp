package coordimate.commons.exceptions;

import java.io.IOException;

/** Indicates that a saved JSON field has a different type from the one CoordiMate expects. */
public class JsonTypeMismatchException extends IOException {
    public JsonTypeMismatchException(String path, String expectedType) {
        super("Invalid JSON type at " + path + ": expected " + expectedType + ".");
    }
}
