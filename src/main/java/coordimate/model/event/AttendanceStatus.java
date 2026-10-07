package coordimate.model.event;

/**
 * Whether a member was present or absent at an event.
 */
public enum AttendanceStatus {
    PRESENT, ABSENT;

    /**
     * Returns the status matching {@code value} ("present" or "absent"), ignoring case.
     */
    public static AttendanceStatus fromString(String value) {
        return switch (value.toLowerCase()) {
            case "present" -> PRESENT;
            case "absent" -> ABSENT;
            default -> throw new IllegalArgumentException("STATUS must be either \"present\" or \"absent\"!");
        };
    }

    @Override
    public String toString() {
        return this == PRESENT ? "present" : "absent";
    }
}
