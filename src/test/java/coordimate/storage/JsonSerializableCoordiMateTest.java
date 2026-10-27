package coordimate.storage;

import static coordimate.testutil.Assert.assertThrows;
import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BENSON;
import static coordimate.testutil.TypicalPersons.CARL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.LongNode;

import coordimate.commons.exceptions.IllegalValueException;
import coordimate.commons.util.JsonUtil;
import coordimate.model.CoordiMate;
import coordimate.model.event.AttendanceStatus;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.person.MemberId;
import coordimate.model.person.Name;
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
    private static final Path INTERNAL_SPACING_FILE = TEST_DATA_FOLDER.resolve("internalSpacingCoordiMate.json");
    private static final Path INTERNAL_SPACING_DUPLICATE_PERSON_FILE =
            TEST_DATA_FOLDER.resolve("internalSpacingDuplicatePersonCoordiMate.json");
    private static final Path INTERNAL_SPACING_DUPLICATE_EVENT_FILE =
            TEST_DATA_FOLDER.resolve("internalSpacingDuplicateEventCoordiMate.json");

    @Test
    public void removeYearZeroEvents_mixedEvents_preservesOtherDataAndRemovesOnlyOnce() throws Exception {
        EventTime start = new EventTime("01-01-0001");
        Event kept = new Event("Keep", start, new EventTime("31-12-9999 23:59"), List.of(ALICE.getName()),
                Map.of(ALICE.getName(), AttendanceStatus.ABSENT), Set.of(start)).withArchived(true);
        List<JsonAdaptedEvent> events = List.of(
                new JsonAdaptedEvent("Zero Start", "01-01-0000", "02-01-0001"),
                new JsonAdaptedEvent(kept),
                new JsonAdaptedEvent("Zero End", "01-01-0001", "02-01-0000 12:30"),
                new JsonAdaptedEvent("Zero History", "01-01-0001", "02-01-0001", List.of(), Map.of(),
                        List.of("01-01-0000")),
                new JsonAdaptedEvent(null, "01-01-0000", "02-01-0001"));
        JsonSerializableCoordiMate data = new JsonSerializableCoordiMate(List.of(new JsonAdaptedPerson(ALICE)),
                events, List.of(new JsonAdaptedTag("Unused")), LongNode.valueOf(50));
        CoordiMate expected = new CoordiMate(50);
        expected.setTags(List.of(new Tag("Unused")));
        expected.addPerson(ALICE);
        expected.addEvent(kept);

        assertEquals(List.of("Zero Start", "Zero End", "Zero History", "(unnamed event)"),
                data.removeYearZeroEvents());
        assertEquals(expected, data.toModelType());
        assertEquals(List.of(), data.removeYearZeroEvents());
        assertEquals(expected, data.toModelType());
    }

    @Test
    public void removeYearZeroEvents_nullEvent_retainsEntryForValidation() {
        JsonSerializableCoordiMate data = new JsonSerializableCoordiMate(List.of(),
                Arrays.asList((JsonAdaptedEvent) null));

        assertEquals(List.of(), data.removeYearZeroEvents());
        assertThrows(IllegalValueException.class, "Events list must not contain null entries.", data::toModelType);
    }

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

        assertEquals(List.of(contact.withMemberId(new MemberId(1))), restored.getPersonList());
    }

    @Test
    public void jsonRoundTrip_optionalContactDetails_preserved() throws Exception {
        CoordiMate source = new CoordiMate();
        Person contact = new PersonBuilder().withBirthday("18-06-2004")
                .withOrganisation("NUS Student Affairs").withNote("Handles venue bookings").build();
        source.addPerson(contact);

        String json = JsonUtil.toJsonString(new JsonSerializableCoordiMate(source));
        CoordiMate restored = JsonUtil.fromJsonString(json, JsonSerializableCoordiMate.class).toModelType();

        assertEquals(List.of(contact.withMemberId(new MemberId(1))), restored.getPersonList());
    }

    @Test
    public void jsonRoundTrip_contactWithoutAddress_remainsUnset() throws Exception {
        CoordiMate source = new CoordiMate();
        Person contact = new PersonBuilder().withoutAddress().build();
        source.addPerson(contact);

        String json = JsonUtil.toJsonString(new JsonSerializableCoordiMate(source));
        CoordiMate restored = JsonUtil.fromJsonString(json, JsonSerializableCoordiMate.class).toModelType();

        assertEquals(List.of(contact.withMemberId(new MemberId(1))), restored.getPersonList());
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
    public void toModelType_duplicateNamesIgnoringCase_throwsIllegalValueException() {
        Person differentAlice = new PersonBuilder(BENSON).withName("  aLiCe pAuLiNe  ").build();
        JsonSerializableCoordiMate data = new JsonSerializableCoordiMate(
                List.of(new JsonAdaptedPerson(ALICE), new JsonAdaptedPerson(differentAlice)), List.of());

        assertThrows(IllegalValueException.class, JsonSerializableCoordiMate.MESSAGE_DUPLICATE_PERSON,
                data::toModelType);
    }

    @Test
    public void toModelType_namesWithExtraInternalSpaces_loadedCollapsedWithMatchingMembers() throws Exception {
        CoordiMate coordiMate = JsonUtil.readJsonFile(INTERNAL_SPACING_FILE, JsonSerializableCoordiMate.class)
                .get().toModelType();
        Name alice = new Name("Alice Pauline");
        assertEquals("Alice Pauline", coordiMate.getPersonList().getFirst().getName().getFullName());
        Event concert = coordiMate.getEventList().getFirst();
        assertEquals("Final Concert", concert.getName());
        assertEquals(List.of(alice), concert.getMembers());
        assertEquals(AttendanceStatus.PRESENT, concert.getAttendance(alice).orElseThrow());
    }

    @Test
    public void toModelType_namesDifferingOnlyInInternalSpacing_throwsIllegalValueException() throws Exception {
        JsonSerializableCoordiMate duplicatePersons = JsonUtil.readJsonFile(INTERNAL_SPACING_DUPLICATE_PERSON_FILE,
                JsonSerializableCoordiMate.class).get();
        assertThrows(IllegalValueException.class, JsonSerializableCoordiMate.MESSAGE_DUPLICATE_PERSON,
                duplicatePersons::toModelType);

        JsonSerializableCoordiMate duplicateEvents = JsonUtil.readJsonFile(INTERNAL_SPACING_DUPLICATE_EVENT_FILE,
                JsonSerializableCoordiMate.class).get();
        assertThrows(IllegalValueException.class, JsonSerializableCoordiMate.MESSAGE_DUPLICATE_EVENT,
                duplicateEvents::toModelType);
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
    public void toModelType_renamedDefaultTag_preservedDuringRoundTrip() throws Exception {
        CoordiMate source = new CoordiMate();
        source.setTag(new Tag("EXCO"), new Tag("Committee"));

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
    public void toModelType_missingTagList_registersContactTagsAfterDefaultTags() throws Exception {
        Person contact = new PersonBuilder().withTags("Publicity", "friends").build();
        JsonSerializableCoordiMate data = new JsonSerializableCoordiMate(
                List.of(new JsonAdaptedPerson(contact)), List.of());

        CoordiMate restored = data.toModelType();

        assertEquals(List.of(new Tag("EXCO"), new Tag("Sponsor"), new Tag("UniversityStaff"),
                new Tag("Logistics"), new Tag("friends"), new Tag("Publicity")), restored.getTagList());
    }

    @Test
    public void toModelType_incompleteTagList_preservesSavedTagsAndRegistersContactTags() throws Exception {
        Person contact = new PersonBuilder().withTags("friends", "Publicity").build();
        JsonSerializableCoordiMate data = new JsonSerializableCoordiMate(
                List.of(new JsonAdaptedPerson(contact)), List.of(),
                List.of(new JsonAdaptedTag("FRIENDS"), new JsonAdaptedTag("Unused")));

        CoordiMate restored = data.toModelType();

        assertEquals(List.of(new Tag("FRIENDS"), new Tag("Unused"), new Tag("Publicity")), restored.getTagList());
        assertEquals(List.of(contact.withMemberId(new MemberId(1))), restored.getPersonList());
    }

    @Test
    public void toModelType_emptyTagList_registersOnlyContactTags() throws Exception {
        Person contact = new PersonBuilder().withTags("friends").build();
        JsonSerializableCoordiMate data = new JsonSerializableCoordiMate(
                List.of(new JsonAdaptedPerson(contact)), List.of(), List.of());

        CoordiMate restored = data.toModelType();

        assertEquals(List.of(new Tag("friends")), restored.getTagList());
    }

    @Test
    public void toModelType_deletedTags_preservedDuringRoundTrip() throws Exception {
        CoordiMate source = new CoordiMate();
        source.getTagList().stream().toList().forEach(source::removeTag);

        CoordiMate restored = new JsonSerializableCoordiMate(source).toModelType();

        assertEquals(List.of(), restored.getTagList());
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

    @Test
    public void toModelType_legacyContactsWithoutIds_keepArchiveStatus() throws Exception {
        String json = """
                {"persons": [
                  {"name": "Alice Tan", "phone": "91234567", "email": "alice@example.com", "role": "Member",
                   "archived": true},
                  {"name": "Bob Lim", "phone": "98765432", "email": "bob@example.com", "role": "Member"}],
                 "events": [
                  {"name": "Orientation", "startTime": "25-10-2026", "endTime": "25-10-2026",
                   "members": ["Alice Tan"], "archived": true}]}
                """;

        CoordiMate data = JsonUtil.fromJsonString(json, JsonSerializableCoordiMate.class).toModelType();

        Person alice = data.getPersonList().get(0);
        Person bob = data.getPersonList().get(1);
        assertEquals(new MemberId(1), alice.getMemberId().orElseThrow());
        assertEquals(new MemberId(2), bob.getMemberId().orElseThrow());
        assertTrue(alice.isArchived());
        assertFalse(bob.isArchived());
        assertTrue(data.getEventList().getFirst().isArchived());

        CoordiMate reloaded = JsonUtil.fromJsonString(
                JsonUtil.toJsonString(new JsonSerializableCoordiMate(data)), JsonSerializableCoordiMate.class)
                .toModelType();
        assertEquals(data.getPersonList(), reloaded.getPersonList());
        assertEquals(data.getEventList(), reloaded.getEventList());
    }
}
