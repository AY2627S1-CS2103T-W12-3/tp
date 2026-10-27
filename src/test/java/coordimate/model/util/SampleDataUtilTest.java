package coordimate.model.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import coordimate.model.ReadOnlyCoordiMate;
import coordimate.model.event.Event;
import coordimate.model.person.MemberId;
import coordimate.model.person.Name;
import coordimate.model.person.Person;
import coordimate.model.tag.Tag;

public class SampleDataUtilTest {

    @Test
    public void getSampleCoordiMate_returnsSamplePersons() {
        Person[] samplePersons = SampleDataUtil.getSamplePersons();
        List<Person> storedPersons = SampleDataUtil.getSampleCoordiMate().getPersonList();
        assertEquals(samplePersons.length, storedPersons.size());
        for (int i = 0; i < samplePersons.length; i++) {
            assertEquals(samplePersons[i].withMemberId(new MemberId(i + 1)), storedPersons.get(i));
        }
    }

    @Test
    public void getSampleCoordiMate_registersAllSamplePersonTags() {
        assertEquals(List.of(new Tag("EXCO"), new Tag("Sponsor"), new Tag("UniversityStaff"),
                new Tag("Logistics"), new Tag("friends"), new Tag("neighbours")),
                SampleDataUtil.getSampleCoordiMate().getTagList());
    }

    @Test
    public void getSampleCoordiMate_returnsEventWithMembersAndAttendance() {
        ReadOnlyCoordiMate sampleData = SampleDataUtil.getSampleCoordiMate();

        assertEquals(1, sampleData.getEventList().size());
        Event event = sampleData.getEventList().getFirst();
        assertFalse(event.getMembers().isEmpty());
        assertTrue(event.getMembers().size() <= 2);
        assertTrue(event.getMembers().stream().allMatch(member -> sampleData.getPersonList().stream()
                .map(Person::getName)
                .anyMatch(member::equals)));
        assertTrue(event.getMembers().stream().anyMatch(member -> event.getAttendance(member).isPresent()));
    }

    @Test
    public void getSampleCoordiMate_excoTagHasContactsForTutorial() {
        ReadOnlyCoordiMate sampleData = SampleDataUtil.getSampleCoordiMate();

        assertEquals(new Tag("EXCO"), sampleData.getTagList().getFirst());
        assertEquals(List.of("Alex Yeoh", "Bernice Yu", "Irfan Ibrahim"), sampleData.getPersonList().stream()
                .filter(person -> person.getTags().contains(new Tag("EXCO")))
                .map(Person::getName)
                .map(Name::getFullName)
                .toList());
    }
}
