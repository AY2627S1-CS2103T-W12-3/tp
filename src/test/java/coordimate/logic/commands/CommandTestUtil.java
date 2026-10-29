package coordimate.logic.commands;

import static coordimate.logic.parser.CliSyntax.PREFIX_ADDRESS;
import static coordimate.logic.parser.CliSyntax.PREFIX_EMAIL;
import static coordimate.logic.parser.CliSyntax.PREFIX_NAME;
import static coordimate.logic.parser.CliSyntax.PREFIX_PHONE;
import static coordimate.logic.parser.CliSyntax.PREFIX_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import coordimate.commons.core.index.Index;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.CoordiMate;
import coordimate.model.Model;
import coordimate.model.person.ContactMatchesKeywordPredicate;
import coordimate.model.person.Person;
import coordimate.testutil.EditPersonDescriptorBuilder;

/**
 * Contains helper methods for testing commands.
 */
public class CommandTestUtil {

    public static final String VALID_NAME_AMY = "Amy Bee";
    public static final String VALID_NAME_BOB = "Bob Choo";
    public static final String VALID_PHONE_AMY = "11111111";
    public static final String VALID_PHONE_BOB = "22222222";
    public static final String VALID_EMAIL_AMY = "amy@example.com";
    public static final String VALID_EMAIL_BOB = "bob@example.com";
    public static final String VALID_ADDRESS_AMY = "Block 312, Amy Street 1";
    public static final String VALID_ADDRESS_BOB = "Block 123, Bobby Street 3";
    public static final String VALID_TAG_HUSBAND = "husband";
    public static final String VALID_TAG_FRIEND = "friend";

    public static final String NAME_DESC_AMY = " " + PREFIX_NAME + VALID_NAME_AMY;
    public static final String NAME_DESC_BOB = " " + PREFIX_NAME + VALID_NAME_BOB;
    public static final String PHONE_DESC_AMY = " " + PREFIX_PHONE + VALID_PHONE_AMY;
    public static final String PHONE_DESC_BOB = " " + PREFIX_PHONE + VALID_PHONE_BOB;
    public static final String EMAIL_DESC_AMY = " " + PREFIX_EMAIL + VALID_EMAIL_AMY;
    public static final String EMAIL_DESC_BOB = " " + PREFIX_EMAIL + VALID_EMAIL_BOB;
    public static final String ADDRESS_DESC_AMY = " " + PREFIX_ADDRESS + VALID_ADDRESS_AMY;
    public static final String ADDRESS_DESC_BOB = " " + PREFIX_ADDRESS + VALID_ADDRESS_BOB;
    public static final String TAG_DESC_FRIEND = " " + PREFIX_TAG + VALID_TAG_FRIEND;
    public static final String TAG_DESC_HUSBAND = " " + PREFIX_TAG + VALID_TAG_HUSBAND;

    public static final String INVALID_NAME_DESC = " " + PREFIX_NAME + "James&"; // '&' not allowed in names
    public static final String INVALID_PHONE_DESC = " " + PREFIX_PHONE + "911a"; // 'a' not allowed in phones
    public static final String INVALID_EMAIL_DESC = " " + PREFIX_EMAIL + "bob!yahoo"; // missing '@' symbol
    public static final String INVALID_ADDRESS_DESC = " " + PREFIX_ADDRESS; // empty string not allowed for addresses
    public static final String INVALID_TAG_DESC = " " + PREFIX_TAG + "hubby*"; // '*' not allowed in tags

    public static final String PREAMBLE_WHITESPACE = "\t  \r  \n";
    public static final String PREAMBLE_NON_EMPTY = "NonEmptyPreamble";

    public static final EditCommand.EditPersonDescriptor DESC_AMY;
    public static final EditCommand.EditPersonDescriptor DESC_BOB;

    static {
        DESC_AMY = new EditPersonDescriptorBuilder().withName(VALID_NAME_AMY)
                .withPhone(VALID_PHONE_AMY).withEmail(VALID_EMAIL_AMY).withAddress(VALID_ADDRESS_AMY)
                .withTags(VALID_TAG_FRIEND).build();
        DESC_BOB = new EditPersonDescriptorBuilder().withName(VALID_NAME_BOB)
                .withPhone(VALID_PHONE_BOB).withEmail(VALID_EMAIL_BOB).withAddress(VALID_ADDRESS_BOB)
                .withTags(VALID_TAG_HUSBAND, VALID_TAG_FRIEND).build();
    }

