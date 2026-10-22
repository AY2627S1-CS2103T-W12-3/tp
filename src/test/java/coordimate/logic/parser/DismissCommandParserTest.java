package coordimate.logic.parser;

import static coordimate.logic.parser.CommandParserTestUtil.assertParseFailure;
import static coordimate.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;

import org.junit.jupiter.api.Test;

import coordimate.commons.core.index.Index;
import coordimate.logic.Messages;
import coordimate.logic.commands.DismissCommand;
import coordimate.logic.reminder.ReminderService;
import coordimate.model.ModelManager;

public class DismissCommandParserTest {
    private final ReminderService reminders = new ReminderService(new ModelManager(), null, Clock.systemDefaultZone());
    private final DismissCommandParser parser = new DismissCommandParser(reminders);

    @Test
    public void parse_positiveIndex_acceptsSurroundingWhitespace() {
        assertParseSuccess(parser, "1", new DismissCommand(Index.fromOneBased(1), reminders));
        assertParseSuccess(parser, " \t2 \t", new DismissCommand(Index.fromOneBased(2), reminders));
        assertParseSuccess(parser, "2147483647", new DismissCommand(Index.fromOneBased(Integer.MAX_VALUE), reminders));
    }

    @Test
    public void parse_missingIndex_showsUsage() {
        for (String input : new String[] {"", " ", "\t"}) {
            assertParseFailure(parser, input,
                    String.format(Messages.MESSAGE_INVALID_COMMAND_FORMAT, DismissCommand.MESSAGE_USAGE));
        }
    }

    @Test
    public void parse_invalidIndex_rejected() {
        for (String input : new String[] {"0", "-1", "+1", "1.5", "one", "1 2", "1 extra", "i/1", "2147483648"}) {
            assertParseFailure(parser, input, ParserUtil.MESSAGE_INVALID_INDEX);
        }
    }

    @Test
    public void parse_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }
}
