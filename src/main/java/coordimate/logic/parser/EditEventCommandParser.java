package coordimate.logic.parser;

import static coordimate.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static coordimate.logic.parser.CliSyntax.PREFIX_END_TIME;
import static coordimate.logic.parser.CliSyntax.PREFIX_EVENT_NAME;
import static coordimate.logic.parser.CliSyntax.PREFIX_NEW_EVENT_NAME;
import static coordimate.logic.parser.CliSyntax.PREFIX_START_TIME;
import static java.util.Objects.requireNonNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import coordimate.logic.commands.EditEventCommand;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.event.EventTime;

/**
 * Parses event edits while rejecting unknown or repeated parameters.
 */
public class EditEventCommandParser implements Parser<EditEventCommand> {
    public static final String MESSAGE_UNKNOWN_PARAMETER = "Unknown parameter. "
            + "Example: editevent evn/Final Concert st/08-08-2026 16:00";
    public static final String MESSAGE_REPEATED_PARAMETER = "Each parameter may only be specified once.";

    private static final Pattern PARAMETER_PATTERN = Pattern.compile("(?<!\\S)([A-Za-z]+/|/[A-Za-z]+)");
    private static final Set<String> PREFIXES = Set.of(PREFIX_EVENT_NAME.toString(), PREFIX_NEW_EVENT_NAME.toString(),
            PREFIX_START_TIME.toString(), PREFIX_END_TIME.toString());

    @Override
    public EditEventCommand parse(String args) throws ParseException {
        requireNonNull(args);
        Map<String, String> values = new LinkedHashMap<>();
        Matcher matcher = PARAMETER_PATTERN.matcher(args);
        String previousPrefix = null;
        int valueStart = 0;
        while (matcher.find()) {
            if (previousPrefix == null && !args.substring(0, matcher.start()).isBlank()) {
                throw invalidFormat();
            }
            if (previousPrefix != null) {
                values.put(previousPrefix, args.substring(valueStart, matcher.start()).strip());
            }
            String prefix = matcher.group();
            if (!PREFIXES.contains(prefix)) {
                throw new ParseException(MESSAGE_UNKNOWN_PARAMETER);
            }
            if (values.containsKey(prefix)) {
                throw new ParseException(MESSAGE_REPEATED_PARAMETER);
            }
            previousPrefix = prefix;
            valueStart = matcher.end();
        }
        if (previousPrefix != null) {
            values.put(previousPrefix, args.substring(valueStart).strip());
        }
        String eventName = values.get(PREFIX_EVENT_NAME.toString());
        if (eventName == null || eventName.isEmpty()) {
            throw invalidFormat();
        }
        if (values.size() == 1) {
            throw new ParseException(EditEventCommand.MESSAGE_NOT_EDITED);
        }
        String newName = values.get(PREFIX_NEW_EVENT_NAME.toString());
        if (newName != null && newName.isEmpty()) {
            throw new ParseException(EditEventCommand.MESSAGE_EMPTY_NEW_NAME);
        }
        return new EditEventCommand(eventName, newName,
                parseTime(values.get(PREFIX_START_TIME.toString())),
                parseTime(values.get(PREFIX_END_TIME.toString())));
    }

    private EventTime parseTime(String value) throws ParseException {
        if (value == null) {
            return null;
        }
        try {
            return new EventTime(value);
        } catch (IllegalArgumentException e) {
            throw new ParseException(EditEventCommand.MESSAGE_TIME_FORMAT, e);
        }
    }

    private ParseException invalidFormat() {
        return new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, EditEventCommand.MESSAGE_USAGE));
    }
}
