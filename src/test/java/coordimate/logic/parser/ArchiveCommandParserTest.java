package coordimate.logic.parser;

import static coordimate.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import coordimate.logic.commands.ArchiveCommand;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.person.MemberId;

public class ArchiveCommandParserTest {
    private final ArchiveCommandParser archiveParser = new ArchiveCommandParser(true);
    private final ArchiveCommandParser unarchiveParser = new ArchiveCommandParser(false);

    @Test
    public void parse_validMemberId_success() throws Exception {
        assertEquals(new ArchiveCommand(new MemberId(2), true),
                CommandParserTestUtil.createParser().parseCommand("archive 2"));
        assertEquals(new ArchiveCommand(new MemberId(12), false),
                CommandParserTestUtil.createParser().parseCommand("unarchive 12"));
        assertEquals(new ArchiveCommand(new MemberId(3), true), archiveParser.parse(" \t3 "));
    }

    @Test
    public void parse_missingMemberId_rejectedWithUsage() {
        assertError(archiveParser, " ", String.format(MESSAGE_INVALID_COMMAND_FORMAT, ArchiveCommand.MESSAGE_USAGE));
        assertError(unarchiveParser, "",
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, ArchiveCommand.MESSAGE_UNARCHIVE_USAGE));
    }

    @Test
    public void parse_invalidMemberId_rejected() {
        for (String input : new String[] {"0", "-1", "1.5", "abc", "1 2", "2147483648"}) {
            assertError(archiveParser, input, String.format(ArchiveCommandParser.MESSAGE_INVALID_MEMBER_ID, "archive"));
            assertError(unarchiveParser, input,
                    String.format(ArchiveCommandParser.MESSAGE_INVALID_MEMBER_ID, "unarchive"));
        }
    }

    private void assertError(ArchiveCommandParser parser, String input, String message) {
        assertEquals(message, assertThrows(ParseException.class, () -> parser.parse(input)).getMessage());
    }
}
