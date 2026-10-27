package coordimate.logic;

import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BENSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import coordimate.commons.exceptions.DataLoadingException;
import coordimate.logic.commands.CommandResult;
import coordimate.logic.commands.DeleteCommand;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.CoordiMate;
import coordimate.model.ModelManager;
import coordimate.model.ReadOnlyCoordiMate;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.storage.JsonCoordiMateStorage;
import coordimate.storage.JsonUserPrefsStorage;
import coordimate.storage.StorageManager;

public class DeleteCommandIntegrationTest {
    @TempDir
    public Path temporaryFolder;

    @Test
    public void execute_requestThenConfirm_deletesContactAndEventAssignmentsAfterSaving() throws Exception {
        ModelManager model = modelWithEvent();
        Path path = temporaryFolder.resolve("contacts.json");
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(path);
        storage.saveCoordiMate(model.getCoordiMate());
        String beforeFile = Files.readString(path);
        CoordiMate beforeModel = new CoordiMate(model.getCoordiMate());
        Logic logic = newLogic(model, storage);

        CommandResult confirmation = logic.execute("delete n/alice pauline");
        assertEquals(DeleteCommand.confirmationPrompt(ALICE), confirmation.getFeedbackToUser());
        assertEquals(CommandResult.View.CONTACTS, confirmation.getViewToShow());
        assertTrue(logic.isAwaitingDeleteConfirmation());
        assertEquals(beforeModel, model.getCoordiMate());
        assertEquals(beforeFile, Files.readString(path));

        CommandResult deletion = logic.execute("y");
        assertEquals("Contact deleted successfully: Alice Pauline.", deletion.getFeedbackToUser());
        assertEquals(CommandResult.View.CONTACTS, deletion.getViewToShow());
        assertEquals(0, deletion.getContactIndexToFocus().orElseThrow());
        assertTrue(deletion.getContactToFocus().isEmpty());
        assertFalse(logic.isAwaitingDeleteConfirmation());
        assertEquals(List.of(BENSON), model.getCoordiMate().getPersonList());
        assertEquals(List.of(BENSON.getName()), model.getCoordiMate().getEventList().getFirst().getMembers());
        assertEquals(model.getCoordiMate(), storage.readCoordiMate().orElseThrow());
    }

    @Test
    public void execute_uppercaseConfirmation_deletesContact() throws Exception {
        ModelManager model = modelWithEvent();
        Logic logic = newLogic(model, new JsonCoordiMateStorage(temporaryFolder.resolve("uppercase.json")));

        logic.execute("delete p/(9435) 1253");
        assertEquals("Contact deleted successfully: Alice Pauline.", logic.execute("Y").getFeedbackToUser());
        assertEquals(List.of(BENSON), model.getCoordiMate().getPersonList());
    }

    @Test
    public void execute_confirmationWithSurroundingWhitespace_deletesContact() throws Exception {
        for (String input : List.of("y ", " y", "\ty\t", " Y ")) {
            ModelManager model = modelWithEvent();
            Path path = temporaryFolder.resolve("padded-confirmation.json");
            JsonCoordiMateStorage storage = new JsonCoordiMateStorage(path);
            storage.saveCoordiMate(model.getCoordiMate());
            Logic logic = newLogic(model, storage);

            logic.execute("delete 1");
            assertEquals("Contact deleted successfully: Alice Pauline.", logic.execute(input).getFeedbackToUser());
            assertFalse(logic.isAwaitingDeleteConfirmation());
            assertEquals(List.of(BENSON), model.getCoordiMate().getPersonList());
            assertEquals(model.getCoordiMate(), storage.readCoordiMate().orElseThrow());
        }
    }

    @Test
    public void execute_otherConfirmationInput_cancelsWithoutExecutingInput() throws Exception {
        String[] cancellations = {"n", "N", "", "   ", "list", "delete 2", "yes", "y n"};
        for (String input : cancellations) {
            ModelManager model = modelWithEvent();
            Path path = temporaryFolder.resolve("cancel.json");
            JsonCoordiMateStorage storage = new JsonCoordiMateStorage(path);
            storage.saveCoordiMate(model.getCoordiMate());
            String beforeFile = Files.readString(path);
            CoordiMate beforeModel = new CoordiMate(model.getCoordiMate());
            Logic logic = newLogic(model, storage);

            logic.execute("delete 1");
            CommandResult cancellation = logic.execute(input);
            assertEquals(LogicManager.MESSAGE_DELETE_CANCELLED, cancellation.getFeedbackToUser());
            assertEquals(CommandResult.View.NONE, cancellation.getViewToShow());
            assertFalse(logic.isAwaitingDeleteConfirmation());
            assertEquals(beforeModel, model.getCoordiMate());
            assertEquals(beforeFile, Files.readString(path));

            // A canceled command has to be entered again to run.
            assertEquals(DeleteCommand.confirmationPrompt(BENSON), logic.execute("delete 2").getFeedbackToUser());
        }
    }

