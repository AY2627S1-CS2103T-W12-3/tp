package coordimate.logic.parser;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import coordimate.logic.Messages;
import coordimate.logic.commands.FilterCommand;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.person.FilterCriterion;
import coordimate.model.person.FilterField;

/**
 * Parses one or more FIELD/VALUE pairs into filter criteria, combined with AND logic.
 */
public class FilterCommandParser implements Parser<FilterCommand> {
    public static final String MESSAGE_UNKNOWN_PARAMETER =
            Messages.MESSAGE_UNKNOWN_PARAMETER + "\n" + FilterCommand.MESSAGE_USAGE;

    private static final Pattern FIELD_PATTERN = Pattern.compile("(?<!\\S)([A-Za-z]+)/");

    @Override
    public FilterCommand parse(String args) throws ParseException {
        requireNonNull(args);
        String trimmed = args.strip();
        if (trimmed.isEmpty()) {
            throw ParserUtil.createInvalidFormatException(FilterCommand.MESSAGE_USAGE);
        }

        List<MatchResult> fieldTokens = findFieldTokens(trimmed);
        if (fieldTokens.isEmpty() || fieldTokens.get(0).start() != 0) {
            throw ParserUtil.createInvalidFormatException(FilterCommand.MESSAGE_USAGE);
        }
        if (hasUnclosedQuoteBeforeField(trimmed)) {
            throw ParserUtil.createInvalidFormatException(FilterCommand.MESSAGE_USAGE);
        }

        List<FilterCriterion> criteria = new ArrayList<>();
        for (int i = 0; i < fieldTokens.size(); i++) {
            MatchResult token = fieldTokens.get(i);
            String fieldName = token.group(1);
            int valueStart = token.end();
            int valueEnd = (i + 1 < fieldTokens.size()) ? fieldTokens.get(i + 1).start() : trimmed.length();
            String value = stripQuotes(trimmed.substring(valueStart, valueEnd).strip());

            FilterField field = FilterField.fromFieldName(fieldName)
                    .orElseThrow(() -> new ParseException(String.format(MESSAGE_UNKNOWN_PARAMETER, fieldName + "/")));
            String example = switch (field) {
                case TAG -> "tag/Publicity";
                case EVENT -> "event/Final Concert";
                case ORGANISATION -> "organisation/NUS Student Affairs";
            };
            ParserUtil.requireValue(value, "Filter value", example);
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

    /**
     * Returns true if {@code args} has an unclosed quotation mark followed by a FIELD/ token, which leaves it
     * unclear where the value ends. An unclosed quotation mark with no field after it is part of the value,
     * since saved names such as an event called {@code "Cali} may contain one.
     */
    private boolean hasUnclosedQuoteBeforeField(String args) {
        if (!isInsideQuotes(args, args.length())) {
            return false;
        }
        return FIELD_PATTERN.matcher(args).find(args.lastIndexOf('"'));
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
