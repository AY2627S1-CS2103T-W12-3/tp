package coordimate.model.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;

public class EventTimeTest {
    @Test
    public void isValidTime_yearZero_rejected() {
        for (String value : new String[] {"01-01-0000", "01-01-0000 00:00"}) {
            assertFalse(EventTime.isValidTime(value));
            assertThrows(IllegalArgumentException.class, () -> new EventTime(value));
        }
    }

    @Test
    public void isValidTime_positiveYearBoundaries_accepted() {
        for (String value : new String[] {"01-01-0001", "01-01-0001 00:00",
            "31-12-9999", "31-12-9999 23:59"}) {
            assertTrue(EventTime.isValidTime(value));
        }
    }

    @Test
    public void getDate_dateOnly_returnsDate() {
        assertEquals(LocalDate.of(2026, 10, 20), new EventTime("20-10-2026").getDate());
    }

    @Test
    public void getDate_timed_returnsDate() {
        assertEquals(LocalDate.of(2026, 10, 20), new EventTime("20-10-2026 15:00").getDate());
    }

    @Test
    public void getDateTime_dateOnly_returnsEmpty() {
        EventTime start = new EventTime("20-10-2026");
        assertEquals(Optional.empty(), start.getDateTime());
        assertEquals("20-10-2026", start.toString());
    }

    @Test
    public void getDateTime_timed_returnsSpecifiedTimeIncludingMidnight() {
        assertEquals(Optional.of(LocalDateTime.of(2026, 10, 20, 15, 0)),
                new EventTime("20-10-2026 15:00").getDateTime());
        assertEquals(Optional.of(LocalDateTime.of(2026, 10, 20, 0, 0)),
                new EventTime("20-10-2026 00:00").getDateTime());
    }
}
