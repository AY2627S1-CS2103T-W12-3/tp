package coordimate.logic.parser;

import static coordimate.logic.Messages.MESSAGE_UNKNOWN_COMMAND;
import static java.util.Objects.requireNonNull;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import coordimate.commons.core.LogsCenter;
import coordimate.commons.util.StringUtil;
import coordimate.logic.Messages;
import coordimate.logic.commands.AddCommand;
import coordimate.logic.commands.AddEventCommand;
import coordimate.logic.commands.AddTagCommand;
import coordimate.logic.commands.ArchiveCommand;
import coordimate.logic.commands.ArchiveEventCommand;
import coordimate.logic.commands.AssignCommand;
import coordimate.logic.commands.ClearCommand;
import coordimate.logic.commands.Command;
import coordimate.logic.commands.DeleteCommand;
import coordimate.logic.commands.DeleteEventCommand;
import coordimate.logic.commands.DeleteTagCommand;
import coordimate.logic.commands.DismissCommand;
import coordimate.logic.commands.EditCommand;
import coordimate.logic.commands.EditEventCommand;
import coordimate.logic.commands.EditTagCommand;
import coordimate.logic.commands.ExitCommand;
import coordimate.logic.commands.FilterCommand;
import coordimate.logic.commands.HelpCommand;
import coordimate.logic.commands.ListCommand;
import coordimate.logic.commands.MarkAttendanceCommand;
import coordimate.logic.commands.MembersCommand;
import coordimate.logic.commands.SearchCommand;
import coordimate.logic.commands.TutorialCommand;
import coordimate.logic.commands.UnassignCommand;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.logic.reminder.ReminderService;

/**
 * Parses user input.
 */
public class CoordiMateParser {

    /**
     * Used for initial separation of command word and args.
     */
    private static final Pattern BASIC_COMMAND_FORMAT = Pattern.compile(
            "(?<commandWord>[^" + StringUtil.WHITESPACE_CHARACTERS + "]+)(?<arguments>.*)");
    private static final Logger logger = LogsCenter.getLogger(CoordiMateParser.class);
    private final ReminderService reminderService;

    /**
     * Supplies the shared reminder service to commands that dismiss reminders.
     */
    public CoordiMateParser(ReminderService reminderService) {
        this.reminderService = requireNonNull(reminderService);
    }

    /**
     * Parses user input into command for execution.
     *
     * @param userInput full user input string.
     * @return the command based on the user input.
     * @throws ParseException if the user input does not conform to the expected format.
     */
    public Command parseCommand(String userInput) throws ParseException {
        final Matcher matcher = BASIC_COMMAND_FORMAT.matcher(StringUtil.stripWhitespace(userInput));
        if (!matcher.matches()) {
            throw ParserUtil.createInvalidFormatException(HelpCommand.MESSAGE_USAGE);
        }

        final String commandWord = matcher.group("commandWord");
        final String arguments = matcher.group("arguments");

        // Note to developers: Change LOG_LEVEL in LogsCenter to enable lower level (i.e., FINE, FINER and lower)
        // log messages such as the one below.
        // Lower level log messages are used sparingly to minimize noise in the code.
        logger.fine("Command word: " + commandWord + "; Arguments: " + arguments);

        try {
            return switch (commandWord) {
                case AddCommand.COMMAND_WORD -> new AddCommandParser().parse(arguments);
                case AddEventCommand.COMMAND_WORD -> new AddEventCommandParser().parse(arguments);
                case ArchiveCommand.COMMAND_WORD -> new ArchiveCommandParser(true).parse(arguments);
                case ArchiveCommand.UNARCHIVE_COMMAND_WORD -> new ArchiveCommandParser(false).parse(arguments);
                case ArchiveEventCommand.COMMAND_WORD -> new ArchiveEventCommandParser(true).parse(arguments);
                case ArchiveEventCommand.UNARCHIVE_COMMAND_WORD ->
                    new ArchiveEventCommandParser(false).parse(arguments);
                case EditCommand.COMMAND_WORD -> new EditCommandParser().parse(arguments);
                case EditEventCommand.COMMAND_WORD -> new EditEventCommandParser().parse(arguments);
                case EditTagCommand.COMMAND_WORD -> new EditTagCommandParser().parse(arguments);
                case AssignCommand.COMMAND_WORD -> new AssignCommandParser().parse(arguments);
                case UnassignCommand.COMMAND_WORD -> new UnassignCommandParser().parse(arguments);
                case MembersCommand.COMMAND_WORD -> new MembersCommandParser().parse(arguments);
                case DeleteCommand.COMMAND_WORD -> new DeleteCommandParser().parse(arguments);
                case DeleteEventCommand.COMMAND_WORD -> new DeleteEventCommandParser().parse(arguments);
                case DeleteTagCommand.COMMAND_WORD -> new DeleteTagCommandParser().parse(arguments);
                case DismissCommand.COMMAND_WORD -> new DismissCommandParser(reminderService).parse(arguments);
                case ClearCommand.COMMAND_WORD -> parseCommandWithoutArguments(
                        arguments, ClearCommand.MESSAGE_USAGE, new ClearCommand());
                case AddTagCommand.COMMAND_WORD -> new AddTagCommandParser().parse(arguments);
                case FilterCommand.COMMAND_WORD -> new FilterCommandParser().parse(arguments);
                case ListCommand.COMMAND_WORD -> new ListCommandParser().parse(arguments);
                case SearchCommand.COMMAND_WORD -> new SearchCommandParser().parse(arguments);
                case MarkAttendanceCommand.COMMAND_WORD -> new MarkAttendanceCommandParser().parse(arguments);
                case ExitCommand.COMMAND_WORD -> parseCommandWithoutArguments(
                        arguments, ExitCommand.MESSAGE_USAGE, new ExitCommand());
                case HelpCommand.COMMAND_WORD -> parseCommandWithoutArguments(
                        arguments, HelpCommand.MESSAGE_USAGE, new HelpCommand());
                case TutorialCommand.COMMAND_WORD -> parseCommandWithoutArguments(
                        arguments, TutorialCommand.MESSAGE_USAGE, new TutorialCommand());
                default -> {
                    logger.finer("This user input caused a ParseException: " + userInput);
                    throw new ParseException(MESSAGE_UNKNOWN_COMMAND);
                }
            };
        } catch (ParseException e) {
            throw new ParseException(Messages.withUsage(e.getMessage(), getUsage(commandWord)), e);
        }
    }

