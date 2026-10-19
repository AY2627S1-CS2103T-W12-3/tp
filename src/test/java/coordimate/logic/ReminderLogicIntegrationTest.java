package coordimate.logic;

import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BENSON;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import coordimate.commons.exceptions.DataLoadingException;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.logic.reminder.ReminderDismissalException;
import coordimate.model.ModelManager;
import coordimate.model.ReadOnlyCoordiMate;
import coordimate.model.event.AttendanceStatus;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.storage.JsonCoordiMateStorage;
import coordimate.storage.JsonUserPrefsStorage;
import coordimate.storage.StorageManager;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

public class ReminderLogicIntegrationTest {
    private static final EventTime START = new EventTime("20-10-2026 15:00");

    @TempDir
    public Path temporaryFolder;

    private ModelManager model;
    private ControlledStorage storage;
    private MutableClock clock;
    private Logic logic;
    private Event concert;

    @BeforeEach
    public void setUp() throws Exception {
        model = new ModelManager();
        model.addPerson(ALICE);
        model.addPerson(BENSON);
        concert = new Event("Concert", START, new EventTime("21-10-2026 18:00"), List.of(ALICE.getName()),
                Map.of(ALICE.getName(), AttendanceStatus.PRESENT));
        model.addEvent(concert);
        storage = new ControlledStorage(temporaryFolder.resolve("data.json"));
        storage.saveCoordiMate(model.getCoordiMate());
        storage.writes = 0;
        clock = new MutableClock(ZonedDateTime.parse("2026-10-19T15:00:00+08:00").toInstant());
        logic = newLogic(model);
    }

    @Test
    public void getReminders_initialEvents_returnsOnlyEligibleUndismissedEventsInOrder() {
        Event allDay = event("All day", "20-10-2026");
        Event tonight = event("Tonight", "19-10-2026 18:00");
        model.addEvent(tonight);
        model.addEvent(allDay);
        model.addEvent(event("Dismissed", "19-10-2026 17:00").withReminderDismissed());
        model.addEvent(event("Past", "19-10-2026 14:00"));
        model.addEvent(event("Later", "20-10-2026 15:01"));

        assertEquals(List.of(tonight, allDay, concert), logic.getReminders());
    }

    @Test
    public void refreshReminders_repeatedChecks_doNotDuplicateOrRepublishUnchangedReminders() {
        ObservableList<Event> view = logic.getReminders();
        AtomicInteger changes = new AtomicInteger();
        view.addListener((ListChangeListener<Event>) change -> changes.incrementAndGet());

        logic.refreshReminders();
        logic.refreshReminders();

        assertSame(view, logic.getReminders());
        assertEquals(List.of(concert), view);
        assertEquals(0, changes.get());
        assertEquals(0, storage.reads);
        assertEquals(0, storage.writes);
        assertThrows(UnsupportedOperationException.class, () -> view.add(concert));
    }

    @Test
    public void refreshReminders_timePasses_updatesExistingViewWithoutStorageAccess() {
        Event later = event("Later", "20-10-2026 15:01");
        model.addEvent(later);
        ObservableList<Event> view = logic.getReminders();
        assertEquals(List.of(concert), view);

        clock.advance(Duration.ofMinutes(1));
        logic.refreshReminders();
        assertEquals(List.of(concert, later), view);
        clock.advance(Duration.ofHours(24));
        logic.refreshReminders();
        assertEquals(List.of(later), view);
        clock.advance(Duration.ofNanos(1));
        logic.refreshReminders();
        assertEquals(List.of(), view);
        assertEquals(0, storage.reads);
        assertEquals(0, storage.writes);
    }

    @Test
    public void refreshReminders_dateOnly_entersOnPreviousDayAndExpiresAfterStartDate() throws Exception {
        logic.execute("editevent evn/Concert st/21-10-2026");
        ObservableList<Event> view = logic.getReminders();
        assertEquals(List.of(), view);
        byte[] before = Files.readAllBytes(storage.getCoordiMateFilePath());

        clock.advance(Duration.ofHours(9));
        logic.refreshReminders();
        assertEquals(model.getCoordiMate().getEventList(), view);
        clock.advance(Duration.ofHours(24));
        logic.refreshReminders();
        assertEquals(model.getCoordiMate().getEventList(), view);
        clock.advance(Duration.ofHours(24).minusNanos(1));
        logic.refreshReminders();
        assertEquals(model.getCoordiMate().getEventList(), view);
        clock.advance(Duration.ofNanos(1));
        logic.refreshReminders();
        assertEquals(List.of(), view);
        assertArrayEquals(before, Files.readAllBytes(storage.getCoordiMateFilePath()));
    }

