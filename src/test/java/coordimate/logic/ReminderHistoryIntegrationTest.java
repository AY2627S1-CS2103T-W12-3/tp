package coordimate.logic;

import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BENSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import coordimate.commons.exceptions.DataLoadingException;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.ModelManager;
import coordimate.model.event.AttendanceStatus;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.person.Name;
import coordimate.storage.JsonCoordiMateStorage;
import coordimate.storage.JsonUserPrefsStorage;
import coordimate.storage.StorageManager;

public class ReminderHistoryIntegrationTest {
    private static final EventTime START = new EventTime("20-10-2026 15:00");
    private static final EventTime END = new EventTime("21-10-2026 18:00");

    @TempDir
    public Path temporaryFolder;

    private ModelManager model;
    private JsonCoordiMateStorage storage;
    private Logic logic;

    @BeforeEach
    public void setUp() throws Exception {
        model = new ModelManager();
        model.addPerson(ALICE);
        model.addPerson(BENSON);
        model.addEvent(new Event("Concert", START, END, List.of(ALICE.getName()),
                Map.of(ALICE.getName(), AttendanceStatus.PRESENT)).withReminderDismissed());
        storage = new JsonCoordiMateStorage(temporaryFolder.resolve("data.json"));
        storage.saveCoordiMate(model.getCoordiMate());
        logic = newLogic(model);
    }

    @Test
    public void reload_dismissedEvent_retainsHistoryAndEventDetails() throws Exception {
        ModelManager reloaded = new ModelManager(storage.readCoordiMate().orElseThrow(), model.getUserPrefs());
        Event loaded = reloaded.getCoordiMate().getEventList().getFirst();

        assertEquals(model.getCoordiMate(), reloaded.getCoordiMate());
        assertTrue(loaded.isReminderDismissed());
        assertEquals(Set.of(START), loaded.getDismissedReminderStarts());
        newLogic(reloaded).execute("editevent evn/Concert nevn/Renamed Concert");
        assertTrue(storage.readCoordiMate().orElseThrow().getEventList().getFirst().isReminderDismissed());
    }

    @Test
    public void execute_renameAndEndOnlyEdit_preservesDismissal() throws Exception {
        logic.execute("editevent evn/Concert nevn/Final Concert et/22-10-2026");

        Event saved = savedEvent();
        assertTrue(saved.isReminderDismissed());
        assertEquals(Set.of(START), saved.getDismissedReminderStarts());
        assertEquals("Final Concert", saved.getName());
        assertEquals(new EventTime("22-10-2026"), saved.getEndTime());
        assertEquals(Map.of(ALICE.getName(), AttendanceStatus.PRESENT), saved.getAttendanceRecord());
    }

    @Test
    public void execute_startEditsAndReversion_resetsDismissal() throws Exception {
        EventTime revisedStart = new EventTime("21-10-2026 16:00");
        logic.execute("editevent evn/Concert st/21-10-2026 16:00");
        assertFalse(savedEvent().isReminderDismissed());
        assertEquals(revisedStart, savedEvent().getStartTime());
        assertEquals(Set.of(), savedEvent().getDismissedReminderStarts());
        Event revised = model.getCoordiMate().getEventList().getFirst();
        model.setEvent(revised, revised.withReminderDismissed());
        storage.saveCoordiMate(model.getCoordiMate());
        logic = newLogic(model);

        logic.execute("editevent evn/Concert st/20-10-2026 15:00");

        Event reverted = savedEvent();
        assertFalse(reverted.isReminderDismissed());
        assertEquals(Set.of(), reverted.getDismissedReminderStarts());
    }

    @Test
    public void execute_dateOnlyAndExplicitMidnight_distinguishesDismissals() throws Exception {
        logic.execute("editevent evn/Concert st/20-10-2026");
        assertFalse(savedEvent().isReminderDismissed());
        Event dateOnly = model.getCoordiMate().getEventList().getFirst();
        model.setEvent(dateOnly, dateOnly.withReminderDismissed());
        storage.saveCoordiMate(model.getCoordiMate());
        logic = newLogic(model);

        logic.execute("editevent evn/Concert st/20-10-2026 00:00");
        assertFalse(savedEvent().isReminderDismissed());
        logic.execute("editevent evn/Concert st/20-10-2026");
        assertFalse(savedEvent().isReminderDismissed());
        assertEquals(Set.of(), savedEvent().getDismissedReminderStarts());
    }

