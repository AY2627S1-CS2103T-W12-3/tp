package coordimate.model.person;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class PrintableTextTest {

    @Test
    public void isValid_invisibleOnlyText_returnsFalse() {
        assertFalse(PrintableText.isValid("\uD800", 10)); // unpaired surrogate
        assertFalse(PrintableText.isValid("\u200B", 10)); // zero-width space
        assertFalse(PrintableText.isValid("\u2060", 10)); // word joiner
        assertFalse(PrintableText.isValid("\u00A0", 10)); // non-breaking space
        assertFalse(PrintableText.isValid("\u0301", 10)); // combining acute accent
        assertFalse(PrintableText.isValid("\u0903", 10)); // combining spacing mark
        assertFalse(PrintableText.isValid("\u20DD", 10)); // enclosing circle
    }

    @Test
    public void isValid_textWithVisibleCharacter_returnsTrue() {
        assertTrue(PrintableText.isValid("Concert", 10));
        assertTrue(PrintableText.isValid("\u200BConcert", 10));
        assertTrue(PrintableText.isValid("Cafe\u0301", 10));
        assertTrue(PrintableText.isValid("👩‍💻", 10));
    }
}
