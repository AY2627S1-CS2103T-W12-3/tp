package coordimate.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import coordimate.commons.exceptions.IllegalValueException;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;

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
}
