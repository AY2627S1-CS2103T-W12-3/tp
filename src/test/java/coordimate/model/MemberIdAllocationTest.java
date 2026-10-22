package coordimate.model;

import static coordimate.testutil.Assert.assertThrows;
import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BENSON;
import static coordimate.testutil.TypicalPersons.CARL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import coordimate.model.person.MemberId;
import coordimate.model.person.Person;
import coordimate.model.person.exceptions.DuplicateMemberIdException;
import coordimate.model.person.exceptions.DuplicatePersonException;
import coordimate.testutil.PersonBuilder;

public class MemberIdAllocationTest {
    @Test
    public void addPerson_allocatesSequentialIdsWithoutChangingDrafts() {
        CoordiMate data = new CoordiMate();
        Person alice = draft(ALICE);
        data.addPerson(alice);
        data.addPerson(draft(BENSON));

        assertEquals(List.of(ALICE, BENSON), data.getPersonList());
        assertEquals(3, data.getNextMemberId());
        assertTrue(alice.getMemberId().isEmpty());
    }

    @Test
    public void addPerson_existingId_isPreservedAndReserved() {
        CoordiMate data = new CoordiMate();
        Person alice = new PersonBuilder(ALICE).withMemberId(12).build();
        data.addPerson(alice);
        data.addPerson(draft(BENSON));

        assertEquals(List.of(alice, new PersonBuilder(BENSON).withMemberId(13).build()), data.getPersonList());
        assertEquals(14, data.getNextMemberId());
    }

    @Test
    public void addPerson_duplicateContactOrId_doesNotConsumeId() {
        CoordiMate data = new CoordiMate();
        data.addPerson(draft(ALICE));
        assertThrows(DuplicatePersonException.class, () ->
                data.addPerson(new PersonBuilder(ALICE).withMemberId(99).build()));
        assertThrows(DuplicateMemberIdException.class, () ->
                data.addPerson(new PersonBuilder(BENSON).withMemberId(1).build()));
        assertEquals(List.of(ALICE), data.getPersonList());
        assertEquals(2, data.getNextMemberId());

        data.addPerson(draft(BENSON));
        assertEquals(List.of(ALICE, BENSON), data.getPersonList());
    }

    @Test
    public void removePerson_doesNotReuseIdsEvenWhenAllMembersAreRemoved() {
        CoordiMate data = new CoordiMate();
        data.addPerson(draft(ALICE));
        data.addPerson(draft(BENSON));
        data.removePerson(BENSON);
        data.removePerson(ALICE);
        data.addPerson(draft(CARL));

        assertEquals(List.of(CARL), data.getPersonList());
        assertEquals(4, data.getNextMemberId());
    }

    @Test
    public void setPerson_unidentifiedReplacement_inheritsTargetId() {
        CoordiMate data = new CoordiMate();
        data.addPerson(ALICE);
        data.setPerson(ALICE, draft(BENSON));
        assertEquals(List.of(new PersonBuilder(BENSON).withMemberId(1).build()), data.getPersonList());
        assertEquals(2, data.getNextMemberId());
    }

    @Test
    public void setPerson_differentId_rejectedWithoutChanges() {
        CoordiMate data = new CoordiMate();
        data.addPerson(ALICE);
        assertThrows(IllegalArgumentException.class, () ->
                data.setPerson(ALICE, new PersonBuilder(ALICE).withMemberId(12).withTags("NewTag").build()));
        assertEquals(List.of(ALICE), data.getPersonList());
        assertEquals(2, data.getNextMemberId());
        assertTrue(data.getTagList().stream().noneMatch(tag -> tag.getTagName().equals("NewTag")));
    }

    @Test
    public void setPersons_mixedIdentifiedAndNewContacts_reservesExistingIdsFirst() {
        CoordiMate data = new CoordiMate();
        Person benson = new PersonBuilder(BENSON).withMemberId(1).build();
        data.setPersons(List.of(draft(ALICE), benson));

        assertEquals(List.of(new PersonBuilder(ALICE).withMemberId(2).build(), benson), data.getPersonList());
        assertEquals(3, data.getNextMemberId());
    }

    @Test
    public void setPersons_reorderAndRemove_preservesIdsAndAllocationState() {
        CoordiMate data = new CoordiMate();
        data.setPersons(List.of(draft(ALICE), draft(BENSON), draft(CARL)));
        data.setPersons(List.of(CARL, ALICE));
        assertEquals(List.of(CARL, ALICE), data.getPersonList());
        assertEquals(4, data.getNextMemberId());

        data.setPersons(List.of());
        data.addPerson(draft(BENSON));
        assertEquals(new MemberId(4), data.getPersonList().getFirst().getMemberId().orElseThrow());
    }

