package coordimate.model.tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import coordimate.model.person.Person;
import coordimate.testutil.PersonBuilder;

public class TagFilterPredicateTest {

    @Test
    public void test_matchesTagIgnoringCase() {
        Person tagged = new PersonBuilder().withTags("EXCO").build();
        Person untagged = new PersonBuilder().withTags("Logistics").build();

        assertTrue(new TagFilterPredicate("exco").test(tagged));
        assertFalse(new TagFilterPredicate("exco").test(untagged));
    }

    @Test
    public void withTagName_followsRenamedTag() {
        Person renamed = new PersonBuilder().withTags("Committee").build();
        TagFilterPredicate predicate = new TagFilterPredicate("EXCO").withTagName("Committee");

        assertEquals("Committee", predicate.getTagName());
        assertTrue(predicate.test(renamed));
    }

    @Test
    public void equals() {
        TagFilterPredicate predicate = new TagFilterPredicate("EXCO");

        assertEquals(predicate, new TagFilterPredicate("exco"));
        assertEquals(predicate.hashCode(), new TagFilterPredicate("exco").hashCode());
        assertNotEquals(predicate, new TagFilterPredicate("Sponsor"));
        assertNotEquals(predicate, "EXCO");
    }

    @Test
    public void toStringMethod() {
        assertEquals(TagFilterPredicate.class.getCanonicalName() + "{tagName=EXCO}",
                new TagFilterPredicate("EXCO").toString());
    }
}
