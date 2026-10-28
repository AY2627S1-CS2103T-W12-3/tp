package coordimate.logic.parser;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import coordimate.commons.util.ParameterSyntax;
import coordimate.commons.util.StringUtil;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.person.Name;

/**
 * Scans command parameters while preserving each command's accepted prefix syntax.
 */
final class PrefixParser {
    static final String PARAMETER_BOUNDARY = ParameterSyntax.BOUNDARY;
    /** Matches a contact command prefix, but not the name words {@code s/o} and {@code d/o}. */
    static final Pattern CONTACT_PARAMETER_PATTERN =
            Pattern.compile(PARAMETER_BOUNDARY + "(?!" + Name.RELATIONSHIP_WORD_REGEX + ")([A-Za-z]+/)");
    static final Pattern EVENT_PARAMETER_PATTERN = ParameterSyntax.EVENT_PARAMETER_PATTERN;

    private PrefixParser() {}

    /**
     * Rejects unknown prefixes without changing how repeated or empty values are handled.
     */
    static void rejectUnknownParameters(String args, Pattern pattern, Collection<String> allowedPrefixes,
            String unknownParameterMessage) throws ParseException {
        requireNonNull(args);
        Matcher matcher = pattern.matcher(args);
        while (matcher.find()) {
            if (!allowedPrefixes.contains(matcher.group())) {
                throw new ParseException(String.format(unknownParameterMessage, matcher.group()));
            }
        }
    }

    /**
     * Parses single-valued parameters, rejecting a preamble and unknown or repeated prefixes.
     * Missing and empty values are left for the command parser to validate.
     */
    static Map<String, String> parseUniqueParameters(String args, Pattern pattern,
            Collection<String> allowedPrefixes, String usage, String unknownParameterMessage,
            String repeatedParameterMessage) throws ParseException {
        requireNonNull(args);
        Map<String, String> values = new LinkedHashMap<>();
        Matcher matcher = pattern.matcher(args);
        String previousPrefix = null;
        int valueStart = 0;
        while (matcher.find()) {
            if (previousPrefix == null && !StringUtil.stripWhitespace(args.substring(0, matcher.start())).isEmpty()) {
                throw ParserUtil.createInvalidFormatException(usage);
            }
            if (previousPrefix != null) {
                values.put(previousPrefix, StringUtil.stripWhitespace(args.substring(valueStart, matcher.start())));
            }
            String prefix = matcher.group();
            if (!allowedPrefixes.contains(prefix)) {
                throw new ParseException(String.format(unknownParameterMessage, prefix));
            }
            if (values.containsKey(prefix)) {
                throw new ParseException(String.format(repeatedParameterMessage, prefix));
            }
            previousPrefix = prefix;
            valueStart = matcher.end();
        }
        if (previousPrefix != null) {
            values.put(previousPrefix, StringUtil.stripWhitespace(args.substring(valueStart)));
        }
        return values;
    }
}
