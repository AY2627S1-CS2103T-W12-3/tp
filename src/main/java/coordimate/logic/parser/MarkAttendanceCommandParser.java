package coordimate.logic.parser;

import static coordimate.logic.parser.CliSyntax.PREFIX_ATTENDANCE;
import static coordimate.logic.parser.CliSyntax.PREFIX_EVENT_NAME;
import static coordimate.logic.parser.CliSyntax.PREFIX_MEMBER;
import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Set;

import coordimate.logic.Messages;
import coordimate.logic.commands.MarkAttendanceCommand;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.event.AttendanceStatus;

/**
 * Parses an event name, member name, and attendance status, rejecting unknown or repeated parameters.
 */
public class MarkAttendanceCommandParser implements Parser<MarkAttendanceCommand> {

    public static final String MESSAGE_UNKNOWN_PARAMETER =
            "Unknown parameter.\n" + MarkAttendanceCommand.MESSAGE_USAGE;
    public static final String MESSAGE_REPEATED_PARAMETER =
            Messages.MESSAGE_REPEATED_PARAMETER + "\n" + MarkAttendanceCommand.MESSAGE_USAGE;
    public static final String MESSAGE_INVALID_STATUS = AttendanceStatus.MESSAGE_CONSTRAINTS;

    private static final Set<String> REQUIRED = Set.of(PREFIX_EVENT_NAME.toString(), PREFIX_MEMBER.toString(),
            PREFIX_ATTENDANCE.toString());

    @Override
    public MarkAttendanceCommand parse(String args) throws ParseException {
        requireNonNull(args);
        Map<String, String> values = PrefixParser.parseUniqueParameters(args,
                PrefixParser.CONTACT_PARAMETER_PATTERN, REQUIRED, MarkAttendanceCommand.MESSAGE_USAGE,
                MESSAGE_UNKNOWN_PARAMETER, MESSAGE_REPEATED_PARAMETER);

        if (!values.containsKey(PREFIX_EVENT_NAME.toString()) || !values.containsKey(PREFIX_MEMBER.toString())
                || !values.containsKey(PREFIX_ATTENDANCE.toString())) {
            throw createInvalidFormatException();
        }

        String eventName = ParserUtil.parseEventName(values.get(PREFIX_EVENT_NAME.toString()));
        String memberName = values.get(PREFIX_MEMBER.toString());
        String statusText = values.get(PREFIX_ATTENDANCE.toString());

        if (memberName.isEmpty()) {
            throw createInvalidFormatException();
        }

        AttendanceStatus status;
        try {
            status = AttendanceStatus.fromString(statusText);
        } catch (IllegalArgumentException e) {
            throw new ParseException(MESSAGE_INVALID_STATUS);
        }

        return new MarkAttendanceCommand(eventName, ParserUtil.parseName(memberName), status);
    }

    private ParseException createInvalidFormatException() {
        return ParserUtil.createInvalidFormatException(MarkAttendanceCommand.MESSAGE_USAGE);
    }
}
