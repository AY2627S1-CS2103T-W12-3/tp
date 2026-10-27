package coordimate.logic.parser;

import static coordimate.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static coordimate.logic.Messages.MESSAGE_REPEATED_PARAMETER;
import static coordimate.logic.Messages.MESSAGE_UNKNOWN_PARAMETER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import coordimate.logic.commands.ArchiveEventCommand;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.event.Event;

public class ArchiveEventCommandParserTest {
    private final ArchiveEventCommandParser archiveParser = new ArchiveEventCommandParser(true);
    private final ArchiveEventCommandParser unarchiveParser = new ArchiveEventCommandParser(false);

    @Test
    public void parse_examples_success() throws Exception {
        assertEquals(new ArchiveEventCommand("Final Concert", true),
                CommandParserTestUtil.createParser().parseCommand("archiveevent evn/Final Concert"));
        assertEquals(new ArchiveEventCommand("Final Concert", false),
                CommandParserTestUtil.createParser().parseCommand("unarchiveevent evn/Final Concert"));
        assertEquals(new ArchiveEventCommand("Student Life Fair", true),
                archiveParser.parse("  evn/ Student  Life Fair  "));
    }

    @Test
    public void parse_missingNameOrUnexpectedPreamble_rejected() {
        for (String input : new String[] {"", "Final Concert", "unexpected evn/Fair"}) {
            assertError(archiveParser, input,
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, ArchiveEventCommand.MESSAGE_USAGE));
            assertError(unarchiveParser, input,
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, ArchiveEventCommand.MESSAGE_UNARCHIVE_USAGE));
        }
    }

    @Test
    public void parse_unknownOrRepeatedParameters_rejected() {
        assertError(archiveParser, "evn/Fair st/09-10-2026",
                String.format(MESSAGE_UNKNOWN_PARAMETER + "\n" + ArchiveEventCommand.MESSAGE_USAGE, "st/"));
        assertError(unarchiveParser, "evn/Fair /et 10-10-2026",
                String.format(MESSAGE_UNKNOWN_PARAMETER + "\n" + ArchiveEventCommand.MESSAGE_UNARCHIVE_USAGE, "/et"));
        assertError(archiveParser, "evn/Fair evn/Concert",
                String.format(MESSAGE_REPEATED_PARAMETER + "\n" + ArchiveEventCommand.MESSAGE_USAGE, "evn/"));
    }

    @Test
    public void parse_invalidName_rejected() {
        assertThrows(ParseException.class, () -> archiveParser.parse("evn/"));
        assertError(archiveParser, "evn/" + "E".repeat(Event.MAX_NAME_LENGTH + 1),
                Event.MESSAGE_NAME_LENGTH_CONSTRAINTS);
    }

    private void assertError(ArchiveEventCommandParser parser, String input, String message) {
        assertEquals(message, assertThrows(ParseException.class, () -> parser.parse(input)).getMessage());
    }
}
