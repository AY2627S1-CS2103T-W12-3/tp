package coordimate.logic.commands;

import static coordimate.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.ModelManager;
import coordimate.model.event.AttendanceStatus;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.person.Name;

public class MarkAttendanceCommandTest {

    private final Name alex = new Name("Alex Yeoh");
    private final Name bernice = new Name("Bernice Yu");
    private ModelManager model;

    @BeforeEach
    public void setUp() {
        model = new ModelManager();
        model.addEvent(new Event("Final Concert", new EventTime("08-08-2026"), new EventTime("08-08-2026"),
                List.of(alex, bernice)));
    }

    @Test
    public void execute_assignedMember_marksAttendance() throws Exception {
        MarkAttendanceCommand command = new MarkAttendanceCommand("Final Concert", alex, AttendanceStatus.PRESENT);

        CommandResult result = command.execute(model);

        assertEquals(String.format(MarkAttendanceCommand.MESSAGE_SUCCESS, alex, AttendanceStatus.PRESENT,
                "Final Concert"), result.getFeedbackToUser());
        assertEquals(AttendanceStatus.PRESENT,
                model.getCoordiMate().getEventList().getFirst().getAttendance(alex).get());
    }

    @Test
    public void execute_sameStatusAgain_noOpReported() throws Exception {
        new MarkAttendanceCommand("Final Concert", alex, AttendanceStatus.PRESENT).execute(model);
        MarkAttendanceCommand repeat = new MarkAttendanceCommand("Final Concert", alex, AttendanceStatus.PRESENT);

        CommandResult result = repeat.execute(model);

        assertEquals(String.format(MarkAttendanceCommand.MESSAGE_ALREADY_MARKED, alex, AttendanceStatus.PRESENT,
                "Final Concert"), result.getFeedbackToUser());
    }

    @Test
    public void execute_differentStatus_overwritesPrevious() throws Exception {
        new MarkAttendanceCommand("Final Concert", alex, AttendanceStatus.ABSENT).execute(model);
        new MarkAttendanceCommand("Final Concert", alex, AttendanceStatus.PRESENT).execute(model);

        assertEquals(AttendanceStatus.PRESENT,
                model.getCoordiMate().getEventList().getFirst().getAttendance(alex).get());
    }

    @Test
    public void execute_eventDoesNotExist_throwsCommandException() {
        MarkAttendanceCommand command = new MarkAttendanceCommand("Gala", alex, AttendanceStatus.PRESENT);
        assertThrows(CommandException.class,
                String.format(MarkAttendanceCommand.MESSAGE_EVENT_NOT_FOUND, "Gala"), () -> command.execute(model));
    }

    @Test
    public void execute_memberNotAssigned_throwsCommandException() {
        Name charlotte = new Name("Charlotte Oliveiro");
        MarkAttendanceCommand command = new MarkAttendanceCommand("Final Concert", charlotte,
                AttendanceStatus.PRESENT);
        assertThrows(CommandException.class,
                String.format(MarkAttendanceCommand.MESSAGE_MEMBER_NOT_ASSIGNED, charlotte,
                    "Final Concert"), () -> command.execute(model));
    }

    @Test
    public void execute_eventNameCaseInsensitive_success() throws Exception {
        MarkAttendanceCommand command = new MarkAttendanceCommand("final concert", alex, AttendanceStatus.PRESENT);
        command.execute(model);
        assertEquals(AttendanceStatus.PRESENT,
                model.getCoordiMate().getEventList().getFirst().getAttendance(alex).get());
    }

    @Test
    public void equalityAndNullArguments() {
        MarkAttendanceCommand command = new MarkAttendanceCommand("Final Concert", alex, AttendanceStatus.PRESENT);

        assertEquals(command, new MarkAttendanceCommand("Final Concert", alex, AttendanceStatus.PRESENT));
        assertEquals(command, new MarkAttendanceCommand("final concert", alex, AttendanceStatus.PRESENT));
        assertNotEquals(command, new MarkAttendanceCommand("Final Concert", bernice, AttendanceStatus.PRESENT));
        assertNotEquals(command, new MarkAttendanceCommand("Final Concert", alex, AttendanceStatus.ABSENT));
        assertNotEquals(command, null);
        assertNotEquals(command, new Object());

        assertThrows(NullPointerException.class, () ->
                new MarkAttendanceCommand(null, alex, AttendanceStatus.PRESENT));
        assertThrows(NullPointerException.class, () ->
                new MarkAttendanceCommand("Final Concert", null, AttendanceStatus.PRESENT));
        assertThrows(NullPointerException.class, () ->
                new MarkAttendanceCommand("Final Concert", alex, null));
        assertThrows(NullPointerException.class, () -> command.execute(null));
    }
}
