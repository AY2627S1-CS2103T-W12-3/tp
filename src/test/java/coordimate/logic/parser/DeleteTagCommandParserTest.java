package coordimate.logic.parser;

import static coordimate.logic.parser.CommandParserTestUtil.assertParseFailure;
import static coordimate.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static coordimate.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

import coordimate.logic.commands.DeleteTagCommand;

public class DeleteTagCommandParserTest {

    private final DeleteTagCommandParser parser = new DeleteTagCommandParser();

    @Test
    public void parse_validArguments_returnsDeleteTagCommand() {
        assertParseSuccess(parser, " t/ProductionCrew", new DeleteTagCommand("ProductionCrew"));
        assertParseSuccess(parser, "  t/ Publicity  ", new DeleteTagCommand("Publicity"));
    }

    @Test
    public void parse_missingPrefix_throwsParseException() {
        assertParseFailure(parser, "", DeleteTagCommandParser.MESSAGE_NO_PREFIX);
        assertParseFailure(parser, " Publicity", DeleteTagCommandParser.MESSAGE_NO_PREFIX);
    }

    @Test
    public void parse_missingTagName_throwsParseException() {
        assertParseFailure(parser, " t/", DeleteTagCommandParser.MESSAGE_NO_TAG_NAME);
        assertParseFailure(parser, " t/   ", DeleteTagCommandParser.MESSAGE_NO_TAG_NAME);
    }

    @Test
    public void parse_extraParameters_throwsParseException() {
        assertParseFailure(parser, "Publicity t/Media", DeleteTagCommandParser.MESSAGE_UNKNOWN_PARAMETERS);
        assertParseFailure(parser, " t/Publicity t/Media", DeleteTagCommandParser.MESSAGE_UNKNOWN_PARAMETERS);
    }

    @Test
    public void parse_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }
}
