package coordimate.logic.parser;

import static coordimate.logic.parser.ParserUtil.MESSAGE_INVALID_INDEX;
import static coordimate.testutil.Assert.assertThrows;
import static coordimate.testutil.TypicalIndexes.INDEX_FIRST_PERSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.event.Event;
import coordimate.model.person.Address;
import coordimate.model.person.Birthday;
import coordimate.model.person.Email;
import coordimate.model.person.MemberId;
import coordimate.model.person.Name;
import coordimate.model.person.Note;
import coordimate.model.person.Organisation;
import coordimate.model.person.Phone;
import coordimate.model.person.Role;
import coordimate.model.tag.Tag;

public class ParserUtilTest {
    private static final String INVALID_NAME = "R@chel";
    private static final String INVALID_PHONE = "+651234";
    private static final String INVALID_ADDRESS = " ";
    private static final String INVALID_EMAIL = "example.com";
    private static final String INVALID_TAG = "#friend";

    private static final String VALID_NAME = "Rachel Walker";
    private static final String VALID_PHONE = "1234567";
    private static final String VALID_ADDRESS = "123 Main Street #0505";
    private static final String VALID_EMAIL = "rachel@example.com";
    private static final String VALID_TAG_1 = "friend";
    private static final String VALID_TAG_2 = "neighbour";

    private static final String WHITESPACE = " \t\r\n";

    @Test
    public void parseMemberId_acceptsPositiveIntegersWithoutListPositionConversion() throws Exception {
        assertEquals(new MemberId(12), ParserUtil.parseMemberId(" 12 "));
        assertEquals(new MemberId(Integer.MAX_VALUE), ParserUtil.parseMemberId("2147483647"));
    }

