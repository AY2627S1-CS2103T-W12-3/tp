package coordimate.storage;

import static coordimate.storage.JsonAdaptedPerson.MISSING_FIELD_MESSAGE_FORMAT;
import static coordimate.testutil.Assert.assertThrows;
import static coordimate.testutil.TypicalPersons.BENSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import coordimate.commons.exceptions.IllegalValueException;
import coordimate.model.person.Address;
import coordimate.model.person.Birthday;
import coordimate.model.person.Email;
import coordimate.model.person.Name;
import coordimate.model.person.Note;
import coordimate.model.person.Organisation;
import coordimate.model.person.Person;
import coordimate.model.person.Phone;
import coordimate.model.person.Role;
import coordimate.model.tag.Tag;
import coordimate.testutil.PersonBuilder;

public class JsonAdaptedPersonTest {
    private static final String INVALID_NAME = "R@chel";
    private static final String INVALID_PHONE = "+651234";
    private static final String INVALID_ADDRESS = " ";
    private static final String INVALID_EMAIL = "example.com";
    private static final String INVALID_TAG = "#friend";

    private static final String VALID_NAME = BENSON.getName().toString();
    private static final String VALID_PHONE = BENSON.getPhone().toString();
    private static final String VALID_EMAIL = BENSON.getEmail().toString();
    private static final String VALID_ADDRESS = BENSON.getAddress().orElseThrow().toString();
    private static final List<JsonAdaptedTag> VALID_TAGS = BENSON.getTags().stream()
            .map(JsonAdaptedTag::new)
            .collect(Collectors.toList());

    @Test
    public void toModelType_validPersonDetails_returnsPerson() throws Exception {
        JsonAdaptedPerson person = new JsonAdaptedPerson(BENSON);
        assertEquals(BENSON, person.toModelType());
    }

    @Test
    public void toModelType_personWithRole_preservesRole() throws Exception {
        Person contact = new PersonBuilder(BENSON).withRole("Logistics").build();
        JsonAdaptedPerson person = new JsonAdaptedPerson(contact);

        assertEquals(contact, person.toModelType());
    }

    @Test
    public void toModelType_personWithOptionalDetails_preservesDetails() throws Exception {
        Person contact = new PersonBuilder(BENSON).withBirthday("18-06-2004")
                .withOrganisation("NUS Student Affairs").withNote("Handles venue bookings").build();
        JsonAdaptedPerson person = new JsonAdaptedPerson(contact);

        assertEquals(contact, person.toModelType());
    }

    @Test
    public void toModelType_legacyPersonWithoutOptionalDetails_leavesDetailsUnset() throws Exception {
        JsonAdaptedPerson person = new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS,
                VALID_TAGS);

