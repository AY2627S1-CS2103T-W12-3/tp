package coordimate.logic.commands;

import static coordimate.testutil.Assert.assertThrows;
import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BENSON;
import static coordimate.testutil.TypicalPersons.getTypicalCoordiMate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.CoordiMate;
import coordimate.model.ModelManager;
import coordimate.model.UserPrefs;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.person.FilterCriterion;
import coordimate.model.person.FilterField;
import coordimate.model.tag.Tag;
import coordimate.testutil.PersonBuilder;

public class FilterCommandTest {

    private ModelManager model;

    @BeforeEach
    public void setUp() {
        model = new ModelManager(getTypicalCoordiMate(), new UserPrefs());
    }

    @Test
    public void execute_tagFilterMatchesContacts_success() throws Exception {
        CoordiMate coordiMate = new CoordiMate();
        coordiMate.addTag(new Tag("Friends"));
        coordiMate.addPerson(new PersonBuilder(ALICE).withTags("Friends").build());
        ModelManager modelWithTaggedPerson = new ModelManager(coordiMate, new UserPrefs());

        FilterCommand command = filter(FilterField.TAG, "Friends");
        CommandResult result = command.execute(modelWithTaggedPerson);

        assertEquals(String.format(FilterCommand.MESSAGE_SUCCESS, 1), result.getFeedbackToUser());
        assertEquals(1, modelWithTaggedPerson.getFilteredPersonList().size());
    }

    @Test
    public void execute_tagFilterNoMatches_showsNoMatchesMessage() throws Exception {
        CoordiMate coordiMate = getTypicalCoordiMate();
        coordiMate.addTag(new Tag("Publicity"));
        ModelManager modelWithExtraTag = new ModelManager(coordiMate, new UserPrefs());
        FilterCommand command = filter(FilterField.TAG, "Publicity");

        CommandResult result = command.execute(modelWithExtraTag);

        assertEquals(FilterCommand.MESSAGE_NO_MATCHES, result.getFeedbackToUser());
        assertEquals(List.of(), modelWithExtraTag.getFilteredPersonList());
    }

    @Test
    public void execute_tagDoesNotExist_throwsCommandException() {
        FilterCommand command = filter(FilterField.TAG, "zzzNoSuchTag");

        assertThrows(CommandException.class, FilterCommand.MESSAGE_NO_MATCHING_VALUE, () -> command.execute(model));
    }

    @Test
    public void execute_eventFilterMatchesAssignedMember_success() throws Exception {
        Event concert = new Event("Final Concert", new EventTime("08-08-2026"), new EventTime("08-08-2026"),
                List.of(ALICE.getName()));
        model.addEvent(concert);
        FilterCommand command = filter(FilterField.EVENT, "Final Concert");

        command.execute(model);

        assertEquals(List.of(ALICE), model.getFilteredPersonList());
    }

    @Test
    public void execute_eventFilterCaseInsensitive_success() throws Exception {
        Event concert = new Event("Final Concert", new EventTime("08-08-2026"), new EventTime("08-08-2026"),
                List.of(ALICE.getName()));
        model.addEvent(concert);
        FilterCommand command = filter(FilterField.EVENT, "final concert");

        command.execute(model);

        assertEquals(List.of(ALICE), model.getFilteredPersonList());
    }

    @Test
    public void execute_eventDoesNotExist_throwsCommandException() {
        FilterCommand command = filter(FilterField.EVENT, "Gala");

        assertThrows(CommandException.class, FilterCommand.MESSAGE_NO_MATCHING_VALUE, () -> command.execute(model));
    }

    @Test
    public void execute_organisationFilterCaseInsensitive_success() throws Exception {
        CoordiMate coordiMate = new CoordiMate();
        var aliceAtNus = new PersonBuilder(ALICE).withOrganisation("NUS Student Affairs").build();
        coordiMate.addPerson(aliceAtNus);
        coordiMate.addPerson(new PersonBuilder(BENSON).withOrganisation("NUS Students").build());
        ModelManager modelWithOrganisations = new ModelManager(coordiMate, new UserPrefs());
        FilterCommand command = filter(FilterField.ORGANISATION, "nus student affairs");

        CommandResult result = command.execute(modelWithOrganisations);

        assertEquals(String.format(FilterCommand.MESSAGE_SUCCESS, 1), result.getFeedbackToUser());
        assertEquals(List.of(aliceAtNus), modelWithOrganisations.getFilteredPersonList());
    }

    @Test
    public void execute_organisationDoesNotExist_throwsCommandException() {
        FilterCommand command = filter(FilterField.ORGANISATION, "zzzNoSuchOrganisation");

        assertThrows(CommandException.class, FilterCommand.MESSAGE_NO_MATCHING_VALUE, () -> command.execute(model));
    }

    @Test
    public void execute_multipleCriteria_requiresAllToMatch() throws Exception {
        CoordiMate coordiMate = new CoordiMate();
        coordiMate.addTag(new Tag("Friends"));
        var taggedAlice = new PersonBuilder(ALICE).withTags("Friends").build();
        var taggedBenson = new PersonBuilder(BENSON).withTags("Friends").build();
        coordiMate.addPerson(taggedAlice);
        coordiMate.addPerson(taggedBenson);

        Event concert = new Event("Final Concert", new EventTime("08-08-2026"), new EventTime("08-08-2026"),
                List.of(taggedAlice.getName(), taggedBenson.getName()));
        coordiMate.addEvent(concert);
        ModelManager modelWithData = new ModelManager(coordiMate, new UserPrefs());

        FilterCommand command = new FilterCommand(List.of(
                new FilterCriterion(FilterField.EVENT, "Final Concert"),
                new FilterCriterion(FilterField.TAG, "Friends")));

        command.execute(modelWithData);

        assertEquals(2, modelWithData.getFilteredPersonList().size());
        assertEquals(true, modelWithData.getFilteredPersonList().contains(taggedAlice));
    }

    @Test
    public void equals() {
        FilterCommand command = filter(FilterField.TAG, "friend");

        // same object -> returns true
        assertEquals(command, command);

        // same values -> returns true
        assertEquals(command, filter(FilterField.TAG, "friend"));

        // different criteria -> returns false
        assertNotEquals(command, filter(FilterField.TAG, "colleague"));

        // different type -> returns false
        assertNotEquals(command, 1);

        // null -> returns false
        assertNotEquals(command, null);
    }

    private static FilterCommand filter(FilterField field, String value) {
        return new FilterCommand(List.of(new FilterCriterion(field, value)));
    }
}
