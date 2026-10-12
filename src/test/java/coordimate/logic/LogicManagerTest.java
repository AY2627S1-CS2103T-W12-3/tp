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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import coordimate.commons.exceptions.DataLoadingException;
import coordimate.logic.commands.AddCommand;
import coordimate.logic.commands.CommandResult;
import coordimate.logic.commands.DeleteCommand;
import coordimate.logic.commands.ListCommand;
import coordimate.logic.commands.MarkAttendanceCommand;
import coordimate.logic.commands.MutatingTagCommand;
import coordimate.logic.commands.exceptions.CommandException;
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
        assertCommandException(deleteCommand, String.format(DeleteCommand.MESSAGE_INVALID_INDEX, 9));
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
    public void execute_newTagSaveFailure_modelUnchangedAndRetrySucceeds() throws Exception {
        useStorageThatFailsNextSave();
        Tag publicity = new Tag("Publicity");

        assertThrows(CommandException.class, MutatingTagCommand.MESSAGE_SAVE_ERROR, () ->
                logic.execute("newtag t/Publicity"));
        assertFalse(model.hasTag(publicity));

        CommandResult retryResult = logic.execute("newtag t/Publicity");
        assertEquals("Created tag: Publicity.", retryResult.getFeedbackToUser());
        assertTrue(model.hasTag(publicity));
    }

    @Test
    public void execute_editTagSaveFailure_modelUnchangedAndRetrySucceeds() throws Exception {
        Tag publicity = new Tag("Publicity");
        Tag media = new Tag("Media");
        model.addTag(publicity);
        useStorageThatFailsNextSave();

        assertThrows(CommandException.class, MutatingTagCommand.MESSAGE_SAVE_ERROR, () ->
                logic.execute("edittag Publicity t/Media"));
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

        assertThrows(CommandException.class, MutatingTagCommand.MESSAGE_SAVE_ERROR, () ->
                logic.execute("deletetag t/Publicity"));
        assertTrue(model.hasTag(publicity));

        CommandResult retryResult = logic.execute("deletetag t/Publicity");
        assertEquals("Publicity successfully deleted.", retryResult.getFeedbackToUser());
        assertFalse(model.hasTag(publicity));
    }

    @Test
    public void execute_newTagLoadFailure_modelUnchanged() {
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

        assertThrows(CommandException.class, MutatingTagCommand.MESSAGE_LOAD_ERROR, () ->
                logic.execute("newtag t/Publicity"));
        assertFalse(model.hasTag(new Tag("Publicity")));
    }

    @Test
    public void execute_markAttendanceMissingEvent_throwsCommandException() {
        String command = "markattendance evn/Gala mem/Alice Pauline att/present";
        String expectedMessage = String.format(MarkAttendanceCommand.MESSAGE_EVENT_NOT_FOUND, "Gala");

        assertCommandException(command, expectedMessage);
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
        assertThrows(expectedException, expectedMessage, () -> logic.execute(inputCommand));
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
