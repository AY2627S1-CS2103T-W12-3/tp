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

        assertEquals(names, storage.removeEventsWithEmbeddedParameters());

        assertEquals(expected, storage.readCoordiMate().orElseThrow());
        String cleaned = Files.readString(path);
        assertEquals(List.of(), storage.removeEventsWithEmbeddedParameters());
        assertEquals(cleaned, Files.readString(path));
        storage.saveCoordiMateIfUnchanged(expected);
        assertEquals(expected, new JsonCoordiMateStorage(path).readCoordiMate().orElseThrow());
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

        assertTrue(storage.removeEventsWithEmbeddedParameters().isEmpty());
        assertEquals(original, Files.readString(path));
        assertEquals(names.size(), storage.readCoordiMate().orElseThrow().getEventList().size());
    }

    @Test
    public void cleanup_missingOrUnaffectedFile_doesNotWrite() throws Exception {
        Path path = testFolder.resolve("data.json");
        StorageManager storage = storage(new JsonCoordiMateStorage(path));
        assertTrue(storage.removeEventsWithEmbeddedParameters().isEmpty());
        assertFalse(Files.exists(path));

        String original = "{\"persons\": [], \"events\": []}\n";
        Files.writeString(path, original);
        assertTrue(storage.removeEventsWithEmbeddedParameters().isEmpty());
        assertEquals(original, Files.readString(path));
    }

    @Test
    public void cleanup_onlyAffectedEvents_savesEmptyEventListAndPreservesIdCounter() throws Exception {
        Path path = testFolder.resolve("data.json");
        CoordiMate expected = new CoordiMate(40);
        writeLegacyData(path, expected, List.of("Bad\u2003c/review"));
        StorageManager storage = storage(new JsonCoordiMateStorage(path));

        assertEquals(List.of("Bad\u2003c/review"), storage.removeEventsWithEmbeddedParameters());
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
                    storage(new JsonCoordiMateStorage(path)).removeEventsWithEmbeddedParameters());
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
                    storage(new JsonCoordiMateStorage(path)).removeEventsWithEmbeddedParameters());
            assertEquals(original, Files.readString(path));
        }
    }

    @Test
    public void cleanup_malformedJsonOrUnreadablePath_preservesOriginalData() throws Exception {
        Path path = testFolder.resolve("data.json");
        for (String invalid : List.of("invalid JSON", "null", "{}")) {
            Files.writeString(path, invalid);
            assertThrows(DataLoadingException.class, () ->
                    storage(new JsonCoordiMateStorage(path)).removeEventsWithEmbeddedParameters());
            assertEquals(invalid, Files.readString(path));
        }
        Path directory = Files.createDirectory(testFolder.resolve("directory"));
        assertThrows(IOException.class, () ->
                storage(new JsonCoordiMateStorage(directory)).removeEventsWithEmbeddedParameters());
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
                storage(failingStorage).removeEventsWithEmbeddedParameters()).getMessage());
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

        assertThrows(DataConflictException.class, () -> storage(changingStorage).removeEventsWithEmbeddedParameters());
        assertEquals(concurrentData, new JsonCoordiMateStorage(path).readCoordiMate().orElseThrow());
    }

    @Test
    public void cleanup_concurrentSaveBeforeInitialLoad_pairsLoadedModelWithLatestRevision() throws Exception {
        Path path = testFolder.resolve("data.json");
        JsonCoordiMateStorage jsonStorage = new JsonCoordiMateStorage(path);
        writeLegacyData(path, new CoordiMate(), List.of("Bad\u2003c/review"));
        StorageManager storage = storage(jsonStorage);
        storage.removeEventsWithEmbeddedParameters();
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
        storage.removeEventsWithEmbeddedParameters();

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

        assertEquals(List.of("Bad\u2003c/review"), storage.removeEventsWithEmbeddedParameters());

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

        assertThrows(DataConflictException.class, storage::removeEventsWithEmbeddedParameters);
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
            assertThrows(IOException.class, storage::removeEventsWithEmbeddedParameters);
            assertEquals(original, Files.readString(path));
        }
        assertEquals(List.of("Bad\u2003c/review"), storage.removeEventsWithEmbeddedParameters());
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
