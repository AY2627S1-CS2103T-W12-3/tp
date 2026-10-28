package coordimate.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import coordimate.commons.core.LogsCenter;
import coordimate.commons.exceptions.DataLoadingException;
import coordimate.commons.util.JsonUtil;
import coordimate.model.ReadOnlyCoordiMate;
import coordimate.model.ReadOnlyUserPrefs;
import coordimate.model.UserPrefs;

/**
 * Manages storage of CoordiMate data in local storage.
 */
public class StorageManager implements Storage {

    private static final Logger logger = LogsCenter.getLogger(StorageManager.class);
    private JsonCoordiMateStorage coordiMateStorage;
    private JsonUserPrefsStorage userPrefsStorage;
    private Optional<String> expectedDataRevision = Optional.empty();
    private boolean isDataRevisionInitialized;

    /**
     * Creates a {@code StorageManager} with the given CoordiMate and user prefs storage.
     */
    public StorageManager(JsonCoordiMateStorage coordiMateStorage, JsonUserPrefsStorage userPrefsStorage) {
        this.coordiMateStorage = coordiMateStorage;
        this.userPrefsStorage = userPrefsStorage;
    }

    // ================ UserPrefs methods ==============================

    @Override
    public Path getUserPrefsFilePath() {
        return userPrefsStorage.getUserPrefsFilePath();
    }

    @Override
    public Optional<UserPrefs> readUserPrefs() throws DataLoadingException {
        return userPrefsStorage.readUserPrefs();
    }

    @Override
    public void saveUserPrefs(ReadOnlyUserPrefs userPrefs) throws IOException {
        userPrefsStorage.saveUserPrefs(userPrefs);
    }


    // ================ CoordiMate methods ==============================

    @Override
    public Path getCoordiMateFilePath() {
        return coordiMateStorage.getCoordiMateFilePath();
    }

    /**
     * Removes unusable names, year-zero dates and history, and renames normalization collisions before saving.
     * Leaves the initial model load responsible for capturing its own matching file revision.
     */
    public EventNameCleanupResult cleanUpEventNames() throws DataLoadingException, IOException {
        try (DataFileGuard guard = new DataFileGuard(getCoordiMateFilePath())) {
            Optional<String> originalRevision = dataRevision();
            Optional<JsonCoordiMateStorage.EventNameCleanup> cleanup = coordiMateStorage.readEventNameCleanup();
            if (cleanup.isEmpty()) {
                return new EventNameCleanupResult(List.of(), List.of());
            }
            if (isDataRevisionInitialized && !expectedDataRevision.equals(originalRevision)) {
                throw new DataConflictException();
            }
            Optional<String> savedRevision = saveCoordiMateIfUnchanged(cleanup.get().data(), originalRevision, guard);
            if (isDataRevisionInitialized) {
                expectedDataRevision = savedRevision;
            }
            return cleanup.get().changes();
        }
    }

    @Override
    public Optional<ReadOnlyCoordiMate> readCoordiMate() throws DataLoadingException {
        logger.fine("Attempting to read data from file: " + coordiMateStorage.getCoordiMateFilePath());
        if (isDataRevisionInitialized) {
            return coordiMateStorage.readCoordiMate();
        }
        try {
            // Pair the initial model with the file it came from, even if another window saves during loading.
            for (int attempt = 0; attempt < 3; attempt++) {
                Optional<String> before = dataRevision();
                Optional<ReadOnlyCoordiMate> data;
                try {
                    data = coordiMateStorage.readCoordiMate();
                } catch (DataLoadingException e) {
                    if (before.equals(dataRevision())) {
                        expectedDataRevision = before;
                        isDataRevisionInitialized = true;
                        throw e;
                    }
                    continue;
                }
                Optional<String> after = dataRevision();
                if (before.equals(after)) {
                    expectedDataRevision = after;
                    isDataRevisionInitialized = true;
                    return data;
                }
            }
            throw new DataLoadingException(new IOException("Data file changed while loading."));
        } catch (IOException e) {
            throw new DataLoadingException(e);
        }
    }

    @Override
    public void saveCoordiMate(ReadOnlyCoordiMate coordiMate) throws IOException {
        logger.fine("Attempting to write to data file: " + coordiMateStorage.getCoordiMateFilePath());
        coordiMateStorage.saveCoordiMate(coordiMate);
    }

    @Override
    public void saveCoordiMateIfUnchanged(ReadOnlyCoordiMate coordiMate) throws IOException {
        if (!isDataRevisionInitialized) {
            throw new IOException("Data file has not been loaded.");
        }
        try (DataFileGuard guard = new DataFileGuard(getCoordiMateFilePath())) {
            expectedDataRevision = saveCoordiMateIfUnchanged(coordiMate, expectedDataRevision, guard);
        }
    }

    private Optional<String> saveCoordiMateIfUnchanged(ReadOnlyCoordiMate coordiMate, Optional<String> expectedRevision,
            DataFileGuard guard) throws IOException {
        Optional<String> savedRevision = Optional.of(DataFileGuard.revisionOf(
                JsonUtil.toJsonString(new JsonSerializableCoordiMate(coordiMate))
                        .getBytes(StandardCharsets.UTF_8)));
        guard.checkRevision(expectedRevision);
        coordiMateStorage.saveCoordiMate(coordiMate);
        return savedRevision;
    }

    private Optional<String> dataRevision() throws IOException {
        return DataFileGuard.readRevision(getCoordiMateFilePath());
    }

}
