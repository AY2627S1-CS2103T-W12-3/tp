package coordimate.model.person;

import static coordimate.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class RoleTest {

    @Test
    public void constructor_invalidRole_throwsIllegalArgumentException() {
        assertThrows(NullPointerException.class, () -> new Role(null));
        assertThrows(IllegalArgumentException.class, () -> new Role(" "));
    }

    @Test
    public void isValidRole() {
        assertThrows(NullPointerException.class, () -> Role.isValidRole(null));

        assertFalse(Role.isValidRole(""));
        assertFalse(Role.isValidRole(" "));
        assertFalse(Role.isValidRole("/"));
        assertFalse(Role.isValidRole("Lead & Advisor"));
        assertFalse(Role.isValidRole("\tLogistics"));
        assertFalse(Role.isValidRole("A".repeat(51)));

        assertTrue(Role.isValidRole("A"));
        assertTrue(Role.isValidRole("A".repeat(50)));
        assertTrue(Role.isValidRole("Logistics Lead/2"));
        assertTrue(Role.isValidRole("Vice-President"));
    }

    @Test
    public void constructor_surroundingSpaces_trimmed() {
        assertEquals("Logistics Lead", new Role("  Logistics Lead  ").getValue());
    }

    @Test
    public void equals_sameValue_returnsTrue() {
        assertEquals(new Role("Advisor"), new Role("Advisor"));
        assertNotEquals(new Role("Advisor"), new Role("President"));
    }
}
