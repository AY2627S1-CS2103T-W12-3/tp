package coordimate.logic;

import static coordimate.model.Model.PREDICATE_SHOW_ALL_PERSONS;

import java.io.IOException;
import java.time.Clock;
import java.util.logging.Level;
import java.util.logging.Logger;

import coordimate.commons.core.GuiSettings;
import coordimate.commons.core.LogsCenter;
import coordimate.commons.exceptions.DataLoadingException;
import coordimate.logic.commands.AddCommand;
import coordimate.logic.commands.AddEventCommand;
import coordimate.logic.commands.ArchiveCommand;
import coordimate.logic.commands.ArchiveEventCommand;
import coordimate.logic.commands.AssignCommand;
import coordimate.logic.commands.ClearCommand;
import coordimate.logic.commands.Command;
import coordimate.logic.commands.CommandResult;
import coordimate.logic.commands.DeleteCommand;
import coordimate.logic.commands.DeleteEventCommand;
import coordimate.logic.commands.DismissCommand;
import coordimate.logic.commands.EditCommand;
import coordimate.logic.commands.EditEventCommand;
import coordimate.logic.commands.MarkAttendanceCommand;
import coordimate.logic.commands.MutatingTagCommand;
import coordimate.logic.commands.UnassignCommand;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.logic.parser.CoordiMateParser;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.logic.reminder.ReminderDismissalException;
import coordimate.logic.reminder.ReminderService;
import coordimate.model.Model;
import coordimate.model.ModelManager;
import coordimate.model.event.Event;
import coordimate.model.event.EventMembersPredicate;
import coordimate.model.person.Person;
import coordimate.model.tag.Tag;
import coordimate.storage.DataConflictException;
import coordimate.storage.Storage;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.collections.ObservableList;

/**
 * The main LogicManager of the app.
 */
public class LogicManager implements Logic {
    public static final String MESSAGE_DELETE_CANCELLED = "Deletion cancelled. No changes were made.";

    private final Logger logger = LogsCenter.getLogger(LogicManager.class);

    private final Model model;
    private final Storage storage;
    private final CoordiMateParser coordiMateParser;
    private final ReminderService reminderService;
    private Person pendingDelete;

    /**
     * Constructs a {@code LogicManager} with the given {@code Model} and {@code Storage}.
     */
    public LogicManager(Model model, Storage storage) {
        this(model, storage, Clock.systemDefaultZone());
    }

    /**
     * Constructs logic using the given clock for reminder eligibility.
     */
    public LogicManager(Model model, Storage storage, Clock clock) {
        this.model = model;
        this.storage = storage;
        reminderService = new ReminderService(model, storage, clock);
        coordiMateParser = new CoordiMateParser(reminderService);
    }

    @Override
    public CommandResult execute(String commandText) throws CommandException, ParseException {
        String commandWord = pendingDelete == null ? commandText.strip().split("\\s+", 2)[0]
                : DeleteCommand.COMMAND_WORD;
        try {
            return executeCommand(commandText);
        } catch (CommandException e) {
            throw new CommandException(Messages.withUsage(e.getMessage(), CoordiMateParser.getUsage(commandWord)),
                    e.getCause() == null ? e : e.getCause());
        } finally {
            reminderService.refreshReminders();
        }
    }

