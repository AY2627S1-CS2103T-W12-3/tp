package coordimate.logic;

import static coordimate.testutil.TypicalPersons.AMY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import coordimate.logic.commands.AddEventCommand;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.CoordiMate;
import coordimate.model.ModelManager;
import coordimate.model.ReadOnlyCoordiMate;
import coordimate.model.event.Event;
import coordimate.storage.JsonCoordiMateStorage;
import coordimate.storage.JsonUserPrefsStorage;
import coordimate.storage.StorageManager;

public class AddEventIntegrationTest {
    private static final String COMMAND = "addevent evn/Fair st/09-10-2026 et/10-10-2026";

    @TempDir
    public Path temporaryFolder;

    @Test
    public void execute_validEvent_savedAndReloaded() throws Exception {
        ModelManager model = new ModelManager();
        model.addPerson(AMY);
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(temporaryFolder.resolve("data.json"));
        Logic logic = newLogic(model, storage);
        assertEquals("Created Event Fair. Start Time: 09-10-2026. End Time: 10-10-2026.",
                logic.execute(COMMAND).getFeedbackToUser());
        assertEquals(model.getCoordiMate(), storage.readCoordiMate().orElseThrow());
        ModelManager reloaded = new ModelManager(storage.readCoordiMate().orElseThrow(), model.getUserPrefs());
        assertEquals("Fair", reloaded.getCoordiMate().getEventList().getFirst().getName());
        assertEquals(AddEventCommand.MESSAGE_DUPLICATE_EVENT,
                assertThrows(CommandException.class, () -> newLogic(reloaded, storage).execute(COMMAND)).getMessage());
        String differentCase = "addevent evn/fair st/11-10-2026 et/12-10-2026";
        assertEquals(AddEventCommand.MESSAGE_DUPLICATE_EVENT,
                assertThrows(CommandException.class, () ->
                        newLogic(reloaded, storage).execute(differentCase)).getMessage());
    }

    @Test
    public void execute_saveFailure_modelAndFileUnchanged() throws Exception {
        IOException[] failures = {new IOException("Disk full"), new AccessDeniedException("data")};
        for (IOException failure : failures) {
            Path path = temporaryFolder.resolve("failed.json");
            ModelManager model = new ModelManager();
            model.addPerson(AMY);
            new JsonCoordiMateStorage(path).saveCoordiMate(model.getCoordiMate());
            String beforeFile = Files.readString(path);
            CoordiMate beforeModel = new CoordiMate(model.getCoordiMate());
            JsonCoordiMateStorage storage = new JsonCoordiMateStorage(path) {
                @Override
                public void saveCoordiMate(ReadOnlyCoordiMate data) throws IOException {
                    throw failure;
                }
            };
            assertEquals(AddEventCommand.MESSAGE_SAVE_ERROR,
                    assertThrows(CommandException.class, () -> newLogic(model, storage).execute(COMMAND)).getMessage());
            assertEquals(beforeModel, model.getCoordiMate());
            assertEquals(beforeFile, Files.readString(path));
        }
    }

    @Test
    public void execute_startAfterEnd_modelAndFileUnchanged() throws Exception {
        ModelManager model = new ModelManager();
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(temporaryFolder.resolve("data.json"));
        Logic logic = newLogic(model, storage);
        logic.execute(COMMAND);
        CoordiMate beforeModel = new CoordiMate(model.getCoordiMate());
        String beforeFile = Files.readString(storage.getCoordiMateFilePath());
        String reversed = "addevent evn/Concert st/08-08-2026 18:00 et/08-08-2026 15:00";
        assertEquals(Event.MESSAGE_INVALID_TIME_ORDER,
                assertThrows(ParseException.class, () -> logic.execute(reversed)).getMessage());
        assertEquals(beforeModel, model.getCoordiMate());
        assertEquals(beforeFile, Files.readString(storage.getCoordiMateFilePath()));
    }

    @Test
    public void execute_corruptedFile_notOverwritten() throws Exception {
        String event = "{\"name\":\"Fair\",\"startTime\":\"09-10-2026\",\"endTime\":\"10-10-2026\"}";
        for (String contents : new String[] {"invalid json", "null", "{}", "{\"persons\":[],\"events\":[null]}",
            "{\"persons\":[],\"events\":[" + event + "," + event + "]}",
            "{\"persons\":[],\"events\":[" + event + "," + event.replace("Fair", "fair") + "]}",
            "{\"persons\":[],\"events\":[{\"name\":\"Fair\",\"startTime\":\"31-02-2026\"}]}",
            "{\"persons\":[],\"events\":[{\"name\":\"Fair\",\"startTime\":\"10-10-2026\","
                + "\"endTime\":\"09-10-2026\"}]}",
            "{\"persons\":[],\"events\":[{\"name\":\"Fair\",\"startTime\":\"09-10-2026 18:00\","
                + "\"endTime\":\"09-10-2026 15:00\"}]}",
            "{\"persons\":[],\"events\":[{\"name\":\" \",\"startTime\":\"09-10-2026\","
                + "\"endTime\":\"10-10-2026\"}]}"}) {
            Path path = temporaryFolder.resolve("corrupt.json");
            Files.writeString(path, contents);
            ModelManager model = new ModelManager();
            Logic logic = newLogic(model, new JsonCoordiMateStorage(path));
            assertEquals(AddEventCommand.MESSAGE_LOAD_ERROR,
                    assertThrows(CommandException.class, () -> logic.execute(COMMAND)).getMessage());
            assertTrue(model.getCoordiMate().getEventList().isEmpty());
            assertEquals(contents, Files.readString(path));
        }
    }

    @Test
    public void execute_legacyFileWithoutEvents_success() throws Exception {
        Path path = temporaryFolder.resolve("legacy.json");
        Files.writeString(path, "{\"persons\":[]}");
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(path);
        assertTrue(storage.readCoordiMate().orElseThrow().getEventList().isEmpty());
        ModelManager model = new ModelManager();
        newLogic(model, storage).execute(COMMAND);
        assertEquals(model.getCoordiMate(), storage.readCoordiMate().orElseThrow());
    }

    @Test
    public void save_failedReplacement_existingTargetPreserved() throws Exception {
        Path path = temporaryFolder.resolve("directory.json");
        Files.createDirectory(path);
        Files.writeString(path.resolve("keep.txt"), "keep");
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(path);
        assertThrows(IOException.class, () -> storage.saveCoordiMate(new CoordiMate()));
        assertEquals("keep", Files.readString(path.resolve("keep.txt")));
        try (var files = Files.list(temporaryFolder)) {
            assertEquals(1, files.count());
        }
    }

    private Logic newLogic(ModelManager model, JsonCoordiMateStorage storage) {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));
    }
}
