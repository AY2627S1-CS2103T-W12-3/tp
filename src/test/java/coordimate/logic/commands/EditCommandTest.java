package coordimate.logic.commands;

import static coordimate.logic.commands.CommandTestUtil.DESC_AMY;
import static coordimate.logic.commands.CommandTestUtil.DESC_BOB;
import static coordimate.logic.commands.CommandTestUtil.VALID_ADDRESS_BOB;
import static coordimate.logic.commands.CommandTestUtil.VALID_NAME_BOB;
import static coordimate.logic.commands.CommandTestUtil.VALID_PHONE_BOB;
import static coordimate.logic.commands.CommandTestUtil.VALID_TAG_HUSBAND;
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
import coordimate.logic.Messages;
import coordimate.logic.commands.EditCommand.EditPersonDescriptor;
import coordimate.logic.commands.EditCommand.TagOperation;
import coordimate.logic.commands.EditCommand.TagOperationType;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.CoordiMate;
import coordimate.model.Model;
import coordimate.model.ModelManager;
import coordimate.model.UserPrefs;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.person.Name;
import coordimate.model.person.Person;
import coordimate.model.tag.Tag;
import coordimate.testutil.EditPersonDescriptorBuilder;
import coordimate.testutil.PersonBuilder;

/**
 * Contains integration tests (interaction with the Model) and unit tests for EditCommand.
 */
public class EditCommandTest {

    private Model model = new ModelManager(getTypicalCoordiMate(), new UserPrefs());

    @Test
    public void constructor_blankTarget_throwsIllegalArgumentException() {
        EditPersonDescriptor descriptor = new EditPersonDescriptor();

        assertThrows(IllegalArgumentException.class, () -> new EditCommand("   ", descriptor));
    }

    @Test
    public void execute_allFieldsSpecifiedUnfilteredList_success() {
        Person editedPerson = new PersonBuilder().build();
        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder(editedPerson).build();
        EditCommand editCommand = new EditCommand(INDEX_FIRST_PERSON, descriptor);

        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_PERSON_SUCCESS, Messages.format(editedPerson));

        Model expectedModel = new ModelManager(new CoordiMate(model.getCoordiMate()), new UserPrefs());
        expectedModel.setPerson(model.getFilteredPersonList().get(0), editedPerson);

        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_someFieldsSpecifiedUnfilteredList_success() {
        Index indexLastPerson = Index.fromOneBased(model.getFilteredPersonList().size());
        Person lastPerson = model.getFilteredPersonList().get(indexLastPerson.getZeroBased());

        PersonBuilder personInList = new PersonBuilder(lastPerson);
        Person editedPerson = personInList.withName(VALID_NAME_BOB).withPhone(VALID_PHONE_BOB)
                .withTags(VALID_TAG_HUSBAND).build();

        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder().withName(VALID_NAME_BOB)
                .withPhone(VALID_PHONE_BOB).withTags(VALID_TAG_HUSBAND).build();
        EditCommand editCommand = new EditCommand(indexLastPerson, descriptor);

        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_PERSON_SUCCESS, Messages.format(editedPerson));

        Model expectedModel = new ModelManager(new CoordiMate(model.getCoordiMate()), new UserPrefs());
        expectedModel.setPerson(lastPerson, editedPerson);

        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_editAddress_preservesExistingContactDetails() {
        Person originalPerson = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        Person contactWithDetails = new PersonBuilder(originalPerson).withRole("Logistics")
                .withBirthday("18-06-2004").withOrganisation("NUS Student Affairs")
                .withNote("Handles venue bookings").build();
        model.setPerson(originalPerson, contactWithDetails);
        EditCommand editCommand = new EditCommand(INDEX_FIRST_PERSON,
                new EditPersonDescriptorBuilder().withAddress(VALID_ADDRESS_BOB).build());
        Person editedPerson = new PersonBuilder(contactWithDetails).withAddress(VALID_ADDRESS_BOB).build();
        Model expectedModel = new ModelManager(new CoordiMate(model.getCoordiMate()), new UserPrefs());
        expectedModel.setPerson(contactWithDetails, editedPerson);

        assertCommandSuccess(editCommand, model,
                String.format(EditCommand.MESSAGE_EDIT_PERSON_SUCCESS, Messages.format(editedPerson)), expectedModel);
    }

