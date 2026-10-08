package coordimate.logic;

import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BENSON;
import static coordimate.testutil.TypicalPersons.CARL;
import static coordimate.testutil.TypicalPersons.getTypicalCoordiMate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import coordimate.logic.commands.UnassignCommand;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.CoordiMate;
import coordimate.model.ModelManager;
import coordimate.model.ReadOnlyCoordiMate;
import coordimate.model.UserPrefs;
import coordimate.model.event.AttendanceStatus;
import coordimate.model.event.Event;
import coordimate.storage.JsonCoordiMateStorage;
import coordimate.storage.JsonUserPrefsStorage;
import coordimate.storage.StorageManager;

public class UnassignIntegrationTest {
    private static final String ADD = "addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00";
    private static final String ASSIGN = "assign evn/Final Concert c/1 2 3";

    @TempDir
    public Path temporaryFolder;

    @Test
    public void execute_unassign_savedAndReloadedWithRemainingAttendance() throws Exception {
        ModelManager model = new ModelManager(getTypicalCoordiMate(), new UserPrefs());
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(temporaryFolder.resolve("data.json"));
        Logic logic = newLogic(model, storage);
        logic.execute(ADD);
        logic.execute(ASSIGN);
        Event assigned = model.getCoordiMate().getEventList().getFirst();
        model.setEvent(assigned, assigned.withAttendance(ALICE.getName(), AttendanceStatus.PRESENT)
                .withAttendance(BENSON.getName(), AttendanceStatus.ABSENT));

        assertEquals("Removed 1 member(s) from Final Concert.",
                logic.execute("unassign evn/final concert c/1").getFeedbackToUser());
        ReadOnlyCoordiMate saved = storage.readCoordiMate().orElseThrow();
        assertEquals(model.getCoordiMate(), saved);
        Event savedEvent = saved.getEventList().getFirst();
        assertEquals(List.of(BENSON.getName(), CARL.getName()), savedEvent.getMembers());
        assertEquals(Map.of(BENSON.getName(), AttendanceStatus.ABSENT), savedEvent.getAttendanceRecord());
    }

    @Test
    public void execute_filteredContactList_removesDisplayedContact() throws Exception {
        ModelManager model = new ModelManager(getTypicalCoordiMate(), new UserPrefs());
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(temporaryFolder.resolve("data.json"));
        Logic logic = newLogic(model, storage);
        logic.execute(ADD);
        logic.execute(ASSIGN);
        model.updateFilteredPersonList(person -> person.equals(CARL));
        logic.execute("unassign evn/Final Concert c/1");
        assertEquals(List.of(ALICE.getName(), BENSON.getName()),
                model.getCoordiMate().getEventList().getFirst().getMembers());
        assertEquals(List.of(CARL), model.getFilteredPersonList());
        assertEquals(String.format(UnassignCommand.MESSAGE_CONTACT_NOT_FOUND, 2),
                assertThrows(CommandException.class, () -> logic.execute("unassign evn/Final Concert c/2"))
                        .getMessage());
    }

    @Test
    public void execute_saveFailure_modelAndFileUnchanged() throws Exception {
        ModelManager model = new ModelManager(getTypicalCoordiMate(), new UserPrefs());
        Path path = temporaryFolder.resolve("data.json");
        Logic logic = newLogic(model, new JsonCoordiMateStorage(path));
        logic.execute(ADD);
        logic.execute(ASSIGN);
        CoordiMate before = new CoordiMate(model.getCoordiMate());
        String beforeFile = Files.readString(path);
        JsonCoordiMateStorage failingStorage = new JsonCoordiMateStorage(path) {
            @Override
            public void saveCoordiMate(ReadOnlyCoordiMate data) throws IOException {
                throw new IOException("Disk full");
            }
        };
        assertEquals(UnassignCommand.MESSAGE_SAVE_ERROR, assertThrows(CommandException.class, () ->
                newLogic(model, failingStorage).execute("unassign evn/Final Concert c/1")).getMessage());
        assertEquals(before, model.getCoordiMate());
        assertEquals(beforeFile, Files.readString(path));
    }

    private Logic newLogic(ModelManager model, JsonCoordiMateStorage storage) {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));
    }
}
