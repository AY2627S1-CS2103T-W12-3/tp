package coordimate.model.person;

import static coordimate.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class OrganisationTest {

    @Test
    public void constructor_invalidOrganisation_throwsIllegalArgumentException() {
        assertThrows(NullPointerException.class, () -> new Organisation(null));
        assertThrows(IllegalArgumentException.class, () -> new Organisation(""));
    }

    @Test
    public void isValidOrganisation() {
        assertThrows(NullPointerException.class, () -> Organisation.isValidOrganisation(null));

        assertFalse(Organisation.isValidOrganisation(""));
        assertFalse(Organisation.isValidOrganisation(" "));
        assertFalse(Organisation.isValidOrganisation("&"));
        assertFalse(Organisation.isValidOrganisation("NUS/CCA"));
        assertFalse(Organisation.isValidOrganisation("NUS\n"));
        assertFalse(Organisation.isValidOrganisation("A".repeat(81)));

        assertTrue(Organisation.isValidOrganisation("A"));
        assertTrue(Organisation.isValidOrganisation("A".repeat(80)));
        assertTrue(Organisation.isValidOrganisation("NUS Student Affairs"));
        assertTrue(Organisation.isValidOrganisation("Arts & Culture-2"));
    }

    @Test
    public void constructor_surroundingSpaces_trimmed() {
        assertEquals("NUS Student Affairs", new Organisation("  NUS Student Affairs  ").getValue());
    }

    @Test
    public void equals_sameValue_returnsTrue() {
        assertEquals(new Organisation("NUS"), new Organisation("NUS"));
        assertNotEquals(new Organisation("NUS"), new Organisation("NTU"));
    }
}
