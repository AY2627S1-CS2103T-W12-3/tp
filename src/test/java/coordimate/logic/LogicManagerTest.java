package coordimate.logic;

import static coordimate.logic.Messages.MESSAGE_UNKNOWN_COMMAND;
import static coordimate.logic.commands.CommandTestUtil.ADDRESS_DESC_AMY;
import static coordimate.logic.commands.CommandTestUtil.EMAIL_DESC_AMY;
import static coordimate.logic.commands.CommandTestUtil.NAME_DESC_AMY;
import static coordimate.logic.commands.CommandTestUtil.PHONE_DESC_AMY;
import static coordimate.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import coordimate.commons.exceptions.DataLoadingException;
import coordimate.logic.commands.AddCommand;
import coordimate.logic.commands.CommandResult;
import coordimate.logic.commands.CreateTagCommand;
import coordimate.logic.commands.DeleteCommand;
import coordimate.logic.commands.DeleteTagCommand;
import coordimate.logic.commands.EditTagCommand;
import coordimate.logic.commands.ListCommand;
import coordimate.logic.commands.MarkAttendanceCommand;
import coordimate.logic.commands.MutatingTagCommand;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.logic.parser.CoordiMateParser;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.Model;
import coordimate.model.ModelManager;
import coordimate.model.ReadOnlyCoordiMate;
import coordimate.model.UserPrefs;
import coordimate.model.tag.Tag;
import coordimate.storage.JsonCoordiMateStorage;
import coordimate.storage.JsonUserPrefsStorage;
import coordimate.storage.StorageManager;

public class LogicManagerTest {
    private static final IOException DUMMY_IO_EXCEPTION = new IOException("dummy IO exception");
    private static final IOException DUMMY_AD_EXCEPTION = new AccessDeniedException("dummy access denied exception");

    @TempDir
    public Path temporaryFolder;

    private Model model = new ModelManager();
    private Logic logic;

    @BeforeEach
    public void setUp() {
        JsonCoordiMateStorage coordiMateStorage =
                new JsonCoordiMateStorage(temporaryFolder.resolve("coordiMate.json"));
        JsonUserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json"));
        StorageManager storage = new StorageManager(coordiMateStorage, userPrefsStorage);
        logic = new LogicManager(model, storage);
    }

    @Test
    public void execute_invalidCommandFormat_throwsParseException() {
        String invalidCommand = "uicfhmowqewca";
        assertParseException(invalidCommand, MESSAGE_UNKNOWN_COMMAND);
    }

    @Test
    public void execute_commandExecutionError_throwsCommandException() {
        String deleteCommand = "delete 9";
        assertCommandException(deleteCommand, String.format(DeleteCommand.MESSAGE_UNKNOWN_MEMBER_ID, 9));
    }

    @Test
    public void execute_validCommand_success() throws Exception {
        String listCommand = ListCommand.COMMAND_WORD;
        assertCommandSuccess(listCommand, ListCommand.MESSAGE_SUCCESS, model);
    }

    @Test
    public void execute_storageThrowsIoException_throwsCommandException() {
        assertCommandFailureForExceptionFromStorage(DUMMY_IO_EXCEPTION, AddCommand.MESSAGE_SAVE_ERROR);
    }

    @Test
    public void execute_storageThrowsAdException_throwsCommandException() {
        assertCommandFailureForExceptionFromStorage(DUMMY_AD_EXCEPTION, AddCommand.MESSAGE_SAVE_ERROR);
    }

    @Test
    public void execute_addTagSaveFailure_modelUnchangedAndRetrySucceeds() throws Exception {
        useStorageThatFailsNextSave();
        Tag publicity = new Tag("Publicity");

        assertThrows(CommandException.class, Messages.withUsage(MutatingTagCommand.MESSAGE_SAVE_ERROR,
                CreateTagCommand.MESSAGE_USAGE), () -> logic.execute("addtag t/Publicity"));
        assertFalse(model.hasTag(publicity));

        CommandResult retryResult = logic.execute("addtag t/Publicity");
        assertEquals("Created tag: Publicity.", retryResult.getFeedbackToUser());
        assertTrue(model.hasTag(publicity));
    }

