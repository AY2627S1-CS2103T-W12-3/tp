package coordimate.logic.parser;

import static coordimate.logic.parser.CliSyntax.PREFIX_TAG;
import static java.util.Objects.requireNonNull;

import java.util.Set;

import coordimate.logic.Messages;
import coordimate.logic.commands.CreateTagCommand;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.tag.Tag;

/**
 * Parses input arguments and creates a {@link CreateTagCommand}.
 */
public class CreateTagCommandParser implements Parser<CreateTagCommand> {
    public static final String MESSAGE_UNKNOWN_PARAMETER =
            Messages.MESSAGE_UNKNOWN_PARAMETER + "\n" + CreateTagCommand.MESSAGE_USAGE;
    public static final String MESSAGE_REPEATED_PARAMETER =
            Messages.MESSAGE_REPEATED_PARAMETER + "\n" + CreateTagCommand.MESSAGE_USAGE;

    @Override
    public CreateTagCommand parse(String args) throws ParseException {
        requireNonNull(args);
        PrefixParser.rejectUnknownParameters(args, PrefixParser.CONTACT_PARAMETER_PATTERN,
                Set.of(PREFIX_TAG.toString()), MESSAGE_UNKNOWN_PARAMETER);
        if (args.isBlank()) {
            throw ParserUtil.createInvalidFormatException(CreateTagCommand.MESSAGE_USAGE);
        }

        ArgumentMultimap argMultimap = ArgumentTokenizer.tokenize(args, PREFIX_TAG);
        if (argMultimap.getAllValues(PREFIX_TAG).isEmpty()) {
            throw ParserUtil.createInvalidFormatException(CreateTagCommand.MESSAGE_USAGE);
        }
        if (!argMultimap.getPreamble().isEmpty()) {
            throw ParserUtil.createInvalidFormatException(CreateTagCommand.MESSAGE_USAGE);
        }
        if (argMultimap.getAllValues(PREFIX_TAG).size() > 1) {
            throw new ParseException(String.format(MESSAGE_REPEATED_PARAMETER, PREFIX_TAG));
        }

        String tagName = argMultimap.getValue(PREFIX_TAG).orElseThrow();
        ParserUtil.requireValue(tagName, "Tag name", "t/Publicity");
        Tag tag = ParserUtil.parseTag(tagName);
        return new CreateTagCommand(tag);
    }
}
