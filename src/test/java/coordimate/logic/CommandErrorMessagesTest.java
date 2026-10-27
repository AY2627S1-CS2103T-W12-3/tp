package coordimate.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import coordimate.logic.commands.exceptions.CommandException;
import coordimate.logic.parser.CommandParserTestUtil;
import coordimate.logic.parser.CoordiMateParser;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.ModelManager;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;

public class CommandErrorMessagesTest {
    private final CoordiMateParser parser = CommandParserTestUtil.createParser();

    @Test
    public void parse_invalidInput_hasExplanationAndExample() {
        String contact = "add n/Alice Tan p/+6591234567 e/alice@example.com r/Logistics";
        String[] inputs = {
            "", "unknown", "add", "edit", "delete", "search", "search \"\"", "filter", "filter unknown/value",
            "help anything", "tutorial 123", "exit extra", "clear garbage",
            "filter tag/", "filter event/\"Concert tag/friend", "addtag", "deletetag", "edittag", "listtags extra",
            "addevent", "editevent", "deleteevent", "assign", "unassign", "members", "markattendance",
            "dismiss", "dismiss 0", "dismiss 1 2",
            contact + " x/value", "edit 1 x/value", "delete x/value", "addtag x/value", "deletetag x/value",
            "edittag Publicity x/value", "addevent x/value", "editevent x/value", "deleteevent x/value",
            "assign x/value", "unassign x/value", "members x/value", "markattendance x/value",
            contact + " n/Bob Tan", contact + " b/ b/18-06-2004", "edit 1 r/Logistics r/President",
            "addtag t/Publicity t/Media", "deletetag t/Publicity t/Media", "edittag Publicity t/Media t/Welfare",
            "addevent evn/Concert evn/Fair", "editevent evn/Concert evn/Fair", "deleteevent evn/Concert evn/Fair",
            "assign evn/Concert c/1 c/2", "unassign evn/Concert c/1 c/2", "members evn/Concert evn/Fair",
            "markattendance evn/Concert mem/Alice Tan att/present att/absent", "edit 1", "editevent evn/Concert",
            "addtag t/", "deletetag t/", "edittag Publicity t/", "addtag t/Bad!", "edittag Publicity t/Bad!",
            contact + " b/31-02-2004", contact + " t/Bad!", "edit 1 t/Bad!", "delete 0", "edit -1 r/Logistics",
            "edit 999999999999999999999 r/Logistics", "assign evn/Concert c/999999999999999999999",
            "addevent evn/Concert st/invalid et/17-09-2026", "addevent evn/Concert", "assign evn/ c/1",
            "assign evn/Concert c/", "unassign evn/Concert c/", "markattendance evn/Concert mem/Alice Tan att/late"
        };

        for (String input : inputs) {
            String message = assertThrows(ParseException.class, () -> parser.parseCommand(input), input).getMessage();
            String explanation = message.split("\n|Example:", 2)[0].strip();
            assertTrue(Character.isUpperCase(explanation.charAt(0)), input);
            assertTrue(explanation.endsWith("."), input);
            assertFalse(explanation.contains("!"), input);
            assertTrue(message.contains("Example: "), input + ": " + message);
            assertFalse(message.contains("Error!"), input);
            assertFalse(message.contains("e.g."), input);
            assertFalse(message.startsWith("Error:"), input);
            assertEquals(1, message.split("\nParameters: ", -1).length - 1, input);
            assertTrue(message.endsWith(CoordiMateParser.getUsage(input.split(" ", 2)[0])), input);
            if (message.contains("Parameters:")) {
                assertTrue(message.contains("\nParameters: "), input);
                assertTrue(message.contains("\nExample: "), input);
            }
        }
    }

    @Test
    public void parse_emptyValues_identifiesFieldAndIncludesUsage() {
        String[][] cases = {
            {"add n/Alice p/ e/alice@example.com r/Lead", "Phone number", "p/+6591234567"},
            {"edit 1 n/", "Name", "n/Aisha Tan"},
            {"edit 1 p/", "Phone number", "p/+6591234567"},
            {"edit 1 e/", "Email", "e/aish@example.com"},
            {"edit 1 r/", "Role", "r/Logistics Lead"},
            {"delete n/", "Contact identifier", "n/Aisha Tan"},
            {"addtag t/", "Tag name", "t/Publicity"},
            {"deletetag t/", "Tag name", "t/Publicity"},
            {"edittag Media t/", "Tag name", "t/Publicity"},
            {"edit 1 at/", "Tag name", "at/Publicity"},
            {"edit 1 rt/", "Tag name", "rt/Publicity"},
            {"addevent evn/ st/17-09-2026 et/18-09-2026", "Event name", "evn/Final Concert"},
            {"deleteevent evn/", "Event name", "evn/Final Concert"},
            {"editevent evn/ nevn/New", "Event name", "evn/Final Concert"},
            {"editevent evn/Fair nevn/", "New event name", "nevn/Final Concert"},
            {"editevent evn/Fair st/", "Start time", "st/17-09-2026 16:30"},
            {"editevent evn/Fair et/", "End time", "et/17-09-2026 18:30"},
            {"addevent evn/Fair st/ et/17-09-2026", "Start time", "st/17-09-2026 16:30"},
            {"addevent evn/Fair st/17-09-2026 et/", "End time", "et/17-09-2026 18:30"},
            {"assign evn/Fair c/", "Member IDs", "c/1 3 5"},
            {"unassign evn/Fair c/", "Member IDs", "c/1 3 5"},
            {"markattendance evn/Fair mem/ att/present", "Member name", "mem/Aisha Tan"},
            {"markattendance evn/Fair mem/Alice att/", "Attendance status", "att/present"},
            {"filter tag/", "Filter value", "tag/Publicity"},
            {"filter event/", "Filter value", "event/Final Concert"}
        };
        for (String[] entry : cases) {
            String usage = CoordiMateParser.getUsage(entry[0].split(" ", 2)[0]);
            String expected = entry[1] + " must not be empty. Example: " + entry[2] + "\n" + usage;
            assertEquals(expected, assertThrows(ParseException.class, () ->
                    parser.parseCommand(entry[0])).getMessage(), entry[0]);
        }
    }

