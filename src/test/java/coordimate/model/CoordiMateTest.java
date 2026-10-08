package coordimate.model;

import static coordimate.logic.commands.CommandTestUtil.VALID_ADDRESS_BOB;
import static coordimate.logic.commands.CommandTestUtil.VALID_TAG_HUSBAND;
import static coordimate.testutil.Assert.assertThrows;
import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BENSON;
import static coordimate.testutil.TypicalPersons.CARL;
import static coordimate.testutil.TypicalPersons.getTypicalCoordiMate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.event.MemberNameConflictException;
import coordimate.model.person.Name;
import coordimate.model.person.Person;
import coordimate.model.person.exceptions.DuplicatePersonException;
import coordimate.model.tag.Tag;
import coordimate.model.tag.exceptions.DuplicateTagException;
import coordimate.testutil.PersonBuilder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class CoordiMateTest {

    private final CoordiMate coordiMate = new CoordiMate();

    @Test
    public void constructor() {
        assertEquals(List.of(), coordiMate.getPersonList());
        assertEquals(List.of(new Tag("EXCO"), new Tag("Sponsor"), new Tag("UniversityStaff"),
                new Tag("Logistics")), coordiMate.getTagList());
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
    public void addPerson_sameNameWithDifferentPhoneAndEmail_throwsDuplicatePersonException() {
        coordiMate.addPerson(ALICE);
        Person differentAlice = new PersonBuilder(BENSON).withName(ALICE.getName().toString()).build();
        assertThrows(DuplicatePersonException.class, () -> coordiMate.addPerson(differentAlice));

        assertEquals(List.of(ALICE), coordiMate.getPersonList());
    }

    @Test
    public void removePerson_assignedContact_removedFromEveryEvent() {
        addContactsAndEvents();
        coordiMate.removePerson(ALICE);
        assertEquals(List.of(event("Concert", BENSON.getName(), CARL.getName()),
                event("Fair", CARL.getName()), event("Meeting")), coordiMate.getEventList());
    }

    @Test
    public void setPerson_renamedContact_renamedInEveryEventInPlace() {
        addContactsAndEvents();
        Person renamedAlice = new PersonBuilder(ALICE).withName("Alice Tan").build();
        coordiMate.setPerson(ALICE, renamedAlice);
        assertEquals(List.of(event("Concert", BENSON.getName(), renamedAlice.getName(), CARL.getName()),
                event("Fair", CARL.getName(), renamedAlice.getName()), event("Meeting")), coordiMate.getEventList());
    }

    @Test
    public void setPerson_nameUnchanged_eventsUnchanged() {
        addContactsAndEvents();
        List<Event> before = List.copyOf(coordiMate.getEventList());
        coordiMate.setPerson(ALICE, new PersonBuilder(ALICE).withAddress(VALID_ADDRESS_BOB).build());
        assertEquals(before, coordiMate.getEventList());
    }

    @Test
    public void setPerson_duplicatePhone_eventsUnchanged() {
        addContactsAndEvents();
        List<Event> before = List.copyOf(coordiMate.getEventList());
        Person aliceWithBensonPhone = new PersonBuilder(ALICE).withPhone(BENSON.getPhone().toString()).build();
        assertThrows(DuplicatePersonException.class, () -> coordiMate.setPerson(ALICE, aliceWithBensonPhone));
        assertEquals(before, coordiMate.getEventList());
        assertEquals(List.of(ALICE, BENSON, CARL), coordiMate.getPersonList());
    }

    @Test
    public void setPerson_renameConflictsWithEventMember_noChanges() {
        addContactsAndEvents();
        List<Event> before = List.copyOf(coordiMate.getEventList());
        Person aliceAsBenson = new PersonBuilder(ALICE).withName(BENSON.getName().toString()).build();

        assertThrows(MemberNameConflictException.class, () -> coordiMate.setPerson(ALICE, aliceAsBenson));
        assertEquals(before, coordiMate.getEventList());
        assertEquals(List.of(ALICE, BENSON, CARL), coordiMate.getPersonList());
    }

    private void addContactsAndEvents() {
        coordiMate.addPerson(ALICE);
        coordiMate.addPerson(BENSON);
        coordiMate.addPerson(CARL);
        coordiMate.addEvent(event("Concert", BENSON.getName(), ALICE.getName(), CARL.getName()));
        coordiMate.addEvent(event("Fair", CARL.getName(), ALICE.getName()));
        coordiMate.addEvent(event("Meeting"));
    }

    private static Event event(String name, Name... members) {
        return new Event(name, new EventTime("08-08-2026"), new EventTime("08-08-2026"), List.of(members));
    }

    @Test
    public void getPersonList_modifyList_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> coordiMate.getPersonList().remove(0));
    }

    @Test
    public void setTags_customTags_replacesDefaultTags() {
        Tag customTag = new Tag("Publicity");

        coordiMate.setTags(List.of(customTag));

        assertEquals(List.of(customTag), coordiMate.getTagList());
    }

    @Test
    public void setTags_defaultWithDifferentCase_preservesSuppliedCapitalization() {
        coordiMate.setTags(List.of(new Tag("exco")));

        assertEquals(List.of(new Tag("exco")), coordiMate.getTagList());
    }

    @Test
    public void setTags_duplicateNamesIgnoringCase_throwsDuplicateTagException() {
        List<Tag> duplicateTags = List.of(new Tag("Publicity"), new Tag("publicity"));

        assertThrows(DuplicateTagException.class, () -> coordiMate.setTags(duplicateTags));
    }

    @Test
    public void hasTag_tagWithSameNameIgnoringCase_returnsTrue() {
        assertTrue(coordiMate.hasTag(new Tag("exco")));
    }

    @Test
    public void hasTag_nullTag_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> coordiMate.hasTag(null));
    }

    @Test
    public void addTag_validTag_addsTag() {
        Tag customTag = new Tag("Publicity");

        coordiMate.addTag(customTag);

        assertTrue(coordiMate.hasTag(customTag));
    }

    @Test
    public void addTag_duplicateNameIgnoringCase_throwsDuplicateTagException() {
        assertThrows(DuplicateTagException.class, () -> coordiMate.addTag(new Tag("exco")));
    }

    @Test
    public void setTag_customTag_replacesTagInListAndContacts() {
        Tag publicity = new Tag("Publicity");
        Tag media = new Tag("Media");
        Person taggedAlice = new PersonBuilder(ALICE).withTags("Publicity", "EXCO").build();
        Person taggedBenson = new PersonBuilder(BENSON).withTags("Publicity").build();
        coordiMate.addTag(publicity);
        coordiMate.addPerson(taggedAlice);
        coordiMate.addPerson(taggedBenson);

        coordiMate.setTag(new Tag("publicity"), media);

        assertEquals(List.of(new Tag("EXCO"), new Tag("Sponsor"), new Tag("UniversityStaff"),
                new Tag("Logistics"), media), coordiMate.getTagList());
        assertEquals(List.of("EXCO", "Media"), coordiMate.getPersonList().get(0).getTags().stream()
                .map(Tag::getTagName).sorted().toList());
        assertEquals(List.of(media), coordiMate.getPersonList().get(1).getTags().stream().toList());
    }

    @Test
    public void setTag_defaultTag_replacesTagInListAndContacts() {
        Tag committee = new Tag("Committee");
        Person taggedAlice = new PersonBuilder(ALICE).withTags("EXCO").build();
        coordiMate.addPerson(taggedAlice);

        coordiMate.setTag(new Tag("exco"), committee);

        assertEquals(List.of(committee, new Tag("Sponsor"), new Tag("UniversityStaff"),
                new Tag("Logistics")), coordiMate.getTagList());
        assertEquals(List.of(committee), coordiMate.getPersonList().get(0).getTags().stream().toList());
        assertEquals(coordiMate, new CoordiMate(coordiMate));
    }

    @Test
    public void setTag_invalidArguments_throwsException() {
        Tag publicity = new Tag("Publicity");
        Tag media = new Tag("Media");
        coordiMate.addTag(publicity);
        coordiMate.addTag(media);

        assertThrows(NullPointerException.class, () -> coordiMate.setTag(null, media));
        assertThrows(NullPointerException.class, () -> coordiMate.setTag(publicity, null));
        assertThrows(IllegalArgumentException.class, () -> coordiMate.setTag(new Tag("Missing"), new Tag("New")));
        assertThrows(IllegalArgumentException.class, () -> coordiMate.setTag(publicity, media));
    }

    @Test
    public void getTagList_modifyList_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> coordiMate.getTagList().remove(0));
    }

    @Test
    public void toStringMethod() {
        String expected = CoordiMate.class.getCanonicalName() + "{persons=" + coordiMate.getPersonList()
                + ", tags=" + coordiMate.getTagList() + ", events=" + coordiMate.getEventList() + "}";
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
        public ObservableList<Tag> getTagList() {
            return FXCollections.emptyObservableList();
        }

        @Override
        public ObservableList<coordimate.model.event.Event> getEventList() {
            return FXCollections.emptyObservableList();
        }
    }

}
