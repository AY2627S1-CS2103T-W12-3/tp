package coordimate.logic.parser;

import static java.util.Objects.requireNonNull;

import coordimate.commons.util.StringUtil;
import coordimate.logic.commands.SearchCommand;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.person.ContactMatchesKeywordPredicate;

/**
 * Parses the remaining input into a search keyword, optionally stripping a pair
 * of surrounding quotation marks used to mark an exact phrase.
 * Each run of spaces in the keyword is collapsed to one.
 */
public class SearchCommandParser implements Parser<SearchCommand> {

    @Override
    public SearchCommand parse(String args) throws ParseException {
        requireNonNull(args);
        String trimmed = args.strip();
        if (trimmed.isEmpty()) {
            throw ParserUtil.createInvalidFormatException(SearchCommand.MESSAGE_USAGE);
        }
        String keyword = StringUtil.normalizeWhitespace(stripQuotes(trimmed));
        if (keyword.isEmpty()) {
            throw ParserUtil.createInvalidFormatException(SearchCommand.MESSAGE_USAGE);
        }
        return new SearchCommand(new ContactMatchesKeywordPredicate(keyword), keyword);
    }

    private String stripQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1).strip();
        }
        return value;
    }
}
