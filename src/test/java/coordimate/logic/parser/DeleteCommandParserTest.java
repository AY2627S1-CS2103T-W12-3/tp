package coordimate.logic.parser;

import static coordimate.logic.parser.CommandParserTestUtil.assertParseFailure;
import static coordimate.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static coordimate.testutil.TypicalIndexes.INDEX_FIRST_PERSON;

import org.junit.jupiter.api.Test;

import coordimate.logic.commands.DeleteCommand;
import coordimate.logic.commands.DeleteCommand.IdentifierType;

public class DeleteCommandParserTest {
    private final DeleteCommandParser parser = new DeleteCommandParser();

    @Test
    public void parse_eachIdentifier_success() {
        assertParseSuccess(parser, "1", new DeleteCommand(INDEX_FIRST_PERSON));
        assertParseSuccess(parser, " n/Alice Pauline ", new DeleteCommand(IdentifierType.NAME, "Alice Pauline"));
        assertParseSuccess(parser, " p/(9435) 1253 ", new DeleteCommand(IdentifierType.PHONE, "(9435) 1253"));
        assertParseSuccess(parser, " e/ALICE@EXAMPLE.COM ",
                new DeleteCommand(IdentifierType.EMAIL, "ALICE@EXAMPLE.COM"));
    }

    @Test
    public void parse_missingOrEmptyIdentifier_failure() {
        assertParseFailure(parser, "", DeleteCommandParser.MESSAGE_MISSING_IDENTIFIER);
        assertParseFailure(parser, "n/", DeleteCommandParser.MESSAGE_MISSING_IDENTIFIER);
        assertParseFailure(parser, "p/   ", DeleteCommandParser.MESSAGE_MISSING_IDENTIFIER);
        assertParseFailure(parser, "e/", DeleteCommandParser.MESSAGE_MISSING_IDENTIFIER);
    }

    @Test
    public void parse_invalidIndex_failure() {
        assertParseFailure(parser, "0", DeleteCommandParser.MESSAGE_INVALID_INDEX);
        assertParseFailure(parser, "-1", DeleteCommandParser.MESSAGE_INVALID_INDEX);
        assertParseFailure(parser, "1 extra", DeleteCommandParser.MESSAGE_INVALID_INDEX);
        assertParseFailure(parser, "999999999999999999999", DeleteCommandParser.MESSAGE_INVALID_INDEX);
    }

    @Test
    public void parse_multipleOrUnknownIdentifiers_failure() {
        assertParseFailure(parser, "1 n/Alice", DeleteCommandParser.MESSAGE_MULTIPLE_IDENTIFIERS);
        assertParseFailure(parser, "n/Alice p/94351253", DeleteCommandParser.MESSAGE_MULTIPLE_IDENTIFIERS);
        assertParseFailure(parser, "e/alice@example.com e/bob@example.com",
                DeleteCommandParser.MESSAGE_MULTIPLE_IDENTIFIERS);
        assertParseFailure(parser, "x/Alice", DeleteCommandParser.MESSAGE_UNKNOWN_PARAMETER);
        assertParseFailure(parser, "n/Alice r/President", DeleteCommandParser.MESSAGE_UNKNOWN_PARAMETER);
    }
}
