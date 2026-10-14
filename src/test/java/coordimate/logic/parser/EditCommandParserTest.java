package coordimate.logic.parser;

import static coordimate.logic.commands.CommandTestUtil.ADDRESS_DESC_AMY;
import static coordimate.logic.commands.CommandTestUtil.ADDRESS_DESC_BOB;
import static coordimate.logic.commands.CommandTestUtil.EMAIL_DESC_AMY;
import static coordimate.logic.commands.CommandTestUtil.EMAIL_DESC_BOB;
import static coordimate.logic.commands.CommandTestUtil.INVALID_ADDRESS_DESC;
import static coordimate.logic.commands.CommandTestUtil.INVALID_EMAIL_DESC;
import static coordimate.logic.commands.CommandTestUtil.INVALID_NAME_DESC;
import static coordimate.logic.commands.CommandTestUtil.INVALID_PHONE_DESC;
import static coordimate.logic.commands.CommandTestUtil.INVALID_TAG_DESC;
import static coordimate.logic.commands.CommandTestUtil.NAME_DESC_AMY;
import static coordimate.logic.commands.CommandTestUtil.PHONE_DESC_AMY;
import static coordimate.logic.commands.CommandTestUtil.PHONE_DESC_BOB;
import static coordimate.logic.commands.CommandTestUtil.TAG_DESC_FRIEND;
import static coordimate.logic.commands.CommandTestUtil.TAG_DESC_HUSBAND;
import static coordimate.logic.commands.CommandTestUtil.VALID_ADDRESS_AMY;
import static coordimate.logic.commands.CommandTestUtil.VALID_EMAIL_AMY;
import static coordimate.logic.commands.CommandTestUtil.VALID_NAME_AMY;
import static coordimate.logic.commands.CommandTestUtil.VALID_PHONE_AMY;
import static coordimate.logic.commands.CommandTestUtil.VALID_PHONE_BOB;
import static coordimate.logic.commands.CommandTestUtil.VALID_TAG_FRIEND;
import static coordimate.logic.commands.CommandTestUtil.VALID_TAG_HUSBAND;
import static coordimate.logic.parser.CliSyntax.PREFIX_TAG;
import static coordimate.logic.parser.CommandParserTestUtil.assertParseFailure;
import static coordimate.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static coordimate.testutil.TypicalIndexes.INDEX_FIRST_PERSON;
import static coordimate.testutil.TypicalIndexes.INDEX_SECOND_PERSON;
import static coordimate.testutil.TypicalIndexes.INDEX_THIRD_PERSON;

import org.junit.jupiter.api.Test;

import coordimate.commons.core.index.Index;
import coordimate.logic.commands.EditCommand;
import coordimate.logic.commands.EditCommand.EditPersonDescriptor;
import coordimate.logic.commands.EditCommand.TagOperation;
import coordimate.logic.commands.EditCommand.TagOperationType;
import coordimate.model.person.Address;
import coordimate.model.person.Birthday;
import coordimate.model.person.Email;
import coordimate.model.person.Name;
import coordimate.model.person.Note;
import coordimate.model.person.Organisation;
import coordimate.model.person.Phone;
import coordimate.model.person.Role;
import coordimate.model.tag.Tag;
import coordimate.testutil.EditPersonDescriptorBuilder;

public class EditCommandParserTest {

    private static final String TAG_EMPTY = " " + PREFIX_TAG;

    private EditCommandParser parser = new EditCommandParser();

    @Test
    public void parse_missingParts_failure() {
        // no index specified
        assertParseFailure(parser, VALID_NAME_AMY, EditCommandParser.MESSAGE_INVALID_INDEX);

        // no field specified
        assertParseFailure(parser, "1", EditCommand.MESSAGE_NOT_EDITED);

        // no index and no field specified
        assertParseFailure(parser, "", EditCommandParser.MESSAGE_INVALID_INDEX);
    }

