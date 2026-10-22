package coordimate.logic.commands;

import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BENSON;
import static coordimate.testutil.TypicalPersons.CARL;
import static coordimate.testutil.TypicalPersons.DANIEL;
import static coordimate.testutil.TypicalPersons.getTypicalCoordiMate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
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
import coordimate.model.person.Name;

public class AssignCommandTest {
    private final Event concert = new Event("Final Concert",
            new EventTime("08-08-2026 15:00"), new EventTime("08-08-2026 18:00"));
    private final Event fair = new Event("Fair", new EventTime("09-10-2026"), new EventTime("10-10-2026"));
    private ModelManager model;

    @BeforeEach
    public void setUp() {
        model = new ModelManager(getTypicalCoordiMate(), new UserPrefs());
        model.addEvent(concert);
        model.addEvent(fair);
    }

    @Test
    public void execute_singleContact_success() throws Exception {
        CommandResult result = assign("Final Concert", 2).execute(model);
        assertEquals("Assigned 1 member(s) to Final Concert.", result.getFeedbackToUser());
        assertEquals(CommandResult.View.NONE, result.getViewToShow());
        assertEquals(List.of(withMembers(concert, BENSON.getName()), fair), model.getCoordiMate().getEventList());
    }

    @Test
    public void execute_multipleContacts_assignedInCommandOrder() throws Exception {
        assertEquals("Assigned 3 member(s) to Final Concert.",
                assign("Final Concert", 3, 1, 4).execute(model).getFeedbackToUser());
        assertEquals(withMembers(concert, CARL.getName(), ALICE.getName(), DANIEL.getName()),
                model.getCoordiMate().getEventList().getFirst());
    }

    @Test
    public void execute_differentCaseEventName_usesSavedName() throws Exception {
        assertEquals("Assigned 1 member(s) to Final Concert.",
                assign("fINAL cONCERT", 1).execute(model).getFeedbackToUser());
        assertEquals(withMembers(concert, ALICE.getName()), model.getCoordiMate().getEventList().getFirst());
    }

    @Test
    public void execute_someAlreadyAssigned_assignsOnlyNewContacts() throws Exception {
        assign("Final Concert", 1, 2).execute(model);
        assertEquals("Assigned 1 member(s) to Final Concert. 2 contact(s) were already assigned.",
                assign("Final Concert", 2, 3, 1).execute(model).getFeedbackToUser());
        assertEquals(withMembers(concert, ALICE.getName(), BENSON.getName(), CARL.getName()),
                model.getCoordiMate().getEventList().getFirst());
    }

    @Test
    public void execute_existingAttendance_preserved() throws Exception {
        assign("Final Concert", 1).execute(model);
        new MarkAttendanceCommand("Final Concert", ALICE.getName(), AttendanceStatus.PRESENT).execute(model);
        assign("Final Concert", 2).execute(model);
        assertEquals(new Event(concert.getName(), concert.getStartTime(), concert.getEndTime(),
                List.of(ALICE.getName(), BENSON.getName()), Map.of(ALICE.getName(), AttendanceStatus.PRESENT)),
                model.getCoordiMate().getEventList().getFirst());
        assertEquals(String.format(MarkAttendanceCommand.MESSAGE_ALREADY_MARKED, ALICE.getName(),
                AttendanceStatus.PRESENT, "Final Concert"),
                new MarkAttendanceCommand("Final Concert", ALICE.getName(), AttendanceStatus.PRESENT)
                        .execute(model).getFeedbackToUser());
    }

    @Test
    public void execute_repeatedId_countedOnce() throws Exception {
        assertEquals("Assigned 2 member(s) to Final Concert.",
                assign("Final Concert", 1, 1, 3).execute(model).getFeedbackToUser());
        assertEquals(withMembers(concert, ALICE.getName(), CARL.getName()),
                model.getCoordiMate().getEventList().getFirst());
    }

    @Test
    public void execute_allAlreadyAssigned_noChanges() throws Exception {
        assign("Final Concert", 1, 2).execute(model);
        List<Event> before = List.copyOf(model.getCoordiMate().getEventList());
        assertEquals("All specified contacts are already assigned to Final Concert. No changes were made.",
                assertThrows(CommandException.class, () -> assign("final concert", 2, 1).execute(model)).getMessage());
        assertEquals(before, model.getCoordiMate().getEventList());
    }

    @Test
    public void execute_missingEvent_noChanges() {
        assertEquals("Event Gala does not exist. Use a saved event name.",
                assertThrows(CommandException.class, () -> assign("Gala", 1).execute(model)).getMessage());
        assertEquals(List.of(concert, fair), model.getCoordiMate().getEventList());
    }

    @Test
    public void execute_unknownMemberId_noChanges() {
        int unknownId = 99;
        assertEquals("No contact exists with member ID: 99. Use an existing member ID.",
                assertThrows(CommandException.class, () ->
                        assign("Final Concert", 1, unknownId, 2).execute(model)).getMessage());
        assertEquals(List.of(concert, fair), model.getCoordiMate().getEventList());
    }

    @Test
    public void execute_filteredList_usesPermanentIds() throws Exception {
        model.updateFilteredPersonList(person -> person.equals(CARL) || person.equals(DANIEL));
        assign("Final Concert", 2).execute(model);
        assertEquals(withMembers(concert, BENSON.getName()), model.getCoordiMate().getEventList().getFirst());
        assign("Final Concert", 3).execute(model);
        assertEquals(withMembers(concert, BENSON.getName(), CARL.getName()),
                model.getCoordiMate().getEventList().getFirst());
        assertEquals(List.of(CARL, DANIEL), model.getFilteredPersonList());
    }

    @Test
    public void equalityAndNullArguments() {
        AssignCommand command = assign("Final Concert", 1, 2);
        assertEquals(command, assign(" Final Concert ", 1, 2));
        assertEquals(command.hashCode(), assign("Final Concert", 1, 2).hashCode());
        assertNotEquals(command, assign("Final Concert", 2, 1));
        assertNotEquals(command, assign("Fair", 1, 2));
        assertNotEquals(command, null);
        assertThrows(NullPointerException.class, () -> command.execute(null));
        assertThrows(NullPointerException.class, () -> new AssignCommand(null, List.of(new MemberId(1))));
        assertThrows(NullPointerException.class, () -> new AssignCommand("Final Concert", null));
    }

    private static AssignCommand assign(String eventName, Integer... memberIds) {
        return new AssignCommand(eventName, Arrays.stream(memberIds).map(MemberId::new).toList());
    }

    private static Event withMembers(Event event, Name... members) {
        return new Event(event.getName(), event.getStartTime(), event.getEndTime(), List.of(members));
    }
}
