package coordimate.logic;

import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BENSON;
import static coordimate.testutil.TypicalPersons.CARL;
import static coordimate.testutil.TypicalPersons.DANIEL;
import static coordimate.testutil.TypicalPersons.getTypicalCoordiMate;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import coordimate.model.ModelManager;
import coordimate.model.UserPrefs;
import coordimate.storage.JsonCoordiMateStorage;
import coordimate.storage.JsonUserPrefsStorage;
import coordimate.storage.StorageManager;

/**
 * Checks that the contact list shown by {@code filter event/...} follows later changes to the event.
 */
public class FilterIntegrationTest {
    @TempDir
    public Path temporaryFolder;

    private ModelManager model;
    private Logic logic;

    @BeforeEach
    public void setUp() throws Exception {
        model = new ModelManager(getTypicalCoordiMate(), new UserPrefs());
        logic = new LogicManager(model, new StorageManager(
                new JsonCoordiMateStorage(temporaryFolder.resolve("data.json")),
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));
        logic.execute("addevent evn/Final Concert st/08-08-2027 et/08-08-2027");
        logic.execute("addevent evn/Fair st/09-10-2027 et/10-10-2027");
        logic.execute("assign evn/Final Concert c/1 2");
    }

    @Test
    public void unassign_filteredEvent_removedMemberDisappears() throws Exception {
        logic.execute("filter event/Final Concert");
        assertEquals(List.of(ALICE, BENSON), model.getFilteredPersonList());

        logic.execute("unassign evn/Final Concert c/1");
        assertEquals(List.of(BENSON), model.getFilteredPersonList());
    }

    @Test
    public void assign_filteredEvent_newMemberAppears() throws Exception {
        logic.execute("filter event/Final Concert");
        logic.execute("assign evn/Final Concert c/3");
        assertEquals(List.of(ALICE, BENSON, CARL), model.getFilteredPersonList());
    }

    @Test
    public void filteredEventDeleted_listEmpties() throws Exception {
        logic.execute("filter event/Final Concert");
        logic.execute("deleteevent evn/Final Concert");
        assertEquals(List.of(), model.getFilteredPersonList());
    }

    @Test
    public void filteredEventRenamed_listKeepsMembers() throws Exception {
        logic.execute("filter event/final concert");
        logic.execute("editevent evn/Final Concert nevn/Grand Concert");
        assertEquals(List.of(ALICE, BENSON), model.getFilteredPersonList());

        logic.execute("unassign evn/Grand Concert c/1");
        assertEquals(List.of(BENSON), model.getFilteredPersonList());
    }

    @Test
    public void otherEventChanged_listUnchanged() throws Exception {
        logic.execute("filter event/Final Concert");
        logic.execute("assign evn/Fair c/3");
        logic.execute("editevent evn/Fair nevn/Final Concert Two");
        logic.execute("editevent evn/Final Concert st/08-08-2027 16:00");
        assertEquals(List.of(ALICE, BENSON), model.getFilteredPersonList());
    }

    @Test
    public void eventChangedWithTagCriterion_bothCriteriaStillApply() throws Exception {
        logic.execute("filter event/Final Concert tag/friends");
        assertEquals(List.of(ALICE, BENSON), model.getFilteredPersonList());

        logic.execute("assign evn/Final Concert c/3 4");
        assertEquals(List.of(ALICE, BENSON, DANIEL), model.getFilteredPersonList());

        logic.execute("editevent evn/Final Concert nevn/Grand Concert");
        logic.execute("unassign evn/Grand Concert c/1");
        assertEquals(List.of(BENSON, DANIEL), model.getFilteredPersonList());
    }
}
