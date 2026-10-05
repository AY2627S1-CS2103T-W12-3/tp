package coordimate.model.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class EventTest {
    private final EventTime date = new EventTime("08-08-2026");
    private final EventTime time = new EventTime("08-08-2026 15:00");

    @Test
    public void constructor_nameIsTrimmed_emptyNameRejected() {
        assertEquals("Final Concert", new Event("  Final Concert  ", date, time).getName());
        assertThrows(IllegalArgumentException.class, () -> new Event(" \t\n", date, time));
        assertThrows(NullPointerException.class, () -> new Event(null, date, time));
        assertThrows(NullPointerException.class, () -> new Event("Concert", null, time));
        assertThrows(NullPointerException.class, () -> new Event("Concert", date, null));
    }

    @Test
    public void identity_sameNameDifferentTimes_isDuplicate() {
        Event event = new Event("Concert", date, time);
        Event differentTimes = new Event("Concert", date, date);
        assertTrue(event.isSameEvent(differentTimes));
        assertNotEquals(event, differentTimes);
        assertFalse(event.isSameEvent(new Event("Fair", date, time)));
        assertFalse(event.isSameEvent(null));
        assertEquals(event, new Event("Concert", date, time));
        assertEquals(event.hashCode(), new Event("Concert", date, time).hashCode());
    }

    @Test
    public void identity_sameNameDifferentCase_isDuplicateAndPreservesCase() {
        Event event = new Event("Fair", date, time);
        for (String name : new String[] {"fair", "FAIR", "fAiR"}) {
            Event duplicate = new Event(name, date, date);
            assertTrue(event.isSameEvent(duplicate));
            assertTrue(duplicate.isSameEvent(event));
            assertEquals(name, duplicate.getName());
        }
        assertEquals("Fair", event.getName());
    }

    @Test
    public void eventTime_validValues_preserveOptionalTime() {
        for (String value : new String[] {"29-02-2024", "09-10-2026", "08-08-2026 00:00", "08-08-2026 23:59"}) {
            assertTrue(EventTime.isValidTime(value));
            assertEquals(value, new EventTime(" " + value + " ").toString());
        }
        assertNotEquals(date, new EventTime("08-08-2026 00:00"));
        assertEquals(time, new EventTime("08-08-2026 15:00"));
        assertEquals(time.hashCode(), new EventTime("08-08-2026 15:00").hashCode());
    }

    @Test
    public void eventTime_invalidValues_rejected() {
        for (String value : new String[] {"", " ", "29-02-2026", "31-04-2026", "08-13-2026",
                "8-08-2026", "08-8-2026", "08-08-26", "2026-08-08", "15:00", "08-08-2026 24:00",
                "08-08-2026 15:60", "08-08-2026 3:00", "08-08-2026 15:00:00", "08-08-2026  15:00"}) {
            assertFalse(EventTime.isValidTime(value));
            assertThrows(IllegalArgumentException.class, () -> new EventTime(value));
        }
        assertThrows(NullPointerException.class, () -> new EventTime(null));
    }

    @Test
    public void constructor_startAfterEnd_rejected() {
        String[][] reversedTimes = {
            {"08-08-2026 18:00", "08-08-2026 15:00"},
            {"10-10-2026", "09-10-2026"},
            {"01-02-2026", "31-01-2026"},
            {"01-01-2027 00:00", "31-12-2026 23:59"},
            {"10-10-2026 00:00", "09-10-2026"},
            {"10-10-2026", "09-10-2026 23:59"}
        };
        for (String[] pair : reversedTimes) {
            EventTime start = new EventTime(pair[0]);
            EventTime end = new EventTime(pair[1]);
            assertTrue(start.isAfter(end));
            assertEquals(Event.MESSAGE_INVALID_TIME_ORDER,
                    assertThrows(IllegalArgumentException.class, () -> new Event("Concert", start, end)).getMessage());
        }
    }

    @Test
    public void constructor_orderedOrEqualTimes_accepted() {
        String[][] validTimes = {
            {"08-08-2026 15:00", "08-08-2026 18:00"},
            {"08-08-2026 15:00", "08-08-2026 15:00"},
            {"09-10-2026", "09-10-2026"},
            {"31-01-2026", "01-02-2026"},
            {"31-12-2026 23:59", "01-01-2027 00:00"},
            {"09-10-2026 23:59", "10-10-2026"},
            {"09-10-2026", "10-10-2026 00:00"},
            {"09-10-2026 23:59", "09-10-2026"},
            {"09-10-2026", "09-10-2026 00:00"}
        };
        for (String[] pair : validTimes) {
            EventTime start = new EventTime(pair[0]);
            EventTime end = new EventTime(pair[1]);
            assertFalse(start.isAfter(end));
            assertEquals(start, new Event("Concert", start, end).getStartTime());
        }
        assertThrows(NullPointerException.class, () -> date.isAfter(null));
    }
}
