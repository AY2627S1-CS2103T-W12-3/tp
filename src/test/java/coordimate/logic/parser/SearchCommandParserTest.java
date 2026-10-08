package coordimate.logic.parser;

import static coordimate.logic.parser.CommandParserTestUtil.assertParseFailure;
import static coordimate.logic.parser.CommandParserTestUtil.assertParseSuccess;

import org.junit.jupiter.api.Test;

import coordimate.logic.commands.SearchCommand;
import coordimate.model.person.ContactMatchesKeywordPredicate;

public class SearchCommandParserTest {

    private final SearchCommandParser parser = new SearchCommandParser();

    @Test
    public void parse_emptyArg_throwsParseException() {
        assertParseFailure(parser, "", SearchCommandParser.MESSAGE_EMPTY_KEYWORD);
        assertParseFailure(parser, "   ", SearchCommandParser.MESSAGE_EMPTY_KEYWORD);
    }

    @Test
    public void parse_quotedOnlyArg_throwsParseException() {
        assertParseFailure(parser, "\"\"", SearchCommandParser.MESSAGE_EMPTY_KEYWORD);
        assertParseFailure(parser, "\"   \"", SearchCommandParser.MESSAGE_EMPTY_KEYWORD);
    }

    @Test
    public void parse_validArgs_returnsSearchCommand() {
        SearchCommand expected = new SearchCommand(new ContactMatchesKeywordPredicate("Alice"), "Alice");
        assertParseSuccess(parser, "Alice", expected);
        assertParseSuccess(parser, "  Alice  ", expected);
    }

    @Test
    public void parse_quotedPhrase_stripsQuotes() {
        SearchCommand expected = new SearchCommand(new ContactMatchesKeywordPredicate("Alice Pauline"),
                "Alice Pauline");
        assertParseSuccess(parser, "\"Alice Pauline\"", expected);
    }

    @Test
    public void parse_null_throwsNullPointerException() {
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> parser.parse(null));
    }
}