    @Test
    public void execute_assignAndUnassign_preservesHistoryAndRemainingAttendance() throws Exception {
        logic.execute("assign evn/Concert c/2");
        assertTrue(savedEvent().isReminderDismissed());
        assertEquals(List.of(ALICE.getName(), BENSON.getName()), savedEvent().getMembers());

        logic.execute("unassign evn/Concert c/2");
        Event saved = savedEvent();
        assertTrue(saved.isReminderDismissed());
        assertEquals(Set.of(START), saved.getDismissedReminderStarts());
        assertEquals(List.of(ALICE.getName()), saved.getMembers());
        assertEquals(Map.of(ALICE.getName(), AttendanceStatus.PRESENT), saved.getAttendanceRecord());
    }

    @Test
    public void execute_markAttendance_preservesDismissal() throws Exception {
        logic.execute("markattendance evn/Concert mem/" + ALICE.getName().getFullName() + " att/absent");

        Event saved = savedEvent();
        assertTrue(saved.isReminderDismissed());
        assertEquals(Set.of(START), saved.getDismissedReminderStarts());
        assertEquals(Map.of(ALICE.getName(), AttendanceStatus.ABSENT), saved.getAttendanceRecord());
    }

    @Test
    public void execute_contactRenameAndDeletion_preservesDismissal() throws Exception {
        logic.execute("edit 1 n/Alice Renamed");
        Event renamed = savedEvent();
        assertTrue(renamed.isReminderDismissed());
        assertEquals(List.of(new Name("Alice Renamed")), renamed.getMembers());
        assertEquals(Map.of(new Name("Alice Renamed"), AttendanceStatus.PRESENT), renamed.getAttendanceRecord());

        logic.execute("delete 1");
        logic.execute("y");

        Event saved = savedEvent();
        assertTrue(saved.isReminderDismissed());
        assertEquals(Set.of(START), saved.getDismissedReminderStarts());
        assertEquals(List.of(), saved.getMembers());
        assertEquals(Map.of(), saved.getAttendanceRecord());
    }

    @Test
    public void execute_deleteThenRecreate_doesNotReuseOldHistory() throws Exception {
        logic.execute("deleteevent evn/Concert");
        assertEquals(List.of(), storage.readCoordiMate().orElseThrow().getEventList());
        logic.execute("addevent evn/Concert st/20-10-2026 15:00 et/21-10-2026 18:00");

        assertFalse(savedEvent().isReminderDismissed());
        assertEquals(Set.of(), savedEvent().getDismissedReminderStarts());
    }

    @Test
    public void execute_clear_removesEventsAndHistory() throws Exception {
        logic.execute("clear");

        assertEquals(List.of(), model.getCoordiMate().getEventList());
        assertEquals(List.of(), storage.readCoordiMate().orElseThrow().getEventList());
    }

    @Test
    public void execute_invalidSchedule_leavesHistoryAndFileUnchanged() throws Exception {
        String before = Files.readString(storage.getCoordiMateFilePath());

        assertThrows(CommandException.class, () -> logic.execute("editevent evn/Concert st/22-10-2026"));

        assertTrue(savedEvent().isReminderDismissed());
        assertEquals(Set.of(START), savedEvent().getDismissedReminderStarts());
        assertEquals(before, Files.readString(storage.getCoordiMateFilePath()));
    }

    @Test
    public void readCoordiMate_invalidHistory_usesExistingLoadingError() throws Exception {
        for (String history : List.of("[null]", "[\"31-02-2026\"]", "[\"20-10-2026 24:00\"]", "{}")) {
            Files.writeString(storage.getCoordiMateFilePath(), """
                    {"persons":[],"events":[{"name":"Concert","startTime":"20-10-2026","endTime":"21-10-2026",
                    "dismissedReminderStarts":%s}]}
                    """.formatted(history));

            assertThrows(DataLoadingException.class, storage::readCoordiMate);
        }
    }

    private Logic newLogic(ModelManager model) {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));
    }

    private Event savedEvent() throws Exception {
        JsonCoordiMateStorage reloadedStorage = new JsonCoordiMateStorage(storage.getCoordiMateFilePath());
        var saved = reloadedStorage.readCoordiMate().orElseThrow();
        assertEquals(model.getCoordiMate(), saved);
        return saved.getEventList().getFirst();
    }
}
