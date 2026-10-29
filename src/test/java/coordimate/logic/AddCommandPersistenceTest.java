package coordimate.logic;

import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.AMY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import coordimate.logic.commands.AddCommand;
import coordimate.logic.commands.CommandResult;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.CoordiMate;
import coordimate.model.ModelManager;
import coordimate.model.UserPrefs;
import coordimate.model.tag.Tag;
import coordimate.storage.JsonCoordiMateStorage;

public class AddCommandPersistenceTest {
    private static final String COMMAND = "add n/New Contact p/82345678 e/new@example.com r/Logistics a/Somewhere";

    @TempDir
    public Path temporaryFolder;

    @Test
    public void execute_validContact_savedAndReloaded() throws Exception {
        ModelManager model = new ModelManager();
        model.addPerson(AMY);
        int initialTagCount = model.getTagList().size();
        model.updateFilteredPersonList(person -> false);
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(temporaryFolder.resolve("data.json"));

        CommandResult result = PersistenceTestUtil.newLogic(model, storage, temporaryFolder)
                .execute(COMMAND + " t/Publicity t/EXCO");

        assertEquals("Contact saved successfully: New Contact", result.getFeedbackToUser());
        assertEquals(model.getCoordiMate().getPersonList().getLast(), result.getContactToFocus().orElseThrow());
        assertTrue(result.getContactIndexToFocus().isEmpty());

        assertEquals(model.getCoordiMate(), storage.readCoordiMate().orElseThrow());
        assertEquals(2, model.getFilteredPersonList().size());
        assertEquals(initialTagCount + 1, model.getTagList().size());
        assertTrue(model.getTagList().contains(new Tag("Publicity")));
        assertTrue(storage.readCoordiMate().orElseThrow().getTagList().contains(new Tag("Publicity")));
        assertEquals("New Contact", storage.readCoordiMate().orElseThrow().getPersonList().getLast().getName()
                .getFullName());
    }

    @Test
    public void execute_memberIdsExhausted_modelAndFileUnchanged() throws Exception {
        Path path = temporaryFolder.resolve("exhausted.json");
        CoordiMate data = new CoordiMate(Integer.MAX_VALUE + 1L);
        data.addPerson(ALICE);
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(path);
        storage.saveCoordiMate(data);
        String beforeFile = Files.readString(path);
        ModelManager model = new ModelManager(storage.readCoordiMate().orElseThrow(), new UserPrefs());
        model.updateFilteredPersonList(person -> false);
        CoordiMate beforeModel = new CoordiMate(model.getCoordiMate());

        Logic logic = PersistenceTestUtil.newLogic(model, storage, temporaryFolder);
        CommandException exception = assertThrows(CommandException.class, () ->
                logic.execute(COMMAND + " t/Publicity"));

        assertEquals(Messages.withUsage("No more member IDs are available.",
                AddCommand.MESSAGE_USAGE), exception.getMessage());
        assertTrue(exception.getCause() instanceof IllegalStateException);
        assertEquals(beforeModel, model.getCoordiMate());
        assertTrue(model.getFilteredPersonList().isEmpty());
        assertEquals(beforeFile, Files.readString(path));
    }

    @Test
    public void execute_duplicateName_modelAndFileUnchanged() throws Exception {
        Path path = temporaryFolder.resolve("contacts.json");
        ModelManager model = new ModelManager();
        model.addPerson(AMY);
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(path);
        storage.saveCoordiMate(model.getCoordiMate());
        String beforeFile = Files.readString(path);
        CoordiMate beforeModel = new CoordiMate(model.getCoordiMate());

        Logic logic = PersistenceTestUtil.newLogic(model, storage, temporaryFolder);
        CommandException exception = assertThrows(CommandException.class, () ->
                logic.execute("add n/  aMy bEe  p/82345678 e/new@example.com r/Logistics"));

        assertEquals(Messages.withUsage(AddCommand.MESSAGE_DUPLICATE_PERSON,
                AddCommand.MESSAGE_USAGE), exception.getMessage());
        assertEquals(beforeModel, model.getCoordiMate());
        assertEquals(beforeFile, Files.readString(path));
    }

    @Test
    public void execute_saveFailure_modelAndFileUnchanged() throws Exception {
        IOException[] failures = {new IOException("Disk full"), new AccessDeniedException("data")};
        for (IOException failure : failures) {
            Path path = temporaryFolder.resolve("failed.json");
            ModelManager model = new ModelManager();
            model.addPerson(AMY);
            model.updateFilteredPersonList(person -> false);
            PersistenceTestUtil.BeforeState before = PersistenceTestUtil.seedThenCapture(model, path);
            JsonCoordiMateStorage storage = PersistenceTestUtil.failingStorage(path, failure);

            Logic logic = PersistenceTestUtil.newLogic(model, storage, temporaryFolder);
            CommandException exception = assertThrows(CommandException.class, () -> logic.execute(COMMAND));
            assertEquals(Messages.withUsage(AddCommand.MESSAGE_SAVE_ERROR,
                    AddCommand.MESSAGE_USAGE), exception.getMessage());
            PersistenceTestUtil.assertSaveFailurePreservedState(exception, failure, model, before);
            assertTrue(model.getFilteredPersonList().isEmpty());
        }
    }

    @Test
    public void execute_corruptedFile_modelAndFileUnchanged() throws Exception {
        String[] corruptedContents = {"invalid json", "null", "{}", "{\"persons\":[null]}"};
        for (String contents : corruptedContents) {
            Path path = temporaryFolder.resolve("corrupt.json");
            Files.writeString(path, contents);
            ModelManager model = new ModelManager();
            model.addPerson(AMY);
            CoordiMate beforeModel = new CoordiMate(model.getCoordiMate());

            Logic logic = PersistenceTestUtil.newLogic(model, new JsonCoordiMateStorage(path), temporaryFolder);
            CommandException exception = assertThrows(CommandException.class, () -> logic.execute(COMMAND));
            assertEquals(Messages.withUsage(AddCommand.MESSAGE_LOAD_ERROR,
                    AddCommand.MESSAGE_USAGE), exception.getMessage());
            assertEquals(beforeModel, model.getCoordiMate());
            assertEquals(contents, Files.readString(path));
        }
    }
}
