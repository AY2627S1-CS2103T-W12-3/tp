package coordimate.testutil;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import coordimate.model.CoordiMate;
import coordimate.model.Model;
import coordimate.model.ReadOnlyCoordiMate;
import coordimate.model.event.Event;
import coordimate.model.person.Person;
import coordimate.storage.JsonCoordiMateStorage;

/**
 * Supplies save failures and independent before-state snapshots for persistence tests.
 */
public final class PersistenceTestUtil {
    private PersistenceTestUtil() {}

    /**
     * Returns storage that reads normally but throws the supplied exception at the normal save boundary.
     */
    public static JsonCoordiMateStorage failingStorage(Path path, IOException failure) {
        return new JsonCoordiMateStorage(path) {
            @Override
            public void saveCoordiMate(ReadOnlyCoordiMate data) throws IOException {
                throw failure;
            }
        };
    }

    /**
     * Copies saved data, both displayed lists, archive-view state, and the file's exact bytes before execution.
     */
    public static SavedState captureState(Model model, Path dataPath) throws IOException {
        return new SavedState(model, dataPath);
    }

    /**
     * Holds private copies of the state so later mutations cannot change the expected values.
     */
    public static final class SavedState {
        private final CoordiMate data;
        private final List<Person> displayedContacts;
        private final List<Event> displayedEvents;
        private final boolean wasShowingArchived;
        private final Path dataPath;
        private final byte[] fileContents;

        private SavedState(Model model, Path dataPath) throws IOException {
            data = new CoordiMate(model.getCoordiMate());
            displayedContacts = List.copyOf(model.getFilteredPersonList());
            displayedEvents = List.copyOf(model.getFilteredEventList());
            wasShowingArchived = model.isShowingArchived();
            this.dataPath = dataPath;
            fileContents = Files.readAllBytes(dataPath);
        }

        /**
         * Checks that the saved data, displayed lists, archive view, and original file bytes remain unchanged.
         */
        public void assertUnchanged(Model model) throws IOException {
            assertEquals(data, model.getCoordiMate(), "Saved model data changed");
            assertEquals(displayedContacts, model.getFilteredPersonList(), "Displayed contacts changed");
            assertEquals(displayedEvents, model.getFilteredEventList(), "Displayed events changed");
            assertEquals(wasShowingArchived, model.isShowingArchived(), "Archive view changed");
            assertArrayEquals(fileContents, Files.readAllBytes(dataPath), "Saved file bytes changed");
        }
    }
}
