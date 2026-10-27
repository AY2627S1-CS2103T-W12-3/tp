package coordimate.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import coordimate.commons.exceptions.IllegalValueException;
import coordimate.commons.util.JsonUtil;
import coordimate.model.event.AttendanceStatus;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.person.Name;
import coordimate.testutil.UnicodeWhitespace;

public class JsonAdaptedEventTest {

    @Test
    public void toModelType_unicodeSpaces_normalized() throws Exception {
        Event expected = new Event("Final Concert", new EventTime("08-08-2026"), new EventTime("09-08-2026"));
        for (String space : UnicodeWhitespace.ALL) {
            if (Character.getType(space.codePointAt(0)) == Character.SPACE_SEPARATOR) {
                assertEquals(expected, new JsonAdaptedEvent(space + "Final" + space + "Concert" + space,
                        "08-08-2026", "09-08-2026", List.of(), Map.of()).toModelType());
            }
        }
    }

    @Test
    public void hasYearZeroEventDate_yearZeroInDatesOnly_returnsTrue() {
        for (String value : List.of("01-01-0000", "01-01-0000 00:00", " 01-01-0000 12:30 ")) {
            assertTrue(new JsonAdaptedEvent("Concert", value, "02-01-2026").hasYearZeroEventDate());
            assertTrue(new JsonAdaptedEvent("Concert", "01-01-2026", value).hasYearZeroEventDate());
            assertFalse(new JsonAdaptedEvent("Concert", "01-01-2026", "02-01-2026", List.of(), Map.of(),
                    List.of("01-01-2026", value)).hasYearZeroEventDate());
        }
    }

    @Test
    public void hasYearZeroEventDate_nullPositiveYearOrMalformedDates_returnsFalse() {
        for (String value : Arrays.asList(null, "", "01-01-0001", "31-12-9999 23:59", "0000-01-01",
                "01-01-0000 24:00:00")) {
            JsonAdaptedEvent event = new JsonAdaptedEvent("Concert", value, value, List.of(), Map.of(),
                    Arrays.asList(value));

            assertFalse(event.hasYearZeroEventDate());
        }
    }

    @Test
    public void removeYearZeroReminderStarts_keepsValidHistoryAndEventDetails() throws Exception {
        JsonAdaptedEvent event = new JsonAdaptedEvent("Concert", "08-08-2026", "09-08-2026",
                List.of("Alex Yeoh"), Map.of("Alex Yeoh", "present"),
                List.of("08-08-2026", "01-01-0000", " 02-01-0000 12:30 "));

        assertTrue(event.removeYearZeroReminderStarts());
        assertFalse(event.removeYearZeroReminderStarts());
        assertEquals(new Event("Concert", new EventTime("08-08-2026"), new EventTime("09-08-2026"),
                List.of(new Name("Alex Yeoh")), Map.of(new Name("Alex Yeoh"), AttendanceStatus.PRESENT),
                Set.of(new EventTime("08-08-2026"))), event.toModelType());
    }

    @Test
    public void toModelType_positiveYearBoundaries_roundTrip() throws Exception {
        for (String time : new String[] {"", " 00:00"}) {
            Event expected = new Event("Boundary Years", new EventTime("01-01-0001" + time),
                    new EventTime("31-12-9999" + time));
            String json = JsonUtil.toJsonString(new JsonAdaptedEvent(expected));
            assertEquals(expected, JsonUtil.fromJsonString(json, JsonAdaptedEvent.class).toModelType());
        }
    }

    @Test
    public void toModelType_yearZero_throwsIllegalValueException() {
        for (String value : new String[] {"01-01-0000", "01-01-0000 00:00"}) {
            for (JsonAdaptedEvent event : new JsonAdaptedEvent[] {
                new JsonAdaptedEvent("Concert", value, "02-01-0001"),
                new JsonAdaptedEvent("Concert", "01-01-0001", value),
                new JsonAdaptedEvent("Concert", "01-01-0001", "02-01-0001", List.of(), Map.of(), List.of(value))}) {
                assertEquals(EventTime.MESSAGE_CONSTRAINTS,
                        assertThrows(IllegalValueException.class, event::toModelType).getMessage());
            }
        }
    }

