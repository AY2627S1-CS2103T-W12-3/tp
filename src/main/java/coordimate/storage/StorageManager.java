package coordimate.storage;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
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
     * Deletes legacy events with embedded parameters at startup, validating the remaining data before saving.
     * Leaves the initial model load responsible for capturing its own matching file revision.
     */
    public List<String> removeEventsWithEmbeddedParameters() throws DataLoadingException, IOException {
        Optional<String> originalRevision = dataRevision();
        Optional<JsonCoordiMateStorage.EventNameCleanup> cleanup = coordiMateStorage.readEventNameCleanup();
        if (cleanup.isEmpty()) {
            return List.of();
        }
        if (isDataRevisionInitialized && !expectedDataRevision.equals(originalRevision)) {
            throw new DataConflictException();
        }
        Optional<String> savedRevision = saveCoordiMateIfUnchanged(cleanup.get().data(), originalRevision);
        if (isDataRevisionInitialized) {
            expectedDataRevision = savedRevision;
        }
        return cleanup.get().deletedNames();
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
        expectedDataRevision = saveCoordiMateIfUnchanged(coordiMate, expectedDataRevision);
    }

    private Optional<String> saveCoordiMateIfUnchanged(ReadOnlyCoordiMate coordiMate, Optional<String> expectedRevision)
            throws IOException {
        Path dataPath = getCoordiMateFilePath().toAbsolutePath();
        Files.createDirectories(dataPath.getParent());
        Path lockPath = dataPath.resolveSibling(dataPath.getFileName() + ".lock");
        try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                FileLock ignored = channel.lock()) {
            if (!expectedRevision.equals(dataRevision())) {
                throw new DataConflictException();
            }
            Optional<String> savedRevision = Optional.of(revisionOf(
                    JsonUtil.toJsonString(new JsonSerializableCoordiMate(coordiMate))
                            .getBytes(StandardCharsets.UTF_8)));
            coordiMateStorage.saveCoordiMate(coordiMate);
            return savedRevision;
        } catch (OverlappingFileLockException e) {
            throw new IOException("The data file is being saved by another window. Try again.", e);
        }
    }

    private Optional<String> dataRevision() throws IOException {
        try {
            return Optional.of(revisionOf(Files.readAllBytes(getCoordiMateFilePath())));
        } catch (NoSuchFileException e) {
            return Optional.empty();
        }
    }

    private static String revisionOf(byte[] contents) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(contents));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 must be available", e);
        }
    }

}
