package coordimate;

import static coordimate.testutil.TypicalPersons.getTypicalCoordiMate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import coordimate.commons.util.JsonUtil;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.CoordiMate;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.util.SampleDataUtil;
import coordimate.storage.EventNameCleanupResult;
import coordimate.storage.JsonCoordiMateStorage;
import coordimate.testutil.UnicodeWhitespace;
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
    public void init_mixedLegacyEvents_appliesBothCleanupsBeforeLoading() throws Exception {
        Path dataFile = testFolder.resolve("coordimate.json");
        Files.writeString(dataFile, """
                {"persons":[],"events":[
                  {"name":"Year Zero","startTime":"01-01-0000","endTime":"01-01-2027"},
                  {"name":"Bad\u2003c/review","startTime":"01-01-2027","endTime":"01-01-2027"},
                  {"name":"Planning Meeting","startTime":"01-01-2027","endTime":"01-01-2027"},
                  {"name":"Planning\u00a0Meeting","startTime":"02-01-2027","endTime":"02-01-2027"}]}
                """);
        MainApp app = new MainApp(testFolder.resolve("preferences.json"), dataFile);

        app.init();

        assertEquals(java.util.List.of("Planning Meeting", "Planning Meeting (2)"),
                app.model.getCoordiMate().getEventList().stream().map(Event::getName).toList());
        app.logic.execute("editevent evn/Planning Meeting (2) nevn/Second Meeting");
        assertEquals(app.model.getCoordiMate(), new JsonCoordiMateStorage(dataFile).readCoordiMate().orElseThrow());
    }

    @Test
    public void init_embeddedParametersInEventNames_deletesAffectedEventsAndLoadsRemainingData() throws Exception {
        Path dataFile = testFolder.resolve("coordimate.json");
        CoordiMate expected = getTypicalCoordiMate();
        expected.addEvent(new Event("Keep 👩\u200d💻", new EventTime("01-01-2027"),
                new EventTime("01-01-2027")));
        for (String character : java.util.List.of("\u180e", "\u200b", "\u2060", "\ufeff")) {
            expected.addEvent(new Event("Keep" + character + "c/review", new EventTime("01-01-2027"),
                    new EventTime("01-01-2027")));
        }
        new JsonCoordiMateStorage(dataFile).saveCoordiMate(expected);
        ObjectNode json = JsonUtil.fromJsonString(Files.readString(dataFile), ObjectNode.class);
        for (String spacing : UnicodeWhitespace.ALL) {
            json.withArray("events").addObject().put("name", "Planning" + spacing + "c/review")
                    .put("startTime", "01-01-2027").put("endTime", "01-01-2027");
        }
        Files.writeString(dataFile, JsonUtil.toJsonString(json));
        MainApp app = new MainApp(testFolder.resolve("preferences.json"), dataFile);

        app.init();

        assertEquals(expected, app.model.getCoordiMate());
        assertEquals(expected, new JsonCoordiMateStorage(dataFile).readCoordiMate().orElseThrow());
        String cleaned = Files.readString(dataFile);
        app.init();
        assertEquals(cleaned, Files.readString(dataFile));
        app.logic.execute("addevent evn/Usable After Cleanup st/02-01-2027 et/02-01-2027");
        assertTrue(new JsonCoordiMateStorage(dataFile).readCoordiMate().orElseThrow()
                .findEvent("Usable After Cleanup").isPresent());
    }

    @Test
    public void init_legacyEventNamesCollide_preservesBothEventsAndAllowsCommands() throws Exception {
        Path dataFile = testFolder.resolve("coordimate.json");
        Files.writeString(dataFile, """
                {"persons":[],"events":[
                  {"name":"Planning Meeting","startTime":"01-01-2027","endTime":"01-01-2027"},
                  {"name":"Planning\u00a0Meeting","startTime":"02-01-2027","endTime":"02-01-2027"}]}
                """);
        MainApp app = new MainApp(testFolder.resolve("preferences.json"), dataFile);

        app.init();

        assertEquals(java.util.List.of("Planning Meeting", "Planning Meeting (2)"),
                app.model.getCoordiMate().getEventList().stream().map(Event::getName).toList());
        app.logic.execute("editevent evn/Planning Meeting (2) nevn/Second Meeting");
        assertEquals(java.util.List.of("Planning Meeting", "Second Meeting"),
                new JsonCoordiMateStorage(dataFile).readCoordiMate().orElseThrow()
                        .getEventList().stream().map(Event::getName).toList());
        app.init();
        assertEquals(2, app.model.getCoordiMate().getEventList().size());
    }

    @Test
    public void formatEventNameCleanup_renamesAndDeletions_explainsAllChanges() {
        EventNameCleanupResult.Rename rename = new EventNameCleanupResult.Rename(
                "Planning\u00a0Meeting", "Planning Meeting (2)");
        String renamedOnly = MainApp.formatEventNameCleanup(new EventNameCleanupResult(
                java.util.List.of(), java.util.List.of(rename)));
        assertTrue(renamedOnly.startsWith("Renamed 1 saved event"));
        assertTrue(renamedOnly.contains("\"Planning\\u00A0Meeting\" -> \"Planning Meeting (2)\""));
        String combined = MainApp.formatEventNameCleanup(new EventNameCleanupResult(
                java.util.List.of("Bad\u2003c/review"), java.util.List.of(rename,
                        new EventNameCleanupResult.Rename("Planning\u202fMeeting", "Planning Meeting (3)"))));
        assertTrue(combined.contains("Deleted 1 saved event"));
        assertTrue(combined.contains("Renamed 2 saved events"));
        assertTrue(combined.contains("\"Planning\\u202FMeeting\" -> \"Planning Meeting (3)\""));
        assertTrue(combined.contains("All event details were preserved"));
        assertEquals(MainApp.formatDeletedEvents(java.util.List.of("Bad c/review")),
                MainApp.formatEventNameCleanup(new EventNameCleanupResult(
                        java.util.List.of("Bad c/review"), java.util.List.of())));
        String yearZeroOnly = MainApp.formatEventNameCleanup(new EventNameCleanupResult(
                java.util.List.of(), java.util.List.of(), java.util.List.of("Year\u2003Zero")));
        assertTrue(yearZeroOnly.contains("year 0000"));
        assertTrue(yearZeroOnly.contains("\"Year\\u2003Zero\""));
        String allChanges = MainApp.formatEventNameCleanup(new EventNameCleanupResult(
                java.util.List.of("Bad c/review"), java.util.List.of(rename), java.util.List.of("Year Zero")));
        assertTrue(allChanges.contains("Deleted 1 saved event"));
        assertTrue(allChanges.contains("Renamed 1 saved event"));
        assertTrue(allChanges.contains("year 0000"));
    }

    @Test
    public void formatDeletedEvents_escapesWhitespaceAndKeepsEmojiJoiners() {
        for (String spacing : UnicodeWhitespace.ALL) {
            if (spacing.equals(" ")) {
                continue;
            }
            String name = "Planning" + spacing + "c/review";
            int codePoint = spacing.codePointAt(0);
            String message = MainApp.formatDeletedEvents(java.util.List.of(name));
            assertTrue(message.startsWith("Deleted 1 saved event"));
            assertTrue(message.contains("\"Planning\\u" + String.format("%04X", codePoint) + "c/review\""));
            assertFalse(message.contains("\"Planning" + spacing + "c/review\""));
        }
        String multiple = MainApp.formatDeletedEvents(java.util.List.of(
                "One c/review", "Music 👩\u200d💻\u2003c/review"));
        assertTrue(multiple.startsWith("Deleted 2 saved events"));
        assertTrue(multiple.contains("\"Music 👩\u200d💻\\u2003c/review\""));
        assertTrue(MainApp.formatDeletedEvents(java.util.List.of("Quoted \" \\ \u2003\nName"))
                .contains("\"Quoted \\\" \\\\ \\u2003\\u000AName\""));
        String withLineSeparators = MainApp.formatDeletedEvents(java.util.List.of("Bad\u2028Next\u2029Last"));
        assertTrue(withLineSeparators.contains("\"Bad\\u2028Next\\u2029Last\""));
        assertFalse(withLineSeparators.contains("\u2028"));
        assertFalse(withLineSeparators.contains("\u2029"));
    }

    @Test
    public void init_invalidDataAfterDeletion_keepsFileAndLoadsEmptyModel() throws Exception {
        Path dataFile = testFolder.resolve("coordimate.json");
        String original = """
                {"persons":[],"events":[
                  {"name":"Bad\u2003c/review","startTime":"01-01-2027","endTime":"01-01-2027"},
                  {"name":"Broken","startTime":"bad","endTime":"01-01-2027"}]}
                """;
        Files.writeString(dataFile, original);
        MainApp app = new MainApp(testFolder.resolve("preferences.json"), dataFile);

        app.init();

        assertEquals(new CoordiMate(), app.model.getCoordiMate());
        assertEquals(original, Files.readString(dataFile));
        assertThrows(CommandException.class, () -> app.logic.execute(
                "addevent evn/New st/02-01-2027 et/02-01-2027"));
        assertEquals(original, Files.readString(dataFile));
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
