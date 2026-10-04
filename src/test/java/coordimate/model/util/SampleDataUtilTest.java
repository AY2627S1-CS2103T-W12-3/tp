package coordimate.model.util;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class SampleDataUtilTest {

    @Test
    public void getSampleCoordiMate_returnsSamplePersons() {
        assertIterableEquals(Arrays.asList(SampleDataUtil.getSamplePersons()),
                SampleDataUtil.getSampleCoordiMate().getPersonList());
    }
}
