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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.Test;

import coordimate.commons.core.index.Index;
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
import coordimate.model.person.MemberId;
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
    public void successMessage_includesUpdatedContactDetails() {
        assertEquals("Contact updated successfully:\n"
                + "Name: Alice Pauline\n"
                + "Phone: 94351253\n"
                + "Email: alice@example.com\n"
                + "Role: NA", EditCommand.successMessage(ALICE));
    }

    @Test
    public void execute_returnsEditedContactForUiFocus() throws Exception {
        Model editModel = new ModelManager(getTypicalCoordiMate(), new UserPrefs());
        Person originalPerson = editModel.getFilteredPersonList().getFirst();
        Person editedPerson = new PersonBuilder(originalPerson).withName("Alice Tan").build();
        EditCommand command = new EditCommand(originalPerson.getMemberId().orElseThrow(),
                new EditPersonDescriptorBuilder().withName("Alice Tan").build());

        CommandResult result = command.execute(editModel);

        assertEquals(editedPerson, result.getContactToFocus().orElseThrow());
        assertTrue(result.getContactIndexToFocus().isEmpty());
    }

    @Test
    public void execute_setAndClearLeave_success() {
        String future = LocalDate.now().plusDays(30).format(DateTimeFormatter.ofPattern("dd-MM-uuuu"));
        Person firstPerson = model.getFilteredPersonList().get(0);
        Person onLeave = new PersonBuilder(firstPerson).withLeaveDate(future).build();
        EditCommand setLeave = new EditCommand(firstPerson.getMemberId().orElseThrow(),
                new EditPersonDescriptorBuilder().withLeaveDate(future).build());
        Model expectedModel = new ModelManager(new CoordiMate(model.getCoordiMate()), new UserPrefs());
        expectedModel.setPerson(firstPerson, onLeave);

        assertCommandSuccess(setLeave, model, EditCommand.successMessage(onLeave), expectedModel);

        EditCommand clearLeave = new EditCommand(firstPerson.getMemberId().orElseThrow(),
                new EditPersonDescriptorBuilder().withoutLeaveDate().build());
        expectedModel.setPerson(onLeave, firstPerson);

        assertCommandSuccess(clearLeave, model, EditCommand.successMessage(firstPerson), expectedModel);
    }

    @Test
    public void execute_otherFieldEdited_keepsLeave() {
        Person firstPerson = model.getFilteredPersonList().get(0);
        Person endedLeave = new PersonBuilder(firstPerson).withLeaveDate("01-01-2020").build();
        model.setPerson(firstPerson, endedLeave);
        Person edited = new PersonBuilder(endedLeave).withRole("Logistics").build();
        EditCommand editRole = new EditCommand(firstPerson.getMemberId().orElseThrow(),
                new EditPersonDescriptorBuilder().withRole("Logistics").build());
        Model expectedModel = new ModelManager(new CoordiMate(model.getCoordiMate()), new UserPrefs());
        expectedModel.setPerson(endedLeave, edited);

        assertCommandSuccess(editRole, model, EditCommand.successMessage(edited), expectedModel);
    }

    @Test
    public void execute_allFieldsSpecifiedUnfilteredList_success() {
        Person editedPerson = new PersonBuilder().build();
        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder(editedPerson).build();
        EditCommand editCommand = new EditCommand(new MemberId(1), descriptor);

        String expectedMessage = EditCommand.successMessage(editedPerson);

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
        EditCommand editCommand = new EditCommand(lastPerson.getMemberId().orElseThrow(), descriptor);

        String expectedMessage = EditCommand.successMessage(editedPerson);

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
        EditCommand editCommand = new EditCommand(new MemberId(1),
                new EditPersonDescriptorBuilder().withAddress(VALID_ADDRESS_BOB).build());
        Person editedPerson = new PersonBuilder(contactWithDetails).withAddress(VALID_ADDRESS_BOB).build();
        Model expectedModel = new ModelManager(new CoordiMate(model.getCoordiMate()), new UserPrefs());
        expectedModel.setPerson(contactWithDetails, editedPerson);

        assertCommandSuccess(editCommand, model,
                EditCommand.successMessage(editedPerson), expectedModel);
    }

    @Test
    public void execute_editExpandedFields_preservesUnspecifiedDetails() {
        Person originalPerson = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        Person contactWithDetails = new PersonBuilder(originalPerson).withBirthday("18-06-2004")
                .withAddress("21 Kent Ridge Road").withNote("Original note").build();
        model.setPerson(originalPerson, contactWithDetails);
        EditCommand editCommand = new EditCommand(new MemberId(1),
                new EditPersonDescriptorBuilder().withRole("Logistics Lead")
                        .withOrganisation("NUS Student Affairs").withNote("New note").build());
        Person editedPerson = new PersonBuilder(contactWithDetails).withRole("Logistics Lead")
                .withOrganisation("NUS Student Affairs").withNote("New note").build();
        Model expectedModel = new ModelManager(new CoordiMate(model.getCoordiMate()), new UserPrefs());
        expectedModel.setPerson(contactWithDetails, editedPerson);

        assertCommandSuccess(editCommand, model,
                EditCommand.successMessage(editedPerson), expectedModel);
    }

    @Test
    public void execute_clearOptionalDetails_removesOnlySelectedValues() {
        Person originalPerson = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        Person contactWithDetails = new PersonBuilder(originalPerson).withBirthday("18-06-2004")
                .withOrganisation("NUS Student Affairs").withNote("Original note").build();
        model.setPerson(originalPerson, contactWithDetails);
        EditCommand editCommand = new EditCommand(new MemberId(1),
                new EditPersonDescriptorBuilder().withoutBirthday().withoutAddress()
                        .withoutOrganisation().withoutNote().build());
        Person editedPerson = new PersonBuilder(contactWithDetails).withoutBirthday().withoutAddress()
                .withoutOrganisation().withoutNote().build();
        Model expectedModel = new ModelManager(new CoordiMate(model.getCoordiMate()), new UserPrefs());
        expectedModel.setPerson(contactWithDetails, editedPerson);

        assertCommandSuccess(editCommand, model,
                EditCommand.successMessage(editedPerson), expectedModel);
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
        EditCommand editCommand = new EditCommand(new MemberId(1),
                new EditPersonDescriptorBuilder().withName("Alice Tan").build());
        Person editedPerson = new PersonBuilder(contactWithoutAddress).withName("Alice Tan").build();
        Model expectedModel = new ModelManager(new CoordiMate(model.getCoordiMate()), new UserPrefs());
        expectedModel.setPerson(contactWithoutAddress, editedPerson);
        String expectedMessage = EditCommand.successMessage(editedPerson);

        assertTrue(editedPerson.getAddress().isEmpty());
        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_noFieldSpecifiedUnfilteredList_success() {
        EditCommand editCommand = new EditCommand(new MemberId(1), new EditPersonDescriptor());
        Person editedPerson = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());

        String expectedMessage = EditCommand.successMessage(editedPerson);

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

        assertCommandSuccess(new EditCommand(new MemberId(1), descriptor), model,
                EditCommand.successMessage(edited), expectedModel);
        assertTrue(model.getTagList().stream().anyMatch(tag -> tag.isSameTag(new Tag("Alumni2026"))));
    }

    @Test
    public void execute_removeTag_keepsTagInRegistry() {
        CoordiMate savedData = new CoordiMate(model.getCoordiMate());
        model = new ModelManager(savedData, new UserPrefs());
        Person original = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        EditPersonDescriptor descriptor = new EditPersonDescriptor();
        descriptor.addTagOperation(new TagOperation(TagOperationType.REMOVE, new Tag("FRIENDS")));
        Person edited = new PersonBuilder(original).withTags().build();
        Model expectedModel = new ModelManager(new CoordiMate(model.getCoordiMate()), new UserPrefs());
        expectedModel.setPerson(original, edited);

        assertCommandSuccess(new EditCommand(new MemberId(1), descriptor), model,
                EditCommand.successMessage(edited), expectedModel);
        assertTrue(model.getTagList().stream().anyMatch(tag -> tag.isSameTag(new Tag("friends"))));
    }

    @Test
    public void execute_tagOperations_applyInCommandOrder() {
        Person original = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        EditPersonDescriptor descriptor = new EditPersonDescriptor();
        descriptor.addTagOperation(new TagOperation(TagOperationType.REMOVE, new Tag("friends")));
        descriptor.addTagOperation(new TagOperation(TagOperationType.ADD, new Tag("FRIENDS")));
        Person edited = new PersonBuilder(original).withTags("friends").build();
        Model expectedModel = new ModelManager(new CoordiMate(model.getCoordiMate()), new UserPrefs());
        expectedModel.setPerson(original, edited);

        assertCommandSuccess(new EditCommand(new MemberId(1), descriptor), model,
                EditCommand.successMessage(edited), expectedModel);
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

        assertCommandSuccess(new EditCommand(new MemberId(1), descriptor), model,
                EditCommand.successMessage(original), expectedModel);
        assertTrue(model.getTagList().stream().anyMatch(tag -> tag.isSameTag(new Tag("Alumni2026"))));
    }

    @Test
    public void execute_replaceTags_createsCustomTagAndRemovesOldTags() {
        Person original = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder().withTags("Alumni2026").build();
        Person edited = new PersonBuilder(original).withTags("Alumni2026").build();
        Model expectedModel = new ModelManager(new CoordiMate(model.getCoordiMate()), new UserPrefs());
        expectedModel.setPerson(original, edited);

        assertCommandSuccess(new EditCommand(new MemberId(1), descriptor), model,
                EditCommand.successMessage(edited), expectedModel);
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

        assertCommandSuccess(new EditCommand(new MemberId(1), descriptor), model,
                EditCommand.successMessage(original), expectedModel);
    }

    @Test
    public void execute_filteredList_success() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);

        Person personInFilteredList = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        Person editedPerson = new PersonBuilder(personInFilteredList).withName(VALID_NAME_BOB).build();
        EditCommand editCommand = new EditCommand(new MemberId(1),
                new EditPersonDescriptorBuilder().withName(VALID_NAME_BOB).build());

        String expectedMessage = EditCommand.successMessage(editedPerson);

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
                EditCommand.successMessage(editedPerson), expectedModel);
    }

    @Test
    public void execute_targetDoesNotMatch_failure() {
        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder().withRole("Logistics").build();

        assertCommandFailure(new EditCommand("Nobody", descriptor), model, EditCommand.MESSAGE_NO_MATCH);
        assertCommandFailure(new EditCommand("Alice", descriptor), model, EditCommand.MESSAGE_NO_MATCH);
        assertCommandFailure(new EditCommand("9435", descriptor), model, EditCommand.MESSAGE_NO_MATCH);
    }

    @Test
    public void resolveTarget_multipleMatches_listsDetailsAndPermanentIds() {
        Person secondAlice = new PersonBuilder(ALICE).withMemberId(12).withPhone("22222222")
                .withEmail("other@example.com").build();

        CommandException exception = assertThrows(CommandException.class, () ->
                EditCommand.resolveTarget("Alice Pauline", List.of(ALICE, secondAlice)));

        assertEquals("Multiple contacts match that identifier:\n"
                + "- Alice Pauline | ID: 1 | Phone: 94351253 | Email: alice@example.com\n"
                + "- Alice Pauline | ID: 12 | Phone: 22222222 | Email: other@example.com\n"
                + "Use a member ID, phone number, or email address to identify the contact.",
                exception.getMessage());
    }

    @Test
    public void execute_duplicatePersonUnfilteredList_failure() {
        Person firstPerson = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder(firstPerson).build();
        EditCommand editCommand = new EditCommand(new MemberId(2), descriptor);

        assertCommandFailure(editCommand, model, EditCommand.MESSAGE_DUPLICATE_PERSON);
    }

    @Test
    public void execute_duplicatePersonFilteredList_failure() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);

        // edit person in filtered list into a duplicate in CoordiMate
        Person personInList = model.getCoordiMate().getPersonList().get(INDEX_SECOND_PERSON.getZeroBased());
        EditCommand editCommand = new EditCommand(new MemberId(1),
                new EditPersonDescriptorBuilder(personInList).build());

        assertCommandFailure(editCommand, model, EditCommand.MESSAGE_DUPLICATE_PERSON);
    }

    @Test
    public void execute_sameNameWithDistinctPhoneAndEmail_failure() {
        EditCommand editCommand = new EditCommand(new MemberId(1),
                new EditPersonDescriptorBuilder().withName("  bEnSoN mEiEr  ").build());

        assertCommandFailure(editCommand, model, EditCommand.MESSAGE_DUPLICATE_PERSON);
    }

    @Test
    public void execute_normalizedPhoneMatchesAnotherContact_noChanges() {
        EditCommand editCommand = new EditCommand(new MemberId(1),
                new EditPersonDescriptorBuilder().withPhone("9876 5432").withTags("Alumni2026").build());

        assertCommandFailure(editCommand, model, EditCommand.MESSAGE_DUPLICATE_PERSON);
        assertFalse(model.getTagList().stream().anyMatch(tag -> tag.isSameTag(new Tag("Alumni2026"))));
    }

    @Test
    public void execute_caseInsensitiveEmailMatchesAnotherContact_noChanges() {
        EditCommand editCommand = new EditCommand(new MemberId(1),
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
        EditCommand editCommand = new EditCommand(new MemberId(1),
                new EditPersonDescriptorBuilder().withName("Alice Tan").build());
        Model expectedModel = new ModelManager(new CoordiMate(model.getCoordiMate()), new UserPrefs());
        expectedModel.setPerson(ALICE, renamedAlice);

        assertCommandSuccess(editCommand, model,
                EditCommand.successMessage(renamedAlice), expectedModel);
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
        EditCommand editCommand = new EditCommand(new MemberId(1),
                new EditPersonDescriptorBuilder().withName("Ghost Member").withTags("Alumni2026").build());

        assertCommandFailure(editCommand, model, EditCommand.MESSAGE_MEMBER_NAME_CONFLICT);
        assertFalse(model.getTagList().stream().anyMatch(tag -> tag.isSameTag(new Tag("Alumni2026"))));
    }

    @Test
    public void execute_renameToExistingMemberName_rejectedAsDuplicate() {
        Person firstPerson = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        model.addEvent(new Event("Concert", new EventTime("08-08-2026"), new EventTime("08-08-2026"),
                List.of(firstPerson.getName(), BENSON.getName())));
        EditCommand editCommand = new EditCommand(new MemberId(1),
                new EditPersonDescriptorBuilder().withName(BENSON.getName().toString()).build());

        assertCommandFailure(editCommand, model, EditCommand.MESSAGE_DUPLICATE_PERSON);
    }

    @Test
    public void execute_unknownMemberIdUnfilteredList_failure() {
        MemberId unknownId = new MemberId(99);
        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder().withName(VALID_NAME_BOB).build();
        EditCommand editCommand = new EditCommand(unknownId, descriptor);

        assertCommandFailure(editCommand, model,
                String.format(EditCommand.MESSAGE_UNKNOWN_MEMBER_ID, unknownId.getValue()));
    }

    @Test
    public void execute_memberHiddenByFilter_success() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);
        Person edited = new PersonBuilder(BENSON).withRole("President").build();
        Model expectedModel = new ModelManager(model.getCoordiMate(), new UserPrefs());
        expectedModel.setPerson(BENSON, edited);
        assertCommandSuccess(new EditCommand(new MemberId(2),
                new EditPersonDescriptorBuilder().withRole("President").build()), model,
                EditCommand.successMessage(edited), expectedModel);
    }

    @Test
    public void equals() {
        final EditCommand standardCommand = new EditCommand(new MemberId(1), DESC_AMY);

        // same values -> returns true
        EditPersonDescriptor copyDescriptor = new EditPersonDescriptor(DESC_AMY);
        EditCommand commandWithSameValues = new EditCommand(new MemberId(1), copyDescriptor);
        assertTrue(standardCommand.equals(commandWithSameValues));

        // same object -> returns true
        assertTrue(standardCommand.equals(standardCommand));

        // null -> returns false
        assertFalse(standardCommand.equals(null));

        // different types -> returns false
        assertFalse(standardCommand.equals(new ClearCommand()));

        // different member ID -> returns false
        assertFalse(standardCommand.equals(new EditCommand(new MemberId(2), DESC_AMY)));

        // different descriptor -> returns false
        assertFalse(standardCommand.equals(new EditCommand(new MemberId(1), DESC_BOB)));

        // target identifier and identifier type are part of command identity
        assertEquals(new EditCommand("Alice Pauline", DESC_AMY), new EditCommand("Alice Pauline", DESC_AMY));
        assertFalse(new EditCommand("Alice Pauline", DESC_AMY).equals(new EditCommand("Bob", DESC_AMY)));
        assertFalse(standardCommand.equals(new EditCommand("1", DESC_AMY)));
    }

    @Test
    public void toStringMethod() {
        MemberId memberId = new MemberId(1);
        EditPersonDescriptor editPersonDescriptor = new EditPersonDescriptor();
        EditCommand editCommand = new EditCommand(memberId, editPersonDescriptor);
        String expected = EditCommand.class.getCanonicalName() + "{memberId=" + memberId
                + ", targetIdentifier=null, editPersonDescriptor="
                + editPersonDescriptor + "}";
        assertEquals(expected, editCommand.toString());
    }

}
