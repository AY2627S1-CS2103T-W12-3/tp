package coordimate.model.person;

import static coordimate.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import coordimate.testutil.PersonBuilder;

public class ContactMatchesKeywordPredicateTest {

    @Test
    public void constructor_nullKeyword_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new ContactMatchesKeywordPredicate(null));
    }

    @Test
    public void test_nameContainsKeyword_returnsTrue() {
        Person person = new PersonBuilder().withName("Alice Pauline").build();

        assertTrue(new ContactMatchesKeywordPredicate("Alice").test(person));
        assertTrue(new ContactMatchesKeywordPredicate("alice").test(person));
        assertTrue(new ContactMatchesKeywordPredicate("Pauline").test(person));
        assertTrue(new ContactMatchesKeywordPredicate("ice Pau").test(person));
    }

    @Test
    public void test_phoneContainsKeyword_returnsTrue() {
        Person person = new PersonBuilder().withPhone("94351253").build();

        assertTrue(new ContactMatchesKeywordPredicate("94351253").test(person));
        assertTrue(new ContactMatchesKeywordPredicate("4351").test(person));
    }

    @Test
    public void test_emailContainsKeyword_returnsTrue() {
        Person person = new PersonBuilder().withEmail("alice@example.com").build();

        assertTrue(new ContactMatchesKeywordPredicate("alice@example.com").test(person));
        assertTrue(new ContactMatchesKeywordPredicate("EXAMPLE").test(person));
        assertTrue(new ContactMatchesKeywordPredicate("example.com").test(person));
    }

    @Test
    public void test_addressContainsKeyword_returnsTrue() {
        Person person = new PersonBuilder().withAddress("123, Jurong West Ave 6, #08-111").build();

        assertTrue(new ContactMatchesKeywordPredicate("Jurong").test(person));
        assertTrue(new ContactMatchesKeywordPredicate("#08-111").test(person));
    }

    @Test
    public void test_noFieldContainsKeyword_returnsFalse() {
        Person person = new PersonBuilder().withName("Alice Pauline").withPhone("94351253")
                .withEmail("alice@example.com").withAddress("123, Jurong West Ave 6, #08-111").build();

        assertFalse(new ContactMatchesKeywordPredicate("Benson").test(person));
    }

    @Test
    public void equals() {
        ContactMatchesKeywordPredicate firstPredicate = new ContactMatchesKeywordPredicate("Alice");
        ContactMatchesKeywordPredicate secondPredicate = new ContactMatchesKeywordPredicate("Benson");

        // same object -> returns true
        assertEquals(firstPredicate, firstPredicate);

        // same value -> returns true
        assertEquals(firstPredicate, new ContactMatchesKeywordPredicate("Alice"));

        // different types -> returns false
        assertNotEquals(firstPredicate, 1);

        // null -> returns false
        assertNotEquals(firstPredicate, null);

        // different keyword -> returns false
        assertNotEquals(firstPredicate, secondPredicate);
    }

    @Test
    public void toStringMethod() {
        ContactMatchesKeywordPredicate predicate = new ContactMatchesKeywordPredicate("Alice");
        String expected = ContactMatchesKeywordPredicate.class.getCanonicalName() + "{keyword=Alice}";
        assertEquals(expected, predicate.toString());
    }
}
