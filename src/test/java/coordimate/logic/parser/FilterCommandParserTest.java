package coordimate.logic.parser;

import static coordimate.logic.parser.CommandParserTestUtil.assertParseFailure;
import static coordimate.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import coordimate.logic.commands.FilterCommand;
import coordimate.model.person.FilterCriterion;
import coordimate.model.person.FilterField;

public class FilterCommandParserTest {

    private final FilterCommandParser parser = new FilterCommandParser();

    @Test
    public void parse_emptyArg_throwsParseException() {
        assertParseFailure(parser, "", FilterCommandParser.MESSAGE_EMPTY_COMMAND);
        assertParseFailure(parser, "   ", FilterCommandParser.MESSAGE_EMPTY_COMMAND);
    }

    @Test
    public void parse_textBeforeFirstField_throwsParseException() {
        assertParseFailure(parser, "oops tag/friend", FilterCommandParser.MESSAGE_EMPTY_COMMAND);
    }

    @Test
    public void parse_singleTagFilter_returnsFilterCommand() {
        FilterCommand expected = new FilterCommand(List.of(new FilterCriterion(FilterField.TAG, "friend")));
        assertParseSuccess(parser, " tag/friend", expected);
    }

    @Test
    public void parse_singleEventFilter_returnsFilterCommand() {
        FilterCommand expected = new FilterCommand(List.of(new FilterCriterion(FilterField.EVENT, "Orientation")));
        assertParseSuccess(parser, " event/Orientation", expected);
    }

    @Test
    public void parse_multipleFilters_combinesAllCriteria() {
        FilterCommand expected = new FilterCommand(List.of(
                new FilterCriterion(FilterField.TAG, "friend"),
                new FilterCriterion(FilterField.EVENT, "Orientation")));
        assertParseSuccess(parser, " tag/friend event/Orientation", expected);
    }

    @Test
    public void parse_valueWithSpacesInQuotes_stripsQuotes() {
        FilterCommand expected = new FilterCommand(
                List.of(new FilterCriterion(FilterField.EVENT, "Final Concert")));
        assertParseSuccess(parser, " event/\"Final Concert\"", expected);
    }

    @Test
    public void parse_emptyValue_throwsParseException() {
        assertParseFailure(parser, " tag/", FilterCommandParser.MESSAGE_EMPTY_VALUE);
        assertParseFailure(parser, " tag/   ", FilterCommandParser.MESSAGE_EMPTY_VALUE);
    }

    @Test
    public void parse_unsupportedField_throwsParseException() {
        assertParseFailure(parser, " organisation/NUS", FilterCommandParser.MESSAGE_INVALID_FIELD);
        assertParseFailure(parser, " favourite/true", FilterCommandParser.MESSAGE_INVALID_FIELD);
        assertParseFailure(parser, " unknown/value", FilterCommandParser.MESSAGE_INVALID_FIELD);
    }

    @Test
    public void parse_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }
}
