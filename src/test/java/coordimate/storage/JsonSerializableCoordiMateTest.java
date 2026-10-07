package coordimate.storage;

import static coordimate.testutil.Assert.assertThrows;
import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BENSON;
import static coordimate.testutil.TypicalPersons.CARL;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import coordimate.commons.exceptions.IllegalValueException;
import coordimate.commons.util.JsonUtil;
import coordimate.model.CoordiMate;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.person.Person;
import coordimate.model.tag.Tag;
import coordimate.testutil.PersonBuilder;
import coordimate.testutil.TypicalPersons;

public class JsonSerializableCoordiMateTest {

    private static final Path TEST_DATA_FOLDER = Paths.get("src", "test", "data", "JsonSerializableCoordiMateTest");
    private static final Path TYPICAL_PERSONS_FILE = TEST_DATA_FOLDER.resolve("typicalPersonsCoordiMate.json");
    private static final Path INVALID_PERSON_FILE = TEST_DATA_FOLDER.resolve("invalidPersonCoordiMate.json");
    private static final Path DUPLICATE_PERSON_FILE = TEST_DATA_FOLDER.resolve("duplicatePersonCoordiMate.json");
    private static final Path EVENT_MEMBERS_FILE = TEST_DATA_FOLDER.resolve("eventMembersCoordiMate.json");
    private static final Path UNKNOWN_MEMBER_FILE = TEST_DATA_FOLDER.resolve("unknownMemberCoordiMate.json");

    @Test
    public void toModelType_typicalPersonsFile_success() throws Exception {
        JsonSerializableCoordiMate dataFromFile = JsonUtil.readJsonFile(TYPICAL_PERSONS_FILE,
                JsonSerializableCoordiMate.class).get();
        CoordiMate coordiMateFromFile = dataFromFile.toModelType();
        CoordiMate typicalPersonsCoordiMate = TypicalPersons.getTypicalCoordiMate();
        assertEquals(coordiMateFromFile, typicalPersonsCoordiMate);
    }

    @Test
    public void jsonRoundTrip_personRole_preserved() throws Exception {
        CoordiMate source = new CoordiMate();
        Person contact = new PersonBuilder().withRole("Logistics").build();
        source.addPerson(contact);

        String json = JsonUtil.toJsonString(new JsonSerializableCoordiMate(source));
        CoordiMate restored = JsonUtil.fromJsonString(json, JsonSerializableCoordiMate.class).toModelType();

        assertEquals(List.of(contact), restored.getPersonList());
    }

    @Test
    public void toModelType_invalidPersonFile_throwsIllegalValueException() throws Exception {
        JsonSerializableCoordiMate dataFromFile = JsonUtil.readJsonFile(INVALID_PERSON_FILE,
                JsonSerializableCoordiMate.class).get();
        assertThrows(IllegalValueException.class, dataFromFile::toModelType);
    }

    @Test
    public void toModelType_duplicatePersons_throwsIllegalValueException() throws Exception {
        JsonSerializableCoordiMate dataFromFile = JsonUtil.readJsonFile(DUPLICATE_PERSON_FILE,
                JsonSerializableCoordiMate.class).get();
        assertThrows(IllegalValueException.class, JsonSerializableCoordiMate.MESSAGE_DUPLICATE_PERSON,
                dataFromFile::toModelType);
    }

    @Test
    public void toModelType_events_preservedDuringRoundTrip() throws Exception {
        CoordiMate source = TypicalPersons.getTypicalCoordiMate();
        source.addEvent(new Event("Concert", new EventTime("08-08-2026"), new EventTime("08-08-2026 18:00")));
        source.addEvent(new Event("Fair", new EventTime("09-08-2026"), new EventTime("10-08-2026")));
        CoordiMate restored = new JsonSerializableCoordiMate(source).toModelType();
        assertEquals(source.getPersonList(), restored.getPersonList());
        assertEquals(source.getEventList(), restored.getEventList());
        assertEquals(source.getTagList(), restored.getTagList());
    }

    @Test
    public void toModelType_eventMembersFile_membersLoadedAndMissingFieldIsEmpty() throws Exception {
        CoordiMate coordiMate = JsonUtil.readJsonFile(EVENT_MEMBERS_FILE, JsonSerializableCoordiMate.class)
                .get().toModelType();
        List<Event> expected = List.of(
                new Event("Final Concert", new EventTime("08-08-2026 15:00"), new EventTime("08-08-2026 18:00"),
                        List.of(BENSON.getName(), ALICE.getName())),
                new Event("Student Life Fair", new EventTime("09-10-2026"), new EventTime("10-10-2026")));
        assertEquals(expected, coordiMate.getEventList());
    }