    @Test
    public void setPersons_duplicateIds_rejectsReplacementWithoutChanges() {
        CoordiMate data = new CoordiMate();
        data.addPerson(CARL);
        CoordiMate before = new CoordiMate(data);
        assertThrows(DuplicateMemberIdException.class, () ->
                data.setPersons(List.of(ALICE, new PersonBuilder(BENSON).withMemberId(1).build())));
        assertEquals(before, data);
    }

    @Test
    public void copyAndReset_deletedHighestId_preservesAllocationState() {
        CoordiMate source = new CoordiMate();
        source.setPersons(List.of(ALICE, BENSON));
        source.removePerson(BENSON);
        CoordiMate copy = new CoordiMate(source);
        CoordiMate reset = new CoordiMate();
        reset.addPerson(new PersonBuilder(CARL).withMemberId(99).build());
        reset.resetData(source);
        reset.resetData(reset);

        assertEquals(source, copy);
        assertEquals(source, reset);
        copy.addPerson(draft(CARL));
        reset.addPerson(draft(CARL));
        assertEquals(copy, reset);
        assertEquals(List.of(ALICE, CARL), reset.getPersonList());
        assertEquals(4, reset.getNextMemberId());
        assertEquals(3, source.getNextMemberId());
    }

    @Test
    public void modelCopy_andFiltering_preserveIdsAndAllocationState() {
        ModelManager model = new ModelManager();
        model.addPerson(draft(ALICE));
        model.addPerson(draft(BENSON));
        model.deletePerson(BENSON);
        model.updateFilteredPersonList(person -> false);
        ModelManager copy = new ModelManager(model.getCoordiMate(), model.getUserPrefs());
        copy.addPerson(draft(CARL));
        model.setCoordiMate(copy.getCoordiMate());
        assertTrue(model.getFilteredPersonList().isEmpty());
        model.updateFilteredPersonList(person -> person.getName().equals(CARL.getName()));
        assertEquals(List.of(CARL), model.getFilteredPersonList());
        model.updateFilteredPersonList(Model.PREDICATE_SHOW_ALL_PERSONS);
        assertEquals(List.of(ALICE, CARL), model.getFilteredPersonList());
        assertEquals(4, model.getCoordiMate().getNextMemberId());
    }

    @Test
    public void equalsAndHashCode_includeAllocationState() {
        CoordiMate data = new CoordiMate();
        data.addPerson(ALICE);
        data.removePerson(ALICE);
        CoordiMate empty = new CoordiMate();
        assertNotEquals(empty, data);
        CoordiMate copy = new CoordiMate(data);
        assertEquals(data, copy);
        assertEquals(data.hashCode(), copy.hashCode());
    }

    @Test
    public void addPerson_idSpaceExhausted_doesNotOverflowOrChangeData() {
        CoordiMate data = new CoordiMate();
        Person lastMember = new PersonBuilder(ALICE).withMemberId(Integer.MAX_VALUE).build();
        data.addPerson(lastMember);
        assertEquals(Integer.MAX_VALUE + 1L, data.getNextMemberId());
        CoordiMate copy = new CoordiMate(data);
        assertEquals(data, copy);
        assertThrows(IllegalStateException.class, () -> data.addPerson(draft(BENSON)));
        assertThrows(DuplicatePersonException.class, () -> data.addPerson(draft(ALICE)));
        assertEquals(copy, data);
    }

    @Test
    public void setPersons_idSpaceExhausted_doesNotPartiallyReplaceData() {
        CoordiMate data = new CoordiMate();
        data.addPerson(CARL);
        CoordiMate before = new CoordiMate(data);
        assertThrows(IllegalStateException.class, () -> data.setPersons(List.of(
                new PersonBuilder(ALICE).withMemberId(Integer.MAX_VALUE).build(), draft(BENSON))));
        assertEquals(before, data);
    }

    @Test
    public void resetData_invalidAllocationState_doesNotChangeData() {
        CoordiMate data = new CoordiMate();
        data.addPerson(ALICE);
        CoordiMate before = new CoordiMate(data);
        for (long invalidNextId : new long[] {-1, 0, Integer.MAX_VALUE + 2L}) {
            CoordiMate invalid = new CoordiMate() {
                @Override
                public long getNextMemberId() {
                    return invalidNextId;
                }
            };
            assertThrows(IllegalArgumentException.class, () -> data.resetData(invalid));
            assertEquals(before, data);
        }
    }

    @Test
    public void addPerson_lastAvailableId_isAllocatedBeforeExhaustion() {
        CoordiMate data = new CoordiMate();
        data.addPerson(new PersonBuilder(ALICE).withMemberId(Integer.MAX_VALUE - 1).build());
        data.addPerson(draft(BENSON));
        assertEquals(new MemberId(Integer.MAX_VALUE), data.getPersonList().getLast().getMemberId().orElseThrow());
        assertEquals(Integer.MAX_VALUE + 1L, data.getNextMemberId());
    }

    private static Person draft(Person person) {
        return new PersonBuilder(person).withoutMemberId().build();
    }
}
