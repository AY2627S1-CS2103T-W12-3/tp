package coordimate.logic;

import static coordimate.testutil.TypicalPersons.AMY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import coordimate.logic.commands.AddCommand;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.CoordiMate;
import coordimate.model.ModelManager;
import coordimate.model.ReadOnlyCoordiMate;
import coordimate.model.tag.Tag;
import coordimate.storage.JsonCoordiMateStorage;
import coordimate.storage.JsonUserPrefsStorage;
import coordimate.storage.StorageManager;

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

        assertEquals("Contact saved successfully: New Contact",
                newLogic(model, storage).execute(COMMAND + " t/Publicity t/EXCO").getFeedbackToUser());

        assertEquals(model.getCoordiMate(), storage.readCoordiMate().orElseThrow());
        assertEquals(2, model.getFilteredPersonList().size());
        assertEquals(initialTagCount + 1, model.getTagList().size());
        assertTrue(model.getTagList().contains(new Tag("Publicity")));
        assertTrue(storage.readCoordiMate().orElseThrow().getTagList().contains(new Tag("Publicity")));
        assertEquals("New Contact", storage.readCoordiMate().orElseThrow().getPersonList().getLast().getName()
                .getFullName());
    }

    @Test
    public void execute_saveFailure_modelAndFileUnchanged() throws Exception {
        IOException[] failures = {new IOException("Disk full"), new AccessDeniedException("data")};
        for (IOException failure : failures) {
            Path path = temporaryFolder.resolve("failed.json");
            ModelManager model = new ModelManager();
            model.addPerson(AMY);
            model.updateFilteredPersonList(person -> false);
            new JsonCoordiMateStorage(path).saveCoordiMate(model.getCoordiMate());
            String beforeFile = Files.readString(path);
            CoordiMate beforeModel = new CoordiMate(model.getCoordiMate());
            JsonCoordiMateStorage storage = new JsonCoordiMateStorage(path) {
                @Override
                public void saveCoordiMate(ReadOnlyCoordiMate data) throws IOException {
                    throw failure;
                }
            };

            Logic logic = newLogic(model, storage);
            CommandException exception = assertThrows(CommandException.class, () -> logic.execute(COMMAND));
            assertEquals(AddCommand.MESSAGE_SAVE_ERROR, exception.getMessage());
            assertSame(failure, exception.getCause());
            assertEquals(beforeModel, model.getCoordiMate());
            assertTrue(model.getFilteredPersonList().isEmpty());
            assertEquals(beforeFile, Files.readString(path));
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

            Logic logic = newLogic(model, new JsonCoordiMateStorage(path));
            CommandException exception = assertThrows(CommandException.class, () -> logic.execute(COMMAND));
            assertEquals(AddCommand.MESSAGE_LOAD_ERROR, exception.getMessage());
            assertEquals(beforeModel, model.getCoordiMate());
            assertEquals(contents, Files.readString(path));
        }
    }

    private Logic newLogic(ModelManager model, JsonCoordiMateStorage storage) {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));
    }
}
