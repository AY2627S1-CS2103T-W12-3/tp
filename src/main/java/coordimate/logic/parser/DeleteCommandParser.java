package coordimate.logic.parser;

import static coordimate.logic.parser.CliSyntax.PREFIX_EMAIL;
import static coordimate.logic.parser.CliSyntax.PREFIX_NAME;
import static coordimate.logic.parser.CliSyntax.PREFIX_PHONE;
import static java.util.Objects.requireNonNull;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import coordimate.commons.core.index.Index;
import coordimate.logic.commands.DeleteCommand;
import coordimate.logic.commands.DeleteCommand.IdentifierType;
import coordimate.logic.parser.exceptions.ParseException;

/** Parses a single displayed index, name, phone number, or email address for deletion. */
public class DeleteCommandParser implements Parser<DeleteCommand> {
    public static final String MESSAGE_MISSING_IDENTIFIER =
            "Please provide an index, name, phone number, or email. Example: delete n/Aisha Tan";
    public static final String MESSAGE_UNKNOWN_PARAMETER =
            "Unknown deletion parameter. Example: delete n/Aisha Tan";
    public static final String MESSAGE_MULTIPLE_IDENTIFIERS =
            "Specify exactly one contact identifier. Example: delete n/Aisha Tan";
    public static final String MESSAGE_INVALID_INDEX = "Invalid contact index. Example: delete 3";

    private static final Pattern PARAMETER_PATTERN = Pattern.compile("(?<!\\S)([A-Za-z]+/)");
    private static final Set<String> ALLOWED_PREFIXES = Set.of("n/", "p/", "e/");

    @Override
    public DeleteCommand parse(String args) throws ParseException {
        requireNonNull(args);
        if (args.isBlank()) {
            throw new ParseException(MESSAGE_MISSING_IDENTIFIER);
        }
        Matcher matcher = PARAMETER_PATTERN.matcher(args);
        while (matcher.find()) {
            if (!ALLOWED_PREFIXES.contains(matcher.group())) {
                throw new ParseException(MESSAGE_UNKNOWN_PARAMETER);
            }
        }
        ArgumentMultimap arguments = ArgumentTokenizer.tokenize(" " + args, PREFIX_NAME, PREFIX_PHONE, PREFIX_EMAIL);
        int prefixCount = arguments.getAllValues(PREFIX_NAME).size()
                + arguments.getAllValues(PREFIX_PHONE).size() + arguments.getAllValues(PREFIX_EMAIL).size();
        if (prefixCount > 1 || (prefixCount == 1 && !arguments.getPreamble().isEmpty())) {
            throw new ParseException(MESSAGE_MULTIPLE_IDENTIFIERS);
        }
        if (prefixCount == 0) {
            try {
                Index index = ParserUtil.parseIndex(arguments.getPreamble());
                return new DeleteCommand(index);
            } catch (ParseException | NumberFormatException e) {
                throw new ParseException(MESSAGE_INVALID_INDEX, e);
            }
        }
        if (!arguments.getAllValues(PREFIX_NAME).isEmpty()) {
            return byDetail(IdentifierType.NAME, arguments.getValue(PREFIX_NAME).orElseThrow());
        }
        if (!arguments.getAllValues(PREFIX_PHONE).isEmpty()) {
            return byDetail(IdentifierType.PHONE, arguments.getValue(PREFIX_PHONE).orElseThrow());
        }
        return byDetail(IdentifierType.EMAIL, arguments.getValue(PREFIX_EMAIL).orElseThrow());
    }

    private static DeleteCommand byDetail(IdentifierType type, String value) throws ParseException {
        if (value.isBlank()) {
            throw new ParseException(MESSAGE_MISSING_IDENTIFIER);
        }
        return new DeleteCommand(type, value);
    }
}
