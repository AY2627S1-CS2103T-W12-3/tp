package coordimate.model.person;

import static coordimate.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

public class MemberIdTest {
    @Test
    public void constructor_nonPositiveValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, MemberId.MESSAGE_CONSTRAINTS, () -> new MemberId(0));
        assertThrows(IllegalArgumentException.class, () -> new MemberId(-1));
        assertThrows(IllegalArgumentException.class, () -> new MemberId(Integer.MIN_VALUE));
    }

    @Test
    public void constructor_positiveValue_preservesValue() {
        assertEquals(1, new MemberId(1).getValue());
        assertEquals(Integer.MAX_VALUE, new MemberId(Integer.MAX_VALUE).getValue());
        assertEquals("12", new MemberId(12).toString());
    }

    @Test
    public void equalsAndHashCode_compareNumericValue() {
        MemberId id = new MemberId(12);
        assertEquals(id, id);
        assertEquals(id, new MemberId(12));
        assertEquals(id.hashCode(), new MemberId(12).hashCode());
        assertNotEquals(id, new MemberId(13));
        assertNotEquals(id, null);
        assertNotEquals(id, 12);
    }
}
