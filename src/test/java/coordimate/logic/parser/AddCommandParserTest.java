package coordimate.logic.parser;

import static coordimate.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static coordimate.logic.parser.CommandParserTestUtil.assertParseFailure;
import static coordimate.logic.parser.CommandParserTestUtil.assertParseSuccess;

import org.junit.jupiter.api.Test;

import coordimate.logic.Messages;
import coordimate.logic.commands.AddCommand;
import coordimate.model.person.Address;
import coordimate.model.person.Birthday;
import coordimate.model.person.Email;
import coordimate.model.person.Name;
import coordimate.model.person.Note;
import coordimate.model.person.Organisation;
import coordimate.model.person.Phone;
import coordimate.model.person.Role;
import coordimate.testutil.PersonBuilder;

public class AddCommandParserTest {
    private static final String REQUIRED = " n/Amy Bee p/85355255 e/amy@gmail.com r/Logistics";
    private static final String INVALID_FORMAT = String.format(MESSAGE_INVALID_COMMAND_FORMAT,
            AddCommand.MESSAGE_USAGE);

    private final AddCommandParser parser = new AddCommandParser();

    @Test
    public void parse_allFieldsPresent_success() {
        AddCommand expected = new AddCommand(new PersonBuilder().withRole("Logistics")
                .withBirthday("18-06-2004").withAddress("21 Kent Ridge Road, #03-12")
                .withOrganisation("NUS Student Affairs").withNote("Handles venue bookings")
                .withTags("EXCO", "Publicity").build());

        assertParseSuccess(parser, " m/Handles venue bookings t/EXCO" + REQUIRED
                + " o/NUS Student Affairs b/18-06-2004 t/Publicity a/21 Kent Ridge Road, #03-12", expected);
    }

    @Test
    public void parse_optionalFieldsOmittedOrEmpty_unset() {
        AddCommand expected = new AddCommand(new PersonBuilder().withRole("Logistics").withoutAddress()
                .withTags().build());

        assertParseSuccess(parser, REQUIRED, expected);
        assertParseSuccess(parser, REQUIRED + " b/ a/ o/ m/ t/", expected);
        assertParseSuccess(parser, REQUIRED + " b/   a/   o/   m/   t/   ", expected);
    }

    @Test
    public void parse_repeatedTagsAndCaseVariants_uniqueTags() {
        AddCommand expected = new AddCommand(new PersonBuilder().withRole("Logistics")
                .withoutAddress().withTags("EXCO", "Publicity").build());

        assertParseSuccess(parser, REQUIRED + " t/EXCO t/exco t/Publicity", expected);
        assertParseSuccess(parser, REQUIRED + " t/ t/EXCO t/Publicity", expected);
    }

    @Test
    public void parse_missingRequiredField_invalidFormat() {
        assertParseFailure(parser, " p/85355255 e/amy@gmail.com r/Logistics", INVALID_FORMAT);
        assertParseFailure(parser, " n/Amy Bee e/amy@gmail.com r/Logistics", INVALID_FORMAT);
        assertParseFailure(parser, " n/Amy Bee p/85355255 r/Logistics", INVALID_FORMAT);
        assertParseFailure(parser, " n/Amy Bee p/85355255 e/amy@gmail.com", INVALID_FORMAT);
        assertParseFailure(parser, "", INVALID_FORMAT);
    }

    @Test
    public void parse_emptyRequiredField_rejected() {
        assertParseFailure(parser, " n/ p/85355255 e/amy@gmail.com r/Logistics",
                AddCommandParser.MESSAGE_EMPTY_REQUIRED_VALUE);
        assertParseFailure(parser, " n/Amy Bee p/ e/amy@gmail.com r/Logistics",
                AddCommandParser.MESSAGE_EMPTY_REQUIRED_VALUE);
        assertParseFailure(parser, " n/Amy Bee p/85355255 e/ r/Logistics",
                AddCommandParser.MESSAGE_EMPTY_REQUIRED_VALUE);
        assertParseFailure(parser, " n/Amy Bee p/85355255 e/amy@gmail.com r/",
                AddCommandParser.MESSAGE_EMPTY_REQUIRED_VALUE);
    }

    @Test
    public void parse_repeatedRequiredField_rejected() {
        assertParseFailure(parser, REQUIRED + " n/Bob", AddCommandParser.MESSAGE_REPEATED_REQUIRED_PARAMETER);
        assertParseFailure(parser, REQUIRED + " p/1234567", AddCommandParser.MESSAGE_REPEATED_REQUIRED_PARAMETER);
        assertParseFailure(parser, REQUIRED + " e/bob@example.com",
                AddCommandParser.MESSAGE_REPEATED_REQUIRED_PARAMETER);
        assertParseFailure(parser, REQUIRED + " r/EXCO", AddCommandParser.MESSAGE_REPEATED_REQUIRED_PARAMETER);
    }

    @Test
    public void parse_repeatedOptionalNonTagField_rejected() {
        assertParseFailure(parser, REQUIRED + " b/ b/18-06-2004",
                Messages.getErrorMessageForDuplicatePrefixes(CliSyntax.PREFIX_BIRTHDAY));
        assertParseFailure(parser, REQUIRED + " a/Somewhere a/Elsewhere",
                Messages.getErrorMessageForDuplicatePrefixes(CliSyntax.PREFIX_ADDRESS));
        assertParseFailure(parser, REQUIRED + " o/NUS o/NTU",
                Messages.getErrorMessageForDuplicatePrefixes(CliSyntax.PREFIX_ORGANISATION));
        assertParseFailure(parser, REQUIRED + " m/First m/Second",
                Messages.getErrorMessageForDuplicatePrefixes(CliSyntax.PREFIX_NOTE));
    }

    @Test
    public void parse_unknownParameterOrPreamble_rejected() {
        assertParseFailure(parser, REQUIRED + " x/value", AddCommandParser.MESSAGE_UNKNOWN_PARAMETER);
        assertParseFailure(parser, " unexpected" + REQUIRED, INVALID_FORMAT);
    }

    @Test
    public void parse_invalidField_relevantError() {
        assertParseFailure(parser, REQUIRED + " n/123", AddCommandParser.MESSAGE_REPEATED_REQUIRED_PARAMETER);
        assertParseFailure(parser, REQUIRED.replace("Amy Bee", "123"), Name.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, REQUIRED.replace("85355255", "123"), Phone.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, REQUIRED.replace("amy@gmail.com", "bad"), Email.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, REQUIRED.replace("Logistics", "***"), Role.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, REQUIRED + " b/31-02-2004", Birthday.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, REQUIRED + " a/Bad\tAddress", Address.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, REQUIRED + " o/!", Organisation.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, REQUIRED + " m/Bad\nNote", Note.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, REQUIRED + " t/Bad Tag", AddCommandParser.MESSAGE_INVALID_TAG);
    }

    @Test
    public void parse_phoneOutsideCurrentLength_rejectedEvenIfLegacyNumber() {
        for (String phone : new String[] {"123", "123456", "1234567890123456"}) {
            assertParseFailure(parser, REQUIRED.replace("85355255", phone), Phone.MESSAGE_CONSTRAINTS);
        }
    }

    @Test
    public void parse_phoneAtCurrentLengthBoundaries_success() {
        for (String phone : new String[] {"1234567", "123456789012345"}) {
            AddCommand expected = new AddCommand(new PersonBuilder().withRole("Logistics")
                    .withPhone(phone).withoutAddress().withTags().build());
            assertParseSuccess(parser, REQUIRED.replace("85355255", phone), expected);
        }
    }
}
