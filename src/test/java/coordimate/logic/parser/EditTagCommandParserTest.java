package coordimate.logic.parser;

import static coordimate.logic.parser.CommandParserTestUtil.assertParseFailure;
import static coordimate.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static coordimate.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

import coordimate.logic.commands.EditTagCommand;
import coordimate.model.tag.Tag;

public class EditTagCommandParserTest {

    private final EditTagCommandParser parser = new EditTagCommandParser();

    @Test
    public void parse_validArguments_returnsEditTagCommand() {
        assertParseSuccess(parser, " Media t/Publicity",
                new EditTagCommand("Media", new Tag("Publicity")));
        assertParseSuccess(parser, "  Media  t/  ProductionCrew  ",
                new EditTagCommand("Media", new Tag("ProductionCrew")));
    }

    @Test
    public void parse_noArguments_throwsParseException() {
        assertParseFailure(parser, "", "Current tag name is not defined.");
        assertParseFailure(parser, "   ", "Current tag name is not defined.");
    }

    @Test
    public void parse_noPrefix_throwsParseException() {
        assertParseFailure(parser, " Media", "No prefix given.");
    }

    @Test
    public void parse_noCurrentTag_throwsParseException() {
        assertParseFailure(parser, " t/Publicity", "Current tag name is not defined.");
    }

    @Test
    public void parse_noNewTag_throwsParseException() {
        assertParseFailure(parser, " Media t/", "New tag name is not defined.");
        assertParseFailure(parser, " Media t/   ", "Tag name cannot be empty.");
    }

    @Test
    public void parse_multipleNewTags_throwsParseException() {
        assertParseFailure(parser, " Media t/Publicity t/ProductionCrew", "Multiple tag names given.");
    }

    @Test
    public void parse_invalidNewTag_throwsParseException() {
        assertParseFailure(parser, " Media t/Publicity Team", Tag.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, " Media t/Publicity!", Tag.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, " Media t/1234567890123456789012345678901",
                Tag.MESSAGE_LENGTH_CONSTRAINTS);
    }

    @Test
    public void parse_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }
}
