package coordimate.logic.commands;

import static coordimate.logic.commands.CommandTestUtil.assertCommandFailure;
import static coordimate.logic.commands.CommandTestUtil.assertCommandSuccess;
import static coordimate.testutil.Assert.assertThrows;
import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BENSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import coordimate.model.CoordiMate;
import coordimate.model.Model;
import coordimate.model.ModelManager;
import coordimate.model.UserPrefs;
import coordimate.model.person.Person;
import coordimate.model.tag.Tag;
import coordimate.testutil.PersonBuilder;

public class DeleteTagCommandTest {

    private static final Tag PUBLICITY = new Tag("Publicity");

    @Test
    public void execute_existingTag_deletesTagAndUpdatesContacts() {
        CoordiMate data = new CoordiMate();
        data.addTag(PUBLICITY);
        Person taggedAlice = new PersonBuilder(ALICE).withTags("Publicity", "EXCO").build();
        data.addPerson(taggedAlice);
        data.addPerson(BENSON);
        Model model = new ModelManager(data, new UserPrefs());
        Model expectedModel = new ModelManager(data, new UserPrefs());
        expectedModel.deleteTag(PUBLICITY);

        CommandResult expectedResult = new CommandResult(
                "Publicity successfully deleted.", false, false, true);
        assertCommandSuccess(new DeleteTagCommand("publicity"), model, expectedResult, expectedModel);
        assertEquals(List.of("EXCO"), model.getFilteredPersonList().get(0).getTags().stream()
                .map(Tag::getTagName).toList());
        assertEquals(BENSON, model.getFilteredPersonList().get(1));
    }

    @Test
    public void execute_missingTag_throwsCommandException() {
        Model model = new ModelManager();
        assertCommandFailure(new DeleteTagCommand("Missing"), model,
                "No such tag exists: Missing.");
    }

    @Test
    public void equals() {
        DeleteTagCommand command = new DeleteTagCommand("Publicity");
        DeleteTagCommand sameCommand = new DeleteTagCommand("Publicity");

        assertEquals(command, sameCommand);
        assertEquals(command.hashCode(), sameCommand.hashCode());
        assertNotEquals(command, new DeleteTagCommand("Media"));
        assertNotEquals(command, null);
        assertNotEquals(command, new Object());
    }

    @Test
    public void nullArguments_throwNullPointerException() {
        assertThrows(NullPointerException.class, () -> new DeleteTagCommand(null));
        assertThrows(NullPointerException.class, () -> new DeleteTagCommand("Publicity").execute(null));
    }
}
