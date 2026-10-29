package coordimate.logic.parser;

import static coordimate.logic.parser.CliSyntax.PREFIX_TAG;
import static java.util.Objects.requireNonNull;

import java.util.Set;

import coordimate.logic.Messages;
import coordimate.logic.parser.exceptions.ParseException;

/**
 * Validates the single {@code t/TAG} parameter shared by the tag commands.
 */
final class TagParserUtil {

    private TagParserUtil() {}

    /**
     * Returns the message shown when {@code args} contains a prefix other than {@code t/}.
     */
    static String unknownParameterMessage(String usage) {
        return Messages.MESSAGE_UNKNOWN_PARAMETER + "\n" + usage;
    }

    /**
     * Returns the message shown when {@code t/} is given more than once.
     */
    static String repeatedParameterMessage(String usage) {
        return Messages.MESSAGE_REPEATED_PARAMETER + "\n" + usage;
    }

    /**
     * Tokenizes {@code args} after checking that it has exactly one non-empty {@code t/} value, no other prefixes,
     * and a preamble only if {@code requiresPreamble} is true.
     *
     * @throws ParseException if any of these checks fail, using {@code usage} in the error message.
     */
    static ArgumentMultimap tokenizeTagArgs(String args, String usage, boolean requiresPreamble)
            throws ParseException {
        requireNonNull(args);
        PrefixParser.rejectUnknownParameters(args, PrefixParser.CONTACT_PARAMETER_PATTERN,
                Set.of(PREFIX_TAG.toString()), unknownParameterMessage(usage));
        ArgumentMultimap argMultimap = ArgumentTokenizer.tokenize(args, PREFIX_TAG);
        if (argMultimap.getAllValues(PREFIX_TAG).isEmpty()) {
            throw ParserUtil.createInvalidFormatException(usage);
        }
        if (argMultimap.getPreamble().isEmpty() == requiresPreamble) {
            throw ParserUtil.createInvalidFormatException(usage);
        }
        if (argMultimap.getAllValues(PREFIX_TAG).size() > 1) {
            throw new ParseException(String.format(repeatedParameterMessage(usage), PREFIX_TAG));
        }

        ParserUtil.requireValue(argMultimap.getValue(PREFIX_TAG).orElseThrow(), "Tag name", "t/Publicity");
        return argMultimap;
    }
}
