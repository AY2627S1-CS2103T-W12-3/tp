package coordimate.model;

import static coordimate.model.Model.PREDICATE_SHOW_ALL_PERSONS;
import static coordimate.testutil.Assert.assertThrows;
import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BENSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import coordimate.commons.core.GuiSettings;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.person.ContactMatchesKeywordPredicate;
import coordimate.model.tag.Tag;
import coordimate.testutil.CoordiMateBuilder;

public class ModelManagerTest {

    private ModelManager modelManager = new ModelManager();

    @Test
    public void constructor() {
        assertEquals(new UserPrefs(), modelManager.getUserPrefs());
        assertEquals(new GuiSettings(), modelManager.getGuiSettings());
        assertEquals(new CoordiMate(), new CoordiMate(modelManager.getCoordiMate()));
    }

    @Test
    public void constructor_validUserPrefs_copiesUserPrefs() {
        UserPrefs userPrefs = new UserPrefs();
        userPrefs.setGuiSettings(new GuiSettings(1, 2, 3, 4));
        modelManager = new ModelManager(new CoordiMate(), userPrefs);
        assertEquals(userPrefs, modelManager.getUserPrefs());

        // Modifying userPrefs should not modify modelManager's userPrefs
        UserPrefs oldUserPrefs = new UserPrefs(userPrefs);
        userPrefs.setGuiSettings(new GuiSettings(5, 6, 7, 8));
        assertEquals(oldUserPrefs, modelManager.getUserPrefs());
    }

