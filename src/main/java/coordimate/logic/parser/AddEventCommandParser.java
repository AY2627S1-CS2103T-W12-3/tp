package coordimate.logic.parser;

import static coordimate.logic.parser.CliSyntax.PREFIX_END_TIME;
import static coordimate.logic.parser.CliSyntax.PREFIX_EVENT_NAME;
import static coordimate.logic.parser.CliSyntax.PREFIX_START_TIME;
import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Set;

import coordimate.logic.Messages;
import coordimate.logic.commands.AddEventCommand;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;

/**
 * Parses the required fields of an add-event command.
 */
public class AddEventCommandParser implements Parser<AddEventCommand> {
    public static final String MESSAGE_UNKNOWN_PARAMETER =
            "Unknown parameter.\n" + AddEventCommand.MESSAGE_USAGE;
    public static final String MESSAGE_REPEATED_PARAMETER =
            Messages.MESSAGE_REPEATED_REQUIRED_PARAMETER + "\n" + AddEventCommand.MESSAGE_USAGE;

    private static final Set<String> REQUIRED_PREFIXES = Set.of(PREFIX_EVENT_NAME.toString(),
            PREFIX_START_TIME.toString(), PREFIX_END_TIME.toString());

    @Override
    public AddEventCommand parse(String args) throws ParseException {
        requireNonNull(args);
        Map<String, String> values = PrefixParser.parseUniqueParameters(args,
                PrefixParser.EVENT_PARAMETER_PATTERN, REQUIRED_PREFIXES, AddEventCommand.MESSAGE_USAGE,
                MESSAGE_UNKNOWN_PARAMETER, MESSAGE_REPEATED_PARAMETER);
        if (!values.containsKey(PREFIX_EVENT_NAME.toString())) {
            throw createInvalidFormatException();
        }
        String name = values.get(PREFIX_EVENT_NAME.toString());
        String start = values.getOrDefault(PREFIX_START_TIME.toString(), "");
        String end = values.getOrDefault(PREFIX_END_TIME.toString(), "");
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

    private ParseException createInvalidFormatException() {
        return ParserUtil.createInvalidFormatException(AddEventCommand.MESSAGE_USAGE);
    }
}
