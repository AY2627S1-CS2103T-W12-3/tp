package coordimate.logic.commands;

import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BENSON;
import static coordimate.testutil.TypicalPersons.getTypicalCoordiMate;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import coordimate.model.Model;
import coordimate.model.ModelManager;
import coordimate.model.UserPrefs;

/**
 * Regression tests for {@link CommandTestUtil#assertCommandSuccess(Command, Model, CommandResult, Model)},
 * confirming that it rejects a wrong expected focus rather than inferring or wildcarding one.
 */
public class CommandTestUtilTest {

    private final Model actualModel = new ModelManager(getTypicalCoordiMate(), new UserPrefs());
    private final Model expectedModel = new ModelManager(getTypicalCoordiMate(), new UserPrefs());

    /** A test-only command that returns a fixed result without mutating the model it executes on. */
    private static final class StubCommand extends Command {
        private final CommandResult result;

        private StubCommand(CommandResult result) {
            this.result = result;
        }

        @Override
        public CommandResult execute(Model model) {
            return result;
        }
    }

    @Test
    public void assertCommandSuccess_wrongFocusedContactAmongExistingContacts_rejected() {
        CommandResult actual = CommandResult.withContactToFocus("msg", CommandResult.View.CONTACTS, BENSON);
        CommandResult expected = CommandResult.withContactToFocus("msg", CommandResult.View.CONTACTS, ALICE);

        assertThrows(AssertionError.class, () ->
                CommandTestUtil.assertCommandSuccess(new StubCommand(actual), actualModel, expected, expectedModel));
    }

    @Test
    public void assertCommandSuccess_wrongIndex_rejected() {
        CommandResult actual = CommandResult.withContactIndexToFocus("msg", CommandResult.View.CONTACTS, 0);
        CommandResult expected = CommandResult.withContactIndexToFocus("msg", CommandResult.View.CONTACTS, 1);

        assertThrows(AssertionError.class, () ->
                CommandTestUtil.assertCommandSuccess(new StubCommand(actual), actualModel, expected, expectedModel));
    }

    @Test
    public void assertCommandSuccess_actualEmptyExpectedPresent_rejected() {
        CommandResult actual = new CommandResult("msg", CommandResult.View.CONTACTS);
        CommandResult expected = CommandResult.withContactToFocus("msg", CommandResult.View.CONTACTS, ALICE);

        assertThrows(AssertionError.class, () ->
                CommandTestUtil.assertCommandSuccess(new StubCommand(actual), actualModel, expected, expectedModel));
    }

    @Test
    public void assertCommandSuccess_actualPresentExpectedEmpty_rejected() {
        CommandResult actual = CommandResult.withContactToFocus("msg", CommandResult.View.CONTACTS, ALICE);
        CommandResult expected = new CommandResult("msg", CommandResult.View.CONTACTS);

        assertThrows(AssertionError.class, () ->
                CommandTestUtil.assertCommandSuccess(new StubCommand(actual), actualModel, expected, expectedModel));
    }

    @Test
    public void assertCommandSuccess_exactContactMatch_passes() {
        CommandResult result = CommandResult.withContactToFocus("msg", CommandResult.View.CONTACTS, ALICE);

        CommandTestUtil.assertCommandSuccess(new StubCommand(result), actualModel, result, expectedModel);
    }

    @Test
    public void assertCommandSuccess_exactIndexMatch_passes() {
        CommandResult result = CommandResult.withContactIndexToFocus("msg", CommandResult.View.CONTACTS, 1);

        CommandTestUtil.assertCommandSuccess(new StubCommand(result), actualModel, result, expectedModel);
    }

    @Test
    public void assertCommandSuccess_bothEmptyFocus_passes() {
        CommandResult result = new CommandResult("msg", CommandResult.View.CONTACTS);

        CommandTestUtil.assertCommandSuccess(new StubCommand(result), actualModel, result, expectedModel);
    }

    @Test
    public void assertCommandSuccess_feedbackMismatch_stillRejected() {
        CommandResult actual = new CommandResult("actual", CommandResult.View.CONTACTS);
        CommandResult expected = new CommandResult("expected", CommandResult.View.CONTACTS);

        assertThrows(AssertionError.class, () ->
                CommandTestUtil.assertCommandSuccess(new StubCommand(actual), actualModel, expected, expectedModel));
    }

    @Test
    public void assertCommandSuccess_modelMismatch_stillRejected() {
        CommandResult result = new CommandResult("msg", CommandResult.View.CONTACTS);
        Model differentExpectedModel = new ModelManager();

        assertThrows(AssertionError.class, () ->
                CommandTestUtil.assertCommandSuccess(
                        new StubCommand(result), actualModel, result, differentExpectedModel));
    }
}
