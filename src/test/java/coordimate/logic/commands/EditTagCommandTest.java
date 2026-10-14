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

public class EditTagCommandTest {

    private static final Tag PUBLICITY = new Tag("Publicity");
    private static final Tag MEDIA = new Tag("Media");

    @Test
    public void execute_existingTag_renamesTagAndUpdatesContacts() {
        CoordiMate data = new CoordiMate();
        data.addTag(PUBLICITY);
        Person taggedAlice = new PersonBuilder(ALICE).withTags("Publicity", "EXCO").build();
        data.addPerson(taggedAlice);
        data.addPerson(BENSON);
        Model model = new ModelManager(data, new UserPrefs());
        Model expectedModel = new ModelManager(data, new UserPrefs());
        expectedModel.setTag(PUBLICITY, MEDIA);

        CommandResult expectedResult = new CommandResult(
                "Publicity successfully renamed to Media.", false, false, true);
        assertCommandSuccess(new EditTagCommand("publicity", MEDIA), model, expectedResult, expectedModel);
        assertEquals(List.of("EXCO", "Media"), model.getFilteredPersonList().get(0).getTags().stream()
                .map(Tag::getTagName).sorted().toList());
        assertEquals(BENSON, model.getFilteredPersonList().get(1));
    }

    @Test
    public void execute_missingTag_throwsCommandException() {
        Model model = modelWithCustomTags(PUBLICITY);
        assertCommandFailure(new EditTagCommand("Missing", MEDIA), model,
                "No such tag exists: Missing.");
    }

    @Test
    public void execute_sameTagIgnoringCase_throwsCommandException() {
        Model model = modelWithCustomTags(PUBLICITY);
        assertCommandFailure(new EditTagCommand("publicity", new Tag("PUBLICITY")), model,
                "PUBLICITY is the same as Publicity.");
    }

    @Test
    public void execute_defaultTag_renamesTag() {
        CoordiMate data = new CoordiMate();
        Model model = new ModelManager(data, new UserPrefs());
        Model expectedModel = new ModelManager(data, new UserPrefs());
        expectedModel.setTag(new Tag("EXCO"), MEDIA);

        CommandResult expectedResult = new CommandResult(
                "EXCO successfully renamed to Media.", false, false, true);
        assertCommandSuccess(new EditTagCommand("exco", MEDIA), model, expectedResult, expectedModel);
    }

    @Test
    public void execute_duplicateTag_throwsCommandException() {
        Model model = modelWithCustomTags(PUBLICITY, MEDIA);
        assertCommandFailure(new EditTagCommand("Publicity", MEDIA), model,
                "This tag already exists.");
    }

    @Test
    public void equals() {
        EditTagCommand command = new EditTagCommand("Publicity", MEDIA);
        EditTagCommand sameCommand = new EditTagCommand("Publicity", new Tag("Media"));

        assertEquals(command, sameCommand);
        assertEquals(command.hashCode(), sameCommand.hashCode());
        assertNotEquals(command, new EditTagCommand("Welfare", MEDIA));
        assertNotEquals(command, new EditTagCommand("Publicity", new Tag("Welfare")));
        assertNotEquals(command, null);
        assertNotEquals(command, new Object());
    }

    @Test
    public void nullArguments_throwNullPointerException() {
        assertThrows(NullPointerException.class, () -> new EditTagCommand(null, MEDIA));
        assertThrows(NullPointerException.class, () -> new EditTagCommand("Publicity", null));
        assertThrows(NullPointerException.class, () -> new EditTagCommand("Publicity", MEDIA).execute(null));
    }

    private Model modelWithCustomTags(Tag... tags) {
        CoordiMate data = new CoordiMate();
        for (Tag tag : tags) {
            data.addTag(tag);
        }
        return new ModelManager(data, new UserPrefs());
    }
}
