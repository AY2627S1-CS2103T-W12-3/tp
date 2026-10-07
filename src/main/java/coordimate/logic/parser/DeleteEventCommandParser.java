package coordimate.logic.parser;

import static coordimate.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static coordimate.logic.parser.CliSyntax.PREFIX_EVENT_NAME;
import static java.util.Objects.requireNonNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import coordimate.logic.commands.DeleteEventCommand;
import coordimate.logic.parser.exceptions.ParseException;

/**
 * Parses event deletion and rejects unknown or repeated parameters.
 */
public class DeleteEventCommandParser implements Parser<DeleteEventCommand> {
    public static final String MESSAGE_UNKNOWN_PARAMETER =
            "Unknown parameter. Example: deleteevent evn/Logistics Meeting";
    public static final String MESSAGE_REPEATED_PARAMETER = "Each parameter may only be specified once.";

    private static final Pattern PARAMETER_PATTERN = Pattern.compile("(?<!\\S)([A-Za-z]+/|/[A-Za-z]+)");

    @Override
    public DeleteEventCommand parse(String args) throws ParseException {
        requireNonNull(args);
        Matcher matcher = PARAMETER_PATTERN.matcher(args);
        int valueStart = -1;
        while (matcher.find()) {
            if (!PREFIX_EVENT_NAME.toString().equals(matcher.group())) {
                throw new ParseException(MESSAGE_UNKNOWN_PARAMETER);
            }
            if (valueStart >= 0) {
                throw new ParseException(MESSAGE_REPEATED_PARAMETER);
            }
            if (!args.substring(0, matcher.start()).isBlank()) {
                throw invalidFormat();
            }
            valueStart = matcher.end();
        }
        if (valueStart < 0 || args.substring(valueStart).isBlank()) {
            throw invalidFormat();
        }
        return new DeleteEventCommand(args.substring(valueStart));
    }

    private ParseException invalidFormat() {
        return new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteEventCommand.MESSAGE_USAGE));
    }
}
