package coordimate.model.person;

import static coordimate.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class NameTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Name(null));
    }

    @Test
    public void constructor_invalidName_throwsIllegalArgumentException() {
        String invalidName = "";
        assertThrows(IllegalArgumentException.class, () -> new Name(invalidName));
    }

    @Test
    public void isValidName() {
        // null name
        assertThrows(NullPointerException.class, () -> Name.isValidName(null));

        // invalid name
        assertFalse(Name.isValidName("")); // empty string
        assertFalse(Name.isValidName(" ")); // spaces only
        assertFalse(Name.isValidName("12345")); // must contain a letter
        assertFalse(Name.isValidName("-.")); // punctuation alone is not a name
        assertFalse(Name.isValidName("peter*")); // unsupported punctuation
        assertFalse(Name.isValidName("Peter\tJack")); // tabs are not spaces
        assertFalse(Name.isValidName("\tPeter")); // control characters are not trimmed as spaces
        assertFalse(Name.isValidName("a".repeat(81))); // too long

        // valid name
        assertTrue(Name.isValidName("A")); // minimum length
        assertTrue(Name.isValidName("a".repeat(80))); // maximum length
        assertTrue(Name.isValidName("peter jack")); // letters and spaces
        assertTrue(Name.isValidName("Anne-Marie O'Neil Jr.")); // supported punctuation
        assertTrue(Name.isValidName("José Tan")); // Unicode letters
        assertTrue(Name.isValidName("Capital Tan")); // with capital letters
        assertTrue(Name.isValidName("  Alice Tan  ")); // surrounding spaces are trimmed
    }

    @Test
    public void constructor_surroundingSpaces_trimmed() {
        assertEquals("Alice Tan", new Name("  Alice Tan  ").getFullName());
    }

    @Test
    public void equals() {
        Name name = new Name("Valid Name");

        // same values -> returns true
        assertTrue(name.equals(new Name("Valid Name")));

        // same object -> returns true
        assertTrue(name.equals(name));

        // null -> returns false
        assertFalse(name.equals(null));

        // different types -> returns false
        assertFalse(name.equals(5.0f));

        // different values -> returns false
        assertFalse(name.equals(new Name("Other Valid Name")));
    }
}
