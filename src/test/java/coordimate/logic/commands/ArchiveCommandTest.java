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

import coordimate.logic.Messages;
import coordimate.logic.commands.EditCommand.EditPersonDescriptor;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.ModelManager;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.person.ContactMatchesKeywordPredicate;
import coordimate.model.person.MemberId;
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
    public void execute_archiveOrUnarchive_opensContacts() throws Exception {
        assertEquals(CommandResult.View.CONTACTS, archive(2).execute(model).getViewToShow());
        assertEquals(CommandResult.View.CONTACTS,
                new ArchiveCommand(new MemberId(2), false).execute(model).getViewToShow());
    }

    @Test
    public void execute_contactHiddenByFilter_archivedById() throws Exception {
        model.updateFilteredPersonList(new ContactMatchesKeywordPredicate("Alice"));

        archive(3).execute(model);

        assertEquals(List.of(ALICE), model.getFilteredPersonList());
        assertTrue(model.getCoordiMate().getPersonList().get(2).isArchived());
    }

    @Test
    public void execute_unarchive_worksFromEitherView() throws Exception {
        archive(1).execute(model);
        archive(2).execute(model);

        assertEquals("Unarchived contact: " + ALICE.getName() + ".",
                unarchive(1).execute(model).getFeedbackToUser());
        assertEquals(List.of(ALICE, CARL), model.getFilteredPersonList());

        model.setShowingArchived(true);
        unarchive(2).execute(model);
        assertEquals(List.of(), model.getFilteredPersonList());
        model.setShowingArchived(false);
        assertEquals(List.of(ALICE, BENSON, CARL), model.getFilteredPersonList());
    }

    @Test
    public void execute_alreadyInRequestedState_rejectedWithoutChanges() throws Exception {
        assertEquals(String.format(ArchiveCommand.MESSAGE_NOT_ARCHIVED, ALICE.getName()),
                assertThrows(CommandException.class, () -> unarchive(1).execute(model)).getMessage());

        archive(1).execute(model);
        assertEquals(String.format(ArchiveCommand.MESSAGE_ALREADY_ARCHIVED, ALICE.getName()),
                assertThrows(CommandException.class, () -> archive(1).execute(model)).getMessage());
        assertEquals(List.of(ALICE.withArchived(true), BENSON, CARL), model.getCoordiMate().getPersonList());
    }

    @Test
    public void execute_unknownMemberId_rejected() {
        for (ArchiveCommand command : List.of(archive(99), unarchive(99))) {
            assertEquals(String.format(Messages.MESSAGE_MEMBER_ID_NOT_FOUND, 99),
                    assertThrows(CommandException.class, () -> command.execute(model)).getMessage());
        }
        assertEquals(List.of(ALICE, BENSON, CARL), model.getCoordiMate().getPersonList());
    }

    @Test
    public void execute_archivedContact_staysEventMemberAndKeepsStatusThroughEdits() throws Exception {
        Event concert = new Event("Concert", new EventTime("08-08-2026"), new EventTime("08-08-2026"),
                List.of(ALICE.getName()));
        model.addEvent(concert);
        archive(1).execute(model);
        assertEquals(List.of(concert), model.getCoordiMate().getEventList());

        EditPersonDescriptor descriptor = new EditPersonDescriptor();
        descriptor.setPhone(new Phone("91234567"));
        new EditCommand(new MemberId(1), descriptor).execute(model);
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
    public void execute_assignArchivedContact_rejectedButUnassignAllowed() throws Exception {
        Event concert = new Event("Concert", new EventTime("08-08-2026"), new EventTime("08-08-2026"),
                List.of(ALICE.getName()));
        model.addEvent(concert);
        archive(2).execute(model);

        assertEquals(String.format(AssignCommand.MESSAGE_CONTACT_ARCHIVED, BENSON.getName(), 2),
                assertThrows(CommandException.class, () ->
                        new AssignCommand("Concert", List.of(new MemberId(3), new MemberId(2))).execute(model))
                        .getMessage());
        assertEquals(List.of(concert), model.getCoordiMate().getEventList());

        archive(1).execute(model);
        new UnassignCommand("Concert", List.of(new MemberId(1))).execute(model);
        assertEquals(List.of(), model.getCoordiMate().getEventList().getFirst().getMembers());
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

    private static ArchiveCommand archive(int memberId) {
        return new ArchiveCommand(new MemberId(memberId), true);
    }

    private static ArchiveCommand unarchive(int memberId) {
        return new ArchiveCommand(new MemberId(memberId), false);
    }
}
