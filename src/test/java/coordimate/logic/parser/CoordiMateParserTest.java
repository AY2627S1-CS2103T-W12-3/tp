package coordimate.logic.parser;

import static coordimate.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static coordimate.logic.Messages.MESSAGE_UNKNOWN_COMMAND;
import static coordimate.logic.Messages.withUsage;
import static coordimate.testutil.Assert.assertThrows;
import static coordimate.testutil.TypicalIndexes.INDEX_FIRST_PERSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import coordimate.commons.core.index.Index;
import coordimate.logic.Messages;
import coordimate.logic.commands.AddCommand;
import coordimate.logic.commands.AddTagCommand;
import coordimate.logic.commands.AssignCommand;
import coordimate.logic.commands.ClearCommand;
import coordimate.logic.commands.DeleteCommand;
import coordimate.logic.commands.DeleteTagCommand;
import coordimate.logic.commands.DismissCommand;
import coordimate.logic.commands.EditCommand;
import coordimate.logic.commands.EditCommand.EditPersonDescriptor;
import coordimate.logic.commands.EditTagCommand;
import coordimate.logic.commands.ExitCommand;
import coordimate.logic.commands.FilterCommand;
import coordimate.logic.commands.HelpCommand;
import coordimate.logic.commands.ListCommand;
import coordimate.logic.commands.MarkAttendanceCommand;
import coordimate.logic.commands.MembersCommand;
import coordimate.logic.commands.SearchCommand;
import coordimate.logic.commands.TutorialCommand;
import coordimate.logic.commands.UnassignCommand;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.logic.reminder.ReminderService;
import coordimate.model.ModelManager;
import coordimate.model.event.AttendanceStatus;
import coordimate.model.person.ContactMatchesKeywordPredicate;
import coordimate.model.person.FilterCriterion;
import coordimate.model.person.FilterField;
import coordimate.model.person.MemberId;
import coordimate.model.person.Name;
import coordimate.model.person.Person;
import coordimate.model.tag.Tag;
import coordimate.testutil.EditPersonDescriptorBuilder;
import coordimate.testutil.PersonBuilder;
import coordimate.testutil.PersonUtil;
import coordimate.testutil.UnicodeWhitespace;

public class CoordiMateParserTest {

    private final CoordiMateParser parser = CommandParserTestUtil.createParser();

    @Test
    public void parseCommand_unicodeWhitespace_separatesCommandAndParameters() throws Exception {
        for (String space : UnicodeWhitespace.ALL) {
            if (space.equals("\n") || space.equals("\r") || space.equals("\u0085")
                    || space.equals("\u2028") || space.equals("\u2029")) {
                continue; // Commands occupy one line; embedded line separators remain invalid.
            }
            assertEquals(new MembersCommand("Final Concert"),
                    parser.parseCommand(space + "members" + space + "evn/Final Concert" + space));
        }
    }

    @Test
    public void parseCommand_dismiss() throws Exception {
        ReminderService reminders = new ReminderService(new ModelManager(), null, java.time.Clock.systemDefaultZone());
        assertEquals(new DismissCommand(Index.fromOneBased(2), reminders), parser.parseCommand("dismiss 2"));
        assertEquals(DismissCommand.MESSAGE_USAGE, CoordiMateParser.getUsage("dismiss"));
        String message = withUsage(ParserUtil.MESSAGE_INVALID_INDEX, DismissCommand.MESSAGE_USAGE);
        assertThrows(ParseException.class, message, () ->
                parser.parseCommand("dismiss 0"));
    }

    @Test
    public void parseCommand_add() throws Exception {
        Person person = new PersonBuilder().build();
        AddCommand command = (AddCommand) parser.parseCommand(PersonUtil.getAddCommand(person));
        assertEquals(new AddCommand(person), command);
    }

    @Test
    public void parseCommand_assign() throws Exception {
        AssignCommand command = (AssignCommand) parser.parseCommand(
                AssignCommand.COMMAND_WORD + " evn/Final Concert c/1 3");
        assertEquals(new AssignCommand("Final Concert", List.of(new MemberId(1), new MemberId(3))),
                command);
    }