    @Test
    public void parseMemberId_invalidValues_throwsParseException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseMemberId(null));
        for (String input : List.of("", "0", "-1", "1.5", "12.0", "1 2", "2147483648", "999999999999999999")) {
            assertThrows(ParseException.class, MemberId.MESSAGE_CONSTRAINTS, () -> ParserUtil.parseMemberId(input));
        }
    }

    @Test
    public void parseIndex_invalidInput_throwsParseException() {
        assertThrows(ParseException.class, () -> ParserUtil.parseIndex("10 a"));
    }

    @Test
    public void parseIndex_outOfRangeInput_throwsParseException() {
        assertThrows(ParseException.class, MESSAGE_INVALID_INDEX, ()
                -> ParserUtil.parseIndex(Long.toString(Integer.MAX_VALUE + 1)));
    }

    @Test
    public void parseIndex_validInput_success() throws Exception {
        // No whitespaces
        assertEquals(INDEX_FIRST_PERSON, ParserUtil.parseIndex("1"));

        // Leading and trailing whitespaces
        assertEquals(INDEX_FIRST_PERSON, ParserUtil.parseIndex("  1  "));
    }

    @Test
    public void parseName_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseName((String) null));
    }

    @Test
    public void parseName_invalidValue_throwsParseException() {
        assertThrows(ParseException.class, () -> ParserUtil.parseName(INVALID_NAME));
    }

    @Test
    public void parseName_validValueWithoutWhitespace_returnsName() throws Exception {
        Name expectedName = new Name(VALID_NAME);
        assertEquals(expectedName, ParserUtil.parseName(VALID_NAME));
    }

    @Test
    public void parseName_validValueWithWhitespace_returnsTrimmedName() throws Exception {
        String nameWithWhitespace = WHITESPACE + VALID_NAME + WHITESPACE;
        Name expectedName = new Name(VALID_NAME);
        assertEquals(expectedName, ParserUtil.parseName(nameWithWhitespace));
    }

    @Test
    public void parsePhone_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parsePhone((String) null));
    }

    @Test
    public void parsePhone_invalidValue_throwsParseException() {
        assertThrows(ParseException.class, () -> ParserUtil.parsePhone(INVALID_PHONE));
    }

    @Test
    public void parsePhone_validValueWithoutWhitespace_returnsPhone() throws Exception {
        Phone expectedPhone = new Phone(VALID_PHONE);
        assertEquals(expectedPhone, ParserUtil.parsePhone(VALID_PHONE));
    }

    @Test
    public void parsePhone_validValueWithWhitespace_returnsTrimmedPhone() throws Exception {
        String phoneWithWhitespace = WHITESPACE + VALID_PHONE + WHITESPACE;
        Phone expectedPhone = new Phone(VALID_PHONE);
        assertEquals(expectedPhone, ParserUtil.parsePhone(phoneWithWhitespace));
    }

    @Test
    public void parseAddress_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseAddress((String) null));
    }

    @Test
    public void parseAddress_invalidValue_throwsParseException() {
        assertThrows(ParseException.class, () -> ParserUtil.parseAddress(INVALID_ADDRESS));
    }

    @Test
    public void parseAddress_validValueWithoutWhitespace_returnsAddress() throws Exception {
        Address expectedAddress = new Address(VALID_ADDRESS);
        assertEquals(expectedAddress, ParserUtil.parseAddress(VALID_ADDRESS));
    }

    @Test
    public void parseAddress_validValueWithWhitespace_returnsTrimmedAddress() throws Exception {
        String addressWithWhitespace = WHITESPACE + VALID_ADDRESS + WHITESPACE;
        Address expectedAddress = new Address(VALID_ADDRESS);
        assertEquals(expectedAddress, ParserUtil.parseAddress(addressWithWhitespace));
    }

    @Test
    public void parseEmail_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseEmail((String) null));
    }

    @Test
    public void parseEmail_invalidValue_throwsParseException() {
        assertThrows(ParseException.class, () -> ParserUtil.parseEmail(INVALID_EMAIL));
    }

    @Test
    public void parseEmail_validValueWithoutWhitespace_returnsEmail() throws Exception {
        Email expectedEmail = new Email(VALID_EMAIL);
        assertEquals(expectedEmail, ParserUtil.parseEmail(VALID_EMAIL));
    }

    @Test
    public void parseEmail_validValueWithWhitespace_returnsTrimmedEmail() throws Exception {
        String emailWithWhitespace = WHITESPACE + VALID_EMAIL + WHITESPACE;
        Email expectedEmail = new Email(VALID_EMAIL);
        assertEquals(expectedEmail, ParserUtil.parseEmail(emailWithWhitespace));
    }

    @Test
    public void parseTag_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseTag(null));
    }

    @Test
    public void parseTag_invalidValue_throwsParseException() {
        assertThrows(ParseException.class, () -> ParserUtil.parseTag(INVALID_TAG));
        assertThrows(ParseException.class, () ->
                ParserUtil.parseTag("1234567890123456789012345678901"));
    }

    @Test
    public void parseTag_validValueWithoutWhitespace_returnsTag() throws Exception {
        Tag expectedTag = new Tag(VALID_TAG_1);
        assertEquals(expectedTag, ParserUtil.parseTag(VALID_TAG_1));
    }

    @Test
    public void parseTag_validValueWithWhitespace_returnsTrimmedTag() throws Exception {
        String tagWithWhitespace = WHITESPACE + VALID_TAG_1 + WHITESPACE;
        Tag expectedTag = new Tag(VALID_TAG_1);
        assertEquals(expectedTag, ParserUtil.parseTag(tagWithWhitespace));
    }

    @Test
    public void parseTags_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseTags(null));
    }

    @Test
    public void parseTags_collectionWithInvalidTags_throwsParseException() {
        assertThrows(ParseException.class, () -> ParserUtil.parseTags(List.of(VALID_TAG_1, INVALID_TAG)));
    }

    @Test
    public void parseTags_emptyCollection_returnsEmptySet() throws Exception {
        assertTrue(ParserUtil.parseTags(List.of()).isEmpty());
    }

    @Test
    public void parseTags_collectionWithValidTags_returnsTagSet() throws Exception {
        Set<Tag> actualTagSet = ParserUtil.parseTags(List.of(VALID_TAG_1, VALID_TAG_2));
        Set<Tag> expectedTagSet = Set.of(new Tag(VALID_TAG_1), new Tag(VALID_TAG_2));

        assertEquals(expectedTagSet, actualTagSet);
    }

    @Test
    public void parseRole_variousValues() throws Exception {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseRole(null));
        assertThrows(ParseException.class, Role.MESSAGE_CONSTRAINTS, () -> ParserUtil.parseRole("***"));
        assertThrows(ParseException.class, Role.MESSAGE_CONSTRAINTS, () ->
                ParserUtil.parseRole("R".repeat(Role.MAX_LENGTH + 1)));
        assertEquals(new Role("Logistics Lead"), ParserUtil.parseRole(WHITESPACE + "Logistics Lead" + WHITESPACE));
    }

    @Test
    public void parseBirthday_variousValues() throws Exception {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseBirthday(null));
        assertThrows(ParseException.class, Birthday.MESSAGE_CONSTRAINTS, () -> ParserUtil.parseBirthday("31-02-2004"));
        assertThrows(ParseException.class, Birthday.MESSAGE_CONSTRAINTS, () -> ParserUtil.parseBirthday("2004-06-18"));
        assertEquals(new Birthday("18-06-2004"), ParserUtil.parseBirthday(WHITESPACE + "18-06-2004" + WHITESPACE));
    }

    @Test
    public void parseOrganisation_variousValues() throws Exception {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseOrganisation(null));
        assertThrows(ParseException.class, Organisation.MESSAGE_CONSTRAINTS, () ->
                ParserUtil.parseOrganisation("NUS @ Kent Ridge"));
        assertThrows(ParseException.class, Organisation.MESSAGE_CONSTRAINTS, () ->
                ParserUtil.parseOrganisation("O".repeat(Organisation.MAX_LENGTH + 1)));
        assertEquals(new Organisation("St. John's Ambulance"),
                ParserUtil.parseOrganisation(WHITESPACE + "St. John's Ambulance" + WHITESPACE));
    }

    @Test
    public void parseNote_variousValues() throws Exception {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseNote(null));
        assertThrows(ParseException.class, Note.MESSAGE_CONSTRAINTS, () -> ParserUtil.parseNote(WHITESPACE));
        assertThrows(ParseException.class, Note.MESSAGE_CONSTRAINTS, () ->
                ParserUtil.parseNote("N".repeat(Note.MAX_LENGTH + 1)));
        assertEquals(new Note("Handles venue bookings"),
                ParserUtil.parseNote(WHITESPACE + "Handles venue bookings" + WHITESPACE));
    }

    @Test
    public void parseEventName_variousValues() throws Exception {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseEventName(null));
        assertThrows(ParseException.class, Event.MESSAGE_EMPTY_NAME, () -> ParserUtil.parseEventName(WHITESPACE));
        assertThrows(ParseException.class, Event.MESSAGE_NAME_LENGTH_CONSTRAINTS, () ->
                ParserUtil.parseEventName("E".repeat(Event.MAX_NAME_LENGTH + 1)));
        assertThrows(ParseException.class, Event.MESSAGE_NAME_CONSTRAINTS, () ->
                ParserUtil.parseEventName("Final\u0007Concert"));
        assertEquals("Final Concert", ParserUtil.parseEventName("  Final Concert  "));
        assertEquals("E".repeat(Event.MAX_NAME_LENGTH), ParserUtil.parseEventName("E".repeat(Event.MAX_NAME_LENGTH)));
    }
}
