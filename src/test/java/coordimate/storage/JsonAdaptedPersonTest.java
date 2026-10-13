package coordimate.storage;

import static coordimate.storage.JsonAdaptedPerson.MISSING_FIELD_MESSAGE_FORMAT;
import static coordimate.testutil.Assert.assertThrows;
import static coordimate.testutil.TypicalPersons.BENSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    public void convertToModelType_validPersonDetails_returnsPerson() throws Exception {
        JsonAdaptedPerson person = new JsonAdaptedPerson(BENSON);
        assertEquals(BENSON, person.convertToModelType());
    }

    @Test
    public void convertToModelType_personWithRole_preservesRole() throws Exception {
        Person contact = new PersonBuilder(BENSON).setRole("Logistics").build();
        JsonAdaptedPerson person = new JsonAdaptedPerson(contact);

        assertEquals(contact, person.convertToModelType());
    }

    @Test
    public void convertToModelType_personWithOptionalDetails_preservesDetails() throws Exception {
        Person contact = new PersonBuilder(BENSON).setBirthday("18-06-2004")
                .setOrganisation("NUS Student Affairs").setNote("Handles venue bookings").build();
        JsonAdaptedPerson person = new JsonAdaptedPerson(contact);

        assertEquals(contact, person.convertToModelType());
    }

    @Test
    public void convertToModelType_legacyPersonWithoutOptionalDetails_leavesDetailsUnset() throws Exception {
        JsonAdaptedPerson person = new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS,
                VALID_TAGS);

        assertEquals(BENSON, person.convertToModelType());
    }

    @Test
    public void convertToModelType_invalidBirthday_throwsIllegalValueException() {
        JsonAdaptedPerson person = new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS,
                "Logistics", "31-02-2004", null, null, VALID_TAGS);

        assertThrows(IllegalValueException.class, Birthday.MESSAGE_CONSTRAINTS, person::convertToModelType);
    }

    @Test
    public void convertToModelType_invalidOrganisation_throwsIllegalValueException() {
        JsonAdaptedPerson person = new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS,
                "Logistics", null, "NUS_Student Affairs", null, VALID_TAGS);

        assertThrows(IllegalValueException.class, Organisation.MESSAGE_CONSTRAINTS, person::convertToModelType);
    }

    @Test
    public void convertToModelType_invalidNote_throwsIllegalValueException() {
        JsonAdaptedPerson person = new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS,
                "Logistics", null, null, "Line one\nLine two", VALID_TAGS);

        assertThrows(IllegalValueException.class, Note.MESSAGE_CONSTRAINTS, person::convertToModelType);
    }

    @Test
    public void convertToModelType_legacyPersonWithoutRole_usesNa() throws Exception {
        JsonAdaptedPerson person = new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS,
                VALID_TAGS);

        assertEquals(Role.NOT_APPLICABLE, person.convertToModelType().getRole());
    }

    @Test
    public void convertToModelType_invalidRole_throwsIllegalValueException() {
        JsonAdaptedPerson person = new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS,
                "invalid_role", VALID_TAGS);

        assertThrows(IllegalValueException.class, Role.MESSAGE_CONSTRAINTS, person::convertToModelType);
    }

    @Test
    public void convertToModelType_emptyRole_throwsIllegalValueException() {
        JsonAdaptedPerson person = new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS,
                "", VALID_TAGS);

        assertThrows(IllegalValueException.class, Role.MESSAGE_CONSTRAINTS, person::convertToModelType);
    }

    @Test
    public void convertToModelType_invalidName_throwsIllegalValueException() {
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(INVALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
        String expectedMessage = Name.MESSAGE_CONSTRAINTS;
        assertThrows(IllegalValueException.class, expectedMessage, person::convertToModelType);
    }

    @Test
    public void convertToModelType_nullName_throwsIllegalValueException() {
        JsonAdaptedPerson person = new JsonAdaptedPerson(null, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
        String expectedMessage = String.format(MISSING_FIELD_MESSAGE_FORMAT, Name.class.getSimpleName());
        assertThrows(IllegalValueException.class, expectedMessage, person::convertToModelType);
    }

    @Test
    public void convertToModelType_invalidPhone_throwsIllegalValueException() {
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(VALID_NAME, INVALID_PHONE, VALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
        String expectedMessage = Phone.MESSAGE_CONSTRAINTS;
        assertThrows(IllegalValueException.class, expectedMessage, person::convertToModelType);
    }

    @Test
    public void convertToModelType_nullPhone_throwsIllegalValueException() {
        JsonAdaptedPerson person = new JsonAdaptedPerson(VALID_NAME, null, VALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
        String expectedMessage = String.format(MISSING_FIELD_MESSAGE_FORMAT, Phone.class.getSimpleName());
        assertThrows(IllegalValueException.class, expectedMessage, person::convertToModelType);
    }

    @Test
    public void convertToModelType_invalidEmail_throwsIllegalValueException() {
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, INVALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
        String expectedMessage = Email.MESSAGE_CONSTRAINTS;
        assertThrows(IllegalValueException.class, expectedMessage, person::convertToModelType);
    }

    @Test
    public void convertToModelType_nullEmail_throwsIllegalValueException() {
        JsonAdaptedPerson person = new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, null, VALID_ADDRESS, VALID_TAGS);
        String expectedMessage = String.format(MISSING_FIELD_MESSAGE_FORMAT, Email.class.getSimpleName());
        assertThrows(IllegalValueException.class, expectedMessage, person::convertToModelType);
    }

    @Test
    public void convertToModelType_invalidAddress_throwsIllegalValueException() {
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, INVALID_ADDRESS, VALID_TAGS);
        String expectedMessage = Address.MESSAGE_CONSTRAINTS;
        assertThrows(IllegalValueException.class, expectedMessage, person::convertToModelType);
    }

    @Test
    public void convertToModelType_nullAddress_leavesAddressUnset() throws Exception {
        JsonAdaptedPerson person = new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, null, VALID_TAGS);

        assertTrue(person.convertToModelType().getAddress().isEmpty());
    }

    @Test
    public void convertToModelType_invalidTags_throwsIllegalValueException() {
        List<JsonAdaptedTag> invalidTags = new ArrayList<>(VALID_TAGS);
        invalidTags.add(new JsonAdaptedTag(INVALID_TAG));
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, invalidTags);
        assertThrows(IllegalValueException.class, person::convertToModelType);
    }

}
