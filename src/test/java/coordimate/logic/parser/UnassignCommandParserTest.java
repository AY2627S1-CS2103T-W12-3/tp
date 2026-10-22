package coordimate.logic.parser;

import static coordimate.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import coordimate.logic.commands.UnassignCommand;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.event.Event;
import coordimate.model.person.MemberId;

public class UnassignCommandParserTest {
    private static final String INVALID_FORMAT =
            String.format(MESSAGE_INVALID_COMMAND_FORMAT, UnassignCommand.MESSAGE_USAGE);

    private final UnassignCommandParser parser = new UnassignCommandParser();

    @Test
    public void parse_validArgs_success() throws Exception {
        assertEquals(unassign("Final Concert", 12, 45, 12), parser.parse(" evn/Final Concert c/12 45 12"));
        assertEquals(unassign("Final Concert", Integer.MAX_VALUE), parser.parse(" evn/Final Concert c/2147483647"));
        assertEquals(unassign("Final Concert", 2), parser.parse(" evn/Final Concert c/2"));
        assertEquals(unassign("Final Concert", 1, 4, 5), parser.parse(" evn/Final Concert c/1 4 5"));
        assertEquals(unassign("Final Concert", 1, 1, 3), parser.parse(" evn/Final Concert c/1 1 3"));
        assertEquals(unassign("Final Concert", 3, 1), parser.parse("  evn/  Final Concert   c/  3   1  "));
        assertEquals(unassign("Final Concert", 2), parser.parse(" c/2 evn/Final Concert"));
    }

    @Test
    public void parse_missingOrExtraText_invalidFormat() {
        for (String input : new String[] {"", " ", " evn/Final Concert", " c/1", " Final Concert c/1",
            " 1 evn/Final Concert c/1"}) {
            assertError(input, INVALID_FORMAT);
        }
    }

    @Test
    public void parse_emptyValues_rejected() {
        assertError(" evn/ c/1", Event.MESSAGE_EMPTY_NAME);
        assertError(" evn/" + "E".repeat(Event.MAX_NAME_LENGTH + 1) + " c/1", Event.MESSAGE_NAME_LENGTH_CONSTRAINTS);
        assertError(" evn/Final Concert c/", UnassignCommandParser.MESSAGE_NO_CONTACTS);
        assertError(" evn/Final Concert c/   ", UnassignCommandParser.MESSAGE_NO_CONTACTS);
    }

    @Test
    public void parse_invalidMemberIds_rejected() {
        for (String ids : new String[] {"0", "-1", "+1", "a", "1 x", "1,2", "1.5", "1.0", "1e0",
            "2147483648", "99999999999"}) {
            assertError(" evn/Final Concert c/" + ids, UnassignCommandParser.MESSAGE_INVALID_MEMBER_IDS);
        }
    }

    @Test
    public void parse_repeatedParameters_rejected() {
        assertError(" evn/Final Concert evn/Fair c/1",
                String.format(UnassignCommandParser.MESSAGE_REPEATED_PARAMETER, "evn/"));
        assertError(" evn/Final Concert c/1 c/2",
                String.format(UnassignCommandParser.MESSAGE_REPEATED_PARAMETER, "c/"));
    }

    @Test
    public void parse_unknownParameters_rejected() {
        for (String unknown : new String[] {" x/1", " r/Logistics", " mem/Alex Yeoh", " /et 1"}) {
            String prefix = unknown.strip().replaceFirst("^(\\w+/).*", "$1").split(" ", 2)[0];
            assertError(" evn/Final Concert c/1" + unknown,
                    String.format(UnassignCommandParser.MESSAGE_UNKNOWN_PARAMETER, prefix));
        }
        assertEquals("Unknown parameter: x/.\n" + UnassignCommand.MESSAGE_USAGE,
                assertThrows(ParseException.class, () -> parser.parse(" x/1 evn/Final Concert c/1")).getMessage());
    }

    private void assertError(String input, String message) {
        assertEquals(message, assertThrows(ParseException.class, () -> parser.parse(input)).getMessage());
    }

    private static UnassignCommand unassign(String eventName, Integer... memberIds) {
        return new UnassignCommand(eventName, Arrays.stream(memberIds).map(MemberId::new).toList());
    }
}
