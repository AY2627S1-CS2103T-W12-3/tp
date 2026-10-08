package coordimate.logic.parser;

import coordimate.logic.commands.MembersCommand;
import coordimate.logic.parser.exceptions.ParseException;

/**
 * Parses an event name, rejecting unknown or repeated parameters.
 */
public class MembersCommandParser implements Parser<MembersCommand> {
    public static final String MESSAGE_UNKNOWN_PARAMETER = "Unknown parameter. Example: members evn/Final Concert";
    public static final String MESSAGE_REPEATED_PARAMETER = EventContactsParser.MESSAGE_REPEATED_PARAMETER;

    @Override
    public MembersCommand parse(String args) throws ParseException {
        return new MembersCommand(EventContactsParser.parseEventName(args, MembersCommand.MESSAGE_USAGE,
                MESSAGE_UNKNOWN_PARAMETER));
    }
}
