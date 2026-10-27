package coordimate;

import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BENSON;
import static coordimate.testutil.TypicalPersons.getTypicalCoordiMate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import coordimate.commons.util.JsonUtil;
import coordimate.model.CoordiMate;
import coordimate.model.event.AttendanceStatus;
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
        assertEquals("not valid JSON", Files.readString(dataFile));
    }

    @Test
    public void init_leaveAssignments_loadsAndSavesCleanedData() throws Exception {
        Path dataFile = testFolder.resolve("coordimate.json");
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(dataFile);
        CoordiMate expected = getTypicalCoordiMate();
        expected.setPerson(ALICE, new PersonBuilder(ALICE).withLeaveDate("08-08-2026").build());
        Event concert = new Event("Concert", new EventTime("08-08-2026"), new EventTime("08-08-2026 18:00"),
                List.of(ALICE.getName(), BENSON.getName()),
                Map.of(ALICE.getName(), AttendanceStatus.PRESENT, BENSON.getName(), AttendanceStatus.ABSENT));
        expected.addEvent(concert);
        storage.saveCoordiMate(expected);
        expected.setEvent(concert, concert.withMembersAndAttendance(List.of(BENSON.getName()),
                Map.of(BENSON.getName(), AttendanceStatus.ABSENT)));
        MainApp app = new MainApp(testFolder.resolve("preferences.json"), dataFile);

        app.init();

        assertFalse(app.isDataRecoveryMode);
        assertEquals(expected, app.model.getCoordiMate());
        assertEquals(expected, storage.readCoordiMate().orElseThrow());
        String cleaned = Files.readString(dataFile);
        app.init();
        assertEquals(cleaned, Files.readString(dataFile));
    }

    @Test
    public void init_leaveAndInvalidData_preservesFileAndEntersRecoveryMode() throws Exception {
        Path dataFile = testFolder.resolve("coordimate.json");
        CoordiMate data = getTypicalCoordiMate();
        data.setPerson(ALICE, new PersonBuilder(ALICE).withLeaveDate("08-08-2026").build());
        data.addEvent(new Event("Concert", new EventTime("08-08-2026"), new EventTime("08-08-2026"),
                List.of(ALICE.getName())));
        new JsonCoordiMateStorage(dataFile).saveCoordiMate(data);
        ObjectNode json = JsonUtil.fromJsonString(Files.readString(dataFile), ObjectNode.class);
        json.withArray("events").add(JsonUtil.fromJsonString("""
                {"name":"Invalid","startTime":"31-02-2026","endTime":"01-03-2026"}
                """, JsonNode.class));
        String original = JsonUtil.toJsonString(json);
        Files.writeString(dataFile, original);
        MainApp app = new MainApp(testFolder.resolve("preferences.json"), dataFile);

        app.init();

        assertTrue(app.isDataRecoveryMode);
        assertEquals(new CoordiMate(), app.model.getCoordiMate());
        assertTrue(app.logic.getReminders().isEmpty());
        assertEquals(original, Files.readString(dataFile));
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
