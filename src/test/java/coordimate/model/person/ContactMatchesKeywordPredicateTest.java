package coordimate.model.person;

import static coordimate.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

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
        assertTrue(new ContactMatchesKeywordPredicate("aLIce").test(person));
        assertTrue(new ContactMatchesKeywordPredicate("Ali").test(person));
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

        assertTrue(new ContactMatchesKeywordPredicate("jUrOnG").test(person));
        assertTrue(new ContactMatchesKeywordPredicate("ong WEST").test(person));
        assertTrue(new ContactMatchesKeywordPredicate("#08-111").test(person));
    }

    @Test
    public void test_noAddress_doesNotMatchAddressKeyword() {
        Person person = new PersonBuilder().withoutAddress().build();

        assertFalse(new ContactMatchesKeywordPredicate("Jurong").test(person));
        assertTrue(new ContactMatchesKeywordPredicate("Amy").test(person));
    }

    @Test
    public void test_noFieldContainsKeyword_returnsFalse() {
        Person person = new PersonBuilder().withName("Alice Pauline").withPhone("94351253")
                .withEmail("alice@example.com").withAddress("123, Jurong West Ave 6, #08-111").build();

        assertFalse(new ContactMatchesKeywordPredicate("Benson").test(person));
        assertFalse(new ContactMatchesKeywordPredicate("Pauline Alice").test(person));
        assertFalse(new ContactMatchesKeywordPredicate("Alice Benson").test(person));
    }

    @Test
    public void test_turkishDefaultLocale_matchesIgnoringCase() {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            Person person = new PersonBuilder().withName("ALICE PAULINE").withEmail("ALICE@EXAMPLE.COM")
                    .withAddress("INDIA STREET").build();
            assertTrue(new ContactMatchesKeywordPredicate("alice pauline").test(person));
            assertTrue(new ContactMatchesKeywordPredicate("alice@example").test(person));
            assertTrue(new ContactMatchesKeywordPredicate("india street").test(person));
        } finally {
            Locale.setDefault(originalLocale);
        }
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
