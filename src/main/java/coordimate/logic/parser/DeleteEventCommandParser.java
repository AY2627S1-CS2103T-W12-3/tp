package coordimate.logic.parser;

import static coordimate.logic.parser.CliSyntax.PREFIX_EVENT_NAME;

import java.util.Map;
import java.util.Set;

import coordimate.logic.Messages;
import coordimate.logic.commands.DeleteEventCommand;
import coordimate.logic.parser.exceptions.ParseException;

/**
 * Parses event deletion and rejects unknown or repeated parameters.
 */
public class DeleteEventCommandParser implements Parser<DeleteEventCommand> {
    public static final String MESSAGE_UNKNOWN_PARAMETER =
            Messages.MESSAGE_UNKNOWN_PARAMETER + "\n" + DeleteEventCommand.MESSAGE_USAGE;
    public static final String MESSAGE_REPEATED_PARAMETER =
            Messages.MESSAGE_REPEATED_PARAMETER + "\n" + DeleteEventCommand.MESSAGE_USAGE;

    @Override
    public DeleteEventCommand parse(String args) throws ParseException {
        Map<String, String> values = PrefixParser.parseUniqueParameters(args, PrefixParser.EVENT_PARAMETER_PATTERN,
                Set.of(PREFIX_EVENT_NAME.toString()), DeleteEventCommand.MESSAGE_USAGE,
                MESSAGE_UNKNOWN_PARAMETER, MESSAGE_REPEATED_PARAMETER);
        String eventName = values.get(PREFIX_EVENT_NAME.toString());
        if (eventName == null) {
            throw ParserUtil.createInvalidFormatException(DeleteEventCommand.MESSAGE_USAGE);
        }
        return new DeleteEventCommand(ParserUtil.parseEventName(eventName));
    }
}
