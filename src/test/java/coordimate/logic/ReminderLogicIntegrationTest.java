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
import coordimate.logic.commands.CommandResult;
import coordimate.logic.commands.DismissCommand;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.logic.reminder.ReminderDismissalException;
import coordimate.model.ModelManager;
import coordimate.model.ReadOnlyCoordiMate;
import coordimate.model.UserPrefs;
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
    public void executeDismiss_validIndex_savesOnceAndUpdatesExistingReminderList() throws Exception {
        Event earlier = event("Earlier", "19-10-2026 18:00");
        model.addEvent(earlier);
        ObservableList<Event> view = logic.getReminders();
        assertEquals(List.of(earlier, concert), view);

        CommandResult result = logic.execute("dismiss 2");

        assertEquals("Reminder dismissed: Concert.", result.getFeedbackToUser());
        assertFalse(result.isWarning());
        assertEquals(List.of(earlier), view);
        assertEquals(1, storage.reads);
        assertEquals(1, storage.writes);
        assertTrue(savedEvent().isReminderDismissed());
    }

    @Test
    public void executeDismiss_outOfRangeOrEmptyPanel_rejectedWithoutStorageAccess() throws Exception {
        String message = Messages.withUsage(String.format(DismissCommand.MESSAGE_INVALID_INDEX, 2),
                DismissCommand.MESSAGE_USAGE);
        assertEquals(message, assertThrows(CommandException.class, () -> logic.execute("dismiss 2")).getMessage());
        assertEquals(List.of(concert), logic.getReminders());
        assertEquals(0, storage.reads);
        assertEquals(0, storage.writes);

        model.deleteEvent(concert);
        logic.refreshReminders();
        message = Messages.withUsage(String.format(DismissCommand.MESSAGE_INVALID_INDEX, 1),
                DismissCommand.MESSAGE_USAGE);
        assertEquals(message, assertThrows(CommandException.class, () -> logic.execute("dismiss 1")).getMessage());
        assertEquals(0, storage.reads);
        assertEquals(0, storage.writes);
    }

    @Test
    public void executeDismiss_shownReminderStartedBeforeRefresh_rejectedWithoutDismissingAnother()
            throws Exception {
        Event earlier = event("Earlier", "19-10-2026 18:00");
        model.addEvent(earlier);
        ObservableList<Event> view = logic.getReminders();
        assertEquals(List.of(earlier, concert), view);

        clock.advance(Duration.ofHours(3).plusSeconds(20));
        String message = Messages.withUsage(String.format(DismissCommand.MESSAGE_REMINDER_ENDED, 1),
                DismissCommand.MESSAGE_USAGE);
        assertEquals(message, assertThrows(CommandException.class, () -> logic.execute("dismiss 1")).getMessage());

        assertEquals(List.of(concert), view);
        assertEquals(0, storage.reads);
        assertEquals(0, storage.writes);
        assertFalse(savedEvent().isReminderDismissed());
    }

    @Test
    public void executeDismiss_reminderAddedBeforeRefresh_dismissesShownReminder() throws Exception {
        Event dateOnly = event("Tomorrow", "21-10-2026");
        model.addEvent(dateOnly);
        clock.advance(Duration.ofHours(8).plusMinutes(59));
        ObservableList<Event> view = logic.getReminders();
        assertEquals(List.of(concert), view);

        clock.advance(Duration.ofMinutes(1));
        assertEquals("Reminder dismissed: Concert.", logic.execute("dismiss 1").getFeedbackToUser());

        assertEquals(List.of(dateOnly), view);
        assertTrue(savedEvent().isReminderDismissed());
    }

    @Test
    public void executeDismiss_saveFailure_returnsWarningAndHidesReminderForSession() throws Exception {
        storage.failSave = true;
        byte[] before = Files.readAllBytes(storage.getCoordiMateFilePath());

        CommandResult result = logic.execute("dismiss 1");

        assertTrue(result.isWarning());
        assertEquals(ReminderDismissalException.MESSAGE, result.getFeedbackToUser());
        assertEquals(List.of(), logic.getReminders());
        assertEquals(1, storage.reads);
        assertEquals(1, storage.writes);
        assertFalse(model.getCoordiMate().getEventList().getFirst().isReminderDismissed());
        assertArrayEquals(before, Files.readAllBytes(storage.getCoordiMateFilePath()));

        storage.failSave = false;
        logic.execute("list");
        assertEquals(List.of(), logic.getReminders());
        assertFalse(savedEvent().isReminderDismissed());
        restartFromSavedData();
        assertEquals(List.of(concert), logic.getReminders());
    }

    @Test
    public void executeDismiss_tiedStarts_usesPanelOrderAndRenumbersAfterDismissal() throws Exception {
        Event zulu = event("Zulu", "20-10-2026");
        Event alpha = event("alpha", "20-10-2026");
        Event earlier = event("Tonight", "19-10-2026 18:00");
        model.addEvent(zulu);
        model.addEvent(alpha);
        model.addEvent(earlier);
        ObservableList<Event> view = logic.getReminders();
        assertEquals(List.of(earlier, alpha, zulu, concert), view);

        assertEquals("Reminder dismissed: alpha.", logic.execute("dismiss 2").getFeedbackToUser());
        assertEquals(List.of(earlier, zulu, concert), view);
        assertEquals("Reminder dismissed: Zulu.", logic.execute("dismiss 2").getFeedbackToUser());
        assertEquals(List.of(earlier, concert), view);
        assertEquals(List.of(concert, zulu.withReminderDismissed(), alpha.withReminderDismissed(), earlier),
                model.getCoordiMate().getEventList());
    }

    @Test
    public void executeDismiss_editedStart_usesUpdatedPanelOrder() throws Exception {
        Event earlier = event("Tonight", "19-10-2026 18:00");
        model.addEvent(earlier);
        ObservableList<Event> view = logic.getReminders();
        assertEquals(List.of(earlier, concert), view);

        logic.execute("editevent evn/Concert st/19-10-2026 17:00");
        Event revised = model.getCoordiMate().getEventList().getFirst();
        assertEquals(List.of(revised, earlier), view);
        assertEquals("Reminder dismissed: Concert.", logic.execute("dismiss 1").getFeedbackToUser());
        assertEquals(List.of(earlier), view);
        assertEquals(revised.withReminderDismissed(), savedEvent());
    }

    @Test
    public void executeDismiss_restart_preservesEventDataAndCurrentContactView() throws Exception {
        logic.execute("members evn/Concert");
        var predicate = model.getFilteredPersonPredicate();
        ObservableList<coordimate.model.person.Person> contacts = logic.getFilteredPersonList();
        assertEquals(List.of(ALICE), contacts);

        CommandResult result = logic.execute("dismiss 1");

        assertEquals(CommandResult.View.NONE, result.getViewToShow());
        assertSame(predicate, model.getFilteredPersonPredicate());
        assertSame(contacts, logic.getFilteredPersonList());
        assertEquals(List.of(ALICE), contacts);
        assertEquals(concert.withReminderDismissed(), savedEvent());
        assertEquals(List.of(ALICE, BENSON), model.getCoordiMate().getPersonList());
        byte[] saved = Files.readAllBytes(storage.getCoordiMateFilePath());

        restartFromSavedData();

        assertEquals(List.of(), logic.getReminders());
        assertEquals(List.of(concert.withReminderDismissed()), logic.getEventList());
        assertEquals(List.of(ALICE, BENSON), logic.getPersonList());
        assertArrayEquals(saved, Files.readAllBytes(storage.getCoordiMateFilePath()));
    }

    @Test
    public void executeDismiss_dateOnlyStart_persistsWithoutAddingTime() throws Exception {
        logic.execute("editevent evn/Concert st/20-10-2026");
        Event dateOnly = logic.getReminders().getFirst();

        logic.execute("dismiss 1");
        restartFromSavedData();

        assertEquals(List.of(), logic.getReminders());
        assertEquals(dateOnly.withReminderDismissed(), savedEvent());
        assertTrue(savedEvent().getStartTime().getDateTime().isEmpty());
    }

    @Test
    public void executeDismiss_readFailure_warnsWithoutOverwritingInvalidFile() throws Exception {
        byte[] valid = Files.readAllBytes(storage.getCoordiMateFilePath());
        Files.writeString(storage.getCoordiMateFilePath(), "{invalid json");
        byte[] invalid = Files.readAllBytes(storage.getCoordiMateFilePath());

        CommandResult result = logic.execute("dismiss 1");

        assertTrue(result.isWarning());
        assertEquals(ReminderDismissalException.MESSAGE, result.getFeedbackToUser());
        assertEquals(List.of(), logic.getReminders());
        assertEquals(List.of(concert), logic.getEventList());
        assertEquals(1, storage.reads);
        assertEquals(0, storage.writes);
        assertArrayEquals(invalid, Files.readAllBytes(storage.getCoordiMateFilePath()));

        Files.write(storage.getCoordiMateFilePath(), valid);
        restartFromSavedData();
        assertEquals(List.of(concert), logic.getReminders());
    }

    @Test
    public void executeDismiss_invalidSyntax_preservesModelAndFileWithoutStorageAccess() throws Exception {
        byte[] before = Files.readAllBytes(storage.getCoordiMateFilePath());
        for (String command : List.of("dismiss", "dismiss 0", "dismiss -1", "dismiss 1.5", "dismiss abc",
                "dismiss 1 2", "dismiss 2147483648")) {
            assertThrows(ParseException.class, () -> logic.execute(command));
            assertEquals(List.of(concert), logic.getReminders());
            assertEquals(List.of(concert), logic.getEventList());
            assertArrayEquals(before, Files.readAllBytes(storage.getCoordiMateFilePath()));
        }
        assertEquals(0, storage.reads);
        assertEquals(0, storage.writes);
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
        byte[] before = Files.readAllBytes(storage.getCoordiMateFilePath());

        restartFromSavedData();
        ObservableList<Event> view = logic.getReminders();
        assertEquals(List.of(), view);
        clock.advance(Duration.ofHours(1));
        logic.refreshReminders();

        assertEquals(List.of(), view);
        assertTrue(model.getCoordiMate().getEventList().getFirst().isReminderDismissed());
        assertArrayEquals(before, Files.readAllBytes(storage.getCoordiMateFilePath()));
    }

    @Test
    public void execute_rescheduleBeyondWindowAndBack_keepsEligibilityAfterRestart() throws Exception {
        verifyRescheduleOutsideWindowAndBack("20-10-2026 15:01");
    }

    @Test
    public void execute_rescheduleToPastAndBack_keepsEligibilityAfterRestart() throws Exception {
        verifyRescheduleOutsideWindowAndBack("19-10-2026 14:59");
    }

    private void verifyRescheduleOutsideWindowAndBack(String outsideStart) throws Exception {
        ObservableList<Event> view = logic.getReminders();
        assertEquals(List.of(concert), view);

        logic.execute("editevent evn/Concert st/" + outsideStart);

        assertEquals(List.of(), view);
        assertFalse(savedEvent().isReminderDismissed());
        restartFromSavedData();
        assertEquals(List.of(), logic.getReminders());
        logic.execute("editevent evn/Concert st/20-10-2026 15:00");
        assertEquals(List.of(concert), logic.getReminders());
        restartFromSavedData();
        assertEquals(List.of(concert), logic.getReminders());
    }

    @Test
    public void execute_timedToDateOnlyAndBack_keepsReminderAndDismissalAfterRestart() throws Exception {
        logic.dismissReminder(concert);
        logic.execute("editevent evn/Concert st/20-10-2026");

        restartFromSavedData();
        Event dateOnly = logic.getReminders().getFirst();
        assertEquals("20-10-2026", dateOnly.getStartTime().toString());
        logic.dismissReminder(dateOnly);
        restartFromSavedData();
        assertEquals(List.of(), logic.getReminders());
        assertEquals(Set.of(new EventTime("20-10-2026")), savedEvent().getDismissedReminderStarts());

        logic.execute("editevent evn/Concert st/20-10-2026 15:00");
        assertEquals(List.of(concert), logic.getReminders());
        restartFromSavedData();
        assertEquals(List.of(concert), logic.getReminders());
    }

    @Test
    public void execute_endOnlyEdit_updatesActiveReminderAfterRestart() throws Exception {
        verifyEndOnlyEditAfterRestart(false);
    }

    @Test
    public void execute_endOnlyEdit_preservesDismissalAfterRestart() throws Exception {
        verifyEndOnlyEditAfterRestart(true);
    }

    private void verifyEndOnlyEditAfterRestart(boolean dismissed) throws Exception {
        if (dismissed) {
            logic.dismissReminder(concert);
        }
        ObservableList<Event> view = logic.getReminders();

        logic.execute("editevent evn/Concert et/22-10-2026 18:00");

        assertEquals(new EventTime("22-10-2026 18:00"), savedEvent().getEndTime());
        assertEquals(START, savedEvent().getStartTime());
        assertEquals(dismissed, savedEvent().isReminderDismissed());
        assertEquals(dismissed ? List.of() : List.of(savedEvent()), view);
        List<Event> expected = List.copyOf(view);
        restartFromSavedData();
        assertEquals(expected, logic.getReminders());
    }

    @Test
    public void execute_deleteActiveEventAndRecreate_restoresReminderAcrossRestarts() throws Exception {
        verifyDeletionAndRecreationAcrossRestarts(false);
    }

    @Test
    public void execute_deleteDismissedEventAndRecreate_restoresReminderAcrossRestarts() throws Exception {
        verifyDeletionAndRecreationAcrossRestarts(true);
    }

    private void verifyDeletionAndRecreationAcrossRestarts(boolean dismissed) throws Exception {
        if (dismissed) {
            logic.dismissReminder(concert);
        }
        ObservableList<Event> view = logic.getReminders();

        logic.execute("deleteevent evn/Concert");

        assertEquals(List.of(), view);
        restartFromSavedData();
        assertEquals(List.of(), model.getCoordiMate().getEventList());
        assertEquals(List.of(), logic.getReminders());
        logic.execute("addevent evn/Concert st/20-10-2026 15:00 et/21-10-2026 18:00");
        Event recreated = new Event("Concert", START, new EventTime("21-10-2026 18:00"));
        assertEquals(List.of(recreated), logic.getReminders());
        restartFromSavedData();
        assertEquals(List.of(recreated), logic.getReminders());
    }

    @Test
    public void execute_clear_removesActiveAndDismissedRemindersAcrossRestart() throws Exception {
        logic.dismissReminder(concert);
        logic.execute("addevent evn/Other st/19-10-2026 18:00 et/19-10-2026 19:00");
        ObservableList<Event> view = logic.getReminders();
        assertEquals(1, view.size());

        logic.execute("clear");

        assertEquals(List.of(), view);
        restartFromSavedData();
        assertEquals(List.of(), logic.getReminders());
        assertEquals(List.of(), model.getCoordiMate().getEventList());
        assertEquals(List.of(), model.getCoordiMate().getPersonList());
        logic.execute("addevent evn/Concert st/20-10-2026 15:00 et/21-10-2026 18:00");
        Event recreated = new Event("Concert", START, new EventTime("21-10-2026 18:00"));
        assertEquals(List.of(recreated), logic.getReminders());
        restartFromSavedData();
        assertEquals(List.of(recreated), logic.getReminders());
    }

    @Test
    public void execute_rejectedEdits_preserveActiveReminderAcrossRestart() throws Exception {
        verifyRejectedEditsAcrossRestart(false);
    }

    @Test
    public void execute_rejectedEdits_preserveDismissalAcrossRestart() throws Exception {
        verifyRejectedEditsAcrossRestart(true);
    }

    private void verifyRejectedEditsAcrossRestart(boolean dismissed) throws Exception {
        if (dismissed) {
            logic.dismissReminder(concert);
        }
        logic.execute("addevent evn/Other st/21-10-2026 16:00 et/21-10-2026 18:00");
        ObservableList<Event> view = logic.getReminders();
        List<Event> expectedReminders = List.copyOf(view);
        List<Event> expectedEvents = List.copyOf(model.getCoordiMate().getEventList());
        byte[] before = Files.readAllBytes(storage.getCoordiMateFilePath());
        AtomicInteger changes = new AtomicInteger();
        view.addListener((ListChangeListener<Event>) change -> changes.incrementAndGet());

        assertThrows(ParseException.class, () -> logic.execute("editevent evn/Concert st/31-02-2026"));
        for (String command : List.of("editevent evn/Concert st/22-10-2026",
                "editevent evn/Concert nevn/Other st/19-10-2026 16:00",
                "editevent evn/Unknown st/19-10-2026 16:00")) {
            assertThrows(CommandException.class, () -> logic.execute(command));
            assertEquals(expectedReminders, view);
            assertEquals(expectedEvents, model.getCoordiMate().getEventList());
            assertArrayEquals(before, Files.readAllBytes(storage.getCoordiMateFilePath()));
        }
        assertEquals(0, changes.get());
        restartFromSavedData();
        assertEquals(expectedReminders, logic.getReminders());
        assertEquals(expectedEvents, model.getCoordiMate().getEventList());
    }

    @Test
    public void execute_failedEdit_preservesActiveReminderAcrossRestart() throws Exception {
        verifyFailedEditAcrossRestart(false);
    }

    @Test
    public void execute_failedEdit_preservesDismissalAcrossRestart() throws Exception {
        verifyFailedEditAcrossRestart(true);
    }

    private void verifyFailedEditAcrossRestart(boolean dismissed) throws Exception {
        if (dismissed) {
            logic.dismissReminder(concert);
        }
        ObservableList<Event> view = logic.getReminders();
        List<Event> expectedReminders = List.copyOf(view);
        List<Event> expectedEvents = List.copyOf(model.getCoordiMate().getEventList());
        byte[] before = Files.readAllBytes(storage.getCoordiMateFilePath());
        storage.failSave = true;

        CommandException error = assertThrows(CommandException.class, () ->
                logic.execute("editevent evn/Concert nevn/Final Concert st/19-10-2026 16:00"));

        assertSame(storage.saveFailure, error.getCause());
        assertEquals(expectedReminders, view);
        assertEquals(expectedEvents, model.getCoordiMate().getEventList());
        assertArrayEquals(before, Files.readAllBytes(storage.getCoordiMateFilePath()));
        restartFromSavedData();
        assertEquals(expectedReminders, logic.getReminders());
        assertEquals(expectedEvents, model.getCoordiMate().getEventList());
    }

    @Test
    public void execute_unreadableData_preservesActiveReminderAndInvalidFile() throws Exception {
        verifyUnreadableDataPreservesReminderState(false);
    }

    @Test
    public void execute_unreadableData_preservesDismissalAndInvalidFile() throws Exception {
        verifyUnreadableDataPreservesReminderState(true);
    }

    private void verifyUnreadableDataPreservesReminderState(boolean dismissed) throws Exception {
        if (dismissed) {
            logic.dismissReminder(concert);
        }
        ObservableList<Event> view = logic.getReminders();
        List<Event> expectedReminders = List.copyOf(view);
        List<Event> expectedEvents = List.copyOf(model.getCoordiMate().getEventList());
        Files.writeString(storage.getCoordiMateFilePath(), "invalid JSON");

        CommandException error = assertThrows(CommandException.class, () ->
                logic.execute("editevent evn/Concert st/19-10-2026 16:00"));

        assertTrue(error.getCause() instanceof DataLoadingException);
        assertEquals(expectedReminders, view);
        assertEquals(expectedEvents, model.getCoordiMate().getEventList());
        assertEquals("invalid JSON", Files.readString(storage.getCoordiMateFilePath()));
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
        logic.dismissReminder(concert);
        restartFromSavedData();
        assertEquals(List.of(), logic.getReminders());
        logic.execute("editevent evn/Concert st/19-10-2026 16:00");

        assertEquals(1, logic.getReminders().size());
        logic.dismissReminder(logic.getReminders().getFirst());
        restartFromSavedData();
        ObservableList<Event> view = logic.getReminders();
        assertEquals(List.of(), view);
        logic.execute("editevent evn/Concert st/20-10-2026 15:00");
        assertEquals(1, view.size());
        assertEquals(Set.of(), savedEvent().getDismissedReminderStarts());
        restartFromSavedData();
        assertEquals(List.of(concert), logic.getReminders());
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

    private void restartFromSavedData() throws Exception {
        JsonCoordiMateStorage reloadedStorage = new JsonCoordiMateStorage(storage.getCoordiMateFilePath());
        JsonUserPrefsStorage reloadedPrefs = new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"));
        model = new ModelManager(reloadedStorage.readCoordiMate().orElseThrow(),
                reloadedPrefs.readUserPrefs().orElseGet(UserPrefs::new));
        logic = new LogicManager(model, new StorageManager(reloadedStorage, reloadedPrefs), clock);
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
