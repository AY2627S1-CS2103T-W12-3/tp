package coordimate.storage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 */
class JsonAdaptedEvent {
    public static final String MESSAGE_NULL_MEMBER = "Event members must not contain null entries.";
    public static final String MESSAGE_NULL_ATTENDANCE =
            "Event attendance must not contain null names or statuses.";
    public static final String MESSAGE_INVALID_ATTENDANCE_STATUS =
            "Event attendance statuses must be either \"present\" or \"absent\".";

    private final String name;
    private final String startTime;
    private final String endTime;
    private final List<String> members = new ArrayList<>();
    private final Map<String, String> attendance = new LinkedHashMap<>();

    /**
     * Creates a JSON event record with the given name, time strings, member names, and attendance.
     */
    @JsonCreator
    public JsonAdaptedEvent(@JsonProperty("name") String name,
                            @JsonProperty("startTime") String startTime, @JsonProperty("endTime") String endTime,
                            @JsonProperty("members") List<String> members,
                            @JsonProperty("attendance") Map<String, String> attendance) {
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        if (members != null) {
            this.members.addAll(members);
        }
        if (attendance != null) {
            this.attendance.putAll(attendance);
        }
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
     * Copies an event's name, time representations, member names, and attendance into a JSON event record.
     */
    public JsonAdaptedEvent(Event source) {
        this(source.getName(), source.getStartTime().toString(), source.getEndTime().toString(),
                source.getMembers().stream().map(Name::toString).toList(),
                toAttendanceStrings(source));
    }

    private static Map<String, String> toAttendanceStrings(Event source) {
        Map<String, String> result = new LinkedHashMap<>();
        source.getAttendanceRecord().forEach((memberName, status) -> result.put(memberName.toString(),
                status.toString()));
        return result;
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
        try {
            List<Name> memberNames = members.stream().map(Name::new).toList();
            Map<Name, AttendanceStatus> attendanceRecord = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : attendance.entrySet()) {
                attendanceRecord.put(new Name(entry.getKey()), parseStatus(entry.getValue()));
            }
            return new Event(name, new EventTime(startTime), new EventTime(endTime), memberNames,
                    attendanceRecord);
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