    @Test
    public void execute_eventEdit_updatesExistingViewAndSortOrder() throws Exception {
        Event tonight = event("Tonight", "19-10-2026 18:00");
        model.addEvent(tonight);
        ObservableList<Event> view = logic.getReminders();

        logic.execute("editevent evn/Concert nevn/Final Concert st/19-10-2026 17:00 et/19-10-2026 19:00");

        Event updated = model.getCoordiMate().getEventList().getFirst();
        assertEquals(List.of(updated, tonight), view);
        assertEquals("Final Concert", updated.getName());
        assertEquals("19-10-2026 17:00", updated.getStartTime().toString());
        assertEquals("19-10-2026 19:00", updated.getEndTime().toString());
        logic.execute("deleteevent evn/Final Concert");
        assertEquals(List.of(tonight), view);
    }

    @Test
    public void dismissReminder_success_savesImmediatelyAndPreservesContactView() throws Exception {
        model.updateFilteredPersonList(person -> person.equals(BENSON));
        ObservableList<Event> view = logic.getReminders();
        model.getCoordiMate().getEventList().addListener((ListChangeListener<Event>) change ->
                assertDoesNotThrow(() -> assertTrue(savedEvent().isReminderDismissed())));

        logic.dismissReminder(concert);

        assertEquals(List.of(), view);
        assertEquals(1, storage.writes);
        assertTrue(model.getCoordiMate().getEventList().getFirst().isReminderDismissed());
        assertEquals(Set.of(START), savedEvent().getDismissedReminderStarts());
        assertEquals(List.of(BENSON), model.getFilteredPersonList());
        assertEquals(concert.getAttendanceRecord(), savedEvent().getAttendanceRecord());
        assertEquals(concert.getMembers(), savedEvent().getMembers());
    }

    @Test
    public void dismissReminder_restart_keepsSavedReminderDismissed() throws Exception {
        logic.dismissReminder(concert);
        ModelManager reloaded = new ModelManager(storage.readCoordiMate().orElseThrow(), model.getUserPrefs());

        assertEquals(List.of(), newLogic(reloaded).getReminders());
    }

    @Test
    public void dismissReminder_repeatedDismissal_hasNoAdditionalWrites() throws Exception {
        logic.dismissReminder(concert);
        logic.dismissReminder(concert);
        logic.dismissReminder(model.getCoordiMate().getEventList().getFirst());

        assertEquals(1, storage.writes);
        assertEquals(List.of(), logic.getReminders());
    }

    @Test
    public void dismissReminder_outdatedSchedule_doesNotDismissRevisedReminder() throws Exception {
        logic.execute("editevent evn/Concert st/19-10-2026 16:00");
        storage.writes = 0;

        logic.dismissReminder(concert);

        assertEquals(0, storage.writes);
        assertEquals(model.getCoordiMate().getEventList(), logic.getReminders());
        assertFalse(savedEvent().isReminderDismissed());
    }

    @Test
    public void dismissReminder_deletedOrExpiredEvent_hasNoEffect() throws Exception {
        clock.advance(Duration.ofHours(24).plusNanos(1));
        logic.dismissReminder(concert);
        assertEquals(0, storage.writes);

        logic.execute("deleteevent evn/Concert");
        storage.writes = 0;
        logic.dismissReminder(concert);

        assertEquals(0, storage.writes);
        assertEquals(List.of(), logic.getReminders());
    }

    @Test
    public void dismissReminder_saveFailure_warnsAndSuppressesOnlyThisReminderForSession() throws Exception {
        Event other = event("Other", "19-10-2026 18:00");
        model.addEvent(other);
        storage.saveCoordiMate(model.getCoordiMate());
        byte[] before = Files.readAllBytes(storage.getCoordiMateFilePath());
        storage.writes = 0;
        storage.failSave = true;

        ReminderDismissalException error = assertThrows(ReminderDismissalException.class, () ->
                logic.dismissReminder(concert));

        assertEquals(ReminderDismissalException.MESSAGE, error.getMessage());
        assertSame(storage.saveFailure, error.getCause());
        assertEquals(List.of(other), logic.getReminders());
        assertEquals(1, storage.writes);
        assertFalse(model.getCoordiMate().getEventList().getFirst().isReminderDismissed());
        assertFalse(savedEvent().isReminderDismissed());
        assertArrayEquals(before, Files.readAllBytes(storage.getCoordiMateFilePath()));

        logic.refreshReminders();
        logic.dismissReminder(concert);
        assertEquals(List.of(other), logic.getReminders());
        assertEquals(1, storage.writes);
        ModelManager reloaded = new ModelManager(storage.readCoordiMate().orElseThrow(), model.getUserPrefs());
        assertEquals(List.of(other, concert), newLogic(reloaded).getReminders());
    }

