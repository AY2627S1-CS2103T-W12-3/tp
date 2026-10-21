package coordimate.logic.parser;

import coordimate.logic.Messages;
import coordimate.logic.commands.UnassignCommand;
import coordimate.logic.parser.exceptions.ParseException;

/**
 * Parses an event name and space-separated member IDs, rejecting unknown or repeated parameters.
 */
public class UnassignCommandParser implements Parser<UnassignCommand> {
    public static final String MESSAGE_UNKNOWN_PARAMETER =
            Messages.MESSAGE_UNKNOWN_PARAMETER + "\n" + UnassignCommand.MESSAGE_USAGE;
    public static final String MESSAGE_REPEATED_PARAMETER =
            EventContactsParser.MESSAGE_REPEATED_PARAMETER + "\n" + UnassignCommand.MESSAGE_USAGE;
    public static final String MESSAGE_NO_CONTACTS = "Member IDs must not be empty. Example: c/1 3 5";
    public static final String MESSAGE_INVALID_MEMBER_IDS = EventContactsParser.MESSAGE_INVALID_MEMBER_IDS;

    @Override
    public UnassignCommand parse(String args) throws ParseException {
        EventContactsParser.Parsed parsed = EventContactsParser.parse(args, UnassignCommand.MESSAGE_USAGE,
                MESSAGE_UNKNOWN_PARAMETER, MESSAGE_NO_CONTACTS);
        return new UnassignCommand(parsed.eventName(), parsed.memberIds());
    }
}
