package coordimate.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import coordimate.commons.core.LogsCenter;
import coordimate.commons.exceptions.DataLoadingException;
import coordimate.commons.exceptions.IllegalValueException;
import coordimate.commons.util.JsonUtil;
import coordimate.model.ReadOnlyCoordiMate;

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
     * Prepares startup cleanup without writing the file. Unrelated invalid data prevents cleanup.
     */
    Optional<EventNameCleanup> readEventNameCleanup() throws DataLoadingException {
        Optional<JsonSerializableCoordiMate> savedData = JsonUtil.readJsonFile(
                filePath, JsonSerializableCoordiMate.class);
        if (savedData.isEmpty()) {
            return Optional.empty();
        }
        try {
            List<String> yearZeroDeletedNames = savedData.get().removeYearZeroEvents();
            List<String> deletedNames = savedData.get().removeEventsWithEmbeddedParameters();
            boolean removedHistory = savedData.get().removeYearZeroReminderStarts();
            List<EventNameCleanupResult.Rename> renamedEvents = savedData.get().renameEventsWithCollidingNames();
            EventNameCleanupResult changes = new EventNameCleanupResult(
                    deletedNames, renamedEvents, yearZeroDeletedNames);
            if (changes.isEmpty() && !removedHistory) {
                return Optional.empty();
            }
            return Optional.of(new EventNameCleanup(savedData.get().toModelType(), changes));
        } catch (IllegalValueException | IllegalArgumentException | NullPointerException e) {
            throw new DataLoadingException(e);
        }
    }

    record EventNameCleanup(ReadOnlyCoordiMate data, EventNameCleanupResult changes) {}

    /**
     * Deletes events with year-zero dates and removes year-zero reminder history at startup,
     * after validating all remaining data.
     * Returns the deleted names only after the cleaned file is saved successfully.
     */
    public List<String> removeYearZeroEvents() throws DataLoadingException, IOException {
        Optional<JsonSerializableCoordiMate> savedData = JsonUtil.readJsonFile(
                filePath, JsonSerializableCoordiMate.class);
        if (savedData.isEmpty()) {
            return List.of();
        }
        List<String> removedNames = savedData.get().removeYearZeroEvents();
        boolean removedHistory = savedData.get().removeYearZeroReminderStarts();
        if (removedNames.isEmpty() && !removedHistory) {
            return List.of();
        }
        ReadOnlyCoordiMate cleanedData;
        try {
            cleanedData = savedData.get().toModelType();
        } catch (IllegalValueException | IllegalArgumentException | NullPointerException e) {
            throw new DataLoadingException(e);
        }
        saveCoordiMate(cleanedData);
        return List.copyOf(removedNames);
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
