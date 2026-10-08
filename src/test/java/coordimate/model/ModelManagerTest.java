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
import coordimate.model.person.NameContainsKeywordsPredicate;
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
        String[] keywords = ALICE.getName().getFullName().split("\\s+");
        modelManager.updateFilteredPersonList(new NameContainsKeywordsPredicate(List.of(keywords)));
        assertFalse(modelManager.equals(new ModelManager(coordiMate, userPrefs)));

        // resets modelManager to initial state for upcoming tests
        modelManager.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);

        // different userPrefs -> returns false
        UserPrefs differentUserPrefs = new UserPrefs();
        differentUserPrefs.setGuiSettings(new GuiSettings(1, 2, 3, 4));
        assertFalse(modelManager.equals(new ModelManager(coordiMate, differentUserPrefs)));
    }
}
