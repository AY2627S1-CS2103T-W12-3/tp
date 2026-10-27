package coordimate.storage;

import static coordimate.testutil.TypicalPersons.getTypicalCoordiMate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import coordimate.commons.core.GuiSettings;
import coordimate.commons.exceptions.DataLoadingException;
import coordimate.model.CoordiMate;
import coordimate.model.ReadOnlyCoordiMate;
import coordimate.model.UserPrefs;

public class StorageManagerTest {

    @TempDir
    public Path testFolder;

    private StorageManager storageManager;

    @BeforeEach
    public void setUp() {
        JsonCoordiMateStorage coordiMateStorage = new JsonCoordiMateStorage(getTempFilePath("coordimate"));
        JsonUserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(getTempFilePath("prefs"));
        storageManager = new StorageManager(coordiMateStorage, userPrefsStorage);
    }

    private Path getTempFilePath(String fileName) {
        return testFolder.resolve(fileName);
    }

    @Test
    public void prefsReadSave() throws Exception {
        /*
         * Note: This is an integration test that verifies the StorageManager is properly wired to the
         * {@link JsonUserPrefsStorage} class.
         * More extensive testing of UserPref saving/reading is done in {@link JsonUserPrefsStorageTest} class.
         */
        UserPrefs original = new UserPrefs();
        original.setGuiSettings(new GuiSettings(300, 600, 4, 6));
        storageManager.saveUserPrefs(original);
        UserPrefs retrieved = storageManager.readUserPrefs().get();
        assertEquals(original, retrieved);
    }

    @Test
    public void coordiMateReadSave() throws Exception {
        /*
         * Note: This is an integration test that verifies the StorageManager is properly wired to the
         * {@link JsonCoordiMateStorage} class.
         * More extensive testing of UserPref saving/reading is done in {@link JsonCoordiMateStorageTest} class.
         */
        CoordiMate original = getTypicalCoordiMate();
        storageManager.saveCoordiMate(original);
        ReadOnlyCoordiMate retrieved = storageManager.readCoordiMate().get();
        assertEquals(original, new CoordiMate(retrieved));
    }

    @Test
    public void initialRead_fileChangesWhileLoading_usesLatestRevision() throws Exception {
        Path path = getTempFilePath("changing.json");
        new JsonCoordiMateStorage(path).saveCoordiMate(new CoordiMate());
        JsonCoordiMateStorage changingStorage = new JsonCoordiMateStorage(path) {
            private boolean isFirstRead = true;

            @Override
            public Optional<ReadOnlyCoordiMate> readCoordiMate() throws DataLoadingException {
                if (isFirstRead) {
                    isFirstRead = false;
                    try {
                        saveCoordiMate(getTypicalCoordiMate());
                    } catch (IOException e) {
                        throw new DataLoadingException(e);
                    }
                }
                return super.readCoordiMate();
            }
        };
        StorageManager storage = new StorageManager(changingStorage,
                new JsonUserPrefsStorage(getTempFilePath("prefs")));

        ReadOnlyCoordiMate loaded = storage.readCoordiMate().orElseThrow();
        assertEquals(getTypicalCoordiMate(), loaded);
        storage.saveCoordiMateIfUnchanged(loaded);
        assertEquals(loaded, new JsonCoordiMateStorage(path).readCoordiMate().orElseThrow());
    }

