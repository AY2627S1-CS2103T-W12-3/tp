package coordimate.logic.parser;

import static coordimate.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import coordimate.logic.commands.AssignCommand;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.event.Event;
import coordimate.model.person.MemberId;

public class AssignCommandParserTest {
    private static final String INVALID_FORMAT =
            String.format(MESSAGE_INVALID_COMMAND_FORMAT, AssignCommand.MESSAGE_USAGE);

    private final AssignCommandParser parser = new AssignCommandParser();

    @Test
    public void parse_validArgs_success() throws Exception {
        assertEquals(assign("Final Concert", 12, 45, 12), parser.parse(" evn/Final Concert c/12 45 12"));
        assertEquals(assign("Final Concert", Integer.MAX_VALUE), parser.parse(" evn/Final Concert c/2147483647"));
        assertEquals(assign("Final Concert", 2), parser.parse(" evn/Final Concert c/2"));
        assertEquals(assign("Final Concert", 1, 4, 5), parser.parse(" evn/Final Concert c/1 4 5"));
        assertEquals(assign("Final Concert", 1, 1, 3), parser.parse(" evn/Final Concert c/1 1 3"));
        assertEquals(assign("Final Concert", 3, 1), parser.parse("  evn/  Final Concert   c/  3   1  "));
        assertEquals(assign("Final Concert", 2), parser.parse(" c/2 evn/Final Concert"));
        assertEquals(assign("Dinner w Seniors", 2), parser.parse(" evn/Dinner w Seniors c/2"));
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
        assertError(" evn/Final Concert c/", AssignCommandParser.MESSAGE_NO_CONTACTS);
        assertError(" evn/Final Concert c/   ", AssignCommandParser.MESSAGE_NO_CONTACTS);
    }

    @Test
    public void parse_invalidMemberIds_rejected() {
        for (String ids : new String[] {"0", "-1", "+1", "a", "1 x", "1,2", "1.5", "1.0", "1e0",
            "2147483648", "99999999999"}) {
            assertError(" evn/Final Concert c/" + ids, AssignCommandParser.MESSAGE_INVALID_MEMBER_IDS);
        }
    }

    @Test
    public void parse_repeatedParameters_rejected() {
        assertError(" evn/Final Concert evn/Fair c/1",
                String.format(AssignCommandParser.MESSAGE_REPEATED_PARAMETER, "evn/"));
        assertError(" evn/Final Concert c/1 c/2", String.format(AssignCommandParser.MESSAGE_REPEATED_PARAMETER, "c/"));
    }

    @Test
    public void parse_unknownParameters_rejected() {
        for (String unknown : new String[] {" x/1", " r/Logistics", " st/08-08-2026", " /et 1"}) {
            String prefix = unknown.strip().replaceFirst("^(\\w+/).*", "$1").split(" ", 2)[0];
            assertError(" evn/Final Concert c/1" + unknown,
                    String.format(AssignCommandParser.MESSAGE_UNKNOWN_PARAMETER, prefix));
        }
        assertError(" x/1 evn/Final Concert c/1", String.format(AssignCommandParser.MESSAGE_UNKNOWN_PARAMETER, "x/"));
    }

    private void assertError(String input, String message) {
        assertEquals(message, assertThrows(ParseException.class, () -> parser.parse(input)).getMessage());
    }

    private static AssignCommand assign(String eventName, Integer... memberIds) {
        return new AssignCommand(eventName, Arrays.stream(memberIds).map(MemberId::new).toList());
    }
}
