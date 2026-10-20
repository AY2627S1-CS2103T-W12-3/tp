package coordimate.model.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import coordimate.model.tag.Tag;

public class SampleDataUtilTest {

    @Test
    public void getSampleCoordiMate_returnsSamplePersons() {
        assertIterableEquals(Arrays.asList(SampleDataUtil.getSamplePersons()),
                SampleDataUtil.getSampleCoordiMate().getPersonList());
    }

    @Test
    public void getSampleCoordiMate_registersAllSamplePersonTags() {
        assertEquals(List.of(new Tag("EXCO"), new Tag("Sponsor"), new Tag("UniversityStaff"),
                new Tag("Logistics"), new Tag("friends"), new Tag("colleagues"),
                new Tag("neighbours"), new Tag("family"), new Tag("classmates")),
                SampleDataUtil.getSampleCoordiMate().getTagList());
    }
}
