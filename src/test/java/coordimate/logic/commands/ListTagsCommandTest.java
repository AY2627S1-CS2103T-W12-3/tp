package coordimate.logic.commands;

import static coordimate.logic.commands.CommandTestUtil.assertCommandSuccess;
import static coordimate.testutil.Assert.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import coordimate.model.CoordiMate;
import coordimate.model.Model;
import coordimate.model.ModelManager;
import coordimate.model.UserPrefs;
import coordimate.model.tag.Tag;

public class ListTagsCommandTest {

    @Test
    public void execute_defaultTags_listsAllDefaultTags() {
        Model model = new ModelManager();
        Model expectedModel = new ModelManager(model.getCoordiMate(), new UserPrefs());
        String expectedMessage = String.format(ListTagsCommand.MESSAGE_SUCCESS,
                "EXCO, Sponsor, UniversityStaff, Logistics");

        CommandResult expectedResult = new CommandResult(expectedMessage, CommandResult.View.TAGS);
        assertCommandSuccess(new ListTagsCommand(), model, expectedResult, expectedModel);
    }

    @Test
    public void execute_customTag_listsDefaultAndCustomTags() {
        CoordiMate coordiMate = new CoordiMate();
        coordiMate.addTag(new Tag("Publicity"));
        Model model = new ModelManager(coordiMate, new UserPrefs());
        Model expectedModel = new ModelManager(model.getCoordiMate(), new UserPrefs());
        String expectedMessage = String.format(ListTagsCommand.MESSAGE_SUCCESS,
                "EXCO, Sponsor, UniversityStaff, Logistics, Publicity");

        CommandResult expectedResult = new CommandResult(expectedMessage, CommandResult.View.TAGS);
        assertCommandSuccess(new ListTagsCommand(), model, expectedResult, expectedModel);
    }

    @Test
    public void execute_noTags_reportsNoTags() {
        CoordiMate coordiMate = new CoordiMate();
        coordiMate.setTags(List.of());
        Model model = new ModelManager(coordiMate, new UserPrefs());
        Model expectedModel = new ModelManager(model.getCoordiMate(), new UserPrefs());

        CommandResult expectedResult = new CommandResult(ListTagsCommand.MESSAGE_NO_TAGS, CommandResult.View.TAGS);
        assertCommandSuccess(new ListTagsCommand(), model, expectedResult, expectedModel);
    }

    @Test
    public void execute_nullModel_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new ListTagsCommand().execute(null));
    }
}
