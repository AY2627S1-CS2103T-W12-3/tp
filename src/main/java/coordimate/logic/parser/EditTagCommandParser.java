package coordimate.logic.parser;

import static coordimate.logic.parser.CliSyntax.PREFIX_TAG;

import coordimate.logic.commands.EditTagCommand;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.tag.Tag;

/**
 * Parses input arguments and creates an {@link EditTagCommand}.
 */
public class EditTagCommandParser implements Parser<EditTagCommand> {

    @Override
    public EditTagCommand parse(String args) throws ParseException {
        ArgumentMultimap argMultimap = TagParserUtil.tokenizeTagArgs(args, EditTagCommand.MESSAGE_USAGE, true);
        Tag editedTag = ParserUtil.parseTag(argMultimap.getValue(PREFIX_TAG).orElseThrow());
        return new EditTagCommand(argMultimap.getPreamble(), editedTag);
    }
}
