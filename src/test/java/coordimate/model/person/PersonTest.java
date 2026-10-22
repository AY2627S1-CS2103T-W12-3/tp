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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import coordimate.testutil.PersonBuilder;

public class PersonTest {

    @Test
    public void withMemberId_assignsIdWithoutChangingDraft() {
        Person draft = new PersonBuilder(ALICE).withoutMemberId().build();
        Person member = draft.withMemberId(new MemberId(12));
        assertTrue(draft.getMemberId().isEmpty());
        assertEquals(new MemberId(12), member.getMemberId().orElseThrow());
        assertTrue(draft.isSamePerson(member));
        assertNotEquals(draft, member);
        assertThrows(NullPointerException.class, () -> draft.withMemberId(null));
    }

    @Test
    public void withMemberId_existingId_cannotBeChanged() {
        Person member = new PersonBuilder(ALICE).withMemberId(12).build();
        assertSame(member, member.withMemberId(new MemberId(12)));
        assertThrows(IllegalArgumentException.class, () -> member.withMemberId(new MemberId(13)));
    }

    @Test
    public void equalsAndHashCode_includeMemberId() {
        Person member = new PersonBuilder(ALICE).withMemberId(12).build();
        Person copy = new PersonBuilder(member).build();
        assertEquals(member, copy);
        assertEquals(member.hashCode(), copy.hashCode());
        assertNotEquals(member, new PersonBuilder(member).withMemberId(13).build());
        assertTrue(member.isSamePerson(new PersonBuilder(member).withMemberId(13).build()));
    }

    @Test
    public void asObservableList_modifyList_throwsUnsupportedOperationException() {
        Person person = new PersonBuilder().build();
        assertThrows(UnsupportedOperationException.class, () -> person.getTags().remove(0));
    }

    @Test
    public void optionalDetails_presentAndAbsent() {
        assertTrue(ALICE.getBirthday().isEmpty());
        assertTrue(ALICE.getAddress().isPresent());
        assertTrue(ALICE.getOrganisation().isEmpty());
        assertTrue(ALICE.getNote().isEmpty());

        assertTrue(new PersonBuilder(ALICE).withoutAddress().build().getAddress().isEmpty());

        Person contact = new PersonBuilder(ALICE).withBirthday("18-06-2004")
                .withOrganisation("NUS Student Affairs").withNote("Handles venue bookings").build();
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
        Person editedAlice = new PersonBuilder(ALICE).withPhone(VALID_PHONE_BOB).withEmail(VALID_EMAIL_BOB)
                .withAddress(VALID_ADDRESS_BOB).withTags(VALID_TAG_HUSBAND).build();
        assertTrue(ALICE.isSamePerson(editedAlice));

        // name matching ignores case and surrounding spaces
        editedAlice = new PersonBuilder(ALICE).withName("  aLiCe pAuLiNe  ")
                .withPhone(VALID_PHONE_BOB).withEmail(VALID_EMAIL_BOB).build();
        assertTrue(ALICE.isSamePerson(editedAlice));

        // different name, same phone and email -> returns true
        editedAlice = new PersonBuilder(ALICE).withName(VALID_NAME_BOB).build();
        assertTrue(ALICE.isSamePerson(editedAlice));

        // a formatted version of the same phone is a duplicate even with a different name and email
        editedAlice = new PersonBuilder(ALICE).withName(VALID_NAME_BOB)
                .withPhone("(9435) 1253").withEmail(VALID_EMAIL_BOB).build();
        assertTrue(ALICE.isSamePerson(editedAlice));

        // email comparison ignores case even with a different name and phone
        editedAlice = new PersonBuilder(ALICE).withName(VALID_NAME_BOB)
                .withPhone(VALID_PHONE_BOB).withEmail("ALICE@EXAMPLE.COM").build();
        assertTrue(ALICE.isSamePerson(editedAlice));

        // changing the role does not create a new contact identity
        editedAlice = new PersonBuilder(ALICE).withRole("Logistics").build();
        assertTrue(ALICE.isSamePerson(editedAlice));

        // different name, phone, and email -> returns false
        assertFalse(ALICE.isSamePerson(BOB));
    }

