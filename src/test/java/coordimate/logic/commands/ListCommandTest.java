package coordimate.logic.commands;

import static coordimate.logic.commands.CommandTestUtil.assertCommandSuccess;
import static coordimate.logic.commands.CommandTestUtil.showPersonAtIndex;
import static coordimate.model.Model.PREDICATE_SHOW_ALL_PERSONS;
import static coordimate.testutil.TypicalIndexes.INDEX_FIRST_PERSON;
import static coordimate.testutil.TypicalPersons.getTypicalCoordiMate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import coordimate.model.Model;
import coordimate.model.ModelManager;
import coordimate.model.UserPrefs;

/**
 * Contains integration tests (interaction with the Model) and unit tests for ListCommand.
 */
public class ListCommandTest {

    private Model model;
    private Model expectedModel;

    @BeforeEach
    public void setUp() {
        model = new ModelManager(getTypicalCoordiMate(), new UserPrefs());
        expectedModel = new ModelManager(model.getCoordiMate(), new UserPrefs());
    }

    @Test
    public void execute_listIsNotFiltered_showsSameList() {
        assertCommandSuccess(new ListCommand(), model, ListCommand.MESSAGE_SUCCESS, expectedModel);
    }

    @Test
    public void execute_listIsFiltered_showsEverything() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);
        assertCommandSuccess(new ListCommand(), model, ListCommand.MESSAGE_SUCCESS, expectedModel);
    }

    @Test
    public void execute_archived_showsArchivedViewAndListReturnsToActive() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);
        expectedModel.setShowingArchived(true);
        expectedModel.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
        assertCommandSuccess(new ListCommand(true), model, ListCommand.MESSAGE_ARCHIVED_SUCCESS, expectedModel);
        assertTrue(model.isShowingArchived());

        expectedModel.setShowingArchived(false);
        assertCommandSuccess(new ListCommand(false), model, ListCommand.MESSAGE_SUCCESS, expectedModel);
        assertFalse(model.isShowingArchived());
    }

    @Test
    public void equals() {
        assertEquals(new ListCommand(), new ListCommand(false));
        assertEquals(new ListCommand(true).hashCode(), new ListCommand(true).hashCode());
        assertNotEquals(new ListCommand(true), new ListCommand(false));
        assertNotEquals(new ListCommand(), new ClearCommand());
    }
}
