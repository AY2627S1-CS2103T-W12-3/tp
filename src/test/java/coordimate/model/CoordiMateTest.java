package coordimate.model;

import static coordimate.logic.commands.CommandTestUtil.VALID_ADDRESS_BOB;
import static coordimate.logic.commands.CommandTestUtil.VALID_TAG_HUSBAND;
import static coordimate.testutil.Assert.assertThrows;
import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.getTypicalCoordiMate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import coordimate.model.person.Person;
import coordimate.model.person.exceptions.DuplicatePersonException;
import coordimate.testutil.PersonBuilder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class CoordiMateTest {

    private final CoordiMate coordiMate = new CoordiMate();

    @Test
    public void constructor() {
        assertEquals(List.of(), coordiMate.getPersonList());
    }

    @Test
    public void resetData_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> coordiMate.resetData(null));
    }

    @Test
    public void resetData_withValidReadOnlyCoordiMate_replacesData() {
        CoordiMate newData = getTypicalCoordiMate();
        coordiMate.resetData(newData);
        assertEquals(newData, coordiMate);
    }

    @Test
    public void resetData_withDuplicatePersons_throwsDuplicatePersonException() {
        // Two persons with the same identity fields
        Person editedAlice = new PersonBuilder(ALICE).withAddress(VALID_ADDRESS_BOB).withTags(VALID_TAG_HUSBAND)
                .build();
        List<Person> newPersons = List.of(ALICE, editedAlice);
        CoordiMateStub newData = new CoordiMateStub(newPersons);

        assertThrows(DuplicatePersonException.class, () -> coordiMate.resetData(newData));
    }

    @Test
    public void hasPerson_nullPerson_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> coordiMate.hasPerson(null));
    }

    @Test
    public void hasPerson_personNotInCoordiMate_returnsFalse() {
        assertFalse(coordiMate.hasPerson(ALICE));
    }

    @Test
    public void hasPerson_personInCoordiMate_returnsTrue() {
        coordiMate.addPerson(ALICE);
        assertTrue(coordiMate.hasPerson(ALICE));
    }

    @Test
    public void hasPerson_personWithSameIdentityFieldsInCoordiMate_returnsTrue() {
        coordiMate.addPerson(ALICE);
        Person editedAlice = new PersonBuilder(ALICE).withAddress(VALID_ADDRESS_BOB).withTags(VALID_TAG_HUSBAND)
                .build();
        assertTrue(coordiMate.hasPerson(editedAlice));
    }

    @Test
    public void getPersonList_modifyList_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> coordiMate.getPersonList().remove(0));
    }

    @Test
    public void toStringMethod() {
        String expected = CoordiMate.class.getCanonicalName() + "{persons=" + coordiMate.getPersonList()
                + ", events=" + coordiMate.getEventList() + "}";
        assertEquals(expected, coordiMate.toString());
    }

    @Test
    public void equals_sameObject_returnsTrue() {
        assertEquals(coordiMate, coordiMate);
    }

    @Test
    public void equals_differentType_returnsFalse() {
        assertNotEquals(coordiMate, new Object());
    }

    @Test
    public void hashCode_sameData_returnsSameHashCode() {
        CoordiMate copy = new CoordiMate(coordiMate);
        assertEquals(coordiMate.hashCode(), copy.hashCode());
    }

    /**
     * A stub ReadOnlyCoordiMate whose persons list can violate interface constraints.
     */
    private static class CoordiMateStub implements ReadOnlyCoordiMate {
        private final ObservableList<Person> persons = FXCollections.observableArrayList();

        CoordiMateStub(Collection<Person> persons) {
            this.persons.setAll(persons);
        }

        @Override
        public ObservableList<Person> getPersonList() {
            return persons;
        }

        @Override
        public ObservableList<coordimate.model.event.Event> getEventList() {
            return FXCollections.emptyObservableList();
        }
    }

}
