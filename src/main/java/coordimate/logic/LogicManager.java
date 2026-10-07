package coordimate.logic;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.logging.Logger;

import coordimate.commons.core.GuiSettings;
import coordimate.commons.core.LogsCenter;
import coordimate.commons.exceptions.DataLoadingException;
import coordimate.logic.commands.AddEventCommand;
import coordimate.logic.commands.AssignCommand;
import coordimate.logic.commands.Command;
import coordimate.logic.commands.CommandResult;
import coordimate.logic.commands.DeleteEventCommand;
import coordimate.logic.commands.EditEventCommand;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.logic.parser.CoordiMateParser;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.Model;
import coordimate.model.ModelManager;
import coordimate.model.event.Event;
import coordimate.model.person.Person;
import coordimate.model.tag.Tag;
import coordimate.storage.Storage;
import javafx.collections.ObservableList;

/**
 * The main LogicManager of the app.
 */
public class LogicManager implements Logic {
    public static final String FILE_OPS_ERROR_FORMAT = "Could not save data due to the following error: %s";

    public static final String FILE_OPS_PERMISSION_ERROR_FORMAT =
            "Could not save data to file %s due to insufficient permissions to write to the file or the folder.";

    private final Logger logger = LogsCenter.getLogger(LogicManager.class);

    private final Model model;
    private final Storage storage;
    private final CoordiMateParser coordiMateParser;

    /**
     * Constructs a {@code LogicManager} with the given {@code Model} and {@code Storage}.
     */
    public LogicManager(Model model, Storage storage) {
        this.model = model;
        this.storage = storage;
        coordiMateParser = new CoordiMateParser();
    }

    @Override
    public CommandResult execute(String commandText) throws CommandException, ParseException {
        logger.info("----------------[USER COMMAND][" + commandText + "]");

        Command command = coordiMateParser.parseCommand(commandText);
        if (command instanceof AddEventCommand || command instanceof EditEventCommand
                || command instanceof AssignCommand || command instanceof DeleteEventCommand) {
            return executeEventCommand(command);
        }
        CommandResult commandResult = command.execute(model);

        try {
            storage.saveCoordiMate(model.getCoordiMate());
        } catch (AccessDeniedException e) {
            throw new CommandException(String.format(FILE_OPS_PERMISSION_ERROR_FORMAT, e.getMessage()), e);
        } catch (IOException ioe) {
            throw new CommandException(String.format(FILE_OPS_ERROR_FORMAT, ioe.getMessage()), ioe);
        }

        return commandResult;
    }

    /**
     * Saves an event on a copy of the model and commits it only after storage succeeds.
     * The copy shows the same contacts as the displayed list, so contact indexes refer to the same contacts.
     * Rejects the command if existing data cannot be loaded or the new data cannot be saved.
     */
    private CommandResult executeEventCommand(Command command) throws CommandException {
        try {
            storage.readCoordiMate();
        } catch (DataLoadingException e) {
            throw new CommandException(AddEventCommand.MESSAGE_LOAD_ERROR, e);
        }
        Model candidate = new ModelManager(model.getCoordiMate(), model.getUserPrefs());
        candidate.updateFilteredPersonList(model.getFilteredPersonList()::contains);
        CommandResult result = command.execute(candidate);
        try {
            storage.saveCoordiMate(candidate.getCoordiMate());
        } catch (IOException e) {
            String message = switch (command) {
                case EditEventCommand _ -> EditEventCommand.MESSAGE_SAVE_ERROR;
                case AssignCommand _ -> AssignCommand.MESSAGE_SAVE_ERROR;
                case DeleteEventCommand _ -> DeleteEventCommand.MESSAGE_SAVE_ERROR;
                default -> AddEventCommand.MESSAGE_SAVE_ERROR;
            };
            throw new CommandException(message, e);
        }
        model.setCoordiMate(candidate.getCoordiMate());
        return result;
    }

    @Override
    public ObservableList<Person> getFilteredPersonList() {
        return model.getFilteredPersonList();
    }

    @Override
    public ObservableList<Event> getEventList() {
        return model.getCoordiMate().getEventList();
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
}
