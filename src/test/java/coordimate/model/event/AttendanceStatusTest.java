package coordimate.model.event;

import static coordimate.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class AttendanceStatusTest {

    @Test
    public void fromString_validValues_returnsMatchingStatus() {
        assertEquals(AttendanceStatus.PRESENT, AttendanceStatus.fromString("present"));
        assertEquals(AttendanceStatus.PRESENT, AttendanceStatus.fromString("PRESENT"));
        assertEquals(AttendanceStatus.PRESENT, AttendanceStatus.fromString("Present"));
        assertEquals(AttendanceStatus.ABSENT, AttendanceStatus.fromString("absent"));
        assertEquals(AttendanceStatus.ABSENT, AttendanceStatus.fromString("ABSENT"));
    }

    @Test
    public void fromString_invalidValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> AttendanceStatus.fromString("here"));
        assertThrows(IllegalArgumentException.class, () -> AttendanceStatus.fromString(""));
    }

    @Test
    public void toStringMethod() {
        assertEquals("present", AttendanceStatus.PRESENT.toString());
        assertEquals("absent", AttendanceStatus.ABSENT.toString());
    }
}