    @Test
    public void toModelType_eventMembers_preservedDuringRoundTrip() throws Exception {
        CoordiMate source = TypicalPersons.getTypicalCoordiMate();
        source.addEvent(new Event("Concert", new EventTime("08-08-2026"), new EventTime("08-08-2026 18:00"),
                List.of(CARL.getName(), ALICE.getName())));
        CoordiMate restored = new JsonSerializableCoordiMate(source).toModelType();
        assertEquals(source, restored);
    }

    @Test
    public void toModelType_unknownMember_throwsIllegalValueException() throws Exception {
        JsonSerializableCoordiMate dataFromFile = JsonUtil.readJsonFile(UNKNOWN_MEMBER_FILE,
                JsonSerializableCoordiMate.class).get();
        assertThrows(IllegalValueException.class, JsonSerializableCoordiMate.MESSAGE_UNKNOWN_MEMBER,
                dataFromFile::toModelType);
    }

    @Test
    public void toModelType_duplicateEventNames_throwsIllegalValueException() {
        List<JsonAdaptedEvent> events = List.of(
                new JsonAdaptedEvent("Concert", "08-08-2026", "08-08-2026"),
                new JsonAdaptedEvent(" Concert ", "09-08-2026", "10-08-2026"));
        JsonSerializableCoordiMate data = new JsonSerializableCoordiMate(List.of(), events);
        assertThrows(IllegalValueException.class, JsonSerializableCoordiMate.MESSAGE_DUPLICATE_EVENT,
                data::toModelType);
    }

    @Test
    public void toModelType_customTags_preservedDuringRoundTrip() throws Exception {
        CoordiMate source = new CoordiMate();
        source.addTag(new Tag("Publicity"));

        CoordiMate restored = new JsonSerializableCoordiMate(source).toModelType();

        assertEquals(source.getTagList(), restored.getTagList());
    }

    @Test
    public void toModelType_missingTags_addsDefaultTags() throws Exception {
        JsonSerializableCoordiMate data = new JsonSerializableCoordiMate(List.of(), List.of());

        CoordiMate restored = data.toModelType();

        assertEquals(new CoordiMate().getTagList(), restored.getTagList());
    }

    @Test
    public void toModelType_duplicateTagNames_throwsIllegalValueException() {
        List<JsonAdaptedTag> tags = List.of(new JsonAdaptedTag("Publicity"), new JsonAdaptedTag("publicity"));
        JsonSerializableCoordiMate data = new JsonSerializableCoordiMate(List.of(), List.of(), tags);

        assertThrows(IllegalValueException.class, JsonSerializableCoordiMate.MESSAGE_DUPLICATE_TAG,
                data::toModelType);
    }

    @Test
    public void toModelType_invalidTag_throwsIllegalValueException() {
        JsonSerializableCoordiMate data = new JsonSerializableCoordiMate(List.of(), List.of(),
                List.of(new JsonAdaptedTag("invalid tag")));

        assertThrows(IllegalValueException.class, data::toModelType);
    }

    @Test
    public void toModelType_nullListEntries_throwsIllegalValueException() {
        JsonSerializableCoordiMate nullPerson = new JsonSerializableCoordiMate(
                Arrays.asList((JsonAdaptedPerson) null), List.of());
        assertThrows(IllegalValueException.class, "Persons list must not contain null entries.",
                nullPerson::toModelType);
        JsonSerializableCoordiMate nullEvent = new JsonSerializableCoordiMate(List.of(),
                Arrays.asList((JsonAdaptedEvent) null));
        assertThrows(IllegalValueException.class, "Events list must not contain null entries.", nullEvent::toModelType);
        JsonSerializableCoordiMate nullTag = new JsonSerializableCoordiMate(List.of(), List.of(),
                Arrays.asList((JsonAdaptedTag) null));
        assertThrows(IllegalValueException.class, "Tags list must not contain null entries.", nullTag::toModelType);
    }

    @Test
    public void constructor_missingPersons_throwsIllegalArgumentException() {
        String message = "Persons list must be present.";
        assertThrows(IllegalArgumentException.class, message, () -> new JsonSerializableCoordiMate(null, List.of()));
    }

}
