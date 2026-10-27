package coordimate;

import static coordimate.testutil.TypicalPersons.getTypicalCoordiMate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import coordimate.model.CoordiMate;
import coordimate.model.util.SampleDataUtil;
import coordimate.storage.JsonCoordiMateStorage;
import coordimate.ui.Ui;
import javafx.stage.Stage;

public class MainAppTest {

    @TempDir
    public Path testFolder;

    @Test
    public void constructor_defaultPaths_success() {
        assertNotNull(new MainApp());
    }

    @Test
    public void init_noStoredData_usesSampleData() throws Exception {
        Path prefsFile = testFolder.resolve("preferences.json");
        MainApp app = new MainApp(prefsFile, testFolder.resolve("coordimate.json"));

        app.init();

        assertEquals(new CoordiMate(SampleDataUtil.getSampleCoordiMate()), app.model.getCoordiMate());
        assertFalse(app.isDataRecoveryMode);
        app.stop();
        assertTrue(Files.exists(prefsFile));
    }

    @Test
    public void init_storedDataExists_usesStoredData() throws Exception {
        Path dataFile = testFolder.resolve("coordimate.json");
        CoordiMate storedData = getTypicalCoordiMate();
        new JsonCoordiMateStorage(dataFile).saveCoordiMate(storedData);
        MainApp app = new MainApp(testFolder.resolve("preferences.json"), dataFile);

        app.init();

        assertEquals(storedData, app.model.getCoordiMate());
        assertFalse(app.isDataRecoveryMode);
    }

    @Test
    public void init_invalidStoredData_usesEmptyData() throws Exception {
        Path dataFile = testFolder.resolve("coordimate.json");
        Files.writeString(dataFile, "not valid JSON");
        MainApp app = new MainApp(testFolder.resolve("preferences.json"), dataFile);

        app.init();

        assertEquals(new CoordiMate(), app.model.getCoordiMate());
        assertTrue(app.logic.getReminders().isEmpty());
        assertTrue(app.isDataRecoveryMode);
        assertEquals("not valid JSON", Files.readString(dataFile));
    }

    @Test
    public void start_startsUi() {
        MainApp app = new MainApp();
        UiStub uiStub = new UiStub();
        app.ui = uiStub;

        app.start(null);

        assertTrue(uiStub.isStarted);
    }

    @Test
    public void stop_stopsUiBeforeApplicationShutdown() throws Exception {
        MainApp app = new MainApp(testFolder.resolve("prefs.json"), testFolder.resolve("data.json"));
        app.init();
        UiStub uiStub = new UiStub();
        app.ui = uiStub;

        app.stop();

        assertTrue(uiStub.isStopped);
    }

    private static class UiStub implements Ui {
        private boolean isStarted;
        private boolean isStopped;

        @Override
        public void start(Stage primaryStage) {
            isStarted = true;
        }

        @Override
        public void stop() {
            isStopped = true;
        }
    }
}
