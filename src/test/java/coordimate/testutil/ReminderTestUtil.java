package coordimate.testutil;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Supplies clocks with a shared starting time and zone for reminder tests.
 */
public final class ReminderTestUtil {
    private static final Instant START = ZonedDateTime.parse("2026-10-19T15:00:00+08:00").toInstant();
    private static final ZoneId ZONE = ZoneId.of("Asia/Makassar");

    private ReminderTestUtil() {}

    /**
     * Returns a fresh mutable clock starting at 15:00 on 19 October 2026 in Asia/Makassar.
     */
    public static MutableClock createClock() {
        return new MutableClock(START, ZONE);
    }

    /**
     * Returns a clock fixed at the same starting time and zone as the mutable reminder clocks.
     */
    public static Clock createFixedClock() {
        return Clock.fixed(START, ZONE);
    }
}
