package coordimate.commons.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class LogsCenterTest {

    @TempDir
    public Path testFolder;

    @Test
    public void addFileHandler_writableFolder_addsFileHandler() {
        Logger target = createTestLogger();
        LogsCenter.addFileHandler(target, testFolder.resolve("coordimate.log").toString());

        Handler[] handlers = target.getHandlers();
        assertEquals(1, handlers.length);
        assertTrue(handlers[0] instanceof FileHandler);
        handlers[0].close();
    }

    @Test
    public void addFileHandler_logFileCannotBeCreated_logsWarningWithoutThrowing() {
        Logger target = createTestLogger();
        List<String> messages = new ArrayList<>();
        target.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                messages.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });

        // The log file cannot be created because its parent folder does not exist
        LogsCenter.addFileHandler(target, testFolder.resolve("missing").resolve("coordimate.log").toString());

        assertEquals(1, target.getHandlers().length);
        assertEquals(List.of("Error adding file handler for logger."), messages);
    }

    private static Logger createTestLogger() {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        return logger;
    }
}
