package coordimate.logic.parser;

import static coordimate.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import coordimate.logic.commands.AddEventCommand;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;

public class AddEventCommandParserTest {
    private final AddEventCommandParser parser = new AddEventCommandParser();

    @Test
    public void parse_documentedExamples_success() throws Exception {
        AddEventCommand concert = new AddEventCommand(new Event("Final Concert",
                new EventTime("08-08-2026 15:00"), new EventTime("08-08-2026 18:00")));
        assertEquals(concert, parser.parse(" evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00"));
        assertEquals(concert, CommandParserTestUtil.createParser().parseCommand(
                "addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00"));
        AddEventCommand fair = new AddEventCommand(new Event("Student Life Fair",
                new EventTime("09-10-2026"), new EventTime("10-10-2026")));
        assertEquals(fair, parser.parse(" evn/Student Life Fair st/09-10-2026 et/10-10-2026"));
        assertEquals(fair, parser.parse(" et/10-10-2026 evn/  Student Life Fair  st/09-10-2026"));
        String longestName = "E".repeat(Event.MAX_NAME_LENGTH);
        assertEquals(new AddEventCommand(new Event(longestName, new EventTime("09-10-2026"),
                new EventTime("10-10-2026"))), parser.parse(" evn/" + longestName + " st/09-10-2026 et/10-10-2026"));
    }

    @Test
    public void parse_emptyValues_specificErrors() {
        assertError(" evn/ st/09-10-2026 et/10-10-2026", Event.MESSAGE_EMPTY_NAME);
        assertError(" evn/" + "E".repeat(Event.MAX_NAME_LENGTH + 1) + " st/09-10-2026 et/10-10-2026",
                Event.MESSAGE_NAME_LENGTH_CONSTRAINTS);
        assertError(" evn/Fair st/ et/10-10-2026", "Start time must not be empty. Example: st/17-09-2026 16:30");
        assertError(" evn/Fair st/09-10-2026 et/", "End time must not be empty. Example: et/17-09-2026 18:30");
        assertError(" evn/Fair et/10-10-2026",
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, AddEventCommand.MESSAGE_USAGE));
        assertError(" evn/Fair st/09-10-2026",
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, AddEventCommand.MESSAGE_USAGE));
    }

    @Test
    public void parse_incorrectDates_formatError() {
        assertError(" evn/Fair st/31-02-2026 et/10-10-2026", EventTime.MESSAGE_CONSTRAINTS);
        assertError(" evn/Fair st/09-10-2026 et/10-10-2026 24:00",
                EventTime.MESSAGE_CONSTRAINTS.replace("Example: st/", "Example: et/"));
    }

    @Test
    public void parse_yearZero_rejected() {
        for (String value : new String[] {"01-01-0000", "01-01-0000 00:00"}) {
            assertError(" evn/Fair st/" + value + " et/10-10-2026", EventTime.MESSAGE_CONSTRAINTS);
            assertError(" evn/Fair st/01-01-0001 et/" + value,
                    EventTime.MESSAGE_CONSTRAINTS.replace("Example: st/", "Example: et/"));
        }
    }

    @Test
    public void parse_startAfterEnd_timeOrderError() {
        assertError(" evn/Fair st/10-10-2026 et/09-10-2026", Event.MESSAGE_INVALID_TIME_ORDER);
        assertError(" evn/Concert st/08-08-2026 18:00 et/08-08-2026 15:00", Event.MESSAGE_INVALID_TIME_ORDER);
    }

    @Test
    public void parse_unknownOrRepeatedParameters_rejected() {
        String valid = " evn/Fair st/09-10-2026 et/10-10-2026";
        assertError(valid + " r/Logistics", String.format(AddEventCommandParser.MESSAGE_UNKNOWN_PARAMETER, "r/"));
        assertError(" x/foo" + valid, String.format(AddEventCommandParser.MESSAGE_UNKNOWN_PARAMETER, "x/"));
        for (String repeated : new String[] {" evn/Fair", " st/09-10-2026", " et/10-10-2026"}) {
            String prefix = repeated.substring(0, repeated.indexOf('/') + 1).strip();
            assertError(valid + repeated,
                    String.format(AddEventCommandParser.MESSAGE_REPEATED_PARAMETER, prefix));
        }
        assertError(" evn/Fair st/09-10-2026 /et 10-10-2026",
                String.format(AddEventCommandParser.MESSAGE_UNKNOWN_PARAMETER, "/et"));
        assertError(" evn/Fair evn/Fair st/09-10-2026 et/10-10-2026",
                String.format(AddEventCommandParser.MESSAGE_REPEATED_PARAMETER, "evn/"));
        assertThrows(ParseException.class, () -> parser.parse("unexpected" + valid));
        assertThrows(ParseException.class, () -> parser.parse(" st/09-10-2026 et/10-10-2026"));
    }

    private void assertError(String input, String message) {
        assertEquals(message, assertThrows(ParseException.class, () -> parser.parse(input)).getMessage());
    }
}
