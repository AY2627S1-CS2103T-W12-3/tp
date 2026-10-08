package coordimate.logic.parser;

import static coordimate.logic.parser.CliSyntax.PREFIX_TAG;
import static java.util.Objects.requireNonNull;

import coordimate.logic.commands.DeleteTagCommand;
import coordimate.logic.parser.exceptions.ParseException;

/**
 * Parses input arguments and creates a {@link DeleteTagCommand}.
 */
public class DeleteTagCommandParser implements Parser<DeleteTagCommand> {

    public static final String MESSAGE_NO_PREFIX = "No prefix given.";
    public static final String MESSAGE_NO_TAG_NAME = "No tag name given.";
    public static final String MESSAGE_UNKNOWN_PARAMETERS = "Unknown parameters given.";

    @Override
    public DeleteTagCommand parse(String args) throws ParseException {
        requireNonNull(args);
        ArgumentMultimap argMultimap = ArgumentTokenizer.tokenize(args, PREFIX_TAG);
        if (argMultimap.getAllValues(PREFIX_TAG).isEmpty()) {
            throw new ParseException(MESSAGE_NO_PREFIX);
        }
        if (!argMultimap.getPreamble().isEmpty()
                || argMultimap.getAllValues(PREFIX_TAG).size() > 1) {
            throw new ParseException(MESSAGE_UNKNOWN_PARAMETERS);
        }

        String tagName = argMultimap.getValue(PREFIX_TAG).orElseThrow();
        if (tagName.isEmpty()) {
            throw new ParseException(MESSAGE_NO_TAG_NAME);
        }
        return new DeleteTagCommand(tagName);
    }
}
