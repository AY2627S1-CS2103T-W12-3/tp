package coordimate.logic.parser;

import static coordimate.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static coordimate.logic.Messages.MESSAGE_UNKNOWN_COMMAND;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import coordimate.commons.core.LogsCenter;
import coordimate.logic.commands.AddCommand;
import coordimate.logic.commands.AddEventCommand;
import coordimate.logic.commands.AssignCommand;
import coordimate.logic.commands.ClearCommand;
import coordimate.logic.commands.Command;
import coordimate.logic.commands.DeleteCommand;
import coordimate.logic.commands.DeleteEventCommand;
import coordimate.logic.commands.EditCommand;
import coordimate.logic.commands.EditEventCommand;
import coordimate.logic.commands.ExitCommand;
import coordimate.logic.commands.FilterCommand;
import coordimate.logic.commands.FindCommand;
import coordimate.logic.commands.HelpCommand;
import coordimate.logic.commands.ListCommand;
import coordimate.logic.parser.exceptions.ParseException;

/**
 * Parses user input.
 */
public class CoordiMateParser {

    /**
     * Used for initial separation of command word and args.
     */
    private static final Pattern BASIC_COMMAND_FORMAT = Pattern.compile("(?<commandWord>\\S+)(?<arguments>.*)");
    private static final Logger logger = LogsCenter.getLogger(CoordiMateParser.class);

    /**
     * Parses user input into command for execution.
     *
     * @param userInput full user input string.
     * @return the command based on the user input.
     * @throws ParseException if the user input does not conform to the expected format.
     */
    public Command parseCommand(String userInput) throws ParseException {
        final Matcher matcher = BASIC_COMMAND_FORMAT.matcher(userInput.trim());
        if (!matcher.matches()) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, HelpCommand.MESSAGE_USAGE));
        }

        final String commandWord = matcher.group("commandWord");
        final String arguments = matcher.group("arguments");

        // Note to developers: Change LOG_LEVEL in LogsCenter to enable lower level (i.e., FINE, FINER and lower)
        // log messages such as the one below.
        // Lower level log messages are used sparingly to minimize noise in the code.
        logger.fine("Command word: " + commandWord + "; Arguments: " + arguments);

        return switch (commandWord) {
            case AddCommand.COMMAND_WORD -> new AddCommandParser().parse(arguments);
            case AddEventCommand.COMMAND_WORD -> new AddEventCommandParser().parse(arguments);
            case EditCommand.COMMAND_WORD -> new EditCommandParser().parse(arguments);
            case EditEventCommand.COMMAND_WORD -> new EditEventCommandParser().parse(arguments);
            case AssignCommand.COMMAND_WORD -> new AssignCommandParser().parse(arguments);
            case DeleteCommand.COMMAND_WORD -> new DeleteCommandParser().parse(arguments);
            case DeleteEventCommand.COMMAND_WORD -> new DeleteEventCommandParser().parse(arguments);
            case ClearCommand.COMMAND_WORD -> new ClearCommand();
            case FindCommand.COMMAND_WORD -> new FindCommandParser().parse(arguments);
            case FilterCommand.COMMAND_WORD -> new FilterCommandParser().parse(arguments);
            case ListCommand.COMMAND_WORD -> new ListCommand();
            case ExitCommand.COMMAND_WORD -> new ExitCommand();
            case HelpCommand.COMMAND_WORD -> new HelpCommand();
            default -> {
                logger.finer("This user input caused a ParseException: " + userInput);
                throw new ParseException(MESSAGE_UNKNOWN_COMMAND);
            }
        };
    }

}
