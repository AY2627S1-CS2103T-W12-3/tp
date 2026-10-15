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
            Messages.MESSAGE_UNKNOWN_PARAMETER + "\n" + AddEventCommand.MESSAGE_USAGE;
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
        String name = ParserUtil.parseEventName(values.get(PREFIX_EVENT_NAME.toString()));
        if (!values.containsKey(PREFIX_START_TIME.toString()) || !values.containsKey(PREFIX_END_TIME.toString())) {
            throw createInvalidFormatException();
        }
        EventTime start = ParserUtil.parseEventTime(values.get(PREFIX_START_TIME.toString()), PREFIX_START_TIME);
        EventTime end = ParserUtil.parseEventTime(values.get(PREFIX_END_TIME.toString()), PREFIX_END_TIME);
        try {
            return new AddEventCommand(new Event(name, start, end));
        } catch (IllegalArgumentException e) {
            throw new ParseException(e.getMessage(), e);
        }
    }

    private ParseException createInvalidFormatException() {
        return ParserUtil.createInvalidFormatException(AddEventCommand.MESSAGE_USAGE);
    }
}
