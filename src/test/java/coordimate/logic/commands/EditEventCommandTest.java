package coordimate.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.ModelManager;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;

public class EditEventCommandTest {
    private final Event concert = new Event("Final Concert",
            new EventTime("08-08-2026 15:00"), new EventTime("08-08-2026 18:00"));

    @Test
    public void execute_allFieldCombinations_retainsUnspecifiedFieldsAndPosition() throws Exception {
        for (int fields = 1; fields < 8; fields++) {
            ModelManager model = new ModelManager();
            model.addEvent(concert);
            Event fair = new Event("Fair", new EventTime("09-10-2026"), new EventTime("10-10-2026"));
            model.addEvent(fair);
            String name = (fields & 1) == 0 ? null : "New Concert";
            EventTime start = (fields & 2) == 0 ? null : new EventTime("08-08-2026 16:00");
            EventTime end = (fields & 4) == 0 ? null : new EventTime("08-08-2026 19:00");
            Event expected = new Event(name == null ? concert.getName() : name,
                    start == null ? concert.getStartTime() : start, end == null ? concert.getEndTime() : end);
            assertEquals(String.format(EditEventCommand.MESSAGE_SUCCESS,
                    expected.getName(), expected.getStartTime(), expected.getEndTime()),
                    new EditEventCommand("Final Concert", name, start, end).execute(model).getFeedbackToUser());
            assertEquals(List.of(expected, fair), model.getCoordiMate().getEventList());
        }
    }

    @Test
    public void execute_missingName_rejected() {
        ModelManager model = new ModelManager();
        model.addEvent(concert);
        for (String name : new String[] {"Missing", "Other Concert"}) {
            assertEquals(String.format(EditEventCommand.MESSAGE_EVENT_NOT_FOUND, name),
                    assertThrows(CommandException.class, () ->
                            new EditEventCommand(name, "New", null, null).execute(model)).getMessage());
            assertEquals(List.of(concert), model.getCoordiMate().getEventList());
        }
    }

    @Test
    public void execute_differentCaseLookup_preservesDisplayName() throws Exception {
        ModelManager model = new ModelManager();
        model.addEvent(concert);
        EventTime end = new EventTime("08-08-2026 19:00");
        assertEquals("Edited Event Final Concert. Start Time: 08-08-2026 15:00. End Time: 08-08-2026 19:00.",
                new EditEventCommand("fINAL cONCERT", null, null, end).execute(model).getFeedbackToUser());
        assertEquals(List.of(new Event("Final Concert", concert.getStartTime(), end)),
                model.getCoordiMate().getEventList());
    }

    @Test
    public void execute_duplicateName_noChanges() {
        ModelManager model = new ModelManager();
        model.addEvent(concert);
        Event fair = new Event("Fair", concert.getStartTime(), concert.getEndTime());
        model.addEvent(fair);
        for (String name : new String[] {"Fair", " fair "}) {
            assertEquals(EditEventCommand.MESSAGE_DUPLICATE_EVENT,
                    assertThrows(CommandException.class, () -> new EditEventCommand("Final Concert", name,
                            new EventTime("08-08-2026 16:00"), null).execute(model)).getMessage());
            assertEquals(List.of(concert, fair), model.getCoordiMate().getEventList());
            Event duplicate = new Event(name, concert.getStartTime(), concert.getEndTime());
            assertThrows(IllegalArgumentException.class, () -> model.setEvent(concert, duplicate));
            assertEquals(List.of(concert, fair), model.getCoordiMate().getEventList());
        }
    }

    @Test
    public void execute_sameNameOrCaseChange_success() throws Exception {
        ModelManager model = new ModelManager();
        model.addEvent(concert);
        new EditEventCommand("Final Concert", "Final Concert", null, null).execute(model);
        new EditEventCommand("FINAL CONCERT", "FINAL CONCERT", null, null).execute(model);
        assertEquals("FINAL CONCERT", model.getCoordiMate().getEventList().getFirst().getName());
    }

    @Test
    public void execute_invalidOrder_noChanges() {
        ModelManager model = new ModelManager();
        model.addEvent(concert);
        EditEventCommand[] commands = {
            new EditEventCommand("Final Concert", "New", new EventTime("09-08-2026"), null),
            new EditEventCommand("Final Concert", null, null, new EventTime("08-08-2026 14:00"))
        };
        for (EditEventCommand command : commands) {
            assertEquals(Event.MESSAGE_INVALID_TIME_ORDER,
                    assertThrows(CommandException.class, () -> command.execute(model)).getMessage());
            assertEquals(List.of(concert), model.getCoordiMate().getEventList());
        }
    }

    @Test
    public void equalityAndNullArguments() {
        EditEventCommand command = new EditEventCommand("Final Concert", "New", null, null);
        assertEquals(command, new EditEventCommand("Final Concert", "New", null, null));
        assertEquals(command.hashCode(), new EditEventCommand("Final Concert", "New", null, null).hashCode());
        assertNotEquals(command, new EditEventCommand("Final Concert", "Other", null, null));
        assertNotEquals(command, null);
        assertThrows(NullPointerException.class, () -> command.execute(null));
        assertThrows(NullPointerException.class, () -> new EditEventCommand(null, "New", null, null));
    }
}
