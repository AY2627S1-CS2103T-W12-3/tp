package coordimate.logic.parser;

import static coordimate.logic.parser.CliSyntax.PREFIX_TAG;

import coordimate.logic.commands.AddTagCommand;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.tag.Tag;

/**
 * Parses input arguments and creates an {@link AddTagCommand}.
 */
public class AddTagCommandParser implements Parser<AddTagCommand> {

    @Override
    public AddTagCommand parse(String args) throws ParseException {
        ArgumentMultimap argMultimap = TagParserUtil.tokenizeTagArgs(args, AddTagCommand.MESSAGE_USAGE, false);
        Tag tag = ParserUtil.parseTag(argMultimap.getValue(PREFIX_TAG).orElseThrow());
        return new AddTagCommand(tag);
    }
}