    @Test
    public void toModelType_dismissalHistory_preservesDateOnlyAndTimedStarts() throws Exception {
        EventTime dateOnly = new EventTime("08-08-2026");
        EventTime midnight = new EventTime("08-08-2026 00:00");
        Event expected = new Event("Concert", dateOnly, new EventTime("09-08-2026"), List.of(), Map.of(),
                Set.of(dateOnly, midnight));
        String json = JsonUtil.toJsonString(new JsonAdaptedEvent(expected));

        assertEquals(expected, JsonUtil.fromJsonString(json, JsonAdaptedEvent.class).toModelType());
        assertEquals(expected, new JsonAdaptedEvent("Concert", "08-08-2026", "09-08-2026", List.of(), Map.of(),
                List.of("08-08-2026", "08-08-2026 00:00")).toModelType());
    }

    @Test
    public void toModelType_legacyJsonOrNullHistory_loadsEmptyHistory() throws Exception {
        String legacyJson = """
                {"name":"Concert","startTime":"08-08-2026","endTime":"09-08-2026"}
                """;
        String nullHistoryJson = """
                {"name":"Concert","startTime":"08-08-2026","endTime":"09-08-2026",
                 "dismissedReminderStarts":null}
                """;
        for (String json : List.of(legacyJson, nullHistoryJson)) {
            Event loaded = JsonUtil.fromJsonString(json, JsonAdaptedEvent.class).toModelType();
            assertEquals(Set.of(), loaded.getDismissedReminderStarts());
            assertEquals(new Event("Concert", new EventTime("08-08-2026"), new EventTime("09-08-2026")), loaded);
        }
    }

    @Test
    public void toModelType_archivedEvent_roundTripsArchiveStatus() throws Exception {
        Event archived = new Event("Concert", new EventTime("08-08-2026"), new EventTime("09-08-2026"))
                .withArchived(true);
        String json = JsonUtil.toJsonString(new JsonAdaptedEvent(archived));

        assertEquals(archived, JsonUtil.fromJsonString(json, JsonAdaptedEvent.class).toModelType());
    }

    @Test
    public void toModelType_missingOrNullArchiveStatus_loadsAsNotArchived() throws Exception {
        String legacyJson = """
                {"name":"Concert","startTime":"08-08-2026","endTime":"09-08-2026"}
                """;
        String nullJson = """
                {"name":"Concert","startTime":"08-08-2026","endTime":"09-08-2026","archived":null}
                """;
        for (String json : List.of(legacyJson, nullJson)) {
            assertFalse(JsonUtil.fromJsonString(json, JsonAdaptedEvent.class).toModelType().isArchived());
        }
    }

    @Test
    public void toModelType_nullDismissedStart_throwsIllegalValueException() {
        JsonAdaptedEvent event = new JsonAdaptedEvent("Concert", "08-08-2026", "09-08-2026", List.of(), Map.of(),
                Arrays.asList("08-08-2026", null));

        assertEquals(JsonAdaptedEvent.MESSAGE_NULL_DISMISSED_REMINDER_START,
                assertThrows(IllegalValueException.class, event::toModelType).getMessage());
    }

    @Test
    public void toModelType_invalidDismissedStart_throwsIllegalValueException() {
        for (String invalid : List.of("", "31-02-2026", "08-08-2026 24:00", "2026-08-08")) {
            JsonAdaptedEvent event = new JsonAdaptedEvent("Concert", "08-08-2026", "09-08-2026", List.of(), Map.of(),
                    List.of(invalid));

            assertThrows(IllegalValueException.class, event::toModelType);
        }
    }

