package coordimate.testutil;

import static coordimate.testutil.PersistenceTestUtil.captureState;
import static coordimate.testutil.PersistenceTestUtil.failingStorage;
import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BENSON;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import coordimate.model.ModelManager;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.tag.Tag;
import coordimate.storage.JsonCoordiMateStorage;
import coordimate.testutil.PersistenceTestUtil.SavedState;

public class PersistenceTestUtilTest {
    @TempDir
    public Path testFolder;

    private ModelManager model;
    private Path dataPath;

    @BeforeEach
    public void setUp() throws IOException {
        model = new ModelManager();
        model.addPerson(ALICE);
        model.addPerson(BENSON);
        EventTime date = new EventTime("29-10-2026");
        model.addEvent(new Event("Concert", date, date, List.of(ALICE.getName())));
        dataPath = testFolder.resolve("data.json");
        new JsonCoordiMateStorage(dataPath).saveCoordiMate(model.getCoordiMate());
    }

    @Test
    public void captureState_unchangedState_passes() throws IOException {
        captureState(model, dataPath).assertUnchanged(model);
    }

    @Test
    public void captureState_tagAddedAfterCapture_rejectsChangedModel() throws IOException {
        SavedState before = captureState(model, dataPath);
        model.addTag(new Tag("Publicity"));

        assertThrows(AssertionError.class, () -> before.assertUnchanged(model));
    }

    @Test
    public void captureState_filterChangedAfterCapture_rejectsChangedDisplayedList() throws IOException {
        model.updateFilteredPersonList(ALICE::equals);
        SavedState before = captureState(model, dataPath);
        model.updateFilteredPersonList(BENSON::equals);

        assertThrows(AssertionError.class, () -> before.assertUnchanged(model));
    }

    @Test
    public void captureState_whitespaceAddedToFile_rejectsChangedBytes() throws IOException {
        SavedState before = captureState(model, dataPath);
        Files.writeString(dataPath, Files.readString(dataPath) + "\n");

        assertThrows(AssertionError.class, () -> before.assertUnchanged(model));
    }

    @Test
    public void failingStorage_saveFailure_preservesExceptionIdentityAndState() throws IOException {
        SavedState before = captureState(model, dataPath);
        for (IOException failure : List.of(new IOException("Disk full"), new AccessDeniedException("data"))) {
            JsonCoordiMateStorage storage = failingStorage(dataPath, failure);

            assertSame(failure, assertThrows(IOException.class, () -> storage.saveCoordiMate(model.getCoordiMate())));
            before.assertUnchanged(model);
        }
    }
}
