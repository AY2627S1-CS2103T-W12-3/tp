package coordimate.logic;

import static coordimate.testutil.TypicalPersons.AMY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import coordimate.logic.commands.ClearCommand;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.CoordiMate;
import coordimate.model.ModelManager;
import coordimate.model.ReadOnlyCoordiMate;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.tag.Tag;
import coordimate.storage.JsonCoordiMateStorage;

public class ClearIntegrationTest {
    @TempDir
    public Path temporaryFolder;

    @Test
    public void execute_success_savedAndReloaded() throws Exception {
        ModelManager model = newModel();
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(temporaryFolder.resolve("data.json"));
        storage.saveCoordiMate(model.getCoordiMate());

        assertEquals(ClearCommand.MESSAGE_SUCCESS,
                PersistenceTestUtil.newLogic(model, storage, temporaryFolder).execute("clear").getFeedbackToUser());
        assertEquals(new CoordiMate(), model.getCoordiMate());
        ReadOnlyCoordiMate reloaded = storage.readCoordiMate().orElseThrow();
        assertEquals(new CoordiMate(), reloaded);
        assertEquals(model.getCoordiMate(), new ModelManager(reloaded, model.getUserPrefs()).getCoordiMate());
    }

    @Test
    public void execute_saveFailure_modelAndFileUnchanged() throws Exception {
        ModelManager model = newModel();
        Path path = temporaryFolder.resolve("data.json");
        PersistenceTestUtil.BeforeState before = PersistenceTestUtil.seedThenCapture(model, path);
        IOException failure = new IOException("Disk full");
        JsonCoordiMateStorage failingStorage = PersistenceTestUtil.failingStorage(path, failure);

        CommandException exception = assertThrows(CommandException.class, () ->
                PersistenceTestUtil.newLogic(model, failingStorage, temporaryFolder).execute("clear"));
        assertEquals(Messages.withUsage(ClearCommand.MESSAGE_SAVE_ERROR,
                ClearCommand.MESSAGE_USAGE), exception.getMessage());
        PersistenceTestUtil.assertSaveFailurePreservedState(exception, failure, model, before);
    }

    @Test
    public void execute_invalidStoredData_modelAndFileUnchanged() throws Exception {
        ModelManager model = newModel();
        Path path = temporaryFolder.resolve("data.json");
        String invalidFile = "invalid json";
        Files.writeString(path, invalidFile);
        CoordiMate beforeModel = new CoordiMate(model.getCoordiMate());

        CommandException exception = assertThrows(CommandException.class, () ->
                PersistenceTestUtil.newLogic(model, new JsonCoordiMateStorage(path), temporaryFolder)
                        .execute("clear"));
        assertEquals(Messages.withUsage(ClearCommand.MESSAGE_LOAD_ERROR,
                ClearCommand.MESSAGE_USAGE), exception.getMessage());
        assertEquals(beforeModel, model.getCoordiMate());
        assertEquals(invalidFile, Files.readString(path));
    }

    @Test
    public void execute_extraArguments_modelAndFileUnchanged() throws Exception {
        ModelManager model = newModel();
        Path path = temporaryFolder.resolve("data.json");
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(path);
        storage.saveCoordiMate(model.getCoordiMate());
        CoordiMate beforeModel = new CoordiMate(model.getCoordiMate());
        String beforeFile = Files.readString(path);

        ParseException exception = assertThrows(ParseException.class, () ->
                PersistenceTestUtil.newLogic(model, storage, temporaryFolder).execute("clear garbage"));

        assertEquals(String.format(Messages.MESSAGE_INVALID_COMMAND_FORMAT, ClearCommand.MESSAGE_USAGE),
                exception.getMessage());
        assertEquals(beforeModel, model.getCoordiMate());
        assertEquals(beforeFile, Files.readString(path));
    }

    private ModelManager newModel() {
        ModelManager model = new ModelManager();
        model.addPerson(AMY);
        model.addTag(new Tag("Publicity"));
        model.addEvent(new Event("Final Concert", new EventTime("08-08-2026"),
                new EventTime("08-08-2026"), List.of(AMY.getName())));
        return model;
    }
}
