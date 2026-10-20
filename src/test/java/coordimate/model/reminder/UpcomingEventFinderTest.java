package coordimate.model.reminder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import coordimate.model.event.Event;
import coordimate.model.event.EventTime;

public class UpcomingEventFinderTest {
    private static final ZoneId LOCAL_ZONE = ZoneId.of("Asia/Makassar");
    private static final Clock NOW = clockAt("2026-10-19T15:00:00+08:00");

    private final UpcomingEventFinder finder = new UpcomingEventFinder(NOW);

    @Test
    public void findUpcomingEvents_timedEvents_includesBothWindowBoundaries() {
        Event past = event("Past", "19-10-2026 14:59");
        Event atStart = event("Starting now", "19-10-2026 15:00");
        Event inside = event("Tonight", "19-10-2026 23:00");
        Event atEnd = event("Tomorrow", "20-10-2026 15:00");
        Event beyond = event("Later", "20-10-2026 15:01");

        assertEquals(List.of(atStart, inside, atEnd),
                finder.findUpcomingEvents(List.of(beyond, atEnd, past, inside, atStart)));
    }

    @Test
    public void findUpcomingEvents_subsecondBoundaries_usesFullClockPrecision() {
        Event atStart = event("Starting now", "19-10-2026 15:00");
        Event atEnd = event("Tomorrow", "20-10-2026 15:00");
        UpcomingEventFinder justBefore = new UpcomingEventFinder(Clock.offset(NOW, Duration.ofNanos(-1)));
        UpcomingEventFinder justAfter = new UpcomingEventFinder(Clock.offset(NOW, Duration.ofNanos(1)));

        assertEquals(List.of(atStart), justBefore.findUpcomingEvents(List.of(atStart, atEnd)));
        assertEquals(List.of(atEnd), justAfter.findUpcomingEvents(List.of(atStart, atEnd)));
    }

    @Test
    public void findUpcomingEvents_pastStartWithFutureEnd_excludesEvent() {
        Event ongoing = new Event("Ongoing", new EventTime("19-10-2026 14:00"),
                new EventTime("19-10-2026 18:00"));

        assertEquals(List.of(), finder.findUpcomingEvents(List.of(ongoing)));
    }

    @Test
    public void findUpcomingEvents_dateOnly_includesTodayAndTomorrow() {
        Event yesterday = new Event("Yesterday", new EventTime("18-10-2026"), new EventTime("20-10-2026"));
        Event today = event("Today", "19-10-2026");
        Event tomorrow = event("Tomorrow", "20-10-2026");
        Event later = event("Later", "21-10-2026");

        assertEquals(List.of(today, tomorrow), finder.findUpcomingEvents(List.of(later, tomorrow, yesterday, today)));
    }

    @Test
    public void findUpcomingEvents_dateOnly_qualifiesThroughoutPreviousDayAndStartDate() {
        Event tomorrow = event("Tomorrow", "20-10-2026");
        List<Event> events = List.of(tomorrow);

        assertEquals(List.of(), finderAt("2026-10-18T23:59:59.999999999+08:00").findUpcomingEvents(events));
        assertEquals(events, finderAt("2026-10-19T00:00:00+08:00").findUpcomingEvents(events));
        assertEquals(events, finderAt("2026-10-19T23:59:59.999999999+08:00").findUpcomingEvents(events));
        assertEquals(events, finderAt("2026-10-20T00:00:00+08:00").findUpcomingEvents(events));
        assertEquals(events, finderAt("2026-10-20T23:59:59.999999999+08:00").findUpcomingEvents(events));
        assertEquals(List.of(), finderAt("2026-10-21T00:00:00+08:00").findUpcomingEvents(events));
    }

    @Test
    public void findUpcomingEvents_dateOnlyAndExplicitMidnight_haveDifferentEligibility() {
        Event dateOnly = event("Date only", "20-10-2026");
        Event midnight = event("Midnight", "20-10-2026 00:00");

        assertEquals(List.of(dateOnly, midnight),
                finderAt("2026-10-19T00:00:00+08:00").findUpcomingEvents(List.of(midnight, dateOnly)));
        assertEquals(List.of(dateOnly, midnight),
                finderAt("2026-10-20T00:00:00+08:00").findUpcomingEvents(List.of(midnight, dateOnly)));
        assertEquals(List.of(dateOnly), finderAt("2026-10-20T00:00:00.000000001+08:00")
                .findUpcomingEvents(List.of(midnight, dateOnly)));
    }

    @Test
    public void findUpcomingEvents_clockZone_determinesLocalDateAndTime() {
        Clock localMidnight = clockAt("2026-10-19T00:00:00+08:00");
        Event tomorrow = event("Tomorrow", "20-10-2026");
        Event midnight = event("Midnight", "19-10-2026 00:00");

        assertEquals(List.of(midnight, tomorrow),
                new UpcomingEventFinder(localMidnight).findUpcomingEvents(List.of(tomorrow, midnight)));
        assertEquals(List.of(midnight), new UpcomingEventFinder(localMidnight.withZone(ZoneId.of("UTC")))
                .findUpcomingEvents(List.of(tomorrow, midnight)));
    }

    @Test
    public void findUpcomingEvents_mixedStarts_ordersChronologicallyThenByNameIgnoringCase() {
        Event dateOnly = event("All day", "20-10-2026");
        Event alpha = event("alpha", "19-10-2026 16:00");
        Event zebra = event("Zebra", "19-10-2026 16:00");
        Event tomorrow = event("Tomorrow", "20-10-2026 00:00");
        List<Event> input = new ArrayList<>(List.of(tomorrow, zebra, dateOnly, alpha));
        List<Event> original = List.copyOf(input);

        List<Event> result = finder.findUpcomingEvents(input);

        assertEquals(List.of(alpha, zebra, dateOnly, tomorrow), result);
        assertEquals(original, input);
        assertThrows(UnsupportedOperationException.class, () -> result.add(tomorrow));
    }

