package coordimate.logic.parser;

import coordimate.logic.commands.AssignCommand;
import coordimate.logic.parser.exceptions.ParseException;

/**
 * Parses an event name and space-separated contact indexes, rejecting unknown or repeated parameters.
 */
public class AssignCommandParser implements Parser<AssignCommand> {
    public static final String MESSAGE_UNKNOWN_PARAMETER = "Unknown parameter.\n" + AssignCommand.MESSAGE_USAGE;
    public static final String MESSAGE_REPEATED_PARAMETER =
            EventContactsParser.MESSAGE_REPEATED_PARAMETER + "\n" + AssignCommand.MESSAGE_USAGE;
    public static final String MESSAGE_NO_CONTACTS = "Specify at least one contact to assign.\n"
            + AssignCommand.MESSAGE_USAGE;
    public static final String MESSAGE_INVALID_CONTACT_INDEXES = EventContactsParser.MESSAGE_INVALID_CONTACT_INDEXES;

    @Override
    public AssignCommand parse(String args) throws ParseException {
        EventContactsParser.Parsed parsed = EventContactsParser.parse(args, AssignCommand.MESSAGE_USAGE,
                MESSAGE_UNKNOWN_PARAMETER, MESSAGE_NO_CONTACTS);
        return new AssignCommand(parsed.eventName(), parsed.contactIndexes());
    }
}
