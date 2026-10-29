package coordimate.testutil;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * A clock that tests can advance without waiting for real time to pass.
 */
public class MutableClock extends Clock {
    private final ZoneId zone;
    private Instant current;

    /**
     * Creates a clock at the supplied instant in the given zone.
     */
    public MutableClock(Instant current, ZoneId zone) {
        this.current = current;
        this.zone = zone;
    }

    /**
     * Advances the clock by the supplied duration, retaining subsecond precision.
     */
    public void advance(Duration duration) {
        current = current.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    /**
     * Returns a fixed snapshot in the requested zone; later advances do not affect that snapshot.
     */
    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(current, zone);
    }

    @Override
    public Instant instant() {
        return current;
    }
}
