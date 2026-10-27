package coordimate.storage;

import static coordimate.testutil.TypicalPersons.getTypicalCoordiMate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import coordimate.commons.exceptions.DataLoadingException;
import coordimate.commons.util.JsonUtil;
import coordimate.model.CoordiMate;
import coordimate.model.ReadOnlyCoordiMate;
import coordimate.model.event.AttendanceStatus;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.person.Name;
import coordimate.model.tag.Tag;
import coordimate.testutil.UnicodeWhitespace;

public class EventNameCleanupTest {
    @TempDir
    public Path testFolder;

    @Test
    public void cleanup_embeddedParametersInActiveAndArchivedNames_deletesOnlyAffectedEvents() throws Exception {
        Path path = testFolder.resolve("data.json");
        CoordiMate expected = getTypicalCoordiMate();
        expected.addTag(new Tag("Custom"));
        Name member = expected.getPersonList().getFirst().getName();
        EventTime date = new EventTime("01-01-2027");
        expected.addEvent(new Event("Family 👨\u200d👩\u200d👧\u200d👦 Day", date, date,
                List.of(member), Map.of(member, AttendanceStatus.PRESENT), Set.of(date), true));
        expected.addEvent(new Event("\u0915\u094d\u200c\u0937 Workshop", date, date));
        List<String> names = new ArrayList<>();
        for (String spacing : UnicodeWhitespace.ALL) {
            names.add("Planning" + spacing + "c/review");
            names.add("Planning" + spacing + "/review");
        }
        writeLegacyData(path, expected, names);
        StorageManager storage = storage(new JsonCoordiMateStorage(path));

        assertEquals(names, storage.cleanUpEventNames().deletedNames());

        assertEquals(expected, storage.readCoordiMate().orElseThrow());
        String cleaned = Files.readString(path);
        assertTrue(storage.cleanUpEventNames().isEmpty());
        assertEquals(cleaned, Files.readString(path));
        storage.saveCoordiMateIfUnchanged(expected);
        assertEquals(expected, new JsonCoordiMateStorage(path).readCoordiMate().orElseThrow());
    }

    @Test
    public void cleanup_mixedNameAndYearZeroData_savesBothMigrationsTogether() throws Exception {
        Path path = testFolder.resolve("data.json");
        Files.writeString(path, """
                {"persons":[],"events":[
                  {"name":"Year\u2003c/review","startTime":"01-01-0000","endTime":"01-01-2027"},
                  {"name":"Bad\u2003c/review","startTime":"01-01-2027","endTime":"01-01-2027"},
                  {"name":"Planning Meeting","startTime":"01-01-2027","endTime":"01-01-2027",
                   "dismissedReminderStarts":["01-01-0000","01-01-2027"],"archived":true},
                  {"name":"Planning\u00a0Meeting","startTime":"02-01-2027","endTime":"02-01-2027"}]}
                """);
        int[] saves = {0};
        StorageManager storage = storage(new JsonCoordiMateStorage(path) {
            @Override
            public void saveCoordiMate(ReadOnlyCoordiMate data) throws IOException {
                saves[0]++;
                super.saveCoordiMate(data);
            }
        });

        EventNameCleanupResult changes = storage.cleanUpEventNames();

        assertEquals(1, saves[0]);
        assertEquals(List.of("Bad\u2003c/review"), changes.deletedNames());
        assertEquals(List.of("Year\u2003c/review"), changes.yearZeroDeletedNames());
        assertEquals(List.of(new EventNameCleanupResult.Rename("Planning\u00a0Meeting", "Planning Meeting (2)")),
                changes.renamedEvents());
        EventTime firstDate = new EventTime("01-01-2027");
        EventTime secondDate = new EventTime("02-01-2027");
        CoordiMate expected = new CoordiMate();
        expected.addEvent(new Event("Planning Meeting", firstDate, firstDate, List.of(), Map.of(),
                Set.of(firstDate), true));
        expected.addEvent(new Event("Planning Meeting (2)", secondDate, secondDate));
        assertEquals(expected, storage.readCoordiMate().orElseThrow());
        storage.saveCoordiMateIfUnchanged(expected);
        assertTrue(storage.cleanUpEventNames().isEmpty());
        assertEquals(2, saves[0]);
    }

