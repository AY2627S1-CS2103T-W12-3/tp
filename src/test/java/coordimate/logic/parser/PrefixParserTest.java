package coordimate.logic.parser;

import static coordimate.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import coordimate.logic.parser.exceptions.ParseException;

public class PrefixParserTest {
    private static final Set<String> PREFIXES = Set.of("evn/", "st/");
    private static final String USAGE = "command evn/NAME st/TIME";
    private static final String UNKNOWN = "Unknown parameter.";
    private static final String REPEATED = "Repeated parameter.";

    @Test
    public void parseUniqueParameters_whitespaceAndEmbeddedSlash_preservesValues() throws Exception {
        assertEquals(Map.of("evn/", "Arts/Music Fair", "st/", "09-10-2026"),
                parse("\tst/09-10-2026\n evn/\u2003Arts/Music Fair\u2003"));
    }

    @Test
    public void parseUniqueParameters_emptyAndMissingValues_leavesCommandValidationToCaller() throws Exception {
        assertEquals(Map.of("evn/", "", "st/", ""), parse("evn/ st/"));
        assertEquals(Map.of("evn/", "Fair"), parse("evn/Fair"));
        assertEquals(Map.of(), parse(""));
    }

    @Test
    public void parseUniqueParameters_repeatedPrefix_rejectsAdjacentAndSeparatedDuplicates() {
        for (String input : new String[] {"evn/ evn/Fair", "evn/Fair st/09-10-2026 evn/Concert"}) {
            assertEquals(REPEATED, assertThrows(ParseException.class, () -> parse(input)).getMessage());
        }
    }

    @Test
    public void parseUniqueParameters_preambleAndUnknownPrefix_preservesErrorPrecedence() {
        assertEquals(String.format(MESSAGE_INVALID_COMMAND_FORMAT, USAGE),
                assertThrows(ParseException.class, () -> parse("unexpected x/value")).getMessage());
        assertEquals(UNKNOWN, assertThrows(ParseException.class, () -> parse("evn/Fair /st today")).getMessage());
    }

    @Test
    public void rejectUnknownParameters_contactAndEventSyntax_remainDistinct() throws Exception {
        String input = "evn/Fair /st today";
        PrefixParser.rejectUnknownParameters(input, PrefixParser.CONTACT_PARAMETER_PATTERN, PREFIXES, UNKNOWN);
        assertEquals(UNKNOWN, assertThrows(ParseException.class, () -> PrefixParser.rejectUnknownParameters(
                input, PrefixParser.EVENT_PARAMETER_PATTERN, PREFIXES, UNKNOWN)).getMessage());
        assertEquals(Map.of("evn/", "Fair /st today"), PrefixParser.parseUniqueParameters(input,
                PrefixParser.CONTACT_PARAMETER_PATTERN, PREFIXES, USAGE, UNKNOWN, REPEATED));
    }

    private Map<String, String> parse(String args) throws ParseException {
        return PrefixParser.parseUniqueParameters(args, PrefixParser.EVENT_PARAMETER_PATTERN,
                PREFIXES, USAGE, UNKNOWN, REPEATED);
    }
}
