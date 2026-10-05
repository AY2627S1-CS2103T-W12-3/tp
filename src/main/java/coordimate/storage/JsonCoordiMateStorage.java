package coordimate.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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

    public JsonCoordiMateStorage(Path filePath) {
        this.filePath = filePath;
    }

    public Path getCoordiMateFilePath() {
        return filePath;
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
     * Similar to {@link #readCoordiMate()}.
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
     * @param coordiMate cannot be null.
     * @throws IOException if there was any problem writing to the file.
     */
    public void saveCoordiMate(ReadOnlyCoordiMate coordiMate) throws IOException {
        saveCoordiMate(coordiMate, filePath);
    }

    /**
     * Similar to {@link #saveCoordiMate(ReadOnlyCoordiMate)}.
     *
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
