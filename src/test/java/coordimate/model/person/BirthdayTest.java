package coordimate.model.person;

import static coordimate.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

public class BirthdayTest {

    @Test
    public void constructor_invalidBirthday_throwsIllegalArgumentException() {
        assertThrows(NullPointerException.class, () -> new Birthday(null));
        assertThrows(IllegalArgumentException.class, () -> new Birthday("2004-02-30"));
    }

    @Test
    public void isValidBirthday() {
        assertThrows(NullPointerException.class, () -> Birthday.isValidBirthday(null));

        assertFalse(Birthday.isValidBirthday(""));
        assertFalse(Birthday.isValidBirthday("2004-6-18"));
        assertFalse(Birthday.isValidBirthday("18-06-2004"));
        assertFalse(Birthday.isValidBirthday("1900-02-29"));
        assertFalse(Birthday.isValidBirthday("0000-01-01"));
        assertFalse(Birthday.isValidBirthday(LocalDate.now().plusDays(10).toString()));

        assertTrue(Birthday.isValidBirthday("2004-02-29"));
        assertTrue(Birthday.isValidBirthday("2004-06-18"));
        assertTrue(Birthday.isValidBirthday(LocalDate.now().minusDays(1).toString()));
    }

    @Test
    public void equals_sameValue_returnsTrue() {
        Birthday birthday = new Birthday("2004-06-18");
        assertEquals("2004-06-18", birthday.getValue());
        assertEquals(birthday, new Birthday("2004-06-18"));
        assertNotEquals(birthday, new Birthday("2004-06-19"));
    }
}