    @Test
    public void execute_editTagSaveFailure_modelUnchangedAndRetrySucceeds() throws Exception {
        Tag publicity = new Tag("Publicity");
        Tag media = new Tag("Media");
        model.addTag(publicity);
        useStorageThatFailsNextSave();

        assertThrows(CommandException.class, Messages.withUsage(MutatingTagCommand.MESSAGE_SAVE_ERROR,
                EditTagCommand.MESSAGE_USAGE), () -> logic.execute("edittag Publicity t/Media"));
        assertTrue(model.hasTag(publicity));
        assertFalse(model.hasTag(media));

        CommandResult retryResult = logic.execute("edittag Publicity t/Media");
        assertEquals("Publicity successfully renamed to Media.", retryResult.getFeedbackToUser());
        assertFalse(model.hasTag(publicity));
        assertTrue(model.hasTag(media));
    }

    @Test
    public void execute_deleteTagSaveFailure_modelUnchangedAndRetrySucceeds() throws Exception {
        Tag publicity = new Tag("Publicity");
        model.addTag(publicity);
        useStorageThatFailsNextSave();

        assertThrows(CommandException.class, Messages.withUsage(MutatingTagCommand.MESSAGE_SAVE_ERROR,
                DeleteTagCommand.MESSAGE_USAGE), () -> logic.execute("deletetag t/Publicity"));
        assertTrue(model.hasTag(publicity));

        CommandResult retryResult = logic.execute("deletetag t/Publicity");
        assertEquals("Publicity successfully deleted.", retryResult.getFeedbackToUser());
        assertFalse(model.hasTag(publicity));
    }

    @Test
    public void execute_addTagLoadFailure_modelUnchanged() {
        JsonCoordiMateStorage coordiMateStorage =
                new JsonCoordiMateStorage(temporaryFolder.resolve("coordiMate.json")) {
                    @Override
                    public Optional<ReadOnlyCoordiMate> readCoordiMate() throws DataLoadingException {
                        throw new DataLoadingException(DUMMY_IO_EXCEPTION);
                    }
                };
        JsonUserPrefsStorage userPrefsStorage =
                new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json"));
        logic = new LogicManager(model, new StorageManager(coordiMateStorage, userPrefsStorage));

        assertThrows(CommandException.class, Messages.withUsage(MutatingTagCommand.MESSAGE_LOAD_ERROR,
                CreateTagCommand.MESSAGE_USAGE), () -> logic.execute("addtag t/Publicity"));
        assertFalse(model.hasTag(new Tag("Publicity")));
    }

    @Test
    public void execute_markAttendanceMissingEvent_throwsCommandException() {
        String command = "markattendance evn/Gala mem/Alice Pauline att/present";
        String expectedMessage = String.format(MarkAttendanceCommand.MESSAGE_EVENT_NOT_FOUND, "Gala");

        assertCommandException(command, expectedMessage);
    }

    @Test
    public void execute_commandErrors_includeUsageExactlyOnce() {
        String[][] cases = {
            {"delete 9", "No contact exists with member ID: 9. Use an existing member ID."},
            {"edit 9 r/Lead", "No contact exists with member ID: 9. Use an existing member ID."},
            {"assign evn/Missing c/1", "Event Missing does not exist. Use a saved event name."},
            {"unassign evn/Missing c/1", "Event Missing does not exist. Use a saved event name."},
            {"members evn/Missing", "Event Missing does not exist. Use a saved event name."},
            {"deleteevent evn/Missing", "Event Missing does not exist. Use a saved event name."},
            {"editevent evn/Missing nevn/New", "Event Missing does not exist. Use a saved event name."},
            {"markattendance evn/Missing mem/Alice att/present",
                "Event Missing does not exist. Use a saved event name."},
            {"deletetag t/Missing", "Tag Missing does not exist. Use a saved tag name."},
            {"edittag Missing t/New", "Tag Missing does not exist. Use a saved tag name."},
            {"addtag t/EXCO", "A tag with this name already exists. No changes were made."}
        };
        for (String[] entry : cases) {
            String usage = CoordiMateParser.getUsage(entry[0].split(" ", 2)[0]);
            String message = Assertions.assertThrows(CommandException.class, () ->
                    logic.execute(entry[0])).getMessage();
            assertEquals(entry[1] + "\n" + usage, message, entry[0]);
            assertEquals(1, message.split("\nParameters: ", -1).length - 1, entry[0]);
        }
    }

    @Test
    public void execute_readOnlyCommandSaveFailure_includesUsage() {
        useStorageThatFailsNextSave();
        String expected = String.format(LogicManager.FILE_OPS_ERROR_FORMAT, DUMMY_IO_EXCEPTION.getMessage())
                + "\n" + ListCommand.MESSAGE_USAGE;
        assertThrows(CommandException.class, expected, () -> logic.execute("list"));
    }

