package coordimate.logic.parser;

import static java.util.Objects.requireNonNull;

import coordimate.logic.commands.SearchCommand;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.person.ContactMatchesKeywordPredicate;

/**
 * Parses the remaining input into a search keyword, optionally stripping a pair
 * of surrounding quotation marks used to mark an exact phrase.
 */
public class SearchCommandParser implements Parser<SearchCommand> {

    public static final String MESSAGE_EMPTY_KEYWORD = "Search keyword cannot be empty.";

    @Override
    public SearchCommand parse(String args) throws ParseException {
        requireNonNull(args);
        String trimmed = args.strip();
        if (trimmed.isEmpty()) {
            throw new ParseException(MESSAGE_EMPTY_KEYWORD);
        }
        String keyword = stripQuotes(trimmed);
        if (keyword.isEmpty()) {
            throw new ParseException(MESSAGE_EMPTY_KEYWORD);
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
