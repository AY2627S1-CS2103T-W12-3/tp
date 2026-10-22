package coordimate.logic.parser;

import static coordimate.logic.parser.CliSyntax.PREFIX_EVENT_NAME;

import java.util.Map;
import java.util.Set;

import coordimate.logic.Messages;
import coordimate.logic.commands.ArchiveEventCommand;
import coordimate.logic.parser.exceptions.ParseException;

/**
 * Parses an event name for archiving or unarchiving, rejecting unknown or repeated parameters.
 */
public class ArchiveEventCommandParser implements Parser<ArchiveEventCommand> {
    private final boolean archive;
    private final String usage;

    /**
     * Creates a parser for {@code archiveevent} if {@code archive} is true, or {@code unarchiveevent} otherwise.
     */
    public ArchiveEventCommandParser(boolean archive) {
        this.archive = archive;
        usage = archive ? ArchiveEventCommand.MESSAGE_USAGE : ArchiveEventCommand.MESSAGE_UNARCHIVE_USAGE;
    }

    @Override
    public ArchiveEventCommand parse(String args) throws ParseException {
        Map<String, String> values = PrefixParser.parseUniqueParameters(args, PrefixParser.EVENT_PARAMETER_PATTERN,
                Set.of(PREFIX_EVENT_NAME.toString()), usage, Messages.MESSAGE_UNKNOWN_PARAMETER + "\n" + usage,
                Messages.MESSAGE_REPEATED_PARAMETER + "\n" + usage);
        String eventName = values.get(PREFIX_EVENT_NAME.toString());
        if (eventName == null) {
            throw ParserUtil.createInvalidFormatException(usage);
        }
        return new ArchiveEventCommand(ParserUtil.parseEventName(eventName), archive);
    }
}
