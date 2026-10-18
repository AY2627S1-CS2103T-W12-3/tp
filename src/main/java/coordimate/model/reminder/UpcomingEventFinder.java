package coordimate.model.reminder;

import static java.util.Objects.requireNonNull;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;

import coordimate.model.event.Event;
import coordimate.model.event.EventTime;

/**
 * Finds events starting within 24 hours, or today when their start has no time of day.
 */
public final class UpcomingEventFinder {
    private static final Duration REMINDER_WINDOW = Duration.ofHours(24);

    private final Clock clock;

    /**
     * Uses the clock's zone to interpret saved event times. The application should supply
     * {@link Clock#systemDefaultZone()} to follow the computer's local timezone.
     */
    public UpcomingEventFinder(Clock clock) {
        this.clock = requireNonNull(clock);
    }

    /**
     * Returns an unmodifiable list ordered by start time, then by name ignoring case.
     * Includes timed starts at both now and exactly 24 hours later. Date-only starts qualify
     * throughout their start date and use midnight only for ordering.
     */
    public List<Event> findUpcomingEvents(List<Event> events) {
        requireNonNull(events);
        ZonedDateTime now = ZonedDateTime.now(clock);
        Comparator<Event> byStart = Comparator.comparing(event -> startInstant(event, now.getZone()));
        return events.stream()
                .filter(event -> isEligible(event, now))
                .sorted(byStart.thenComparing(Event::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private boolean isEligible(Event event, ZonedDateTime now) {
        EventTime start = event.getStartTime();
        if (start.getDateTime().isEmpty()) {
            return start.getDate().equals(now.toLocalDate());
        }
        Instant startTime = startInstant(event, now.getZone());
        Instant currentTime = now.toInstant();
        return !startTime.isBefore(currentTime) && !startTime.isAfter(currentTime.plus(REMINDER_WINDOW));
    }

    private Instant startInstant(Event event, ZoneId zone) {
        EventTime start = event.getStartTime();
        return start.getDateTime().map(time -> time.atZone(zone).toInstant())
                .orElseGet(() -> start.getDate().atStartOfDay(zone).toInstant());
    }
}
