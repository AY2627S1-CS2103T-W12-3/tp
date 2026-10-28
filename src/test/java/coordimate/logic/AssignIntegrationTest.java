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

import coordimate.logic.commands.AssignCommand;
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
import coordimate.testutil.PersonBuilder;

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
    public void execute_filteredContactList_assignsPermanentId() throws Exception {
        ModelManager model = new ModelManager(getTypicalCoordiMate(), new UserPrefs());
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(temporaryFolder.resolve("data.json"));
        Logic logic = newLogic(model, storage);
        logic.execute(ADD);
        model.updateFilteredPersonList(person -> person.equals(CARL));
        logic.execute("assign evn/Final Concert c/3");
        assertEquals(List.of(CARL.getName()), model.getCoordiMate().getEventList().getFirst().getMembers());
        assertEquals(List.of(CARL), model.getFilteredPersonList());
        assertEquals(Messages.withUsage(String.format(AssignCommand.MESSAGE_UNKNOWN_MEMBER_ID,
                99), AssignCommand.MESSAGE_USAGE),
                assertThrows(CommandException.class, () -> logic.execute("assign evn/Final Concert c/99"))
                        .getMessage());
    }

    @Test
    public void execute_batchContainsContactOnLeave_assignsAndSavesAvailableMembers() throws Exception {
        ModelManager model = new ModelManager(getTypicalCoordiMate(), new UserPrefs());
        model.setPerson(ALICE, new PersonBuilder(ALICE).withLeaveDate("08-08-2026").build());
        Path path = temporaryFolder.resolve("data.json");
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(path);
        Logic logic = newLogic(model, storage);
        logic.execute(ADD);
        logic.execute("assign evn/Final Concert c/2");
        logic.execute("markattendance evn/Final Concert mem/Benson Meier att/present");
        model.updateFilteredPersonList(person -> person.equals(CARL));
        CoordiMate expected = new CoordiMate(model.getCoordiMate());
        Event before = expected.getEventList().getFirst();
        expected.setEvent(before, before.withMembersAndAttendance(List.of(BENSON.getName(), CARL.getName()),
                Map.of(BENSON.getName(), AttendanceStatus.PRESENT)));

        assertEquals("Assigned 1 member(s) to Final Concert. "
                + "Skipped 1 contact(s) on leave for the entire event: Alice Pauline (ID: 1).",
                logic.execute("assign evn/Final Concert c/3 1").getFeedbackToUser());
        assertEquals(expected, model.getCoordiMate());
        assertEquals(List.of(CARL), model.getFilteredPersonList());
        assertEquals(expected, storage.readCoordiMate().orElseThrow());
    }

    @Test
    public void execute_onlyLeaveAndExistingMembers_modelAndFileUnchanged() throws Exception {
        ModelManager model = new ModelManager(getTypicalCoordiMate(), new UserPrefs());
        model.setPerson(ALICE, new PersonBuilder(ALICE).withLeaveDate("08-08-2026").build());
        Path path = temporaryFolder.resolve("data.json");
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(path);
        Logic logic = newLogic(model, storage);
        logic.execute(ADD);
        logic.execute("assign evn/Final Concert c/2");
        CoordiMate before = new CoordiMate(model.getCoordiMate());
        String beforeFile = Files.readString(path);
        String message = "No new members could be assigned to Final Concert. No changes were made. "
                + "1 contact(s) were already assigned. "
                + "Skipped 1 contact(s) on leave for the entire event: Alice Pauline (ID: 1).";

        assertEquals(Messages.withUsage(message, AssignCommand.MESSAGE_USAGE),
                assertThrows(CommandException.class, () -> logic.execute("assign evn/Final Concert c/1 2"))
                        .getMessage());
        assertEquals(before, model.getCoordiMate());
        assertEquals(beforeFile, Files.readString(path));
    }

    @Test
    public void execute_reloadedLeaveComparedWithEventEnd() throws Exception {
        ModelManager model = new ModelManager(getTypicalCoordiMate(), new UserPrefs());
        model.setPerson(ALICE, new PersonBuilder(ALICE).withLeaveDate("08-08-2026").build());
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(temporaryFolder.resolve("data.json"));
        newLogic(model, storage).execute(ADD);
        ModelManager reloaded = new ModelManager(storage.readCoordiMate().orElseThrow(), new UserPrefs());
        Logic logic = newLogic(reloaded, storage);

        assertThrows(CommandException.class, () -> logic.execute("assign evn/Final Concert c/1"));
        logic.execute("editevent evn/Final Concert et/09-08-2026 00:00");
        assertEquals("Assigned 1 member(s) to Final Concert.",
                logic.execute("assign evn/Final Concert c/1").getFeedbackToUser());
        ReadOnlyCoordiMate saved = storage.readCoordiMate().orElseThrow();
        assertEquals(reloaded.getCoordiMate(), saved);
        assertEquals(List.of(ALICE.getName()), saved.getEventList().getFirst().getMembers());
    }

    @Test
    public void execute_deleteAssignedContact_savedFileStillLoads() throws Exception {
        ModelManager model = new ModelManager(getTypicalCoordiMate(), new UserPrefs());
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(temporaryFolder.resolve("data.json"));
        Logic logic = newLogic(model, storage);
        logic.execute(ADD);
        logic.execute("assign evn/Final Concert c/1 2");
        logic.execute("delete 1");
        logic.execute("y");
        ReadOnlyCoordiMate saved = storage.readCoordiMate().orElseThrow();
        assertEquals(List.of(BENSON.getName()), saved.getEventList().getFirst().getMembers());
    }

    @Test
    public void execute_saveFailure_modelAndFileUnchanged() throws Exception {
        ModelManager model = new ModelManager(getTypicalCoordiMate(), new UserPrefs());
        model.setPerson(ALICE, new PersonBuilder(ALICE).withLeaveDate("08-08-2026").build());
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
        assertEquals(Messages.withUsage(AssignCommand.MESSAGE_SAVE_ERROR,
                AssignCommand.MESSAGE_USAGE), assertThrows(CommandException.class, () ->
                newLogic(model, failingStorage).execute("assign evn/Final Concert c/1 2")).getMessage());
        assertEquals(before, model.getCoordiMate());
        assertEquals(beforeFile, Files.readString(path));
    }

    private Logic newLogic(ModelManager model, JsonCoordiMateStorage storage) {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));
    }
}
