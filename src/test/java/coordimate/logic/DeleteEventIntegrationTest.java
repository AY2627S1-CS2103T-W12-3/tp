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

import coordimate.logic.commands.DeleteEventCommand;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.CoordiMate;
import coordimate.model.ModelManager;
import coordimate.model.ReadOnlyCoordiMate;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.storage.JsonCoordiMateStorage;
import coordimate.storage.JsonUserPrefsStorage;
import coordimate.storage.StorageManager;

public class DeleteEventIntegrationTest {
    @TempDir
    public Path temporaryFolder;

    private final Event concert = new Event("Final Concert",
            new EventTime("08-08-2026 15:00"), new EventTime("08-08-2026 18:00"));
    private final Event fair = new Event("Student Life Fair",
            new EventTime("09-10-2026"), new EventTime("09-10-2026"));

    @Test
    public void execute_delete_savedAndReloadedWithOtherEventsAndContacts() throws Exception {
        ModelManager model = newModel();
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(temporaryFolder.resolve("data.json"));
        Logic logic = newLogic(model, storage);
        assertEquals("Deleted Event Final Concert.",
                logic.execute("deleteevent evn/final concert").getFeedbackToUser());
        assertEquals(List.of(fair), model.getCoordiMate().getEventList());
        assertEquals(List.of(AMY), model.getCoordiMate().getPersonList());
        assertEquals(model.getCoordiMate(), storage.readCoordiMate().orElseThrow());
        ModelManager reloaded = new ModelManager(storage.readCoordiMate().orElseThrow(), model.getUserPrefs());
        assertEquals("Deleted Event Student Life Fair.", newLogic(reloaded, storage)
                .execute("deleteevent evn/sTuDeNt LiFe FaIr").getFeedbackToUser());
        assertEquals(List.of(), reloaded.getCoordiMate().getEventList());
        assertEquals(reloaded.getCoordiMate(), storage.readCoordiMate().orElseThrow());
    }

    @Test
    public void execute_missingOrInexactName_modelAndFileUnchanged() throws Exception {
        ModelManager model = newModel();
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(temporaryFolder.resolve("data.json"));
        storage.saveCoordiMate(model.getCoordiMate());
        String beforeFile = Files.readString(storage.getCoordiMateFilePath());
        CoordiMate before = new CoordiMate(model.getCoordiMate());
        Logic logic = newLogic(model, storage);
        for (String name : new String[] {"Missing", "Final", "Final  Concert"}) {
            assertEquals("Event " + name + " does not exist.", assertThrows(
                    CommandException.class, () -> logic.execute("deleteevent evn/" + name)).getMessage());
            assertEquals(before, model.getCoordiMate());
            assertEquals(beforeFile, Files.readString(storage.getCoordiMateFilePath()));
        }
    }

    @Test
    public void execute_saveFailure_modelAndFileUnchanged() throws Exception {
        ModelManager model = newModel();
        Path path = temporaryFolder.resolve("data.json");
        new JsonCoordiMateStorage(path).saveCoordiMate(model.getCoordiMate());
        CoordiMate before = new CoordiMate(model.getCoordiMate());
        String beforeFile = Files.readString(path);
        JsonCoordiMateStorage failingStorage = new JsonCoordiMateStorage(path) {
            @Override
            public void saveCoordiMate(ReadOnlyCoordiMate data) throws IOException {
                throw new IOException("Disk full");
            }
        };
        Logic failingLogic = newLogic(model, failingStorage);
        assertEquals(DeleteEventCommand.MESSAGE_SAVE_ERROR, assertThrows(
                CommandException.class, () -> failingLogic.execute("deleteevent evn/Final Concert")).getMessage());
        assertEquals(before, model.getCoordiMate());
        assertEquals(beforeFile, Files.readString(path));
    }

    private ModelManager newModel() {
        ModelManager model = new ModelManager();
        model.addEvent(concert);
        model.addEvent(fair);
        model.addPerson(AMY);
        return model;
    }

    private Logic newLogic(ModelManager model, JsonCoordiMateStorage storage) {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));
    }
}
