package coordimate.model.event;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.function.Predicate;

import coordimate.commons.util.ToStringBuilder;
import coordimate.model.person.Person;

/**
 * Tests that a {@code Person} is a member of the event with the given name, ignoring case.
 * Membership is looked up in the given event list on every test, so the result follows later changes to the
 * event's members. No person matches once no event has that name.
 */
public class EventMembersPredicate implements Predicate<Person> {
    private final String eventName;
    private final List<Event> events;

    /**
     * Creates a predicate for the event named {@code eventName} in {@code events}, which should be a live view
     * of the saved events.
     */
    public EventMembersPredicate(String eventName, List<Event> events) {
        this.eventName = requireNonNull(eventName).strip();
        this.events = requireNonNull(events);
    }

    @Override
    public boolean test(Person person) {
        return events.stream()
                .filter(event -> event.getName().equalsIgnoreCase(eventName))
                .anyMatch(event -> event.hasMember(person.getName()));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof EventMembersPredicate otherPredicate
                && eventName.equalsIgnoreCase(otherPredicate.eventName);
    }

    @Override
    public int hashCode() {
        return eventName.toLowerCase().hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("eventName", eventName).toString();
    }
}