    @Test
    public void parse_invalidValues_examplesUseOffendingParameter() {
        String[][] cases = {
            {"editevent evn/Fair nevn/Bad\u0007Name", "nevn/Final-Concert"},
            {"editevent evn/Fair nevn/" + "E".repeat(Event.MAX_NAME_LENGTH + 1), "nevn/Final Concert"},
            {"markattendance evn/Fair mem/Alice! att/present", "mem/Aisha Tan"},
            {"addevent evn/Fair st/17-09-2026 et/invalid", "et/17-09-2026 16:30"},
            {"editevent evn/Fair et/invalid", "et/17-09-2026 16:30"},
            {"edit 1 at/Bad!", "at/Publicity"},
            {"edit 1 rt/Bad!", "rt/Publicity"}
        };
        for (String[] entry : cases) {
            String message = assertThrows(ParseException.class, () -> parser.parseCommand(entry[0])).getMessage();
            String usage = CoordiMateParser.getUsage(entry[0].split(" ", 2)[0]);
            assertTrue(message.substring(0, message.length() - usage.length()).contains("Example: " + entry[1]),
                    entry[0]);
            assertTrue(message.endsWith("\n" + usage), entry[0]);
        }
    }

    @Test
    public void parse_missingParameters_showsUsageInsteadOfEmptyValueError() {
        for (String input : new String[] {"addtag", "deleteevent", "editevent nevn/New", "addevent evn/Fair",
            "assign evn/Fair", "markattendance evn/Fair mem/Alice"}) {
            String usage = CoordiMateParser.getUsage(input.split(" ", 2)[0]);
            assertEquals("Invalid command format.\n" + usage,
                    assertThrows(ParseException.class, () -> parser.parseCommand(input)).getMessage(), input);
        }
    }

    @Test
    public void parse_unknownParameters_identifiesCommandAndShowsUsage() {
        String[] inputs = {
            "add x/value", "edit x/value", "delete x/value", "filter x/value", "addtag t/Publicity x/value",
            "deletetag t/Publicity x/value", "edittag Publicity t/Media x/value", "addevent x/value",
            "editevent x/value", "deleteevent x/value", "assign x/value", "unassign x/value", "members x/value",
            "markattendance x/value"
        };

        for (String input : inputs) {
            String commandWord = input.split(" ", 2)[0];
            String message = assertThrows(ParseException.class, () -> parser.parseCommand(input), input).getMessage();
            assertTrue(message.startsWith("Unknown parameter: x/.\n" + commandWord + ": "), input);
            assertTrue(message.contains("\nParameters: "), input);
            assertTrue(message.contains("\nExample: " + commandWord + " "), input);
        }
    }

    @Test
    public void execute_missingEvent_usesSameMessageAcrossCommands() {
        ModelManager model = new ModelManager();
        String[] inputs = {
            "deleteevent evn/Missing", "editevent evn/Missing nevn/Renamed", "assign evn/Missing c/1",
            "unassign evn/Missing c/1", "members evn/Missing",
            "markattendance evn/Missing mem/Alice Tan att/present"
        };

        for (String input : inputs) {
            CommandException error = assertThrows(CommandException.class, () ->
                    parser.parseCommand(input).execute(model), input);
            assertEquals("Event Missing does not exist. Use a saved event name.", error.getMessage(), input);
        }
    }

    @Test
    public void execute_missingContact_usesSameMessageAcrossCommands() {
        ModelManager model = new ModelManager();
        model.addEvent(new Event("Concert", new EventTime("17-09-2026"), new EventTime("17-09-2026")));
        String[] inputs = {"delete 1", "edit 1 r/Logistics", "assign evn/Concert c/1", "unassign evn/Concert c/1"};

        for (String input : inputs) {
            CommandException error = assertThrows(CommandException.class, () ->
                    parser.parseCommand(input).execute(model), input);
            assertEquals("No contact exists with member ID: 1. Use an existing member ID.", error.getMessage(), input);
        }
    }

    @Test
    public void execute_missingTag_usesSameMessageAcrossCommands() {
        ModelManager model = new ModelManager();
        for (String input : new String[] {"deletetag t/Missing", "edittag Missing t/Renamed"}) {
            CommandException error = assertThrows(CommandException.class, () ->
                    parser.parseCommand(input).execute(model), input);
            assertEquals("Tag Missing does not exist. Use a saved tag name.", error.getMessage(), input);
        }
    }
}
