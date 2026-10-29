package coordimate.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import coordimate.model.person.Person;
import coordimate.testutil.PersonBuilder;

public class MemberIdentityTest {

    @Test
    public void initials_singleWordName_returnsFirstLetter() {
        assertEquals("A", MemberIdentity.initials("Aisha"));
    }

    @Test
    public void initials_multipleWordName_returnsFirstAndLastWordLetters() {
        assertEquals("AT", MemberIdentity.initials("Aisha Binte Tan"));
    }

    @Test
    public void initials_validUnicodeName_returnsUppercasedLetters() {
        assertEquals("ŁĆ", MemberIdentity.initials("Łukasz Ćwik"));
    }

    @Test
    public void initials_extraWhitespace_isStrippedBeforeSplitting() {
        assertEquals("AB", MemberIdentity.initials("  Aisha   Bee  "));
    }

    @Test
    public void avatarColorClass_nullPerson_returnsPurple() {
        assertEquals("event-member-avatar-purple", MemberIdentity.avatarColorClass(null));
    }

    @Test
    public void avatarColorClass_logisticsTag_returnsTeal() {
        Person person = new PersonBuilder().withTags("Logistics").build();
        assertEquals("event-member-avatar-teal", MemberIdentity.avatarColorClass(person));
    }

    @Test
    public void avatarColorClass_sponsorTag_returnsAmber() {
        Person person = new PersonBuilder().withTags("Sponsor").build();
        assertEquals("event-member-avatar-amber", MemberIdentity.avatarColorClass(person));
    }

    @Test
    public void avatarColorClass_logisticsAndSponsorTags_prefersLogistics() {
        Person person = new PersonBuilder().withTags("Sponsor", "Logistics").build();
        assertEquals("event-member-avatar-teal", MemberIdentity.avatarColorClass(person));
    }

    @Test
    public void avatarColorClass_noMatchingTag_returnsPurple() {
        Person person = new PersonBuilder().withTags("Exco").build();
        assertEquals("event-member-avatar-purple", MemberIdentity.avatarColorClass(person));
    }

    @Test
    public void formatDetails_nullPerson_returnsUnavailableMessage() {
        assertEquals("Contact details unavailable", MemberIdentity.formatDetails(null));
    }

    @Test
    public void formatDetails_personWithoutOrganisation_returnsRoleOnly() {
        Person person = new PersonBuilder().withRole("Treasurer").withoutOrganisation().build();
        assertEquals("Treasurer", MemberIdentity.formatDetails(person));
    }

    @Test
    public void formatDetails_personWithOrganisation_returnsRoleAndOrganisation() {
        Person person = new PersonBuilder().withRole("Treasurer").withOrganisation("Student Union").build();
        assertEquals("Treasurer · Student Union", MemberIdentity.formatDetails(person));
    }
}
