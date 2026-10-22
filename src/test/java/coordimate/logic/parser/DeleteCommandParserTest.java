package coordimate.logic.parser;

import static coordimate.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static coordimate.logic.parser.CommandParserTestUtil.assertParseFailure;
import static coordimate.logic.parser.CommandParserTestUtil.assertParseSuccess;

import org.junit.jupiter.api.Test;

import coordimate.logic.commands.DeleteCommand;
import coordimate.logic.commands.DeleteCommand.IdentifierType;
import coordimate.model.person.MemberId;

public class DeleteCommandParserTest {
    private final DeleteCommandParser parser = new DeleteCommandParser();
    private final String invalidFormat = String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteCommand.MESSAGE_USAGE);

    @Test
    public void parse_eachIdentifier_success() {
        assertParseSuccess(parser, "1", new DeleteCommand(new MemberId(1)));
        assertParseSuccess(parser, " n/Alice Pauline ", new DeleteCommand(IdentifierType.NAME, "Alice Pauline"));
        assertParseSuccess(parser, " p/(9435) 1253 ", new DeleteCommand(IdentifierType.PHONE, "(9435) 1253"));
        assertParseSuccess(parser, " e/ALICE@EXAMPLE.COM ",
                new DeleteCommand(IdentifierType.EMAIL, "ALICE@EXAMPLE.COM"));
    }

    @Test
    public void parse_missingOrEmptyIdentifier_failure() {
        assertParseFailure(parser, "", invalidFormat);
        assertParseFailure(parser, "n/", "Contact identifier must not be empty. Example: n/Aisha Tan");
        assertParseFailure(parser, "p/   ", "Contact identifier must not be empty. Example: p/+6591234567");
        assertParseFailure(parser, "e/", "Contact identifier must not be empty. Example: e/aish@example.com");
    }

    @Test
    public void parse_invalidIndex_failure() {
        assertParseFailure(parser, "0", DeleteCommandParser.MESSAGE_INVALID_MEMBER_ID);
        assertParseFailure(parser, "-1", DeleteCommandParser.MESSAGE_INVALID_MEMBER_ID);
        assertParseFailure(parser, "1 extra", DeleteCommandParser.MESSAGE_INVALID_MEMBER_ID);
        assertParseFailure(parser, "999999999999999999999", DeleteCommandParser.MESSAGE_INVALID_MEMBER_ID);
    }

    @Test
    public void parse_multipleOrUnknownIdentifiers_failure() {
        assertParseFailure(parser, "1 n/Alice", DeleteCommandParser.MESSAGE_MULTIPLE_IDENTIFIERS);
        assertParseFailure(parser, "n/Alice p/94351253", DeleteCommandParser.MESSAGE_MULTIPLE_IDENTIFIERS);
        assertParseFailure(parser, "e/alice@example.com e/bob@example.com",
                "Parameter e/ may only be specified once.\n" + DeleteCommand.MESSAGE_USAGE);
        assertParseFailure(parser, "x/Alice", String.format(DeleteCommandParser.MESSAGE_UNKNOWN_PARAMETER, "x/"));
        assertParseFailure(parser,
                "n/Alice r/President", String.format(DeleteCommandParser.MESSAGE_UNKNOWN_PARAMETER, "r/"));
    }
}
