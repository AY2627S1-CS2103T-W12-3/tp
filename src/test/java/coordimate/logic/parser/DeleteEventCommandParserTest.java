package coordimate.logic.parser;

import static coordimate.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import coordimate.logic.commands.DeleteEventCommand;
import coordimate.logic.parser.exceptions.ParseException;

public class DeleteEventCommandParserTest {
    private final DeleteEventCommandParser parser = new DeleteEventCommandParser();

    @Test
    public void parse_examples_success() throws Exception {
        assertEquals(new DeleteEventCommand("Final Concert"),
                new CoordiMateParser().parseCommand("deleteevent evn/Final Concert"));
        assertEquals(new DeleteEventCommand("Student Life Fair"),
                new CoordiMateParser().parseCommand("deleteevent evn/Student Life Fair"));
        assertEquals(new DeleteEventCommand("Student  Life Fair"),
                parser.parse("  evn/ Student  Life Fair  "));
    }

    @Test
    public void parse_missingNameOrUnexpectedPreamble_rejected() {
        for (String input : new String[] {"", "Final Concert", "evn/", "evn/  ", "unexpected evn/Fair"}) {
            assertError(input, String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteEventCommand.MESSAGE_USAGE));
        }
    }

    @Test
    public void parse_unknownParameters_rejected() {
        for (String input : new String[] {"x/Fair", "evn/Fair st/09-10-2026", "r/Logistics evn/Fair",
            "evn/Fair /et 10-10-2026", "evn/Fair nevn/New Fair"}) {
            assertError(input, "Unknown parameter. Example: deleteevent evn/Logistics Meeting");
        }
    }

    @Test
    public void parse_repeatedName_rejected() {
        assertError("evn/Fair evn/Concert", DeleteEventCommandParser.MESSAGE_REPEATED_PARAMETER);
    }

    private void assertError(String input, String message) {
        assertEquals(message, assertThrows(ParseException.class, () -> parser.parse(input)).getMessage());
    }
}