    @Test
    public void cleanup_yearZeroHistoryOnly_savesWithoutDeletingEvent() throws Exception {
        Path path = testFolder.resolve("data.json");
        Files.writeString(path, """
                {"persons":[],"events":[
                  {"name":"Planning Meeting","startTime":"01-01-2027","endTime":"01-01-2027",
                   "dismissedReminderStarts":["01-01-0000","01-01-2027"]}]}
                """);
        StorageManager storage = storage(new JsonCoordiMateStorage(path));

        assertTrue(storage.cleanUpEventNames().isEmpty());

        Event event = storage.readCoordiMate().orElseThrow().getEventList().getFirst();
        assertEquals("Planning Meeting", event.getName());
        assertEquals(Set.of(new EventTime("01-01-2027")), event.getDismissedReminderStarts());
        assertFalse(Files.readString(path).contains("0000"));
        storage.saveCoordiMateIfUnchanged(storage.readCoordiMate().orElseThrow());
    }

    @Test
    public void cleanup_mixedLegacyDataWithInvalidRemainder_preservesOriginalFile() throws Exception {
        Path path = testFolder.resolve("data.json");
        String original = """
                {"persons":[],"events":[
                  {"name":"Year Zero","startTime":"01-01-0000","endTime":"01-01-2027"},
                  {"name":"Bad\u2003c/review","startTime":"01-01-2027","endTime":"01-01-2027"},
                  {"name":"Planning Meeting","startTime":"01-01-2027","endTime":"01-01-2027"},
                  {"name":"Planning\u00a0Meeting","startTime":"02-01-2027","endTime":"02-01-2027"},
                  {"name":"Broken","startTime":"bad","endTime":"01-01-2027"}]}
                """;
        Files.writeString(path, original);

        assertThrows(DataLoadingException.class, () -> storage(new JsonCoordiMateStorage(path)).cleanUpEventNames());

        assertEquals(original, Files.readString(path));
    }

    @Test
    public void cleanup_usableNamesAndFormatCharacters_preservesFile() throws Exception {
        Path path = testFolder.resolve("data.json");
        List<String> names = new ArrayList<>(List.of("/review", "\u2003c/review\u2003",
                "Planningc/review", "Planning 123/review", "Planning /123", "Planning 中文/review"));
        for (String character : List.of("\u180e", "\u200b", "\u2060", "\ufeff")) {
            names.add("Planning" + character + "c/review");
            names.add(character + "Planning");
            names.add("Planning" + character);
        }
        writeLegacyData(path, new CoordiMate(), names);
        String original = Files.readString(path);
        StorageManager storage = storage(new JsonCoordiMateStorage(path));

        assertTrue(storage.cleanUpEventNames().isEmpty());
        assertEquals(original, Files.readString(path));
        assertEquals(names.size(), storage.readCoordiMate().orElseThrow().getEventList().size());
    }

    @Test
    public void cleanup_missingOrUnaffectedFile_doesNotWrite() throws Exception {
        Path path = testFolder.resolve("data.json");
        StorageManager storage = storage(new JsonCoordiMateStorage(path));
        assertTrue(storage.cleanUpEventNames().isEmpty());
        assertFalse(Files.exists(path));

        String original = "{\"persons\": [], \"events\": []}\n";
        Files.writeString(path, original);
        assertTrue(storage.cleanUpEventNames().isEmpty());
        assertEquals(original, Files.readString(path));
    }

    @Test
    public void cleanup_onlyAffectedEvents_savesEmptyEventListAndPreservesIdCounter() throws Exception {
        Path path = testFolder.resolve("data.json");
        CoordiMate expected = new CoordiMate(40);
        writeLegacyData(path, expected, List.of("Bad\u2003c/review"));
        StorageManager storage = storage(new JsonCoordiMateStorage(path));

        assertEquals(List.of("Bad\u2003c/review"), storage.cleanUpEventNames().deletedNames());
        assertEquals(expected, storage.readCoordiMate().orElseThrow());
        assertEquals(40, storage.readCoordiMate().orElseThrow().getNextMemberId());
    }

    @Test
    public void cleanup_invalidRemainingData_preservesOriginalFile() throws Exception {
        Path path = testFolder.resolve("data.json");
        for (String remaining : new String[] {"null", "{}",
            "{\"name\":\"Keep\",\"startTime\":\"invalid\",\"endTime\":\"01-01-2027\"}",
            "{\"name\":\"Keep\",\"startTime\":\"01-01-2027\",\"endTime\":\"01-01-2027\","
                    + "\"members\":[\"Missing Contact\"]}"}) {
            String original = """
                    {"persons":[],"events":[
                      {"name":"Bad\u2003c/review","startTime":"01-01-2027","endTime":"01-01-2027"},
                      %s]}
                    """.formatted(remaining);
            Files.writeString(path, original);

            assertThrows(DataLoadingException.class, () ->
                    storage(new JsonCoordiMateStorage(path)).cleanUpEventNames());
            assertEquals(original, Files.readString(path));
        }
    }

