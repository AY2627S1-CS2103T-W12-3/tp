package coordimate.logic.commands;

import static coordimate.logic.commands.CommandTestUtil.assertCommandFailure;
import static coordimate.logic.commands.CommandTestUtil.assertCommandSuccess;
import static coordimate.testutil.Assert.assertThrows;
import static coordimate.testutil.TypicalTags.defaultTagsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import coordimate.model.Model;
import coordimate.model.ModelManager;
import coordimate.model.UserPrefs;
import coordimate.model.tag.Tag;

public class AddTagCommandTest {

    private static final Tag PUBLICITY = new Tag("Publicity");

    @Test
    public void execute_addTag_addsTag() {
        Model model = new ModelManager();
        Model expectedModel = new ModelManager(model.getCoordiMate(), new UserPrefs());
        expectedModel.addTag(PUBLICITY);

        CommandResult expectedResult = new CommandResult("Created tag: Publicity.", CommandResult.View.TAGS);
        assertCommandSuccess(new AddTagCommand(PUBLICITY), model, expectedResult, expectedModel);
        assertEquals(defaultTagsWith(PUBLICITY), model.getTagList());
    }

    @Test
    public void execute_duplicateTag_throwsCommandException() {
        Model model = new ModelManager();
        assertCommandFailure(new AddTagCommand(new Tag("exco")), model,
                "A tag with this name already exists. No changes were made.");
    }

    @Test
    public void equals() {
        AddTagCommand command = new AddTagCommand(PUBLICITY);
        assertEquals(command, new AddTagCommand(new Tag("Publicity")));
        assertEquals(command.hashCode(), new AddTagCommand(new Tag("Publicity")).hashCode());
        assertNotEquals(command, new AddTagCommand(new Tag("Welfare")));
        assertNotEquals(command, null);
        assertNotEquals(command, new Object());
    }

    @Test
    public void nullArguments_throwNullPointerException() {
        assertThrows(NullPointerException.class, () -> new AddTagCommand(null));
        assertThrows(NullPointerException.class, () -> new AddTagCommand(PUBLICITY).execute(null));
    }
}
