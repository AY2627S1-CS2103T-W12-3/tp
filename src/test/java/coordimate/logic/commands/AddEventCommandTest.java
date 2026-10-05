package coordimate.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.CoordiMate;
import coordimate.model.ModelManager;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;

public class AddEventCommandTest {
    private final Event event = new Event("Final Concert",
            new EventTime("08-08-2026 15:00"), new EventTime("08-08-2026 18:00"));

    @Test
    public void execute_newEvent_success() throws Exception {
        ModelManager model = new ModelManager();
        assertEquals("Created Event Final Concert. Start Time: 08-08-2026 15:00. End Time: 08-08-2026 18:00.",
                new AddEventCommand(event).execute(model).getFeedbackToUser());
        assertEquals(List.of(event), model.getCoordiMate().getEventList());
    }

    @Test
    public void execute_duplicateName_rejected() throws Exception {
        ModelManager model = new ModelManager();
        new AddEventCommand(event).execute(model);
        Event duplicate = new Event(" Final Concert ", new EventTime("09-10-2026"), new EventTime("10-10-2026"));
        assertEquals(AddEventCommand.MESSAGE_DUPLICATE_EVENT,
                assertThrows(CommandException.class, () -> new AddEventCommand(duplicate).execute(model)).getMessage());
        assertEquals(List.of(event), model.getCoordiMate().getEventList());
        Event sameTimes = new Event("Fair", event.getStartTime(), event.getEndTime());
        new AddEventCommand(sameTimes).execute(model);
        assertEquals(List.of(event, sameTimes), model.getCoordiMate().getEventList());
    }

    @Test
    public void model_eventsCopiedAndReset_listIsUnmodifiable() {
        CoordiMate data = new CoordiMate();
        data.addEvent(event);
        CoordiMate copy = new CoordiMate(data);
        assertEquals(data, copy);
        assertEquals(data.hashCode(), copy.hashCode());
        assertTrue(copy.hasEvent(event));
        assertThrows(UnsupportedOperationException.class, () -> copy.getEventList().clear());
        assertThrows(IllegalArgumentException.class, () -> copy.addEvent(event));
        assertThrows(IllegalArgumentException.class, () -> copy.setEvents(List.of(event, event)));
        assertEquals(List.of(event), copy.getEventList());
        copy.resetData(new CoordiMate());
        assertNotEquals(data, copy);
        assertTrue(copy.getEventList().isEmpty());
    }

    @Test
    public void equality_andNullArguments() {
        AddEventCommand command = new AddEventCommand(event);
        assertEquals(command, new AddEventCommand(event));
        assertEquals(command.hashCode(), new AddEventCommand(event).hashCode());
        assertNotEquals(command, null);
        assertNotEquals(command, new Object());
        assertThrows(NullPointerException.class, () -> new AddEventCommand(null));
        assertThrows(NullPointerException.class, () -> command.execute(null));
    }
}