    private CommandResult executeCommand(String commandText) throws CommandException, ParseException {
        logger.info("----------------[USER COMMAND][" + commandText + "]");

        if (pendingDelete != null) {
            Person target = pendingDelete;
            pendingDelete = null;
            return "y".equalsIgnoreCase(commandText.strip())
                    ? executeAtomicCommand(DeleteCommand.forResolvedPerson(target))
                    : new CommandResult(MESSAGE_DELETE_CANCELLED);
        }

        Command command = coordiMateParser.parseCommand(commandText);
        if (command instanceof DismissCommand) {
            return command.execute(model);
        }
        if (command instanceof DeleteCommand deleteCommand) {
            try {
                storage.readCoordiMate();
            } catch (DataLoadingException e) {
                logContactOperationFailure(deleteCommand, "load contact data", e);
                throw new CommandException(DeleteCommand.MESSAGE_LOAD_ERROR, e);
            }
            Person target = deleteCommand.resolvePerson(model);
            pendingDelete = target;
            return new CommandResult(DeleteCommand.confirmationPrompt(target), CommandResult.View.CONTACTS);
        }

        if (command instanceof AddCommand || command instanceof EditCommand || command instanceof ClearCommand
                || command instanceof MutatingTagCommand
                || command instanceof AddEventCommand || command instanceof ArchiveEventCommand
                || command instanceof ArchiveCommand
                || command instanceof EditEventCommand || command instanceof AssignCommand
                || command instanceof DeleteEventCommand || command instanceof MarkAttendanceCommand
                || command instanceof UnassignCommand) {
            return executeAtomicCommand(command);
        }
        return command.execute(model);
    }

    /**
     * Executes a command on a copy of the model and commits it only after storage succeeds.
     * The copy preserves member IDs and the active view while commands resolve IDs across all saved contacts.
     * Rejects the command if existing data cannot be loaded or the new data cannot be saved.
     */
    private CommandResult executeAtomicCommand(Command command) throws CommandException {
        try {
            storage.readCoordiMate();
        } catch (DataLoadingException e) {
            logContactOperationFailure(command, "load contact data", e);
            String message = switch (command) {
                case AddCommand _ -> AddCommand.MESSAGE_LOAD_ERROR;
                case EditCommand _ -> EditCommand.MESSAGE_LOAD_ERROR;
                case DeleteCommand _ -> DeleteCommand.MESSAGE_LOAD_ERROR;
                case ClearCommand _ -> ClearCommand.MESSAGE_LOAD_ERROR;
                case MutatingTagCommand _ -> MutatingTagCommand.MESSAGE_LOAD_ERROR;
                default -> AddEventCommand.MESSAGE_LOAD_ERROR;
            };
            throw new CommandException(message, e);
        }
        Model candidate = new ModelManager(model.getCoordiMate(), model.getUserPrefs());
        candidate.setShowingArchived(model.isShowingArchived());
        candidate.updateFilteredPersonList(model.getFilteredPersonList()::contains);
        CommandResult result = command.execute(candidate);
        try {
            storage.saveCoordiMateIfUnchanged(candidate.getCoordiMate());
        } catch (DataConflictException e) {
            throw new CommandException(DataConflictException.MESSAGE, e);
        } catch (IOException e) {
            logContactOperationFailure(command, "save contact data", e);
            String message = switch (command) {
                case AddCommand _ -> AddCommand.MESSAGE_SAVE_ERROR;
                case EditCommand _ -> EditCommand.MESSAGE_SAVE_ERROR;
                case DeleteCommand _ -> DeleteCommand.MESSAGE_SAVE_ERROR;
                case EditEventCommand _ -> EditEventCommand.MESSAGE_SAVE_ERROR;
                case AssignCommand _ -> AssignCommand.MESSAGE_SAVE_ERROR;
                case DeleteEventCommand _ -> DeleteEventCommand.MESSAGE_SAVE_ERROR;
                case ArchiveEventCommand _ -> ArchiveEventCommand.MESSAGE_SAVE_ERROR;
                case ArchiveCommand _ -> ArchiveCommand.MESSAGE_SAVE_ERROR;
                case ClearCommand _ -> ClearCommand.MESSAGE_SAVE_ERROR;
                case MarkAttendanceCommand _ -> MarkAttendanceCommand.MESSAGE_SAVE_ERROR;
                case UnassignCommand _ -> UnassignCommand.MESSAGE_SAVE_ERROR;
                case MutatingTagCommand _ -> MutatingTagCommand.MESSAGE_SAVE_ERROR;
                default -> AddEventCommand.MESSAGE_SAVE_ERROR;
            };
            throw new CommandException(message, e);
        }
        keepReminderDismissalAfterRename(command);
        model.setCoordiMate(candidate.getCoordiMate());
        if (command instanceof AddCommand || command instanceof AddEventCommand) {
            // Return to active items so that the new contact or event is visible.
            model.setShowingArchived(false);
        }
        if (command instanceof AddCommand || command instanceof EditCommand) {
            model.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
        }
        keepMembersViewAfterRename(command);
        logContactOperationSuccess(command);
        return result;
    }

