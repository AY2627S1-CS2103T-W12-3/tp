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
            Messages.MESSAGE_UNKNOWN_PARAMETER + "\n" + DeleteEventCommand.MESSAGE_USAGE;
    public static final String MESSAGE_REPEATED_PARAMETER =
            Messages.MESSAGE_REPEATED_PARAMETER + "\n" + DeleteEventCommand.MESSAGE_USAGE;

    @Override
    public DeleteEventCommand parse(String args) throws ParseException {
        requireNonNull(args);
        Matcher matcher = PrefixParser.EVENT_PARAMETER_PATTERN.matcher(args);
        int valueStart = -1;
        while (matcher.find()) {
            if (!PREFIX_EVENT_NAME.toString().equals(matcher.group())) {
                throw new ParseException(String.format(MESSAGE_UNKNOWN_PARAMETER, matcher.group()));
            }
            if (valueStart >= 0) {
                throw new ParseException(String.format(MESSAGE_REPEATED_PARAMETER, PREFIX_EVENT_NAME));
            }
            if (!args.substring(0, matcher.start()).isBlank()) {
                throw createInvalidFormatException();
            }
            valueStart = matcher.end();
        }
        if (valueStart < 0) {
            throw createInvalidFormatException();
        }
        return new DeleteEventCommand(ParserUtil.parseEventName(args.substring(valueStart)));
    }

    private ParseException createInvalidFormatException() {
        return ParserUtil.createInvalidFormatException(DeleteEventCommand.MESSAGE_USAGE);
    }
}
