package coordimate.model.person;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class PrintableTextTest {

    // Every character listed on https://invisible-characters.com/ that renders as blank space.
    private static final int[] INVISIBLE_CODE_POINTS = {
        0x0009, 0x000A, 0x000B, 0x000C, 0x000D, 0x0020, 0x00A0, 0x00AD, 0x034F, 0x061C,
        0x115F, 0x1160, 0x1680, 0x17B4, 0x17B5, 0x180B, 0x180C, 0x180D, 0x180E,
        0x2000, 0x2001, 0x2002, 0x2003, 0x2004, 0x2005, 0x2006, 0x2007, 0x2008, 0x2009, 0x200A,
        0x200B, 0x200C, 0x200D, 0x200E, 0x200F, 0x202A, 0x202B, 0x202C, 0x202D, 0x202E, 0x202F,
        0x205F, 0x2060, 0x2061, 0x2062, 0x2063, 0x2064, 0x2065, 0x2066, 0x2067, 0x2068, 0x2069,
        0x206A, 0x206B, 0x206C, 0x206D, 0x206E, 0x206F, 0x2800, 0x3000, 0x3164,
        0xFEFF, 0xFFA0, 0xFFF9, 0xFFFA, 0xFFFB, 0xFFFC,
        0x1D159, 0x1D173, 0x1D174, 0x1D175, 0x1D176, 0x1D177, 0x1D178, 0x1D179, 0x1D17A
    };

    // Also listed on that site, but these are real symbols that only look blank in fonts without a glyph for them.
    private static final int[] SYMBOLS_WITH_GLYPHS = {
        0x133FC, 0x1D000, 0x1D0F0, 0x1D100, 0x1D129, 0x1D130, 0x1D13F, 0x1D140, 0x1D145, 0x1D150
    };

    @Test
    public void isValid_invisibleOnlyText_returnsFalse() {
        assertFalse(PrintableText.isValid("", 10));
        assertFalse(PrintableText.isValid("\uD800", 10)); // unpaired surrogate
        assertFalse(PrintableText.isValid("́", 10)); // combining acute accent
        assertFalse(PrintableText.isValid("ः", 10)); // combining spacing mark
        assertFalse(PrintableText.isValid("⃝", 10)); // enclosing circle
        assertFalse(PrintableText.isValid("", 10)); // private use
        for (int codePoint : INVISIBLE_CODE_POINTS) {
            String text = Character.toString(codePoint);
            assertFalse(PrintableText.isValid(text, 10), String.format("U+%04X", codePoint));
            assertFalse(PrintableText.isValid(text.repeat(3), 10), String.format("U+%04X repeated", codePoint));
        }
    }

    @Test
    public void isValid_textWithVisibleCharacter_returnsTrue() {
        assertTrue(PrintableText.isValid("Concert", 10));
        assertTrue(PrintableText.isValid("​Concert", 10));
        assertTrue(PrintableText.isValid("ㅤConcert", 10));
        assertTrue(PrintableText.isValid("Café", 10));
        assertTrue(PrintableText.isValid("👩‍💻", 10));
        for (int codePoint : SYMBOLS_WITH_GLYPHS) {
            assertTrue(PrintableText.isValid(Character.toString(codePoint), 10), String.format("U+%04X", codePoint));
        }
    }
}
