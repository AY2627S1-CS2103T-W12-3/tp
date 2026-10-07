package coordimate.logic.commands;

import static coordimate.logic.commands.CommandTestUtil.assertCommandFailure;
import static coordimate.logic.commands.CommandTestUtil.assertCommandSuccess;
import static coordimate.testutil.TypicalPersons.getTypicalCoordiMate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import coordimate.model.Model;
import coordimate.model.ModelManager;
import coordimate.model.UserPrefs;
import coordimate.model.person.Person;
import coordimate.testutil.PersonBuilder;

/**
 * Contains integration tests (interaction with the Model) for {@code AddCommand}.
 */
public class AddCommandIntegrationTest {

    private Model model;

    @BeforeEach
    public void setUp() {
        model = new ModelManager(getTypicalCoordiMate(), new UserPrefs());
    }

    @Test
    public void execute_newPerson_success() {
        Person validPerson = new PersonBuilder().build();

        Model expectedModel = new ModelManager(model.getCoordiMate(), new UserPrefs());
        expectedModel.addPerson(validPerson);

        assertCommandSuccess(new AddCommand(validPerson), model,
                String.format(AddCommand.MESSAGE_SUCCESS, validPerson.getName()),
                expectedModel);
    }

    @Test
    public void execute_duplicatePerson_throwsCommandException() {
        Person personInList = model.getCoordiMate().getPersonList().get(0);
        assertCommandFailure(new AddCommand(personInList), model,
                AddCommand.MESSAGE_DUPLICATE_PERSON);
    }

    @Test
    public void execute_sameNameOutsideFilteredList_failure() {
        model.updateFilteredPersonList(person -> false);
        Person differentPerson = new PersonBuilder().withName("  aLiCe pAuLiNe  ").build();

        assertCommandFailure(new AddCommand(differentPerson), model, AddCommand.MESSAGE_DUPLICATE_PERSON);
    }

    @Test
    public void execute_normalizedPhoneOrEmailDuplicate_failure() {
        Person samePhone = new PersonBuilder().withPhone("(9435) 1253").build();
        Person sameEmail = new PersonBuilder().withEmail("ALICE@EXAMPLE.COM").build();

        assertCommandFailure(new AddCommand(samePhone), model, AddCommand.MESSAGE_DUPLICATE_PERSON);
        assertCommandFailure(new AddCommand(sameEmail), model, AddCommand.MESSAGE_DUPLICATE_PERSON);
    }

}