    @Test
    public void execute_editExpandedFields_preservesUnspecifiedDetails() {
        Person originalPerson = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        Person contactWithDetails = new PersonBuilder(originalPerson).withBirthday("18-06-2004")
                .withAddress("21 Kent Ridge Road").withNote("Original note").build();
        model.setPerson(originalPerson, contactWithDetails);
        EditCommand editCommand = new EditCommand(INDEX_FIRST_PERSON,
                new EditPersonDescriptorBuilder().withRole("Logistics Lead")
                        .withOrganisation("NUS Student Affairs").withNote("New note").build());
        Person editedPerson = new PersonBuilder(contactWithDetails).withRole("Logistics Lead")
                .withOrganisation("NUS Student Affairs").withNote("New note").build();
        Model expectedModel = new ModelManager(new CoordiMate(model.getCoordiMate()), new UserPrefs());
        expectedModel.setPerson(contactWithDetails, editedPerson);

        assertCommandSuccess(editCommand, model,
                String.format(EditCommand.MESSAGE_EDIT_PERSON_SUCCESS, Messages.format(editedPerson)), expectedModel);
    }

    @Test
    public void execute_clearOptionalDetails_removesOnlySelectedValues() {
        Person originalPerson = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        Person contactWithDetails = new PersonBuilder(originalPerson).withBirthday("18-06-2004")
                .withOrganisation("NUS Student Affairs").withNote("Original note").build();
        model.setPerson(originalPerson, contactWithDetails);
        EditCommand editCommand = new EditCommand(INDEX_FIRST_PERSON,
                new EditPersonDescriptorBuilder().withoutBirthday().withoutAddress()
                        .withoutOrganisation().withoutNote().build());
        Person editedPerson = new PersonBuilder(contactWithDetails).withoutBirthday().withoutAddress()
                .withoutOrganisation().withoutNote().build();
        Model expectedModel = new ModelManager(new CoordiMate(model.getCoordiMate()), new UserPrefs());
        expectedModel.setPerson(contactWithDetails, editedPerson);

        assertCommandSuccess(editCommand, model,
                String.format(EditCommand.MESSAGE_EDIT_PERSON_SUCCESS, Messages.format(editedPerson)), expectedModel);
    }

    @Test
    public void descriptorCopy_preservesClearOperations() {
        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder().withoutBirthday().withoutAddress()
                .withoutOrganisation().withoutNote().build();

        assertEquals(descriptor, new EditPersonDescriptor(descriptor));
        assertTrue(descriptor.isAnyFieldEdited());
    }

    @Test
    public void execute_editName_preservesUnsetAddress() {
        Person originalPerson = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        Person contactWithoutAddress = new PersonBuilder(originalPerson).withoutAddress().build();
        model.setPerson(originalPerson, contactWithoutAddress);
        EditCommand editCommand = new EditCommand(INDEX_FIRST_PERSON,
                new EditPersonDescriptorBuilder().withName("Alice Tan").build());
        Person editedPerson = new PersonBuilder(contactWithoutAddress).withName("Alice Tan").build();
        Model expectedModel = new ModelManager(new CoordiMate(model.getCoordiMate()), new UserPrefs());
        expectedModel.setPerson(contactWithoutAddress, editedPerson);
        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_PERSON_SUCCESS, Messages.format(editedPerson));