    @Test
    public void toModelType_repeatedDismissedStart_loadsOneEntry() throws Exception {
        Event loaded = new JsonAdaptedEvent("Concert", "08-08-2026", "09-08-2026", List.of(), Map.of(),
                List.of("08-08-2026", "08-08-2026")).toModelType();

        assertEquals(Set.of(new EventTime("08-08-2026")), loaded.getDismissedReminderStarts());
    }

    @Test
    public void toModelType_validDetails_preservesOptionalTimesAndTrimsName() throws Exception {
        for (String start : new String[] {"08-08-2026", "08-08-2026 15:00"}) {
            Event expected = new Event("Concert", new EventTime(start), new EventTime("08-08-2026 18:00"));
            assertEquals(expected, new JsonAdaptedEvent(expected).toModelType());
            assertEquals(expected, new JsonAdaptedEvent("  Concert  ", start, "08-08-2026 18:00").toModelType());
        }
    }

    @Test
    public void toModelType_members_preservedInOrder() throws Exception {
        Event expected = new Event("Concert", new EventTime("08-08-2026"), new EventTime("08-08-2026 18:00"),
                List.of(new Name("Bernice Yu"), new Name("Alex Yeoh")));
        assertEquals(expected, new JsonAdaptedEvent(expected).toModelType());
        assertEquals(expected, new JsonAdaptedEvent("Concert", "08-08-2026", "08-08-2026 18:00",
                List.of("Bernice Yu", "Alex Yeoh")).toModelType());
    }

    @Test
    public void toModelType_missingMembers_noMembers() throws Exception {
        Event expected = new Event("Concert", new EventTime("08-08-2026"), new EventTime("08-08-2026"));
        assertEquals(expected, new JsonAdaptedEvent("Concert", "08-08-2026", "08-08-2026", null).toModelType());
        assertEquals(expected, new JsonAdaptedEvent("Concert", "08-08-2026", "08-08-2026").toModelType());
    }

    @Test
    public void toModelType_invalidMembers_throwsIllegalValueException() {
        List<List<String>> invalidMembers = List.of(
                Arrays.asList("Alex Yeoh", null), List.of("Alex Yeoh!"), List.of(""),
                List.of("Alex Yeoh", "Bernice Yu", "Alex Yeoh"));
        String[] messages = {
            JsonAdaptedEvent.MESSAGE_NULL_MEMBER, Name.MESSAGE_CONSTRAINTS, Name.MESSAGE_CONSTRAINTS,
            Event.MESSAGE_DUPLICATE_MEMBER
        };
        for (int i = 0; i < messages.length; i++) {
            JsonAdaptedEvent event = new JsonAdaptedEvent("Concert", "08-08-2026", "08-08-2026", invalidMembers.get(i));
            assertEquals(messages[i], assertThrows(IllegalValueException.class, event::toModelType).getMessage());
        }
    }

    @Test
    public void toModelType_missingField_throwsIllegalValueException() {
        JsonAdaptedEvent[] events = {
            new JsonAdaptedEvent(null, "08-08-2026", "08-08-2026"),
            new JsonAdaptedEvent("Concert", null, "08-08-2026"),
            new JsonAdaptedEvent("Concert", "08-08-2026", null)
        };
        for (JsonAdaptedEvent event : events) {
            assertEquals("Event name, start time and end time must be present.",
                    assertThrows(IllegalValueException.class, event::toModelType).getMessage());
        }
    }