    @Test
    public void setGuiSettings_nullGuiSettings_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> modelManager.setGuiSettings(null));
    }

    @Test
    public void setGuiSettings_validGuiSettings_setsGuiSettings() {
        GuiSettings guiSettings = new GuiSettings(1, 2, 3, 4);
        modelManager.setGuiSettings(guiSettings);
        assertEquals(guiSettings, modelManager.getGuiSettings());
    }

    @Test
    public void hasPerson_nullPerson_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> modelManager.hasPerson(null));
    }

    @Test
    public void hasPerson_personNotInCoordiMate_returnsFalse() {
        assertFalse(modelManager.hasPerson(ALICE));
    }

    @Test
    public void hasPerson_personInCoordiMate_returnsTrue() {
        modelManager.addPerson(ALICE);
        assertTrue(modelManager.hasPerson(ALICE));
    }

    @Test
    public void getFilteredPersonList_modifyList_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> modelManager.getFilteredPersonList().remove(0));
    }

    @Test
    public void getFilteredPersonPredicate_returnsLatestPredicate() {
        assertEquals(null, modelManager.getFilteredPersonPredicate());
        ContactMatchesKeywordPredicate predicate = new ContactMatchesKeywordPredicate("Alice");
        modelManager.updateFilteredPersonList(predicate);
        assertEquals(predicate, modelManager.getFilteredPersonPredicate());
        modelManager.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
        assertEquals(PREDICATE_SHOW_ALL_PERSONS, modelManager.getFilteredPersonPredicate());
    }

    @Test
    public void getTagList_returnsDefaultTags() {
        assertEquals(List.of(new Tag("EXCO"), new Tag("Sponsor"), new Tag("UniversityStaff"),
                new Tag("Logistics")), modelManager.getTagList());
    }

    @Test
    public void deleteTag_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> modelManager.deleteTag(null));
    }

    @Test
    public void tagOperations_nullArguments_throwNullPointerException() {
        assertThrows(NullPointerException.class, () -> modelManager.hasTag(null));
        assertThrows(NullPointerException.class, () -> modelManager.addTag(null));
        assertThrows(NullPointerException.class, () -> modelManager.setTag(null, new Tag("Media")));
        assertThrows(NullPointerException.class, () -> modelManager.setTag(new Tag("Publicity"), null));
    }

    @Test
    public void addTag_validTag_addsTag() {
        Tag publicity = new Tag("Publicity");

        modelManager.addTag(publicity);

        assertTrue(modelManager.hasTag(publicity));
    }

    @Test
    public void equals() {
        CoordiMate coordiMate = new CoordiMateBuilder().withPerson(ALICE).withPerson(BENSON).build();
        CoordiMate differentCoordiMate = new CoordiMate();
        UserPrefs userPrefs = new UserPrefs();

        // same values -> returns true
        modelManager = new ModelManager(coordiMate, userPrefs);
        ModelManager modelManagerCopy = new ModelManager(coordiMate, userPrefs);
        assertTrue(modelManager.equals(modelManagerCopy));

        // same object -> returns true
        assertTrue(modelManager.equals(modelManager));

        // null -> returns false
        assertFalse(modelManager.equals(null));

        // different types -> returns false
        assertFalse(modelManager.equals(5));

        // different coordiMate -> returns false
        assertFalse(modelManager.equals(new ModelManager(differentCoordiMate, userPrefs)));

        // different filteredList -> returns false
        modelManager.updateFilteredPersonList(new ContactMatchesKeywordPredicate(ALICE.getName().getFullName()));
        assertFalse(modelManager.equals(new ModelManager(coordiMate, userPrefs)));

        // resets modelManager to initial state for upcoming tests
        modelManager.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);

        // different userPrefs -> returns false
        UserPrefs differentUserPrefs = new UserPrefs();
        differentUserPrefs.setGuiSettings(new GuiSettings(1, 2, 3, 4));
        assertFalse(modelManager.equals(new ModelManager(coordiMate, differentUserPrefs)));
    }

    @Test
    public void getFilteredEventList_hidesArchivedEventsAndFollowsChanges() {
        Event concert = new Event("Concert", new EventTime("08-08-2026"), new EventTime("09-08-2026"));
        Event fair = new Event("Fair", new EventTime("09-10-2026"), new EventTime("10-10-2026"));
        modelManager.addEvent(concert);
        modelManager.addEvent(fair.withArchived(true));
        List<Event> shown = modelManager.getFilteredEventList();

        assertEquals(List.of(concert), shown);
        modelManager.setEvent(concert, concert.withArchived(true));
        assertEquals(List.of(), shown);
        modelManager.setEvent(fair.withArchived(true), fair);
        assertEquals(List.of(fair), shown);
        assertThrows(UnsupportedOperationException.class, () -> modelManager.getFilteredEventList().remove(0));
    }

    @Test
    public void setShowingArchived_switchesContactsAndKeepsLatestFilter() {
        modelManager.addPerson(ALICE);
        modelManager.addPerson(BENSON);
        modelManager.setPerson(BENSON, BENSON.withArchived(true));
        List<coordimate.model.person.Person> shown = modelManager.getFilteredPersonList();
        assertEquals(List.of(ALICE), shown);

        modelManager.setShowingArchived(true);
        assertTrue(modelManager.isShowingArchived());
        assertEquals(List.of(BENSON.withArchived(true)), shown);

        ContactMatchesKeywordPredicate predicate = new ContactMatchesKeywordPredicate("Alice");
        modelManager.updateFilteredPersonList(predicate);
        assertEquals(predicate, modelManager.getFilteredPersonPredicate());
        assertEquals(List.of(), shown);
        modelManager.setShowingArchived(false);
        assertFalse(modelManager.isShowingArchived());
        assertEquals(List.of(ALICE), shown);

        ModelManager other = new ModelManager(modelManager.getCoordiMate(), modelManager.getUserPrefs());
        other.updateFilteredPersonList(predicate);
        assertEquals(modelManager, other);
        other.setShowingArchived(true);
        assertFalse(modelManager.equals(other));
    }

    @Test
    public void setShowingArchived_switchesEventsAndNotifiesListeners() {
        Event concert = new Event("Concert", new EventTime("08-08-2026"), new EventTime("09-08-2026"));
        Event fair = new Event("Fair", new EventTime("09-10-2026"), new EventTime("10-10-2026")).withArchived(true);
        modelManager.addEvent(concert);
        modelManager.addEvent(fair);
        List<Boolean> changes = new java.util.ArrayList<>();
        modelManager.showingArchivedProperty().addListener((observable, oldValue, newValue) -> changes.add(newValue));

        modelManager.setShowingArchived(true);
        assertEquals(List.of(fair), modelManager.getFilteredEventList());
        modelManager.setShowingArchived(false);
        assertEquals(List.of(concert), modelManager.getFilteredEventList());
        assertEquals(List.of(true, false), changes);
    }
}
