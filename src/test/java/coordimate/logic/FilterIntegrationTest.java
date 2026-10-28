package coordimate.logic;

import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BENSON;
import static coordimate.testutil.TypicalPersons.CARL;
import static coordimate.testutil.TypicalPersons.DANIEL;
import static coordimate.testutil.TypicalPersons.getTypicalCoordiMate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import coordimate.logic.commands.AssignCommand;
import coordimate.logic.commands.DeleteEventCommand;
import coordimate.logic.commands.EditEventCommand;
import coordimate.logic.commands.UnassignCommand;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.CoordiMate;
import coordimate.model.ModelManager;
import coordimate.model.ReadOnlyCoordiMate;
import coordimate.model.UserPrefs;
import coordimate.model.person.Person;
import coordimate.storage.DataConflictException;
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
    public void filteredEventCaseOnlyRename_membershipChangesStillUpdateFilter() throws Exception {
        logic.execute("filter event/Final Concert tag/friends");
        logic.execute("editevent evn/Final Concert nevn/FINAL CONCERT");
        assertEquals(List.of(ALICE, BENSON), model.getFilteredPersonList());

        logic.execute("assign evn/FINAL CONCERT c/3 4");
        logic.execute("unassign evn/FINAL CONCERT c/1");
        assertEquals(List.of(BENSON, DANIEL), model.getFilteredPersonList());
    }

    @Test
    public void filteredEventDuplicateRename_rejectedWithoutChangingFilter() throws Exception {
        logic.execute("filter event/Final Concert");
        Predicate<? super Person> originalFilter = model.getFilteredPersonPredicate();
        CoordiMate originalData = new CoordiMate(model.getCoordiMate());
        Path dataPath = temporaryFolder.resolve("data.json");
        String originalFile = Files.readString(dataPath);

        assertThrows(CommandException.class, () -> logic.execute("editevent evn/Final Concert nevn/Fair"));

        assertSame(originalFilter, model.getFilteredPersonPredicate());
        assertEquals(List.of(ALICE, BENSON), model.getFilteredPersonList());
        assertEquals(originalData, model.getCoordiMate());
        assertEquals(originalFile, Files.readString(dataPath));
        logic.execute("unassign evn/Final Concert c/1");
        assertEquals(List.of(BENSON), model.getFilteredPersonList());
    }

    @Test
    public void filteredEventRename_concurrentSave_preservesFilterAndOtherWritersData() throws Exception {
        logic.execute("filter event/Final Concert tag/friends");
        Predicate<? super Person> originalFilter = model.getFilteredPersonPredicate();
        CoordiMate originalData = new CoordiMate(model.getCoordiMate());
        Path dataPath = temporaryFolder.resolve("data.json");
        JsonCoordiMateStorage otherStorage = new JsonCoordiMateStorage(dataPath);
        ModelManager otherModel = new ModelManager(otherStorage.readCoordiMate().orElseThrow(), new UserPrefs());
        Logic otherLogic = new LogicManager(otherModel, new StorageManager(otherStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("other-prefs.json"))));
        otherLogic.execute("assign evn/Final Concert c/3");
        String otherWritersFile = Files.readString(dataPath);

        CommandException exception = assertThrows(CommandException.class, () ->
                logic.execute("editevent evn/Final Concert nevn/Grand Concert"));

        assertEquals(Messages.withUsage(DataConflictException.MESSAGE, EditEventCommand.MESSAGE_USAGE),
                exception.getMessage());
        assertSame(originalFilter, model.getFilteredPersonPredicate());
        assertEquals(List.of(ALICE, BENSON), model.getFilteredPersonList());
        assertEquals(originalData, model.getCoordiMate());
        assertEquals(otherWritersFile, Files.readString(dataPath));
        assertEquals(otherModel.getCoordiMate(), otherStorage.readCoordiMate().orElseThrow());
    }

    @Test
    public void filteredEventRename_saveFails_filterAndContactsUnchangedThenRetrySucceeds() throws Exception {
        String rename = "editevent evn/Final Concert nevn/Grand Concert";
        assertSaveFailurePreservesFilter("filter event/Final Concert", rename,
                EditEventCommand.MESSAGE_SAVE_ERROR, EditEventCommand.MESSAGE_USAGE);

        logic.execute(rename);
        assertEquals(List.of(ALICE, BENSON), model.getFilteredPersonList());
        logic.execute("unassign evn/Grand Concert c/1");
        assertEquals(List.of(BENSON), model.getFilteredPersonList());
    }

    @Test
    public void combinedEventFilterRename_saveFails_allCriteriaUnchangedThenRetrySucceeds() throws Exception {
        String rename = "editevent evn/Final Concert nevn/Grand Concert";
        assertSaveFailurePreservesFilter("filter event/Final Concert tag/friends", rename,
                EditEventCommand.MESSAGE_SAVE_ERROR, EditEventCommand.MESSAGE_USAGE);

        logic.execute(rename);
        logic.execute("assign evn/Grand Concert c/3 4");
        assertEquals(List.of(ALICE, BENSON, DANIEL), model.getFilteredPersonList());
    }

    @Test
    public void filteredEventAssignment_saveFails_filterAndContactsUnchangedThenRetrySucceeds() throws Exception {
        String assign = "assign evn/Final Concert c/3";
        assertSaveFailurePreservesFilter("filter event/Final Concert", assign,
                AssignCommand.MESSAGE_SAVE_ERROR, AssignCommand.MESSAGE_USAGE);

        logic.execute(assign);
        assertEquals(List.of(ALICE, BENSON, CARL), model.getFilteredPersonList());
    }

    @Test
    public void filteredEventUnassignment_saveFails_filterAndContactsUnchangedThenRetrySucceeds() throws Exception {
        String unassign = "unassign evn/Final Concert c/1";
        assertSaveFailurePreservesFilter("filter event/Final Concert", unassign,
                UnassignCommand.MESSAGE_SAVE_ERROR, UnassignCommand.MESSAGE_USAGE);

        logic.execute(unassign);
        assertEquals(List.of(BENSON), model.getFilteredPersonList());
    }

    @Test
    public void filteredEventDeletion_saveFails_filterAndContactsUnchangedThenRetrySucceeds() throws Exception {
        String delete = "deleteevent evn/Final Concert";
        assertSaveFailurePreservesFilter("filter event/Final Concert", delete,
                DeleteEventCommand.MESSAGE_SAVE_ERROR, DeleteEventCommand.MESSAGE_USAGE);

        logic.execute(delete);
        assertEquals(List.of(), model.getFilteredPersonList());
    }

    @Test
    public void normalizedEventFilter_renameAndMembershipChanges_keepsFollowingEvent() throws Exception {
        logic.execute("filter event/  fInAl   CoNcErT  tag/friends");
        assertEquals(List.of(ALICE, BENSON), model.getFilteredPersonList());

        logic.execute("editevent evn/Final Concert nevn/Grand Concert");
        logic.execute("assign evn/Grand Concert c/3 4");
        logic.execute("unassign evn/Grand Concert c/1");
        assertEquals(List.of(BENSON, DANIEL), model.getFilteredPersonList());

        logic.execute("deleteevent evn/Grand Concert");
        assertEquals(List.of(), model.getFilteredPersonList());
    }

    @Test
    public void multipleEventCriteria_rename_preservesIntersection() throws Exception {
        logic.execute("assign evn/Fair c/2 3");
        logic.execute("filter event/Final Concert event/Fair");
        assertEquals(List.of(BENSON), model.getFilteredPersonList());

        logic.execute("editevent evn/Fair nevn/Welcome Fair");
        logic.execute("assign evn/Final Concert c/3");
        assertEquals(List.of(BENSON, CARL), model.getFilteredPersonList());

        logic.execute("editevent evn/Final Concert nevn/Grand Concert");
        logic.execute("unassign evn/Welcome Fair c/2");
        assertEquals(List.of(CARL), model.getFilteredPersonList());
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

    private void assertSaveFailurePreservesFilter(String filter, String command, String saveError, String usage)
            throws Exception {
        logic.execute(filter);
        assertEquals(List.of(ALICE, BENSON), model.getFilteredPersonList());
        Predicate<? super Person> originalFilter = model.getFilteredPersonPredicate();
        List<Person> originalContacts = List.copyOf(model.getFilteredPersonList());
        CoordiMate originalData = new CoordiMate(model.getCoordiMate());
        Path dataPath = temporaryFolder.resolve("data.json");
        String originalFile = Files.readString(dataPath);
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(dataPath) {
            private boolean failNextSave = true;

            @Override
            public void saveCoordiMate(ReadOnlyCoordiMate data) throws IOException {
                if (failNextSave) {
                    failNextSave = false;
                    throw new IOException("Disk full");
                }
                super.saveCoordiMate(data);
            }
        };
        logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));

        CommandException exception = assertThrows(CommandException.class, () -> logic.execute(command));

        assertEquals(Messages.withUsage(saveError, usage), exception.getMessage());
        assertSame(originalFilter, model.getFilteredPersonPredicate());
        assertEquals(originalContacts, model.getFilteredPersonList());
        assertEquals(originalData, model.getCoordiMate());
        assertEquals(originalFile, Files.readString(dataPath));
    }
}
