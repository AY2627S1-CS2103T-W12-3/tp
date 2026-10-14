package coordimate.logic;

import static coordimate.testutil.TypicalPersons.AMY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import coordimate.logic.commands.EditEventCommand;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.CoordiMate;
import coordimate.model.ModelManager;
import coordimate.model.ReadOnlyCoordiMate;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.storage.JsonCoordiMateStorage;
import coordimate.storage.JsonUserPrefsStorage;
import coordimate.storage.StorageManager;

public class EditEventIntegrationTest {
    private static final String ADD = "addevent evn/Student Life Fair st/09-10-2026 et/09-10-2026";
    private static final String EDIT =
            "editevent evn/Student Life Fair nevn/NUS Student Life Fair et/10-10-2026";

    @TempDir
    public Path temporaryFolder;

    @Test
    public void execute_edit_savedAndReloadedWithContacts() throws Exception {
        ModelManager model = new ModelManager();
        model.addPerson(AMY);
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(temporaryFolder.resolve("data.json"));
        Logic logic = newLogic(model, storage);
        logic.execute(ADD);
        assertEquals("Edited Event NUS Student Life Fair. Start Time: 09-10-2026. End Time: 10-10-2026.",
                logic.execute(EDIT).getFeedbackToUser());
        assertEquals(model.getCoordiMate(), storage.readCoordiMate().orElseThrow());
        ModelManager reloaded = new ModelManager(storage.readCoordiMate().orElseThrow(), model.getUserPrefs());
        newLogic(reloaded, storage).execute("editevent evn/NUS Student Life Fair st/09-10-2026 16:00");
        assertEquals("09-10-2026 16:00", reloaded.getCoordiMate().getEventList().getFirst().getStartTime().toString());
        assertEquals(model.getCoordiMate().getPersonList(), reloaded.getCoordiMate().getPersonList());
    }

    @Test
    public void execute_caseOnlyRename_savedAndReloaded() throws Exception {
        ModelManager model = new ModelManager();
        Event meeting = new Event("CCA Meeting", new EventTime("09-10-2026"), new EventTime("10-10-2026"));
        model.addEvent(meeting);
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(temporaryFolder.resolve("data.json"));
        Logic logic = newLogic(model, storage);
        assertEquals("Edited Event CCA MEETING. Start Time: 09-10-2026. End Time: 10-10-2026.",
                logic.execute("editevent evn/CCA MEETING nevn/CCA MEETING").getFeedbackToUser());
        assertEquals(new Event("CCA MEETING", meeting.getStartTime(), meeting.getEndTime()),
                storage.readCoordiMate().orElseThrow().getEventList().getFirst());
        assertEquals(model.getCoordiMate(), storage.readCoordiMate().orElseThrow());
    }

    @Test
    public void execute_duplicate_modelAndFileUnchanged() throws Exception {
        ModelManager model = new ModelManager();
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(temporaryFolder.resolve("data.json"));
        Logic logic = newLogic(model, storage);
        logic.execute(ADD);
        logic.execute("addevent evn/NUS Student Life Fair st/09-10-2026 et/10-10-2026");
        CoordiMate before = new CoordiMate(model.getCoordiMate());
        String beforeFile = Files.readString(storage.getCoordiMateFilePath());
        assertEquals(EditEventCommand.MESSAGE_DUPLICATE_EVENT,
                assertThrows(CommandException.class, () -> logic.execute(EDIT)).getMessage());
        assertEquals(before, model.getCoordiMate());
        assertEquals(beforeFile, Files.readString(storage.getCoordiMateFilePath()));
    }

    @Test
    public void execute_saveFailure_modelAndFileUnchanged() throws Exception {
        ModelManager model = new ModelManager();
        Path path = temporaryFolder.resolve("data.json");
        newLogic(model, new JsonCoordiMateStorage(path)).execute(ADD);
        CoordiMate before = new CoordiMate(model.getCoordiMate());
        String beforeFile = Files.readString(path);
        JsonCoordiMateStorage failingStorage = new JsonCoordiMateStorage(path) {
            @Override
            public void saveCoordiMate(ReadOnlyCoordiMate data) throws IOException {
                throw new IOException("Disk full");
            }
        };
        assertEquals(EditEventCommand.MESSAGE_SAVE_ERROR,
                assertThrows(CommandException.class, () -> newLogic(model, failingStorage).execute(EDIT)).getMessage());
        assertEquals(before, model.getCoordiMate());
        assertEquals(beforeFile, Files.readString(path));
    }

    private Logic newLogic(ModelManager model, JsonCoordiMateStorage storage) {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));
    }
}
