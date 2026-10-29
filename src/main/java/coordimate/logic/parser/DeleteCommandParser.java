package coordimate.logic.parser;

import static coordimate.logic.parser.CliSyntax.PREFIX_EMAIL;
import static coordimate.logic.parser.CliSyntax.PREFIX_NAME;
import static coordimate.logic.parser.CliSyntax.PREFIX_PHONE;
import static java.util.Objects.requireNonNull;

import java.util.Set;

import coordimate.logic.Messages;
import coordimate.logic.commands.DeleteCommand;
import coordimate.logic.commands.DeleteCommand.IdentifierType;
import coordimate.logic.parser.exceptions.ParseException;

/**
 * Parses a permanent member ID, name, phone number, or email address for deletion.
 */
public class DeleteCommandParser implements Parser<DeleteCommand> {
    public static final String MESSAGE_UNKNOWN_PARAMETER =
            Messages.MESSAGE_UNKNOWN_PARAMETER + "\n" + DeleteCommand.MESSAGE_USAGE;
    public static final String MESSAGE_MULTIPLE_IDENTIFIERS =
            "Specify exactly one contact identifier. Example: delete n/Aisha Tan";
    public static final String MESSAGE_INVALID_MEMBER_ID = "Invalid member ID. Example: delete 12";

    private static final Set<String> ALLOWED_PREFIXES =
            Set.of(PREFIX_NAME.toString(), PREFIX_PHONE.toString(), PREFIX_EMAIL.toString());

    @Override
    public DeleteCommand parse(String args) throws ParseException {
        requireNonNull(args);
        if (args.isBlank()) {
            throw ParserUtil.createInvalidFormatException(DeleteCommand.MESSAGE_USAGE);
        }
        PrefixParser.rejectUnknownParameters(args, PrefixParser.CONTACT_PARAMETER_PATTERN,
                ALLOWED_PREFIXES, MESSAGE_UNKNOWN_PARAMETER);
        ArgumentMultimap arguments = ArgumentTokenizer.tokenize(" " + args, PREFIX_NAME, PREFIX_PHONE, PREFIX_EMAIL);
        try {
            arguments.verifyNoDuplicatePrefixesFor(PREFIX_NAME, PREFIX_PHONE, PREFIX_EMAIL);
        } catch (ParseException e) {
            throw new ParseException(Messages.withUsage(e.getMessage(), DeleteCommand.MESSAGE_USAGE), e);
        }
        int prefixCount = arguments.getAllValues(PREFIX_NAME).size()
                + arguments.getAllValues(PREFIX_PHONE).size() + arguments.getAllValues(PREFIX_EMAIL).size();
        if (prefixCount > 1 || (prefixCount == 1 && !arguments.getPreamble().isEmpty())) {
            throw new ParseException(MESSAGE_MULTIPLE_IDENTIFIERS);
        }
        if (prefixCount == 0) {
            try {
                return new DeleteCommand(ParserUtil.parseMemberId(arguments.getPreamble()));
            } catch (ParseException | NumberFormatException e) {
                throw new ParseException(MESSAGE_INVALID_MEMBER_ID, e);
            }
        }
        if (!arguments.getAllValues(PREFIX_NAME).isEmpty()) {
            return createDeleteCommandByDetail(IdentifierType.NAME, arguments.getValue(PREFIX_NAME).orElseThrow());
        }
        if (!arguments.getAllValues(PREFIX_PHONE).isEmpty()) {
            return createDeleteCommandByDetail(IdentifierType.PHONE, arguments.getValue(PREFIX_PHONE).orElseThrow());
        }
        return createDeleteCommandByDetail(IdentifierType.EMAIL, arguments.getValue(PREFIX_EMAIL).orElseThrow());
    }

    private static DeleteCommand createDeleteCommandByDetail(IdentifierType type, String value) throws ParseException {
        String example = switch (type) {
            case NAME -> "n/Aisha Tan";
            case PHONE -> "p/+6591234567";
            case EMAIL -> "e/aish@example.com";
        };
        ParserUtil.requireValue(value, "Contact identifier", example);
        return new DeleteCommand(type, value);
    }
}