    private static Command parseCommandWithoutArguments(String arguments, String usage, Command command)
            throws ParseException {
        if (!arguments.isBlank()) {
            throw ParserUtil.createInvalidFormatException(usage);
        }
        return command;
    }

    /**
     * Returns usage for a known command, or help usage when the command is unknown.
     */
    public static String getUsage(String commandWord) {
        return switch (commandWord) {
            case AddCommand.COMMAND_WORD -> AddCommand.MESSAGE_USAGE;
            case AddEventCommand.COMMAND_WORD -> AddEventCommand.MESSAGE_USAGE;
            case ArchiveCommand.COMMAND_WORD -> ArchiveCommand.MESSAGE_USAGE;
            case ArchiveCommand.UNARCHIVE_COMMAND_WORD -> ArchiveCommand.MESSAGE_UNARCHIVE_USAGE;
            case ArchiveEventCommand.COMMAND_WORD -> ArchiveEventCommand.MESSAGE_USAGE;
            case ArchiveEventCommand.UNARCHIVE_COMMAND_WORD -> ArchiveEventCommand.MESSAGE_UNARCHIVE_USAGE;
            case EditCommand.COMMAND_WORD -> EditCommand.MESSAGE_USAGE;
            case EditEventCommand.COMMAND_WORD -> EditEventCommand.MESSAGE_USAGE;
            case EditTagCommand.COMMAND_WORD -> EditTagCommand.MESSAGE_USAGE;
            case AssignCommand.COMMAND_WORD -> AssignCommand.MESSAGE_USAGE;
            case UnassignCommand.COMMAND_WORD -> UnassignCommand.MESSAGE_USAGE;
            case MembersCommand.COMMAND_WORD -> MembersCommand.MESSAGE_USAGE;
            case DeleteCommand.COMMAND_WORD -> DeleteCommand.MESSAGE_USAGE;
            case DeleteEventCommand.COMMAND_WORD -> DeleteEventCommand.MESSAGE_USAGE;
            case DeleteTagCommand.COMMAND_WORD -> DeleteTagCommand.MESSAGE_USAGE;
            case DismissCommand.COMMAND_WORD -> DismissCommand.MESSAGE_USAGE;
            case ClearCommand.COMMAND_WORD -> ClearCommand.MESSAGE_USAGE;
            case AddTagCommand.COMMAND_WORD -> AddTagCommand.MESSAGE_USAGE;
            case FilterCommand.COMMAND_WORD -> FilterCommand.MESSAGE_USAGE;
            case ListCommand.COMMAND_WORD -> ListCommand.MESSAGE_USAGE;
            case SearchCommand.COMMAND_WORD -> SearchCommand.MESSAGE_USAGE;
            case MarkAttendanceCommand.COMMAND_WORD -> MarkAttendanceCommand.MESSAGE_USAGE;
            case ExitCommand.COMMAND_WORD -> ExitCommand.MESSAGE_USAGE;
            case HelpCommand.COMMAND_WORD -> HelpCommand.MESSAGE_USAGE;
            case TutorialCommand.COMMAND_WORD -> TutorialCommand.MESSAGE_USAGE;
            default -> HelpCommand.MESSAGE_USAGE;
        };
    }

}
