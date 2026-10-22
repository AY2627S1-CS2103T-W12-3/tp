package coordimate.logic.commands;

import static coordimate.testutil.TypicalPersons.AMY;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import coordimate.model.ModelManager;
import coordimate.model.person.MemberId;
import coordimate.model.person.Person;
import coordimate.model.person.Role;

public class ContactCommandInvariantTest {
    @Test
    public void execute_addModelDoesNotAddContact_triggersAssertion() {
        assertThrows(AssertionError.class, () -> new AddCommand(AMY).execute(new ModelWithoutAddedContact()));
    }

    @Test
    public void execute_editModelDoesNotReplaceContact_triggersAssertion() {
        ModelWithoutEditedContact model = new ModelWithoutEditedContact();
        model.addPerson(AMY);
        EditCommand.EditPersonDescriptor descriptor = new EditCommand.EditPersonDescriptor();
        descriptor.setRole(new Role("President"));
        EditCommand command = new EditCommand(new MemberId(1), descriptor);

        assertThrows(AssertionError.class, () -> command.execute(model));
    }

    @Test
    public void execute_deleteModelDoesNotRemoveContact_triggersAssertion() {
        ModelWithoutDeletedContact model = new ModelWithoutDeletedContact();
        model.addPerson(AMY);
        DeleteCommand command = new DeleteCommand(new MemberId(1));

        assertThrows(AssertionError.class, () -> command.execute(model));
    }

    private static final class ModelWithoutAddedContact extends ModelManager {
        @Override
        public void addPerson(Person person) {
            // Simulates a broken model implementation that violates the add contract.
        }
    }

    private static final class ModelWithoutEditedContact extends ModelManager {
        @Override
        public void setPerson(Person target, Person editedPerson) {
            // Simulates a broken model implementation that violates the edit contract.
        }
    }

    private static final class ModelWithoutDeletedContact extends ModelManager {
        @Override
        public void deletePerson(Person target) {
            // Simulates a broken model implementation that violates the delete contract.
        }
    }
}
