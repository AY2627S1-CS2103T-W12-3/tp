package coordimate.logic.parser;

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
        assertEquals(concert, parser.parse(" evn/Final Concert st/08-08-2026 15:00 /et 08-08-2026 18:00"));
        assertEquals(concert, new CoordiMateParser().parseCommand(
                "addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00"));
        AddEventCommand fair = new AddEventCommand(new Event("Student Life Fair",
                new EventTime("09-10-2026"), new EventTime("10-10-2026")));
        assertEquals(fair, parser.parse(" evn/Student Life Fair st/09-10-2026 /et 10-10-2026"));
        assertEquals(fair, parser.parse(" et/10-10-2026 evn/  Student Life Fair  st/09-10-2026"));
    }

    @Test
    public void parse_emptyValues_specificErrors() {
        assertError(" evn/ st/09-10-2026 et/10-10-2026", Event.MESSAGE_EMPTY_NAME);
        assertError(" evn/Fair st/ et/10-10-2026", EventTime.MESSAGE_EMPTY);
        assertError(" evn/Fair st/09-10-2026 et/", EventTime.MESSAGE_EMPTY);
        assertError(" evn/Fair et/10-10-2026", EventTime.MESSAGE_EMPTY);
        assertError(" evn/Fair st/09-10-2026", EventTime.MESSAGE_EMPTY);
    }

    @Test
    public void parse_incorrectDates_formatError() {
        assertError(" evn/Fair st/31-02-2026 et/10-10-2026", EventTime.MESSAGE_CONSTRAINTS);
        assertError(" evn/Fair st/09-10-2026 et/10-10-2026 24:00", EventTime.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void parse_startAfterEnd_timeOrderError() {
        assertError(" evn/Fair st/10-10-2026 et/09-10-2026", Event.MESSAGE_INVALID_TIME_ORDER);
        assertError(" evn/Concert st/08-08-2026 18:00 /et 08-08-2026 15:00", Event.MESSAGE_INVALID_TIME_ORDER);
    }

    @Test
    public void parse_unknownOrRepeatedParameters_rejected() {
        String valid = " evn/Fair st/09-10-2026 et/10-10-2026";
        assertError(valid + " r/Logistics", AddEventCommandParser.MESSAGE_UNKNOWN_PARAMETER);
        assertError(" x/foo" + valid, AddEventCommandParser.MESSAGE_UNKNOWN_PARAMETER);
        for (String repeated : new String[] {" evn/Fair", " st/09-10-2026", " et/10-10-2026",
                " /et 10-10-2026"}) {
            assertError(valid + repeated, AddEventCommandParser.MESSAGE_REPEATED_PARAMETER);
        }
        assertError(" evn/Fair evn/Fair st/09-10-2026 et/10-10-2026",
                AddEventCommandParser.MESSAGE_REPEATED_PARAMETER);
        assertThrows(ParseException.class, () -> parser.parse("unexpected" + valid));
        assertThrows(ParseException.class, () -> parser.parse(" st/09-10-2026 et/10-10-2026"));
    }

    private void assertError(String input, String message) {
        assertEquals(message, assertThrows(ParseException.class, () -> parser.parse(input)).getMessage());
    }
}