    @Test
    public void parseCommand_unassign() throws Exception {
        UnassignCommand command = (UnassignCommand) parser.parseCommand(
                UnassignCommand.COMMAND_WORD + " evn/Final Concert c/1 3");
        assertEquals(new UnassignCommand("Final Concert", List.of(new MemberId(1), new MemberId(3))),
                command);
    }

    @Test
    public void parseCommand_members() throws Exception {
        assertEquals(new MembersCommand("Final Concert"),
                parser.parseCommand(MembersCommand.COMMAND_WORD + " evn/Final Concert"));
    }

    @Test
    public void parseCommand_clear() throws Exception {
        assertTrue(parser.parseCommand(ClearCommand.COMMAND_WORD) instanceof ClearCommand);
        assertTrue(parser.parseCommand(ClearCommand.COMMAND_WORD + "  \t") instanceof ClearCommand);
    }

    @Test
    public void parseCommand_addTag() throws Exception {
        assertEquals(new AddTagCommand(new Tag("Publicity")),
                parser.parseCommand(AddTagCommand.COMMAND_WORD + " t/Publicity"));
    }

    @Test
    public void parseCommand_removedNewTagCommand_throwsParseException() {
        assertThrows(ParseException.class, withUsage(MESSAGE_UNKNOWN_COMMAND, HelpCommand.MESSAGE_USAGE), () ->
                parser.parseCommand("newtag t/Publicity"));
    }

    @Test
    public void parseCommand_delete() throws Exception {
        DeleteCommand command = (DeleteCommand) parser.parseCommand(
                DeleteCommand.COMMAND_WORD + " " + INDEX_FIRST_PERSON.getOneBased());
        assertEquals(new DeleteCommand(new MemberId(1)), command);
    }

    @Test
    public void parseCommand_deleteTag() throws Exception {
        assertEquals(new DeleteTagCommand("Publicity"),
                parser.parseCommand(DeleteTagCommand.COMMAND_WORD + " t/Publicity"));
    }

    @Test
    public void parseCommand_edit() throws Exception {
        Person person = new PersonBuilder().build();
        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder(person).build();
        EditCommand command = (EditCommand) parser.parseCommand(EditCommand.COMMAND_WORD + " "
                + INDEX_FIRST_PERSON.getOneBased() + " " + PersonUtil.getEditPersonDescriptorDetails(descriptor));
        assertEquals(new EditCommand(new MemberId(1), descriptor), command);
    }

    @Test
    public void parseCommand_editTag() throws Exception {
        assertEquals(new EditTagCommand("Media", new Tag("Publicity")),
                parser.parseCommand(EditTagCommand.COMMAND_WORD + " Media t/Publicity"));
    }

    @Test
    public void parseCommand_editByTarget() throws Exception {
        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder().withRole("Logistics").build();

        EditCommand command = (EditCommand) parser.parseCommand("edit target/alice@example.com r/Logistics");

        assertEquals(new EditCommand("alice@example.com", descriptor), command);
    }

    @Test
    public void parseCommand_exit() throws Exception {
        assertTrue(parser.parseCommand(ExitCommand.COMMAND_WORD) instanceof ExitCommand);
        assertTrue(parser.parseCommand(ExitCommand.COMMAND_WORD + "  \t") instanceof ExitCommand);
    }

    @Test
    public void parseCommand_filter() throws Exception {
        FilterCommand command = (FilterCommand) parser.parseCommand(
                FilterCommand.COMMAND_WORD + " tag/friend");
        assertEquals(new FilterCommand(List.of(new FilterCriterion(FilterField.TAG, "friend"))), command);
    }

    @Test
    public void parseCommand_removedCommand_throwsUnknownCommandException() {
        for (String input : List.of("find", "find Alice", "switch events", "listtags", "listtags 1")) {
            assertThrows(ParseException.class, withUsage(MESSAGE_UNKNOWN_COMMAND, HelpCommand.MESSAGE_USAGE), () ->
                    parser.parseCommand(input));
        }
    }

