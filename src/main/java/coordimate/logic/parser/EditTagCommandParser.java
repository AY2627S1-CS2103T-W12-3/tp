package coordimate.logic.parser;

import static coordimate.logic.parser.CliSyntax.PREFIX_TAG;
import static java.util.Objects.requireNonNull;

import java.util.Set;

import coordimate.logic.Messages;
import coordimate.logic.commands.EditTagCommand;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.tag.Tag;

/**
 * Parses input arguments and creates an {@link EditTagCommand}.
 */
public class EditTagCommandParser implements Parser<EditTagCommand> {
    public static final String MESSAGE_UNKNOWN_PARAMETER =
            Messages.MESSAGE_UNKNOWN_PARAMETER + "\n" + EditTagCommand.MESSAGE_USAGE;
    public static final String MESSAGE_REPEATED_PARAMETER =
            Messages.MESSAGE_REPEATED_PARAMETER + "\n" + EditTagCommand.MESSAGE_USAGE;

    @Override
    public EditTagCommand parse(String args) throws ParseException {
        requireNonNull(args);
        PrefixParser.rejectUnknownParameters(args, PrefixParser.CONTACT_PARAMETER_PATTERN,
                Set.of(PREFIX_TAG.toString()), MESSAGE_UNKNOWN_PARAMETER);
        ArgumentMultimap argMultimap = ArgumentTokenizer.tokenize(args, PREFIX_TAG);
        if (argMultimap.getAllValues(PREFIX_TAG).isEmpty()) {
            throw ParserUtil.createInvalidFormatException(EditTagCommand.MESSAGE_USAGE);
        }
        if (argMultimap.getPreamble().isEmpty()) {
            throw ParserUtil.createInvalidFormatException(EditTagCommand.MESSAGE_USAGE);
        }
        if (argMultimap.getAllValues(PREFIX_TAG).size() > 1) {
            throw new ParseException(String.format(MESSAGE_REPEATED_PARAMETER, PREFIX_TAG));
        }

        String newTagName = argMultimap.getValue(PREFIX_TAG).orElseThrow();
        ParserUtil.requireValue(newTagName, "Tag name", "t/Publicity");
        Tag editedTag = ParserUtil.parseTag(newTagName);
        return new EditTagCommand(argMultimap.getPreamble(), editedTag);
    }
}
