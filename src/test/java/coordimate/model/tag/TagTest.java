package coordimate.model.tag;

import static coordimate.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TagTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Tag(null));
    }

    @Test
    public void constructor_invalidTagName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new Tag(""));
        assertThrows(IllegalArgumentException.class, () -> new Tag("Publicity Team"));
        assertThrows(IllegalArgumentException.class, () -> new Tag("1234567890123456789012345678901"));
    }

    @Test
    public void isValidTagName() {
        // null tag name
        assertThrows(NullPointerException.class, () -> Tag.isValidTagName(null));

        assertFalse(Tag.isValidTagName(""));
        assertFalse(Tag.isValidTagName("Publicity Team"));
        assertFalse(Tag.isValidTagName("1234567890123456789012345678901"));
        assertTrue(Tag.isValidTagName("Publicity"));
        assertTrue(Tag.isValidTagName("123456789012345678901234567890"));
    }

    @Test
    public void isSameTag() {
        Tag tag = new Tag("EXCO");

        assertTrue(tag.isSameTag(tag));
        assertTrue(tag.isSameTag(new Tag("exco")));
        assertFalse(tag.isSameTag(new Tag("Sponsor")));
        assertFalse(tag.isSameTag(null));
    }

}