    @Test
    public void parseCommand_searchPhrase() throws Exception {
        SearchCommand command = (SearchCommand) parser.parseCommand("search Alice Pauline");
        assertEquals(new SearchCommand(new ContactMatchesKeywordPredicate("Alice Pauline"), "Alice Pauline"), command);
    }

    @Test
    public void parseCommand_help() throws Exception {
        assertTrue(parser.parseCommand(HelpCommand.COMMAND_WORD) instanceof HelpCommand);
        assertTrue(parser.parseCommand(HelpCommand.COMMAND_WORD + "  \t") instanceof HelpCommand);
    }

    @Test
    public void parseCommand_tutorial() throws Exception {
        assertTrue(parser.parseCommand(TutorialCommand.COMMAND_WORD) instanceof TutorialCommand);
        assertTrue(parser.parseCommand(TutorialCommand.COMMAND_WORD + "  \t") instanceof TutorialCommand);
        assertEquals(TutorialCommand.MESSAGE_USAGE, CoordiMateParser.getUsage(TutorialCommand.COMMAND_WORD));
    }

    @Test
    public void parseCommand_parameterlessCommandWithArguments_throwsParseException() {
        String[][] cases = {
            {"clear garbage", ClearCommand.MESSAGE_USAGE},
            {"exit extra", ExitCommand.MESSAGE_USAGE},
            {"help anything", HelpCommand.MESSAGE_USAGE},
            {"tutorial 123", TutorialCommand.MESSAGE_USAGE}
        };

        for (String[] entry : cases) {
            String expected = String.format(MESSAGE_INVALID_COMMAND_FORMAT, entry[1]);
            assertThrows(ParseException.class, expected, () -> parser.parseCommand(entry[0]));
        }
    }

    @Test
    public void parseCommand_list() throws Exception {
        assertTrue(parser.parseCommand(ListCommand.COMMAND_WORD) instanceof ListCommand);
        assertEquals(new ListCommand(false), parser.parseCommand("list  "));
        assertEquals(new ListCommand(true), parser.parseCommand("list archived"));
        assertEquals(new ListCommand(true), parser.parseCommand("list  ARCHIVED "));
        for (String option : List.of("3", "archive")) {
            assertThrows(ParseException.class, Messages.withUsage(
                    String.format(ListCommandParser.MESSAGE_UNKNOWN_OPTION, option), ListCommand.MESSAGE_USAGE), () ->
                    parser.parseCommand("list " + option));
        }
    }

    @Test
    public void parseCommand_search() throws Exception {
        SearchCommand command = (SearchCommand) parser.parseCommand(
                SearchCommand.COMMAND_WORD + " Alice");
        assertEquals(new SearchCommand(new ContactMatchesKeywordPredicate("Alice"), "Alice"), command);
    }

    @Test
    public void parseCommand_markattendance() throws Exception {
        MarkAttendanceCommand command = (MarkAttendanceCommand) parser.parseCommand(
                MarkAttendanceCommand.COMMAND_WORD + " evn/Final Concert mem/Alice Tan att/present");
        assertEquals(new MarkAttendanceCommand("Final Concert", new Name("Alice Tan"), AttendanceStatus.PRESENT),
                command);
    }

    @Test
    public void parseCommand_unrecognisedInput_throwsParseException() {
        assertThrows(ParseException.class, String.format(MESSAGE_INVALID_COMMAND_FORMAT, HelpCommand.MESSAGE_USAGE), ()
                -> parser.parseCommand(""));
    }

    @Test
    public void parseCommand_unknownCommand_throwsParseException() {
        assertThrows(ParseException.class, withUsage(MESSAGE_UNKNOWN_COMMAND, HelpCommand.MESSAGE_USAGE), () ->
                parser.parseCommand("unknownCommand"));
    }
}
