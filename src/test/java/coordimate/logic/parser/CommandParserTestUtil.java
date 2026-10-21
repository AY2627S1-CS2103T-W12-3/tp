package coordimate.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;

import coordimate.logic.commands.Command;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.logic.reminder.ReminderService;
import coordimate.model.ModelManager;

/**
 * Contains helper methods for testing command parsers.
 */
public class CommandParserTestUtil {

    /** Creates a parser with an empty reminder list for tests that do not execute dismissals. */
    public static CoordiMateParser createParser() {
        return new CoordiMateParser(new ReminderService(new ModelManager(), null, Clock.systemDefaultZone()));
    }

    /**
     * Asserts that the parsing of {@code userInput} by {@code parser} is successful and the command created
     * is equal to {@code expectedCommand}.
     */
    public static void assertParseSuccess(Parser<? extends Command> parser, String userInput,
            Command expectedCommand) {
        try {
            Command command = parser.parse(userInput);
            assertEquals(expectedCommand, command);
        } catch (ParseException pe) {
            throw new IllegalArgumentException("Invalid userInput.", pe);
        }
    }

    /**
     * Asserts that the parsing of {@code userInput} by {@code parser} is unsuccessful and the error message
     * is equal to {@code expectedMessage}.
     */
    public static void assertParseFailure(Parser<? extends Command> parser, String userInput, String expectedMessage) {
        try {
            parser.parse(userInput);
            throw new AssertionError("The expected ParseException was not thrown.");
        } catch (ParseException pe) {
            assertEquals(expectedMessage, pe.getMessage());
        }
    }
}
