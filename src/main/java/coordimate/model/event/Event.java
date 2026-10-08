package coordimate.model.event;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import coordimate.model.person.Name;

/**
 * An event's identity is its trimmed, case-insensitive name.
 */
public final class Event {
    public static final String MESSAGE_EMPTY_NAME = "Event name must not be empty.";
    public static final String MESSAGE_INVALID_TIME_ORDER = "Start time must not be after end time.";
    public static final String MESSAGE_DUPLICATE_MEMBER = "An event must not list the same member more than once.";
    public static final String MESSAGE_UNASSIGNED_ATTENDANCE =
            "Attendance can only be recorded for a member assigned to the event.";

    private final String name;
    private final EventTime startTime;
    private final EventTime endTime;
    private final List<Name> members;
    private final Map<Name, AttendanceStatus> attendance;

    /**
     * Creates an event with no members.
     */
    public Event(String name, EventTime startTime, EventTime endTime) {
        this(name, startTime, endTime, List.of());
    }

    /**
     * Creates an event with a trimmed, non-empty name, a start time no later than its end time,
     * and distinct members kept in assignment order. No member has a recorded attendance status.
     * Preserves the name's internal spacing and capitalisation.
     */
    public Event(String name, EventTime startTime, EventTime endTime, List<Name> members) {
        this(name, startTime, endTime, members, Map.of());
    }

    /**
     * Creates an event with the given members and attendance record.
     * Every key in {@code attendance} must be an assigned member.
     */
    public Event(String name, EventTime startTime, EventTime endTime, List<Name> members,
                 Map<Name, AttendanceStatus> attendance) {
        this.name = requireNonNull(name).strip();
        if (this.name.isEmpty()) {
            throw new IllegalArgumentException(MESSAGE_EMPTY_NAME);
        }
        this.startTime = requireNonNull(startTime);
        this.endTime = requireNonNull(endTime);
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException(MESSAGE_INVALID_TIME_ORDER);
        }
        this.members = List.copyOf(members);
        if (new HashSet<>(this.members).size() != this.members.size()) {
            throw new IllegalArgumentException(MESSAGE_DUPLICATE_MEMBER);
        }
        requireNonNull(attendance);
        if (!this.members.containsAll(attendance.keySet())) {
            throw new IllegalArgumentException(MESSAGE_UNASSIGNED_ATTENDANCE);
        }
        this.attendance = Map.copyOf(attendance);
    }

    public String getName() {
        return name;
    }

    public EventTime getStartTime() {
        return startTime;
    }

    public EventTime getEndTime() {
        return endTime;
    }

    /**
     * Returns the names of the assigned members in assignment order, as an unmodifiable list.
     */
    public List<Name> getMembers() {
        return members;
    }

    /**
     * Returns true if the contact with the given name is assigned to this event.
     */
    public boolean hasMember(Name member) {
        return members.contains(requireNonNull(member));
    }

    /**
     * Returns the recorded attendance status for the given member, or empty if none has been recorded.
     */
    public Optional<AttendanceStatus> getAttendance(Name member) {
        requireNonNull(member);
        return Optional.ofNullable(attendance.get(member));
    }

    /**
     * Returns the full attendance record, as an unmodifiable map.
     */
    public Map<Name, AttendanceStatus> getAttendanceRecord() {
        return attendance;
    }

    /**
     * Returns a copy of this event with {@code member}'s attendance set to {@code status}.
     * {@code member} must already be assigned to this event.
     */
    public Event withAttendance(Name member, AttendanceStatus status) {
        requireNonNull(member);
        requireNonNull(status);
        if (!hasMember(member)) {
            throw new IllegalArgumentException(MESSAGE_UNASSIGNED_ATTENDANCE);
        }
        Map<Name, AttendanceStatus> updated = new HashMap<>(attendance);
        updated.put(member, status);
        return new Event(name, startTime, endTime, members, updated);
    }

    /**
     * Returns true if the other event has the same name, ignoring case and timings.
     */
    public boolean isSameEvent(Event other) {
        return other != null && name.equalsIgnoreCase(other.name);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Event otherEvent && name.equals(otherEvent.name)
                && startTime.equals(otherEvent.startTime) && endTime.equals(otherEvent.endTime)
                && members.equals(otherEvent.members) && attendance.equals(otherEvent.attendance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, startTime, endTime, members, attendance);
    }
}
