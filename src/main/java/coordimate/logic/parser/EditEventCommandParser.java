package coordimate.logic.parser;

import static coordimate.logic.parser.CliSyntax.PREFIX_END_TIME;
import static coordimate.logic.parser.CliSyntax.PREFIX_EVENT_NAME;
import static coordimate.logic.parser.CliSyntax.PREFIX_NEW_EVENT_NAME;
import static coordimate.logic.parser.CliSyntax.PREFIX_START_TIME;
import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Set;

import coordimate.logic.Messages;
import coordimate.logic.commands.EditEventCommand;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.event.EventTime;

/**
 * Parses event edits while rejecting unknown or repeated parameters.
 */
public class EditEventCommandParser implements Parser<EditEventCommand> {
    public static final String MESSAGE_UNKNOWN_PARAMETER = "Unknown parameter.\n" + EditEventCommand.MESSAGE_USAGE;
    public static final String MESSAGE_REPEATED_PARAMETER =
            Messages.MESSAGE_REPEATED_PARAMETER + "\n" + EditEventCommand.MESSAGE_USAGE;

    private static final Set<String> PREFIXES = Set.of(PREFIX_EVENT_NAME.toString(), PREFIX_NEW_EVENT_NAME.toString(),
            PREFIX_START_TIME.toString(), PREFIX_END_TIME.toString());

    @Override
    public EditEventCommand parse(String args) throws ParseException {
        requireNonNull(args);
        Map<String, String> values = PrefixParser.parseUniqueParameters(args,
                PrefixParser.EVENT_PARAMETER_PATTERN, PREFIXES, EditEventCommand.MESSAGE_USAGE,
                MESSAGE_UNKNOWN_PARAMETER, MESSAGE_REPEATED_PARAMETER);
        String eventName = values.get(PREFIX_EVENT_NAME.toString());
        if (eventName == null || eventName.isEmpty()) {
            throw createInvalidFormatException();
        }
        if (values.size() == 1) {
            throw new ParseException(EditEventCommand.MESSAGE_NOT_EDITED);
        }
        String newName = values.get(PREFIX_NEW_EVENT_NAME.toString());
        if (newName != null && newName.isEmpty()) {
            throw new ParseException(EditEventCommand.MESSAGE_EMPTY_NEW_NAME);
        }
        return new EditEventCommand(ParserUtil.parseEventName(eventName),
                newName == null ? null : ParserUtil.parseEventName(newName),
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

    private ParseException createInvalidFormatException() {
        return ParserUtil.createInvalidFormatException(EditEventCommand.MESSAGE_USAGE);
    }
}
