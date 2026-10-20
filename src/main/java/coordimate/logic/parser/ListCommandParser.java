package coordimate.logic.parser;

import static java.util.Objects.requireNonNull;

import java.util.Locale;

import coordimate.logic.commands.ListCommand;
import coordimate.logic.parser.exceptions.ParseException;

/**
 * Parses {@code list} with no parameters, or {@code list archived} in any letter case.
 */
public class ListCommandParser implements Parser<ListCommand> {
    public static final String MESSAGE_UNKNOWN_OPTION = "Unknown list option: %s. Use list, or list "
            + ListCommand.ARCHIVED_OPTION + " to show archived contacts and events.";

    @Override
    public ListCommand parse(String args) throws ParseException {
        requireNonNull(args);
        String option = args.strip();
        if (option.isEmpty()) {
            return new ListCommand(false);
        }
        if (option.toLowerCase(Locale.ROOT).equals(ListCommand.ARCHIVED_OPTION)) {
            return new ListCommand(true);
        }
        throw new ParseException(String.format(MESSAGE_UNKNOWN_OPTION, option));
    }
}
