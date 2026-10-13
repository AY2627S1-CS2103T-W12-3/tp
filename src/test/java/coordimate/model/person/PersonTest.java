package coordimate.model.person;

import static coordimate.logic.commands.CommandTestUtil.VALID_ADDRESS_BOB;
import static coordimate.logic.commands.CommandTestUtil.VALID_EMAIL_BOB;
import static coordimate.logic.commands.CommandTestUtil.VALID_NAME_BOB;
import static coordimate.logic.commands.CommandTestUtil.VALID_PHONE_BOB;
import static coordimate.logic.commands.CommandTestUtil.VALID_TAG_HUSBAND;
import static coordimate.testutil.Assert.assertThrows;
import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BOB;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import coordimate.testutil.PersonBuilder;

public class PersonTest {

    @Test
    public void getObservableList_modifyList_throwsUnsupportedOperationException() {
        Person person = new PersonBuilder().build();
        assertThrows(UnsupportedOperationException.class, () -> person.getTags().remove(0));
    }

    @Test
    public void handleOptionalDetails_presentAndAbsent() {
        assertTrue(ALICE.getBirthday().isEmpty());
        assertTrue(ALICE.getAddress().isPresent());
        assertTrue(ALICE.getOrganisation().isEmpty());
        assertTrue(ALICE.getNote().isEmpty());

        assertTrue(new PersonBuilder(ALICE).clearAddress().build().getAddress().isEmpty());

        Person contact = new PersonBuilder(ALICE).setBirthday("18-06-2004")
                .setOrganisation("NUS Student Affairs").setNote("Handles venue bookings").build();
        assertEquals("18-06-2004", contact.getBirthday().orElseThrow().getValue());
        assertEquals("NUS Student Affairs", contact.getOrganisation().orElseThrow().getValue());
        assertEquals("Handles venue bookings", contact.getNote().orElseThrow().getValue());
    }

    @Test
    public void isSamePerson() {
        // same object -> returns true
        assertTrue(ALICE.isSamePerson(ALICE));

        // null -> returns false
        assertFalse(ALICE.isSamePerson(null));

        // same name, different phone and email -> returns true
        Person editedAlice = new PersonBuilder(ALICE).setPhone(VALID_PHONE_BOB).setEmail(VALID_EMAIL_BOB)
                .setAddress(VALID_ADDRESS_BOB).setTags(VALID_TAG_HUSBAND).build();
        assertTrue(ALICE.isSamePerson(editedAlice));

        // name matching ignores case and surrounding spaces
        editedAlice = new PersonBuilder(ALICE).setName("  aLiCe pAuLiNe  ")
                .setPhone(VALID_PHONE_BOB).setEmail(VALID_EMAIL_BOB).build();
        assertTrue(ALICE.isSamePerson(editedAlice));

        // different name, same phone and email -> returns true
        editedAlice = new PersonBuilder(ALICE).setName(VALID_NAME_BOB).build();
        assertTrue(ALICE.isSamePerson(editedAlice));

        // a formatted version of the same phone is a duplicate even with a different name and email
        editedAlice = new PersonBuilder(ALICE).setName(VALID_NAME_BOB)
                .setPhone("(9435) 1253").setEmail(VALID_EMAIL_BOB).build();
        assertTrue(ALICE.isSamePerson(editedAlice));

        // email comparison ignores case even with a different name and phone
        editedAlice = new PersonBuilder(ALICE).setName(VALID_NAME_BOB)
                .setPhone(VALID_PHONE_BOB).setEmail("ALICE@EXAMPLE.COM").build();
        assertTrue(ALICE.isSamePerson(editedAlice));

        // changing the role does not create a new contact identity
        editedAlice = new PersonBuilder(ALICE).setRole("Logistics").build();
        assertTrue(ALICE.isSamePerson(editedAlice));

        // different name, phone, and email -> returns false
        assertFalse(ALICE.isSamePerson(BOB));
    }

    @Test
    public void checkEquals() {
        // same values -> returns true
        Person aliceCopy = new PersonBuilder(ALICE).build();
        assertTrue(ALICE.equals(aliceCopy));

        // same object -> returns true
        assertTrue(ALICE.equals(ALICE));

        // null -> returns false
        assertFalse(ALICE.equals(null));

        // different type -> returns false
        assertFalse(ALICE.equals(5));

        // different person -> returns false
        assertFalse(ALICE.equals(BOB));

        // different name -> returns false
        Person editedAlice = new PersonBuilder(ALICE).setName(VALID_NAME_BOB).build();
        assertFalse(ALICE.equals(editedAlice));

        // different phone -> returns false
        editedAlice = new PersonBuilder(ALICE).setPhone(VALID_PHONE_BOB).build();
        assertFalse(ALICE.equals(editedAlice));

        // different email -> returns false
        editedAlice = new PersonBuilder(ALICE).setEmail(VALID_EMAIL_BOB).build();
        assertFalse(ALICE.equals(editedAlice));

        // different role -> returns false
        editedAlice = new PersonBuilder(ALICE).setRole("Logistics").build();
        assertFalse(ALICE.equals(editedAlice));

        // different optional details -> returns false
        assertFalse(ALICE.equals(new PersonBuilder(ALICE).setBirthday("18-06-2004").build()));
        assertFalse(ALICE.equals(new PersonBuilder(ALICE).setOrganisation("NUS Student Affairs").build()));
        assertFalse(ALICE.equals(new PersonBuilder(ALICE).setNote("Handles venue bookings").build()));

        // different address -> returns false
        editedAlice = new PersonBuilder(ALICE).setAddress(VALID_ADDRESS_BOB).build();
        assertFalse(ALICE.equals(editedAlice));
        assertFalse(ALICE.equals(new PersonBuilder(ALICE).clearAddress().build()));

        // different tags -> returns false
        editedAlice = new PersonBuilder(ALICE).setTags(VALID_TAG_HUSBAND).build();
        assertFalse(ALICE.equals(editedAlice));
    }

    @Test
    public void checkToStringMethod() {
        String expected = Person.class.getCanonicalName() + "{name=" + ALICE.getName() + ", phone=" + ALICE.getPhone()
                + ", email=" + ALICE.getEmail() + ", role=" + ALICE.getRole() + ", birthday=null, address="
                + ALICE.getAddress().orElseThrow() + ", organisation=null, note=null, tags=" + ALICE.getTags() + "}";
        assertEquals(expected, ALICE.toString());
    }
}
