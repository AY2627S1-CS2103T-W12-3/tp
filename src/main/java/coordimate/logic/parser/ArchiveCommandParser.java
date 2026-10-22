package coordimate.logic.parser;

import static java.util.Objects.requireNonNull;

import coordimate.logic.commands.ArchiveCommand;
import coordimate.logic.parser.exceptions.ParseException;

/**
 * Parses a single permanent member ID for archiving or unarchiving.
 */
public class ArchiveCommandParser implements Parser<ArchiveCommand> {
    public static final String MESSAGE_INVALID_MEMBER_ID = "Invalid member ID. Example: %s 2";

    private final boolean archive;

    /**
     * Creates a parser for {@code archive} if {@code archive} is true, or {@code unarchive} otherwise.
     */
    public ArchiveCommandParser(boolean archive) {
        this.archive = archive;
    }

    @Override
    public ArchiveCommand parse(String args) throws ParseException {
        requireNonNull(args);
        if (args.isBlank()) {
            throw ParserUtil.createInvalidFormatException(archive
                    ? ArchiveCommand.MESSAGE_USAGE : ArchiveCommand.MESSAGE_UNARCHIVE_USAGE);
        }
        try {
            return new ArchiveCommand(ParserUtil.parseMemberId(args), archive);
        } catch (ParseException pe) {
            throw new ParseException(String.format(MESSAGE_INVALID_MEMBER_ID,
                    archive ? ArchiveCommand.COMMAND_WORD : ArchiveCommand.UNARCHIVE_COMMAND_WORD), pe);
        }
    }
}
