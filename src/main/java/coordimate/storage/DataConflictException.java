package coordimate.storage;

import java.io.IOException;

/**
 * The data file has changed since this application instance loaded or last saved it.
 */
public class DataConflictException extends IOException {
    public static final String MESSAGE =
            "The data file changed in another window. Reopen this window before saving. No changes were made.";

    public DataConflictException() {
        super(MESSAGE);
    }
}
