package coordimate.model.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import coordimate.model.CoordiMate;
import coordimate.model.ReadOnlyCoordiMate;
import coordimate.model.event.AttendanceStatus;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.person.Address;
import coordimate.model.person.Email;
import coordimate.model.person.Name;
import coordimate.model.person.Person;
import coordimate.model.person.Phone;
import coordimate.model.person.Role;
import coordimate.model.tag.Tag;

/**
 * Contains utility methods for populating {@code CoordiMate} with sample data.
 */
public class SampleDataUtil {
    /**
     * Returns the sample contacts used when no saved data exists.
     */
    public static Person[] getSamplePersons() {
        return new Person[] {
            new Person(new Name("Alex Yeoh"), new Phone("87438807"), new Email("alexyeoh@example.com"),
                    new Role("President"), new Address("Blk 30 Geylang Street 29, #06-40"),
                    getTagSet("EXCO", "friends")),
            new Person(new Name("Bernice Yu"), new Phone("99272758"), new Email("berniceyu@example.com"),
                    new Role("Logistics Lead"), new Address("Blk 30 Lorong 3 Serangoon Gardens, #07-18"),
                    getTagSet("EXCO", "Logistics", "friends")),
            new Person(new Name("Charlotte Oliveiro"), new Phone("93210283"), new Email("charlotte@example.com"),
                    new Role("Faculty Advisor"), new Address("Blk 11 Ang Mo Kio Street 74, #11-04"),
                    getTagSet("UniversityStaff", "neighbours")),
            new Person(new Name("David Li"), new Phone("91031282"), new Email("lidavid@example.com"),
                    new Role("Sponsor Representative"), new Address("Blk 436 Serangoon Gardens Street 26, #16-43"),
                    getTagSet("Sponsor")),
            new Person(new Name("Irfan Ibrahim"), new Phone("92492021"), new Email("irfan@example.com"),
                    new Role("Welfare Secretary"), new Address("Blk 47 Tampines Street 20, #17-35"),
                    getTagSet("EXCO")),
            new Person(new Name("Roy Balakrishnan"), new Phone("92624417"), new Email("royb@example.com"),
                    new Role("Logistics Volunteer"), new Address("Blk 45 Aljunied Street 85, #11-31"),
                    getTagSet("Logistics"))
        };
    }

    /**
     * Returns the sample events used when no saved data exists.
     */
    public static Event[] getSampleEvents() {
        Name alex = new Name("Alex Yeoh");
        Name bernice = new Name("Bernice Yu");
        return new Event[] {
            new Event("Freshmen Welcome Night", new EventTime("17-10-2026 18:00"),
                    new EventTime("17-10-2026 21:00"), List.of(alex, bernice),
                    Map.of(alex, AttendanceStatus.PRESENT))
        };
    }

    /**
     * Returns a new CoordiMate populated with the sample contacts and events.
     */
    public static ReadOnlyCoordiMate getSampleCoordiMate() {
        CoordiMate sampleCoordiMate = new CoordiMate();
        for (Person samplePerson : getSamplePersons()) {
            sampleCoordiMate.addPerson(samplePerson);
        }
        for (Event sampleEvent : getSampleEvents()) {
            sampleCoordiMate.addEvent(sampleEvent);
        }
        return sampleCoordiMate;
    }

    /**
     * Returns a tag set containing the list of strings given.
     */
    public static Set<Tag> getTagSet(String... strings) {
        return Arrays.stream(strings)
                .map(Tag::new)
                .collect(Collectors.toSet());
    }

}
