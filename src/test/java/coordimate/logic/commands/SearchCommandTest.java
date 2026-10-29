package coordimate.logic.commands;

import static coordimate.logic.commands.CommandTestUtil.assertCommandSuccess;
import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BENSON;
import static coordimate.testutil.TypicalPersons.DANIEL;
import static coordimate.testutil.TypicalPersons.getTypicalCoordiMate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import coordimate.model.Model;
import coordimate.model.ModelManager;
import coordimate.model.UserPrefs;
import coordimate.model.person.ContactMatchesKeywordPredicate;
import coordimate.model.person.Person;
import coordimate.testutil.PersonBuilder;

public class SearchCommandTest {

    private final Model model = new ModelManager(getTypicalCoordiMate(), new UserPrefs());
    private final Model expectedModel = new ModelManager(getTypicalCoordiMate(), new UserPrefs());

    @Test
    public void execute_sharedNameSubstring_multipleContactsFound() {
        String keyword = "eIeR";
        ContactMatchesKeywordPredicate predicate = new ContactMatchesKeywordPredicate(keyword);
        SearchCommand command = new SearchCommand(predicate, keyword);
        expectedModel.updateFilteredPersonList(predicate);

        assertCommandSuccess(command, model, String.format(SearchCommand.MESSAGE_SUCCESS, 2, keyword), expectedModel);
        assertEquals(List.of(BENSON, DANIEL), model.getFilteredPersonList());
    }

    @Test
    public void execute_dottedCapitalIName_matchesLowercaseKeyword() {
        Person ipek = new PersonBuilder().withMemberId(8).withName("İpek")
                .withPhone("8000001").withEmail("contactx@example.com").build();
        model.addPerson(ipek);
        expectedModel.addPerson(ipek);
        String keyword = "ipek";
        ContactMatchesKeywordPredicate predicate = new ContactMatchesKeywordPredicate(keyword);
        expectedModel.updateFilteredPersonList(predicate);

        assertCommandSuccess(new SearchCommand(predicate, keyword), model,
                String.format(SearchCommand.MESSAGE_SUCCESS, 1, keyword), expectedModel);
        assertEquals(List.of(ipek), model.getFilteredPersonList());
    }

    @Test
    public void execute_previouslyFilteredList_searchesAllContacts() {
        model.updateFilteredPersonList(new ContactMatchesKeywordPredicate("Alice"));
        ContactMatchesKeywordPredicate predicate = new ContactMatchesKeywordPredicate("Benson");
        expectedModel.updateFilteredPersonList(predicate);

        assertCommandSuccess(new SearchCommand(predicate, "Benson"), model,
                String.format(SearchCommand.MESSAGE_SUCCESS, 1, "Benson"), expectedModel);
        assertEquals(List.of(BENSON), model.getFilteredPersonList());
    }

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

        List<Person> expectedContacts = List.of(BENSON, DANIEL);
        String expectedMessage = String.format(SearchCommand.MESSAGE_SUCCESS, expectedContacts.size(), keyword);
        assertCommandSuccess(command, model, expectedMessage, expectedModel);
        assertEquals(expectedContacts, model.getFilteredPersonList(),
                "The phone substring should match exactly Benson and Daniel, in saved order.");
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