    /**
     * Executes the given {@code command}, confirms that <br>
     * - the returned {@link CommandResult} matches its expected user-visible fields and focus target <br>
     * - the {@code actualModel} matches {@code expectedModel}.
     */
    public static void assertCommandSuccess(Command command, Model actualModel, CommandResult expectedCommandResult,
            Model expectedModel) {
        try {
            CommandResult result = command.execute(actualModel);
            assertEquals(expectedCommandResult.getFeedbackToUser(), result.getFeedbackToUser());
            assertEquals(expectedCommandResult.shouldShowHelp(), result.shouldShowHelp());
            assertEquals(expectedCommandResult.shouldExit(), result.shouldExit());
            assertEquals(expectedCommandResult.shouldShowTutorial(), result.shouldShowTutorial());
            assertEquals(expectedCommandResult.getViewToShow(), result.getViewToShow());
            assertEquals(expectedCommandResult.isWarning(), result.isWarning());
            assertEquals(expectedCommandResult.getContactToFocus(), result.getContactToFocus());
            assertEquals(expectedCommandResult.getContactIndexToFocus(), result.getContactIndexToFocus());
            assertEquals(expectedModel, actualModel);
        } catch (CommandException ce) {
            throw new AssertionError("Execution of command should not fail.", ce);
        }
    }

    /**
     * Convenience wrapper to {@link #assertCommandSuccess(Command, Model, CommandResult, Model)}
     * that takes a string {@code expectedMessage}. The expected focus target is derived only where it is
     * deterministic from {@code command}, a pre-execution snapshot of {@code actualModel}, and the
     * independently constructed {@code expectedModel}; every other command is expected to request no focus.
     */
    public static void assertCommandSuccess(Command command, Model actualModel, String expectedMessage,
            Model expectedModel) {
        CommandResult.View view = expectedViewFor(command);
        CommandResult expectedCommandResult;
        if (command instanceof AddCommand) {
            List<Person> contactsBefore = new ArrayList<>(actualModel.getCoordiMate().getPersonList());
            Person addedContact = expectedModel.getCoordiMate().getPersonList().stream()
                    .filter(person -> !contactsBefore.contains(person))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "expectedModel must contain exactly one newly saved contact to derive focus."));
            expectedCommandResult = CommandResult.withContactToFocus(expectedMessage, view, addedContact);
        } else if (command instanceof DeleteCommand deleteCommand) {
            List<Person> displayedBefore = new ArrayList<>(actualModel.getFilteredPersonList());
            int deletedIndex = deletedContactIndex(deleteCommand, actualModel, displayedBefore);
            expectedCommandResult = deletedIndex < 0
                    ? new CommandResult(expectedMessage, view)
                    : CommandResult.withContactIndexToFocus(expectedMessage, view, deletedIndex);
        } else {
            expectedCommandResult = new CommandResult(expectedMessage, view);
        }
        assertCommandSuccess(command, actualModel, expectedCommandResult, expectedModel);
    }

    private static int deletedContactIndex(DeleteCommand deleteCommand, Model model, List<Person> displayedBefore) {
        try {
            return displayedBefore.indexOf(deleteCommand.resolvePerson(model));
        } catch (CommandException e) {
            throw new AssertionError("The target of a successful deletion should resolve.", e);
        }
    }

    private static CommandResult.View expectedViewFor(Command command) {
        return switch (command) {
            case AddCommand _, EditCommand _, DeleteCommand _, ListCommand _, SearchCommand _, FilterCommand _,
                MembersCommand _ -> CommandResult.View.CONTACTS;
            case AddEventCommand _, EditEventCommand _, DeleteEventCommand _, MarkAttendanceCommand _ ->
                CommandResult.View.EVENTS;
            case MutatingTagCommand _ -> CommandResult.View.TAGS;
            default -> CommandResult.View.NONE;
        };
    }

    /**
     * Executes the given {@code command}, confirms that <br>
     * - a {@code CommandException} is thrown <br>
     * - the CommandException message matches {@code expectedMessage} <br>
     * - the CoordiMate, filtered person list and selected person in {@code actualModel} remain unchanged.
     */
    public static void assertCommandFailure(Command command, Model actualModel, String expectedMessage) {
        // we are unable to defensively copy the model for comparison later, so we can
        // only do so by copying its components.
        CoordiMate expectedCoordiMate = new CoordiMate(actualModel.getCoordiMate());
        List<Person> expectedFilteredList = new ArrayList<>(actualModel.getFilteredPersonList());

        CommandException exception = assertThrows(CommandException.class, () -> command.execute(actualModel));
        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(expectedCoordiMate, actualModel.getCoordiMate());
        assertEquals(expectedFilteredList, actualModel.getFilteredPersonList());
    }
    /**
     * Updates {@code model}'s filtered list to show only the person at the given {@code targetIndex} in the
     * {@code model}'s CoordiMate.
     */
    public static void showPersonAtIndex(Model model, Index targetIndex) {
        assertTrue(targetIndex.getZeroBased() < model.getFilteredPersonList().size());

        Person person = model.getFilteredPersonList().get(targetIndex.getZeroBased());
        model.updateFilteredPersonList(new ContactMatchesKeywordPredicate(person.getName().getFullName()));

        assertEquals(1, model.getFilteredPersonList().size());
    }

}
