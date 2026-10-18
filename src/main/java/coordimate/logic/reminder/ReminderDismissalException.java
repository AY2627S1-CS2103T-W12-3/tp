package coordimate.logic.reminder;

/**
 * A reminder was dismissed for this session, but its dismissal could not be saved.
 */
public class ReminderDismissalException extends Exception {
    public static final String MESSAGE =
            "Reminder dismissal could not be saved and may reappear after restarting.";

    /**
     * Keeps the underlying storage or data-loading error for logging.
     */
    public ReminderDismissalException(Exception cause) {
        super(MESSAGE, cause);
    }
}
