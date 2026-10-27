package coordimate.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import coordimate.commons.exceptions.DataLoadingException;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.logic.reminder.ReminderDismissalException;
import coordimate.model.CoordiMate;
import coordimate.model.ModelManager;
import coordimate.model.ReadOnlyCoordiMate;
import coordimate.model.UserPrefs;
import coordimate.storage.DataConflictException;
import coordimate.storage.JsonCoordiMateStorage;
import coordimate.storage.JsonUserPrefsStorage;
import coordimate.storage.StorageManager;

public class ConcurrentDataSaveTest {
    private static final String FIRST = "add n/First Writer p/80000001 e/first@example.com r/Member";
    private static final String SECOND = "add n/Second Writer p/80000002 e/second@example.com r/Member";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-19T07:00:00Z"), ZoneOffset.ofHours(8));

    @TempDir
    public Path temporaryFolder;

    @Test
    public void secondWindowListsAndAttemptsSave_firstWindowContactIsPreserved() throws Exception {
        Path path = temporaryFolder.resolve("data.json");
        new JsonCoordiMateStorage(path).saveCoordiMate(new CoordiMate());
        AppInstance first = open(path);
        AppInstance second = open(path);

        first.logic.execute(FIRST);
        assertEquals("Listed all contacts.", second.logic.execute("list").getFeedbackToUser());
        assertEquals(0, second.model.getCoordiMate().getPersonList().size());

        CommandException conflict = assertThrows(CommandException.class, () -> second.logic.execute(SECOND));
        assertEquals(Messages.withUsage(DataConflictException.MESSAGE,
                coordimate.logic.commands.AddCommand.MESSAGE_USAGE), conflict.getMessage());
        assertEquals(0, second.model.getCoordiMate().getPersonList().size());
        assertEquals(1, savedData(path).getPersonList().size());
        assertEquals("First Writer", savedData(path).getPersonList().getFirst().getName().getFullName());

        AppInstance reopened = open(path);
        reopened.logic.execute(SECOND);
        assertEquals(2, savedData(path).getPersonList().size());
    }

    @Test
    public void staleClearAndConfirmedDeletion_doNotOverwriteNewerData() throws Exception {
        Path path = temporaryFolder.resolve("data.json");
        new JsonCoordiMateStorage(path).saveCoordiMate(new CoordiMate());
        AppInstance first = open(path);
        AppInstance second = open(path);

        first.logic.execute(FIRST);
        assertEquals(DataConflictException.MESSAGE,
                assertThrows(CommandException.class, () -> second.logic.execute("clear")).getMessage()
                        .split("\\n", 2)[0]);
        assertEquals(1, savedData(path).getPersonList().size());

        AppInstance third = open(path);
        third.logic.execute("delete 1");
        first.logic.execute(SECOND);
        assertTrue(third.logic.isAwaitingDeleteConfirmation());
        assertTrue(assertThrows(CommandException.class, () -> third.logic.execute("y"))
                .getMessage().startsWith(DataConflictException.MESSAGE));
        assertEquals(2, savedData(path).getPersonList().size());
    }

    @Test
    public void missingFileCreatedByAnotherWindow_staleSaveIsRejected() throws Exception {
        Path path = temporaryFolder.resolve("data.json");
        AppInstance first = open(path);
        AppInstance second = open(path);

        first.logic.execute(FIRST);
        assertTrue(assertThrows(CommandException.class, () -> second.logic.execute(SECOND))
                .getMessage().startsWith(DataConflictException.MESSAGE));
        assertEquals(1, savedData(path).getPersonList().size());
    }

    @Test
    public void removedOrInvalidFile_viewCommandsPreserveFileAndChangesAreRejected() throws Exception {
        Path path = temporaryFolder.resolve("data.json");
        new JsonCoordiMateStorage(path).saveCoordiMate(new CoordiMate());
        AppInstance window = open(path);

        Files.writeString(path, "invalid JSON");
        window.logic.execute("list");
        assertEquals("invalid JSON", Files.readString(path));
        assertThrows(CommandException.class, () -> window.logic.execute(FIRST));
        assertEquals("invalid JSON", Files.readString(path));

        Files.delete(path);
        assertTrue(assertThrows(CommandException.class, () -> window.logic.execute(FIRST))
                .getMessage().startsWith(DataConflictException.MESSAGE));
        assertFalse(Files.exists(path));
    }

    @Test
    public void fileRepairedAfterInvalidStartup_staleWindowCannotReplaceIt() throws Exception {
        Path path = temporaryFolder.resolve("data.json");
        Files.writeString(path, "invalid JSON");
        StorageManager storage = new StorageManager(new JsonCoordiMateStorage(path),
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json")));
        assertThrows(DataLoadingException.class, storage::readCoordiMate);
        Logic window = new LogicManager(new ModelManager(), storage, CLOCK);

        new JsonCoordiMateStorage(path).saveCoordiMate(new CoordiMate());
        assertTrue(assertThrows(CommandException.class, () -> window.execute(FIRST))
                .getMessage().startsWith(DataConflictException.MESSAGE));
        assertEquals(0, savedData(path).getPersonList().size());
    }

    @Test
    public void staleReminderDismissal_doesNotOverwriteNewerData() throws Exception {
        Path path = temporaryFolder.resolve("data.json");
        new JsonCoordiMateStorage(path).saveCoordiMate(new CoordiMate());
        AppInstance first = open(path);
        first.logic.execute("addevent evn/Concert st/20-10-2026 15:00 et/20-10-2026 18:00");
        AppInstance second = open(path);

        first.logic.execute(FIRST);
        ReminderDismissalException conflict = assertThrows(ReminderDismissalException.class, () ->
                second.logic.dismissReminder(second.logic.getReminders().getFirst()));
        assertTrue(conflict.getMessage().contains("Reopen this window"));
        assertFalse(savedData(path).getEventList().getFirst().isReminderDismissed());
        assertEquals(1, savedData(path).getPersonList().size());
        assertTrue(second.logic.getReminders().isEmpty());
    }

    private AppInstance open(Path path) throws Exception {
        StorageManager storage = new StorageManager(new JsonCoordiMateStorage(path),
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json")));
        ReadOnlyCoordiMate data = storage.readCoordiMate().orElseGet(CoordiMate::new);
        ModelManager model = new ModelManager(data, new UserPrefs());
        return new AppInstance(new LogicManager(model, storage, CLOCK), model);
    }

    private ReadOnlyCoordiMate savedData(Path path) throws Exception {
        return new JsonCoordiMateStorage(path).readCoordiMate().orElseThrow();
    }

    private record AppInstance(Logic logic, ModelManager model) {
    }
}