    @Test
    public void equals() {
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
        Person editedAlice = new PersonBuilder(ALICE).withName(VALID_NAME_BOB).build();
        assertFalse(ALICE.equals(editedAlice));

        // different phone -> returns false
        editedAlice = new PersonBuilder(ALICE).withPhone(VALID_PHONE_BOB).build();
        assertFalse(ALICE.equals(editedAlice));

        // different email -> returns false
        editedAlice = new PersonBuilder(ALICE).withEmail(VALID_EMAIL_BOB).build();
        assertFalse(ALICE.equals(editedAlice));

        // different role -> returns false
        editedAlice = new PersonBuilder(ALICE).withRole("Logistics").build();
        assertFalse(ALICE.equals(editedAlice));

        // different optional details -> returns false
        assertFalse(ALICE.equals(new PersonBuilder(ALICE).withBirthday("18-06-2004").build()));
        assertFalse(ALICE.equals(new PersonBuilder(ALICE).withOrganisation("NUS Student Affairs").build()));
        assertFalse(ALICE.equals(new PersonBuilder(ALICE).withNote("Handles venue bookings").build()));

        // different address -> returns false
        editedAlice = new PersonBuilder(ALICE).withAddress(VALID_ADDRESS_BOB).build();
        assertFalse(ALICE.equals(editedAlice));
        assertFalse(ALICE.equals(new PersonBuilder(ALICE).withoutAddress().build()));

        // different tags -> returns false
        editedAlice = new PersonBuilder(ALICE).withTags(VALID_TAG_HUSBAND).build();
        assertFalse(ALICE.equals(editedAlice));
    }

    @Test
    public void isOnLeave_untilAndIncludingLastDay() {
        Person onLeave = new PersonBuilder(ALICE).withLeaveDate("30-11-2026").build();

        assertThrows(NullPointerException.class, () -> onLeave.isOnLeave(null));
        assertTrue(onLeave.isOnLeave(LocalDate.of(2026, 10, 20)));
        assertTrue(onLeave.isOnLeave(LocalDate.of(2026, 11, 30)));
        assertFalse(onLeave.isOnLeave(LocalDate.of(2026, 12, 1)));
        assertFalse(ALICE.isOnLeave(LocalDate.of(2026, 10, 20)));
    }

    @Test
    public void isOnLeave_endedLeave_keepsDateButNotOnLeave() {
        Person endedLeave = new PersonBuilder(ALICE).withLeaveDate("01-01-2020").build();

        assertFalse(endedLeave.isOnLeave(LocalDate.of(2026, 10, 20)));
        assertEquals("01-01-2020", endedLeave.getLeaveDate().orElseThrow().getValue());
    }

    @Test
    public void equals_differentLeaveDate_returnsFalse() {
        Person onLeave = new PersonBuilder(ALICE).withLeaveDate("30-11-2026").build();

        assertFalse(ALICE.equals(onLeave));
        assertFalse(onLeave.equals(new PersonBuilder(ALICE).withLeaveDate("01-12-2026").build()));
        assertTrue(onLeave.equals(new PersonBuilder(ALICE).withLeaveDate("30-11-2026").build()));
        assertEquals(onLeave.hashCode(), new PersonBuilder(ALICE).withLeaveDate("30-11-2026").build().hashCode());
    }

    @Test
    public void toStringMethod() {
        String expected = Person.class.getCanonicalName() + "{memberId=" + ALICE.getMemberId().orElseThrow()
                + ", name=" + ALICE.getName()
                + ", phone=" + ALICE.getPhone()
                + ", email=" + ALICE.getEmail() + ", role=" + ALICE.getRole() + ", birthday=null, address="
                + ALICE.getAddress().orElseThrow() + ", organisation=null, note=null, leaveDate=null, tags="
                + ALICE.getTags() + ", archived=false}";
        assertEquals(expected, ALICE.toString());
    }

    @Test
    public void withArchived_keepsDetailsAndChangesEquality() {
        Person archived = ALICE.withArchived(true);

        assertFalse(ALICE.isArchived());
        assertTrue(archived.isArchived());
        assertFalse(ALICE.equals(archived));
        assertNotEquals(ALICE.hashCode(), archived.hashCode());
        assertTrue(ALICE.isSamePerson(archived));
        assertEquals(ALICE, archived.withArchived(false));
        assertSame(ALICE, ALICE.withArchived(false));
        assertEquals(ALICE.getName(), archived.getName());
        assertEquals(ALICE.getTags(), archived.getTags());
        assertEquals(ALICE.getAddress(), archived.getAddress());
        assertTrue(archived.toString().endsWith("archived=true}"));
    }

    @Test
    public void withMemberId_archivedContactWithoutId_staysArchived() {
        Person withoutId = new Person(ALICE.getName(), ALICE.getPhone(), ALICE.getEmail(), ALICE.getRole(),
                ALICE.getAddress().orElse(null), ALICE.getTags()).withArchived(true);

        Person identified = withoutId.withMemberId(new MemberId(7));

        assertEquals(new MemberId(7), identified.getMemberId().orElseThrow());
        assertTrue(identified.isArchived());
        assertEquals(new MemberId(7), identified.withArchived(false).getMemberId().orElseThrow());
    }

    @Test
    public void constructor_archivedArgument_setsStatusThatCopiesKeep() {
        Person archived = new PersonBuilder(ALICE).withArchived(true).build();

        assertTrue(archived.isArchived());
        assertEquals(ALICE.withArchived(true), archived);
        assertTrue(new PersonBuilder(archived).build().isArchived());
        assertFalse(new PersonBuilder(archived).withArchived(false).build().isArchived());
    }
}
