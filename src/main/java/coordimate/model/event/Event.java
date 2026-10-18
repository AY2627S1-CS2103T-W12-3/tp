package coordimate.model.event;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import coordimate.model.person.Name;
import coordimate.model.person.PrintableText;

/**
 * An event's identity is its trimmed, case-insensitive name.
 */
public final class Event {
    public static final int MAX_NAME_LENGTH = 100;
    public static final String MESSAGE_EMPTY_NAME = "Event name must not be empty. Example: evn/Final Concert";
    public static final String MESSAGE_NAME_LENGTH_CONSTRAINTS = "Event name must not exceed " + MAX_NAME_LENGTH
            + " characters. Example: evn/Final Concert";
    public static final String MESSAGE_NAME_CONSTRAINTS = "Invalid event name. Use letters, digits, spaces, "
            + "or punctuation. Example: evn/Final-Concert";
    public static final String MESSAGE_INVALID_TIME_ORDER = "Start time must not be after end time. "
            + "Example: st/17-09-2026 16:30 et/17-09-2026 18:30";
    public static final String MESSAGE_DUPLICATE_MEMBER = "An event must not list the same member more than once.";
    public static final String MESSAGE_UNASSIGNED_ATTENDANCE =
            "Attendance can only be recorded for a member assigned to the event.";

    private final String name;
    private final EventTime startTime;
    private final EventTime endTime;
    private final List<Name> members;
    private final Map<Name, AttendanceStatus> attendance;
    private final Set<EventTime> dismissedReminderStarts;

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
        this(name, startTime, endTime, members, attendance, Set.of());
    }

    /**
     * Creates an event with attendance and the start values whose reminders have been dismissed.
     * Date-only starts and explicitly timed starts are distinct dismissal schedules.
     */
    public Event(String name, EventTime startTime, EventTime endTime, List<Name> members,
                 Map<Name, AttendanceStatus> attendance, Set<EventTime> dismissedReminderStarts) {
        this.name = requireNonNull(name).strip();
        if (this.name.isEmpty()) {
            throw new IllegalArgumentException(MESSAGE_EMPTY_NAME);
        }
        if (this.name.codePointCount(0, this.name.length()) > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(MESSAGE_NAME_LENGTH_CONSTRAINTS);
        }
        if (!isValidName(this.name)) {
            throw new IllegalArgumentException(MESSAGE_NAME_CONSTRAINTS);
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
        this.dismissedReminderStarts = Set.copyOf(dismissedReminderStarts);
    }

    public String getName() {
        return name;
    }

    /**
     * Returns true if {@code test}, ignoring surrounding whitespace, is a valid event name of 1 to
     * {@value #MAX_NAME_LENGTH} printable characters.
     */
    public static boolean isValidName(String test) {
        requireNonNull(test);
        return PrintableText.isValid(test.strip(), MAX_NAME_LENGTH);
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
     * Returns the dismissed start values, including schedules that are no longer current.
     */
    public Set<EventTime> getDismissedReminderStarts() {
        return dismissedReminderStarts;
    }

    /**
     * Returns whether the reminder for the current start value has been dismissed.
     */
    public boolean isReminderDismissed() {
        return dismissedReminderStarts.contains(startTime);
    }

    /**
     * Returns a copy with the current start value added to its dismissal history.
     */
    public Event withReminderDismissed() {
        Set<EventTime> updated = new HashSet<>(dismissedReminderStarts);
        updated.add(startTime);
        return new Event(name, startTime, endTime, members, attendance, updated);
    }

    /**
     * Returns a copy with the given name and times, preserving members, attendance and dismissal history.
     */
    public Event withDetails(String name, EventTime startTime, EventTime endTime) {
        return new Event(name, startTime, endTime, members, attendance, dismissedReminderStarts);
    }

    /**
     * Returns a copy with the given members and attendance, preserving event details and dismissal history.
     */
    public Event withMembersAndAttendance(List<Name> members, Map<Name, AttendanceStatus> attendance) {
        return new Event(name, startTime, endTime, members, attendance, dismissedReminderStarts);
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
        return withMembersAndAttendance(members, updated);
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
                && members.equals(otherEvent.members) && attendance.equals(otherEvent.attendance)
                && dismissedReminderStarts.equals(otherEvent.dismissedReminderStarts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, startTime, endTime, members, attendance, dismissedReminderStarts);
    }
}