    @Test
    public void cleanup_invalidContactOrMetadata_preservesOriginalFile() throws Exception {
        Path path = testFolder.resolve("data.json");
        for (String field : List.of("persons", "tags", "nextMemberId")) {
            writeLegacyData(path, getTypicalCoordiMate(), List.of("Bad\u2003c/review"));
            ObjectNode json = JsonUtil.fromJsonString(Files.readString(path), ObjectNode.class);
            if (field.equals("nextMemberId")) {
                json.set(field, LongNode.valueOf(-1));
            } else {
                json.withArray(field).addNull();
            }
            String original = JsonUtil.toJsonString(json);
            Files.writeString(path, original);

            assertThrows(DataLoadingException.class, () ->
                    storage(new JsonCoordiMateStorage(path)).cleanUpEventNames());
            assertEquals(original, Files.readString(path));
        }
    }

    @Test
    public void cleanup_malformedJsonOrUnreadablePath_preservesOriginalData() throws Exception {
        Path path = testFolder.resolve("data.json");
        for (String invalid : List.of("invalid JSON", "null", "{}")) {
            Files.writeString(path, invalid);
            assertThrows(DataLoadingException.class, () ->
                    storage(new JsonCoordiMateStorage(path)).cleanUpEventNames());
            assertEquals(invalid, Files.readString(path));
        }
        Path directory = Files.createDirectory(testFolder.resolve("directory"));
        assertThrows(IOException.class, () ->
                storage(new JsonCoordiMateStorage(directory)).cleanUpEventNames());
        assertTrue(Files.isDirectory(directory));
    }

    @Test
    public void cleanup_saveFailure_doesNotReturnDeletionsOrChangeFile() throws Exception {
        Path path = testFolder.resolve("data.json");
        writeLegacyData(path, getTypicalCoordiMate(), List.of("Bad\u2003c/review"));
        String original = Files.readString(path);
        JsonCoordiMateStorage failingStorage = new JsonCoordiMateStorage(path) {
            @Override
            public void saveCoordiMate(ReadOnlyCoordiMate data) throws IOException {
                throw new IOException("Cannot save cleanup");
            }
        };

        assertEquals("Cannot save cleanup", assertThrows(IOException.class, () ->
                storage(failingStorage).cleanUpEventNames()).getMessage());
        assertEquals(original, Files.readString(path));
    }

    @Test
    public void cleanup_fileChangesWhilePreparing_doesNotOverwriteConcurrentSave() throws Exception {
        Path path = testFolder.resolve("data.json");
        writeLegacyData(path, new CoordiMate(), List.of("Bad\u2003c/review"));
        CoordiMate concurrentData = getTypicalCoordiMate();
        JsonCoordiMateStorage changingStorage = new JsonCoordiMateStorage(path) {
            @Override
            Optional<EventNameCleanup> readEventNameCleanup() throws DataLoadingException {
                Optional<EventNameCleanup> result = super.readEventNameCleanup();
                try {
                    new JsonCoordiMateStorage(path).saveCoordiMate(concurrentData);
                } catch (IOException e) {
                    throw new DataLoadingException(e);
                }
                return result;
            }
        };

        assertThrows(DataConflictException.class, () -> storage(changingStorage).cleanUpEventNames());
        assertEquals(concurrentData, new JsonCoordiMateStorage(path).readCoordiMate().orElseThrow());
    }

    @Test
    public void cleanup_concurrentSaveBeforeInitialLoad_pairsLoadedModelWithLatestRevision() throws Exception {
        Path path = testFolder.resolve("data.json");
        JsonCoordiMateStorage jsonStorage = new JsonCoordiMateStorage(path);
        writeLegacyData(path, new CoordiMate(), List.of("Bad\u2003c/review"));
        StorageManager storage = storage(jsonStorage);
        storage.cleanUpEventNames();
        StorageManager otherWindow = storage(new JsonCoordiMateStorage(path));
        otherWindow.readCoordiMate();
        CoordiMate concurrentData = getTypicalCoordiMate();
        otherWindow.saveCoordiMateIfUnchanged(concurrentData);

        ReadOnlyCoordiMate loaded = storage.readCoordiMate().orElseThrow();

        assertEquals(concurrentData, loaded);
        storage.saveCoordiMateIfUnchanged(loaded);
        assertEquals(concurrentData, jsonStorage.readCoordiMate().orElseThrow());
    }

