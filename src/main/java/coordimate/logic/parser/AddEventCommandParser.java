package coordimate.logic.parser;

import static coordimate.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
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
 * Accepts the documented et/ prefix and the /et spelling used in the examples.
 */
public class AddEventCommandParser implements Parser<AddEventCommand> {
    public static final String MESSAGE_UNKNOWN_PARAMETER = "Unknown parameter. Example: r/Logistics";
    public static final String MESSAGE_REPEATED_PARAMETER = "Each required parameter may only be specified once.";

    private static final Pattern PARAMETER = Pattern.compile("(?<!\\S)([A-Za-z]+/|/[A-Za-z]+)");
    private static final Set<String> REQUIRED = Set.of("evn/", "st/", "et/");

    @Override
    public AddEventCommand parse(String args) throws ParseException {
        requireNonNull(args);
        Map<String, String> values = new LinkedHashMap<>();
        Matcher matcher = PARAMETER.matcher(args);
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
            if (prefix.equals("/et")) {
                prefix = "et/";
            }
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
        if (!values.containsKey("evn/")) {
            throw invalidFormat();
        }
        String name = values.get("evn/");
        String start = values.getOrDefault("st/", "");
        String end = values.getOrDefault("et/", "");
        if (name.isEmpty()) {
            throw new ParseException(Event.MESSAGE_EMPTY_NAME);
        }
        if (start.isEmpty() || end.isEmpty()) {
            throw new ParseException(EventTime.MESSAGE_EMPTY);
        }
        try {
            return new AddEventCommand(new Event(name, new EventTime(start), new EventTime(end)));
        } catch (IllegalArgumentException e) {
            throw new ParseException(e.getMessage(), e);
        }
    }

    private ParseException invalidFormat() {
        return new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, AddEventCommand.MESSAGE_USAGE));
    }
}
