package coordimate.logic.commands;

import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BENSON;
import static coordimate.testutil.TypicalPersons.CARL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import coordimate.commons.core.index.Index;
import coordimate.logic.commands.EditCommand.EditPersonDescriptor;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.ModelManager;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.person.Person;
import coordimate.model.person.Phone;
import coordimate.model.tag.Tag;

public class ArchiveCommandTest {
    private ModelManager model;

    @BeforeEach
    public void setUp() {
        model = new ModelManager();
        model.addPerson(ALICE);
        model.addPerson(BENSON);
        model.addPerson(CARL);
    }

    @Test
    public void execute_archive_hidesContactAndKeepsDetails() throws Exception {
        assertEquals("Archived contact: " + BENSON.getName() + ".",
                archive(2).execute(model).getFeedbackToUser());

        assertEquals(List.of(ALICE, CARL), model.getFilteredPersonList());
        assertEquals(List.of(ALICE, BENSON.withArchived(true), CARL), model.getCoordiMate().getPersonList());
    }

    @Test
    public void execute_unarchiveInArchivedView_restoresContact() throws Exception {
        archive(1).execute(model);
        model.setShowingArchived(true);
        assertEquals(List.of(ALICE.withArchived(true)), model.getFilteredPersonList());

        assertEquals("Unarchived contact: " + ALICE.getName() + ".",
                unarchive(1).execute(model).getFeedbackToUser());
        assertEquals(List.of(), model.getFilteredPersonList());
        model.setShowingArchived(false);
        assertEquals(List.of(ALICE, BENSON, CARL), model.getFilteredPersonList());
    }

    @Test
    public void execute_alreadyInRequestedState_rejectedWithoutChanges() throws Exception {
        assertEquals(String.format(ArchiveCommand.MESSAGE_NOT_ARCHIVED, ALICE.getName()),
                assertThrows(CommandException.class, () -> unarchive(1).execute(model)).getMessage());

        archive(1).execute(model);
        model.setShowingArchived(true);
        assertEquals(String.format(ArchiveCommand.MESSAGE_ALREADY_ARCHIVED, ALICE.getName()),
                assertThrows(CommandException.class, () -> archive(1).execute(model)).getMessage());
        assertEquals(List.of(ALICE.withArchived(true), BENSON, CARL), model.getCoordiMate().getPersonList());
    }

    @Test
    public void execute_indexOutsideDisplayedList_rejected() throws Exception {
        assertEquals(String.format(ArchiveCommand.MESSAGE_INVALID_INDEX, 4),
                assertThrows(CommandException.class, () -> archive(4).execute(model)).getMessage());

        archive(1).execute(model);
        assertEquals(String.format(ArchiveCommand.MESSAGE_INVALID_INDEX, 3),
                assertThrows(CommandException.class, () -> archive(3).execute(model)).getMessage());
        assertEquals(List.of(BENSON, CARL), model.getFilteredPersonList());
    }

    @Test
    public void execute_archivedContact_staysEventMemberAndKeepsStatusThroughEdits() throws Exception {
        Event concert = new Event("Concert", new EventTime("08-08-2026"), new EventTime("08-08-2026"),
                List.of(ALICE.getName()));
        model.addEvent(concert);
        archive(1).execute(model);
        assertEquals(List.of(concert), model.getCoordiMate().getEventList());

        model.setShowingArchived(true);
        EditPersonDescriptor descriptor = new EditPersonDescriptor();
        descriptor.setPhone(new Phone("91234567"));
        new EditCommand(Index.fromOneBased(1), descriptor).execute(model);
        Person edited = model.getCoordiMate().getPersonList().getFirst();
        assertEquals(new Phone("91234567"), edited.getPhone());
        assertTrue(edited.isArchived());

        Tag friends = ALICE.getTags().iterator().next();
        model.registerTag(friends);
        model.setTag(friends, new Tag("Buddies"));
        assertTrue(model.getCoordiMate().getPersonList().getFirst().isArchived());
        model.deleteTag(new Tag("Buddies"));
        assertTrue(model.getCoordiMate().getPersonList().getFirst().isArchived());
    }

    @Test
    public void equals() {
        ArchiveCommand archiveFirst = archive(1);

        assertEquals(archiveFirst, archiveFirst);
        assertEquals(archiveFirst, archive(1));
        assertEquals(archiveFirst.hashCode(), archive(1).hashCode());
        assertNotEquals(archiveFirst, unarchive(1));
        assertNotEquals(archiveFirst, archive(2));
        assertNotEquals(archiveFirst, new ArchiveEventCommand("1", true));
        assertNotEquals(archiveFirst, null);
        assertThrows(NullPointerException.class, () -> new ArchiveCommand(null, true));
    }

    private static ArchiveCommand archive(int oneBasedIndex) {
        return new ArchiveCommand(Index.fromOneBased(oneBasedIndex), true);
    }

    private static ArchiveCommand unarchive(int oneBasedIndex) {
        return new ArchiveCommand(Index.fromOneBased(oneBasedIndex), false);
    }
}
