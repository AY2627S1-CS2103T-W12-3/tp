package coordimate.model.person;

import static coordimate.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

public class FilterCriterionTest {

    @Test
    public void constructor_nullField_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new FilterCriterion(null, "friend"));
    }

    @Test
    public void constructor_nullValue_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new FilterCriterion(FilterField.TAG, null));
    }

    @Test
    public void equals() {
        FilterCriterion criterion = new FilterCriterion(FilterField.TAG, "friend");

        // same object -> returns true
        assertEquals(criterion, criterion);

        // same values -> returns true
        assertEquals(criterion, new FilterCriterion(FilterField.TAG, "friend"));

        // different field -> returns false
        assertNotEquals(criterion, new FilterCriterion(FilterField.EVENT, "friend"));

        // different value -> returns false
        assertNotEquals(criterion, new FilterCriterion(FilterField.TAG, "colleague"));

        // different type -> returns false
        assertNotEquals(criterion, "tag/friend");

        // null -> returns false
        assertNotEquals(criterion, null);
    }
}
