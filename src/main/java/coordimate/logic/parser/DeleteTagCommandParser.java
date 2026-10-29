package coordimate.logic.parser;

import static coordimate.logic.parser.CliSyntax.PREFIX_TAG;

import coordimate.logic.commands.DeleteTagCommand;
import coordimate.logic.parser.exceptions.ParseException;

/**
 * Parses input arguments and creates a {@link DeleteTagCommand}.
 */
public class DeleteTagCommandParser implements Parser<DeleteTagCommand> {

    @Override
    public DeleteTagCommand parse(String args) throws ParseException {
        ArgumentMultimap argMultimap = TagParserUtil.tokenizeTagArgs(args, DeleteTagCommand.MESSAGE_USAGE, false);
        return new DeleteTagCommand(argMultimap.getValue(PREFIX_TAG).orElseThrow());
    }
}
