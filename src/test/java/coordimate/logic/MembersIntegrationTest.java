package coordimate.logic;

import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BENSON;
import static coordimate.testutil.TypicalPersons.CARL;
import static coordimate.testutil.TypicalPersons.getTypicalCoordiMate;
import static coordimate.testutil.TypicalPersons.getTypicalPersons;
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
 * Checks that the contact list shown by {@code members} follows later changes to the event.
 */
public class MembersIntegrationTest {
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
        logic.execute("addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00");
        logic.execute("addevent evn/Fair st/09-10-2026 et/10-10-2026");
        logic.execute("assign evn/Final Concert c/1 2 3");
    }

    @Test
    public void unassign_shownEvent_contactDisappearsAndIndexesShift() throws Exception {
        logic.execute("members evn/Final Concert");
        assertEquals(List.of(ALICE, BENSON, CARL), model.getFilteredPersonList());

        logic.execute("unassign evn/Final Concert c/2");
        assertEquals(List.of(ALICE, CARL), model.getFilteredPersonList());

        logic.execute("unassign evn/Final Concert c/2");
        assertEquals(List.of(ALICE), model.getFilteredPersonList());
    }

    @Test
    public void changesToAnotherEvent_shownListUnchanged() throws Exception {
        logic.execute("members evn/Final Concert");
        logic.execute("assign evn/Fair c/1 2");
        logic.execute("unassign evn/Fair c/1");
        assertEquals(List.of(ALICE, BENSON, CARL), model.getFilteredPersonList());
    }

    @Test
    public void shownEventRenamed_listKeepsMembers() throws Exception {
        logic.execute("members evn/Final Concert");
        logic.execute("editevent evn/final concert nevn/Grand Concert");
        assertEquals(List.of(ALICE, BENSON, CARL), model.getFilteredPersonList());

        logic.execute("unassign evn/Grand Concert c/1");
        assertEquals(List.of(BENSON, CARL), model.getFilteredPersonList());

        logic.execute("editevent evn/Grand Concert nevn/GRAND CONCERT st/08-08-2026 16:00");
        assertEquals(List.of(BENSON, CARL), model.getFilteredPersonList());
    }

    @Test
    public void otherEventRenamedOrShownEventTimeEdited_listUnchanged() throws Exception {
        logic.execute("assign evn/Fair c/4");
        logic.execute("members evn/Final Concert");
        logic.execute("editevent evn/Fair nevn/Final Concert Two");
        logic.execute("editevent evn/Final Concert st/08-08-2026 16:00");
        assertEquals(List.of(ALICE, BENSON, CARL), model.getFilteredPersonList());
    }

    @Test
    public void shownEventDeleted_listEmpties() throws Exception {
        logic.execute("members evn/Final Concert");
        logic.execute("deleteevent evn/Final Concert");
        assertEquals(List.of(), model.getFilteredPersonList());
    }

    @Test
    public void renameAfterFind_listUnchanged() throws Exception {
        logic.execute("find Kurz");
        logic.execute("editevent evn/Final Concert nevn/Grand Concert");
        assertEquals(List.of(CARL), model.getFilteredPersonList());
    }

    @Test
    public void list_afterMembers_showsAllContacts() throws Exception {
        logic.execute("members evn/Fair");
        assertEquals(List.of(), model.getFilteredPersonList());
        logic.execute("list");
        assertEquals(getTypicalPersons(), model.getFilteredPersonList());
    }
}
