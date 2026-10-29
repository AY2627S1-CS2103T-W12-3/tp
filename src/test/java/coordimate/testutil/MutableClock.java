package coordimate.testutil;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A clock that tests can advance without waiting for real time to pass.
 */
public class MutableClock extends Clock {
    private final ZoneId zone;
    private final AtomicReference<Instant> current;

    /**
     * Creates a clock at the supplied instant in the given zone.
     */
    public MutableClock(Instant current, ZoneId zone) {
        this(new AtomicReference<>(Objects.requireNonNull(current)), zone);
    }

    /**
     * Creates a clock that shares its current instant with clocks in other zones.
     */
    private MutableClock(AtomicReference<Instant> current, ZoneId zone) {
        this.current = current;
        this.zone = Objects.requireNonNull(zone);
    }

    /**
     * Advances the clock by the supplied duration, retaining subsecond precision.
     */
    public void advance(Duration duration) {
        current.updateAndGet(instant -> instant.plus(duration));
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    /**
     * Returns a clock in the requested zone that shares this clock's advances.
     */
    @Override
    public Clock withZone(ZoneId zone) {
        return this.zone.equals(zone) ? this : new MutableClock(current, zone);
    }

    @Override
    public Instant instant() {
        return current.get();
    }
}
