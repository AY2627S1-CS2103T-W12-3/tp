package coordimate.logic.commands;

import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.getTypicalCoordiMate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.ModelManager;
import coordimate.model.UserPrefs;
import coordimate.model.event.AttendanceStatus;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.person.MemberId;

public class ArchiveEventCommandTest {
    private final Event concert = new Event("Final Concert", new EventTime("08-08-2026 15:00"),
            new EventTime("08-08-2026 18:00"), List.of(ALICE.getName()),
            Map.of(ALICE.getName(), AttendanceStatus.PRESENT));
    private final Event fair = new Event("Fair", new EventTime("09-10-2026"), new EventTime("10-10-2026"));
    private ModelManager model;

    @BeforeEach
    public void setUp() {
        model = new ModelManager(getTypicalCoordiMate(), new UserPrefs());
        model.addEvent(concert);
        model.addEvent(fair);
    }

    @Test
    public void execute_archive_hidesEventAndKeepsDetails() throws Exception {
        assertEquals("Archived Event Final Concert.",
                new ArchiveEventCommand("fINAL cONCERT", true).execute(model).getFeedbackToUser());

        Event archived = model.getCoordiMate().getEventList().getFirst();
        assertTrue(archived.isArchived());
        assertEquals(concert.withArchived(true), archived);
        assertEquals(concert.getMembers(), archived.getMembers());
        assertEquals(concert.getAttendanceRecord(), archived.getAttendanceRecord());
        assertEquals(List.of(fair), model.getFilteredEventList());
    }

    @Test
    public void execute_unarchive_restoresEventToList() throws Exception {
        new ArchiveEventCommand("Final Concert", true).execute(model);

        assertEquals("Unarchived Event Final Concert.",
                new ArchiveEventCommand(" Final Concert ", false).execute(model).getFeedbackToUser());
        assertEquals(List.of(concert, fair), model.getCoordiMate().getEventList());
        assertEquals(List.of(concert, fair), model.getFilteredEventList());
    }

    @Test
    public void execute_alreadyInRequestedState_rejectedWithoutChanges() throws Exception {
        assertEquals(String.format(ArchiveEventCommand.MESSAGE_NOT_ARCHIVED, "Fair"),
                assertThrows(CommandException.class, () ->
                        new ArchiveEventCommand("fair", false).execute(model)).getMessage());

        new ArchiveEventCommand("Fair", true).execute(model);
        assertEquals(String.format(ArchiveEventCommand.MESSAGE_ALREADY_ARCHIVED, "Fair"),
                assertThrows(CommandException.class, () ->
                        new ArchiveEventCommand("Fair", true).execute(model)).getMessage());
        assertEquals(List.of(concert, fair.withArchived(true)), model.getCoordiMate().getEventList());
    }

    @Test
    public void execute_unknownEvent_rejected() {
        for (boolean archive : new boolean[] {true, false}) {
            assertEquals(String.format(ArchiveEventCommand.MESSAGE_EVENT_NOT_FOUND, "Gala"),
                    assertThrows(CommandException.class, () ->
                            new ArchiveEventCommand("Gala", archive).execute(model)).getMessage());
        }
        assertEquals(List.of(concert, fair), model.getCoordiMate().getEventList());
    }

    @Test
    public void execute_archivedEvent_membersCannotChangeButOtherEditsAllowed() throws Exception {
        new ArchiveEventCommand("Final Concert", true).execute(model);
        String message = String.format(EventMembersCommand.MESSAGE_EVENT_ARCHIVED, "Final Concert");

        assertEquals(message, assertThrows(CommandException.class, () ->
                new AssignCommand("final concert", List.of(new MemberId(2))).execute(model)).getMessage());
        assertEquals(message, assertThrows(CommandException.class, () ->
                new UnassignCommand("Final Concert", List.of(new MemberId(1))).execute(model)).getMessage());
        assertEquals(concert.withArchived(true), model.getCoordiMate().getEventList().getFirst());

        new MarkAttendanceCommand("Final Concert", ALICE.getName(), AttendanceStatus.ABSENT).execute(model);
        Event marked = model.getCoordiMate().getEventList().getFirst();
        assertEquals(AttendanceStatus.ABSENT, marked.getAttendance(ALICE.getName()).orElseThrow());
        assertTrue(marked.isArchived());
    }

    @Test
    public void execute_addEventWithArchivedName_explainsHowToRestore() throws Exception {
        new ArchiveEventCommand("Fair", true).execute(model);
        Event duplicate = new Event("FAIR", new EventTime("11-10-2026"), new EventTime("11-10-2026"));

        assertEquals(String.format(AddEventCommand.MESSAGE_DUPLICATE_ARCHIVED_EVENT, "Fair"),
                assertThrows(CommandException.class, () ->
                        new AddEventCommand(duplicate).execute(model)).getMessage());
        assertFalse(model.getFilteredEventList().contains(duplicate));
    }

    @Test
    public void equals() {
        ArchiveEventCommand archive = new ArchiveEventCommand(" Fair ", true);

        assertEquals(archive, archive);
        assertEquals(archive, new ArchiveEventCommand("Fair", true));
        assertEquals(archive.hashCode(), new ArchiveEventCommand("Fair", true).hashCode());
        assertNotEquals(archive, new ArchiveEventCommand("Fair", false));
        assertNotEquals(archive, new ArchiveEventCommand("Gala", true));
        assertNotEquals(archive, new DeleteEventCommand("Fair"));
        assertNotEquals(archive, null);
        assertThrows(NullPointerException.class, () -> new ArchiveEventCommand(null, true));
    }
}
