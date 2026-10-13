package coordimate.model.event;

import static coordimate.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class AttendanceStatusTest {

    @Test
    public void parseFromString_validValues_returnsMatchingStatus() {
        assertEquals(AttendanceStatus.PRESENT, AttendanceStatus.parseFromString("present"));
        assertEquals(AttendanceStatus.PRESENT, AttendanceStatus.parseFromString("PRESENT"));
        assertEquals(AttendanceStatus.PRESENT, AttendanceStatus.parseFromString("Present"));
        assertEquals(AttendanceStatus.ABSENT, AttendanceStatus.parseFromString("absent"));
        assertEquals(AttendanceStatus.ABSENT, AttendanceStatus.parseFromString("ABSENT"));
    }

    @Test
    public void parseFromString_invalidValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> AttendanceStatus.parseFromString("here"));
        assertThrows(IllegalArgumentException.class, () -> AttendanceStatus.parseFromString(""));
    }

    @Test
    public void checkToStringMethod() {
        assertEquals("present", AttendanceStatus.PRESENT.toString());
        assertEquals("absent", AttendanceStatus.ABSENT.toString());
    }
}
