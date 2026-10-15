package coordimate.logic.parser;

import static coordimate.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
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
    private final String invalidFormat = String.format(MESSAGE_INVALID_COMMAND_FORMAT, FilterCommand.MESSAGE_USAGE);

    @Test
    public void parse_emptyArg_throwsParseException() {
        assertParseFailure(parser, "", invalidFormat);
        assertParseFailure(parser, "   ", invalidFormat);
    }

    @Test
    public void parse_textBeforeFirstField_throwsParseException() {
        assertParseFailure(parser, "oops tag/friend", invalidFormat);
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
    public void parse_fieldLikeTextInQuotes_treatedAsOneValue() {
        FilterCommand expected = new FilterCommand(
                List.of(new FilterCriterion(FilterField.EVENT, "Planning tag/review")));
        assertParseSuccess(parser, " event/\"Planning tag/review\"", expected);
    }

    @Test
    public void parse_fieldAfterQuotedValue_parsedAsSeparateFilter() {
        FilterCommand expected = new FilterCommand(List.of(
                new FilterCriterion(FilterField.EVENT, "Planning tag/review"),
                new FilterCriterion(FilterField.TAG, "friend")));
        assertParseSuccess(parser, " event/\"Planning tag/review\" tag/friend", expected);
    }

    @Test
    public void parse_unclosedQuoteBeforeAnotherField_throwsParseException() {
        assertParseFailure(parser, " event/\"Planning tag/review tag/friend",
                invalidFormat);
        assertParseFailure(parser, " event/\"Final Concert\" tag/\"friend event/Orientation",
                invalidFormat);
    }

    @Test
    public void parse_unclosedQuoteWithNoFieldAfter_keptAsPartOfValue() {
        assertParseSuccess(parser, " event/\"Cali",
                new FilterCommand(List.of(new FilterCriterion(FilterField.EVENT, "\"Cali"))));
        assertParseSuccess(parser, " event/\"Final Concert",
                new FilterCommand(List.of(new FilterCriterion(FilterField.EVENT, "\"Final Concert"))));
        assertParseSuccess(parser, " event/Final Concert\"",
                new FilterCommand(List.of(new FilterCriterion(FilterField.EVENT, "Final Concert\""))));
        assertParseSuccess(parser, " tag/friend event/\"Cali", new FilterCommand(List.of(
                new FilterCriterion(FilterField.TAG, "friend"),
                new FilterCriterion(FilterField.EVENT, "\"Cali"))));
    }

    @Test
    public void parse_singleOrganisationFilter_returnsFilterCommand() {
        FilterCommand expected = new FilterCommand(
                List.of(new FilterCriterion(FilterField.ORGANISATION, "NUS Student Affairs")));
        assertParseSuccess(parser, " organisation/NUS Student Affairs", expected);
        assertParseSuccess(parser, " ORGANISATION/\"NUS Student Affairs\"", expected);
    }

    @Test
    public void parse_emptyValue_throwsParseException() {
        assertParseFailure(parser, " tag/", invalidFormat);
        assertParseFailure(parser, " tag/   ", invalidFormat);
    }

    @Test
    public void parse_unsupportedField_throwsParseException() {
        assertParseFailure(parser, " favourite/true", FilterCommandParser.MESSAGE_UNKNOWN_PARAMETER);
        assertParseFailure(parser, " unknown/value", FilterCommandParser.MESSAGE_UNKNOWN_PARAMETER);
    }

    @Test
    public void parse_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }
}
