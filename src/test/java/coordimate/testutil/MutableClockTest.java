package coordimate.testutil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

public class MutableClockTest {
    private static final Instant START = Instant.parse("2026-10-19T07:00:00.123456789Z");
    private static final ZoneId ZONE = ZoneId.of("Asia/Makassar");

    @Test
    public void withZone_sameZone_returnsSameClockAndKeepsAdvancing() {
        MutableClock clock = new MutableClock(START, ZONE);
        Clock sameZone = clock.withZone(ZONE);

        assertSame(clock, sameZone);
        clock.advance(Duration.ofNanos(1));
        assertEquals(START.plusNanos(1), sameZone.instant());
    }

    @Test
    public void withZone_differentZones_keepsAllClocksAdvancing() {
        MutableClock clock = new MutableClock(START, ZONE);
        ZoneId otherZone = ZoneId.of("Europe/London");
        Clock other = clock.withZone(otherZone);
        Clock utc = other.withZone(ZoneId.of("UTC"));

        assertEquals(ZONE, clock.getZone());
        assertEquals(otherZone, other.getZone());
        assertEquals(ZoneId.of("UTC"), utc.getZone());
        assertEquals(START, other.instant());
        assertEquals(START, utc.instant());

        clock.advance(Duration.ofSeconds(60).plusNanos(1));
        Instant expected = START.plusSeconds(60).plusNanos(1);
        assertEquals(expected, clock.instant());
        assertEquals(expected, other.instant());
        assertEquals(expected, utc.instant());

        clock.advance(Duration.ofHours(1));
        assertEquals(expected.plusSeconds(3600), other.instant());
        assertEquals(expected.plusSeconds(3600), utc.instant());
    }
}
