package coordimate.model.event;

/**
 * Whether a member was present or absent at an event.
 */
public enum AttendanceStatus {
    PRESENT, ABSENT;

    public static final String MESSAGE_CONSTRAINTS =
            "Invalid attendance status. Use present or absent. Example: att/present";

    /**
     * Returns the status matching {@code value} ("present" or "absent"), ignoring case.
     */
    public static AttendanceStatus fromString(String value) {
        return switch (value.toLowerCase()) {
            case "present" -> PRESENT;
            case "absent" -> ABSENT;
            default -> throw new IllegalArgumentException(MESSAGE_CONSTRAINTS);
        };
    }

    @Override
    public String toString() {
        return this == PRESENT ? "present" : "absent";
    }
}
