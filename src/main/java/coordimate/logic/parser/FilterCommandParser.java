package coordimate.logic.parser;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import coordimate.logic.commands.FilterCommand;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.person.FilterCriterion;
import coordimate.model.person.FilterField;

/**
 * Parses one or more FIELD/VALUE pairs into filter criteria, combined with AND logic.
 */
public class FilterCommandParser implements Parser<FilterCommand> {

    public static final String MESSAGE_EMPTY_COMMAND = "Filter field cannot be empty.";
    public static final String MESSAGE_EMPTY_VALUE = "Filter value cannot be empty.";
    public static final String MESSAGE_INVALID_FIELD =
            "Invalid filter field. Supported fields: tag, event, organisation.";

    private static final Pattern FIELD_PATTERN = Pattern.compile("(?<!\\S)([A-Za-z]+)/");

    @Override
    public FilterCommand parse(String args) throws ParseException {
        requireNonNull(args);
        String trimmed = args.strip();
        if (trimmed.isEmpty()) {
            throw new ParseException(MESSAGE_EMPTY_COMMAND);
        }

        List<MatchResult> fieldTokens = findFieldTokens(trimmed);
        if (fieldTokens.isEmpty() || fieldTokens.get(0).start() != 0) {
            throw new ParseException(MESSAGE_EMPTY_COMMAND);
        }

        List<FilterCriterion> criteria = new ArrayList<>();
        for (int i = 0; i < fieldTokens.size(); i++) {
            MatchResult token = fieldTokens.get(i);
            String fieldName = token.group(1);
            int valueStart = token.end();
            int valueEnd = (i + 1 < fieldTokens.size()) ? fieldTokens.get(i + 1).start() : trimmed.length();
            String value = stripQuotes(trimmed.substring(valueStart, valueEnd).strip());

            if (value.isEmpty()) {
                throw new ParseException(MESSAGE_EMPTY_VALUE);
            }
            FilterField field = FilterField.fromFieldName(fieldName)
                    .orElseThrow(() -> new ParseException(MESSAGE_INVALID_FIELD));
            criteria.add(new FilterCriterion(field, value));
        }

        return new FilterCommand(criteria);
    }

    /**
     * Returns the FIELD/ tokens in {@code args}, ignoring any that appear inside a quoted value.
     */
    private List<MatchResult> findFieldTokens(String args) {
        Matcher matcher = FIELD_PATTERN.matcher(args);
        List<MatchResult> tokens = new ArrayList<>();
        while (matcher.find()) {
            if (!isInsideQuotes(args, matcher.start())) {
                tokens.add(matcher.toMatchResult());
            }
        }
        return tokens;
    }

    private boolean isInsideQuotes(String args, int index) {
        return args.substring(0, index).chars().filter(c -> c == '"').count() % 2 == 1;
    }

    private String stripQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1).strip();
        }
        return value;
    }
}
