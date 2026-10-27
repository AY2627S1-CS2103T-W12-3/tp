package coordimate.storage;

import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BENSON;
import static coordimate.testutil.TypicalPersons.CARL;
import static coordimate.testutil.TypicalPersons.HOON;
import static coordimate.testutil.TypicalPersons.IDA;
import static coordimate.testutil.TypicalPersons.getTypicalCoordiMate;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import coordimate.commons.exceptions.DataLoadingException;
import coordimate.commons.util.JsonUtil;
import coordimate.model.CoordiMate;
import coordimate.model.ReadOnlyCoordiMate;
import coordimate.model.event.AttendanceStatus;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.person.Person;
import coordimate.model.tag.Tag;
import coordimate.testutil.PersonBuilder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class JsonCoordiMateStorageTest {
    private static final Path TEST_DATA_FOLDER = Paths.get("src", "test", "data", "JsonCoordiMateStorageTest");

    @TempDir
    public Path testFolder;

    @Test
    public void recoverSavedData_missingFile_doesNotCreateFile() throws Exception {
        Path path = testFolder.resolve("missing.json");
        assertEquals("", new JsonCoordiMateStorage(path).recoverSavedData().getWarning());
        assertFalse(Files.exists(path));
    }

    @Test
    public void recoverSavedData_noRepairs_doesNotRewriteFile() throws Exception {
        Path path = testFolder.resolve("data.json");
        String original = "{\"persons\":[], \"events\":[]}";
        Files.writeString(path, original);
        assertEquals("", new JsonCoordiMateStorage(path).recoverSavedData().getWarning());
        assertEquals(original, Files.readString(path));
    }

    @Test
    public void recoverSavedData_yearZeroEvents_preservesValidDataAndDeletesOnlyOnce() throws Exception {
        CoordiMate expected = getTypicalCoordiMate();
        EventTime start = new EventTime("01-01-0001");
        expected.addEvent(new Event("Keep", start, new EventTime("31-12-9999 23:59"),
                List.of(ALICE.getName()), Map.of(ALICE.getName(), AttendanceStatus.PRESENT), Set.of(start)));
        Path path = writeLegacyEvents(expected,
                new JsonAdaptedEvent("Zero Start", " 01-01-0000 ", "02-01-0001"),
                new JsonAdaptedEvent("Zero End", "01-01-0001", "02-01-0000 12:30"),
                new JsonAdaptedEvent("Zero History", "01-01-0001", "02-01-0001",
                        List.of(ALICE.getName().toString()), Map.of(ALICE.getName().toString(), "absent"),
                        List.of("01-01-0001", "01-01-0000 00:00"), true),
                new JsonAdaptedEvent(null, "01-01-0000", "02-01-0000"));
        expected.addEvent(new Event("Zero History", start, new EventTime("02-01-0001"),
                List.of(ALICE.getName()), Map.of(ALICE.getName(), AttendanceStatus.ABSENT), Set.of(start))
                .withArchived(true));
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(path);

        assertEquals(List.of("Zero Start", "Zero End", "(unnamed event)"),
                storage.recoverSavedData().deletedEvents());
        assertEquals(expected, storage.readCoordiMate().orElseThrow());
        String cleaned = Files.readString(path);
        assertEquals("", storage.recoverSavedData().getWarning());
        assertEquals(cleaned, Files.readString(path));
    }

    @Test
    public void recoverSavedData_historyOnly_preservesEventAndSavesCleanedHistory() throws Exception {
        CoordiMate expected = getTypicalCoordiMate();
        EventTime start = new EventTime("08-08-2026");
        expected.addEvent(new Event("Concert", start, new EventTime("09-08-2026"),
                List.of(ALICE.getName()), Map.of(ALICE.getName(), AttendanceStatus.PRESENT), Set.of(start)));
        Path path = writeLegacyEvents(getTypicalCoordiMate(),
                new JsonAdaptedEvent("Concert", "08-08-2026", "09-08-2026",
                        List.of(ALICE.getName().toString()), Map.of(ALICE.getName().toString(), "present"),
                        List.of("08-08-2026", "01-01-0000")));
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(path);

        assertEquals("", storage.recoverSavedData().getWarning());
        assertEquals(expected, storage.readCoordiMate().orElseThrow());
        String cleaned = Files.readString(path);
        assertFalse(cleaned.contains("01-01-0000"));
        assertEquals("", storage.recoverSavedData().getWarning());
        assertEquals(cleaned, Files.readString(path));
    }

    @Test
    public void recoverSavedData_otherInvalidData_doesNotRewriteFile() throws Exception {
        Path path = writeLegacyEvents(new CoordiMate(),
                new JsonAdaptedEvent("Zero", "01-01-0000", "02-01-0000"),
                new JsonAdaptedEvent("Invalid", "31-02-2026", "01-03-2026"));
        String original = Files.readString(path);
        assertThrows(DataLoadingException.class, () -> new JsonCoordiMateStorage(path).recoverSavedData());
        assertEquals(original, Files.readString(path));
    }

    @Test
    public void recoverSavedData_saveFailure_doesNotRewriteFile() throws Exception {
        Path path = writeLegacyEvents(new CoordiMate(),
                new JsonAdaptedEvent("Zero", "01-01-0000", "02-01-0000"));
        String original = Files.readString(path);
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(path) {
            @Override
            public void saveCoordiMate(ReadOnlyCoordiMate data) throws IOException {
                throw new IOException("Disk full");
            }
        };
        assertThrows(IOException.class, storage::recoverSavedData);
        assertEquals(original, Files.readString(path));
    }

    @Test
    public void recoverSavedData_leaveAssignments_removesMembersAndAttendanceOnlyOnce() throws Exception {
        CoordiMate original = getTypicalCoordiMate();
        original.setPerson(ALICE, new PersonBuilder(ALICE).withLeaveDate("08-08-2026").build());
        original.setPerson(BENSON, new PersonBuilder(BENSON).withLeaveDate("09-08-2026").build());
        EventTime start = new EventTime("08-08-2026 15:00");
        Event concert = new Event("Concert", start, new EventTime("08-08-2026 23:59"),
                List.of(CARL.getName(), ALICE.getName(), BENSON.getName()),
                Map.of(CARL.getName(), AttendanceStatus.PRESENT, ALICE.getName(), AttendanceStatus.ABSENT),
                Set.of(start));
        Event archived = new Event("Archived", new EventTime("07-08-2026"), new EventTime("08-08-2026"),
                List.of(ALICE.getName()), Map.of(ALICE.getName(), AttendanceStatus.PRESENT)).withArchived(true);
        original.addEvent(concert);
        original.addEvent(archived);
        Path path = testFolder.resolve("data.json");
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(path);
        storage.saveCoordiMate(original);
        CoordiMate expected = new CoordiMate(original);
        expected.setEvent(concert, concert.withMembersAndAttendance(List.of(CARL.getName()),
                Map.of(CARL.getName(), AttendanceStatus.PRESENT)));
        expected.setEvent(archived, archived.withMembersAndAttendance(List.of(), Map.of()));

        JsonCoordiMateStorage.RecoveryResult result = storage.recoverSavedData();

        assertEquals(List.of(), result.deletedEvents());
        assertEquals(List.of("Concert: Alice Pauline, Benson Meier", "Archived: Alice Pauline"),
                result.removedAssignments());
        assertEquals("Removed the following event assignments because the members are on leave for the entire event. "
                + "Any associated attendance records were also removed:\n\n"
                + "Concert: Alice Pauline, Benson Meier\nArchived: Alice Pauline", result.getWarning());
        assertEquals(expected, storage.readCoordiMate().orElseThrow());
        String cleaned = Files.readString(path);
        assertEquals("", storage.recoverSavedData().getWarning());
        assertEquals(cleaned, Files.readString(path));
    }

    @Test
    public void recoverSavedData_partialOverlapAndEndedLeave_preservesAssignmentsAndFile() throws Exception {
        CoordiMate original = getTypicalCoordiMate();
        original.setPerson(ALICE, new PersonBuilder(ALICE).withLeaveDate("08-08-2026").build());
        original.setPerson(BENSON, new PersonBuilder(BENSON).withLeaveDate("07-08-2026").build());
        Event overnight = new Event("Overnight", new EventTime("08-08-2026 15:00"),
                new EventTime("09-08-2026 00:00"), List.of(ALICE.getName(), BENSON.getName(), CARL.getName()),
                Map.of(ALICE.getName(), AttendanceStatus.PRESENT, BENSON.getName(), AttendanceStatus.ABSENT));
        original.addEvent(overnight);
        original.addEvent(overnight.withDetails("Date Only", new EventTime("08-08-2026"),
                new EventTime("09-08-2026")));
        Path path = testFolder.resolve("data.json");
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(path);
        storage.saveCoordiMate(original);
        String before = Files.readString(path);

        assertEquals("", storage.recoverSavedData().getWarning());
        assertEquals(original, storage.readCoordiMate().orElseThrow());
        assertEquals(before, Files.readString(path));
    }

    @Test
    public void recoverSavedData_yearZeroAndLeave_combinesRepairsAndWarnings() throws Exception {
        CoordiMate original = getTypicalCoordiMate();
        original.setPerson(ALICE, new PersonBuilder(ALICE).withLeaveDate("08-08-2026").build());
        Event concert = new Event("Concert", new EventTime("08-08-2026"), new EventTime("08-08-2026"),
                List.of(ALICE.getName()));
        original.addEvent(concert);
        Path path = writeLegacyEvents(original, new JsonAdaptedEvent("Year Zero", "01-01-0000", "02-01-0000"));
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(path);

        JsonCoordiMateStorage.RecoveryResult result = storage.recoverSavedData();

        assertEquals(List.of("Year Zero"), result.deletedEvents());
        assertEquals(List.of("Concert: Alice Pauline"), result.removedAssignments());
        assertTrue(result.getWarning().startsWith("Deleted the following events"));
        assertTrue(result.getWarning().contains("Year Zero\n\nRemoved the following event assignments"));
        original.setEvent(concert, concert.withMembersAndAttendance(List.of(), Map.of()));
        assertEquals(original, storage.readCoordiMate().orElseThrow());
    }

    @Test
    public void recoverSavedData_leaveAndInvalidData_doesNotRewriteFile() throws Exception {
        CoordiMate original = getTypicalCoordiMate();
        original.setPerson(ALICE, new PersonBuilder(ALICE).withLeaveDate("08-08-2026").build());
        original.addEvent(new Event("Concert", new EventTime("08-08-2026"), new EventTime("08-08-2026"),
                List.of(ALICE.getName())));
        Path path = writeLegacyEvents(original, new JsonAdaptedEvent("Unknown Member", "08-08-2026", "08-08-2026",
                List.of("Missing Contact")));
        String before = Files.readString(path);

        assertThrows(DataLoadingException.class, () -> new JsonCoordiMateStorage(path).recoverSavedData());
        assertEquals(before, Files.readString(path));
    }

    @Test
    public void recoverSavedData_leaveSaveFailure_doesNotRewriteFile() throws Exception {
        CoordiMate original = getTypicalCoordiMate();
        original.setPerson(ALICE, new PersonBuilder(ALICE).withLeaveDate("08-08-2026").build());
        original.addEvent(new Event("Concert", new EventTime("08-08-2026"), new EventTime("08-08-2026"),
                List.of(ALICE.getName()), Map.of(ALICE.getName(), AttendanceStatus.PRESENT)));
        Path path = testFolder.resolve("data.json");
        new JsonCoordiMateStorage(path).saveCoordiMate(original);
        String before = Files.readString(path);
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(path) {
            @Override
            public void saveCoordiMate(ReadOnlyCoordiMate data) throws IOException {
                throw new IOException("Disk full");
            }
        };

        assertThrows(IOException.class, storage::recoverSavedData);
        assertEquals(before, Files.readString(path));
    }

    private Path writeLegacyEvents(CoordiMate validData, JsonAdaptedEvent... legacyEvents) throws Exception {
        ObjectNode json = JsonUtil.fromJsonString(
                JsonUtil.toJsonString(new JsonSerializableCoordiMate(validData)), ObjectNode.class);
        for (JsonAdaptedEvent event : legacyEvents) {
            json.withArray("events").add(JsonUtil.fromJsonString(JsonUtil.toJsonString(event), JsonNode.class));
        }
        Path path = testFolder.resolve("legacy.json");
        Files.writeString(path, JsonUtil.toJsonString(json));
        return path;
    }

    @Test
    public void readCoordiMate_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> readCoordiMate(null));
    }

    private java.util.Optional<ReadOnlyCoordiMate> readCoordiMate(String filePath) throws Exception {
        return new JsonCoordiMateStorage(Paths.get(filePath)).readCoordiMate(addToTestDataPathIfNotNull(filePath));
    }

    private Path addToTestDataPathIfNotNull(String prefsFileInTestDataFolder) {
        return prefsFileInTestDataFolder != null
                ? TEST_DATA_FOLDER.resolve(prefsFileInTestDataFolder)
                : null;
    }

    @Test
    public void read_missingFile_emptyResult() throws Exception {
        assertFalse(readCoordiMate("NonExistentFile.json").isPresent());
    }

    @Test
    public void read_notJsonFormat_exceptionThrown() {
        assertThrows(DataLoadingException.class, () -> readCoordiMate("notJsonFormatCoordiMate.json"));
    }

    @Test
    public void readCoordiMate_invalidPersonCoordiMate_throwDataLoadingException() {
        assertThrows(DataLoadingException.class, () -> readCoordiMate("invalidPersonCoordiMate.json"));
    }

    @Test
    public void readCoordiMate_invalidAndValidPersonCoordiMate_throwDataLoadingException() {
        assertThrows(DataLoadingException.class, () -> readCoordiMate("invalidAndValidPersonCoordiMate.json"));
    }

    @Test
    public void readCoordiMate_invalidEvents_throwDataLoadingException() {
        assertLoadFails("invalidEventCoordiMate.json", Event.MESSAGE_INVALID_TIME_ORDER);
        assertLoadFails("invalidEventNameCoordiMate.json", Event.MESSAGE_EMPTY_NAME);
        assertLoadFails("invalidAttendanceCoordiMate.json", JsonAdaptedEvent.MESSAGE_INVALID_ATTENDANCE_STATUS);
        assertLoadFails("unassignedAttendanceCoordiMate.json", Event.MESSAGE_UNASSIGNED_ATTENDANCE);
    }

    @Test
    public void readCoordiMate_invalidTag_throwDataLoadingException() {
        assertLoadFails("invalidTagCoordiMate.json", Tag.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void readCoordiMate_duplicateEventsOrTags_throwDataLoadingException() {
        assertLoadFails("duplicateEventCoordiMate.json", JsonSerializableCoordiMate.MESSAGE_DUPLICATE_EVENT);
        assertLoadFails("duplicateTagCoordiMate.json", JsonSerializableCoordiMate.MESSAGE_DUPLICATE_TAG);
    }

    @Test
    public void readCoordiMate_jsonNullOrMissingPersons_throwDataLoadingException() {
        assertLoadFails("nullJsonCoordiMate.json", "JSON file must contain an object.");
        assertLoadFails("missingPersonsCoordiMate.json", "Persons list must be present.");
    }

    /**
     * Asserts that loading {@code fileName} from the test data folder fails, and that the innermost cause
     * has {@code expectedMessage}, so the file is rejected for the intended reason.
     */
    private void assertLoadFails(String fileName, String expectedMessage) {
        DataLoadingException exception =
                assertThrows(DataLoadingException.class, () -> readCoordiMate(fileName), fileName);
        Throwable rootCause = exception;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }
        assertEquals(expectedMessage, rootCause.getMessage(), fileName);
    }

    @Test
    public void readAndSaveCoordiMate_legacyPhoneNumbers_preservesContacts() throws Exception {
        ReadOnlyCoordiMate legacyData = readCoordiMate("legacyPhoneCoordiMate.json").orElseThrow();
        assertEquals("123", legacyData.getPersonList().get(0).getPhone().getValue());
        assertEquals("1234567890123456", legacyData.getPersonList().get(1).getPhone().getValue());

        Path filePath = testFolder.resolve("LegacyPhones.json");
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(filePath);
        storage.saveCoordiMate(legacyData);

        assertEquals(legacyData, storage.readCoordiMate().orElseThrow());
    }

    @Test
    public void readAndSaveCoordiMate_legacyLongTag_preservesContactAndTag() throws Exception {
        String legacyTagName = "1234567890123456789012345678901";
        ReadOnlyCoordiMate legacyData = readCoordiMate("legacyLongTagCoordiMate.json").orElseThrow();

        assertEquals(1, legacyData.getPersonList().size());
        assertTrue(legacyData.getPersonList().getFirst().getTags().contains(new Tag(legacyTagName)));
        assertTrue(legacyData.getTagList().contains(new Tag(legacyTagName)));

        Path filePath = testFolder.resolve("LegacyLongTag.json");
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(filePath);
        storage.saveCoordiMate(legacyData);

        assertEquals(legacyData, storage.readCoordiMate().orElseThrow());
    }

    @Test
    public void saveCoordiMate_existingFile_replacedWithoutTemporaryFiles() throws Exception {
        Path filePath = testFolder.resolve("coordimate.json");
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(filePath);
        storage.saveCoordiMate(getTypicalCoordiMate());

        CoordiMate smaller = new CoordiMate();
        smaller.addPerson(HOON);
        storage.saveCoordiMate(smaller);

        assertEquals(smaller, new CoordiMate(storage.readCoordiMate().orElseThrow()));
        assertEquals(List.of("coordimate.json"), listFileNames(testFolder));
    }

    @Test
    public void saveCoordiMate_parentPathIsFile_throwsIoExceptionAndKeepsFile() throws Exception {
        Path notAFolder = testFolder.resolve("data");
        Files.writeString(notAFolder, "not a folder");
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(notAFolder.resolve("coordimate.json"));

        assertThrows(IOException.class, () -> storage.saveCoordiMate(getTypicalCoordiMate()));
        assertEquals("not a folder", Files.readString(notAFolder));
        assertEquals(List.of("data"), listFileNames(testFolder));
    }

    @Test
    public void saveCoordiMate_failureWhileWriting_existingFileUnchangedAndNoTemporaryFile() throws Exception {
        Path filePath = testFolder.resolve("coordimate.json");
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(filePath);
        storage.saveCoordiMate(getTypicalCoordiMate());
        byte[] savedBytes = Files.readAllBytes(filePath);

        assertThrows(IllegalStateException.class, () -> storage.saveCoordiMate(new FailingCoordiMate()));

        assertArrayEquals(savedBytes, Files.readAllBytes(filePath));
        assertEquals(List.of("coordimate.json"), listFileNames(testFolder));
    }

    @Test
    public void readAndSaveCoordiMate_allInOrder_success() throws Exception {
        Path filePath = testFolder.resolve("TempCoordiMate.json");
        CoordiMate original = getTypicalCoordiMate();
        JsonCoordiMateStorage jsonCoordiMateStorage = new JsonCoordiMateStorage(filePath);

        // Save in new file and read back
        jsonCoordiMateStorage.saveCoordiMate(original, filePath);
        ReadOnlyCoordiMate readBack = jsonCoordiMateStorage.readCoordiMate(filePath).get();
        assertEquals(original, new CoordiMate(readBack));

        // Modify data, overwrite existing file, and read back
        original.addPerson(HOON);
        original.removePerson(ALICE);
        jsonCoordiMateStorage.saveCoordiMate(original, filePath);
        readBack = jsonCoordiMateStorage.readCoordiMate(filePath).get();
        assertEquals(original, new CoordiMate(readBack));

        // Save and read without specifying file path
        original.addPerson(IDA);
        jsonCoordiMateStorage.saveCoordiMate(original); // file path not specified
        readBack = jsonCoordiMateStorage.readCoordiMate().get(); // file path not specified
        assertEquals(original, new CoordiMate(readBack));

    }

    @Test
    public void saveCoordiMate_nullCoordiMate_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveCoordiMate(null, "SomeFile.json"));
    }

    /**
     * Saves {@code coordiMate} at the specified {@code filePath}.
     */
    private void saveCoordiMate(ReadOnlyCoordiMate coordiMate, String filePath) {
        try {
            new JsonCoordiMateStorage(Paths.get(filePath))
                    .saveCoordiMate(coordiMate, addToTestDataPathIfNotNull(filePath));
        } catch (IOException ioe) {
            throw new AssertionError("There should not be an error writing to the file.", ioe);
        }
    }

    @Test
    public void saveCoordiMate_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveCoordiMate(new CoordiMate(), null));
    }

    private static List<String> listFileNames(Path folder) throws IOException {
        try (Stream<Path> files = Files.list(folder)) {
            return files.map(file -> file.getFileName().toString()).sorted().toList();
        }
    }

    /**
     * Fails while its data is being converted for saving, after the temporary file has been created.
     */
    private static class FailingCoordiMate implements ReadOnlyCoordiMate {

        @Override
        public long getNextMemberId() {
            return 1;
        }

        @Override
        public ObservableList<Person> getPersonList() {
            return FXCollections.observableArrayList(ALICE);
        }

        @Override
        public ObservableList<Tag> getTagList() {
            return FXCollections.observableArrayList();
        }

        @Override
        public ObservableList<Event> getEventList() {
            throw new IllegalStateException("Event data could not be read.");
        }
    }
}
