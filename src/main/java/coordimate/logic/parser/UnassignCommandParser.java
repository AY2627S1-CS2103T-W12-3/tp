package coordimate.logic.parser;

import coordimate.logic.commands.UnassignCommand;
import coordimate.logic.parser.exceptions.ParseException;

/**
 * Parses an event name and space-separated contact indexes, rejecting unknown or repeated parameters.
 */
public class UnassignCommandParser implements Parser<UnassignCommand> {
    public static final String MESSAGE_UNKNOWN_PARAMETER =
            "Unknown parameter. Example: unassign evn/Final Concert c/2 3";
    public static final String MESSAGE_REPEATED_PARAMETER = EventContactsParser.MESSAGE_REPEATED_PARAMETER;
    public static final String MESSAGE_NO_CONTACTS = "Please specify at least one contact to remove.";
    public static final String MESSAGE_INVALID_CONTACT_INDEXES = EventContactsParser.MESSAGE_INVALID_CONTACT_INDEXES;

    @Override
    public UnassignCommand parse(String args) throws ParseException {
        EventContactsParser.Parsed parsed = EventContactsParser.parse(args, UnassignCommand.MESSAGE_USAGE,
                MESSAGE_UNKNOWN_PARAMETER, MESSAGE_NO_CONTACTS);
        return new UnassignCommand(parsed.eventName(), parsed.contactIndexes());
    }
}