    @Test
    public void findUpcomingEvents_yearBoundary_includesNextYearsEvent() {
        Event nextYear = event("New year", "01-01-2027 00:00");

        assertEquals(List.of(nextYear), finderAt("2026-12-31T23:59:00+08:00")
                .findUpcomingEvents(List.of(nextYear)));
    }

    @Test
    public void findUpcomingEvents_dateOnly_handlesYearAndLeapDayBoundaries() {
        Event newYear = event("New year", "01-01-2027");
        Event march = event("March", "01-03-2024");

        assertEquals(List.of(newYear), finderAt("2026-12-31T00:00:00+08:00")
                .findUpcomingEvents(List.of(newYear)));
        assertEquals(List.of(march), finderAt("2024-02-29T00:00:00+08:00")
                .findUpcomingEvents(List.of(march)));
        assertEquals(List.of(newYear), finderAt("2027-01-01T23:59:59.999999999+08:00")
                .findUpcomingEvents(List.of(newYear)));
        assertEquals(List.of(), finderAt("2027-01-02T00:00:00+08:00").findUpcomingEvents(List.of(newYear)));
        assertEquals(List.of(march), finderAt("2024-03-01T23:59:59.999999999+08:00")
                .findUpcomingEvents(List.of(march)));
        assertEquals(List.of(), finderAt("2024-03-02T00:00:00+08:00").findUpcomingEvents(List.of(march)));
    }

    @Test
    public void findUpcomingEvents_dateOnly_usesCalendarDaysAcrossDaylightSavingChanges() {
        String[][] days = {
            {"09-03-2026", "2026-03-07T23:59:59.999999999-05:00", "2026-03-08T00:00:00-05:00",
                "2026-03-08T23:59:59.999999999-04:00", "2026-03-09T00:00:00-04:00",
                "2026-03-09T23:59:59.999999999-04:00", "2026-03-10T00:00:00-04:00"},
            {"02-11-2026", "2026-10-31T23:59:59.999999999-04:00", "2026-11-01T00:00:00-04:00",
                "2026-11-01T23:59:59.999999999-05:00", "2026-11-02T00:00:00-05:00",
                "2026-11-02T23:59:59.999999999-05:00", "2026-11-03T00:00:00-05:00"},
            {"08-03-2026", "2026-03-06T23:59:59.999999999-05:00", "2026-03-07T00:00:00-05:00",
                "2026-03-07T23:59:59.999999999-05:00", "2026-03-08T00:00:00-05:00",
                "2026-03-08T23:59:59.999999999-04:00", "2026-03-09T00:00:00-04:00"},
            {"01-11-2026", "2026-10-30T23:59:59.999999999-04:00", "2026-10-31T00:00:00-04:00",
                "2026-10-31T23:59:59.999999999-04:00", "2026-11-01T00:00:00-04:00",
                "2026-11-01T23:59:59.999999999-05:00", "2026-11-02T00:00:00-05:00"}
        };
        for (String[] day : days) {
            List<Event> events = List.of(event("Date only", day[0]));
            for (int index = 1; index < day.length; index++) {
                Clock clock = Clock.fixed(ZonedDateTime.parse(day[index]).toInstant(), ZoneId.of("America/New_York"));
                List<Event> expected = index >= 2 && index <= 5 ? events : List.of();
                assertEquals(expected, new UpcomingEventFinder(clock).findUpcomingEvents(events));
            }
        }
    }

    @Test
    public void findUpcomingEvents_springClockChange_uses24ElapsedHours() {
        Clock beforeChange = Clock.fixed(ZonedDateTime.parse("2026-03-07T12:00:00-05:00").toInstant(),
                ZoneId.of("America/New_York"));
        Event atEnd = event("At boundary", "08-03-2026 13:00");
        Event beyond = event("Beyond boundary", "08-03-2026 13:01");

        assertEquals(List.of(atEnd), new UpcomingEventFinder(beforeChange)
                .findUpcomingEvents(List.of(beyond, atEnd)));
    }

    @Test
    public void findUpcomingEvents_autumnClockChange_uses24ElapsedHours() {
        Clock beforeChange = Clock.fixed(ZonedDateTime.parse("2026-10-31T12:00:00-04:00").toInstant(),
                ZoneId.of("America/New_York"));
        Event atEnd = event("At boundary", "01-11-2026 11:00");
        Event beyond = event("Beyond boundary", "01-11-2026 11:01");

        assertEquals(List.of(atEnd), new UpcomingEventFinder(beforeChange)
                .findUpcomingEvents(List.of(beyond, atEnd)));
    }

    @Test
    public void findUpcomingEvents_emptyList_returnsEmpty() {
        assertEquals(List.of(), finder.findUpcomingEvents(List.of()));
    }

    @Test
    public void nullArguments_rejected() {
        assertThrows(NullPointerException.class, () -> new UpcomingEventFinder(null));
        assertThrows(NullPointerException.class, () -> finder.findUpcomingEvents(null));
    }

    private static Event event(String name, String start) {
        EventTime time = new EventTime(start);
        return new Event(name, time, time);
    }

    private static Clock clockAt(String dateTime) {
        return Clock.fixed(ZonedDateTime.parse(dateTime).toInstant(), LOCAL_ZONE);
    }

    private static UpcomingEventFinder finderAt(String dateTime) {
        return new UpcomingEventFinder(clockAt(dateTime));
    }
}