    @Test
    public void toModelType_invalidDetails_throwsIllegalValueException() {
        JsonAdaptedEvent[] events = {
            new JsonAdaptedEvent(" ", "08-08-2026", "08-08-2026"),
            new JsonAdaptedEvent("Concert", "31-02-2026", "08-08-2026"),
            new JsonAdaptedEvent("Concert", "08-08-2026", "08-08-2026 24:00"),
            new JsonAdaptedEvent("Concert", "", "08-08-2026"),
            new JsonAdaptedEvent("Concert", "08-08-2026", " "),
            new JsonAdaptedEvent("Concert", "08-08-2026 18:00", "08-08-2026 15:00"),
            new JsonAdaptedEvent("E".repeat(Event.MAX_NAME_LENGTH + 1), "08-08-2026", "08-08-2026"),
            new JsonAdaptedEvent("Final\u0007Concert", "08-08-2026", "08-08-2026")
        };
        String[] messages = {
            Event.MESSAGE_EMPTY_NAME, EventTime.MESSAGE_CONSTRAINTS, EventTime.MESSAGE_CONSTRAINTS,
            EventTime.MESSAGE_EMPTY, EventTime.MESSAGE_EMPTY, Event.MESSAGE_INVALID_TIME_ORDER,
            Event.MESSAGE_NAME_LENGTH_CONSTRAINTS, Event.MESSAGE_NAME_CONSTRAINTS
        };
        for (int i = 0; i < events.length; i++) {
            assertEquals(messages[i], assertThrows(IllegalValueException.class, events[i]::toModelType).getMessage());
        }
    }

    @Test
    public void toModelType_attendance_preservedInRoundTrip() throws Exception {
        Event expected = new Event("Concert", new EventTime("08-08-2026"), new EventTime("08-08-2026 18:00"),
                List.of(new Name("Bernice Yu"), new Name("Alex Yeoh")),
                Map.of(new Name("Bernice Yu"), AttendanceStatus.PRESENT));
        assertEquals(expected, new JsonAdaptedEvent(expected).toModelType());
    }

    @Test
    public void toModelType_missingAttendance_noAttendanceRecorded() throws Exception {
        Event expected = new Event("Concert", new EventTime("08-08-2026"), new EventTime("08-08-2026"),
                List.of(new Name("Alex Yeoh")));
        assertEquals(expected, new JsonAdaptedEvent("Concert", "08-08-2026", "08-08-2026",
                List.of("Alex Yeoh")).toModelType());
    }

    @Test
    public void toModelType_attendanceForUnassignedMember_throwsIllegalValueException() {
        JsonAdaptedEvent event = new JsonAdaptedEvent("Concert", "08-08-2026", "08-08-2026",
                List.of("Alex Yeoh"), Map.of("Bernice Yu", "present"));
        assertEquals(Event.MESSAGE_UNASSIGNED_ATTENDANCE,
                assertThrows(IllegalValueException.class, event::toModelType).getMessage());
    }

    @Test
    public void toModelType_invalidAttendanceStatus_throwsIllegalValueException() {
        JsonAdaptedEvent event = new JsonAdaptedEvent("Concert", "08-08-2026", "08-08-2026",
                List.of("Alex Yeoh"), Map.of("Alex Yeoh", "maybe"));
        assertEquals(JsonAdaptedEvent.MESSAGE_INVALID_ATTENDANCE_STATUS,
                assertThrows(IllegalValueException.class, event::toModelType).getMessage());
    }

    @Test
    public void toModelType_nullAttendanceKeyOrValue_throwsIllegalValueException() {
        Map<String, String> nullKey = new HashMap<>();
        nullKey.put(null, "present");
        JsonAdaptedEvent eventWithNullKey = new JsonAdaptedEvent("Concert", "08-08-2026", "08-08-2026",
                List.of("Alex Yeoh"), nullKey);
        assertEquals(JsonAdaptedEvent.MESSAGE_NULL_ATTENDANCE,
                assertThrows(IllegalValueException.class, eventWithNullKey::toModelType).getMessage());

        Map<String, String> nullValue = new HashMap<>();
        nullValue.put("Alex Yeoh", null);
        JsonAdaptedEvent eventWithNullValue = new JsonAdaptedEvent("Concert", "08-08-2026", "08-08-2026",
                List.of("Alex Yeoh"), nullValue);
        assertEquals(JsonAdaptedEvent.MESSAGE_NULL_ATTENDANCE,
                assertThrows(IllegalValueException.class, eventWithNullValue::toModelType).getMessage());
    }
}
