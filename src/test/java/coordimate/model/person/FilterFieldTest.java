package coordimate.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

public class FilterFieldTest {

    @Test
    public void findByFieldName_validNames_returnsMatchingField() {
        assertEquals(Optional.of(FilterField.TAG), FilterField.findByFieldName("tag"));
        assertEquals(Optional.of(FilterField.TAG), FilterField.findByFieldName("TAG"));
        assertEquals(Optional.of(FilterField.TAG), FilterField.findByFieldName("Tag"));
        assertEquals(Optional.of(FilterField.EVENT), FilterField.findByFieldName("event"));
        assertEquals(Optional.of(FilterField.EVENT), FilterField.findByFieldName("EVENT"));
        assertEquals(Optional.of(FilterField.ORGANISATION), FilterField.findByFieldName("organisation"));
        assertEquals(Optional.of(FilterField.ORGANISATION), FilterField.findByFieldName("Organisation"));
    }

    @Test
    public void findByFieldName_unknownName_returnsEmpty() {
        assertTrue(FilterField.findByFieldName("favourite").isEmpty());
        assertTrue(FilterField.findByFieldName("").isEmpty());
        assertTrue(FilterField.findByFieldName("unknown").isEmpty());
    }
}
