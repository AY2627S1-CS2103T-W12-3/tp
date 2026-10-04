package coordimate.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

import coordimate.commons.core.LogsCenter;
import coordimate.commons.exceptions.DataLoadingException;
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

    @Override
    public Optional<ReadOnlyCoordiMate> readCoordiMate() throws DataLoadingException {
        logger.fine("Attempting to read data from file: " + coordiMateStorage.getCoordiMateFilePath());
        return coordiMateStorage.readCoordiMate();
    }

    @Override
    public void saveCoordiMate(ReadOnlyCoordiMate coordiMate) throws IOException {
        logger.fine("Attempting to write to data file: " + coordiMateStorage.getCoordiMateFilePath());
        coordiMateStorage.saveCoordiMate(coordiMate);
    }

}