    @Test
    public void getFilteredPersonList_modifyList_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> logic.getFilteredPersonList().remove(0));
    }

    @Test
    public void getTagList_returnsUnmodifiableDefaultTags() {
        assertEquals(model.getTagList(), logic.getTagList());
        assertEquals(new Tag("EXCO"), logic.getTagList().get(0));
        assertThrows(UnsupportedOperationException.class, () -> logic.getTagList().remove(0));
    }

    /**
     * Executes the command and confirms that
     * - no exceptions are thrown <br>
     * - the feedback message is equal to {@code expectedMessage} <br>
     * - the internal model manager state is the same as that in {@code expectedModel} <br>.
     *
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandSuccess(String inputCommand, String expectedMessage,
            Model expectedModel) throws CommandException, ParseException {
        CommandResult result = logic.execute(inputCommand);
        assertEquals(expectedMessage, result.getFeedbackToUser());
        assertEquals(expectedModel, model);
    }

    /**
     * Executes the command, confirms that a ParseException is thrown and that the result message is correct.
     *
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertParseException(String inputCommand, String expectedMessage) {
        assertCommandFailure(inputCommand, ParseException.class, expectedMessage);
    }

    /**
     * Executes the command, confirms that a CommandException is thrown and that the result message is correct.
     *
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandException(String inputCommand, String expectedMessage) {
        assertCommandFailure(inputCommand, CommandException.class, expectedMessage);
    }

    /**
     * Executes the command, confirms that the exception is thrown and that the result message is correct.
     *
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandFailure(String inputCommand, Class<? extends Throwable> expectedException,
            String expectedMessage) {
        Model expectedModel = new ModelManager(model.getCoordiMate(), new UserPrefs());
        assertCommandFailure(inputCommand, expectedException, expectedMessage, expectedModel);
    }

    /**
     * Executes the command and confirms that
     * - the {@code expectedException} is thrown <br>
     * - the resulting error message is equal to {@code expectedMessage} <br>
     * - the internal model manager state is the same as that in {@code expectedModel} <br>.
     *
     * @see #assertCommandSuccess(String, String, Model)
     */
    private void assertCommandFailure(String inputCommand, Class<? extends Throwable> expectedException,
            String expectedMessage, Model expectedModel) {
        assertThrows(expectedException, Messages.withUsage(expectedMessage,
                CoordiMateParser.getUsage(inputCommand.split(" ", 2)[0])), () -> logic.execute(inputCommand));
        assertEquals(expectedModel, model);
    }

    /**
     * Tests the Logic component's handling of an {@code IOException} thrown by the Storage component.
     *
     * @param e the exception to be thrown by the Storage component.
     * @param expectedMessage the message expected inside exception thrown by the Logic component.
     */
    private void assertCommandFailureForExceptionFromStorage(IOException e, String expectedMessage) {
        Path prefPath = temporaryFolder.resolve("ExceptionUserPrefs.json");

        // Inject LogicManager with a JsonCoordiMateStorage that throws the IOException e when saving
        JsonCoordiMateStorage coordiMateStorage = new JsonCoordiMateStorage(prefPath) {
            @Override
            public void saveCoordiMate(ReadOnlyCoordiMate coordiMate) throws IOException {
                throw e;
            }
        };

        JsonUserPrefsStorage userPrefsStorage =
                new JsonUserPrefsStorage(temporaryFolder.resolve("ExceptionUserPrefs.json"));
        StorageManager storage = new StorageManager(coordiMateStorage, userPrefsStorage);

        logic = new LogicManager(model, storage);

        // Triggers the saveCoordiMate method by executing an add command
        String addCommand = AddCommand.COMMAND_WORD + NAME_DESC_AMY + PHONE_DESC_AMY
                + EMAIL_DESC_AMY + " r/NA" + ADDRESS_DESC_AMY;
        assertCommandFailure(addCommand, CommandException.class, expectedMessage);
    }

    private void useStorageThatFailsNextSave() {
        Path dataPath = temporaryFolder.resolve("coordiMate.json");
        JsonCoordiMateStorage coordiMateStorage = new JsonCoordiMateStorage(dataPath) {
            private boolean failNextSave = true;

            @Override
            public void saveCoordiMate(ReadOnlyCoordiMate coordiMate) throws IOException {
                if (failNextSave) {
                    failNextSave = false;
                    throw DUMMY_IO_EXCEPTION;
                }
                super.saveCoordiMate(coordiMate);
            }
        };
        JsonUserPrefsStorage userPrefsStorage =
                new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json"));
        logic = new LogicManager(model, new StorageManager(coordiMateStorage, userPrefsStorage));
    }
}
