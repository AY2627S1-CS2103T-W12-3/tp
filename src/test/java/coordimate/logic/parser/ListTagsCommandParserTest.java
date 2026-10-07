package coordimate.logic.parser;

import static coordimate.logic.parser.CommandParserTestUtil.assertParseFailure;
import static coordimate.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

import coordimate.logic.commands.ListTagsCommand;

public class ListTagsCommandParserTest {

    private final ListTagsCommandParser parser = new ListTagsCommandParser();

    @Test
    public void parse_noParameters_returnsListTagsCommand() throws Exception {
        assertInstanceOf(ListTagsCommand.class, parser.parse(""));
        assertInstanceOf(ListTagsCommand.class, parser.parse("  \t  "));
    }

    @Test
    public void parse_extraParameters_throwsParseException() {
        assertParseFailure(parser, "unexpected", ListTagsCommandParser.MESSAGE_UNKNOWN_PARAMETERS);
        assertParseFailure(parser, "t/EXCO", ListTagsCommandParser.MESSAGE_UNKNOWN_PARAMETERS);
    }

    @Test
    public void parse_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }
}