    private void logContactOperationSuccess(Command command) {
        String action = getContactOperation(command);
        if (action != null) {
            logger.info("Contact " + action + " completed successfully.");
        }
    }

    private void logContactOperationFailure(Command command, String operation, Exception exception) {
        String action = getContactOperation(command);
        if (action != null) {
            logger.log(Level.WARNING, "Failed to " + operation + " during contact " + action + ".", exception);
        }
    }

    private static String getContactOperation(Command command) {
        return switch (command) {
            case AddCommand _ -> "addition";
            case EditCommand _ -> "edit";
            case DeleteCommand _ -> "deletion";
            default -> null;
        };
    }

    /**
     * Keeps showing the same members when {@code command} renames the event whose members are displayed,
     * since the displayed list follows the event by its name.
     */
    private void keepMembersViewAfterRename(Command command) {
        if (command instanceof EditEventCommand editEvent
                && model.getFilteredPersonPredicate() instanceof EventMembersPredicate shown
                && editEvent.renamesEvent(shown.getEventName())) {
            model.updateFilteredPersonList(new EventMembersPredicate(editEvent.getNewName(),
                    model.getCoordiMate().getEventList()));
        }
    }

    private void keepReminderDismissalAfterRename(Command command) {
        if (command instanceof EditEventCommand editEvent) {
            model.getCoordiMate().getEventList().stream()
                    .filter(event -> editEvent.renamesEvent(event.getName()))
                    .findFirst()
                    .ifPresent(event -> reminderService.renameEvent(event.getName(), editEvent.getNewName()));
        }
    }

    @Override
    public boolean isAwaitingDeleteConfirmation() {
        return pendingDelete != null;
    }

    @Override
    public ObservableList<Person> getFilteredPersonList() {
        return model.getFilteredPersonList();
    }

    @Override
    public ObservableList<Person> getPersonList() {
        return model.getCoordiMate().getPersonList();
    }

    @Override
    public ObservableList<Event> getEventList() {
        return model.getCoordiMate().getEventList();
    }

    @Override
    public ObservableList<Event> getFilteredEventList() {
        return model.getFilteredEventList();
    }

    @Override
    public ReadOnlyBooleanProperty showingArchivedProperty() {
        return model.showingArchivedProperty();
    }

    @Override
    public void showActiveItems() {
        model.setShowingArchived(false);
    }

    @Override
    public ObservableList<Event> getReminders() {
        return reminderService.getReminders();
    }

    @Override
    public void refreshReminders() {
        reminderService.refreshReminders();
    }

    @Override
    public void dismissReminder(Event reminder) throws ReminderDismissalException {
        reminderService.dismissReminder(reminder);
    }

    @Override
    public ObservableList<Tag> getTagList() {
        return model.getTagList();
    }

    @Override
    public GuiSettings getGuiSettings() {
        return model.getGuiSettings();
    }

    @Override
    public void setGuiSettings(GuiSettings guiSettings) {
        model.setGuiSettings(guiSettings);
    }

    @Override
    public boolean hasSeenTutorial() {
        return model.hasSeenTutorial();
    }

    @Override
    public void setHasSeenTutorial(boolean hasSeenTutorial) {
        model.setHasSeenTutorial(hasSeenTutorial);
    }
}
