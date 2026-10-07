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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import coordimate.logic.commands.AssignCommand;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.CoordiMate;
import coordimate.model.ModelManager;
import coordimate.model.ReadOnlyCoordiMate;
import coordimate.model.UserPrefs;
import coordimate.storage.JsonCoordiMateStorage;
import coordimate.storage.JsonUserPrefsStorage;
import coordimate.storage.StorageManager;

public class AssignIntegrationTest {
    private static final String ADD = "addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00";

    @TempDir
    public Path temporaryFolder;

    @Test
    public void execute_assign_savedAndReloaded() throws Exception {
        ModelManager model = new ModelManager(getTypicalCoordiMate(), new UserPrefs());
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(temporaryFolder.resolve("data.json"));
        Logic logic = newLogic(model, storage);
        logic.execute(ADD);
        assertEquals("Assigned 2 member(s) to Final Concert.",
                logic.execute("assign evn/final concert c/2 1").getFeedbackToUser());
        ReadOnlyCoordiMate saved = storage.readCoordiMate().orElseThrow();
        assertEquals(model.getCoordiMate(), saved);
        assertEquals(List.of(BENSON.getName(), ALICE.getName()), saved.getEventList().getFirst().getMembers());
    }

    @Test
    public void execute_filteredContactList_assignsDisplayedContact() throws Exception {
        ModelManager model = new ModelManager(getTypicalCoordiMate(), new UserPrefs());
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(temporaryFolder.resolve("data.json"));
        Logic logic = newLogic(model, storage);
        logic.execute(ADD);
        model.updateFilteredPersonList(person -> person.equals(CARL));
        logic.execute("assign evn/Final Concert c/1");
        assertEquals(List.of(CARL.getName()), model.getCoordiMate().getEventList().getFirst().getMembers());
        assertEquals(List.of(CARL), model.getFilteredPersonList());
        assertEquals(String.format(AssignCommand.MESSAGE_CONTACT_NOT_FOUND, 2),
                assertThrows(CommandException.class, () -> logic.execute("assign evn/Final Concert c/2"))
                        .getMessage());
    }

    @Test
    public void execute_deleteAssignedContact_savedFileStillLoads() throws Exception {
        ModelManager model = new ModelManager(getTypicalCoordiMate(), new UserPrefs());
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(temporaryFolder.resolve("data.json"));
        Logic logic = newLogic(model, storage);
        logic.execute(ADD);
        logic.execute("assign evn/Final Concert c/1 2");
        logic.execute("delete 1");
        ReadOnlyCoordiMate saved = storage.readCoordiMate().orElseThrow();
        assertEquals(List.of(BENSON.getName()), saved.getEventList().getFirst().getMembers());
    }

    @Test
    public void execute_saveFailure_modelAndFileUnchanged() throws Exception {
        ModelManager model = new ModelManager(getTypicalCoordiMate(), new UserPrefs());
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
        assertEquals(AssignCommand.MESSAGE_SAVE_ERROR, assertThrows(CommandException.class, () ->
                newLogic(model, failingStorage).execute("assign evn/Final Concert c/1")).getMessage());
        assertEquals(before, model.getCoordiMate());
        assertEquals(beforeFile, Files.readString(path));
    }

    private Logic newLogic(ModelManager model, JsonCoordiMateStorage storage) {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));
    }
}
