package coordimate.logic.parser;

import static coordimate.logic.parser.CliSyntax.PREFIX_TAG;
import static java.util.Objects.requireNonNull;

import coordimate.logic.commands.EditTagCommand;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.tag.Tag;

/**
 * Parses input arguments and creates an {@link EditTagCommand}.
 */
public class EditTagCommandParser implements Parser<EditTagCommand> {

    public static final String MESSAGE_NO_PREFIX = "No prefix given.";
    public static final String MESSAGE_CURRENT_TAG_NOT_DEFINED = "Current tag name is not defined.";
    public static final String MESSAGE_NEW_TAG_NOT_DEFINED = "New tag name is not defined.";
    public static final String MESSAGE_EMPTY_TAG_NAME = "Tag name cannot be empty.";
    public static final String MESSAGE_MULTIPLE_TAG_NAMES = "Multiple tag names given.";

    @Override
    public EditTagCommand parse(String args) throws ParseException {
        requireNonNull(args);
        ArgumentMultimap argMultimap = ArgumentTokenizer.tokenize(args, PREFIX_TAG);
        if (argMultimap.getAllValues(PREFIX_TAG).isEmpty()) {
            if (args.isBlank()) {
                throw new ParseException(MESSAGE_CURRENT_TAG_NOT_DEFINED);
            }
            throw new ParseException(MESSAGE_NO_PREFIX);
        }
        if (argMultimap.getPreamble().isEmpty()) {
            throw new ParseException(MESSAGE_CURRENT_TAG_NOT_DEFINED);
        }
        if (argMultimap.getAllValues(PREFIX_TAG).size() > 1) {
            throw new ParseException(MESSAGE_MULTIPLE_TAG_NAMES);
        }

        String newTagName = argMultimap.getValue(PREFIX_TAG).orElseThrow();
        if (newTagName.isEmpty()) {
            String errorMessage = args.endsWith(PREFIX_TAG.getPrefix())
                    ? MESSAGE_NEW_TAG_NOT_DEFINED
                    : MESSAGE_EMPTY_TAG_NAME;
            throw new ParseException(errorMessage);
        }
        Tag editedTag = ParserUtil.parseTag(newTagName);
        return new EditTagCommand(argMultimap.getPreamble(), editedTag);
    }
}