    @Test
    public void execute_filteredList_usesPermanentId() throws Exception {
        ModelManager model = modelWithEvent();
        model.updateFilteredPersonList(person -> person.equals(BENSON));
        Logic logic = newLogic(model, new JsonCoordiMateStorage(temporaryFolder.resolve("filtered.json")));

        assertEquals(DeleteCommand.confirmationPrompt(BENSON), logic.execute("delete 2").getFeedbackToUser());
        logic.execute("y");

        assertEquals(List.of(ALICE), model.getCoordiMate().getPersonList());
        assertEquals(List.of(ALICE.getName()), model.getCoordiMate().getEventList().getFirst().getMembers());
        assertTrue(model.getFilteredPersonList().isEmpty());
    }

    @Test
    public void execute_emailOutsideDisplayedList_findsSavedContact() throws Exception {
        ModelManager model = modelWithEvent();
        model.updateFilteredPersonList(person -> false);
        Logic logic = newLogic(model, new JsonCoordiMateStorage(temporaryFolder.resolve("hidden.json")));

        assertEquals(DeleteCommand.confirmationPrompt(ALICE),
                logic.execute("delete e/ALICE@EXAMPLE.COM").getFeedbackToUser());
        logic.execute("y");

        assertEquals(List.of(BENSON), model.getCoordiMate().getPersonList());
        assertEquals(List.of(BENSON.getName()), model.getCoordiMate().getEventList().getFirst().getMembers());
    }

    @Test
    public void execute_saveFailure_modelAndFileUnchanged() throws Exception {
        IOException[] failures = {new IOException("Disk full"), new AccessDeniedException("contacts.json")};
        for (IOException failure : failures) {
            ModelManager model = modelWithEvent();
            Path path = temporaryFolder.resolve("failed.json");
            new JsonCoordiMateStorage(path).saveCoordiMate(model.getCoordiMate());
            String beforeFile = Files.readString(path);
            CoordiMate beforeModel = new CoordiMate(model.getCoordiMate());
            JsonCoordiMateStorage failingStorage = new JsonCoordiMateStorage(path) {
                @Override
                public void saveCoordiMate(ReadOnlyCoordiMate data) throws IOException {
                    throw failure;
                }
            };
            Logic logic = newLogic(model, failingStorage);
            logic.execute("delete 1");

            CommandException exception = assertThrows(CommandException.class, () -> logic.execute("y"));

            assertEquals(Messages.withUsage(DeleteCommand.MESSAGE_SAVE_ERROR,
                    DeleteCommand.MESSAGE_USAGE), exception.getMessage());
            assertSame(failure, exception.getCause());
            assertFalse(logic.isAwaitingDeleteConfirmation());
            assertEquals(beforeModel, model.getCoordiMate());
            assertEquals(beforeFile, Files.readString(path));
        }
    }

    @Test
    public void execute_corruptedFile_cannotRequestOrConfirmDeletion() throws Exception {
        Path path = temporaryFolder.resolve("corrupt.json");
        Files.writeString(path, "not json");
        ModelManager model = modelWithEvent();
        CoordiMate beforeModel = new CoordiMate(model.getCoordiMate());
        Logic logic = newLogic(model, new JsonCoordiMateStorage(path));

        CommandException firstError = assertThrows(CommandException.class, () -> logic.execute("delete 1"));
        assertEquals(Messages.withUsage(DeleteCommand.MESSAGE_LOAD_ERROR,
                DeleteCommand.MESSAGE_USAGE), firstError.getMessage());
        assertTrue(firstError.getCause() instanceof DataLoadingException);
        assertFalse(logic.isAwaitingDeleteConfirmation());
        assertEquals(beforeModel, model.getCoordiMate());

        new JsonCoordiMateStorage(path).saveCoordiMate(model.getCoordiMate());
        logic.execute("delete 1");
        Files.writeString(path, "not json");
        CommandException secondError = assertThrows(CommandException.class, () -> logic.execute("y"));
        assertEquals(Messages.withUsage(DeleteCommand.MESSAGE_LOAD_ERROR,
                DeleteCommand.MESSAGE_USAGE), secondError.getMessage());
        assertFalse(logic.isAwaitingDeleteConfirmation());
        assertEquals(beforeModel, model.getCoordiMate());
        assertEquals("not json", Files.readString(path));
    }

    private ModelManager modelWithEvent() {
        ModelManager model = new ModelManager();
        model.addPerson(ALICE);
        model.addPerson(BENSON);
        EventTime date = new EventTime("08-08-2026");
        model.addEvent(new Event("Concert", date, date, List.of(ALICE.getName(), BENSON.getName())));
        return model;
    }

    private Logic newLogic(ModelManager model, JsonCoordiMateStorage contactStorage) {
        return new LogicManager(model, new StorageManager(contactStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));
    }
}
