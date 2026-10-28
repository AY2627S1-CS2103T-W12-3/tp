package coordimate.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import coordimate.commons.core.LogsCenter;
import coordimate.commons.exceptions.DataLoadingException;
import coordimate.commons.exceptions.IllegalValueException;
import coordimate.commons.util.JsonUtil;
import coordimate.model.CoordiMate;
import coordimate.model.ReadOnlyCoordiMate;
import coordimate.model.event.AttendanceStatus;
import coordimate.model.event.Event;
import coordimate.model.person.Name;
import coordimate.model.person.Person;

/**
 * A class to access CoordiMate data stored as a JSON file on the hard disk.
 */
public class JsonCoordiMateStorage {

    private static final Logger logger = LogsCenter.getLogger(JsonCoordiMateStorage.class);

    private Path filePath;

    /**
     * Creates JSON storage for contacts and events at the given file path.
     */
    public JsonCoordiMateStorage(Path filePath) {
        this.filePath = filePath;
    }

    public Path getCoordiMateFilePath() {
        return filePath;
    }

    /**
     * Repairs legacy event dates and assignments at startup after validating the remaining data.
     * Returns the recovery details only after all repairs are saved successfully.
     */
    public RecoveryResult recoverSavedData() throws DataLoadingException, IOException {
        byte[] originalContents;
        JsonSerializableCoordiMate savedData;
        try {
            originalContents = Files.readAllBytes(filePath);
            savedData = JsonUtil.fromJsonString(new String(originalContents, StandardCharsets.UTF_8),
                    JsonSerializableCoordiMate.class);
            if (savedData == null) {
                throw new IOException("JSON file must contain an object.");
            }
        } catch (NoSuchFileException e) {
            return new RecoveryResult(List.of(), List.of());
        } catch (IOException e) {
            throw new DataLoadingException(e);
        }
        List<String> removedNames = savedData.removeYearZeroEvents();
        boolean removedHistory = savedData.removeYearZeroReminderStarts();
        CoordiMate cleanedData;
        try {
            cleanedData = savedData.toModelType();
        } catch (IllegalValueException | IllegalArgumentException | NullPointerException e) {
            throw new DataLoadingException(e);
        }
        List<String> removedAssignments = removeAssignmentsCoveredByLeave(cleanedData);
        if (!removedNames.isEmpty() || removedHistory || !removedAssignments.isEmpty()) {
            saveRecoveredData(cleanedData, originalContents);
        }
        return new RecoveryResult(removedNames, removedAssignments);
    }

    private void saveRecoveredData(ReadOnlyCoordiMate data, byte[] originalContents) throws IOException {
        Path dataPath = getCoordiMateFilePath().toAbsolutePath();
        Path lockPath = dataPath.resolveSibling(dataPath.getFileName() + ".lock");
        try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                FileLock ignored = channel.lock()) {
            if (!Arrays.equals(originalContents, Files.readAllBytes(dataPath))) {
                throw new DataConflictException();
            }
            saveCoordiMate(data);
        } catch (OverlappingFileLockException e) {
            throw new IOException("The data file is being saved by another window. Try again.", e);
        }
    }

    private List<String> removeAssignmentsCoveredByLeave(CoordiMate data) {
        Map<Name, Person> contacts = data.getPersonList().stream()
                .collect(Collectors.toMap(Person::getName, person -> person));
        List<String> removedAssignments = new ArrayList<>();
        for (Event event : List.copyOf(data.getEventList())) {
            List<Name> unavailable = event.getMembers().stream()
                    .filter(name -> contacts.get(name).isOnLeave(event.getEndTime().getDate()))
                    .toList();
            if (unavailable.isEmpty()) {
                continue;
            }
            List<Name> remainingMembers = event.getMembers().stream()
                    .filter(name -> !unavailable.contains(name)).toList();
            Map<Name, AttendanceStatus> attendance = new HashMap<>(event.getAttendanceRecord());
            attendance.keySet().removeAll(unavailable);
            data.setEvent(event, event.withMembersAndAttendance(remainingMembers, attendance));
            removedAssignments.add(event.getName() + ": "
                    + unavailable.stream().map(Name::toString).collect(Collectors.joining(", ")));
        }
        return removedAssignments;
    }

    /**
     * Describes saved startup repairs and supplies the visible recovery warning.
     */
    public record RecoveryResult(List<String> deletedEvents, List<String> removedAssignments) {
        /**
         * Copies recovery details so the warning cannot change after the repaired data is saved.
         */
        public RecoveryResult {
            deletedEvents = List.copyOf(deletedEvents);
            removedAssignments = List.copyOf(removedAssignments);
        }

        /**
         * Returns the startup warning, or an empty string if there are no events or assignments to report.
         */
        public String getWarning() {
            List<String> warnings = new ArrayList<>();
            if (!deletedEvents.isEmpty()) {
                warnings.add("Deleted the following events because their saved dates use year 0000, "
                        + "which does not exist in the BC/AD calendar:\n\n" + String.join("\n", deletedEvents));
            }
            if (!removedAssignments.isEmpty()) {
                warnings.add("Removed the following event assignments because the members are on leave for the "
                        + "entire event. Any associated attendance records were also removed:\n\n"
                        + String.join("\n", removedAssignments));
            }
            return String.join("\n\n", warnings);
        }
    }

    /**
     * Returns CoordiMate data as a {@link ReadOnlyCoordiMate}.
     * Returns {@code Optional.empty()} if storage file is not found.
     *
     * @throws DataLoadingException if loading the data from storage failed.
     */
    public Optional<ReadOnlyCoordiMate> readCoordiMate() throws DataLoadingException {
        return readCoordiMate(filePath);
    }

    /**
     * Returns CoordiMate data from the given file, or an empty optional if it does not exist.
     *
     * @param filePath location of the data. Cannot be null.
     * @throws DataLoadingException if loading the data from storage failed.
     */
    public Optional<ReadOnlyCoordiMate> readCoordiMate(Path filePath) throws DataLoadingException {
        requireNonNull(filePath);

        Optional<JsonSerializableCoordiMate> jsonCoordiMate = JsonUtil.readJsonFile(
                filePath, JsonSerializableCoordiMate.class);
        if (!jsonCoordiMate.isPresent()) {
            return Optional.empty();
        }

        try {
            return Optional.of(jsonCoordiMate.get().toModelType());
        } catch (IllegalValueException | IllegalArgumentException | NullPointerException ive) {
            logger.info("Illegal values found in " + filePath + ": " + ive.getMessage());
            throw new DataLoadingException(ive);
        }
    }

    /**
     * Saves the given {@link ReadOnlyCoordiMate} to the storage.
     *
     * @param coordiMate cannot be null.
     * @throws IOException if there was any problem writing to the file.
     */
    public void saveCoordiMate(ReadOnlyCoordiMate coordiMate) throws IOException {
        saveCoordiMate(coordiMate, filePath);
    }

    /**
     * Saves the given data to the specified file by atomically replacing its contents.
     *
     * @param coordiMate The contact and event data to save. Cannot be null.
     * @param filePath location of the data. Cannot be null.
     */
    public void saveCoordiMate(ReadOnlyCoordiMate coordiMate, Path filePath) throws IOException {
        requireNonNull(coordiMate);
        requireNonNull(filePath);

        Path target = filePath.toAbsolutePath();
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), "coordimate-", ".tmp");
        try {
            JsonUtil.saveJsonFile(new JsonSerializableCoordiMate(coordiMate), temporary);
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

}
