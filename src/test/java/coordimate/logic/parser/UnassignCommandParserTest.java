package coordimate.logic.parser;

import static coordimate.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import coordimate.commons.core.index.Index;
import coordimate.logic.commands.UnassignCommand;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.event.Event;

public class UnassignCommandParserTest {
    private static final String INVALID_FORMAT =
            String.format(MESSAGE_INVALID_COMMAND_FORMAT, UnassignCommand.MESSAGE_USAGE);

    private final UnassignCommandParser parser = new UnassignCommandParser();

    @Test
    public void parse_validArgs_success() throws Exception {
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
        assertError(" evn/Final Concert c/", UnassignCommandParser.MESSAGE_NO_CONTACTS);
        assertError(" evn/Final Concert c/   ", UnassignCommandParser.MESSAGE_NO_CONTACTS);
    }

    @Test
    public void parse_invalidContactIndexes_rejected() {
        for (String indexes : new String[] {"0", "-1", "+1", "a", "1 x", "1,2", "1.5", "99999999999"}) {
            assertError(" evn/Final Concert c/" + indexes, UnassignCommandParser.MESSAGE_INVALID_CONTACT_INDEXES);
        }
    }

    @Test
    public void parse_repeatedParameters_rejected() {
        assertError(" evn/Final Concert evn/Fair c/1", UnassignCommandParser.MESSAGE_REPEATED_PARAMETER);
        assertError(" evn/Final Concert c/1 c/2", UnassignCommandParser.MESSAGE_REPEATED_PARAMETER);
    }

    @Test
    public void parse_unknownParameters_rejected() {
        for (String unknown : new String[] {" x/1", " r/Logistics", " mem/Alex Yeoh", " /et 1"}) {
            assertError(" evn/Final Concert c/1" + unknown, UnassignCommandParser.MESSAGE_UNKNOWN_PARAMETER);
        }
        assertEquals("Unknown parameter. Example: unassign evn/Final Concert c/2 3",
                assertThrows(ParseException.class, () -> parser.parse(" x/1 evn/Final Concert c/1")).getMessage());
    }

    private void assertError(String input, String message) {
        assertEquals(message, assertThrows(ParseException.class, () -> parser.parse(input)).getMessage());
    }

    private static UnassignCommand unassign(String eventName, Integer... oneBasedIndexes) {
        return new UnassignCommand(eventName, Arrays.stream(oneBasedIndexes).map(Index::fromOneBased).toList());
    }
}
