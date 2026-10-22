package coordimate.logic;

import static coordimate.testutil.TypicalPersons.AMY;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import coordimate.commons.core.LogsCenter;
import coordimate.commons.exceptions.DataLoadingException;
import coordimate.logic.commands.CommandResult;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.ModelManager;
import coordimate.model.ReadOnlyCoordiMate;
import coordimate.storage.JsonCoordiMateStorage;
import coordimate.storage.JsonUserPrefsStorage;
import coordimate.storage.StorageManager;

public class ContactOperationLoggingTest {
    @TempDir
    public Path temporaryFolder;

    @Test
    public void execute_addContact_logsSuccessfulAdditionWithoutContactDetails() throws Exception {
        ModelManager model = new ModelManager();
        LogicManager logic = newLogic(model, new JsonCoordiMateStorage(temporaryFolder.resolve("add.json")));

        try (CapturingLog capture = new CapturingLog()) {
            logic.execute("add n/Log Test p/82345678 e/log.test@example.com r/Member");

            assertHasMessage(capture, Level.INFO, "Contact addition completed successfully.");
            assertNoContactDetails(capture, "Log Test", "82345678", "log.test@example.com");
        }
    }

    @Test
    public void execute_editContact_logsSuccessfulEditWithoutContactDetails() throws Exception {
        ModelManager model = new ModelManager();
        model.addPerson(AMY);
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(temporaryFolder.resolve("edit.json"));
        storage.saveCoordiMate(model.getCoordiMate());
        LogicManager logic = newLogic(model, storage);

        try (CapturingLog capture = new CapturingLog()) {
            CommandResult result = logic.execute("edit 1 r/President");

            assertHasMessage(capture, Level.INFO, "Contact edit completed successfully.");
            assertSame(model.getCoordiMate().getPersonList().getFirst(), result.getContactToFocus().orElseThrow());
            assertTrue(result.getContactIndexToFocus().isEmpty());
            assertNoContactDetails(capture, AMY.getName().getFullName(), AMY.getPhone().toString(),
                    AMY.getEmail().toString());
        }
    }

    @Test
    public void execute_confirmedDelete_logsSuccessfulDeletionWithoutContactDetails() throws Exception {
        ModelManager model = new ModelManager();
        model.addPerson(AMY);
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(temporaryFolder.resolve("delete.json"));
        storage.saveCoordiMate(model.getCoordiMate());
        LogicManager logic = newLogic(model, storage);

        try (CapturingLog capture = new CapturingLog()) {
            logic.execute("delete 1");
            logic.execute("y");

            assertHasMessage(capture, Level.INFO, "Contact deletion completed successfully.");
            assertNoContactDetails(capture, AMY.getName().getFullName(), AMY.getPhone().toString(),
                    AMY.getEmail().toString());
        }
    }

    @Test
    public void execute_contactDataLoadFailure_logsWarningWithCauseAndNoSuccess() {
        IOException cause = new IOException("Data file unavailable");
        JsonCoordiMateStorage failingStorage = new JsonCoordiMateStorage(temporaryFolder.resolve("load-failure.json")) {
            @Override
            public Optional<ReadOnlyCoordiMate> readCoordiMate() throws DataLoadingException {
                throw new DataLoadingException(cause);
            }
        };
        LogicManager logic = newLogic(new ModelManager(), failingStorage);

        try (CapturingLog capture = new CapturingLog()) {
            assertThrows(CommandException.class, () ->
                    logic.execute("add n/Log Test p/82345678 e/log.test@example.com r/Member"));

            LogRecord warning = assertHasMessage(capture, Level.WARNING,
                    "Failed to load contact data during contact addition.");
            assertTrue(warning.getThrown() instanceof DataLoadingException);
            assertSame(cause, warning.getThrown().getCause());
            assertNoMessage(capture, "Contact addition completed successfully.");
        }
    }

    @Test
    public void execute_contactDataSaveFailure_logsWarningWithCauseAndNoSuccess() {
        IOException failure = new IOException("Disk full");
        JsonCoordiMateStorage failingStorage = new JsonCoordiMateStorage(temporaryFolder.resolve("save-failure.json")) {
            @Override
            public void saveCoordiMate(ReadOnlyCoordiMate coordiMate) throws IOException {
                throw failure;
            }
        };
        LogicManager logic = newLogic(new ModelManager(), failingStorage);

        try (CapturingLog capture = new CapturingLog()) {
            assertThrows(CommandException.class, () ->
                    logic.execute("add n/Log Test p/82345678 e/log.test@example.com r/Member"));

            LogRecord warning = assertHasMessage(capture, Level.WARNING,
                    "Failed to save contact data during contact addition.");
            assertSame(failure, warning.getThrown());
            assertNoMessage(capture, "Contact addition completed successfully.");
        }
    }

    private LogicManager newLogic(ModelManager model, JsonCoordiMateStorage contactStorage) {
        return new LogicManager(model, new StorageManager(contactStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));
    }

    private static LogRecord assertHasMessage(CapturingLog capture, Level level, String expectedMessage) {
        return capture.getRecords().stream()
                .filter(record -> record.getLevel().equals(level) && record.getMessage().equals(expectedMessage))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing " + level + " log: " + expectedMessage));
    }

    private static void assertNoMessage(CapturingLog capture, String message) {
        assertFalse(capture.getRecords().stream().anyMatch(record -> record.getMessage().equals(message)));
    }

    private static void assertNoContactDetails(CapturingLog capture, String... details) {
        List<String> contactOperationMessages = capture.getRecords().stream()
                .map(LogRecord::getMessage)
                .filter(message -> message.startsWith("Contact ") || message.startsWith("Failed to "))
                .toList();
        for (String detail : details) {
            assertFalse(contactOperationMessages.stream().anyMatch(message -> message.contains(detail)),
                    "Contact detail was included in a log message: " + detail);
        }
    }

    private static final class CapturingLog implements AutoCloseable {
        private final Logger logger = LogsCenter.getLogger(LogicManager.class);
        private final boolean originalUseParentHandlers = logger.getUseParentHandlers();
        private final List<LogRecord> records = new ArrayList<>();
        private final Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (isLoggable(record)) {
                    records.add(record);
                }
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        };

        private CapturingLog() {
            handler.setLevel(Level.ALL);
            logger.setUseParentHandlers(false);
            logger.addHandler(handler);
        }

        private List<LogRecord> getRecords() {
            return List.copyOf(records);
        }

        @Override
        public void close() {
            logger.removeHandler(handler);
            logger.setUseParentHandlers(originalUseParentHandlers);
        }
    }
}