    @Test
    public void dismissReminder_invalidDataFile_preservesFileAndUsesSessionSuppression() throws Exception {
        Files.writeString(storage.getCoordiMateFilePath(), "invalid JSON");

        ReminderDismissalException error = assertThrows(ReminderDismissalException.class, () ->
                logic.dismissReminder(concert));

        assertEquals(ReminderDismissalException.MESSAGE, error.getMessage());
        assertTrue(error.getCause() instanceof DataLoadingException);
        assertEquals("invalid JSON", Files.readString(storage.getCoordiMateFilePath()));
        assertEquals(0, storage.writes);
        assertFalse(model.getCoordiMate().getEventList().getFirst().isReminderDismissed());
        assertEquals(List.of(), logic.getReminders());
    }

    @Test
    public void execute_renameAfterFailedDismissal_preservesTemporarySuppression() throws Exception {
        failDismissal();
        storage.failSave = false;

        logic.execute("editevent evn/Concert nevn/Final Concert");
        assertEquals(List.of(), logic.getReminders());
        logic.execute("editevent evn/Final Concert nevn/FINAL CONCERT et/22-10-2026");
        assertEquals(List.of(), logic.getReminders());
        assertEquals(Set.of(), savedEvent().getDismissedReminderStarts());
        logic.execute("editevent evn/FINAL CONCERT st/19-10-2026 16:00");
        assertEquals(1, logic.getReminders().size());
        logic.execute("editevent evn/FINAL CONCERT st/20-10-2026 15:00");
        assertEquals(1, logic.getReminders().size());
    }

    @Test
    public void execute_failedRename_doesNotMoveTemporarySuppression() throws Exception {
        failDismissal();

        assertThrows(CommandException.class, () -> logic.execute("editevent evn/Concert nevn/Final Concert"));

        assertEquals("Concert", model.getCoordiMate().getEventList().getFirst().getName());
        assertEquals(List.of(), logic.getReminders());
        storage.failSave = false;
        logic.execute("editevent evn/Concert nevn/Final Concert");
        assertEquals(List.of(), logic.getReminders());
    }

    @Test
    public void execute_deleteAndRecreate_forgetsTemporarySuppression() throws Exception {
        failDismissal();
        storage.failSave = false;

        logic.execute("deleteevent evn/Concert");
        logic.execute("addevent evn/Concert st/20-10-2026 15:00 et/21-10-2026 18:00");

        assertEquals(1, logic.getReminders().size());
        assertFalse(savedEvent().isReminderDismissed());
    }

    @Test
    public void execute_clearAndRecreate_forgetsTemporarySuppression() throws Exception {
        failDismissal();
        storage.failSave = false;
        ObservableList<Event> view = logic.getReminders();

        logic.execute("clear");
        assertEquals(List.of(), view);
        logic.execute("addevent evn/Concert st/20-10-2026 15:00 et/21-10-2026 18:00");

        assertEquals(1, view.size());
    }

    @Test
    public void dismissReminder_newScheduleSaved_clearsFailedOldDismissal() throws Exception {
        failDismissal();
        storage.failSave = false;
        logic.execute("editevent evn/Concert st/19-10-2026 16:00");
        Event revised = logic.getReminders().getFirst();

        logic.dismissReminder(revised);
        assertEquals(Set.of(revised.getStartTime()), savedEvent().getDismissedReminderStarts());
        logic.execute("editevent evn/Concert st/20-10-2026 15:00");
        assertEquals(1, logic.getReminders().size());
        assertEquals(Set.of(), savedEvent().getDismissedReminderStarts());

        ModelManager reloaded = new ModelManager(storage.readCoordiMate().orElseThrow(), model.getUserPrefs());
        assertEquals(1, newLogic(reloaded).getReminders().size());
    }

    @Test
    public void dismissReminder_savedDismissal_scheduleChangeAndReversionRestoreReminder()
            throws Exception {
        ObservableList<Event> view = logic.getReminders();
        logic.dismissReminder(concert);
        logic.execute("editevent evn/Concert st/19-10-2026 16:00");

        assertEquals(1, view.size());
        logic.dismissReminder(view.getFirst());
        assertEquals(List.of(), view);
        logic.execute("editevent evn/Concert st/20-10-2026 15:00");
        assertEquals(1, view.size());
        assertEquals(Set.of(), savedEvent().getDismissedReminderStarts());
        ModelManager reloaded = new ModelManager(storage.readCoordiMate().orElseThrow(), model.getUserPrefs());
        assertEquals(1, newLogic(reloaded).getReminders().size());
    }

    @Test
    public void execute_sameStart_preservesSavedDismissal() throws Exception {
        logic.dismissReminder(concert);

        logic.execute("editevent evn/Concert st/20-10-2026 15:00");

        assertEquals(List.of(), logic.getReminders());
        assertEquals(Set.of(START), savedEvent().getDismissedReminderStarts());
    }

