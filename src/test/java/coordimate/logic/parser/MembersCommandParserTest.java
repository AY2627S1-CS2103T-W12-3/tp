package coordimate.logic.parser;

import static coordimate.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import coordimate.logic.commands.MembersCommand;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.event.Event;

public class MembersCommandParserTest {
    private static final String INVALID_FORMAT =
            String.format(MESSAGE_INVALID_COMMAND_FORMAT, MembersCommand.MESSAGE_USAGE);

    private final MembersCommandParser parser = new MembersCommandParser();

    @Test
    public void parse_validArgs_success() throws Exception {
        assertEquals(new MembersCommand("Final Concert"), parser.parse(" evn/Final Concert"));
        assertEquals(new MembersCommand("Final Concert"), parser.parse("   evn/   Final Concert   "));
        assertEquals(new MembersCommand("final concert"), parser.parse(" evn/final concert"));
        assertEquals(new MembersCommand("Dinner w Seniors"), parser.parse(" evn/Dinner w Seniors"));
    }

    @Test
    public void parse_missingOrExtraText_invalidFormat() {
        for (String input : new String[] {"", " ", " Final Concert", " hello evn/Final Concert"}) {
            assertError(input, INVALID_FORMAT);
        }
    }

    @Test
    public void parse_emptyName_rejected() {
        assertError(" evn/", Event.MESSAGE_EMPTY_NAME);
        assertError(" evn/   ", Event.MESSAGE_EMPTY_NAME);
    }

    @Test
    public void parse_repeatedName_rejected() {
        assertError(" evn/Final Concert evn/Fair", MembersCommandParser.MESSAGE_REPEATED_PARAMETER);
    }

    @Test
    public void parse_unknownParameters_rejected() {
        for (String unknown : new String[] {" c/1", " x/1", " st/08-08-2026", " /et 1"}) {
            assertError(" evn/Final Concert" + unknown, MembersCommandParser.MESSAGE_UNKNOWN_PARAMETER);
        }
        assertEquals("Unknown parameter. Example: members evn/Final Concert",
                assertThrows(ParseException.class, () -> parser.parse(" c/1 evn/Final Concert")).getMessage());
    }

    private void assertError(String input, String message) {
        assertEquals(message, assertThrows(ParseException.class, () -> parser.parse(input)).getMessage());
    }
}
