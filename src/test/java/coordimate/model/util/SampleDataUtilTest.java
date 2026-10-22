package coordimate.model.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import coordimate.model.person.MemberId;
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
                new Tag("Logistics"), new Tag("friends"), new Tag("colleagues"),
                new Tag("neighbours"), new Tag("family"), new Tag("classmates")),
                SampleDataUtil.getSampleCoordiMate().getTagList());
    }
}
