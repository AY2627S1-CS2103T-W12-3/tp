package coordimate.logic.parser;

import static coordimate.logic.parser.CommandParserTestUtil.assertParseFailure;
import static coordimate.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static coordimate.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

import coordimate.logic.commands.CreateTagCommand;
import coordimate.model.tag.Tag;

public class CreateTagCommandParserTest {

    private final CreateTagCommandParser parser = new CreateTagCommandParser();

    @Test
    public void parse_validTag_returnsCreateTagCommand() {
        assertParseSuccess(parser, " t/Publicity", new CreateTagCommand(new Tag("Publicity")));
        assertParseSuccess(parser, " t/  Welfare  ", new CreateTagCommand(new Tag("Welfare")));
    }

    @Test
    public void parse_noParameters_throwsParseException() {
        assertParseFailure(parser, "", "No parameters given.");
        assertParseFailure(parser, "   ", "No parameters given.");
    }

    @Test
    public void parse_noPrefix_throwsParseException() {
        assertParseFailure(parser, "Publicity", "No prefix given.");
    }

    @Test
    public void parse_unknownParameters_throwsParseException() {
        assertParseFailure(parser, "1 t/Publicity", "Unknown parameters given.");
    }

    @Test
    public void parse_multipleTags_throwsParseException() {
        assertParseFailure(parser, " t/Publicity t/Welfare", "Multiple tag names given.");
    }

    @Test
    public void parse_invalidTag_throwsParseException() {
        assertParseFailure(parser, " t/Publicity Team", Tag.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, " t/Publicity!", Tag.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, " t/", "Tag name cannot be empty.");
        assertParseFailure(parser, " t/1234567890123456789012345678901", Tag.MESSAGE_LENGTH_CONSTRAINTS);
    }

    @Test
    public void parse_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }
}
