package coordimate.logic.commands;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import coordimate.logic.commands.CommandResult.View;

public class SwitchCommandTest {
    @Test
    public void constructor_noView_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new SwitchCommand(View.NONE));
    }
}
