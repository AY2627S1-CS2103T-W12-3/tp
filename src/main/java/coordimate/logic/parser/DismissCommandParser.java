package coordimate.logic.parser;

import static java.util.Objects.requireNonNull;

import coordimate.commons.core.index.Index;
import coordimate.logic.commands.DismissCommand;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.logic.reminder.ReminderService;

/**
 * Parses a single 1-based reminder index.
 */
public class DismissCommandParser implements Parser<DismissCommand> {
    private final ReminderService reminderService;

    /**
     * Creates a parser whose commands dismiss reminders through the supplied service.
     */
    public DismissCommandParser(ReminderService reminderService) {
        this.reminderService = requireNonNull(reminderService);
    }

    @Override
    public DismissCommand parse(String args) throws ParseException {
        requireNonNull(args);
        if (args.isBlank()) {
            throw ParserUtil.createInvalidFormatException(DismissCommand.MESSAGE_USAGE);
        }
        Index index = ParserUtil.parseIndex(args);
        return new DismissCommand(index, reminderService);
    }
}
