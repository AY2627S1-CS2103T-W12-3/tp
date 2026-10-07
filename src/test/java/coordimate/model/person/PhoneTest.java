package coordimate.model.person;

import static coordimate.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class PhoneTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Phone(null));
    }

    @Test
    public void constructor_invalidPhone_throwsIllegalArgumentException() {
        String invalidPhone = "";
        assertThrows(IllegalArgumentException.class, () -> new Phone(invalidPhone));
    }

    @Test
    public void isValidPhone() {
        // null phone number
        assertThrows(NullPointerException.class, () -> Phone.isValidPhone(null));

        // invalid phone numbers
        assertFalse(Phone.isValidPhone("")); // empty string
        assertFalse(Phone.isValidPhone(" ")); // spaces only
        assertFalse(Phone.isValidPhone("123456")); // fewer than 7 digits
        assertFalse(Phone.isValidPhone("1234567890123456")); // more than 15 digits
        assertFalse(Phone.isValidPhone("phone")); // non-numeric
        assertFalse(Phone.isValidPhone("9011p041")); // alphabets within digits
        assertFalse(Phone.isValidPhone("9123+4567")); // plus sign is only allowed at the start
        assertFalse(Phone.isValidPhone("++6591234567")); // repeated plus sign
        assertFalse(Phone.isValidPhone("9123#4567")); // unsupported punctuation
        assertFalse(Phone.isValidPhone("\n91234567")); // control characters are not allowed

        // valid phone numbers
        assertTrue(Phone.isValidPhone("1234567")); // minimum digit count
        assertTrue(Phone.isValidPhone("93121534"));
        assertTrue(Phone.isValidPhone("124293842033123")); // maximum digit count
        assertTrue(Phone.isValidPhone("+65 (9123) 4567")); // country code, spaces and brackets
        assertTrue(Phone.isValidPhone("9123-4567")); // hyphen separator
    }

    @Test
    public void constructor_surroundingSpaces_trimmed() {
        assertEquals("91234567", new Phone(" 91234567 ").getValue());
    }

    @Test
    public void equals() {
        Phone phone = new Phone("9999999");

        // same values -> returns true
        assertTrue(phone.equals(new Phone("9999999")));

        // same object -> returns true
        assertTrue(phone.equals(phone));

        // null -> returns false
        assertFalse(phone.equals(null));

        // different types -> returns false
        assertFalse(phone.equals(5.0f));

        // different values -> returns false
        assertFalse(phone.equals(new Phone("9999998")));
    }
}
