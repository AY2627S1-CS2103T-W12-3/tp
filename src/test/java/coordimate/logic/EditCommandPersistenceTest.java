package coordimate.logic;

import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BENSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import coordimate.commons.exceptions.DataLoadingException;
import coordimate.logic.commands.EditCommand;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.CoordiMate;
import coordimate.model.ModelManager;
import coordimate.model.ReadOnlyCoordiMate;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.person.Person;
import coordimate.model.tag.Tag;
import coordimate.storage.JsonCoordiMateStorage;
import coordimate.testutil.PersonBuilder;

public class EditCommandPersistenceTest {
    private static final String EDIT = "edit 2 n/Benson Tan at/Alumni2026";

    @TempDir
    public Path temporaryFolder;

    @Test
    public void execute_filteredMemberId_savedAndReloadedWithRenamedEventMemberAndCustomTag() throws Exception {
        ModelManager model = modelWithAssignedEvent();
        model.updateFilteredPersonList(person -> person.equals(BENSON));
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(temporaryFolder.resolve("data.json"));

        assertEquals("Contact updated successfully:\n"
                + "Name: Benson Tan\n"
                + "Phone: 98765432\n"
                + "Email: johnd@example.com\n"
                + "Role: NA", PersistenceTestUtil.newLogic(model, storage, temporaryFolder)
                        .execute(EDIT).getFeedbackToUser());

        ReadOnlyCoordiMate saved = storage.readCoordiMate().orElseThrow();
        assertEquals(model.getCoordiMate(), saved);
        assertEquals("Benson Tan", saved.getPersonList().get(1).getName().getFullName());
        assertEquals(List.of(ALICE.getName(), saved.getPersonList().get(1).getName()),
                saved.getEventList().getFirst().getMembers());
        assertTrue(saved.getTagList().contains(new Tag("Alumni2026")));
        assertEquals(2, model.getFilteredPersonList().size());
    }

    @Test
    public void execute_targetOutsideDisplayedList_savedAndReloaded() throws Exception {
        ModelManager model = modelWithAssignedEvent();
        model.updateFilteredPersonList(person -> false);
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(temporaryFolder.resolve("data.json"));

        PersistenceTestUtil.newLogic(model, storage, temporaryFolder)
                .execute("edit target/alice@example.com r/President");

        assertEquals("President", model.getCoordiMate().getPersonList().getFirst().getRole().toString());
        assertEquals(model.getCoordiMate(), storage.readCoordiMate().orElseThrow());
        assertEquals(2, model.getFilteredPersonList().size());
    }

    @Test
    public void execute_nonPhoneEditOnLegacyContact_preservesPhoneAndSaves() throws Exception {
        ModelManager model = new ModelManager();
        model.addPerson(new PersonBuilder(ALICE).withPhone("123").build());
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(temporaryFolder.resolve("legacy.json"));
        storage.saveCoordiMate(model.getCoordiMate());

        PersistenceTestUtil.newLogic(model, storage, temporaryFolder).execute("edit 1 r/President");

        Person editedPerson = storage.readCoordiMate().orElseThrow().getPersonList().getFirst();
        assertEquals("123", editedPerson.getPhone().getValue());
        assertEquals("President", editedPerson.getRole().getValue());
        assertEquals(model.getCoordiMate(), storage.readCoordiMate().orElseThrow());
    }

    @Test
    public void execute_saveFailure_modelAndFileUnchanged() throws Exception {
        IOException[] failures = {new IOException("Disk full"), new AccessDeniedException("data")};
        for (IOException failure : failures) {
            ModelManager model = modelWithAssignedEvent();
            model.updateFilteredPersonList(person -> person.equals(BENSON));
            Path path = temporaryFolder.resolve("failed.json");
            PersistenceTestUtil.BeforeState before = PersistenceTestUtil.seedThenCapture(model, path);
            List<Person> beforeDisplayed = PersistenceTestUtil.captureDisplayedList(model);
            JsonCoordiMateStorage failingStorage = PersistenceTestUtil.failingStorage(path, failure);

            CommandException exception = assertThrows(CommandException.class, () ->
                    PersistenceTestUtil.newLogic(model, failingStorage, temporaryFolder).execute(EDIT));

            assertEquals(Messages.withUsage(EditCommand.MESSAGE_SAVE_ERROR,
                    EditCommand.MESSAGE_USAGE), exception.getMessage());
            PersistenceTestUtil.assertSaveFailurePreservedState(exception, failure, model, before);
            assertEquals(beforeDisplayed, model.getFilteredPersonList());
        }
    }

    @Test
    public void execute_corruptedFile_modelAndFileUnchanged() throws Exception {
        String[] corruptedContents = {"invalid json", "null", "{}", "{\"persons\":[null]}"};
        for (String contents : corruptedContents) {
            Path path = temporaryFolder.resolve("corrupt.json");
            Files.writeString(path, contents);
            ModelManager model = modelWithAssignedEvent();
            model.updateFilteredPersonList(person -> false);
            CoordiMate beforeModel = new CoordiMate(model.getCoordiMate());

            Logic logic = PersistenceTestUtil.newLogic(model, new JsonCoordiMateStorage(path), temporaryFolder);
            CommandException exception = assertThrows(CommandException.class, () ->
                    logic.execute("edit target/alice@example.com r/President"));

            assertEquals(Messages.withUsage(EditCommand.MESSAGE_LOAD_ERROR,
                    EditCommand.MESSAGE_USAGE), exception.getMessage());
            assertTrue(exception.getCause() instanceof DataLoadingException);
            assertEquals(beforeModel, model.getCoordiMate());
            assertTrue(model.getFilteredPersonList().isEmpty());
            assertEquals(contents, Files.readString(path));
        }
    }

    private ModelManager modelWithAssignedEvent() {
        ModelManager model = new ModelManager();
        model.addPerson(ALICE);
        model.addPerson(BENSON);
        EventTime eventTime = new EventTime("08-08-2026");
        model.addEvent(new Event("Concert", eventTime, eventTime, List.of(ALICE.getName(), BENSON.getName())));
        return model;
    }
}
