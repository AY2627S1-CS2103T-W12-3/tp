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

public class UnassignCommandTest {
    private final EventTime start = new EventTime("08-08-2026 15:00");
    private final EventTime end = new EventTime("08-08-2026 18:00");
    private final Event concert = event(ALICE.getName(), BENSON.getName(), CARL.getName());
    private final Event fair = new Event("Fair", new EventTime("09-10-2026"), new EventTime("10-10-2026"),
            List.of(ALICE.getName()));
    private ModelManager model;

    @BeforeEach
    public void setUp() {
        model = new ModelManager(getTypicalCoordiMate(), new UserPrefs());
        model.addEvent(concert);
        model.addEvent(fair);
    }

    @Test
    public void execute_singleMember_success() throws Exception {
        CommandResult result = unassign("Final Concert", 2).execute(model);
        assertEquals("Removed 1 member(s) from Final Concert.", result.getFeedbackToUser());
        assertEquals(CommandResult.View.NONE, result.getViewToShow());
        assertEquals(List.of(event(ALICE.getName(), CARL.getName()), fair), model.getCoordiMate().getEventList());
    }

    @Test
    public void execute_multipleMembers_remainingKeepOrder() throws Exception {
        assertEquals("Removed 2 member(s) from Final Concert.",
                unassign("Final Concert", 3, 1).execute(model).getFeedbackToUser());
        assertEquals(event(BENSON.getName()), model.getCoordiMate().getEventList().getFirst());
    }

    @Test
    public void execute_allMembers_eventHasNoMembers() throws Exception {
        unassign("Final Concert", 1, 2, 3).execute(model);
        assertEquals(event(), model.getCoordiMate().getEventList().getFirst());
    }

    @Test
    public void execute_differentCaseEventName_usesSavedName() throws Exception {
        assertEquals("Removed 1 member(s) from Final Concert.",
                unassign("fINAL cONCERT", 1).execute(model).getFeedbackToUser());
        assertEquals(event(BENSON.getName(), CARL.getName()), model.getCoordiMate().getEventList().getFirst());
    }

    @Test
    public void execute_someNotAssigned_removesOnlyMembers() throws Exception {
        assertEquals("Removed 1 member(s) from Final Concert. 2 contact(s) were not assigned.",
                unassign("Final Concert", 4, 2, 5).execute(model).getFeedbackToUser());
        assertEquals(event(ALICE.getName(), CARL.getName()), model.getCoordiMate().getEventList().getFirst());
    }

    @Test
    public void execute_repeatedId_countedOnce() throws Exception {
        assertEquals("Removed 1 member(s) from Final Concert.",
                unassign("Final Concert", 1, 1).execute(model).getFeedbackToUser());
        assertEquals("Removed 1 member(s) from Final Concert. 1 contact(s) were not assigned.",
                unassign("Final Concert", 2, 4, 4, 2).execute(model).getFeedbackToUser());
    }

    @Test
    public void execute_noneAssigned_noChanges() {
        assertEquals("None of the specified contacts are assigned to Final Concert. No changes were made.",
                assertThrows(CommandException.class, () -> unassign("final concert", 4, 5).execute(model))
                        .getMessage());
        assertEquals(List.of(concert, fair), model.getCoordiMate().getEventList());
    }

    @Test
    public void execute_missingEvent_noChanges() {
        assertEquals("Event Gala does not exist. Use a saved event name.",
                assertThrows(CommandException.class, () -> unassign("Gala", 1).execute(model)).getMessage());
        assertEquals(List.of(concert, fair), model.getCoordiMate().getEventList());
    }

    @Test
    public void execute_unknownMemberId_noChanges() {
        int unknownId = 99;
        assertEquals("No contact exists with member ID: 99. Use an existing member ID.",
                assertThrows(CommandException.class, () ->
                        unassign("Final Concert", 1, unknownId, 2).execute(model)).getMessage());
        assertEquals(List.of(concert, fair), model.getCoordiMate().getEventList());
    }

    @Test
    public void execute_filteredList_usesPermanentIds() throws Exception {
        model.updateFilteredPersonList(person -> person.equals(BENSON) || person.equals(DANIEL));
        assertEquals("Removed 1 member(s) from Final Concert. 1 contact(s) were not assigned.",
                unassign("Final Concert", 2, 4).execute(model).getFeedbackToUser());
        assertEquals(event(ALICE.getName(), CARL.getName()), model.getCoordiMate().getEventList().getFirst());
        unassign("Final Concert", 1).execute(model);
        assertEquals(event(CARL.getName()), model.getCoordiMate().getEventList().getFirst());
        assertEquals(List.of(BENSON, DANIEL), model.getFilteredPersonList());
    }

    @Test
    public void execute_memberWithAttendance_removesOnlyTheirAttendance() throws Exception {
        Event marked = new Event("Final Concert", start, end, concert.getMembers(), Map.of(
                ALICE.getName(), AttendanceStatus.PRESENT, BENSON.getName(), AttendanceStatus.ABSENT));
        model.setEvent(concert, marked);
        unassign("Final Concert", 1).execute(model);
        assertEquals(new Event("Final Concert", start, end, List.of(BENSON.getName(), CARL.getName()),
                Map.of(BENSON.getName(), AttendanceStatus.ABSENT)), model.getCoordiMate().getEventList().getFirst());
    }

    @Test
    public void equalityAndNullArguments() {
        UnassignCommand command = unassign("Final Concert", 1, 2);
        assertEquals(command, unassign(" Final Concert ", 1, 2));
        assertEquals(command.hashCode(), unassign("Final Concert", 1, 2).hashCode());
        assertNotEquals(command, unassign("Final Concert", 2, 1));
        assertNotEquals(command, unassign("Fair", 1, 2));
        assertNotEquals(command, new AssignCommand("Final Concert", List.of(new MemberId(1),
                new MemberId(2))));
        assertNotEquals(command, null);
        assertThrows(NullPointerException.class, () -> command.execute(null));
        assertThrows(NullPointerException.class, () -> new UnassignCommand(null, List.of(new MemberId(1))));
        assertThrows(NullPointerException.class, () -> new UnassignCommand("Final Concert", null));
    }

    private static UnassignCommand unassign(String eventName, Integer... memberIds) {
        return new UnassignCommand(eventName, Arrays.stream(memberIds).map(MemberId::new).toList());
    }

    private Event event(Name... members) {
        return new Event("Final Concert", start, end, List.of(members));
    }
}
