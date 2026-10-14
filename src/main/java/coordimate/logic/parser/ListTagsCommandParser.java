package coordimate.logic.parser;

import static java.util.Objects.requireNonNull;

import coordimate.logic.commands.ListTagsCommand;
import coordimate.logic.parser.exceptions.ParseException;

/**
 * Parses a list-tags command and rejects unexpected parameters.
 */
public class ListTagsCommandParser implements Parser<ListTagsCommand> {

    @Override
    public ListTagsCommand parse(String args) throws ParseException {
        requireNonNull(args);
        if (!args.isBlank()) {
            throw ParserUtil.createInvalidFormatException(ListTagsCommand.MESSAGE_USAGE);
        }
        return new ListTagsCommand();
    }
}
