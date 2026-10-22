package coordimate.logic;

import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BENSON;
import static coordimate.testutil.TypicalPersons.CARL;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import coordimate.logic.commands.DeleteCommand;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.CoordiMate;
import coordimate.model.ModelManager;
import coordimate.model.UserPrefs;
import coordimate.model.event.AttendanceStatus;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.person.Person;
import coordimate.storage.JsonCoordiMateStorage;
import coordimate.storage.JsonUserPrefsStorage;
import coordimate.storage.StorageManager;
import coordimate.testutil.PersonBuilder;

public class PermanentMemberIdCommandsTest {
    private static final Person ALICE_12 = new PersonBuilder(ALICE).withMemberId(12).build();
    private static final Person BENSON_45 = new PersonBuilder(BENSON).withMemberId(45).withTags("Sponsor").build();
    private static final Person CARL_99 = new PersonBuilder(CARL).withMemberId(99).build();
    private static final List<String> VIEWS = List.of("list", "search Alice", "search Benson",
            "filter tag/Sponsor", "members evn/Concert", "search Nobody");

    @TempDir
    public Path testFolder;

    private ModelManager model;
    private JsonCoordiMateStorage storage;
    private LogicManager logic;

    @Test
    public void legacyMigration_throughCommandsAndRestarts_preservesIdsAndDeletedIdGaps() throws Exception {
        Path legacyFile = Path.of("src", "test", "data", "JsonSerializableCoordiMateTest",
                "typicalPersonsCoordiMate.json");
        storage = new JsonCoordiMateStorage(testFolder.resolve("members.json"));
        Files.copy(legacyFile, storage.getCoordiMateFilePath());
        model = new ModelManager(storage.readCoordiMate().orElseThrow(), new UserPrefs());
        logic = newLogic();
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7), model.getCoordiMate().getPersonList().stream()
                .map(person -> person.getMemberId().orElseThrow().getValue()).toList());
        assertArrayEquals(Files.readAllBytes(legacyFile), Files.readAllBytes(storage.getCoordiMateFilePath()));

        logic.execute("search Carl");
        assertEquals(3, model.getFilteredPersonList().getFirst().getMemberId().orElseThrow().getValue());
        logic.execute("edit 3 r/Treasurer t/Sponsor");
        Person editedCarl = model.getCoordiMate().getPersonList().get(2);
        assertEquals(3, editedCarl.getMemberId().orElseThrow().getValue());
        assertEquals("Treasurer", editedCarl.getRole().toString());
        logic.execute("filter tag/Sponsor");
        assertEquals(List.of(editedCarl), model.getFilteredPersonList());
        logic.execute("addevent evn/Concert st/08-08-2026 et/08-08-2026");
        logic.execute("assign evn/Concert c/3 1");
        logic.execute("markattendance evn/Concert mem/Carl Kurz att/present");
        logic.execute("unassign evn/Concert c/1");
        logic.execute("delete 2");
        logic.execute("y");
        logic.execute("delete 7");
        logic.execute("y");
        List<Person> remaining = List.copyOf(model.getCoordiMate().getPersonList());
        assertEquals(List.of(1, 3, 4, 5, 6), remaining.stream()
                .map(person -> person.getMemberId().orElseThrow().getValue()).toList());

        storage = new JsonCoordiMateStorage(storage.getCoordiMateFilePath());
        model = new ModelManager(storage.readCoordiMate().orElseThrow(), new UserPrefs());
        logic = newLogic();
        assertEquals(remaining, model.getCoordiMate().getPersonList());
        assertEquals(8, model.getCoordiMate().getNextMemberId());
        logic.execute("add n/Amy Bee p/85355255 e/amy@gmail.com r/Member");
        Person amy = model.getCoordiMate().getPersonList().getLast();
        assertEquals(8, amy.getMemberId().orElseThrow().getValue());
        assertEquals(remaining, model.getCoordiMate().getPersonList().subList(0, remaining.size()));
        logic.execute("members evn/Concert");
        assertEquals(List.of(editedCarl), model.getFilteredPersonList());
        assertEquals(Map.of(editedCarl.getName(), AttendanceStatus.PRESENT),
                model.getCoordiMate().getEventList().getFirst().getAttendanceRecord());

        CoordiMate before = new CoordiMate(model.getCoordiMate());
        byte[] savedBefore = Files.readAllBytes(storage.getCoordiMateFilePath());
        for (String command : List.of("edit 2 r/President", "delete 2",
                "assign evn/Concert c/8 2", "unassign evn/Concert c/3 2")) {
            CommandException error = assertThrows(CommandException.class, () -> logic.execute(command));
            assertTrue(error.getMessage().startsWith(String.format(Messages.MESSAGE_MEMBER_ID_NOT_FOUND, 2)));
            assertEquals(before, model.getCoordiMate());
            assertEquals(List.of(editedCarl), model.getFilteredPersonList());
            assertArrayEquals(savedBefore, Files.readAllBytes(storage.getCoordiMateFilePath()));
            assertFalse(logic.isAwaitingDeleteConfirmation());
        }

        logic.execute("search Nobody");
        logic.execute("unassign evn/Concert c/3");
        logic.execute("assign evn/Concert c/8");
        assertTrue(model.getFilteredPersonList().isEmpty());
        CoordiMate saved = new CoordiMate(model.getCoordiMate());
        model = new ModelManager(new JsonCoordiMateStorage(storage.getCoordiMateFilePath())
                .readCoordiMate().orElseThrow(), new UserPrefs());
        assertEquals(saved, model.getCoordiMate());
        assertEquals(editedCarl, model.getCoordiMate().getPersonList().get(1));
        assertEquals(List.of(amy.getName()), model.getCoordiMate().getEventList().getFirst().getMembers());
        assertTrue(model.getCoordiMate().getEventList().getFirst().getAttendanceRecord().isEmpty());
    }

    @Test
    public void assignIds_acrossSearchesFiltersAndEmptyResults_preservesCommandOrder() throws Exception {
        for (String view : VIEWS) {
            startApplication();
            logic.execute("addevent evn/Fair st/08-08-2026 et/08-08-2026");
            logic.execute(view);
            List<Person> displayed = List.copyOf(model.getFilteredPersonList());

            assertEquals("Assigned 3 member(s) to Fair.",
                    logic.execute("assign evn/Fair c/99 12 99 45").getFeedbackToUser());
            assertEquals(List.of(CARL_99.getName(), ALICE_12.getName(), BENSON_45.getName()),
                    model.getCoordiMate().getEventList().getLast().getMembers(), view);
            assertEquals(displayed, model.getFilteredPersonList());
            assertEquals(List.of(BENSON_45, ALICE_12, CARL_99), model.getCoordiMate().getPersonList());
            assertEquals(model.getCoordiMate(), storage.readCoordiMate().orElseThrow());
        }
    }

    @Test
    public void unassignIds_acrossSearchesFiltersAndEmptyResults_preservesRemainingAttendance() throws Exception {
        for (String view : VIEWS) {
            startApplication();
            logic.execute("markattendance evn/Concert mem/Alice Pauline att/present");
            logic.execute("markattendance evn/Concert mem/Benson Meier att/absent");
            logic.execute(view);

            assertEquals("Removed 1 member(s) from Concert. 1 contact(s) were not assigned.",
                    logic.execute("unassign evn/Concert c/12 99 12").getFeedbackToUser());
            Event concert = model.getCoordiMate().getEventList().getFirst();
            assertEquals(List.of(BENSON_45.getName()), concert.getMembers(), view);
            assertEquals(Map.of(BENSON_45.getName(), AttendanceStatus.ABSENT), concert.getAttendanceRecord());
            assertEquals(List.of(BENSON_45, ALICE_12, CARL_99), model.getCoordiMate().getPersonList());
            assertEquals(model.getCoordiMate(), storage.readCoordiMate().orElseThrow());
        }
    }

    @Test
    public void assignAndUnassign_mixedKnownAndUnknownIds_neverPartiallyChangeData() throws Exception {
        startApplication();
        logic.execute("markattendance evn/Concert mem/Alice Pauline att/present");
        logic.execute("search Carl");
        CoordiMate before = new CoordiMate(model.getCoordiMate());
        byte[] savedBefore = Files.readAllBytes(storage.getCoordiMateFilePath());
        for (int unknownId : new int[] {1, 2, 3, 46}) {
            for (String input : List.of("assign evn/Concert c/99 " + unknownId + " 12",
                    "unassign evn/Concert c/12 " + unknownId + " 45")) {
                CommandException error = assertThrows(CommandException.class, () -> logic.execute(input));
                assertTrue(error.getMessage().startsWith(
                        String.format(Messages.MESSAGE_MEMBER_ID_NOT_FOUND, unknownId)));
                assertEquals(before, model.getCoordiMate());
                assertEquals(List.of(CARL_99), model.getFilteredPersonList());
                assertArrayEquals(savedBefore, Files.readAllBytes(storage.getCoordiMateFilePath()));
            }
        }
    }

    @Test
    public void assignAndUnassign_afterEditsDeletionReorderingAndRestart_useSameIds() throws Exception {
        startApplication();
        logic.execute("assign evn/Concert c/99");
        logic.execute("markattendance evn/Concert mem/Carl Kurz att/present");
        logic.execute("edit 12 n/Alice Tan");
        logic.execute("delete 45");
        logic.execute("y");
        logic.execute("add n/Amy Bee p/85355255 e/amy@gmail.com r/Member");
        Person newMember = model.getCoordiMate().getPersonList().getLast();
        assertEquals(100, newMember.getMemberId().orElseThrow().getValue());
        Person renamedAlice = model.getCoordiMate().getPersonList().getFirst();
        CoordiMate reordered = new CoordiMate(model.getCoordiMate());
        reordered.setPersons(List.of(newMember, CARL_99, renamedAlice));
        storage.saveCoordiMate(reordered);

        model = new ModelManager(new JsonCoordiMateStorage(storage.getCoordiMateFilePath())
                .readCoordiMate().orElseThrow(), new UserPrefs());
        logic = newLogic();
        logic.execute("search Nobody");
        logic.execute("unassign evn/Concert c/12");
        logic.execute("assign evn/Concert c/100");
        Event concert = model.getCoordiMate().getEventList().getFirst();
        assertEquals(List.of(CARL_99.getName(), newMember.getName()), concert.getMembers());
        assertEquals(Map.of(CARL_99.getName(), AttendanceStatus.PRESENT), concert.getAttendanceRecord());
        assertEquals(List.of(newMember, CARL_99, renamedAlice), model.getCoordiMate().getPersonList());
        assertThrows(CommandException.class, () -> logic.execute("assign evn/Concert c/45"));
        assertTrue(model.getFilteredPersonList().isEmpty());
        assertEquals(model.getCoordiMate(), storage.readCoordiMate().orElseThrow());
    }

    @Test
    public void editId_acrossSearchesFiltersAndEmptyResults_targetsSameSavedMember() throws Exception {
        startApplication();
        for (int i = 0; i < VIEWS.size(); i++) {
            logic.execute(VIEWS.get(i));
            logic.execute("edit 12 m/Update " + i);
            assertEquals(new PersonBuilder(ALICE_12).withNote("Update " + i).build(),
                    model.getCoordiMate().getPersonList().get(1));
            assertEquals(BENSON_45, model.getCoordiMate().getPersonList().getFirst());
            assertEquals(CARL_99, model.getCoordiMate().getPersonList().getLast());
            assertEquals(model.getCoordiMate(), storage.readCoordiMate().orElseThrow());
        }
    }

    @Test
    public void deleteId_acrossSearchesFiltersAndEmptyResults_targetsSameSavedMember() throws Exception {
        for (String view : VIEWS) {
            startApplication();
            logic.execute(view);
            assertEquals(DeleteCommand.confirmationPrompt(ALICE_12), logic.execute("delete 12").getFeedbackToUser());
            logic.execute("y");
            assertEquals(List.of(BENSON_45, CARL_99), model.getCoordiMate().getPersonList(), view);
            assertEquals(List.of(BENSON_45.getName()), model.getCoordiMate().getEventList().getFirst().getMembers());
            assertEquals(model.getCoordiMate(), storage.readCoordiMate().orElseThrow());
        }
    }

    @Test
    public void unknownIds_neverFallBackToDisplayedPositionsOrChangeData() throws Exception {
        startApplication();
        logic.execute("search Benson");
        CoordiMate before = new CoordiMate(model.getCoordiMate());
        byte[] savedBefore = Files.readAllBytes(storage.getCoordiMateFilePath());
        for (int id : new int[] {1, 2, 3, 46}) {
            for (String input : List.of("edit " + id + " r/President", "delete " + id)) {
                CommandException error = assertThrows(CommandException.class, () -> logic.execute(input));
                assertTrue(error.getMessage().startsWith(String.format(Messages.MESSAGE_MEMBER_ID_NOT_FOUND, id)));
                assertEquals(before, model.getCoordiMate());
                assertEquals(List.of(BENSON_45), model.getFilteredPersonList());
                assertArrayEquals(savedBefore, Files.readAllBytes(storage.getCoordiMateFilePath()));
                assertFalse(logic.isAwaitingDeleteConfirmation());
            }
        }
    }

    @Test
    public void afterDeletionAndRestart_idsStillTargetSameMembers() throws Exception {
        startApplication();
        logic.execute("delete 45");
        logic.execute("y");
        model = new ModelManager(storage.readCoordiMate().orElseThrow(), new UserPrefs());
        logic = newLogic();
        logic.execute("search Alice");
        logic.execute("edit 12 r/President");
        assertEquals(new PersonBuilder(ALICE_12).withRole("President").build(),
                model.getCoordiMate().getPersonList().getFirst());
        assertThrows(CommandException.class, () -> logic.execute("delete 45"));
        assertEquals(List.of(CARL_99), model.getCoordiMate().getPersonList().subList(1, 2));
        assertEquals(model.getCoordiMate(), storage.readCoordiMate().orElseThrow());
    }

    @Test
    public void confirmation_afterReorderingAndFiltering_deletesOriginalId() throws Exception {
        startApplication();
        logic.execute("delete 12");
        CoordiMate reordered = new CoordiMate(model.getCoordiMate());
        reordered.setPersons(List.of(CARL_99, BENSON_45, ALICE_12));
        model.setCoordiMate(reordered);
        model.updateFilteredPersonList(person -> false);
        logic.execute("y");
        assertEquals(List.of(CARL_99, BENSON_45), model.getCoordiMate().getPersonList());
        assertEquals(model.getCoordiMate(), storage.readCoordiMate().orElseThrow());
    }

    @Test
    public void confirmation_memberEditedSincePrompt_requiresNewConfirmation() throws Exception {
        startApplication();
        logic.execute("delete 12");
        model.setPerson(ALICE_12, new PersonBuilder(ALICE_12).withRole("President").build());
        CoordiMate before = new CoordiMate(model.getCoordiMate());
        byte[] savedBefore = Files.readAllBytes(storage.getCoordiMateFilePath());
        CommandException error = assertThrows(CommandException.class, () -> logic.execute("y"));
        assertTrue(error.getMessage().startsWith(String.format(DeleteCommand.MESSAGE_TARGET_CHANGED, 12)));
        assertEquals(before, model.getCoordiMate());
        assertArrayEquals(savedBefore, Files.readAllBytes(storage.getCoordiMateFilePath()));
        assertFalse(logic.isAwaitingDeleteConfirmation());
    }

    @Test
    public void confirmation_memberRemovedSincePrompt_neverDeletesReplacement() throws Exception {
        startApplication();
        logic.execute("delete 12");
        model.deletePerson(ALICE_12);
        model.addPerson(new PersonBuilder().build());
        CoordiMate before = new CoordiMate(model.getCoordiMate());
        byte[] savedBefore = Files.readAllBytes(storage.getCoordiMateFilePath());
        CommandException error = assertThrows(CommandException.class, () -> logic.execute("y"));
        assertTrue(error.getMessage().startsWith(String.format(Messages.MESSAGE_MEMBER_ID_NOT_FOUND, 12)));
        assertEquals(before, model.getCoordiMate());
        assertArrayEquals(savedBefore, Files.readAllBytes(storage.getCoordiMateFilePath()));
        assertFalse(logic.isAwaitingDeleteConfirmation());
    }

    private void startApplication() throws IOException {
        CoordiMate data = new CoordiMate();
        data.setPersons(List.of(BENSON_45, ALICE_12, CARL_99));
        EventTime time = new EventTime("08-08-2026");
        data.addEvent(new Event("Concert", time, time, List.of(ALICE_12.getName(), BENSON_45.getName())));
        model = new ModelManager(data, new UserPrefs());
        storage = new JsonCoordiMateStorage(testFolder.resolve("members.json"));
        storage.saveCoordiMate(model.getCoordiMate());
        logic = newLogic();
    }

    private LogicManager newLogic() {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(testFolder.resolve("prefs.json"))));
    }
}
