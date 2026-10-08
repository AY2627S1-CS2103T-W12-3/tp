package coordimate.model.person;

import static coordimate.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class NoteTest {

    @Test
    public void constructor_invalidNote_throwsIllegalArgumentException() {
        assertThrows(NullPointerException.class, () -> new Note(null));
        assertThrows(IllegalArgumentException.class, () -> new Note(" "));
    }

    @Test
    public void isValidNote() {
        assertThrows(NullPointerException.class, () -> Note.isValidNote(null));

        assertFalse(Note.isValidNote(""));
        assertFalse(Note.isValidNote(" "));
        assertFalse(Note.isValidNote("N".repeat(501)));
        assertFalse(Note.isValidNote("Call\tAlice"));
        assertFalse(Note.isValidNote("Call\r\nAlice"));
        assertFalse(Note.isValidNote("Call\u2029Alice"));

        assertTrue(Note.isValidNote("N".repeat(500)));
        assertTrue(Note.isValidNote("🎵".repeat(500))); // supplementary Unicode counts as one code point
        assertFalse(Note.isValidNote("🎵".repeat(501)));
        assertTrue(Note.isValidNote("Handles bookings, invoices & follow-ups."));
    }

    @Test
    public void equals_sameValue_returnsTrue() {
        Note note = new Note("Handles venue bookings");
        assertEquals("Handles venue bookings", note.getValue());
        assertEquals(note, new Note("Handles venue bookings"));
        assertNotEquals(note, new Note("Handles transport"));
    }
}
