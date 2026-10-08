package coordimate.logic.parser;

import static coordimate.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static java.util.Objects.requireNonNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import coordimate.logic.commands.MarkAttendanceCommand;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.event.AttendanceStatus;
import coordimate.model.event.Event;
import coordimate.model.person.Name;

/**
 * Parses an event name, member name, and attendance status, rejecting unknown or repeated parameters.
 */
public class MarkAttendanceCommandParser implements Parser<MarkAttendanceCommand> {

    public static final String MESSAGE_UNKNOWN_PARAMETER =
            "Unknown parameter. Example: markattendance evn/Final Concert mem/Alice Tan att/present";
    public static final String MESSAGE_REPEATED_PARAMETER = "Each parameter may only be specified once.";
    public static final String MESSAGE_INVALID_STATUS = "STATUS must be either \"present\" or \"absent\"!";

    private static final Pattern PARAMETER_PATTERN = Pattern.compile("(?<!\\S)([A-Za-z]+/)");
    private static final Set<String> REQUIRED = Set.of("evn/", "mem/", "att/");

    @Override
    public MarkAttendanceCommand parse(String args) throws ParseException {
        requireNonNull(args);
        Map<String, String> values = new LinkedHashMap<>();
        Matcher matcher = PARAMETER_PATTERN.matcher(args);
        String previous = null;
        int valueStart = 0;

        while (matcher.find()) {
            if (previous == null && !args.substring(0, matcher.start()).isBlank()) {
                throw invalidFormat();
            }
            if (previous != null) {
                values.put(previous, args.substring(valueStart, matcher.start()).strip());
            }
            String prefix = matcher.group();
            if (!REQUIRED.contains(prefix)) {
                throw new ParseException(MESSAGE_UNKNOWN_PARAMETER);
            }
            if (values.containsKey(prefix)) {
                throw new ParseException(MESSAGE_REPEATED_PARAMETER);
            }
            previous = prefix;
            valueStart = matcher.end();
        }
        if (previous != null) {
            values.put(previous, args.substring(valueStart).strip());
        }

        if (!values.containsKey("evn/") || !values.containsKey("mem/") || !values.containsKey("att/")) {
            throw invalidFormat();
        }

        String eventName = values.get("evn/");
        String memberName = values.get("mem/");
        String statusText = values.get("att/");

        if (eventName.isEmpty()) {
            throw new ParseException(Event.MESSAGE_EMPTY_NAME);
        }
        if (memberName.isEmpty()) {
            throw invalidFormat();
        }

        AttendanceStatus status;
        try {
            status = AttendanceStatus.fromString(statusText);
        } catch (IllegalArgumentException e) {
            throw new ParseException(MESSAGE_INVALID_STATUS);
        }

        return new MarkAttendanceCommand(eventName, new Name(memberName), status);
    }

    private ParseException invalidFormat() {
        return new ParseException(
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, MarkAttendanceCommand.MESSAGE_USAGE));
    }
}
