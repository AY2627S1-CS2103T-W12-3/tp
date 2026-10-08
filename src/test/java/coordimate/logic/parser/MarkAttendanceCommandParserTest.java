package coordimate.logic.parser;

import static coordimate.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import coordimate.logic.commands.MarkAttendanceCommand;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.event.AttendanceStatus;
import coordimate.model.event.Event;
import coordimate.model.person.Name;

public class MarkAttendanceCommandParserTest {

    private static final String INVALID_FORMAT =
            String.format(MESSAGE_INVALID_COMMAND_FORMAT, MarkAttendanceCommand.MESSAGE_USAGE);

    private final MarkAttendanceCommandParser parser = new MarkAttendanceCommandParser();

    @Test
    public void parse_validArgs_success() throws Exception {
        MarkAttendanceCommand expected = new MarkAttendanceCommand("Final Concert", new Name("Alice Tan"),
                AttendanceStatus.PRESENT);
        assertEquals(expected, parser.parse(" evn/Final Concert mem/Alice Tan att/present"));
        assertEquals(expected, parser.parse(" mem/Alice Tan att/present evn/Final Concert"));
        assertEquals(expected, parser.parse("  evn/  Final Concert   mem/  Alice Tan   att/  present  "));
    }

    @Test
    public void parse_statusCaseInsensitive_success() throws Exception {
        MarkAttendanceCommand expected = new MarkAttendanceCommand("Final Concert", new Name("Alice Tan"),
                AttendanceStatus.ABSENT);
        assertEquals(expected, parser.parse(" evn/Final Concert mem/Alice Tan att/ABSENT"));
        assertEquals(expected, parser.parse(" evn/Final Concert mem/Alice Tan att/Absent"));
    }

    @Test
    public void parse_missingParameters_invalidFormat() {
        for (String input : new String[] {"", " ", " evn/Final Concert", " mem/Alice Tan",
            " att/present", " evn/Final Concert mem/Alice Tan"}) {
            assertError(input, INVALID_FORMAT);
        }
    }

    @Test
    public void parse_emptyEventName_rejected() {
        assertError(" evn/ mem/Alice Tan att/present", Event.MESSAGE_EMPTY_NAME);
    }

    @Test
    public void parse_emptyMemberName_rejected() {
        assertError(" evn/Final Concert mem/ att/present", INVALID_FORMAT);
    }

    @Test
    public void parse_invalidStatus_rejected() {
        assertError(" evn/Final Concert mem/Alice Tan att/maybe",
                MarkAttendanceCommandParser.MESSAGE_INVALID_STATUS);
        assertError(" evn/Final Concert mem/Alice Tan att/",
                MarkAttendanceCommandParser.MESSAGE_INVALID_STATUS);
    }

    @Test
    public void parse_repeatedParameters_rejected() {
        assertError(" evn/Final Concert evn/Fair mem/Alice Tan att/present",
                MarkAttendanceCommandParser.MESSAGE_REPEATED_PARAMETER);
        assertError(" evn/Final Concert mem/Alice Tan mem/Bob Lim att/present",
                MarkAttendanceCommandParser.MESSAGE_REPEATED_PARAMETER);
    }

    @Test
    public void parse_unknownParameters_rejected() {
        String valid = " evn/Final Concert mem/Alice Tan att/present";
        assertError(valid + " x/1", MarkAttendanceCommandParser.MESSAGE_UNKNOWN_PARAMETER);
        assertError(" x/1" + valid, MarkAttendanceCommandParser.MESSAGE_UNKNOWN_PARAMETER);
    }

    private void assertError(String input, String message) {
        assertEquals(message, assertThrows(ParseException.class, () -> parser.parse(input)).getMessage());
    }
}
