package coordimate.logic.parser;

import static coordimate.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static coordimate.logic.parser.CommandParserTestUtil.assertParseFailure;
import static coordimate.logic.parser.CommandParserTestUtil.assertParseSuccess;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Test;

import coordimate.logic.Messages;
import coordimate.logic.commands.AddCommand;
import coordimate.model.person.Address;
import coordimate.model.person.Birthday;
import coordimate.model.person.Email;
import coordimate.model.person.LeaveDate;
import coordimate.model.person.Name;
import coordimate.model.person.Note;
import coordimate.model.person.Organisation;
import coordimate.model.person.Phone;
import coordimate.model.person.Role;
import coordimate.model.tag.Tag;
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
    public void parse_leaveDate_setsLeave() {
        String future = LocalDate.now().plusDays(30).format(DateTimeFormatter.ofPattern("dd-MM-uuuu"));
        AddCommand expected = new AddCommand(new PersonBuilder().withRole("Logistics").withoutAddress()
                .withLeaveDate(future).build());

        assertParseSuccess(parser, REQUIRED + " lv/" + future, expected);
        assertParseSuccess(parser, " lv/ " + future + " " + REQUIRED, expected);
    }

    @Test
    public void parse_emptyLeaveDate_notOnLeave() {
        AddCommand expected = new AddCommand(new PersonBuilder().withRole("Logistics").withoutAddress().build());

        assertParseSuccess(parser, REQUIRED + " lv/", expected);
    }

    @Test
    public void parse_invalidOrRepeatedLeaveDate_failure() {
        String future = LocalDate.now().plusDays(30).format(DateTimeFormatter.ofPattern("dd-MM-uuuu"));

        assertParseFailure(parser, REQUIRED + " lv/01-01-2020", LeaveDate.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, REQUIRED + " lv/31-11-2026", LeaveDate.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, REQUIRED + " lv/next week", LeaveDate.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, REQUIRED + " lv/" + future + " lv/" + future,
                Messages.getErrorMessageForDuplicatePrefixes(CliSyntax.PREFIX_LEAVE)
                        + "\n" + AddCommand.MESSAGE_USAGE);
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
                "Name must not be empty. Example: n/Aisha Tan");
        assertParseFailure(parser, " n/Amy Bee p/ e/amy@gmail.com r/Logistics",
                "Phone number must not be empty. Example: p/+6591234567");
        assertParseFailure(parser, " n/Amy Bee p/85355255 e/ r/Logistics",
                "Email must not be empty. Example: e/aish@example.com");
        assertParseFailure(parser, " n/Amy Bee p/85355255 e/amy@gmail.com r/",
                "Role must not be empty. Example: r/Logistics Lead");
    }

    @Test
    public void parse_repeatedRequiredField_rejected() {
        assertParseFailure(parser,
                REQUIRED + " n/Bob", String.format(AddCommandParser.MESSAGE_REPEATED_REQUIRED_PARAMETER, "n/"));
        assertParseFailure(parser,
                REQUIRED + " p/1234567", String.format(AddCommandParser.MESSAGE_REPEATED_REQUIRED_PARAMETER, "p/"));
        assertParseFailure(parser, REQUIRED + " e/bob@example.com",
                String.format(AddCommandParser.MESSAGE_REPEATED_REQUIRED_PARAMETER, "e/"));
        assertParseFailure(parser,
                REQUIRED + " r/EXCO", String.format(AddCommandParser.MESSAGE_REPEATED_REQUIRED_PARAMETER, "r/"));
    }

    @Test
    public void parse_repeatedOptionalNonTagField_rejected() {
        assertParseFailure(parser, REQUIRED + " b/ b/18-06-2004",
                Messages.getErrorMessageForDuplicatePrefixes(CliSyntax.PREFIX_BIRTHDAY)
                        + "\n" + AddCommand.MESSAGE_USAGE);
        assertParseFailure(parser, REQUIRED + " a/Somewhere a/Elsewhere",
                Messages.getErrorMessageForDuplicatePrefixes(CliSyntax.PREFIX_ADDRESS)
                        + "\n" + AddCommand.MESSAGE_USAGE);
        assertParseFailure(parser, REQUIRED + " o/NUS o/NTU",
                Messages.getErrorMessageForDuplicatePrefixes(CliSyntax.PREFIX_ORGANISATION)
                        + "\n" + AddCommand.MESSAGE_USAGE);
        assertParseFailure(parser, REQUIRED + " m/First m/Second",
                Messages.getErrorMessageForDuplicatePrefixes(CliSyntax.PREFIX_NOTE) + "\n" + AddCommand.MESSAGE_USAGE);
    }

    @Test
    public void parse_unknownParameterOrPreamble_rejected() {
        assertParseFailure(parser,
                REQUIRED + " x/value", String.format(AddCommandParser.MESSAGE_UNKNOWN_PARAMETER, "x/"));
        assertParseFailure(parser, " unexpected" + REQUIRED, INVALID_FORMAT);
    }

    @Test
    public void parse_invalidField_relevantError() {
        assertParseFailure(parser,
                REQUIRED + " n/123", String.format(AddCommandParser.MESSAGE_REPEATED_REQUIRED_PARAMETER, "n/"));
        assertParseFailure(parser, REQUIRED.replace("Amy Bee", "123"), Name.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, REQUIRED.replace("85355255", "123"), Phone.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, REQUIRED.replace("amy@gmail.com", "bad"), Email.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, REQUIRED.replace("Logistics", "***"), Role.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, REQUIRED + " b/31-02-2004", Birthday.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, REQUIRED + " a/Bad\tAddress", Address.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, REQUIRED + " o/!", Organisation.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, REQUIRED + " m/Bad\nNote", Note.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, REQUIRED + " t/Bad Tag", Tag.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, REQUIRED + " t/1234567890123456789012345678901",
                Tag.MESSAGE_LENGTH_CONSTRAINTS);
        assertParseFailure(parser, REQUIRED + " t/Bad Tag", Tag.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, REQUIRED + " t/1234567890123456789012345678901", Tag.MESSAGE_LENGTH_CONSTRAINTS);
    }

    @Test
    public void parse_fieldsAtAndOverMaximumLength_acceptedThenRejected() throws Exception {
        String[][] cases = {
            {"Logistics", "r/", "R".repeat(Role.MAX_LENGTH), Role.MESSAGE_CONSTRAINTS},
            {null, "a/", "A".repeat(Address.MAX_LENGTH), Address.MESSAGE_CONSTRAINTS},
            {null, "o/", "O".repeat(Organisation.MAX_LENGTH), Organisation.MESSAGE_CONSTRAINTS},
            {null, "m/", "M".repeat(Note.MAX_LENGTH), Note.MESSAGE_CONSTRAINTS},
            {null, "t/", "T".repeat(Tag.MAX_LENGTH), Tag.MESSAGE_LENGTH_CONSTRAINTS},
        };
        for (String[] c : cases) {
            String atLimit = c[0] == null ? REQUIRED + " " + c[1] + c[2] : REQUIRED.replace(c[0], c[2]);
            String overLimit = c[0] == null ? REQUIRED + " " + c[1] + c[2] + "X" : REQUIRED.replace(c[0], c[2] + "X");
            parser.parse(atLimit);
            assertParseFailure(parser, overLimit, c[3]);
        }
        parser.parse(REQUIRED.replace("Amy Bee", "N".repeat(Name.MAX_LENGTH)));
        assertParseFailure(parser, REQUIRED.replace("Amy Bee", "N".repeat(Name.MAX_LENGTH + 1)),
                Name.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, REQUIRED.replace("amy@gmail.com", "a".repeat(65) + "@gmail.com"),
                Email.MESSAGE_CONSTRAINTS);
        parser.parse(REQUIRED + " o/St. John's Ambulance (NUS)");
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
