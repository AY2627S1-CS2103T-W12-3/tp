package coordimate.model.person;

import static coordimate.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Test;

public class BirthdayTest {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-uuuu");

    @Test
    public void constructor_invalidBirthday_throwsIllegalArgumentException() {
        assertThrows(NullPointerException.class, () -> new Birthday(null));
        assertThrows(IllegalArgumentException.class, () -> new Birthday("30-02-2004"));
    }

    @Test
    public void isValidBirthday() {
        assertThrows(NullPointerException.class, () -> Birthday.isValidBirthday(null));

        assertFalse(Birthday.isValidBirthday(""));
        assertFalse(Birthday.isValidBirthday("18-6-2004"));
        assertFalse(Birthday.isValidBirthday("2004-06-18"));
        assertFalse(Birthday.isValidBirthday("29-02-1900"));
        assertFalse(Birthday.isValidBirthday("01-01-0000"));
        assertFalse(Birthday.isValidBirthday(LocalDate.now().plusDays(10).format(DATE_FORMAT)));

        assertTrue(Birthday.isValidBirthday("29-02-2004"));
        assertTrue(Birthday.isValidBirthday("18-06-2004"));
        assertTrue(Birthday.isValidBirthday(LocalDate.now().minusDays(1).format(DATE_FORMAT)));
    }

    @Test
    public void equals_sameValue_returnsTrue() {
        Birthday birthday = new Birthday("18-06-2004");
        assertEquals("18-06-2004", birthday.getValue());
        assertEquals(birthday, new Birthday("18-06-2004"));
        assertNotEquals(birthday, new Birthday("19-06-2004"));
    }
}
