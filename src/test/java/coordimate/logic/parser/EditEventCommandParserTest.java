package coordimate.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import coordimate.logic.commands.EditEventCommand;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;

public class EditEventCommandParserTest {
    private final EditEventCommandParser parser = new EditEventCommandParser();

    @Test
    public void parse_examplesAndOptionalFields_success() throws Exception {
        assertEquals(new EditEventCommand("Final Concert", null,
                new EventTime("08-08-2026 16:00"), new EventTime("08-08-2026 19:00")),
                CommandParserTestUtil.createParser().parseCommand(
                        "editevent evn/Final Concert st/08-08-2026 16:00 et/08-08-2026 19:00"));
        assertEquals(new EditEventCommand("Student Life Fair", "NUS Student Life Fair", null,
                new EventTime("10-10-2026")), parser.parse(
                        " evn/Student Life Fair nevn/NUS Student Life Fair et/10-10-2026"));
        assertEquals(new EditEventCommand("Fair", "New Fair", null, null),
                parser.parse(" nevn/ New Fair  evn/ Fair "));
        assertEquals(new EditEventCommand("Fair", null, new EventTime("09-10-2026"), null),
                parser.parse(" evn/Fair st/09-10-2026"));
        assertEquals(new EditEventCommand("Fair", null, null, new EventTime("10-10-2026")),
                parser.parse(" et/10-10-2026 evn/Fair"));
    }

    @Test
    public void parse_emptyOrMissingFields_rejected() {
        assertError(" evn/Fair", EditEventCommand.MESSAGE_NOT_EDITED);
        assertError(" evn/Fair nevn/  ", EditEventCommand.MESSAGE_EMPTY_NEW_NAME);
        assertError(" evn/" + "E".repeat(Event.MAX_NAME_LENGTH + 1)
                + " st/09-10-2026", Event.MESSAGE_NAME_LENGTH_CONSTRAINTS);
        assertError(" evn/Fair nevn/" + "E".repeat(Event.MAX_NAME_LENGTH + 1),
                Event.MESSAGE_NAME_LENGTH_CONSTRAINTS.replace("Example: evn/", "Example: nevn/"));
        assertError(" evn/Fair st/", "Start time must not be empty. Example: st/17-09-2026 16:30");
        assertError(" evn/Fair et/", "End time must not be empty. Example: et/17-09-2026 18:30");
        for (String input : new String[] {"", "nevn/Fair", "evn/ nevn/Fair", "unexpected evn/Fair nevn/New"}) {
            assertThrows(ParseException.class, () -> parser.parse(input));
        }
    }

    @Test
    public void parse_invalidTimes_rejected() {
        for (String value : new String[] {"31-02-2026", "10-10-2026 24:00", "2026-10-10",
            "1-10-2026", "10-10-2026 16:30:00", "16:30", "01-01-0000", "01-01-0000 00:00"}) {
            assertError(" evn/Fair st/" + value, EditEventCommand.MESSAGE_TIME_FORMAT);
            assertError(" evn/Fair et/" + value,
                    EditEventCommand.MESSAGE_TIME_FORMAT.replace("Example: st/", "Example: et/"));
        }
    }

    @Test
    public void parse_repeatedAndUnknownParameters_rejected() {
        String valid = " evn/Fair nevn/New Fair st/09-10-2026 et/10-10-2026";
        for (String repeated : new String[] {" evn/Fair", " nevn/New Fair", " st/09-10-2026", " et/10-10-2026"}) {
            String prefix = repeated.substring(0, repeated.indexOf('/') + 1).strip();
            assertError(valid + repeated,
                    String.format(EditEventCommandParser.MESSAGE_REPEATED_PARAMETER, prefix));
        }
        assertError(" evn/Fair evn/Fair nevn/New",
                String.format(EditEventCommandParser.MESSAGE_REPEATED_PARAMETER, "evn/"));
        for (String unknown : new String[] {" r/Logistics", " x/unknown", " /et 10-10-2026"}) {
            String prefix = unknown.strip().replaceFirst("^(\\w+/).*", "$1").split(" ", 2)[0];
            assertError(valid + unknown,
                    String.format(EditEventCommandParser.MESSAGE_UNKNOWN_PARAMETER, prefix));
        }
    }

    private void assertError(String input, String message) {
        assertEquals(message, assertThrows(ParseException.class, () -> parser.parse(input)).getMessage());
    }
}
