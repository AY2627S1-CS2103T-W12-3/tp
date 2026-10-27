package coordimate;

import static coordimate.testutil.TypicalPersons.getTypicalCoordiMate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import coordimate.commons.exceptions.DataLoadingException;
import coordimate.commons.util.JsonUtil;
import coordimate.model.CoordiMate;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.util.SampleDataUtil;
import coordimate.storage.JsonCoordiMateStorage;
import coordimate.testutil.PersonBuilder;
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
    public void init_yearZeroEvents_deletesInvalidEventsAndLoadsRemainingData() throws Exception {
        Path dataFile = testFolder.resolve("coordimate.json");
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(dataFile);
        CoordiMate expected = getTypicalCoordiMate();
        expected.addEvent(new Event("Keep", new EventTime("01-01-0001"), new EventTime("31-12-9999")));
        storage.saveCoordiMate(expected);
        ObjectNode json = JsonUtil.fromJsonString(Files.readString(dataFile), ObjectNode.class);
        json.withArray("events").add(JsonUtil.fromJsonString("""
                {"name":"Year Zero","startTime":"01-01-0000","endTime":"02-01-0000"}
                """, JsonNode.class));
        Files.writeString(dataFile, JsonUtil.toJsonString(json));
        MainApp app = new MainApp(testFolder.resolve("preferences.json"), dataFile);

        app.init();

        assertEquals(expected, app.model.getCoordiMate());
        assertEquals(expected, storage.readCoordiMate().orElseThrow());
        String cleaned = Files.readString(dataFile);
        app.init();
        assertEquals(cleaned, Files.readString(dataFile));
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
        assertEquals("", app.dataRecoveryDetail);
        assertEquals("not valid JSON", Files.readString(dataFile));
    }

    @Test
    public void init_numericArchivedStatus_entersRecoveryModeAndLeavesFileUnchanged() throws Exception {
        Path dataFile = testFolder.resolve("coordimate.json");
        CoordiMate savedData = new CoordiMate();
        savedData.addPerson(new PersonBuilder().withName("Alex Yeoh").build());
        new JsonCoordiMateStorage(dataFile).saveCoordiMate(savedData);
        String validJson = Files.readString(dataFile);
        String invalidJson = validJson.replaceFirst("\"archived\" : false", "\"archived\" : 17");
        assertNotEquals(validJson, invalidJson);
        Files.writeString(dataFile, invalidJson);
        DataLoadingException loadError = assertThrows(DataLoadingException.class, () ->
                new JsonCoordiMateStorage(dataFile).readCoordiMate());
        assertEquals("Contact \"Alex Yeoh\": archived must be true or false.", loadError.getCause().getMessage());
        MainApp app = new MainApp(testFolder.resolve("preferences.json"), dataFile);

        app.init();

        assertTrue(app.isDataRecoveryMode);
        assertEquals(loadError.getCause().getMessage(), app.dataRecoveryDetail);
        assertEquals(new CoordiMate(), app.model.getCoordiMate());
        assertEquals(invalidJson, Files.readString(dataFile));
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
