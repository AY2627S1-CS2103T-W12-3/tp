package coordimate.logic.parser;

import coordimate.logic.Messages;
import coordimate.logic.commands.MembersCommand;
import coordimate.logic.parser.exceptions.ParseException;

/**
 * Parses an event name, rejecting unknown or repeated parameters.
 */
public class MembersCommandParser implements Parser<MembersCommand> {
    public static final String MESSAGE_UNKNOWN_PARAMETER =
            Messages.MESSAGE_UNKNOWN_PARAMETER + "\n" + MembersCommand.MESSAGE_USAGE;
    public static final String MESSAGE_REPEATED_PARAMETER =
            EventContactsParser.MESSAGE_REPEATED_PARAMETER + "\n" + MembersCommand.MESSAGE_USAGE;

    @Override
    public MembersCommand parse(String args) throws ParseException {
        return new MembersCommand(EventContactsParser.parseEventName(args, MembersCommand.MESSAGE_USAGE,
                MESSAGE_UNKNOWN_PARAMETER));
    }
}