    @Test
    public void parse_invalidPreamble_failure() {
        // negative index
        assertParseFailure(parser, "-5" + NAME_DESC_AMY, EditCommandParser.MESSAGE_INVALID_INDEX);

        // zero index
        assertParseFailure(parser, "0" + NAME_DESC_AMY, EditCommandParser.MESSAGE_INVALID_INDEX);

        // invalid arguments being parsed as preamble
        assertParseFailure(parser, "1 some random string", EditCommandParser.MESSAGE_INVALID_INDEX);

        // invalid prefix being parsed as preamble
        assertParseFailure(parser, "1 i/ string", EditCommandParser.MESSAGE_UNKNOWN_PARAMETER);
    }

    @Test
    public void parse_invalidValue_failure() {
        assertParseFailure(parser, "1" + INVALID_NAME_DESC, Name.MESSAGE_CONSTRAINTS); // invalid name
        assertParseFailure(parser, "1" + INVALID_PHONE_DESC, Phone.MESSAGE_CONSTRAINTS); // invalid phone
        assertParseFailure(parser, "1" + INVALID_EMAIL_DESC, Email.MESSAGE_CONSTRAINTS); // invalid email
        assertParseFailure(parser, "1 a/Bad\tAddress", Address.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, "1 r/", Role.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, "1 b/31-02-2020", Birthday.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, "1 o/*", Organisation.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, "1 m/Bad\tNote", Note.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, "1" + INVALID_TAG_DESC, Tag.MESSAGE_CONSTRAINTS); // invalid tag

        // invalid phone followed by valid email
        assertParseFailure(parser, "1" + INVALID_PHONE_DESC + EMAIL_DESC_AMY, Phone.MESSAGE_CONSTRAINTS);

        // multiple invalid values, but only the first invalid value is captured
        assertParseFailure(parser, "1" + INVALID_NAME_DESC + INVALID_EMAIL_DESC + VALID_ADDRESS_AMY + VALID_PHONE_AMY,
                Name.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void parse_phoneOutsideCurrentLength_rejectedEvenIfLegacyNumber() {
        for (String phone : new String[] {"123", "123456", "1234567890123456"}) {
            assertParseFailure(parser, "1 p/" + phone, Phone.MESSAGE_CONSTRAINTS);
        }
    }

    @Test
    public void parse_phoneAtCurrentLengthBoundaries_success() {
        for (String phone : new String[] {"1234567", "123456789012345"}) {
            EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder().withPhone(phone).build();
            assertParseSuccess(parser, "1 p/" + phone, new EditCommand(INDEX_FIRST_PERSON, descriptor));
        }
    }

    @Test
    public void parse_allFieldsSpecified_success() {
        Index targetIndex = INDEX_SECOND_PERSON;
        String userInput = targetIndex.getOneBased() + PHONE_DESC_BOB + TAG_DESC_HUSBAND
                + EMAIL_DESC_AMY + ADDRESS_DESC_AMY + NAME_DESC_AMY + TAG_DESC_FRIEND;

        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder().withName(VALID_NAME_AMY)
                .withPhone(VALID_PHONE_BOB).withEmail(VALID_EMAIL_AMY).withAddress(VALID_ADDRESS_AMY)
                .withTags(VALID_TAG_HUSBAND, VALID_TAG_FRIEND).build();
        EditCommand expectedCommand = new EditCommand(targetIndex, descriptor);

        assertParseSuccess(parser, userInput, expectedCommand);
    }

    @Test
    public void parse_someFieldsSpecified_success() {
        Index targetIndex = INDEX_FIRST_PERSON;
        String userInput = targetIndex.getOneBased() + PHONE_DESC_BOB + EMAIL_DESC_AMY;

        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder().withPhone(VALID_PHONE_BOB)
                .withEmail(VALID_EMAIL_AMY).build();
        EditCommand expectedCommand = new EditCommand(targetIndex, descriptor);

        assertParseSuccess(parser, userInput, expectedCommand);
    }

    @Test
    public void parse_expandedFields_success() {
        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder()
                .withRole("Logistics Lead").withBirthday("18-06-2004")
                .withOrganisation("NUS Student Affairs").withNote("Handles venue bookings").build();

        assertParseSuccess(parser,
                "1 r/Logistics Lead b/18-06-2004 o/NUS Student Affairs m/Handles venue bookings",
                new EditCommand(INDEX_FIRST_PERSON, descriptor));
    }

    @Test
    public void parse_emptyOptionalFields_success() {
        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder()
                .withoutBirthday().withoutAddress().withoutOrganisation().withoutNote().build();

        assertParseSuccess(parser, "1 b/ a/ o/ m/", new EditCommand(INDEX_FIRST_PERSON, descriptor));
    }

    @Test
    public void parse_emptyRequiredFields_failure() {
        assertParseFailure(parser, "1 n/", Name.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, "1 p/", Phone.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, "1 e/", Email.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, "1 r/", Role.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void parse_repeatedExpandedFields_failure() {
        assertParseFailure(parser, "1 r/Logistics r/President", EditCommandParser.MESSAGE_REPEATED_PARAMETER);
        assertParseFailure(parser, "1 b/ b/18-06-2004", EditCommandParser.MESSAGE_REPEATED_PARAMETER);
        assertParseFailure(parser, "1 o/One o/Two", EditCommandParser.MESSAGE_REPEATED_PARAMETER);
        assertParseFailure(parser, "1 m/One m/Two", EditCommandParser.MESSAGE_REPEATED_PARAMETER);
    }

    @Test
    public void parse_unknownParameter_failure() {
        assertParseFailure(parser, "1 r/Logistics x/value", EditCommandParser.MESSAGE_UNKNOWN_PARAMETER);
        assertParseFailure(parser, "1 id/Alice r/Logistics", EditCommandParser.MESSAGE_UNKNOWN_PARAMETER);
    }

    @Test
    public void parse_targetIdentifier_success() {
        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder().withRole("Logistics").build();

        assertParseSuccess(parser, "target/Alice Pauline r/Logistics",
                new EditCommand("Alice Pauline", descriptor));
        assertParseSuccess(parser, "target/alice@example.com r/Logistics",
                new EditCommand("alice@example.com", descriptor));
        assertParseSuccess(parser, "target/(9435) 1253 r/Logistics",
                new EditCommand("(9435) 1253", descriptor));
    }

    @Test
    public void parse_invalidTargetIdentifier_failure() {
        assertParseFailure(parser, "target/ r/Logistics", EditCommandParser.MESSAGE_EMPTY_TARGET);
        assertParseFailure(parser, "1 target/Alice r/Logistics", EditCommandParser.MESSAGE_MULTIPLE_IDENTIFIERS);
        assertParseFailure(parser, "target/Alice target/Bob r/Logistics",
                EditCommandParser.MESSAGE_REPEATED_PARAMETER);
        assertParseFailure(parser, "target/Alice", EditCommand.MESSAGE_NOT_EDITED);
    }

    @Test
    public void parse_oneFieldSpecified_success() {
        // name
        Index targetIndex = INDEX_THIRD_PERSON;
        String userInput = targetIndex.getOneBased() + NAME_DESC_AMY;
        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder().withName(VALID_NAME_AMY).build();
        EditCommand expectedCommand = new EditCommand(targetIndex, descriptor);
        assertParseSuccess(parser, userInput, expectedCommand);

        // phone
        userInput = targetIndex.getOneBased() + PHONE_DESC_AMY;
        descriptor = new EditPersonDescriptorBuilder().withPhone(VALID_PHONE_AMY).build();
        expectedCommand = new EditCommand(targetIndex, descriptor);
        assertParseSuccess(parser, userInput, expectedCommand);

        // email
        userInput = targetIndex.getOneBased() + EMAIL_DESC_AMY;
        descriptor = new EditPersonDescriptorBuilder().withEmail(VALID_EMAIL_AMY).build();
        expectedCommand = new EditCommand(targetIndex, descriptor);
        assertParseSuccess(parser, userInput, expectedCommand);

        // address
        userInput = targetIndex.getOneBased() + ADDRESS_DESC_AMY;
        descriptor = new EditPersonDescriptorBuilder().withAddress(VALID_ADDRESS_AMY).build();
        expectedCommand = new EditCommand(targetIndex, descriptor);
        assertParseSuccess(parser, userInput, expectedCommand);

        // tags
        userInput = targetIndex.getOneBased() + TAG_DESC_FRIEND;
        descriptor = new EditPersonDescriptorBuilder().withTags(VALID_TAG_FRIEND).build();
        expectedCommand = new EditCommand(targetIndex, descriptor);
        assertParseSuccess(parser, userInput, expectedCommand);
    }

    @Test
    public void parse_multipleRepeatedFields_failure() {
        // More extensive testing of duplicate parameter detections is done in
        // AddCommandParserTest#parse_repeatedNonTagValue_failure()

        // valid followed by invalid
        Index targetIndex = INDEX_FIRST_PERSON;
        String userInput = targetIndex.getOneBased() + INVALID_PHONE_DESC + PHONE_DESC_BOB;

        assertParseFailure(parser, userInput, EditCommandParser.MESSAGE_REPEATED_PARAMETER);

        // invalid followed by valid
        userInput = targetIndex.getOneBased() + PHONE_DESC_BOB + INVALID_PHONE_DESC;

        assertParseFailure(parser, userInput, EditCommandParser.MESSAGE_REPEATED_PARAMETER);

        // multiple valid fields repeated
        userInput = targetIndex.getOneBased() + PHONE_DESC_AMY + ADDRESS_DESC_AMY + EMAIL_DESC_AMY
                + TAG_DESC_FRIEND + PHONE_DESC_AMY + ADDRESS_DESC_AMY + EMAIL_DESC_AMY + TAG_DESC_FRIEND
                + PHONE_DESC_BOB + ADDRESS_DESC_BOB + EMAIL_DESC_BOB + TAG_DESC_HUSBAND;

        assertParseFailure(parser, userInput, EditCommandParser.MESSAGE_REPEATED_PARAMETER);

        // multiple invalid values
        userInput = targetIndex.getOneBased() + INVALID_PHONE_DESC + INVALID_ADDRESS_DESC + INVALID_EMAIL_DESC
                + INVALID_PHONE_DESC + INVALID_ADDRESS_DESC + INVALID_EMAIL_DESC;

        assertParseFailure(parser, userInput, EditCommandParser.MESSAGE_REPEATED_PARAMETER);
    }

    @Test
    public void parse_resetTags_success() {
        Index targetIndex = INDEX_THIRD_PERSON;
        String userInput = targetIndex.getOneBased() + TAG_EMPTY;

        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder().withTags().build();
        EditCommand expectedCommand = new EditCommand(targetIndex, descriptor);

        assertParseSuccess(parser, userInput, expectedCommand);
    }

    @Test
    public void parse_replacementTags_emptyValueClearsPrecedingTags() {
        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder().withTags().build();
        assertParseSuccess(parser, "1 t/Friend t/Husband t/", new EditCommand(INDEX_FIRST_PERSON, descriptor));

        descriptor = new EditPersonDescriptorBuilder().withTags("Husband").build();
        assertParseSuccess(parser, "1 t/Friend t/ t/Husband", new EditCommand(INDEX_FIRST_PERSON, descriptor));
    }

    @Test
    public void parse_replacementTags_caseInsensitiveDuplicatesIgnored() {
        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder().withTags("Friend").build();
        assertParseSuccess(parser, "1 t/Friend t/friend", new EditCommand(INDEX_FIRST_PERSON, descriptor));
    }

    @Test
    public void parse_addAndRemoveTags_preservesCommandOrder() {
        EditPersonDescriptor descriptor = new EditPersonDescriptor();
        descriptor.addTagOperation(new TagOperation(TagOperationType.ADD, new Tag("Sponsor")));
        descriptor.addTagOperation(new TagOperation(TagOperationType.REMOVE, new Tag("Logistics")));
        descriptor.addTagOperation(new TagOperation(TagOperationType.ADD, new Tag("EXCO")));

        assertParseSuccess(parser, "1 at/Sponsor rt/Logistics at/EXCO",
                new EditCommand(INDEX_FIRST_PERSON, descriptor));
    }

    @Test
    public void parse_invalidTagOperations_failure() {
        assertParseFailure(parser, "1 at/", Tag.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, "1 rt/", Tag.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, "1 at/*", Tag.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, "1 rt/*", Tag.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, "1 t/Friend at/Sponsor", EditCommandParser.MESSAGE_CONFLICTING_TAG_OPERATIONS);
        assertParseFailure(parser, "1 rt/Friend t/", EditCommandParser.MESSAGE_CONFLICTING_TAG_OPERATIONS);
    }
}
