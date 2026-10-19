package coordimate.model.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import coordimate.model.person.Name;

public class EventReminderHistoryTest {
    private final EventTime start = new EventTime("20-10-2026 15:00");
    private final EventTime end = new EventTime("21-10-2026 18:00");
    private final Name alex = new Name("Alex Yeoh");
    private final Event event = new Event("Concert", start, end, List.of(alex),
            Map.of(alex, AttendanceStatus.PRESENT));

    @Test
    public void constructor_noHistory_reminderNotDismissed() {
        assertEquals(Set.of(), event.getDismissedReminderStarts());
        assertFalse(event.isReminderDismissed());
    }

    @Test
    public void constructor_history_isDefensivelyCopiedAndUnmodifiable() {
        Set<EventTime> history = new HashSet<>(Set.of(start));
        Event dismissed = new Event("Concert", start, end, List.of(), Map.of(), history);
        history.clear();

        assertEquals(Set.of(start), dismissed.getDismissedReminderStarts());
        assertTrue(dismissed.isReminderDismissed());
        assertThrows(UnsupportedOperationException.class, () -> dismissed.getDismissedReminderStarts().clear());
    }

    @Test
    public void constructor_nullHistoryOrEntry_rejected() {
        assertThrows(NullPointerException.class, () -> new Event("Concert", start, end, List.of(), Map.of(), null));
        Set<EventTime> history = new HashSet<>();
        history.add(null);
        assertThrows(NullPointerException.class, () -> new Event("Concert", start, end, List.of(), Map.of(), history));
    }

    @Test
    public void withReminderDismissed_returnsCopyAndRepeatedDismissalAddsNoDuplicate() {
        Event dismissed = event.withReminderDismissed();

        assertFalse(event.isReminderDismissed());
        assertTrue(dismissed.isReminderDismissed());
        assertEquals(Set.of(start), dismissed.getDismissedReminderStarts());
        assertEquals(dismissed, dismissed.withReminderDismissed());
        assertEquals(event.getMembers(), dismissed.getMembers());
        assertEquals(event.getAttendanceRecord(), dismissed.getAttendanceRecord());
    }

    @Test
    public void withDetails_newStartAndReversion_resetDismissal() {
        EventTime revisedStart = new EventTime("20-10-2026 16:00");
        Event revised = event.withReminderDismissed().withDetails("Concert", revisedStart, end);

        assertFalse(revised.isReminderDismissed());
        assertEquals(Set.of(), revised.getDismissedReminderStarts());
        Event revisedDismissed = revised.withReminderDismissed();
        Event reverted = revisedDismissed.withDetails("Concert", start, end);
        assertFalse(reverted.isReminderDismissed());
        assertTrue(revisedDismissed.isReminderDismissed());
        assertEquals(Set.of(revisedStart), revisedDismissed.getDismissedReminderStarts());
        assertEquals(Set.of(), reverted.getDismissedReminderStarts());
    }

    @Test
    public void withDetails_nameAndEndOnly_preserveDismissalAndAttendance() {
        EventTime revisedEnd = new EventTime("22-10-2026");
        Event renamed = event.withReminderDismissed().withDetails("Renamed Concert", start, revisedEnd);

        assertTrue(renamed.isReminderDismissed());
        assertEquals("Renamed Concert", renamed.getName());
        assertEquals(revisedEnd, renamed.getEndTime());
        assertEquals(event.getMembers(), renamed.getMembers());
        assertEquals(event.getAttendanceRecord(), renamed.getAttendanceRecord());
    }

    @Test
    public void withDetails_dateOnlyAndMidnight_areDistinctSchedules() {
        Event dateOnly = event.withDetails("Concert", new EventTime("20-10-2026"), end).withReminderDismissed();
        Event midnight = dateOnly.withDetails("Concert", new EventTime("20-10-2026 00:00"), end);

        assertFalse(midnight.isReminderDismissed());
        assertFalse(midnight.withReminderDismissed().withDetails("Concert", dateOnly.getStartTime(), end)
                .isReminderDismissed());
        assertEquals(1, midnight.withReminderDismissed().getDismissedReminderStarts().size());
    }

    @Test
    public void withDetails_sameStart_preservesDismissal() {
        Event dismissed = event.withReminderDismissed();

        Event unchanged = dismissed.withDetails("Concert", new EventTime("20-10-2026 15:00"), end);

        assertTrue(unchanged.isReminderDismissed());
        assertEquals(dismissed, unchanged);
    }

    @Test
    public void withDetails_legacyHistory_newStartClearsPreviouslyDismissedValues() {
        EventTime previousStart = new EventTime("20-10-2026 16:00");
        Event loaded = new Event("Concert", start, end, event.getMembers(), event.getAttendanceRecord(),
                Set.of(start, previousStart));

        Event revised = loaded.withDetails("Concert", previousStart, end);

        assertFalse(revised.isReminderDismissed());
        assertEquals(Set.of(), revised.getDismissedReminderStarts());
    }

    @Test
    public void withMembersAndAttendance_andWithAttendance_preserveDismissalHistory() {
        Event dismissed = event.withReminderDismissed();
        Event marked = dismissed.withAttendance(alex, AttendanceStatus.ABSENT);
        Event unassigned = marked.withMembersAndAttendance(List.of(), Map.of());

        assertTrue(marked.isReminderDismissed());
        assertEquals(AttendanceStatus.ABSENT, marked.getAttendance(alex).orElseThrow());
        assertTrue(unassigned.isReminderDismissed());
        assertEquals(Set.of(start), unassigned.getDismissedReminderStarts());
        assertEquals(List.of(), unassigned.getMembers());
        assertEquals(Map.of(), unassigned.getAttendanceRecord());
    }

    @Test
    public void equalsAndHashCode_includeHistoryWithoutChangingIdentity() {
        Event dismissed = event.withReminderDismissed();
        Event same = new Event("Concert", start, end, event.getMembers(), event.getAttendanceRecord(), Set.of(start));

        assertTrue(event.isSameEvent(dismissed));
        assertNotEquals(event, dismissed);
        assertEquals(dismissed, same);
        assertEquals(dismissed.hashCode(), same.hashCode());
    }
}
