package coordimate.logic.commands;

import static coordimate.logic.commands.CommandTestUtil.assertCommandSuccess;
import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BENSON;
import static coordimate.testutil.TypicalPersons.getTypicalCoordiMate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import coordimate.model.Model;
import coordimate.model.ModelManager;
import coordimate.model.UserPrefs;
import coordimate.model.person.ContactMatchesKeywordPredicate;

public class SearchCommandTest {

    private final Model model = new ModelManager(getTypicalCoordiMate(), new UserPrefs());
    private final Model expectedModel = new ModelManager(getTypicalCoordiMate(), new UserPrefs());

    @Test
    public void equals() {
        ContactMatchesKeywordPredicate firstPredicate = new ContactMatchesKeywordPredicate("Alice");
        ContactMatchesKeywordPredicate secondPredicate = new ContactMatchesKeywordPredicate("Benson");

        SearchCommand searchFirstCommand = new SearchCommand(firstPredicate, "Alice");
        SearchCommand searchSecondCommand = new SearchCommand(secondPredicate, "Benson");

        // same object -> returns true
        assertEquals(searchFirstCommand, searchFirstCommand);

        // same values -> returns true
        SearchCommand searchFirstCommandCopy = new SearchCommand(new ContactMatchesKeywordPredicate("Alice"),
                "Alice");
        assertEquals(searchFirstCommand, searchFirstCommandCopy);

        // different types -> returns false
        assertNotEquals(searchFirstCommand, 1);

        // null -> returns false
        assertNotEquals(searchFirstCommand, null);

        // different predicate -> returns false
        assertNotEquals(searchFirstCommand, searchSecondCommand);
    }

    @Test
    public void execute_keywordMatchesOnePerson_oneContactFound() {
        ContactMatchesKeywordPredicate predicate = new ContactMatchesKeywordPredicate(ALICE.getName().getFullName());
        SearchCommand command = new SearchCommand(predicate, ALICE.getName().getFullName());

        String expectedMessage = String.format(SearchCommand.MESSAGE_SUCCESS, 1, ALICE.getName().getFullName());
        expectedModel.updateFilteredPersonList(predicate);

        assertCommandSuccess(command, model, expectedMessage, expectedModel);
        assertEquals(List.of(ALICE), model.getFilteredPersonList());
    }

    @Test
    public void execute_keywordMatchesPhoneSubstring_contactFound() {
        String keyword = BENSON.getPhone().getValue().substring(1, 5);
        ContactMatchesKeywordPredicate predicate = new ContactMatchesKeywordPredicate(keyword);
        SearchCommand command = new SearchCommand(predicate, keyword);

        expectedModel.updateFilteredPersonList(predicate);

        assertTrue(expectedModel.getFilteredPersonList().contains(BENSON));
    }

    @Test
    public void execute_noMatches_showsNoMatchesMessage() {
        ContactMatchesKeywordPredicate predicate = new ContactMatchesKeywordPredicate("zzzNoSuchContactzzz");
        SearchCommand command = new SearchCommand(predicate, "zzzNoSuchContactzzz");

        String expectedMessage = String.format(SearchCommand.MESSAGE_NO_MATCHES, "zzzNoSuchContactzzz");
        expectedModel.updateFilteredPersonList(predicate);

        assertCommandSuccess(command, model, expectedMessage, expectedModel);
        assertEquals(List.of(), model.getFilteredPersonList());
    }
}
