package coordimate.logic.commands;

import static coordimate.logic.commands.CommandTestUtil.assertCommandFailure;
import static coordimate.logic.commands.CommandTestUtil.assertCommandSuccess;
import static coordimate.logic.commands.CommandTestUtil.showPersonAtIndex;
import static coordimate.testutil.TypicalIndexes.INDEX_FIRST_PERSON;
import static coordimate.testutil.TypicalIndexes.INDEX_SECOND_PERSON;
import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BENSON;
import static coordimate.testutil.TypicalPersons.getTypicalCoordiMate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import coordimate.commons.core.index.Index;
import coordimate.logic.commands.DeleteCommand.IdentifierType;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.Model;
import coordimate.model.ModelManager;
import coordimate.model.UserPrefs;
import coordimate.model.person.Person;
import coordimate.testutil.PersonBuilder;

/**
 * Contains integration tests (interaction with the Model) and unit tests for
 * {@code DeleteCommand}.
 */
public class DeleteCommandTest {

    private Model model = new ModelManager(getTypicalCoordiMate(), new UserPrefs());

    @Test
    public void execute_validIndexUnfilteredList_success() {
        Person personToDelete = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        DeleteCommand deleteCommand = new DeleteCommand(INDEX_FIRST_PERSON);

        String expectedMessage = String.format(DeleteCommand.MESSAGE_DELETE_PERSON_SUCCESS,
                personToDelete.getName());

        ModelManager expectedModel = new ModelManager(model.getCoordiMate(), new UserPrefs());
        expectedModel.deletePerson(personToDelete);

        assertCommandSuccess(deleteCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_invalidIndexUnfilteredList_throwsCommandException() {
        Index outOfBoundIndex = Index.fromOneBased(model.getFilteredPersonList().size() + 1);
        DeleteCommand deleteCommand = new DeleteCommand(outOfBoundIndex);

        assertCommandFailure(deleteCommand, model,
                String.format(DeleteCommand.MESSAGE_INVALID_INDEX, outOfBoundIndex.getOneBased()));
    }

    @Test
    public void execute_validIndexFilteredList_success() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);

        Person personToDelete = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        DeleteCommand deleteCommand = new DeleteCommand(INDEX_FIRST_PERSON);

        String expectedMessage = String.format(DeleteCommand.MESSAGE_DELETE_PERSON_SUCCESS,
                personToDelete.getName());

        Model expectedModel = new ModelManager(model.getCoordiMate(), new UserPrefs());
        expectedModel.deletePerson(personToDelete);
        showNoPerson(expectedModel);

        assertCommandSuccess(deleteCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_invalidIndexFilteredList_throwsCommandException() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);

        Index outOfBoundIndex = INDEX_SECOND_PERSON;
        // ensures that outOfBoundIndex is still in bounds of CoordiMate list
        assertTrue(outOfBoundIndex.getZeroBased() < model.getCoordiMate().getPersonList().size());

        DeleteCommand deleteCommand = new DeleteCommand(outOfBoundIndex);

        assertCommandFailure(deleteCommand, model,
                String.format(DeleteCommand.MESSAGE_INVALID_INDEX, outOfBoundIndex.getOneBased()));
    }

    @Test
    public void resolvePerson_detailIdentifiersSearchAllSavedContacts() throws Exception {
        model.updateFilteredPersonList(person -> false);

        assertEquals(ALICE, new DeleteCommand(IdentifierType.NAME, " aLiCe pAuLiNe ").resolvePerson(model));
        assertEquals(ALICE, new DeleteCommand(IdentifierType.PHONE, "(9435) 1253").resolvePerson(model));
        assertEquals(ALICE, new DeleteCommand(IdentifierType.EMAIL, "ALICE@EXAMPLE.COM").resolvePerson(model));
        assertEquals(BENSON, new DeleteCommand(IdentifierType.NAME, "Benson Meier").resolvePerson(model));
    }

    @Test
    public void resolvePerson_noExactMatch_failure() {
        assertEquals(DeleteCommand.MESSAGE_NO_MATCH,
                assertThrows(CommandException.class, () ->
                        new DeleteCommand(IdentifierType.NAME, "Alice").resolvePerson(model)).getMessage());
    }

    @Test
    public void resolveMatches_ambiguousName_showsOnlyDisplayedIndex() {
        Person otherAlice = new PersonBuilder(ALICE).withPhone("22222222")
                .withEmail("other@example.com").build();
        DeleteCommand command = new DeleteCommand(IdentifierType.NAME, "Alice Pauline");

        CommandException exception = assertThrows(CommandException.class, () ->
                command.resolveMatches(List.of(ALICE, otherAlice), List.of(otherAlice)));

        assertEquals("Multiple contacts match that identifier:\n"
                + "- Alice Pauline | Phone: 94351253 | Email: alice@example.com\n"
                + "- Alice Pauline | Phone: 22222222 | Email: other@example.com | Current-list index: 1\n"
                + "Use a displayed index, phone number, or email address to identify the contact.",
                exception.getMessage());
    }

    @Test
    public void confirmationPrompt_containsContactAndQuestion() {
        assertEquals("Contact found:\nName: Alice Pauline\nPhone: 94351253\nEmail: alice@example.com\n"
                + "Role: NA\nConfirm deletion? [y/N]", DeleteCommand.confirmationPrompt(ALICE));
    }

    @Test
    public void equals() {
        DeleteCommand deleteFirstCommand = new DeleteCommand(INDEX_FIRST_PERSON);
        DeleteCommand deleteSecondCommand = new DeleteCommand(INDEX_SECOND_PERSON);

        // same object -> returns true
        assertTrue(deleteFirstCommand.equals(deleteFirstCommand));

        // same values -> returns true
        DeleteCommand deleteFirstCommandCopy = new DeleteCommand(INDEX_FIRST_PERSON);
        assertTrue(deleteFirstCommand.equals(deleteFirstCommandCopy));

        // different types -> returns false
        assertFalse(deleteFirstCommand.equals(1));

        // null -> returns false
        assertFalse(deleteFirstCommand.equals(null));

        // different person -> returns false
        assertFalse(deleteFirstCommand.equals(deleteSecondCommand));
    }

    @Test
    public void toStringMethod() {
        Index targetIndex = Index.fromOneBased(1);
        DeleteCommand deleteCommand = new DeleteCommand(targetIndex);
        String expected = DeleteCommand.class.getCanonicalName() + "{targetIndex=" + targetIndex
                + ", identifierType=null, identifier=null, resolvedTarget=null}";
        assertEquals(expected, deleteCommand.toString());
    }

    /**
     * Updates {@code model}'s filtered list to show no one.
     */
    private void showNoPerson(Model model) {
        model.updateFilteredPersonList(p -> false);

        assertTrue(model.getFilteredPersonList().isEmpty());
    }
}
