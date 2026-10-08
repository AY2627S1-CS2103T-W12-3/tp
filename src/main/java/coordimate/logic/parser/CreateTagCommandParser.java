package coordimate.logic.parser;

import static coordimate.logic.parser.CliSyntax.PREFIX_TAG;
import static java.util.Objects.requireNonNull;

import coordimate.logic.commands.CreateTagCommand;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.tag.Tag;

/**
 * Parses input arguments and creates a {@link CreateTagCommand}.
 */
public class CreateTagCommandParser implements Parser<CreateTagCommand> {

    public static final String MESSAGE_NO_PARAMETERS = "No parameters given.";
    public static final String MESSAGE_NO_PREFIX = "No prefix given.";
    public static final String MESSAGE_MULTIPLE_TAG_NAMES = "Multiple tag names given.";
    public static final String MESSAGE_UNKNOWN_PARAMETERS = "Unknown parameters given.";
    public static final String MESSAGE_EMPTY_TAG_NAME = "Tag name cannot be empty.";

    @Override
    public CreateTagCommand parse(String args) throws ParseException {
        requireNonNull(args);
        if (args.isBlank()) {
            throw new ParseException(MESSAGE_NO_PARAMETERS);
        }

        ArgumentMultimap argMultimap = ArgumentTokenizer.tokenize(args, PREFIX_TAG);
        if (argMultimap.getAllValues(PREFIX_TAG).isEmpty()) {
            throw new ParseException(MESSAGE_NO_PREFIX);
        }
        if (!argMultimap.getPreamble().isEmpty()) {
            throw new ParseException(MESSAGE_UNKNOWN_PARAMETERS);
        }
        if (argMultimap.getAllValues(PREFIX_TAG).size() > 1) {
            throw new ParseException(MESSAGE_MULTIPLE_TAG_NAMES);
        }

        String tagName = argMultimap.getValue(PREFIX_TAG).orElseThrow();
        if (tagName.isEmpty()) {
            throw new ParseException(MESSAGE_EMPTY_TAG_NAME);
        }
        Tag tag = ParserUtil.parseTag(tagName);
        return new CreateTagCommand(tag);
    }
}
