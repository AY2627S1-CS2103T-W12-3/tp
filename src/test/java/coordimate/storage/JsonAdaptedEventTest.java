package coordimate.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import coordimate.commons.exceptions.IllegalValueException;
import coordimate.model.event.AttendanceStatus;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.person.Name;

public class JsonAdaptedEventTest {
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
            new JsonAdaptedEvent("Concert", "08-08-2026 18:00", "08-08-2026 15:00")
        };
        String[] messages = {
            Event.MESSAGE_EMPTY_NAME, EventTime.MESSAGE_CONSTRAINTS, EventTime.MESSAGE_CONSTRAINTS,
            EventTime.MESSAGE_EMPTY, EventTime.MESSAGE_EMPTY, Event.MESSAGE_INVALID_TIME_ORDER
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
        Map<String, String> nullValue = new java.util.HashMap<>();
        nullValue.put("Alex Yeoh", null);
        JsonAdaptedEvent eventWithNullValue = new JsonAdaptedEvent("Concert", "08-08-2026", "08-08-2026",
                List.of("Alex Yeoh"), nullValue);
        assertEquals(JsonAdaptedEvent.MESSAGE_NULL_ATTENDANCE,
                assertThrows(IllegalValueException.class, eventWithNullValue::toModelType).getMessage());
    }
}
