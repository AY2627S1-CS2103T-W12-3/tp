package coordimate.logic.commands;

import static coordimate.logic.commands.CommandTestUtil.assertCommandSuccess;
import static coordimate.testutil.TypicalPersons.getTypicalCoordiMate;

import org.junit.jupiter.api.Test;

import coordimate.model.CoordiMate;
import coordimate.model.Model;
import coordimate.model.ModelManager;
import coordimate.model.UserPrefs;

public class ClearCommandTest {

    @Test
    public void execute_emptyCoordiMate_success() {
        Model model = new ModelManager();
        Model expectedModel = new ModelManager();

        assertCommandSuccess(new ClearCommand(), model, ClearCommand.MESSAGE_SUCCESS, expectedModel);
    }

    @Test
    public void execute_nonEmptyCoordiMate_success() {
        Model model = new ModelManager(getTypicalCoordiMate(), new UserPrefs());
        Model expectedModel = new ModelManager(getTypicalCoordiMate(), new UserPrefs());
        expectedModel.setCoordiMate(new CoordiMate());

        assertCommandSuccess(new ClearCommand(), model, ClearCommand.MESSAGE_SUCCESS, expectedModel);
    }

}
