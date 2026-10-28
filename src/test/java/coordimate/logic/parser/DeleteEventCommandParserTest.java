package coordimate.logic.parser;

import static coordimate.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import coordimate.logic.commands.DeleteEventCommand;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.event.Event;
import coordimate.testutil.UnicodeWhitespace;

public class DeleteEventCommandParserTest {
    private final DeleteEventCommandParser parser = new DeleteEventCommandParser();

    @Test
    public void parse_examples_success() throws Exception {
        assertEquals(new DeleteEventCommand("Final Concert"),
                CommandParserTestUtil.createParser().parseCommand("deleteevent evn/Final Concert"));
        assertEquals(new DeleteEventCommand("Student Life Fair"),
                CommandParserTestUtil.createParser().parseCommand("deleteevent evn/Student Life Fair"));
        assertEquals(new DeleteEventCommand("Student Life Fair"),
                parser.parse("  evn/ Student  Life Fair  "));
    }

    @Test
    public void parse_unicodeSpaceSeparators_success() throws Exception {
        CoordiMateParser commandParser = CommandParserTestUtil.createParser();
        for (String space : UnicodeWhitespace.ALL) {
            if (Character.getType(space.codePointAt(0)) == Character.SPACE_SEPARATOR) {
                assertEquals(new DeleteEventCommand("Final Concert"), commandParser.parseCommand(
                        "deleteevent" + space + "evn/Final" + space + "Concert"));
            }
        }
    }

    @Test
    public void parse_missingNameOrUnexpectedPreamble_rejected() {
        for (String input : new String[] {"", "Final Concert", "unexpected evn/Fair"}) {
            assertError(input, String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteEventCommand.MESSAGE_USAGE));
        }
    }

    @Test
    public void parse_unknownParameters_rejected() {
        String[][] cases = {
            {"x/Fair", "x/"}, {"evn/Fair st/09-10-2026", "st/"}, {"r/Logistics evn/Fair", "r/"},
            {"evn/Fair /et 10-10-2026", "/et"}, {"evn/Fair nevn/New Fair", "nevn/"}
        };
        for (String[] entry : cases) {
            assertError(entry[0], String.format(DeleteEventCommandParser.MESSAGE_UNKNOWN_PARAMETER, entry[1]));
        }
    }

    @Test
    public void parse_repeatedName_rejected() {
        assertError("evn/Fair evn/Concert", String.format(DeleteEventCommandParser.MESSAGE_REPEATED_PARAMETER, "evn/"));
        assertError("evn/" + "E".repeat(Event.MAX_NAME_LENGTH + 1), Event.MESSAGE_NAME_LENGTH_CONSTRAINTS);
    }

    private void assertError(String input, String message) {
        assertEquals(message, assertThrows(ParseException.class, () -> parser.parse(input)).getMessage());
    }
}
