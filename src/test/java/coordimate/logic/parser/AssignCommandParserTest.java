package coordimate.logic.parser;

import static coordimate.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import coordimate.commons.core.index.Index;
import coordimate.logic.commands.AssignCommand;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.event.Event;

public class AssignCommandParserTest {
    private static final String INVALID_FORMAT =
            String.format(MESSAGE_INVALID_COMMAND_FORMAT, AssignCommand.MESSAGE_USAGE);

    private final AssignCommandParser parser = new AssignCommandParser();

    @Test
    public void parse_validArgs_success() throws Exception {
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
        assertError(" evn/Final Concert c/", AssignCommandParser.MESSAGE_NO_CONTACTS);
        assertError(" evn/Final Concert c/   ", AssignCommandParser.MESSAGE_NO_CONTACTS);
    }

    @Test
    public void parse_invalidContactIndexes_rejected() {
        for (String indexes : new String[] {"0", "-1", "+1", "a", "1 x", "1,2", "1.5", "99999999999"}) {
            assertError(" evn/Final Concert c/" + indexes, AssignCommandParser.MESSAGE_INVALID_CONTACT_INDEXES);
        }
    }

    @Test
    public void parse_repeatedParameters_rejected() {
        assertError(" evn/Final Concert evn/Fair c/1", AssignCommandParser.MESSAGE_REPEATED_PARAMETER);
        assertError(" evn/Final Concert c/1 c/2", AssignCommandParser.MESSAGE_REPEATED_PARAMETER);
    }

    @Test
    public void parse_unknownParameters_rejected() {
        for (String unknown : new String[] {" x/1", " r/Logistics", " st/08-08-2026", " /et 1"}) {
            assertError(" evn/Final Concert c/1" + unknown, AssignCommandParser.MESSAGE_UNKNOWN_PARAMETER);
        }
        assertError(" x/1 evn/Final Concert c/1", AssignCommandParser.MESSAGE_UNKNOWN_PARAMETER);
    }

    private void assertError(String input, String message) {
        assertEquals(message, assertThrows(ParseException.class, () -> parser.parse(input)).getMessage());
    }

    private static AssignCommand assign(String eventName, Integer... oneBasedIndexes) {
        return new AssignCommand(eventName, Arrays.stream(oneBasedIndexes).map(Index::fromOneBased).toList());
    }
}
