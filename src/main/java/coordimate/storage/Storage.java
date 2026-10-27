package coordimate.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import coordimate.commons.exceptions.DataLoadingException;
import coordimate.model.ReadOnlyCoordiMate;
import coordimate.model.ReadOnlyUserPrefs;
import coordimate.model.UserPrefs;

/**
 * API of the Storage component.
 */
public interface Storage {

    /**
     * Returns the file path of the UserPrefs data file.
     */
    Path getUserPrefsFilePath();

    /**
     * Returns UserPrefs data from storage.
     * Returns {@code Optional.empty()} if storage file is not found.
     *
     * @throws DataLoadingException if the loading of data from preference file failed.
     */
    Optional<UserPrefs> readUserPrefs() throws DataLoadingException;

    /**
     * Saves the given {@link coordimate.model.ReadOnlyUserPrefs} to the storage.
     *
     * @param userPrefs cannot be null.
     * @throws IOException if there was any problem writing to the file.
     */
    void saveUserPrefs(ReadOnlyUserPrefs userPrefs) throws IOException;

    /**
     * Returns the file path of the CoordiMate data file.
     */
    Path getCoordiMateFilePath();

    /**
     * Returns CoordiMate data as a {@link ReadOnlyCoordiMate}.
     * Returns {@code Optional.empty()} if storage file is not found.
     *
     * @throws DataLoadingException if loading the data from storage failed.
     */
    Optional<ReadOnlyCoordiMate> readCoordiMate() throws DataLoadingException;

    /**
     * Saves the given {@link ReadOnlyCoordiMate} to the storage.
     *
     * @param coordiMate cannot be null.
     * @throws IOException if there was any problem writing to the file.
     */
    void saveCoordiMate(ReadOnlyCoordiMate coordiMate) throws IOException;

    /**
     * Saves data only if the file still matches the version this storage instance loaded or last saved.
     *
     * @throws DataConflictException if the file has changed.
     * @throws IOException if the file cannot be checked or saved.
     */
    void saveCoordiMateIfUnchanged(ReadOnlyCoordiMate coordiMate) throws IOException;

}
