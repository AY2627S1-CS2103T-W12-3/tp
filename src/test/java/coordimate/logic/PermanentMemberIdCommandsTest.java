package coordimate.logic;

import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BENSON;
import static coordimate.testutil.TypicalPersons.CARL;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import coordimate.logic.commands.DeleteCommand;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.CoordiMate;
import coordimate.model.ModelManager;
import coordimate.model.UserPrefs;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.person.Person;
import coordimate.storage.JsonCoordiMateStorage;
import coordimate.storage.JsonUserPrefsStorage;
import coordimate.storage.StorageManager;
import coordimate.testutil.PersonBuilder;

public class PermanentMemberIdCommandsTest {
    private static final Person ALICE_12 = new PersonBuilder(ALICE).withMemberId(12).build();
    private static final Person BENSON_45 = new PersonBuilder(BENSON).withMemberId(45).withTags("Sponsor").build();
    private static final Person CARL_99 = new PersonBuilder(CARL).withMemberId(99).build();
    private static final List<String> VIEWS = List.of("list", "search Alice", "search Benson",
            "filter tag/Sponsor", "members evn/Concert", "search Nobody");

    @TempDir
    public Path testFolder;

    private ModelManager model;
    private JsonCoordiMateStorage storage;
    private LogicManager logic;

    @Test
    public void editId_acrossSearchesFiltersAndEmptyResults_targetsSameSavedMember() throws Exception {
        startApplication();
        for (int i = 0; i < VIEWS.size(); i++) {
            logic.execute(VIEWS.get(i));
            logic.execute("edit 12 m/Update " + i);
            assertEquals(new PersonBuilder(ALICE_12).withNote("Update " + i).build(),
                    model.getCoordiMate().getPersonList().get(1));
            assertEquals(BENSON_45, model.getCoordiMate().getPersonList().getFirst());
            assertEquals(CARL_99, model.getCoordiMate().getPersonList().getLast());
            assertEquals(model.getCoordiMate(), storage.readCoordiMate().orElseThrow());
        }
    }

    @Test
    public void deleteId_acrossSearchesFiltersAndEmptyResults_targetsSameSavedMember() throws Exception {
        for (String view : VIEWS) {
            startApplication();
            logic.execute(view);
            assertEquals(DeleteCommand.confirmationPrompt(ALICE_12), logic.execute("delete 12").getFeedbackToUser());
            logic.execute("y");
            assertEquals(List.of(BENSON_45, CARL_99), model.getCoordiMate().getPersonList(), view);
            assertEquals(List.of(BENSON_45.getName()), model.getCoordiMate().getEventList().getFirst().getMembers());
            assertEquals(model.getCoordiMate(), storage.readCoordiMate().orElseThrow());
        }
    }

    @Test
    public void unknownIds_neverFallBackToDisplayedPositionsOrChangeData() throws Exception {
        startApplication();
        logic.execute("search Benson");
        CoordiMate before = new CoordiMate(model.getCoordiMate());
        byte[] savedBefore = Files.readAllBytes(storage.getCoordiMateFilePath());
        for (int id : new int[] {1, 2, 3, 46}) {
            for (String input : List.of("edit " + id + " r/President", "delete " + id)) {
                CommandException error = assertThrows(CommandException.class, () -> logic.execute(input));
                assertTrue(error.getMessage().startsWith(String.format(Messages.MESSAGE_MEMBER_ID_NOT_FOUND, id)));
                assertEquals(before, model.getCoordiMate());
                assertEquals(List.of(BENSON_45), model.getFilteredPersonList());
                assertArrayEquals(savedBefore, Files.readAllBytes(storage.getCoordiMateFilePath()));
                assertFalse(logic.isAwaitingDeleteConfirmation());
            }
        }
    }

    @Test
    public void afterDeletionAndRestart_idsStillTargetSameMembers() throws Exception {
        startApplication();
        logic.execute("delete 45");
        logic.execute("y");
        model = new ModelManager(storage.readCoordiMate().orElseThrow(), new UserPrefs());
        logic = newLogic();
        logic.execute("search Alice");
        logic.execute("edit 12 r/President");
        assertEquals(new PersonBuilder(ALICE_12).withRole("President").build(),
                model.getCoordiMate().getPersonList().getFirst());
        assertThrows(CommandException.class, () -> logic.execute("delete 45"));
        assertEquals(List.of(CARL_99), model.getCoordiMate().getPersonList().subList(1, 2));
        assertEquals(model.getCoordiMate(), storage.readCoordiMate().orElseThrow());
    }

    @Test
    public void confirmation_afterReorderingAndFiltering_deletesOriginalId() throws Exception {
        startApplication();
        logic.execute("delete 12");
        CoordiMate reordered = new CoordiMate(model.getCoordiMate());
        reordered.setPersons(List.of(CARL_99, BENSON_45, ALICE_12));
        model.setCoordiMate(reordered);
        model.updateFilteredPersonList(person -> false);
        logic.execute("y");
        assertEquals(List.of(CARL_99, BENSON_45), model.getCoordiMate().getPersonList());
        assertEquals(model.getCoordiMate(), storage.readCoordiMate().orElseThrow());
    }

    @Test
    public void confirmation_memberEditedSincePrompt_requiresNewConfirmation() throws Exception {
        startApplication();
        logic.execute("delete 12");
        model.setPerson(ALICE_12, new PersonBuilder(ALICE_12).withRole("President").build());
        CoordiMate before = new CoordiMate(model.getCoordiMate());
        byte[] savedBefore = Files.readAllBytes(storage.getCoordiMateFilePath());
        CommandException error = assertThrows(CommandException.class, () -> logic.execute("y"));
        assertTrue(error.getMessage().startsWith(String.format(DeleteCommand.MESSAGE_TARGET_CHANGED, 12)));
        assertEquals(before, model.getCoordiMate());
        assertArrayEquals(savedBefore, Files.readAllBytes(storage.getCoordiMateFilePath()));
        assertFalse(logic.isAwaitingDeleteConfirmation());
    }

    @Test
    public void confirmation_memberRemovedSincePrompt_neverDeletesReplacement() throws Exception {
        startApplication();
        logic.execute("delete 12");
        model.deletePerson(ALICE_12);
        model.addPerson(new PersonBuilder().build());
        CoordiMate before = new CoordiMate(model.getCoordiMate());
        byte[] savedBefore = Files.readAllBytes(storage.getCoordiMateFilePath());
        CommandException error = assertThrows(CommandException.class, () -> logic.execute("y"));
        assertTrue(error.getMessage().startsWith(String.format(Messages.MESSAGE_MEMBER_ID_NOT_FOUND, 12)));
        assertEquals(before, model.getCoordiMate());
        assertArrayEquals(savedBefore, Files.readAllBytes(storage.getCoordiMateFilePath()));
        assertFalse(logic.isAwaitingDeleteConfirmation());
    }

    private void startApplication() throws IOException {
        CoordiMate data = new CoordiMate();
        data.setPersons(List.of(BENSON_45, ALICE_12, CARL_99));
        EventTime time = new EventTime("08-08-2026");
        data.addEvent(new Event("Concert", time, time, List.of(ALICE_12.getName(), BENSON_45.getName())));
        model = new ModelManager(data, new UserPrefs());
        storage = new JsonCoordiMateStorage(testFolder.resolve("members.json"));
        storage.saveCoordiMate(model.getCoordiMate());
        logic = newLogic();
    }

    private LogicManager newLogic() {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(testFolder.resolve("prefs.json"))));
    }
}
