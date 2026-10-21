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
import coordimate.logic.commands.AddCommand;
import coordimate.logic.commands.AssignCommand;
import coordimate.logic.commands.ClearCommand;
import coordimate.logic.commands.CommandResult;
import coordimate.logic.commands.CreateTagCommand;
import coordimate.logic.commands.DeleteCommand;
import coordimate.logic.commands.DeleteTagCommand;
import coordimate.logic.commands.EditCommand;
import coordimate.logic.commands.EditCommand.EditPersonDescriptor;
import coordimate.logic.commands.EditTagCommand;
import coordimate.logic.commands.ExitCommand;
import coordimate.logic.commands.FilterCommand;
import coordimate.logic.commands.HelpCommand;
import coordimate.logic.commands.ListCommand;
import coordimate.logic.commands.ListTagsCommand;
import coordimate.logic.commands.MarkAttendanceCommand;
import coordimate.logic.commands.MembersCommand;
import coordimate.logic.commands.SearchCommand;
import coordimate.logic.commands.SwitchCommand;
import coordimate.logic.commands.UnassignCommand;
import coordimate.logic.parser.exceptions.ParseException;
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

public class CoordiMateParserTest {

    private final CoordiMateParser parser = new CoordiMateParser();

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
        assertEquals(new AssignCommand("Final Concert", List.of(INDEX_FIRST_PERSON, Index.fromOneBased(3))),
                command);
    }

    @Test
    public void parseCommand_unassign() throws Exception {
        UnassignCommand command = (UnassignCommand) parser.parseCommand(
                UnassignCommand.COMMAND_WORD + " evn/Final Concert c/1 3");
        assertEquals(new UnassignCommand("Final Concert", List.of(INDEX_FIRST_PERSON, Index.fromOneBased(3))),
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
        assertTrue(parser.parseCommand(ClearCommand.COMMAND_WORD + " 3") instanceof ClearCommand);
    }

    @Test
    public void parseCommand_createTag() throws Exception {
        assertEquals(new CreateTagCommand(new Tag("Publicity")),
                parser.parseCommand(CreateTagCommand.COMMAND_WORD + " t/Publicity"));
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
        assertTrue(parser.parseCommand(ExitCommand.COMMAND_WORD + " 3") instanceof ExitCommand);
    }

    @Test
    public void parseCommand_filter() throws Exception {
        FilterCommand command = (FilterCommand) parser.parseCommand(
                FilterCommand.COMMAND_WORD + " tag/friend");
        assertEquals(new FilterCommand(List.of(new FilterCriterion(FilterField.TAG, "friend"))), command);
    }

    @Test
    public void parseCommand_removedCommand_throwsUnknownCommandException() {
        assertThrows(ParseException.class, withUsage(MESSAGE_UNKNOWN_COMMAND, HelpCommand.MESSAGE_USAGE), () ->
                parser.parseCommand("find"));
        assertThrows(ParseException.class, withUsage(MESSAGE_UNKNOWN_COMMAND, HelpCommand.MESSAGE_USAGE), () ->
                parser.parseCommand("find Alice"));
    }

    @Test
    public void parseCommand_searchPhrase() throws Exception {
        SearchCommand command = (SearchCommand) parser.parseCommand("search Alice Pauline");
        assertEquals(new SearchCommand(new ContactMatchesKeywordPredicate("Alice Pauline"), "Alice Pauline"), command);
    }

    @Test
    public void parseCommand_help() throws Exception {
        assertTrue(parser.parseCommand(HelpCommand.COMMAND_WORD) instanceof HelpCommand);
        assertTrue(parser.parseCommand(HelpCommand.COMMAND_WORD + " 3") instanceof HelpCommand);
    }

    @Test
    public void parseCommand_list() throws Exception {
        assertTrue(parser.parseCommand(ListCommand.COMMAND_WORD) instanceof ListCommand);
        assertTrue(parser.parseCommand(ListCommand.COMMAND_WORD + " 3") instanceof ListCommand);
    }

    @Test
    public void parseCommand_search() throws Exception {
        SearchCommand command = (SearchCommand) parser.parseCommand(
                SearchCommand.COMMAND_WORD + " Alice");
        assertEquals(new SearchCommand(new ContactMatchesKeywordPredicate("Alice"), "Alice"), command);
    }

    @Test
    public void parseCommand_switch() throws Exception {
        ModelManager model = new ModelManager();
        assertEquals(new CommandResult("Switched to Contacts view.", CommandResult.View.CONTACTS),
                parser.parseCommand("switch contacts").execute(model));
        assertEquals(new CommandResult("Switched to Events view.", CommandResult.View.EVENTS),
                parser.parseCommand("switch events").execute(model));
        assertEquals(new CommandResult("Switched to Tags view.", CommandResult.View.TAGS),
                parser.parseCommand("switch tags").execute(model));
        assertEquals(new CommandResult("Switched to Events view.", CommandResult.View.EVENTS),
                parser.parseCommand(" switch   events  ").execute(model));
    }

    @Test
    public void parseCommand_switchInvalidView_throwsParseException() {
        String expected = String.format(MESSAGE_INVALID_COMMAND_FORMAT, SwitchCommand.MESSAGE_USAGE);
        assertThrows(ParseException.class, expected, () -> parser.parseCommand("switch"));
        assertThrows(ParseException.class, expected, () -> parser.parseCommand("switch calendar"));
        assertThrows(ParseException.class, expected, () -> parser.parseCommand("switch Events"));
        assertThrows(ParseException.class, expected, () -> parser.parseCommand("switch events extra"));
    }

    @Test
    public void parseCommand_listTags() throws Exception {
        assertTrue(parser.parseCommand(ListTagsCommand.COMMAND_WORD) instanceof ListTagsCommand);
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