    @Test
    public void cleanup_collidingNames_preservesDetailsAndSkipsReservedSuffixes() throws Exception {
        Path path = testFolder.resolve("data.json");
        CoordiMate expected = getTypicalCoordiMate();
        Name member = expected.getPersonList().getFirst().getName();
        EventTime date = new EventTime("01-01-2027");
        Event first = new Event("Planning Meeting", date, date);
        Event second = new Event("planning meeting (3)", date, date, List.of(member),
                Map.of(member, AttendanceStatus.ABSENT), Set.of(date), true);
        Event reserved = new Event("Planning Meeting (2)", date, date);
        expected.addEvent(first);
        expected.addEvent(second);
        expected.addEvent(reserved);
        new JsonCoordiMateStorage(path).saveCoordiMate(expected);
        ObjectNode json = JsonUtil.fromJsonString(Files.readString(path), ObjectNode.class);
        ObjectNode savedSecondEvent = (ObjectNode) json.withArray("events").get(1);
        savedSecondEvent.put("name", "planning\u00a0meeting");
        Files.writeString(path, JsonUtil.toJsonString(json));
        StorageManager storage = storage(new JsonCoordiMateStorage(path));

        EventNameCleanupResult changes = storage.cleanUpEventNames();

        assertEquals(List.of(), changes.deletedNames());
        assertEquals(List.of(new EventNameCleanupResult.Rename("planning\u00a0meeting", "planning meeting (3)")),
                changes.renamedEvents());
        assertEquals(expected, storage.readCoordiMate().orElseThrow());
        String migrated = Files.readString(path);
        assertTrue(storage.cleanUpEventNames().isEmpty());
        assertEquals(migrated, Files.readString(path));
        storage.saveCoordiMateIfUnchanged(expected);
    }

    @Test
    public void cleanup_multipleCollisionsWithLongUnicodeNames_usesUniqueValidNames() throws Exception {
        Path path = testFolder.resolve("data.json");
        String name = "\ud83d\ude00".repeat(96) + " X Y";
        List<String> names = List.of(name, name.replace(" X", "\u00a0X"), name.replace(" X", "\u202fX"));
        writeLegacyData(path, new CoordiMate(), names);
        StorageManager storage = storage(new JsonCoordiMateStorage(path));

        EventNameCleanupResult changes = storage.cleanUpEventNames();

        assertEquals(List.of(
                new EventNameCleanupResult.Rename(names.get(1), "\ud83d\ude00".repeat(96) + " (2)"),
                new EventNameCleanupResult.Rename(names.get(2), "\ud83d\ude00".repeat(96) + " (3)")),
                changes.renamedEvents());
        List<Event> loaded = storage.readCoordiMate().orElseThrow().getEventList();
        assertEquals(3, loaded.size());
        assertTrue(loaded.stream().allMatch(event -> Event.isValidName(event.getName())));
        assertEquals(3, loaded.stream().map(Event::getName).distinct().count());
    }

    @Test
    public void cleanup_existingDuplicateNames_keepsOriginalFile() throws Exception {
        Path path = testFolder.resolve("data.json");
        for (List<String> names : List.of(
                List.of("Planning Meeting", "PLANNING MEETING"),
                List.of("Planning  Meeting", "Planning Meeting"),
                List.of("Planning\u00a0Meeting", "PLANNING\u00a0MEETING"))) {
            writeLegacyData(path, new CoordiMate(), names);
            String original = Files.readString(path);

            assertThrows(DataLoadingException.class, () ->
                    storage(new JsonCoordiMateStorage(path)).cleanUpEventNames());

            assertEquals(original, Files.readString(path));
        }
    }

    @Test
    public void cleanup_collisionWithInvalidRemainingData_keepsOriginalFile() throws Exception {
        Path path = testFolder.resolve("data.json");
        writeLegacyData(path, new CoordiMate(), List.of("Planning Meeting", "Planning\u00a0Meeting"));
        ObjectNode json = JsonUtil.fromJsonString(Files.readString(path), ObjectNode.class);
        ObjectNode savedSecondEvent = (ObjectNode) json.withArray("events").get(1);
        savedSecondEvent.putArray("members").add("Missing Contact");
        Files.writeString(path, JsonUtil.toJsonString(json));
        String original = Files.readString(path);

        assertThrows(DataLoadingException.class, () -> storage(new JsonCoordiMateStorage(path)).cleanUpEventNames());

        assertEquals(original, Files.readString(path));
    }

    @Test
    public void cleanup_collisionSaveFailure_keepsOriginalFile() throws Exception {
        Path path = testFolder.resolve("data.json");
        writeLegacyData(path, new CoordiMate(), List.of("Planning Meeting", "Planning\u00a0Meeting"));
        String original = Files.readString(path);
        JsonCoordiMateStorage failingStorage = new JsonCoordiMateStorage(path) {
            @Override
            public void saveCoordiMate(ReadOnlyCoordiMate data) throws IOException {
                throw new IOException("Cannot save migration");
            }
        };

        assertThrows(IOException.class, () -> storage(failingStorage).cleanUpEventNames());

        assertEquals(original, Files.readString(path));
    }

