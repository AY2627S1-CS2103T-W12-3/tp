package coordimate.logic.parser;

import static coordimate.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import coordimate.logic.parser.exceptions.ParseException;
import coordimate.testutil.UnicodeWhitespace;

public class PrefixParserTest {
    private static final Set<String> PREFIXES = Set.of("evn/", "st/");
    private static final String USAGE = "command evn/NAME st/TIME";
    private static final String UNKNOWN = "Unknown parameter: %s.";
    private static final String REPEATED = "Parameter %s may only be specified once.";

    @Test
    public void parseUniqueParameters_unicodeWhitespace_usesSameBoundariesAsNormalization() throws Exception {
        for (String space : UnicodeWhitespace.ALL) {
            assertEquals(Map.of("evn/", "Fair", "st/", "09-10-2026"),
                    parse(space + "evn/" + space + "Fair" + space + "st/09-10-2026" + space));
            for (String prefix : new String[] {"c/", "/st"}) {
                assertEquals(String.format(UNKNOWN, prefix), assertThrows(ParseException.class, () ->
                        parse("evn/Planning" + space + prefix + " review")).getMessage());
            }
            assertEquals(String.format(REPEATED, "evn/"), assertThrows(ParseException.class, () ->
                    parse("evn/Fair" + space + "evn/Concert")).getMessage());
            assertEquals(String.format(UNKNOWN, "c/"), assertThrows(ParseException.class, () ->
                    PrefixParser.rejectUnknownParameters("n/Alice" + space + "c/review",
                            PrefixParser.CONTACT_PARAMETER_PATTERN, Set.of("n/"), UNKNOWN)).getMessage());
        }
    }

    @Test
    public void parseUniqueParameters_whitespaceAndEmbeddedSlash_preservesValues() throws Exception {
        assertEquals(Map.of("evn/", "Arts/Music Fair", "st/", "09-10-2026"),
                parse("\tst/09-10-2026\n evn/Arts/Music Fair\u2003"));
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
            assertEquals(String.format(REPEATED,
                    "evn/"), assertThrows(ParseException.class, () -> parse(input)).getMessage());
        }
    }

    @Test
    public void parseUniqueParameters_preambleAndUnknownPrefix_preservesErrorPrecedence() {
        assertEquals(String.format(MESSAGE_INVALID_COMMAND_FORMAT, USAGE),
                assertThrows(ParseException.class, () -> parse("unexpected x/value")).getMessage());
        assertEquals(String.format(UNKNOWN,
                "/st"), assertThrows(ParseException.class, () -> parse("evn/Fair /st today")).getMessage());
    }

    @Test
    public void rejectUnknownParameters_contactAndEventSyntax_remainDistinct() throws Exception {
        String input = "evn/Fair /st today";
        PrefixParser.rejectUnknownParameters(input, PrefixParser.CONTACT_PARAMETER_PATTERN, PREFIXES, UNKNOWN);
        assertEquals(String.format(UNKNOWN,
                "/st"), assertThrows(ParseException.class, () -> PrefixParser.rejectUnknownParameters(
                input, PrefixParser.EVENT_PARAMETER_PATTERN, PREFIXES, UNKNOWN)).getMessage());
        assertEquals(Map.of("evn/", "Fair /st today"), PrefixParser.parseUniqueParameters(input,
                PrefixParser.CONTACT_PARAMETER_PATTERN, PREFIXES, USAGE, UNKNOWN, REPEATED));
    }

    @Test
    public void rejectUnknownParameters_relationshipWordsInContactInput_notParameters() throws Exception {
        for (String input : new String[] {"n/Rajesh s/o Kumar", "n/Priya D/O Raman", "n/Rajesh s/o"}) {
            PrefixParser.rejectUnknownParameters(input, PrefixParser.CONTACT_PARAMETER_PATTERN,
                    Set.of("n/"), UNKNOWN);
        }
        for (String prefix : new String[] {"s/", "d/", "so/"}) {
            String input = "n/Rajesh " + prefix + "old";
            assertEquals(String.format(UNKNOWN, prefix), assertThrows(ParseException.class, () ->
                    PrefixParser.rejectUnknownParameters(input, PrefixParser.CONTACT_PARAMETER_PATTERN,
                            Set.of("n/"), UNKNOWN)).getMessage());
        }
    }

    private Map<String, String> parse(String args) throws ParseException {
        return PrefixParser.parseUniqueParameters(args, PrefixParser.EVENT_PARAMETER_PATTERN,
                PREFIXES, USAGE, UNKNOWN, REPEATED);
    }
}
