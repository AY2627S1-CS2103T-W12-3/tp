package coordimate.logic;

import static coordimate.testutil.TypicalPersons.AMY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import coordimate.logic.commands.ClearCommand;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.CoordiMate;
import coordimate.model.ModelManager;
import coordimate.model.ReadOnlyCoordiMate;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.tag.Tag;
import coordimate.storage.JsonCoordiMateStorage;
import coordimate.storage.JsonUserPrefsStorage;
import coordimate.storage.StorageManager;

public class ClearIntegrationTest {
    @TempDir
    public Path temporaryFolder;

    @Test
    public void execute_success_savedAndReloaded() throws Exception {
        ModelManager model = createModel();
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(temporaryFolder.resolve("data.json"));
        storage.saveCoordiMate(model.getCoordiMate());

        assertEquals(ClearCommand.MESSAGE_SUCCESS, createLogic(model, storage).execute("clear").getFeedbackToUser());
        assertEquals(new CoordiMate(), model.getCoordiMate());
        ReadOnlyCoordiMate reloaded = storage.readCoordiMate().orElseThrow();
        assertEquals(new CoordiMate(), reloaded);
        assertEquals(model.getCoordiMate(), new ModelManager(reloaded, model.getUserPrefs()).getCoordiMate());
    }

    @Test
    public void execute_saveFailure_modelAndFileUnchanged() throws Exception {
        ModelManager model = createModel();
        Path path = temporaryFolder.resolve("data.json");
        new JsonCoordiMateStorage(path).saveCoordiMate(model.getCoordiMate());
        CoordiMate beforeModel = new CoordiMate(model.getCoordiMate());
        String beforeFile = Files.readString(path);
        IOException failure = new IOException("Disk full");
        JsonCoordiMateStorage failingStorage = new JsonCoordiMateStorage(path) {
            @Override
            public void saveCoordiMate(ReadOnlyCoordiMate data) throws IOException {
                throw failure;
            }
        };

        CommandException exception = assertThrows(CommandException.class, () ->
                createLogic(model, failingStorage).execute("clear"));
        assertEquals(ClearCommand.MESSAGE_SAVE_ERROR, exception.getMessage());
        assertSame(failure, exception.getCause());
        assertEquals(beforeModel, model.getCoordiMate());
        assertEquals(beforeFile, Files.readString(path));
    }

    @Test
    public void execute_invalidStoredData_modelAndFileUnchanged() throws Exception {
        ModelManager model = createModel();
        Path path = temporaryFolder.resolve("data.json");
        String invalidFile = "invalid json";
        Files.writeString(path, invalidFile);
        CoordiMate beforeModel = new CoordiMate(model.getCoordiMate());

        CommandException exception = assertThrows(CommandException.class, () ->
                createLogic(model, new JsonCoordiMateStorage(path)).execute("clear"));
        assertEquals(ClearCommand.MESSAGE_LOAD_ERROR, exception.getMessage());
        assertEquals(beforeModel, model.getCoordiMate());
        assertEquals(invalidFile, Files.readString(path));
    }

    private ModelManager createModel() {
        ModelManager model = new ModelManager();
        model.addPerson(AMY);
        model.addTag(new Tag("Publicity"));
        model.addEvent(new Event("Final Concert", new EventTime("08-08-2026"),
                new EventTime("08-08-2026"), List.of(AMY.getName())));
        return model;
    }

    private Logic createLogic(ModelManager model, JsonCoordiMateStorage storage) {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));
    }
}