    @Test
    public void cleanup_fileChangesDuringInitialLoad_retriesWithLatestRevision() throws Exception {
        Path path = testFolder.resolve("data.json");
        writeLegacyData(path, new CoordiMate(), List.of("Bad\u2003c/review"));
        CoordiMate concurrentData = getTypicalCoordiMate();
        JsonCoordiMateStorage changingStorage = new JsonCoordiMateStorage(path) {
            private boolean isFirstRead = true;

            @Override
            public Optional<ReadOnlyCoordiMate> readCoordiMate() throws DataLoadingException {
                if (isFirstRead) {
                    isFirstRead = false;
                    try {
                        new JsonCoordiMateStorage(path).saveCoordiMate(concurrentData);
                    } catch (IOException e) {
                        throw new DataLoadingException(e);
                    }
                }
                return super.readCoordiMate();
            }
        };
        StorageManager storage = storage(changingStorage);
        storage.cleanUpEventNames();

        ReadOnlyCoordiMate loaded = storage.readCoordiMate().orElseThrow();

        assertEquals(concurrentData, loaded);
        storage.saveCoordiMateIfUnchanged(loaded);
    }

    @Test
    public void cleanup_storageAlreadyLoaded_updatesRevisionAfterSuccessfulCleanup() throws Exception {
        Path path = testFolder.resolve("data.json");
        writeLegacyData(path, new CoordiMate(), List.of("Bad\u2003c/review"));
        StorageManager storage = storage(new JsonCoordiMateStorage(path));
        storage.readCoordiMate();

        assertEquals(List.of("Bad\u2003c/review"), storage.cleanUpEventNames().deletedNames());

        storage.saveCoordiMateIfUnchanged(storage.readCoordiMate().orElseThrow());
        new JsonCoordiMateStorage(path).saveCoordiMate(getTypicalCoordiMate());
        assertThrows(DataConflictException.class, () ->
                storage.saveCoordiMateIfUnchanged(storage.readCoordiMate().orElseThrow()));
    }

    @Test
    public void cleanup_storageAlreadyLoaded_rejectsChangedRevision() throws Exception {
        Path path = testFolder.resolve("data.json");
        JsonCoordiMateStorage jsonStorage = new JsonCoordiMateStorage(path);
        jsonStorage.saveCoordiMate(new CoordiMate());
        StorageManager storage = storage(jsonStorage);
        storage.readCoordiMate();
        writeLegacyData(path, getTypicalCoordiMate(), List.of("Bad\u2003c/review"));
        String original = Files.readString(path);

        assertThrows(DataConflictException.class, storage::cleanUpEventNames);
        assertEquals(original, Files.readString(path));
    }

    @Test
    public void cleanup_lockHeld_preservesFileAndCanRetryAfterRelease() throws Exception {
        Path path = testFolder.resolve("data.json");
        CoordiMate expected = getTypicalCoordiMate();
        writeLegacyData(path, expected, List.of("Bad\u2003c/review"));
        String original = Files.readString(path);
        StorageManager storage = storage(new JsonCoordiMateStorage(path));
        Path lockPath = path.resolveSibling(path.getFileName() + ".lock");
        try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                FileLock ignored = channel.lock()) {
            assertThrows(IOException.class, storage::cleanUpEventNames);
            assertEquals(original, Files.readString(path));
        }
        assertEquals(List.of("Bad\u2003c/review"), storage.cleanUpEventNames().deletedNames());
        assertEquals(expected, storage.readCoordiMate().orElseThrow());
    }

    private StorageManager storage(JsonCoordiMateStorage jsonStorage) {
        return new StorageManager(jsonStorage, new JsonUserPrefsStorage(testFolder.resolve("prefs.json")));
    }

    private void writeLegacyData(Path path, CoordiMate validData, List<String> names) throws Exception {
        new JsonCoordiMateStorage(path).saveCoordiMate(validData);
        ObjectNode json = JsonUtil.fromJsonString(Files.readString(path), ObjectNode.class);
        for (int i = 0; i < names.size(); i++) {
            json.withArray("events").addObject().put("name", names.get(i))
                    .put("startTime", "01-01-2027").put("endTime", "01-01-2027")
                    .put("archived", i % 2 == 1);
        }
        Files.writeString(path, JsonUtil.toJsonString(json));
    }
}
