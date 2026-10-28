package coordimate.model.person;

import static coordimate.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import coordimate.testutil.UnicodeWhitespace;

public class NameTest {

    @Test
    public void isValidName_unicodeSpaces_onlyOrdinaryInternalSpacesAllowed() {
        for (String space : UnicodeWhitespace.ALL) {
            assertEquals(space.equals(" "), Name.isValidName("Alice" + space + "Tan"));
        }
    }

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
    public void isValidName_relationshipWords() {
        assertTrue(Name.isValidName("Rajesh s/o Kumar"));
        assertTrue(Name.isValidName("Priya d/o Raman"));
        assertTrue(Name.isValidName("RAJESH S/O KUMAR")); // any capitalization
        assertTrue(Name.isValidName("Rajesh s/o")); // at the end of the name

        assertFalse(Name.isValidName("s/o")); // needs a letter outside the relationship word
        assertFalse(Name.isValidName("s/o d/o"));
        assertFalse(Name.isValidName("Rajeshs/o Kumar")); // not a separate word
        assertFalse(Name.isValidName("Rajesh s/oKumar"));
        assertFalse(Name.isValidName("Rajesh w/o Kumar")); // only s/o and d/o are allowed
        assertFalse(Name.isValidName("Alex/Yeoh"));
    }

    @Test
    public void constructor_surroundingSpaces_trimmed() {
        assertEquals("Alice Tan", new Name("  Alice Tan  ").getFullName());
    }

    @Test
    public void constructor_internalSpaces_collapsedToOne() {
        assertEquals("Alice Tan Mei", new Name("Alice   Tan  Mei").getFullName());
        assertEquals(new Name("Alice Tan"), new Name(" Alice    Tan "));
        assertEquals(new Name("Alice Tan").hashCode(), new Name("Alice    Tan").hashCode());
    }

    @Test
    public void isValidName_internalSpaces_lengthCountedAfterCollapsing() {
        String eightyCharacters = "A".repeat(39) + " " + "B".repeat(40);
        String withExtraSpaces = "A".repeat(39) + "     " + "B".repeat(40);
        assertTrue(Name.isValidName(withExtraSpaces));
        assertEquals(eightyCharacters, new Name(withExtraSpaces).getFullName());
        assertFalse(Name.isValidName("A".repeat(40) + "  " + "B".repeat(40)));
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