        assertTrue(expectedMessage.contains("— (not specified)"));
        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_noFieldSpecifiedUnfilteredList_success() {
        EditCommand editCommand = new EditCommand(INDEX_FIRST_PERSON, new EditPersonDescriptor());
        Person editedPerson = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());

        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_PERSON_SUCCESS, Messages.format(editedPerson));

        Model expectedModel = new ModelManager(new CoordiMate(model.getCoordiMate()), new UserPrefs());
        expectedModel.setPerson(editedPerson, editedPerson);

        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_addCustomTag_registersTagAndRetainsExistingTags() {
        Person original = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        EditPersonDescriptor descriptor = new EditPersonDescriptor();
        descriptor.addTagOperation(new TagOperation(TagOperationType.ADD, new Tag("Alumni2026")));
        Person edited = new PersonBuilder(original).withTags("friends", "Alumni2026").build();
        Model expectedModel = new ModelManager(new CoordiMate(model.getCoordiMate()), new UserPrefs());
        expectedModel.setPerson(original, edited);

        assertCommandSuccess(new EditCommand(INDEX_FIRST_PERSON, descriptor), model,
                String.format(EditCommand.MESSAGE_EDIT_PERSON_SUCCESS, Messages.format(edited)), expectedModel);
        assertTrue(model.getTagList().stream().anyMatch(tag -> tag.isSameTag(new Tag("Alumni2026"))));
    }

    @Test
    public void execute_removeTag_keepsTagInRegistry() {
        CoordiMate savedData = new CoordiMate(model.getCoordiMate());
        savedData.addTag(new Tag("friends"));
        model = new ModelManager(savedData, new UserPrefs());
        Person original = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        EditPersonDescriptor descriptor = new EditPersonDescriptor();
        descriptor.addTagOperation(new TagOperation(TagOperationType.REMOVE, new Tag("FRIENDS")));
        Person edited = new PersonBuilder(original).withTags().build();
        Model expectedModel = new ModelManager(new CoordiMate(model.getCoordiMate()), new UserPrefs());
        expectedModel.setPerson(original, edited);

        assertCommandSuccess(new EditCommand(INDEX_FIRST_PERSON, descriptor), model,
                String.format(EditCommand.MESSAGE_EDIT_PERSON_SUCCESS, Messages.format(edited)), expectedModel);
        assertTrue(model.getTagList().stream().anyMatch(tag -> tag.isSameTag(new Tag("friends"))));
    }

    @Test
    public void execute_tagOperations_applyInCommandOrder() {
        Person original = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        EditPersonDescriptor descriptor = new EditPersonDescriptor();
        descriptor.addTagOperation(new TagOperation(TagOperationType.REMOVE, new Tag("friends")));
        descriptor.addTagOperation(new TagOperation(TagOperationType.ADD, new Tag("FRIENDS")));
        Person edited = new PersonBuilder(original).withTags("FRIENDS").build();
        Model expectedModel = new ModelManager(new CoordiMate(model.getCoordiMate()), new UserPrefs());
        expectedModel.setPerson(original, edited);

        assertCommandSuccess(new EditCommand(INDEX_FIRST_PERSON, descriptor), model,
                String.format(EditCommand.MESSAGE_EDIT_PERSON_SUCCESS, Messages.format(edited)), expectedModel);
    }

    @Test
    public void execute_addThenRemoveSameTag_leavesTagAbsent() {
        Person original = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        EditPersonDescriptor descriptor = new EditPersonDescriptor();
        descriptor.addTagOperation(new TagOperation(TagOperationType.ADD, new Tag("Alumni2026")));
        descriptor.addTagOperation(new TagOperation(TagOperationType.REMOVE, new Tag("alumni2026")));
        Model expectedModel = new ModelManager(new CoordiMate(model.getCoordiMate()), new UserPrefs());
        expectedModel.setPerson(original, original);
        expectedModel.registerTag(new Tag("Alumni2026"));

        assertCommandSuccess(new EditCommand(INDEX_FIRST_PERSON, descriptor), model,
                String.format(EditCommand.MESSAGE_EDIT_PERSON_SUCCESS, Messages.format(original)), expectedModel);
        assertTrue(model.getTagList().stream().anyMatch(tag -> tag.isSameTag(new Tag("Alumni2026"))));
    }

    @Test
    public void execute_replaceTags_createsCustomTagAndRemovesOldTags() {
        Person original = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder().withTags("Alumni2026").build();
        Person edited = new PersonBuilder(original).withTags("Alumni2026").build();
        Model expectedModel = new ModelManager(new CoordiMate(model.getCoordiMate()), new UserPrefs());
        expectedModel.setPerson(original, edited);

        assertCommandSuccess(new EditCommand(INDEX_FIRST_PERSON, descriptor), model,
                String.format(EditCommand.MESSAGE_EDIT_PERSON_SUCCESS, Messages.format(edited)), expectedModel);
        assertTrue(model.getTagList().stream().anyMatch(tag -> tag.isSameTag(new Tag("Alumni2026"))));
    }

    @Test
    public void execute_addExistingTagAndRemoveAbsentTag_noDuplicateOrNewTag() {
        Person original = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        EditPersonDescriptor descriptor = new EditPersonDescriptor();
        descriptor.addTagOperation(new TagOperation(TagOperationType.ADD, new Tag("FRIENDS")));
        descriptor.addTagOperation(new TagOperation(TagOperationType.REMOVE, new Tag("Absent")));
        Model expectedModel = new ModelManager(new CoordiMate(model.getCoordiMate()), new UserPrefs());
        expectedModel.setPerson(original, original);

        assertCommandSuccess(new EditCommand(INDEX_FIRST_PERSON, descriptor), model,
                String.format(EditCommand.MESSAGE_EDIT_PERSON_SUCCESS, Messages.format(original)), expectedModel);
    }

    @Test
    public void execute_filteredList_success() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);

        Person personInFilteredList = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        Person editedPerson = new PersonBuilder(personInFilteredList).withName(VALID_NAME_BOB).build();
        EditCommand editCommand = new EditCommand(INDEX_FIRST_PERSON,
                new EditPersonDescriptorBuilder().withName(VALID_NAME_BOB).build());

        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_PERSON_SUCCESS, Messages.format(editedPerson));

        Model expectedModel = new ModelManager(new CoordiMate(model.getCoordiMate()), new UserPrefs());
        expectedModel.setPerson(model.getFilteredPersonList().get(0), editedPerson);

        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_targetNameOutsideDisplayedList_success() {
        assertTargetEditSuccess("  aLiCe pAuLiNe  ");
    }

    @Test
    public void execute_targetEmailOutsideDisplayedList_success() {
        assertTargetEditSuccess("ALICE@EXAMPLE.COM");
    }

    @Test
    public void execute_targetPhoneOutsideDisplayedList_success() {
        assertTargetEditSuccess("(9435) 1253");
    }

    private void assertTargetEditSuccess(String identifier) {
        model.updateFilteredPersonList(person -> false);
        EditCommand editCommand = new EditCommand(identifier,
                new EditPersonDescriptorBuilder().withRole("Logistics Lead").build());
        Person editedPerson = new PersonBuilder(ALICE).withRole("Logistics Lead").build();
        Model expectedModel = new ModelManager(new CoordiMate(model.getCoordiMate()), new UserPrefs());
        expectedModel.setPerson(ALICE, editedPerson);

        assertCommandSuccess(editCommand, model,
                String.format(EditCommand.MESSAGE_EDIT_PERSON_SUCCESS, Messages.format(editedPerson)), expectedModel);
    }

    @Test
    public void execute_targetDoesNotMatch_failure() {
        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder().withRole("Logistics").build();

        assertCommandFailure(new EditCommand("Nobody", descriptor), model, EditCommand.MESSAGE_NO_MATCH);
        assertCommandFailure(new EditCommand("Alice", descriptor), model, EditCommand.MESSAGE_NO_MATCH);
        assertCommandFailure(new EditCommand("9435", descriptor), model, EditCommand.MESSAGE_NO_MATCH);
    }

    @Test
    public void resolveTarget_multipleMatches_listsDetailsAndOnlyDisplayedIndex() {
        Person secondAlice = new PersonBuilder(ALICE).withPhone("22222222")
                .withEmail("other@example.com").build();

        CommandException exception = assertThrows(CommandException.class, () ->
                EditCommand.resolveTarget("Alice Pauline", List.of(ALICE, secondAlice), List.of(secondAlice)));

        assertEquals("Multiple contacts match that identifier:\n"
                + "- Alice Pauline | Phone: 94351253 | Email: alice@example.com\n"
                + "- Alice Pauline | Phone: 22222222 | Email: other@example.com | Current-list index: 1\n"
                + "Retry with a unique phone or email, or a displayed index if shown.", exception.getMessage());
    }

    @Test
    public void execute_duplicatePersonUnfilteredList_failure() {
        Person firstPerson = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder(firstPerson).build();
        EditCommand editCommand = new EditCommand(INDEX_SECOND_PERSON, descriptor);

        assertCommandFailure(editCommand, model, EditCommand.MESSAGE_DUPLICATE_PERSON);
    }

    @Test
    public void execute_duplicatePersonFilteredList_failure() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);

        // edit person in filtered list into a duplicate in CoordiMate
        Person personInList = model.getCoordiMate().getPersonList().get(INDEX_SECOND_PERSON.getZeroBased());
        EditCommand editCommand = new EditCommand(INDEX_FIRST_PERSON,
                new EditPersonDescriptorBuilder(personInList).build());

        assertCommandFailure(editCommand, model, EditCommand.MESSAGE_DUPLICATE_PERSON);
    }

    @Test
    public void execute_sameNameWithDistinctPhoneAndEmail_failure() {
        EditCommand editCommand = new EditCommand(INDEX_FIRST_PERSON,
                new EditPersonDescriptorBuilder().withName("  bEnSoN mEiEr  ").build());

        assertCommandFailure(editCommand, model, EditCommand.MESSAGE_DUPLICATE_PERSON);
    }

    @Test
    public void execute_normalizedPhoneMatchesAnotherContact_noChanges() {
        EditCommand editCommand = new EditCommand(INDEX_FIRST_PERSON,
                new EditPersonDescriptorBuilder().withPhone("9876 5432").withTags("Alumni2026").build());

        assertCommandFailure(editCommand, model, EditCommand.MESSAGE_DUPLICATE_PERSON);
        assertFalse(model.getTagList().stream().anyMatch(tag -> tag.isSameTag(new Tag("Alumni2026"))));
    }

    @Test
    public void execute_caseInsensitiveEmailMatchesAnotherContact_noChanges() {
        EditCommand editCommand = new EditCommand(INDEX_FIRST_PERSON,
                new EditPersonDescriptorBuilder().withEmail("JOHND@EXAMPLE.COM").build());

        assertCommandFailure(editCommand, model, EditCommand.MESSAGE_DUPLICATE_PERSON);
    }

    @Test
    public void execute_renameContact_updatesEveryAssignedEventInPlace() {
        EventTime eventTime = new EventTime("08-08-2026");
        model.addEvent(new Event("Concert", eventTime, eventTime,
                List.of(BENSON.getName(), ALICE.getName())));
        model.addEvent(new Event("Fair", eventTime, eventTime,
                List.of(ALICE.getName(), BENSON.getName())));
        model.addEvent(new Event("Meeting", eventTime, eventTime,
                List.of(BENSON.getName())));
        Person renamedAlice = new PersonBuilder(ALICE).withName("Alice Tan").build();
        EditCommand editCommand = new EditCommand(INDEX_FIRST_PERSON,
                new EditPersonDescriptorBuilder().withName("Alice Tan").build());
        Model expectedModel = new ModelManager(new CoordiMate(model.getCoordiMate()), new UserPrefs());
        expectedModel.setPerson(ALICE, renamedAlice);

        assertCommandSuccess(editCommand, model,
                String.format(EditCommand.MESSAGE_EDIT_PERSON_SUCCESS, Messages.format(renamedAlice)), expectedModel);
        assertEquals(List.of(new Event("Concert", eventTime, eventTime,
                List.of(BENSON.getName(), renamedAlice.getName())),
                new Event("Fair", eventTime, eventTime, List.of(renamedAlice.getName(), BENSON.getName())),
                new Event("Meeting", eventTime, eventTime, List.of(BENSON.getName()))),
                model.getCoordiMate().getEventList());
    }

    @Test
    public void execute_renameConflictsWithEventMember_noChanges() {
        EventTime eventTime = new EventTime("08-08-2026");
        model.addEvent(new Event("Concert", eventTime, eventTime, List.of(ALICE.getName())));
        model.addEvent(new Event("Fair", eventTime, eventTime,
                List.of(ALICE.getName(), new Name("Ghost Member"))));
        EditCommand editCommand = new EditCommand(INDEX_FIRST_PERSON,
                new EditPersonDescriptorBuilder().withName("Ghost Member").withTags("Alumni2026").build());

        assertCommandFailure(editCommand, model, EditCommand.MESSAGE_MEMBER_NAME_CONFLICT);
        assertFalse(model.getTagList().stream().anyMatch(tag -> tag.isSameTag(new Tag("Alumni2026"))));
    }

    @Test
    public void execute_renameToExistingMemberName_rejectedAsDuplicate() {
        Person firstPerson = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        model.addEvent(new Event("Concert", new EventTime("08-08-2026"), new EventTime("08-08-2026"),
                List.of(firstPerson.getName(), BENSON.getName())));
        EditCommand editCommand = new EditCommand(INDEX_FIRST_PERSON,
                new EditPersonDescriptorBuilder().withName(BENSON.getName().toString()).build());

        assertCommandFailure(editCommand, model, EditCommand.MESSAGE_DUPLICATE_PERSON);
    }

    @Test
    public void execute_invalidPersonIndexUnfilteredList_failure() {
        Index outOfBoundIndex = Index.fromOneBased(model.getFilteredPersonList().size() + 1);
        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder().withName(VALID_NAME_BOB).build();
        EditCommand editCommand = new EditCommand(outOfBoundIndex, descriptor);

        assertCommandFailure(editCommand, model,
                String.format(EditCommand.MESSAGE_INVALID_INDEX, outOfBoundIndex.getOneBased()));
    }

    /**
     * Edit filtered list where index is larger than size of filtered list,
     * but smaller than size of CoordiMate.
     */
    @Test
    public void execute_invalidPersonIndexFilteredList_failure() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);
        Index outOfBoundIndex = INDEX_SECOND_PERSON;
        // ensures that outOfBoundIndex is still in bounds of CoordiMate list
        assertTrue(outOfBoundIndex.getZeroBased() < model.getCoordiMate().getPersonList().size());

        EditCommand editCommand = new EditCommand(outOfBoundIndex,
                new EditPersonDescriptorBuilder().withName(VALID_NAME_BOB).build());

        assertCommandFailure(editCommand, model,
                String.format(EditCommand.MESSAGE_INVALID_INDEX, outOfBoundIndex.getOneBased()));
    }

    @Test
    public void equals() {
        final EditCommand standardCommand = new EditCommand(INDEX_FIRST_PERSON, DESC_AMY);

        // same values -> returns true
        EditPersonDescriptor copyDescriptor = new EditPersonDescriptor(DESC_AMY);
        EditCommand commandWithSameValues = new EditCommand(INDEX_FIRST_PERSON, copyDescriptor);
        assertTrue(standardCommand.equals(commandWithSameValues));

        // same object -> returns true
        assertTrue(standardCommand.equals(standardCommand));

        // null -> returns false
        assertFalse(standardCommand.equals(null));

        // different types -> returns false
        assertFalse(standardCommand.equals(new ClearCommand()));

        // different index -> returns false
        assertFalse(standardCommand.equals(new EditCommand(INDEX_SECOND_PERSON, DESC_AMY)));

        // different descriptor -> returns false
        assertFalse(standardCommand.equals(new EditCommand(INDEX_FIRST_PERSON, DESC_BOB)));

        // target identifier and identifier type are part of command identity
        assertEquals(new EditCommand("Alice Pauline", DESC_AMY), new EditCommand("Alice Pauline", DESC_AMY));
        assertFalse(new EditCommand("Alice Pauline", DESC_AMY).equals(new EditCommand("Bob", DESC_AMY)));
        assertFalse(standardCommand.equals(new EditCommand("1", DESC_AMY)));
    }

    @Test
    public void toStringMethod() {
        Index index = Index.fromOneBased(1);
        EditPersonDescriptor editPersonDescriptor = new EditPersonDescriptor();
        EditCommand editCommand = new EditCommand(index, editPersonDescriptor);
        String expected = EditCommand.class.getCanonicalName() + "{index=" + index
                + ", targetIdentifier=null, editPersonDescriptor="
                + editPersonDescriptor + "}";
        assertEquals(expected, editCommand.toString());
    }

}
