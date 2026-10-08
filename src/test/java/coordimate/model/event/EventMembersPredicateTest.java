package coordimate.model.event;

import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BENSON;
import static coordimate.testutil.TypicalPersons.CARL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class EventMembersPredicateTest {
    private final EventTime date = new EventTime("08-08-2026");
    private final Event concert = new Event("Final Concert", date, date, List.of(ALICE.getName(), BENSON.getName()));
    private final Event fair = new Event("Fair", date, date, List.of(CARL.getName()));

    @Test
    public void test_membersOfNamedEventOnly() {
        EventMembersPredicate predicate = new EventMembersPredicate("Final Concert", List.of(concert, fair));
        assertTrue(predicate.test(ALICE));
        assertTrue(predicate.test(BENSON));
        assertFalse(predicate.test(CARL));
    }

    @Test
    public void test_eventNameIgnoringCaseAndSpaces() {
        assertTrue(new EventMembersPredicate("  fINAL cONCERT ", List.of(concert)).test(ALICE));
    }

    @Test
    public void test_noSuchEvent_noMatches() {
        EventMembersPredicate predicate = new EventMembersPredicate("Gala", List.of(concert, fair));
        assertFalse(predicate.test(ALICE));
        assertFalse(predicate.test(CARL));
    }

    @Test
    public void test_followsChangesToEventList() {
        List<Event> events = new ArrayList<>(List.of(concert));
        EventMembersPredicate predicate = new EventMembersPredicate("Final Concert", events);
        assertFalse(predicate.test(CARL));

        events.set(0, new Event("Final Concert", date, date, List.of(ALICE.getName(), CARL.getName())));
        assertTrue(predicate.test(CARL));
        assertFalse(predicate.test(BENSON));

        events.clear();
        assertFalse(predicate.test(ALICE));
    }

    @Test
    public void equalityAndNullArguments() {
        EventMembersPredicate predicate = new EventMembersPredicate(" Final Concert ", List.of(concert));
        assertEquals("Final Concert", predicate.getEventName());
        assertEquals(predicate, new EventMembersPredicate("final concert", List.of()));
        assertEquals(predicate.hashCode(), new EventMembersPredicate("FINAL CONCERT", List.of()).hashCode());
        assertNotEquals(predicate, new EventMembersPredicate("Fair", List.of(concert)));
        assertNotEquals(predicate, null);
        assertThrows(NullPointerException.class, () -> new EventMembersPredicate(null, List.of()));
        assertThrows(NullPointerException.class, () -> new EventMembersPredicate("Final Concert", null));
    }
}
