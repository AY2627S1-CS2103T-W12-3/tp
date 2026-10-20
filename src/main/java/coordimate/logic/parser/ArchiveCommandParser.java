package coordimate.logic.parser;

import static java.util.Objects.requireNonNull;

import coordimate.logic.commands.ArchiveCommand;
import coordimate.logic.parser.exceptions.ParseException;

/**
 * Parses a single 1-based contact index for archiving or unarchiving.
 */
public class ArchiveCommandParser implements Parser<ArchiveCommand> {
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
        return new ArchiveCommand(ParserUtil.parseIndex(args), archive);
    }
}