        assertEquals(new PersonBuilder(BENSON).withoutMemberId().build(), person.toModelType());
    }

    @Test
    public void toModelType_invalidBirthday_throwsIllegalValueException() {
        JsonAdaptedPerson person = new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS,
                "Logistics", "31-02-2004", null, null, VALID_TAGS);

        assertThrows(IllegalValueException.class, Birthday.MESSAGE_CONSTRAINTS, person::toModelType);
    }

    @Test
    public void toModelType_invalidOrganisation_throwsIllegalValueException() {
        JsonAdaptedPerson person = new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS,
                "Logistics", null, "NUS_Student Affairs", null, VALID_TAGS);

        assertThrows(IllegalValueException.class, Organisation.MESSAGE_CONSTRAINTS, person::toModelType);
    }

    @Test
    public void toModelType_invalidNote_throwsIllegalValueException() {
        JsonAdaptedPerson person = new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS,
                "Logistics", null, null, "Line one\nLine two", VALID_TAGS);

        assertThrows(IllegalValueException.class, Note.MESSAGE_CONSTRAINTS, person::toModelType);
    }

    @Test
    public void toModelType_legacyPersonWithoutRole_usesNa() throws Exception {
        JsonAdaptedPerson person = new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS,
                VALID_TAGS);

        assertEquals(Role.NOT_APPLICABLE, person.toModelType().getRole());
    }

    @Test
    public void toModelType_invalidRole_throwsIllegalValueException() {
        JsonAdaptedPerson person = new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS,
                "invalid_role", VALID_TAGS);

        assertThrows(IllegalValueException.class, Role.MESSAGE_CONSTRAINTS, person::toModelType);
    }

    @Test
    public void toModelType_emptyRole_throwsIllegalValueException() {
        JsonAdaptedPerson person = new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS,
                "", VALID_TAGS);

        assertThrows(IllegalValueException.class, Role.MESSAGE_CONSTRAINTS, person::toModelType);
    }

    @Test
    public void toModelType_invalidName_throwsIllegalValueException() {
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(INVALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
        String expectedMessage = Name.MESSAGE_CONSTRAINTS;
        assertThrows(IllegalValueException.class, expectedMessage, person::toModelType);
    }

    @Test
    public void toModelType_nullName_throwsIllegalValueException() {
        JsonAdaptedPerson person = new JsonAdaptedPerson(null, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
        String expectedMessage = String.format(MISSING_FIELD_MESSAGE_FORMAT, Name.class.getSimpleName());
        assertThrows(IllegalValueException.class, expectedMessage, person::toModelType);
    }

    @Test
    public void toModelType_invalidPhone_throwsIllegalValueException() {
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(VALID_NAME, INVALID_PHONE, VALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
        String expectedMessage = Phone.MESSAGE_CONSTRAINTS;
        assertThrows(IllegalValueException.class, expectedMessage, person::toModelType);
    }

    @Test
    public void toModelType_nullPhone_throwsIllegalValueException() {
        JsonAdaptedPerson person = new JsonAdaptedPerson(VALID_NAME, null, VALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
        String expectedMessage = String.format(MISSING_FIELD_MESSAGE_FORMAT, Phone.class.getSimpleName());
        assertThrows(IllegalValueException.class, expectedMessage, person::toModelType);
    }

    @Test
    public void toModelType_invalidEmail_throwsIllegalValueException() {
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, INVALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
        String expectedMessage = Email.MESSAGE_CONSTRAINTS;
        assertThrows(IllegalValueException.class, expectedMessage, person::toModelType);
    }

    @Test
    public void toModelType_nullEmail_throwsIllegalValueException() {
        JsonAdaptedPerson person = new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, null, VALID_ADDRESS, VALID_TAGS);
        String expectedMessage = String.format(MISSING_FIELD_MESSAGE_FORMAT, Email.class.getSimpleName());
        assertThrows(IllegalValueException.class, expectedMessage, person::toModelType);
    }

    @Test
    public void toModelType_invalidAddress_throwsIllegalValueException() {
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, INVALID_ADDRESS, VALID_TAGS);
        String expectedMessage = Address.MESSAGE_CONSTRAINTS;
        assertThrows(IllegalValueException.class, expectedMessage, person::toModelType);
    }

    @Test
    public void toModelType_nullAddress_leavesAddressUnset() throws Exception {
        JsonAdaptedPerson person = new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, null, VALID_TAGS);

        assertTrue(person.toModelType().getAddress().isEmpty());
    }

    @Test
    public void toModelType_invalidTags_throwsIllegalValueException() {
        List<JsonAdaptedTag> invalidTags = new ArrayList<>(VALID_TAGS);
        invalidTags.add(new JsonAdaptedTag(INVALID_TAG));
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, invalidTags);
        assertThrows(IllegalValueException.class, person::toModelType);
    }

    @Test
    public void toModelType_fieldsAtMaximumLength_returnsPerson() throws Exception {
        String maxName = "N".repeat(Name.MAX_LENGTH);
        String maxEmail = "e".repeat(Email.MAX_LOCAL_PART_LENGTH) + "@"
                + "d".repeat(Email.MAX_LENGTH - Email.MAX_LOCAL_PART_LENGTH - 5) + ".com";
        String maxTag = "T".repeat(Tag.MAX_LENGTH);
        JsonAdaptedPerson adapted = new JsonAdaptedPerson(maxName, VALID_PHONE, maxEmail,
                "A".repeat(Address.MAX_LENGTH), "R".repeat(Role.MAX_LENGTH), null,
                "O".repeat(Organisation.MAX_LENGTH), "M".repeat(Note.MAX_LENGTH), List.of(new JsonAdaptedTag(maxTag)));

        Person person = adapted.toModelType();
        assertEquals(maxName, person.getName().toString());
        assertEquals(maxEmail, person.getEmail().toString());
        assertEquals(Email.MAX_LENGTH, maxEmail.length());
        assertTrue(person.getTags().contains(new Tag(maxTag)));
    }

    @Test
    public void toModelType_fieldOverMaximumLength_throwsFieldConstraints() {
        String over = "X";
        String overEmail = "e".repeat(Email.MAX_LOCAL_PART_LENGTH + 1) + "@example.com";
        JsonAdaptedPerson[] people = {
            new JsonAdaptedPerson("N".repeat(Name.MAX_LENGTH) + over, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS,
                    VALID_TAGS),
            new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, overEmail, VALID_ADDRESS, VALID_TAGS),
            new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, "A".repeat(Address.MAX_LENGTH) + over,
                    VALID_TAGS),
            new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS,
                    "R".repeat(Role.MAX_LENGTH) + over, VALID_TAGS),
            new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, "Logistics", null,
                    "O".repeat(Organisation.MAX_LENGTH) + over, null, VALID_TAGS),
            new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, "Logistics", null, null,
                    "M".repeat(Note.MAX_LENGTH) + over, VALID_TAGS),
        };
        String[] messages = {
            Name.MESSAGE_CONSTRAINTS, Email.MESSAGE_CONSTRAINTS, Address.MESSAGE_CONSTRAINTS,
            Role.MESSAGE_CONSTRAINTS, Organisation.MESSAGE_CONSTRAINTS, Note.MESSAGE_CONSTRAINTS
        };
        for (int i = 0; i < people.length; i++) {
            assertThrows(IllegalValueException.class, messages[i], people[i]::toModelType);
        }
    }

    @Test
    public void toModelType_organisationWithPunctuation_returnsPerson() throws Exception {
        JsonAdaptedPerson person = new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS,
                "Logistics", null, "St. John's Ambulance (NUS)", null, VALID_TAGS);

        assertEquals("St. John's Ambulance (NUS)", person.toModelType().getOrganisation().orElseThrow().getValue());
    }

    /**
     * Phones of 3 to 6 digits were valid in earlier versions, so they still load,
     * but commands only accept the current 7 to 15 digits.
     */
    @Test
    public void toModelType_legacyShortPhone_loadsButIsRejectedAsInput() throws Exception {
        String legacyPhone = "123";
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(VALID_NAME, legacyPhone, VALID_EMAIL, VALID_ADDRESS, VALID_TAGS);

        assertEquals(legacyPhone, person.toModelType().getPhone().toString());
        assertFalse(Phone.isValidInputPhone(legacyPhone));
    }

    /**
     * Tags longer than 30 characters were valid in earlier versions, so they still load,
     * but commands only accept tags of at most 30 characters.
     */
    @Test
    public void toModelType_legacyLongTag_loadsButIsRejectedAsInput() throws Exception {
        String legacyTag = "T".repeat(Tag.MAX_LENGTH + 1);
        JsonAdaptedPerson person = new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS,
                List.of(new JsonAdaptedTag(legacyTag)));

        assertTrue(person.toModelType().getTags().contains(new Tag(legacyTag)));
        assertFalse(Tag.isValidInputTagName(legacyTag));
    }
}
