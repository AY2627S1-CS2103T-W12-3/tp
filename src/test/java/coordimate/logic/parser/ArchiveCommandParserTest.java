package coordimate.logic.parser;

import static coordimate.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import coordimate.commons.core.index.Index;
import coordimate.logic.commands.ArchiveCommand;
import coordimate.logic.parser.exceptions.ParseException;

public class ArchiveCommandParserTest {
    private final ArchiveCommandParser archiveParser = new ArchiveCommandParser(true);
    private final ArchiveCommandParser unarchiveParser = new ArchiveCommandParser(false);

    @Test
    public void parse_validIndex_success() throws Exception {
        assertEquals(new ArchiveCommand(Index.fromOneBased(2), true),
                new CoordiMateParser().parseCommand("archive 2"));
        assertEquals(new ArchiveCommand(Index.fromOneBased(1), false),
                new CoordiMateParser().parseCommand("unarchive 1"));
        assertEquals(new ArchiveCommand(Index.fromOneBased(3), true), archiveParser.parse(" \t3 "));
    }

    @Test
    public void parse_missingIndex_rejectedWithUsage() {
        assertError(archiveParser, " ", String.format(MESSAGE_INVALID_COMMAND_FORMAT, ArchiveCommand.MESSAGE_USAGE));
        assertError(unarchiveParser, "",
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, ArchiveCommand.MESSAGE_UNARCHIVE_USAGE));
    }

    @Test
    public void parse_invalidIndex_rejected() {
        for (String input : new String[] {"0", "-1", "1.5", "abc", "1 2", "2147483648"}) {
            assertError(archiveParser, input, ParserUtil.MESSAGE_INVALID_INDEX);
            assertError(unarchiveParser, input, ParserUtil.MESSAGE_INVALID_INDEX);
        }
    }

    private void assertError(ArchiveCommandParser parser, String input, String message) {
        assertEquals(message, assertThrows(ParseException.class, () -> parser.parse(input)).getMessage());
    }
}
