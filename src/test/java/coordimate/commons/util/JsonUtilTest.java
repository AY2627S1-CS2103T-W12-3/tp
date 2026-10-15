package coordimate.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import coordimate.commons.exceptions.DataLoadingException;
import coordimate.testutil.SerializableTestClass;
import coordimate.testutil.TestUtil;

/**
 * Tests JSON Read and Write.
 */
public class JsonUtilTest {

    private static final Path SERIALIZATION_FILE = TestUtil.getFilePathInSandboxFolder("serialize.json");

    @TempDir
    public Path testFolder;

    @Test
    public void serializeObjectToJsonFile_noExceptionThrown() throws IOException {
        SerializableTestClass serializableTestClass = new SerializableTestClass();
        serializableTestClass.setTestValues();

        JsonUtil.serializeObjectToJsonFile(SERIALIZATION_FILE, serializableTestClass);

        assertEquals(FileUtil.readFromFile(SERIALIZATION_FILE), SerializableTestClass.JSON_STRING_REPRESENTATION);
    }

    @Test
    public void deserializeObjectFromJsonFile_noExceptionThrown() throws IOException {
        FileUtil.writeToFile(SERIALIZATION_FILE, SerializableTestClass.JSON_STRING_REPRESENTATION);

        SerializableTestClass serializableTestClass = JsonUtil
                .deserializeObjectFromJsonFile(SERIALIZATION_FILE, SerializableTestClass.class);

        assertHasTestValues(serializableTestClass);
    }

    @Test
    public void fromJsonString_validJson_returnsObjectWithValues() throws IOException {
        SerializableTestClass serializableTestClass = JsonUtil.fromJsonString(
                SerializableTestClass.JSON_STRING_REPRESENTATION, SerializableTestClass.class);

        assertHasTestValues(serializableTestClass);
    }

    @Test
    public void toJsonString_thenFromJsonString_preservesValues() throws IOException {
        SerializableTestClass original = new SerializableTestClass();
        original.setTestValues();

        String json = JsonUtil.toJsonString(original);

        assertEquals(SerializableTestClass.JSON_STRING_REPRESENTATION, json);
        assertHasTestValues(JsonUtil.fromJsonString(json, SerializableTestClass.class));
    }

    @Test
    public void readJsonFile_nullPath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> JsonUtil.readJsonFile(null, SerializableTestClass.class));
    }

    @Test
    public void readJsonFile_missingFile_returnsEmpty() throws DataLoadingException {
        Path missingFile = testFolder.resolve("missing.json");

        assertFalse(JsonUtil.readJsonFile(missingFile, SerializableTestClass.class).isPresent());
    }

    @Test
    public void readJsonFile_validFile_returnsObject() throws Exception {
        Path file = writeFile("valid.json", SerializableTestClass.JSON_STRING_REPRESENTATION);

        Optional<SerializableTestClass> result = JsonUtil.readJsonFile(file, SerializableTestClass.class);

        assertTrue(result.isPresent());
        assertHasTestValues(result.get());
    }

    @Test
    public void readJsonFile_malformedJson_throwsDataLoadingException() throws IOException {
        for (String content : new String[] {"", "{", "{\"name\": }", "not json"}) {
            Path file = writeFile("malformed.json", content);
            assertThrows(DataLoadingException.class, () -> JsonUtil.readJsonFile(file, SerializableTestClass.class));
        }
    }

    @Test
    public void readJsonFile_wrongJsonType_throwsDataLoadingException() throws IOException {
        for (String content : new String[] {"[1, 2, 3]", "\"text\"", "42", "{\"listOfLocalDateTimes\": \"text\"}"}) {
            Path file = writeFile("wrongType.json", content);
            assertThrows(DataLoadingException.class, () -> JsonUtil.readJsonFile(file, SerializableTestClass.class));
        }
    }

    @Test
    public void readJsonFile_jsonNull_throwsDataLoadingException() throws IOException {
        Path file = writeFile("null.json", "null");

        DataLoadingException exception = assertThrows(DataLoadingException.class, () ->
                JsonUtil.readJsonFile(file, SerializableTestClass.class));
        assertEquals("JSON file must contain an object.", exception.getCause().getMessage());
    }

    @Test
    public void saveJsonFile_nullArguments_throwsNullPointerException() {
        SerializableTestClass serializableTestClass = new SerializableTestClass();
        Path file = testFolder.resolve("saved.json");

        assertThrows(NullPointerException.class, () -> JsonUtil.saveJsonFile(null, file));
        assertThrows(NullPointerException.class, () -> JsonUtil.saveJsonFile(serializableTestClass, null));
    }

    @Test
    public void saveJsonFile_thenReadJsonFile_preservesValues() throws Exception {
        SerializableTestClass original = new SerializableTestClass();
        original.setTestValues();
        Path file = testFolder.resolve("saved.json");

        JsonUtil.saveJsonFile(original, file);

        assertHasTestValues(JsonUtil.readJsonFile(file, SerializableTestClass.class).orElseThrow());
    }

    /**
     * {@code saveJsonFile} does not create missing folders; callers such as {@code JsonCoordiMateStorage}
     * create them first.
     */
    @Test
    public void saveJsonFile_missingParentFolder_throwsIoException() {
        SerializableTestClass serializableTestClass = new SerializableTestClass();
        Path file = testFolder.resolve("missingFolder").resolve("saved.json");

        assertThrows(IOException.class, () -> JsonUtil.saveJsonFile(serializableTestClass, file));
        assertFalse(Files.exists(file));
    }

    private Path writeFile(String fileName, String content) throws IOException {
        Path file = testFolder.resolve(fileName);
        Files.writeString(file, content);
        return file;
    }

    private static void assertHasTestValues(SerializableTestClass serializableTestClass) {
        assertEquals(SerializableTestClass.getNameTestValue(), serializableTestClass.getName());
        assertEquals(SerializableTestClass.getListTestValues(), serializableTestClass.getListOfLocalDateTimes());
        assertEquals(SerializableTestClass.getHashMapTestValues(), serializableTestClass.getMapOfIntegerToString());
    }
}
