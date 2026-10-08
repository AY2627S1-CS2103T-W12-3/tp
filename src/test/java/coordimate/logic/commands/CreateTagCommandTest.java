package coordimate.logic.commands;

import static coordimate.logic.commands.CommandTestUtil.assertCommandFailure;
import static coordimate.logic.commands.CommandTestUtil.assertCommandSuccess;
import static coordimate.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import coordimate.model.Model;
import coordimate.model.ModelManager;
import coordimate.model.UserPrefs;
import coordimate.model.tag.Tag;

public class CreateTagCommandTest {

    private static final Tag PUBLICITY = new Tag("Publicity");

    @Test
    public void execute_newTag_addsTag() {
        Model model = new ModelManager();
        Model expectedModel = new ModelManager(model.getCoordiMate(), new UserPrefs());
        expectedModel.addTag(PUBLICITY);

        CommandResult expectedResult = new CommandResult("Created tag: Publicity.", false, false, true);
        assertCommandSuccess(new CreateTagCommand(PUBLICITY), model, expectedResult, expectedModel);
        assertEquals(List.of("EXCO", "Sponsor", "UniversityStaff", "Logistics", "Publicity"),
                model.getTagList().stream().map(Tag::getTagName).toList());
    }

    @Test
    public void execute_duplicateTag_throwsCommandException() {
        Model model = new ModelManager();
        assertCommandFailure(new CreateTagCommand(new Tag("exco")), model, "This tag already exists.");
    }

    @Test
    public void equals() {
        CreateTagCommand command = new CreateTagCommand(PUBLICITY);
        assertEquals(command, new CreateTagCommand(new Tag("Publicity")));
        assertEquals(command.hashCode(), new CreateTagCommand(new Tag("Publicity")).hashCode());
        assertNotEquals(command, new CreateTagCommand(new Tag("Welfare")));
        assertNotEquals(command, null);
        assertNotEquals(command, new Object());
    }

    @Test
    public void nullArguments_throwNullPointerException() {
        assertThrows(NullPointerException.class, () -> new CreateTagCommand(null));
        assertThrows(NullPointerException.class, () -> new CreateTagCommand(PUBLICITY).execute(null));
    }
}