    @Test
    public void initialRead_loadFailsAsFileChanges_retriesUpdatedFile() throws Exception {
        Path path = getTempFilePath("repaired.json");
        new JsonCoordiMateStorage(path).saveCoordiMate(new CoordiMate());
        JsonCoordiMateStorage changingStorage = new JsonCoordiMateStorage(path) {
            private boolean isFirstRead = true;

            @Override
            public Optional<ReadOnlyCoordiMate> readCoordiMate() throws DataLoadingException {
                if (isFirstRead) {
                    isFirstRead = false;
                    try {
                        saveCoordiMate(getTypicalCoordiMate());
                    } catch (IOException e) {
                        throw new DataLoadingException(e);
                    }
                    throw new DataLoadingException(new IOException("The old file could not be read."));
                }
                return super.readCoordiMate();
            }
        };
        StorageManager storage = new StorageManager(changingStorage,
                new JsonUserPrefsStorage(getTempFilePath("prefs")));

        ReadOnlyCoordiMate loaded = storage.readCoordiMate().orElseThrow();
        assertEquals(getTypicalCoordiMate(), loaded);
        storage.saveCoordiMateIfUnchanged(loaded);
    }

    @Test
    public void initialRead_fileKeepsChanging_rejectsInconsistentSnapshot() throws Exception {
        Path path = getTempFilePath("unstable.json");
        new JsonCoordiMateStorage(path).saveCoordiMate(new CoordiMate());
        JsonCoordiMateStorage changingStorage = new JsonCoordiMateStorage(path) {
            @Override
            public Optional<ReadOnlyCoordiMate> readCoordiMate() throws DataLoadingException {
                try {
                    Files.writeString(path, "\n", StandardOpenOption.APPEND);
                } catch (IOException e) {
                    throw new DataLoadingException(e);
                }
                return super.readCoordiMate();
            }
        };
        StorageManager storage = new StorageManager(changingStorage,
                new JsonUserPrefsStorage(getTempFilePath("prefs")));

        DataLoadingException error = assertThrows(DataLoadingException.class, storage::readCoordiMate);
        assertEquals("Data file changed while loading.", error.getCause().getMessage());
        assertEquals(new CoordiMate(), new JsonCoordiMateStorage(path).readCoordiMate().orElseThrow());
    }

    @Test
    public void conditionalSave_beforeInitialRead_rejectsWrite() throws Exception {
        Path path = getTempFilePath("unloaded.json");
        StorageManager storage = new StorageManager(new JsonCoordiMateStorage(path),
                new JsonUserPrefsStorage(getTempFilePath("prefs")));

        IOException error = assertThrows(IOException.class, () ->
                storage.saveCoordiMateIfUnchanged(getTypicalCoordiMate()));
        assertEquals("Data file has not been loaded.", error.getMessage());
        assertFalse(Files.exists(path));
    }

    @Test
    public void initialRead_dataPathIsDirectory_reportsLoadingFailure() throws Exception {
        Path path = getTempFilePath("directory.json");
        Files.createDirectory(path);
        StorageManager storage = new StorageManager(new JsonCoordiMateStorage(path),
                new JsonUserPrefsStorage(getTempFilePath("prefs")));

        DataLoadingException error = assertThrows(DataLoadingException.class, storage::readCoordiMate);
        assertInstanceOf(IOException.class, error.getCause());
        assertTrue(Files.isDirectory(path));
    }

    @Test
    public void conditionalSave_whileLockIsHeld_leavesFileUnchanged() throws Exception {
        Path path = getTempFilePath("locked.json");
        JsonCoordiMateStorage jsonStorage = new JsonCoordiMateStorage(path);
        jsonStorage.saveCoordiMate(new CoordiMate());
        StorageManager storage = new StorageManager(jsonStorage,
                new JsonUserPrefsStorage(getTempFilePath("prefs")));
        storage.readCoordiMate();
        Path lockPath = path.resolveSibling(path.getFileName() + ".lock");

        try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                FileLock ignored = channel.lock()) {
            assertThrows(IOException.class, () -> storage.saveCoordiMateIfUnchanged(getTypicalCoordiMate()));
        }
        assertEquals(new CoordiMate(), jsonStorage.readCoordiMate().orElseThrow());
    }

    @Test
    public void getCoordiMateFilePath() {
        assertNotNull(storageManager.getCoordiMateFilePath());
    }

    @Test
    public void getUserPrefsFilePath() {
        assertNotNull(storageManager.getUserPrefsFilePath());
    }

}
