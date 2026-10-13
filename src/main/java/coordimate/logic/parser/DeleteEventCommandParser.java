package coordimate.logic.parser;

import static coordimate.logic.parser.CliSyntax.PREFIX_EVENT_NAME;
import static java.util.Objects.requireNonNull;

import java.util.regex.Matcher;

import coordimate.logic.Messages;
import coordimate.logic.commands.DeleteEventCommand;
import coordimate.logic.parser.exceptions.ParseException;

/**
 * Parses event deletion and rejects unknown or repeated parameters.
 */
public class DeleteEventCommandParser implements Parser<DeleteEventCommand> {
    public static final String MESSAGE_UNKNOWN_PARAMETER =
            "Unknown parameter. Example: deleteevent evn/Logistics Meeting";
    public static final String MESSAGE_REPEATED_PARAMETER =
            Messages.MESSAGE_REPEATED_PARAMETER;

    @Override
    public DeleteEventCommand parse(String args) throws ParseException {
        requireNonNull(args);
        Matcher matcher = PrefixParser.EVENT_PARAMETER_PATTERN.matcher(args);
        int valueStart = -1;
        while (matcher.find()) {
            if (!PREFIX_EVENT_NAME.toString().equals(matcher.group())) {
                throw new ParseException(MESSAGE_UNKNOWN_PARAMETER);
            }
            if (valueStart >= 0) {
                throw new ParseException(MESSAGE_REPEATED_PARAMETER);
            }
            if (!args.substring(0, matcher.start()).isBlank()) {
                throw createInvalidFormatException();
            }
            valueStart = matcher.end();
        }
        if (valueStart < 0 || args.substring(valueStart).isBlank()) {
            throw createInvalidFormatException();
        }
        return new DeleteEventCommand(args.substring(valueStart));
    }

    private ParseException createInvalidFormatException() {
        return ParserUtil.createInvalidFormatException(DeleteEventCommand.MESSAGE_USAGE);
    }
}
