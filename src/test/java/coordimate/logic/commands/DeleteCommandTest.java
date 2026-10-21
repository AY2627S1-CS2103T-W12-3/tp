package coordimate.logic.commands;

import static coordimate.logic.commands.CommandTestUtil.assertCommandFailure;
import static coordimate.logic.commands.CommandTestUtil.assertCommandSuccess;
import static coordimate.logic.commands.CommandTestUtil.showPersonAtIndex;
import static coordimate.testutil.TypicalIndexes.INDEX_FIRST_PERSON;
import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BENSON;
import static coordimate.testutil.TypicalPersons.getTypicalCoordiMate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import coordimate.logic.commands.DeleteCommand.IdentifierType;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.Model;
import coordimate.model.ModelManager;
import coordimate.model.UserPrefs;
import coordimate.model.person.MemberId;
import coordimate.model.person.Person;
import coordimate.testutil.PersonBuilder;

/**
 * Contains integration tests (interaction with the Model) and unit tests for
 * {@code DeleteCommand}.
 */
public class DeleteCommandTest {

    private Model model = new ModelManager(getTypicalCoordiMate(), new UserPrefs());

    @Test
    public void execute_validMemberIdUnfilteredList_success() {
        Person personToDelete = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        DeleteCommand deleteCommand = new DeleteCommand(new MemberId(1));

        String expectedMessage = String.format(DeleteCommand.MESSAGE_DELETE_PERSON_SUCCESS,
                personToDelete.getName());

        ModelManager expectedModel = new ModelManager(model.getCoordiMate(), new UserPrefs());
        expectedModel.deletePerson(personToDelete);

        assertCommandSuccess(deleteCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_unknownMemberIdUnfilteredList_throwsCommandException() {
        MemberId unknownId = new MemberId(99);
        DeleteCommand deleteCommand = new DeleteCommand(unknownId);

        assertCommandFailure(deleteCommand, model,
                String.format(DeleteCommand.MESSAGE_UNKNOWN_MEMBER_ID, unknownId.getValue()));
    }

    @Test
    public void execute_validMemberIdFilteredList_success() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);

        Person personToDelete = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        DeleteCommand deleteCommand = new DeleteCommand(new MemberId(1));

        String expectedMessage = String.format(DeleteCommand.MESSAGE_DELETE_PERSON_SUCCESS,
                personToDelete.getName());

        Model expectedModel = new ModelManager(model.getCoordiMate(), new UserPrefs());
        expectedModel.deletePerson(personToDelete);
        showNoPerson(expectedModel);

        assertCommandSuccess(deleteCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_memberHiddenByFilter_success() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);
        Model expectedModel = new ModelManager(model.getCoordiMate(), new UserPrefs());
        expectedModel.deletePerson(BENSON);
        expectedModel.updateFilteredPersonList(person -> person.equals(ALICE));
        assertCommandSuccess(new DeleteCommand(new MemberId(2)), model,
                String.format(DeleteCommand.MESSAGE_DELETE_PERSON_SUCCESS, BENSON.getName()), expectedModel);
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
    public void resolveMatches_ambiguousName_showsPermanentIds() {
        Person otherAlice = new PersonBuilder(ALICE).withMemberId(12).withPhone("22222222")
                .withEmail("other@example.com").build();
        DeleteCommand command = new DeleteCommand(IdentifierType.NAME, "Alice Pauline");

        CommandException exception = assertThrows(CommandException.class, () ->
                command.resolveMatches(List.of(ALICE, otherAlice)));

        assertEquals("Multiple contacts match that identifier:\n"
                + "- Alice Pauline | ID: 1 | Phone: 94351253 | Email: alice@example.com\n"
                + "- Alice Pauline | ID: 12 | Phone: 22222222 | Email: other@example.com\n"
                + "Use a member ID, phone number, or email address to identify the contact.",
                exception.getMessage());
    }

    @Test
    public void confirmationPrompt_containsContactAndQuestion() {
        assertEquals("Contact found:\nID: 1\nName: Alice Pauline\nPhone: 94351253\nEmail: alice@example.com\n"
                + "Role: NA\nConfirm deletion? [y/N]", DeleteCommand.confirmationPrompt(ALICE));
    }

    @Test
    public void equals() {
        DeleteCommand deleteFirstCommand = new DeleteCommand(new MemberId(1));
        DeleteCommand deleteSecondCommand = new DeleteCommand(new MemberId(2));

        // same object -> returns true
        assertTrue(deleteFirstCommand.equals(deleteFirstCommand));

        // same values -> returns true
        DeleteCommand deleteFirstCommandCopy = new DeleteCommand(new MemberId(1));
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
        MemberId memberId = new MemberId(1);
        DeleteCommand deleteCommand = new DeleteCommand(memberId);
        String expected = DeleteCommand.class.getCanonicalName() + "{memberId=" + memberId
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
