package coordimate.logic.parser;

import static coordimate.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static coordimate.logic.parser.CommandParserTestUtil.assertParseFailure;
import static coordimate.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static coordimate.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

import coordimate.logic.commands.DeleteTagCommand;

public class DeleteTagCommandParserTest {

    private final DeleteTagCommandParser parser = new DeleteTagCommandParser();
    private final String invalidFormat = String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteTagCommand.MESSAGE_USAGE);

    @Test
    public void parse_validArguments_returnsDeleteTagCommand() {
        assertParseSuccess(parser, " t/ProductionCrew", new DeleteTagCommand("ProductionCrew"));
        assertParseSuccess(parser, "  t/ Publicity  ", new DeleteTagCommand("Publicity"));
    }

    @Test
    public void parse_missingPrefix_throwsParseException() {
        assertParseFailure(parser, "", invalidFormat);
        assertParseFailure(parser, " Publicity", invalidFormat);
    }

    @Test
    public void parse_missingTagName_throwsParseException() {
        assertParseFailure(parser, " t/", "Tag name must not be empty. Example: t/Publicity");
        assertParseFailure(parser, " t/   ", "Tag name must not be empty. Example: t/Publicity");
    }

    @Test
    public void parse_extraParameters_throwsParseException() {
        assertParseFailure(parser, "Publicity t/Media", invalidFormat);
        assertParseFailure(parser,
                " t/Publicity t/Media", String.format(DeleteTagCommandParser.MESSAGE_REPEATED_PARAMETER, "t/"));
    }

    @Test
    public void parse_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }
}
