package coordimate.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.CoordiMate;
import coordimate.model.Model;
import coordimate.model.ModelManager;
import coordimate.model.ReadOnlyCoordiMate;
import coordimate.model.person.Person;
import coordimate.storage.JsonCoordiMateStorage;
import coordimate.storage.JsonUserPrefsStorage;
import coordimate.storage.StorageManager;

/**
 * Shared test-only fixtures for exercising a failing save through the real atomic-save path
 * ({@link LogicManager} and {@link StorageManager}), used by the persistence/integration test suites.
 */
public final class PersistenceTestUtil {

    private PersistenceTestUtil() {}

    /** Returns a {@code JsonCoordiMateStorage} whose save always throws {@code failure}. */
    public static JsonCoordiMateStorage failingStorage(Path path, IOException failure) {
        return new JsonCoordiMateStorage(path) {
            @Override
            public void saveCoordiMate(ReadOnlyCoordiMate data) throws IOException {
                throw failure;
            }
        };
    }

    /** Builds a {@code Logic} over {@code storage}, with user prefs stored under {@code temporaryFolder}. */
    public static Logic newLogic(ModelManager model, JsonCoordiMateStorage storage, Path temporaryFolder) {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));
    }

    /** An immutable snapshot of a saved CoordiMate and its file content, captured before a command runs. */
    public record BeforeState(CoordiMate model, Path path, String fileContent) {
        /** Captures {@code model}'s current state and the file content at {@code path}. */
        public static BeforeState capture(ModelManager model, Path path) throws IOException {
            return new BeforeState(new CoordiMate(model.getCoordiMate()), path, Files.readString(path));
        }
    }

    /** Writes {@code model} to {@code path} via a real storage, then captures the resulting before-state. */
    public static BeforeState seedThenCapture(ModelManager model, Path path) throws IOException {
        new JsonCoordiMateStorage(path).saveCoordiMate(model.getCoordiMate());
        return BeforeState.capture(model, path);
    }

    /** Returns an independent copy of {@code model}'s currently displayed contacts. */
    public static List<Person> captureDisplayedList(Model model) {
        return List.copyOf(model.getFilteredPersonList());
    }

    /**
     * Asserts the common save-failure triad: {@code cause} caused {@code exception}, and {@code model} and
     * the file at {@code before}'s path are exactly as they were before the failing save was attempted.
     */
    public static void assertSaveFailurePreservedState(CommandException exception, IOException cause,
            ModelManager model, BeforeState before) throws IOException {
        assertSame(cause, exception.getCause());
        assertEquals(before.model(), model.getCoordiMate());
        assertEquals(before.fileContent(), Files.readString(before.path()));
    }
}
