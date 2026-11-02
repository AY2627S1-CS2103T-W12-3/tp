package coordimate.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import coordimate.logic.parser.exceptions.ParseException;

public class CaseInsensitivePrefixTest {
    private static final Pattern PREFIX = PrefixParser.CONTACT_PARAMETER_PATTERN;
    private final CoordiMateParser parser = CommandParserTestUtil.createParser();

    @Test
    public void parseCommand_prefixCaseVariants_produceSameCommands() throws Exception {
        for (String command : List.of(
                "add n/Alice Tan p/91234567 e/alice@example.com r/Member b/01-01-2000"
                        + " a/Kent Ridge o/NUS m/Call Alice lv/01-01-2027 t/EXCO t/Logistics",
                "add n/Rajesh s/o Kumar p/91234567 e/rajesh@example.com r/Member",
                "edit 1 n/Alice Tan p/91234567 e/alice@example.com r/Member b/01-01-2000"
                        + " a/Kent Ridge o/NUS m/Call Alice lv/01-01-2027 t/EXCO t/Logistics",
                "edit target/Alice Tan r/Leader at/EXCO rt/EXCO at/Logistics",
                "edit 1 b/ a/ o/ m/ lv/ t/",
                "delete n/Alice Tan", "delete p/91234567", "delete e/alice@example.com",
                "addevent evn/Arts/Music Fair st/09-10-2026 et/10-10-2026",
                "editevent evn/Arts/Music Fair nevn/New Fair st/10-10-2026 et/11-10-2026",
                "deleteevent evn/Arts/Music Fair", "archiveevent evn/Arts/Music Fair",
                "unarchiveevent evn/Arts/Music Fair", "members evn/Arts/Music Fair",
                "assign evn/Arts/Music Fair c/1 3", "unassign evn/Arts/Music Fair c/1 3",
                "markattendance evn/Arts/Music Fair mem/Alice Tan att/present",
                "addtag t/Publicity", "edittag Publicity t/Media", "deletetag t/Publicity",
                "filter tag/EXCO event/Arts Fair organisation/NUS")) {
            assertEquals(parser.parseCommand(command), parser.parseCommand(uppercasePrefixes(command)), command);
            assertEquals(parser.parseCommand(command), parser.parseCommand(mixedCasePrefixes(command)), command);
        }
    }

    @Test
    public void parseCommand_mixedCaseDuplicates_preserveDuplicateErrors() {
        for (String command : List.of(
                "add p/91234567 e/alice@example.com r/Member n/Alice n/Bob",
                "add n/Alice p/91234567 e/alice@example.com r/Member m/First m/Second",
                "edit 1 n/Alice n/Bob", "edit target/Alice target/Bob r/Leader",
                "delete n/Alice n/Bob",
                "addevent evn/Fair st/09-10-2026 et/10-10-2026 evn/Concert",
                "editevent evn/Fair nevn/Concert nevn/Meeting",
                "deleteevent evn/Fair evn/Concert", "archiveevent evn/Fair evn/Concert",
                "unarchiveevent evn/Fair evn/Concert", "members evn/Fair evn/Concert",
                "assign evn/Fair c/1 c/2", "unassign evn/Fair c/1 c/2",
                "markattendance evn/Fair mem/Alice att/present att/absent",
                "addtag t/EXCO t/Media", "edittag EXCO t/Media t/Publicity", "deletetag t/EXCO t/Media")) {
            int lastPrefix = command.lastIndexOf(' ') + 1;
            String mixedDuplicate = command.substring(0, lastPrefix)
                    + uppercasePrefixes(" " + command.substring(lastPrefix)).stripLeading();
            String expected = assertThrows(ParseException.class, () -> parser.parseCommand(command)).getMessage();
            assertEquals(expected, assertThrows(ParseException.class, () ->
                    parser.parseCommand(mixedDuplicate)).getMessage(), command);
        }
    }

    @Test
    public void parseCommand_invalidValuesWithUppercasePrefixes_preserveValidationErrors() {
        for (String command : List.of(
                "add n/Alice p/123 e/alice@example.com r/Member",
                "edit 1 b/31-02-2026", "delete n/",
                "addevent evn/Fair st/31-02-2026 et/10-10-2026",
                "editevent evn/Fair nevn/", "assign evn/Fair c/0",
                "markattendance evn/Fair mem/Alice att/unknown", "addtag t/")) {
            String expected = assertThrows(ParseException.class, () -> parser.parseCommand(command)).getMessage();
            assertEquals(expected, assertThrows(ParseException.class, () ->
                    parser.parseCommand(uppercasePrefixes(command))).getMessage(), command);
        }
    }

    private static String uppercasePrefixes(String command) {
        return PREFIX.matcher(command).replaceAll(match -> match.group().toUpperCase(Locale.ROOT));
    }

    private static String mixedCasePrefixes(String command) {
        return PREFIX.matcher(command).replaceAll(match -> match.group().substring(0, 1).toUpperCase(Locale.ROOT)
                + match.group().substring(1));
    }
}