    @Test
    public void execute_failedStartEdit_preservesSavedDismissalAndFile() throws Exception {
        logic.dismissReminder(concert);
        byte[] before = Files.readAllBytes(storage.getCoordiMateFilePath());
        storage.failSave = true;

        assertThrows(CommandException.class, () -> logic.execute("editevent evn/Concert st/19-10-2026 16:00"));

        assertEquals(List.of(), logic.getReminders());
        assertEquals(Set.of(START), savedEvent().getDismissedReminderStarts());
        assertArrayEquals(before, Files.readAllBytes(storage.getCoordiMateFilePath()));
    }

    @Test
    public void execute_failedStartEdit_preservesTemporarySuppression() throws Exception {
        failDismissal();
        byte[] before = Files.readAllBytes(storage.getCoordiMateFilePath());

        assertThrows(CommandException.class, () -> logic.execute("editevent evn/Concert st/19-10-2026 16:00"));

        assertEquals(List.of(), logic.getReminders());
        assertEquals(START, savedEvent().getStartTime());
        assertArrayEquals(before, Files.readAllBytes(storage.getCoordiMateFilePath()));
        storage.failSave = false;
        logic.execute("editevent evn/Concert st/20-10-2026 15:00");
        assertEquals(List.of(), logic.getReminders());
    }

    @Test
    public void execute_dateOnlyStartReversion_restoresReminder() throws Exception {
        logic.execute("editevent evn/Concert st/20-10-2026");
        logic.dismissReminder(logic.getReminders().getFirst());
        logic.execute("editevent evn/Concert st/20-10-2026 00:00");
        logic.dismissReminder(logic.getReminders().getFirst());

        logic.execute("editevent evn/Concert st/20-10-2026");

        assertEquals(1, logic.getReminders().size());
        assertEquals(Set.of(), savedEvent().getDismissedReminderStarts());
        assertTrue(savedEvent().getStartTime().getDateTime().isEmpty());
    }

    @Test
    public void execute_revertingToPastStart_resetsDismissalWithoutShowingReminder() throws Exception {
        logic.dismissReminder(concert);
        clock.advance(Duration.ofHours(24).plusMinutes(1));
        logic.execute("editevent evn/Concert st/20-10-2026 17:00");
        assertEquals(1, logic.getReminders().size());

        logic.execute("editevent evn/Concert st/20-10-2026 15:00");

        assertEquals(List.of(), logic.getReminders());
        assertFalse(savedEvent().isReminderDismissed());
    }

    @Test
    public void dismissReminder_dateOnly_persistsWithoutAssigningTime() throws Exception {
        logic.execute("editevent evn/Concert st/20-10-2026");
        Event dateOnly = logic.getReminders().getFirst();

        logic.dismissReminder(dateOnly);

        assertEquals(List.of(), logic.getReminders());
        assertEquals(Set.of(new EventTime("20-10-2026")), savedEvent().getDismissedReminderStarts());
        assertTrue(savedEvent().getStartTime().getDateTime().isEmpty());
    }

    @Test
    public void execute_unrelatedCommand_doesNotPersistFailedDismissal() throws Exception {
        failDismissal();
        storage.failSave = false;

        logic.execute("list");

        assertEquals(List.of(), logic.getReminders());
        assertFalse(savedEvent().isReminderDismissed());
    }

    @Test
    public void dismissReminder_null_rejectedWithoutStorageAccess() {
        assertThrows(NullPointerException.class, () -> logic.dismissReminder(null));
        assertEquals(0, storage.reads);
        assertEquals(0, storage.writes);
    }

    private void failDismissal() {
        storage.failSave = true;
        assertThrows(ReminderDismissalException.class, () -> logic.dismissReminder(concert));
    }

    private Logic newLogic(ModelManager source) {
        return new LogicManager(source, new StorageManager(storage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))), clock);
    }

    private Event savedEvent() throws Exception {
        return storage.readCoordiMate().orElseThrow().getEventList().getFirst();
    }

    private static Event event(String name, String time) {
        EventTime start = new EventTime(time);
        return new Event(name, start, start);
    }

    private static class ControlledStorage extends JsonCoordiMateStorage {
        private final IOException saveFailure = new AccessDeniedException("Read-only data file");
        private boolean failSave;
        private int writes;
        private int reads;

        ControlledStorage(Path path) {
            super(path);
        }

        @Override
        public java.util.Optional<ReadOnlyCoordiMate> readCoordiMate() throws DataLoadingException {
            reads++;
            return super.readCoordiMate();
        }

        @Override
        public void saveCoordiMate(ReadOnlyCoordiMate data) throws IOException {
            writes++;
            if (failSave) {
                throw saveFailure;
            }
            super.saveCoordiMate(data);
        }
    }

    private static class MutableClock extends Clock {
        private Instant current;

        MutableClock(Instant current) {
            this.current = current;
        }

        void advance(Duration duration) {
            current = current.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("Asia/Makassar");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(current, zone);
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
