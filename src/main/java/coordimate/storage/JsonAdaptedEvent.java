package coordimate.storage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import coordimate.commons.exceptions.IllegalValueException;
import coordimate.model.event.AttendanceStatus;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.person.Name;

/**
 * Stores optional times as text so date-only events survive a round trip.
 * Stores members as contact names; files saved before members existed load with no members.
 * Stores attendance as member-name to status-text pairs; files saved before attendance existed
 * load with no attendance recorded.
 * Missing dismissal history loads as an empty history for older files.
 * A missing archive status loads as not archived for older files.
 */
class JsonAdaptedEvent {
    public static final String MESSAGE_NULL_MEMBER = "Event members must not contain null entries.";
    public static final String MESSAGE_NULL_ATTENDANCE =
            "Event attendance must not contain null names or statuses.";
    public static final String MESSAGE_INVALID_ATTENDANCE_STATUS =
            "Event attendance statuses must be either \"present\" or \"absent\".";
    public static final String MESSAGE_NULL_DISMISSED_REMINDER_START =
            "Dismissed reminder starts must not contain null entries.";

    private final String name;
    private final String startTime;
    private final String endTime;
    private final List<String> members = new ArrayList<>();
    private final Map<String, String> attendance = new LinkedHashMap<>();
    private final List<String> dismissedReminderStarts = new ArrayList<>();
    private final boolean archived;

    /**
     * Creates a JSON event record with the given details, attendance, dismissed reminder starts and
     * archive status.
     */
    @JsonCreator
    public JsonAdaptedEvent(@JsonProperty("name") String name,
                            @JsonProperty("startTime") String startTime, @JsonProperty("endTime") String endTime,
                            @JsonProperty("members") List<String> members,
                            @JsonProperty("attendance") Map<String, String> attendance,
                            @JsonProperty("dismissedReminderStarts") List<String> dismissedReminderStarts,
                            @JsonProperty("archived") Boolean archived) {
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        if (members != null) {
            this.members.addAll(members);
        }
        if (attendance != null) {
            this.attendance.putAll(attendance);
        }
        if (dismissedReminderStarts != null) {
            this.dismissedReminderStarts.addAll(dismissedReminderStarts);
        }
        this.archived = Boolean.TRUE.equals(archived);
    }

    /**
     * Creates a JSON event record with attendance and dismissed reminder starts, which is not archived.
     */
    public JsonAdaptedEvent(String name, String startTime, String endTime, List<String> members,
                            Map<String, String> attendance, List<String> dismissedReminderStarts) {
        this(name, startTime, endTime, members, attendance, dismissedReminderStarts, null);
    }

    /**
     * Creates a JSON event record with attendance and no dismissed reminder starts.
     */
    public JsonAdaptedEvent(String name, String startTime, String endTime, List<String> members,
                            Map<String, String> attendance) {
        this(name, startTime, endTime, members, attendance, null);
    }

    /**
     * Creates a JSON event record with the given name, time strings and member names, and no attendance.
     */
    public JsonAdaptedEvent(String name, String startTime, String endTime, List<String> members) {
        this(name, startTime, endTime, members, null);
    }

    /**
     * Creates a JSON event record with no members and no attendance.
     */
    public JsonAdaptedEvent(String name, String startTime, String endTime) {
        this(name, startTime, endTime, null, null);
    }

    /**
     * Copies an event's details, attendance, dismissal history and archive status into a JSON event record.
     */
    public JsonAdaptedEvent(Event source) {
        this(source.getName(), source.getStartTime().toString(), source.getEndTime().toString(),
                source.getMembers().stream().map(Name::toString).toList(),
                toAttendanceStrings(source),
                source.getDismissedReminderStarts().stream().map(EventTime::toString).sorted().toList(),
                source.isArchived());
    }

    private static Map<String, String> toAttendanceStrings(Event source) {
        Map<String, String> result = new LinkedHashMap<>();
        source.getAttendanceRecord().forEach((memberName, status) -> result.put(memberName.toString(),
                status.toString()));
        return result;
    }

    String getName() {
        return name;
    }

    /**
     * Identifies legacy event start or end dates that use year zero.
     */
    boolean hasYearZeroEventDate() {
        return isYearZero(startTime) || isYearZero(endTime);
    }

    /**
     * Removes year-zero reminder history from an event whose dates are otherwise valid.
     */
    boolean removeYearZeroReminderStarts() {
        return dismissedReminderStarts.removeIf(JsonAdaptedEvent::isYearZero);
    }

    private static boolean isYearZero(String value) {
        return value != null && value.strip().matches("[0-9]{2}-[0-9]{2}-0000( [0-9]{2}:[0-9]{2})?");
    }

    /**
     * Returns an event reconstructed from this record using the model's validation rules.
     *
     * @throws IllegalValueException If required fields are missing or violate event constraints.
     */
    public Event toModelType() throws IllegalValueException {
        if (name == null || startTime == null || endTime == null) {
            throw new IllegalValueException("Event name, start time and end time must be present.");
        }
        if (members.contains(null)) {
            throw new IllegalValueException(MESSAGE_NULL_MEMBER);
        }
        if (attendance.containsKey(null) || attendance.containsValue(null)) {
            throw new IllegalValueException(MESSAGE_NULL_ATTENDANCE);
        }
        if (dismissedReminderStarts.contains(null)) {
            throw new IllegalValueException(MESSAGE_NULL_DISMISSED_REMINDER_START);
        }
        try {
            List<Name> memberNames = members.stream().map(Name::new).toList();
            Map<Name, AttendanceStatus> attendanceRecord = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : attendance.entrySet()) {
                attendanceRecord.put(new Name(entry.getKey()), parseStatus(entry.getValue()));
            }
            return new Event(name, new EventTime(startTime), new EventTime(endTime), memberNames,
                    attendanceRecord, dismissedReminderStarts.stream().map(EventTime::new).collect(Collectors.toSet()),
                    archived);
        } catch (IllegalArgumentException e) {
            throw new IllegalValueException(e.getMessage());
        }
    }

    private AttendanceStatus parseStatus(String value) throws IllegalValueException {
        try {
            return AttendanceStatus.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalValueException(MESSAGE_INVALID_ATTENDANCE_STATUS);
        }
    }
}
