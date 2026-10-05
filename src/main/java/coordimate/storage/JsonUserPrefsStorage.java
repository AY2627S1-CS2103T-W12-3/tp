package coordimate.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import coordimate.commons.exceptions.DataLoadingException;
import coordimate.commons.util.JsonUtil;
import coordimate.model.ReadOnlyUserPrefs;
import coordimate.model.UserPrefs;

/**
 * A class to access UserPrefs stored on the hard disk as a JSON file.
 */
public class JsonUserPrefsStorage {

    private Path filePath;

    /**
     * Creates JSON storage for user preferences at the given file path.
     */
    public JsonUserPrefsStorage(Path filePath) {
        this.filePath = filePath;
    }

    public Path getUserPrefsFilePath() {
        return filePath;
    }

    /**
     * Returns UserPrefs data from storage.
     * Returns {@code Optional.empty()} if storage file is not found.
     *
     * @throws DataLoadingException if the loading of data from preference file failed.
     */
    public Optional<UserPrefs> readUserPrefs() throws DataLoadingException {
        return readUserPrefs(filePath);
    }

    /**
     * Returns preferences from the given file, or an empty optional if it does not exist.
     *
     * @param prefsFilePath location of the data. Cannot be null.
     * @throws DataLoadingException if the file format is not as expected.
     */
    public Optional<UserPrefs> readUserPrefs(Path prefsFilePath) throws DataLoadingException {
        return JsonUtil.readJsonFile(prefsFilePath, UserPrefs.class);
    }

    /**
     * Saves the given {@link coordimate.model.ReadOnlyUserPrefs} to the storage.
     *
     * @param userPrefs cannot be null.
     * @throws IOException if there was any problem writing to the file.
     */
    public void saveUserPrefs(ReadOnlyUserPrefs userPrefs) throws IOException {
        JsonUtil.saveJsonFile(userPrefs, filePath);
    }

}
