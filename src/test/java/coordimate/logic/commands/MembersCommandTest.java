package coordimate.logic.commands;

import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BENSON;
import static coordimate.testutil.TypicalPersons.CARL;
import static coordimate.testutil.TypicalPersons.getTypicalCoordiMate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.ModelManager;
import coordimate.model.UserPrefs;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.person.Person;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class MembersCommandTest {
    private final EventTime date = new EventTime("08-08-2026");
    private ModelManager model;

    @BeforeEach
    public void setUp() {
        model = new ModelManager(getTypicalCoordiMate(), new UserPrefs());
        model.addEvent(new Event("Final Concert", date, date, List.of(CARL.getName(), ALICE.getName())));
        model.addEvent(new Event("Empty Event", date, date));
    }

    @Test
    public void execute_eventWithMembers_listsMembersInContactListOrder() throws Exception {
        assertEquals("Listed 2 member(s) of Final Concert. Use list to show all contacts.",
                new MembersCommand("Final Concert").execute(model).getFeedbackToUser());
        assertEquals(List.of(ALICE, CARL), model.getFilteredPersonList());
    }

    @Test
    public void execute_differentCaseEventName_usesSavedName() throws Exception {
        assertEquals("Listed 2 member(s) of Final Concert. Use list to show all contacts.",
                new MembersCommand("fINAL cONCERT").execute(model).getFeedbackToUser());
        assertEquals(List.of(ALICE, CARL), model.getFilteredPersonList());
    }

    @Test
    public void execute_eventWithoutMembers_emptyList() throws Exception {
        assertEquals("Empty Event has no members assigned. Use list to show all contacts.",
                new MembersCommand("empty event").execute(model).getFeedbackToUser());
        assertEquals(List.of(), model.getFilteredPersonList());
    }

    @Test
    public void execute_eventWithoutMembersInArchivedView_noMembersMessage() throws Exception {
        model.setShowingArchived(true);
        assertEquals("Empty Event has no members assigned. Use list to show all contacts.",
                new MembersCommand("Empty Event").execute(model).getFeedbackToUser());
        assertEquals(List.of(), model.getFilteredPersonList());
    }

    @Test
    public void execute_allMembersArchived_reportsArchivedMembers() throws Exception {
        archive(ALICE);
        archive(CARL);
        assertEquals("No active members of Final Concert are shown. 2 member(s) are archived. "
                + "Use list archived to show archived contacts.",
                new MembersCommand("Final Concert").execute(model).getFeedbackToUser());
        assertEquals(List.of(), model.getFilteredPersonList());
    }

    @Test
    public void execute_allMembersActiveInArchivedView_reportsActiveMembers() throws Exception {
        model.setShowingArchived(true);
        assertEquals("No archived members of Final Concert are shown. 2 member(s) are active. "
                + "Use list to show all contacts.",
                new MembersCommand("Final Concert").execute(model).getFeedbackToUser());
        assertEquals(List.of(), model.getFilteredPersonList());
    }

    @Test
    public void execute_someMembersArchived_listsActiveMembersAndReportsArchived() throws Exception {
        archive(ALICE);
        assertEquals("Listed 1 member(s) of Final Concert. 1 member(s) are archived. "
                + "Use list archived to show archived contacts.",
                new MembersCommand("Final Concert").execute(model).getFeedbackToUser());
        assertEquals(List.of(CARL), model.getFilteredPersonList());
    }

    @Test
    public void execute_someMembersArchivedInArchivedView_listsArchivedMembersAndReportsActive() throws Exception {
        archive(ALICE);
        model.setShowingArchived(true);
        assertEquals("Listed 1 member(s) of Final Concert. 1 member(s) are active. Use list to show all contacts.",
                new MembersCommand("Final Concert").execute(model).getFeedbackToUser());
        assertEquals(List.of(ALICE.withArchived(true)), model.getFilteredPersonList());
    }

    @Test
    public void execute_success_showsContactsView() throws Exception {
        assertEquals(new CommandResult("Listed 2 member(s) of Final Concert. Use list to show all contacts.",
                CommandResult.View.CONTACTS), new MembersCommand("Final Concert").execute(model));
        assertEquals(new CommandResult("Empty Event has no members assigned. Use list to show all contacts.",
                CommandResult.View.CONTACTS), new MembersCommand("Empty Event").execute(model));
    }

    @Test
    public void execute_missingEvent_listUnchanged() {
        model.updateFilteredPersonList(person -> person.equals(BENSON));
        assertEquals("Event Gala does not exist. Use a saved event name.",
                assertThrows(CommandException.class, () -> new MembersCommand("Gala").execute(model)).getMessage());
        assertEquals(List.of(BENSON), model.getFilteredPersonList());
    }

    @Test
    public void execute_afterAnotherFilter_showsAllMembers() throws Exception {
        model.updateFilteredPersonList(person -> person.equals(BENSON));
        new MembersCommand("Final Concert").execute(model);
        assertEquals(List.of(ALICE, CARL), model.getFilteredPersonList());
    }

    @Test
    public void equalityAndNullArguments() {
        MembersCommand command = new MembersCommand("Final Concert");
        assertEquals(command, new MembersCommand(" Final Concert "));
        assertEquals(command.hashCode(), new MembersCommand("Final Concert").hashCode());
        assertNotEquals(command, new MembersCommand("Fair"));
        assertNotEquals(command, null);
        assertThrows(NullPointerException.class, () -> command.execute(null));
        assertThrows(NullPointerException.class, () -> new MembersCommand(null));
    }

    @Test
    public void execute_moreShownThanSavedMembers_triggersAssertion() {
        ModelShowingExtraContact brokenModel = new ModelShowingExtraContact();
        brokenModel.addEvent(new Event("Final Concert", date, date, List.of(CARL.getName(), ALICE.getName())));
        assertThrows(AssertionError.class, () -> new MembersCommand("Final Concert").execute(brokenModel));
    }

    private void archive(Person person) {
        model.setPerson(person, person.withArchived(true));
    }

    private static final class ModelShowingExtraContact extends ModelManager {
        ModelShowingExtraContact() {
            super(getTypicalCoordiMate(), new UserPrefs());
        }
    }

    @Override
    public ObservableList<Person> getFilteredPersonList() {
        // Simulates a broken model whose displayed list contains contacts that are not saved members.
        return FXCollections.observableArrayList(ALICE, BENSON, CARL);
    }
}
