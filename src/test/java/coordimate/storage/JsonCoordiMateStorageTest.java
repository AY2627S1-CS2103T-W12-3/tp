package coordimate.storage;

import static coordimate.testutil.Assert.assertThrows;
import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.HOON;
import static coordimate.testutil.TypicalPersons.IDA;
import static coordimate.testutil.TypicalPersons.getTypicalCoordiMate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import coordimate.commons.exceptions.DataLoadingException;
import coordimate.model.CoordiMate;
import coordimate.model.ReadOnlyCoordiMate;

public class JsonCoordiMateStorageTest {
    private static final Path TEST_DATA_FOLDER = Paths.get("src", "test", "data", "JsonCoordiMateStorageTest");

    @TempDir
    public Path testFolder;

    @Test
    public void readCoordiMate_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> readCoordiMate(null));
    }

    private java.util.Optional<ReadOnlyCoordiMate> readCoordiMate(String filePath) throws Exception {
        return new JsonCoordiMateStorage(Paths.get(filePath)).readCoordiMate(addToTestDataPathIfNotNull(filePath));
    }

    private Path addToTestDataPathIfNotNull(String prefsFileInTestDataFolder) {
        return prefsFileInTestDataFolder != null
                ? TEST_DATA_FOLDER.resolve(prefsFileInTestDataFolder)
                : null;
    }

    @Test
    public void read_missingFile_emptyResult() throws Exception {
        assertFalse(readCoordiMate("NonExistentFile.json").isPresent());
    }

    @Test
    public void read_notJsonFormat_exceptionThrown() {
        assertThrows(DataLoadingException.class, () -> readCoordiMate("notJsonFormatCoordiMate.json"));
    }

    @Test
    public void readCoordiMate_invalidPersonCoordiMate_throwDataLoadingException() {
        assertThrows(DataLoadingException.class, () -> readCoordiMate("invalidPersonCoordiMate.json"));
    }

    @Test
    public void readCoordiMate_invalidAndValidPersonCoordiMate_throwDataLoadingException() {
        assertThrows(DataLoadingException.class, () -> readCoordiMate("invalidAndValidPersonCoordiMate.json"));
    }

    @Test
    public void readAndSaveCoordiMate_allInOrder_success() throws Exception {
        Path filePath = testFolder.resolve("TempCoordiMate.json");
        CoordiMate original = getTypicalCoordiMate();
        JsonCoordiMateStorage jsonCoordiMateStorage = new JsonCoordiMateStorage(filePath);

        // Save in new file and read back
        jsonCoordiMateStorage.saveCoordiMate(original, filePath);
        ReadOnlyCoordiMate readBack = jsonCoordiMateStorage.readCoordiMate(filePath).get();
        assertEquals(original, new CoordiMate(readBack));

        // Modify data, overwrite existing file, and read back
        original.addPerson(HOON);
        original.removePerson(ALICE);
        jsonCoordiMateStorage.saveCoordiMate(original, filePath);
        readBack = jsonCoordiMateStorage.readCoordiMate(filePath).get();
        assertEquals(original, new CoordiMate(readBack));

        // Save and read without specifying file path
        original.addPerson(IDA);
        jsonCoordiMateStorage.saveCoordiMate(original); // file path not specified
        readBack = jsonCoordiMateStorage.readCoordiMate().get(); // file path not specified
        assertEquals(original, new CoordiMate(readBack));

    }

    @Test
    public void saveCoordiMate_nullCoordiMate_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveCoordiMate(null, "SomeFile.json"));
    }

    /**
     * Saves {@code coordiMate} at the specified {@code filePath}.
     */
    private void saveCoordiMate(ReadOnlyCoordiMate coordiMate, String filePath) {
        try {
            new JsonCoordiMateStorage(Paths.get(filePath))
                    .saveCoordiMate(coordiMate, addToTestDataPathIfNotNull(filePath));
        } catch (IOException ioe) {
            throw new AssertionError("There should not be an error writing to the file.", ioe);
        }
    }

    @Test
    public void saveCoordiMate_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveCoordiMate(new CoordiMate(), null));
    }
}
