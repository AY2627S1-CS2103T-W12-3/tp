package coordimate.model.person;

import static coordimate.testutil.Assert.assertThrows;
import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BENSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

public class ContactFilterPredicateTest {
    private static final Predicate<Person> IS_ALICE = ALICE::equals;
    private static final Predicate<Person> HAS_FRIENDS_TAG = person -> person.getTags().stream()
            .anyMatch(tag -> tag.getTagName().equals("friends"));

    @Test
    public void constructor_invalidCriteria_rejected() {
        assertThrows(AssertionError.class, () -> new ContactFilterPredicate(List.of()));
        assertThrows(NullPointerException.class, () -> new ContactFilterPredicate(Arrays.asList(IS_ALICE, null)));
    }

    @Test
    public void constructor_copiesCriteria_subsequentChangesDoNotAffectFilter() {
        List<Predicate<Person>> criteria = new ArrayList<>(List.of(IS_ALICE));
        ContactFilterPredicate predicate = new ContactFilterPredicate(criteria);

        criteria.clear();
        criteria.add(BENSON::equals);

        assertTrue(predicate.test(ALICE));
        assertFalse(predicate.test(BENSON));
    }

    @Test
    public void withCriteriaReplaced_invalidReplacement_rejectedWithoutChangingOriginal() {
        ContactFilterPredicate predicate = new ContactFilterPredicate(List.of(IS_ALICE));
        assertThrows(NullPointerException.class, () -> predicate.withCriteriaReplaced(null));
        assertThrows(NullPointerException.class, () -> predicate.withCriteriaReplaced(criterion -> null));
        assertTrue(predicate.test(ALICE));
        assertFalse(predicate.test(BENSON));
    }

    @Test
    public void toString_includesCriteria() {
        ContactFilterPredicate predicate = new ContactFilterPredicate(List.of(IS_ALICE));
        assertEquals(ContactFilterPredicate.class.getCanonicalName() + "{criteria=" + List.of(IS_ALICE) + "}",
                predicate.toString());
    }

    @Test
    public void test_requiresEveryCriterion() {
        ContactFilterPredicate predicate = new ContactFilterPredicate(List.of(HAS_FRIENDS_TAG, IS_ALICE));
        assertTrue(predicate.test(ALICE));
        assertFalse(predicate.test(BENSON));
    }

    @Test
    public void withCriteriaReplaced_replacesOnlyReturnedCriteria() {
        ContactFilterPredicate predicate = new ContactFilterPredicate(List.of(HAS_FRIENDS_TAG, IS_ALICE));
        Predicate<Person> isBenson = BENSON::equals;

        ContactFilterPredicate replaced = predicate.withCriteriaReplaced(criterion ->
                criterion == IS_ALICE ? isBenson : criterion);

        assertEquals(new ContactFilterPredicate(List.of(HAS_FRIENDS_TAG, isBenson)), replaced);
        assertTrue(replaced.test(BENSON));
        assertFalse(replaced.test(ALICE));
        assertTrue(predicate.test(ALICE));
        assertFalse(predicate.test(BENSON));
        assertEquals(predicate, predicate.withCriteriaReplaced(criterion -> criterion));
    }

    @Test
    public void equals() {
        ContactFilterPredicate predicate = new ContactFilterPredicate(List.of(IS_ALICE));
        assertEquals(predicate, new ContactFilterPredicate(List.of(IS_ALICE)));
        assertEquals(predicate.hashCode(), new ContactFilterPredicate(List.of(IS_ALICE)).hashCode());
        assertNotEquals(predicate, new ContactFilterPredicate(List.of(HAS_FRIENDS_TAG)));
        assertNotEquals(predicate, IS_ALICE);
        assertThrows(NullPointerException.class, () -> new ContactFilterPredicate(null));
    }
}
