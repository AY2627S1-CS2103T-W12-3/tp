package coordimate.storage;

import static coordimate.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import coordimate.commons.core.GuiSettings;
import coordimate.commons.exceptions.DataLoadingException;
import coordimate.model.UserPrefs;

public class JsonUserPrefsStorageTest {

    private static final Path TEST_DATA_FOLDER = Paths.get("src", "test", "data", "JsonUserPrefsStorageTest");

    @TempDir
    public Path testFolder;

    @Test
    public void readUserPrefs_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> readUserPrefs(null));
    }

    private Optional<UserPrefs> readUserPrefs(String userPrefsFileInTestDataFolder) throws DataLoadingException {
        Path prefsFilePath = addToTestDataPathIfNotNull(userPrefsFileInTestDataFolder);
        return new JsonUserPrefsStorage(prefsFilePath).readUserPrefs(prefsFilePath);
    }

    @Test
    public void readUserPrefs_missingFile_emptyResult() throws DataLoadingException {
        assertFalse(readUserPrefs("NonExistentFile.json").isPresent());
    }

    @Test
    public void readUserPrefs_notJsonFormat_exceptionThrown() {
        assertThrows(DataLoadingException.class, () -> readUserPrefs("NotJsonFormatUserPrefs.json"));
    }

    private Path addToTestDataPathIfNotNull(String userPrefsFileInTestDataFolder) {
        return userPrefsFileInTestDataFolder != null
                ? TEST_DATA_FOLDER.resolve(userPrefsFileInTestDataFolder)
                : null;
    }

    @Test
    public void readUserPrefs_fileInOrder_successfullyRead() throws DataLoadingException {
        UserPrefs expected = getTypicalUserPrefs();
        UserPrefs actual = readUserPrefs("TypicalUserPref.json").get();
        assertEquals(expected, actual);
        assertFalse(actual.hasSeenTutorial());
    }

    @Test
    public void readUserPrefs_valuesMissingFromFile_defaultValuesUsed() throws DataLoadingException {
        UserPrefs actual = readUserPrefs("EmptyUserPrefs.json").get();
        assertEquals(new UserPrefs(), actual);
    }

    @Test
    public void readUserPrefs_extraValuesInFile_extraValuesIgnored() throws DataLoadingException {
        UserPrefs expected = getTypicalUserPrefs();
        UserPrefs actual = readUserPrefs("ExtraValuesUserPref.json").get();

        assertEquals(expected, actual);
    }

    @Test
    public void readUserPrefs_wrongScalarTypes_rejectsWithFieldPath() throws IOException {
        Path prefsFile = testFolder.resolve("preferences.json");
        String[][] cases = {
            {"{\"guiSettings\":{\"windowMaximized\":17}}", "guiSettings.windowMaximized", "a boolean"},
            {"{\"hasSeenTutorial\":\"false\"}", "hasSeenTutorial", "a boolean"},
            {"{\"guiSettings\":{\"windowWidth\":\"1200\"}}", "guiSettings.windowWidth", "a number"},
            {"{\"guiSettings\":{\"windowCoordinates\":{\"x\":1.5}}}",
                "guiSettings.windowCoordinates.x", "an integer"},
            {"{\"guiSettings\":{\"windowCoordinates\":{\"x\":2147483648}}}",
                "guiSettings.windowCoordinates.x", "an integer"},
            {"{\"guiSettings\":null}", "guiSettings", "an object"},
            {"{\"guiSettings\":{\"windowCoordinates\":17}}", "guiSettings.windowCoordinates", "an object"},
            {"{\"hasSeenTutorial\":null}", "hasSeenTutorial", "a boolean"}
        };
        for (String[] testCase : cases) {
            Files.writeString(prefsFile, testCase[0]);
            JsonUserPrefsStorage storage = new JsonUserPrefsStorage(prefsFile);
            DataLoadingException error = org.junit.jupiter.api.Assertions.assertThrows(DataLoadingException.class,
                    storage::readUserPrefs);
            assertEquals("Invalid JSON type at " + testCase[1] + ": expected " + testCase[2] + ".",
                    error.getCause().getMessage());
        }
    }

    @Test
    public void readUserPrefs_nullWindowCoordinates_usesDefaultPosition() throws Exception {
        Path prefsFile = testFolder.resolve("preferences.json");
        Files.writeString(prefsFile, "{\"guiSettings\":{\"windowCoordinates\":null}}");

        UserPrefs loaded = new JsonUserPrefsStorage(prefsFile).readUserPrefs().orElseThrow();

        assertNull(loaded.getGuiSettings().getWindowCoordinates());
    }

    private UserPrefs getTypicalUserPrefs() {
        UserPrefs userPrefs = new UserPrefs();
        userPrefs.setGuiSettings(new GuiSettings(1000, 500, 300, 100));
        return userPrefs;
    }

    @Test
    public void savePrefs_nullPrefs_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveUserPrefs(null, "SomeFile.json"));
    }

    @Test
    public void saveUserPrefs_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveUserPrefs(new UserPrefs(), null));
    }

    /**
     * Saves {@code userPrefs} at the specified {@code prefsFileInTestDataFolder} filepath.
     */
    private void saveUserPrefs(UserPrefs userPrefs, String prefsFileInTestDataFolder) {
        try {
            new JsonUserPrefsStorage(addToTestDataPathIfNotNull(prefsFileInTestDataFolder))
                    .saveUserPrefs(userPrefs);
        } catch (IOException ioe) {
            throw new AssertionError("There should not be an error writing to the file", ioe);
        }
    }

    @Test
    public void saveUserPrefs_allInOrder_success() throws DataLoadingException, IOException {

        UserPrefs original = new UserPrefs();
        original.setGuiSettings(new GuiSettings(1200, 200, 0, 2, false));

        Path pefsFilePath = testFolder.resolve("TempPrefs.json");
        JsonUserPrefsStorage jsonUserPrefsStorage = new JsonUserPrefsStorage(pefsFilePath);

        //Try writing when the file doesn't exist
        jsonUserPrefsStorage.saveUserPrefs(original);
        UserPrefs readBack = jsonUserPrefsStorage.readUserPrefs().get();
        assertEquals(original, readBack);

        //Try saving when the file exists
        original.setGuiSettings(new GuiSettings(5, 5, 5, 5, true));
        jsonUserPrefsStorage.saveUserPrefs(original);
        readBack = jsonUserPrefsStorage.readUserPrefs().get();
        assertEquals(original, readBack);

        original.setHasSeenTutorial(true);
        jsonUserPrefsStorage.saveUserPrefs(original);
        readBack = jsonUserPrefsStorage.readUserPrefs().get();
        assertEquals(original, readBack);
        assertTrue(readBack.hasSeenTutorial());
    }

    @Test
    public void saveUserPrefs_parentPathIsFile_throwsIoExceptionAndKeepsFile() throws IOException {
        Path notAFolder = testFolder.resolve("config");
        Files.writeString(notAFolder, "not a folder");
        JsonUserPrefsStorage storage = new JsonUserPrefsStorage(notAFolder.resolve("preferences.json"));

        assertThrows(IOException.class, () -> storage.saveUserPrefs(new UserPrefs()));
        assertEquals("not a folder", Files.readString(notAFolder));
    }

    @Test
    public void saveUserPrefs_targetIsFolder_throwsIoExceptionAndKeepsFolder() throws IOException {
        Path folder = Files.createDirectory(testFolder.resolve("preferences.json"));
        Files.writeString(folder.resolve("keep.txt"), "keep");
        JsonUserPrefsStorage storage = new JsonUserPrefsStorage(folder);

        assertThrows(IOException.class, () -> storage.saveUserPrefs(new UserPrefs()));
        assertEquals("keep", Files.readString(folder.resolve("keep.txt")));
    }
}
