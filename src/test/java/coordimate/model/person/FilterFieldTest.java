package coordimate.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

public class FilterFieldTest {

    @Test
    public void fromFieldName_validNames_returnsMatchingField() {
        assertEquals(Optional.of(FilterField.TAG), FilterField.fromFieldName("tag"));
        assertEquals(Optional.of(FilterField.TAG), FilterField.fromFieldName("TAG"));
        assertEquals(Optional.of(FilterField.TAG), FilterField.fromFieldName("Tag"));
        assertEquals(Optional.of(FilterField.EVENT), FilterField.fromFieldName("event"));
        assertEquals(Optional.of(FilterField.EVENT), FilterField.fromFieldName("EVENT"));
    }

    @Test
    public void fromFieldName_unknownName_returnsEmpty() {
        assertTrue(FilterField.fromFieldName("organisation").isEmpty());
        assertTrue(FilterField.fromFieldName("favourite").isEmpty());
        assertTrue(FilterField.fromFieldName("").isEmpty());
        assertTrue(FilterField.fromFieldName("unknown").isEmpty());
    }
}
