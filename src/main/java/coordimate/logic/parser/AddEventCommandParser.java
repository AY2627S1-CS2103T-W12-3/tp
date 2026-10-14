package coordimate.logic.parser;

import static coordimate.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static coordimate.logic.parser.CliSyntax.PREFIX_END_TIME;
import static coordimate.logic.parser.CliSyntax.PREFIX_EVENT_NAME;
import static coordimate.logic.parser.CliSyntax.PREFIX_START_TIME;
import static java.util.Objects.requireNonNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import coordimate.logic.commands.AddEventCommand;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;

/**
 * Parses the required fields of an add-event command.
 */
public class AddEventCommandParser implements Parser<AddEventCommand> {
    public static final String MESSAGE_UNKNOWN_PARAMETER = "Unknown parameter. Example: r/Logistics";
    public static final String MESSAGE_REPEATED_PARAMETER = "Each required parameter may only be specified once.";

    private static final Pattern PARAMETER_PATTERN = Pattern.compile("(?<!\\S)([A-Za-z]+/|/[A-Za-z]+)");
    private static final Set<String> REQUIRED_PREFIXES = Set.of(PREFIX_EVENT_NAME.toString(),
            PREFIX_START_TIME.toString(), PREFIX_END_TIME.toString());

    @Override
    public AddEventCommand parse(String args) throws ParseException {
        requireNonNull(args);
        Map<String, String> values = new LinkedHashMap<>();
        Matcher matcher = PARAMETER_PATTERN.matcher(args);
        String previousPrefix = null;
        int valueStart = 0;
        while (matcher.find()) {
            if (previousPrefix == null && !args.substring(0, matcher.start()).isBlank()) {
                throw createInvalidFormatException();
            }
            if (previousPrefix != null) {
                values.put(previousPrefix, args.substring(valueStart, matcher.start()).strip());
            }
            String prefix = matcher.group();
            if (!REQUIRED_PREFIXES.contains(prefix)) {
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
        if (!values.containsKey(PREFIX_EVENT_NAME.toString())) {
            throw createInvalidFormatException();
        }
        String name = ParserUtil.parseEventName(values.get(PREFIX_EVENT_NAME.toString()));
        String start = values.getOrDefault(PREFIX_START_TIME.toString(), "");
        String end = values.getOrDefault(PREFIX_END_TIME.toString(), "");
        if (start.isEmpty() || end.isEmpty()) {
            throw new ParseException(EventTime.MESSAGE_EMPTY);
        }
        try {
            return new AddEventCommand(new Event(name, new EventTime(start), new EventTime(end)));
        } catch (IllegalArgumentException e) {
            throw new ParseException(e.getMessage(), e);
        }
    }

    private ParseException createInvalidFormatException() {
        return new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, AddEventCommand.MESSAGE_USAGE));
    }
}
