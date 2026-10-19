package coordimate.logic.parser;

import static java.util.Objects.requireNonNull;

import coordimate.logic.commands.SwitchCommand;
import coordimate.logic.commands.SwitchCommand.View;
import coordimate.logic.parser.exceptions.ParseException;

/**
 * Parses the view name for a switch command.
 */
public class SwitchCommandParser implements Parser<SwitchCommand> {

    @Override
    public SwitchCommand parse(String args) throws ParseException {
        String viewName = requireNonNull(args).strip();
        return switch (viewName) {
            case "contacts" -> new SwitchCommand(View.CONTACTS);
            case "events" -> new SwitchCommand(View.EVENTS);
            case "tags" -> new SwitchCommand(View.TAGS);
            default -> throw ParserUtil.createInvalidFormatException(SwitchCommand.MESSAGE_USAGE);
        };
    }
}
