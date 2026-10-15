package coordimate.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import coordimate.logic.commands.exceptions.CommandException;
import coordimate.logic.parser.CoordiMateParser;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.ModelManager;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;

public class CommandErrorMessagesTest {
    private final CoordiMateParser parser = new CoordiMateParser();

    @Test
    public void parse_invalidInput_hasExplanationAndExample() {
        String contact = "add n/Alice Tan p/+6591234567 e/alice@example.com r/Logistics";
        String[] inputs = {
            "", "unknown", "add", "edit", "delete", "search", "search \"\"", "filter", "filter unknown/value",
            "filter tag/", "filter event/\"Concert tag/friend", "addtag", "deletetag", "edittag", "listtags extra",
            "addevent", "editevent", "deleteevent", "assign", "unassign", "members", "markattendance",
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
            if (message.contains("Parameters:")) {
                assertTrue(message.contains("\nParameters: "), input);
                assertTrue(message.contains("\nExample: "), input);
            }
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
            assertTrue(message.startsWith("Unknown parameter.\n" + commandWord + ": "), input);
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
            assertEquals("No contact exists at index 1. Use an index from the current contact list.",
                    error.getMessage(), input);
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
