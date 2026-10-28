package coordimate.model.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import coordimate.model.ReadOnlyCoordiMate;
import coordimate.model.event.AttendanceStatus;
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
        Name alex = new Name("Alex Yeoh");
        Name bernice = new Name("Bernice Yu");
        assertEquals(List.of(alex, bernice), event.getMembers());
        assertEquals(Optional.of(AttendanceStatus.PRESENT), event.getAttendance(alex));
        assertEquals(Optional.empty(), event.getAttendance(bernice));
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
