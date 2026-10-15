package coordimate.logic.parser;

import static coordimate.logic.parser.CliSyntax.PREFIX_TAG;
import static java.util.Objects.requireNonNull;

import java.util.Set;

import coordimate.logic.Messages;
import coordimate.logic.commands.DeleteTagCommand;
import coordimate.logic.parser.exceptions.ParseException;

/**
 * Parses input arguments and creates a {@link DeleteTagCommand}.
 */
public class DeleteTagCommandParser implements Parser<DeleteTagCommand> {
    public static final String MESSAGE_UNKNOWN_PARAMETER =
            Messages.MESSAGE_UNKNOWN_PARAMETER + "\n" + DeleteTagCommand.MESSAGE_USAGE;
    public static final String MESSAGE_REPEATED_PARAMETER =
            Messages.MESSAGE_REPEATED_PARAMETER + "\n" + DeleteTagCommand.MESSAGE_USAGE;

    @Override
    public DeleteTagCommand parse(String args) throws ParseException {
        requireNonNull(args);
        PrefixParser.rejectUnknownParameters(args, PrefixParser.CONTACT_PARAMETER_PATTERN,
                Set.of(PREFIX_TAG.toString()), MESSAGE_UNKNOWN_PARAMETER);
        ArgumentMultimap argMultimap = ArgumentTokenizer.tokenize(args, PREFIX_TAG);
        if (argMultimap.getAllValues(PREFIX_TAG).isEmpty()) {
            throw ParserUtil.createInvalidFormatException(DeleteTagCommand.MESSAGE_USAGE);
        }
        if (!argMultimap.getPreamble().isEmpty()) {
            throw ParserUtil.createInvalidFormatException(DeleteTagCommand.MESSAGE_USAGE);
        }
        if (argMultimap.getAllValues(PREFIX_TAG).size() > 1) {
            throw new ParseException(String.format(MESSAGE_REPEATED_PARAMETER, PREFIX_TAG));
        }

        String tagName = argMultimap.getValue(PREFIX_TAG).orElseThrow();
        ParserUtil.requireValue(tagName, "Tag name", "t/Publicity");
        return new DeleteTagCommand(tagName);
    }
}
