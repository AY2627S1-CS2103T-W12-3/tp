package coordimate.model.tag;

import static coordimate.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

public class TagTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Tag(null));
    }

    @Test
    public void alphabeticalOrder_ignoresCaseThenBreaksTiesByCase() {
        List<Tag> tags = List.of(new Tag("friends"), new Tag("Logistics"), new Tag("EXCO"), new Tag("budget"),
                new Tag("Budget"));
        assertEquals(List.of(new Tag("Budget"), new Tag("budget"), new Tag("EXCO"), new Tag("friends"),
                new Tag("Logistics")), tags.stream().sorted(Tag.ALPHABETICAL_ORDER).toList());
    }

    @Test
    public void constructor_invalidTagName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new Tag(""));
        assertThrows(IllegalArgumentException.class, () -> new Tag("Publicity Team"));
    }

    @Test
    public void isValidTagName() {
        // null tag name
        assertThrows(NullPointerException.class, () -> Tag.isValidTagName(null));

        assertFalse(Tag.isValidTagName(""));
        assertFalse(Tag.isValidTagName("Publicity Team"));
        // valid tag names
        assertTrue(Tag.isValidTagName("Publicity"));
        assertTrue(Tag.isValidTagName("123456789012345678901234567890"));
        assertTrue(Tag.isValidTagName("1234567890123456789012345678901"));
    }

    @Test
    public void isValidInputTagName_enforcesCurrentLengthLimit() {
        assertThrows(NullPointerException.class, () -> Tag.isValidInputTagName(null));

        assertFalse(Tag.isValidInputTagName(""));
        assertFalse(Tag.isValidInputTagName("Publicity Team"));
        assertFalse(Tag.isValidInputTagName("1234567890123456789012345678901"));
        assertFalse(Tag.isValidInputTagName("Café")); // letters must be ASCII, so length() counts characters

        assertTrue(Tag.isValidInputTagName("Publicity"));
        assertTrue(Tag.isValidInputTagName("123456789012345678901234567890"));
    }

    @Test
    public void messageLengthConstraints_statesLimit() {
        assertTrue(Tag.MESSAGE_LENGTH_CONSTRAINTS.contains(String.valueOf(Tag.MAX_LENGTH)));
    }

    @Test
    public void constructor_legacyLongTag_retainsName() {
        String legacyTagName = "1234567890123456789012345678901";
        assertEquals(legacyTagName, new Tag(legacyTagName).getTagName());
    }

    @Test
    public void isSameTag() {
        Tag tag = new Tag("EXCO");

        assertTrue(tag.isSameTag(tag));
        assertTrue(tag.isSameTag(new Tag("exco")));
        assertFalse(tag.isSameTag(new Tag("Sponsor")));
        assertFalse(tag.isSameTag(null));
    }

    @Test
    public void findSavedTag() {
        List<Tag> savedTags = List.of(new Tag("EXCO"), new Tag("Sponsor"));

        // saved tag typed in different case -> saved tag
        assertEquals(new Tag("EXCO"), new Tag("exco").findSavedTag(savedTags));

        // no saved match -> typed tag
        Tag newTag = new Tag("publicity");
        assertSame(newTag, newTag.findSavedTag(savedTags));
        assertSame(newTag, newTag.findSavedTag(List.of()));

        assertThrows(NullPointerException.class, () -> newTag.findSavedTag(null));
    }

}
