package coordimate.commons.core.index;

import static coordimate.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class IndexTest {

    @Test
    public void createOneBasedIndex() {
        // invalid index
        assertThrows(IndexOutOfBoundsException.class, () -> Index.createFromOneBased(0));

        // check equality using the same base
        assertEquals(1, Index.createFromOneBased(1).getOneBased());
        assertEquals(5, Index.createFromOneBased(5).getOneBased());

        // convert from one-based index to zero-based index
        assertEquals(0, Index.createFromOneBased(1).getZeroBased());
        assertEquals(4, Index.createFromOneBased(5).getZeroBased());
    }

    @Test
    public void createZeroBasedIndex() {
        // invalid index
        assertThrows(IndexOutOfBoundsException.class, () -> Index.createFromZeroBased(-1));

        // check equality using the same base
        assertEquals(0, Index.createFromZeroBased(0).getZeroBased());
        assertEquals(5, Index.createFromZeroBased(5).getZeroBased());

        // convert from zero-based index to one-based index
        assertEquals(1, Index.createFromZeroBased(0).getOneBased());
        assertEquals(6, Index.createFromZeroBased(5).getOneBased());
    }

    @Test
    public void checkEquals() {
        final Index fifthPersonIndex = Index.createFromOneBased(5);

        // same values -> returns true
        assertTrue(fifthPersonIndex.equals(Index.createFromOneBased(5)));
        assertTrue(fifthPersonIndex.equals(Index.createFromZeroBased(4)));

        // same object -> returns true
        assertTrue(fifthPersonIndex.equals(fifthPersonIndex));

        // null -> returns false
        assertFalse(fifthPersonIndex.equals(null));

        // different types -> returns false
        assertFalse(fifthPersonIndex.equals(5.0f));

        // different index -> returns false
        assertFalse(fifthPersonIndex.equals(Index.createFromOneBased(1)));
    }

    @Test
    public void checkToStringMethod() {
        Index index = Index.createFromZeroBased(0);
        String expected = Index.class.getCanonicalName() + "{zeroBasedIndex=" + index.getZeroBased() + "}";
        assertEquals(expected, index.toString());
    }
}
