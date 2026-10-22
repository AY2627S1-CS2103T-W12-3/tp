package coordimate.model.person;

import static coordimate.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

public class LeaveDateTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    @Test
    public void constructor_invalidLeaveDate_throwsException() {
        assertThrows(NullPointerException.class, () -> new LeaveDate(null));
        assertThrows(IllegalArgumentException.class, () -> new LeaveDate("31-11-2026"));
        assertThrows(IllegalArgumentException.class, () -> new LeaveDate("2026-11-30"));
    }

    @Test
    public void constructor_pastDate_accepted() {
        assertEquals(LocalDate.of(2020, 1, 1), new LeaveDate("01-01-2020").getDate());
    }

    @Test
    public void isValidLeaveDate() {
        assertThrows(NullPointerException.class, () -> LeaveDate.isValidLeaveDate(null));

        assertFalse(LeaveDate.isValidLeaveDate(""));
        assertFalse(LeaveDate.isValidLeaveDate("30-11-26"));
        assertFalse(LeaveDate.isValidLeaveDate("1-11-2026"));
        assertFalse(LeaveDate.isValidLeaveDate("30/11/2026"));
        assertFalse(LeaveDate.isValidLeaveDate("29-02-2027"));
        assertFalse(LeaveDate.isValidLeaveDate("01-01-0000"));
        assertFalse(LeaveDate.isValidLeaveDate(" 30-11-2026"));

        assertTrue(LeaveDate.isValidLeaveDate("29-02-2028"));
        assertTrue(LeaveDate.isValidLeaveDate("30-11-2026"));
        assertTrue(LeaveDate.isValidLeaveDate("01-01-2020"));
    }

    @Test
    public void isValidInputLeaveDate_rejectsDatesBeforeToday() {
        assertThrows(NullPointerException.class, () -> LeaveDate.isValidInputLeaveDate(null, TODAY));
        assertThrows(NullPointerException.class, () -> LeaveDate.isValidInputLeaveDate("30-11-2026", null));

        assertFalse(LeaveDate.isValidInputLeaveDate("19-10-2026", TODAY));
        assertFalse(LeaveDate.isValidInputLeaveDate("31-11-2026", TODAY));

        assertTrue(LeaveDate.isValidInputLeaveDate("20-10-2026", TODAY));
        assertTrue(LeaveDate.isValidInputLeaveDate("21-10-2026", TODAY));
    }

    @Test
    public void covers_includesLastDayOnly() {
        LeaveDate leaveDate = new LeaveDate("20-10-2026");

        assertThrows(NullPointerException.class, () -> leaveDate.covers(null));
        assertTrue(leaveDate.covers(TODAY.minusDays(30)));
        assertTrue(leaveDate.covers(TODAY));
        assertFalse(leaveDate.covers(TODAY.plusDays(1)));
    }

    @Test
    public void equals() {
        LeaveDate leaveDate = new LeaveDate("30-11-2026");

        assertEquals("30-11-2026", leaveDate.getValue());
        assertEquals("30-11-2026", leaveDate.toString());
        assertEquals(leaveDate, leaveDate);
        assertEquals(leaveDate, new LeaveDate("30-11-2026"));
        assertEquals(leaveDate.hashCode(), new LeaveDate("30-11-2026").hashCode());
        assertNotEquals(leaveDate, new LeaveDate("01-12-2026"));
        assertNotEquals(leaveDate, "30-11-2026");
        assertNotEquals(leaveDate, null);
    }
}
