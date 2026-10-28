package coordimate.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import coordimate.logic.commands.AddCommand;
import coordimate.logic.commands.AddEventCommand;
import coordimate.logic.commands.EditCommand;
import coordimate.logic.commands.EditEventCommand;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.CoordiMate;
import coordimate.model.ModelManager;
import coordimate.model.event.AttendanceStatus;
import coordimate.model.event.Event;
import coordimate.model.person.Name;
import coordimate.model.person.Person;
import coordimate.storage.JsonCoordiMateStorage;
import coordimate.storage.JsonUserPrefsStorage;
import coordimate.storage.StorageManager;
import coordimate.testutil.UnicodeWhitespace;

/**
 * Checks that contact and event names differing only in internal spacing refer to the same contact or event.
 */
public class NameSpacingIntegrationTest {
    @TempDir
    public Path temporaryFolder;

    private final Name alex = new Name("Alex Yeoh");
    private ModelManager model;
    private JsonCoordiMateStorage storage;
    private Logic logic;

    @BeforeEach
    public void setUp() throws Exception {
        model = new ModelManager();
        storage = new JsonCoordiMateStorage(temporaryFolder.resolve("data.json"));
        logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));
        logic.execute("add n/Alex Yeoh p/90000000 e/alex@example.com r/Member");
        logic.execute("add n/Bernice Yu p/90000002 e/bernice@example.com r/Member");
        logic.execute("addevent evn/Final Concert st/08-08-2027 et/08-08-2027");
        logic.execute("addevent evn/Open Day st/09-08-2027 et/09-08-2027");
    }

    @Test
    public void add_nameDifferingOnlyInInternalSpacing_rejectedAsDuplicate() {
        assertRejected("add n/Alex  Yeoh p/90000001 e/alex2@example.com r/Member",
                AddCommand.MESSAGE_DUPLICATE_PERSON);
        assertRejected("add n/ALEX    yeoh p/90000001 e/alex2@example.com r/Member",
                AddCommand.MESSAGE_DUPLICATE_PERSON);
    }

    @Test
    public void add_nameWithExtraInternalSpaces_savedWithSingleSpaces() throws Exception {
        logic.execute("add n/Charlotte   Oliveiro p/90000003 e/charlotte@example.com r/Member");

        assertEquals(new Name("Charlotte Oliveiro"), model.getCoordiMate().getPersonList().get(2).getName());
        assertEquals("Charlotte Oliveiro", model.getCoordiMate().getPersonList().get(2).getName().getFullName());
        assertEquals(model.getCoordiMate(), storage.readCoordiMate().orElseThrow());
    }

    @Test
    public void edit_renameToNameDifferingOnlyInInternalSpacing_rejectedAsDuplicate() {
        assertRejected("edit target/bernice@example.com n/Alex   Yeoh", EditCommand.MESSAGE_DUPLICATE_PERSON);
        assertRejected("edit 2 n/ALEX  YEOH", EditCommand.MESSAGE_DUPLICATE_PERSON);
    }

    @Test
    public void edit_targetNameWithExtraInternalSpaces_editsContact() throws Exception {
        logic.execute("edit target/Alex   Yeoh r/Treasurer");

        assertEquals("Treasurer", findPerson(alex).getRole().getValue());
    }

    @Test
    public void delete_nameWithExtraInternalSpaces_deletesContact() throws Exception {
        String prompt = logic.execute("delete n/Alex  Yeoh").getFeedbackToUser();
        assertTrue(prompt.contains("Alex Yeoh"));

        logic.execute("y");

        assertEquals(List.of(new Name("Bernice Yu")),
                model.getCoordiMate().getPersonList().stream().map(Person::getName).toList());
    }

    @Test
    public void addEvent_nameDifferingOnlyInInternalSpacing_rejectedAsDuplicate() {
        assertRejected("addevent evn/Final  Concert st/08-08-2027 et/08-08-2027",
                AddEventCommand.MESSAGE_DUPLICATE_EVENT);
        assertEquals(2, model.getCoordiMate().getEventList().size());
    }

    @Test
    public void editEvent_renameToNameDifferingOnlyInInternalSpacing_rejectedAsDuplicate() {
        assertRejected("editevent evn/Open Day nevn/final   concert", EditEventCommand.MESSAGE_DUPLICATE_EVENT);
    }

    @Test
    public void editEvent_namesWithExtraInternalSpaces_targetFoundAndNewNameCollapsed() throws Exception {
        logic.execute("editevent evn/Open   Day nevn/Orientation    Day");

        assertEquals(List.of("Final Concert", "Orientation Day"),
                model.getCoordiMate().getEventList().stream().map(Event::getName).toList());
    }

    @Test
    public void markAttendance_namesWithExtraInternalSpaces_marksAssignedMember() throws Exception {
        logic.execute("assign evn/Final Concert c/1");

        logic.execute("markattendance evn/Final  Concert mem/Alex  Yeoh att/present");

        assertEquals(AttendanceStatus.PRESENT, findEvent("Final Concert").getAttendance(alex).orElseThrow());
    }

    @Test
    public void eventCommands_namesWithExtraInternalSpaces_findEvent() throws Exception {
        logic.execute("assign evn/Final   Concert c/1 2");
        logic.execute("unassign evn/Final  Concert c/2");
        assertEquals(List.of(alex), findEvent("Final Concert").getMembers());

        logic.execute("members evn/Final  Concert");
        assertEquals(List.of(alex), model.getFilteredPersonList().stream().map(Person::getName).toList());

        logic.execute("list");
        logic.execute("filter event/Final  Concert");
        assertEquals(List.of(alex), model.getFilteredPersonList().stream().map(Person::getName).toList());

        logic.execute("archiveevent evn/Open   Day");
        assertTrue(findEvent("Open Day").isArchived());

        logic.execute("deleteevent evn/Final   Concert");
        assertEquals(List.of("Open Day"),
                model.getCoordiMate().getEventList().stream().map(Event::getName).toList());
    }

    @Test
    public void eventCommands_unicodeSpaces_savedDisplayNameRemainsUsableAfterReload() throws Exception {
        for (String space : UnicodeWhitespace.ALL) {
            if (Character.getType(space.codePointAt(0)) != Character.SPACE_SEPARATOR) {
                continue;
            }
            logic.execute("addevent evn/Planning" + space.repeat(2) + "Meeting st/01-01-2027 et/01-01-2027");
            assertEquals("Planning Meeting", findEvent("Planning Meeting").getName());
            assertEquals(model.getCoordiMate(), storage.readCoordiMate().orElseThrow());
            model.setCoordiMate(storage.readCoordiMate().orElseThrow());

            assertRejected("addevent evn/Planning Meeting st/01-01-2027 et/01-01-2027",
                    AddEventCommand.MESSAGE_DUPLICATE_EVENT);
            logic.execute("editevent evn/Planning Meeting nevn/Final" + space + "Review");
            String displayedName = findEvent("Final Review").getName();
            assertEquals("Final Review", displayedName);
            logic.execute("archiveevent evn/" + displayedName);
            assertTrue(findEvent(displayedName).isArchived());
            logic.execute("unarchiveevent evn/Final" + space + "Review");
            logic.execute("deleteevent evn/" + displayedName);

            assertEquals(List.of("Final Concert", "Open Day"),
                    model.getCoordiMate().getEventList().stream().map(Event::getName).toList());
            assertEquals(model.getCoordiMate(), storage.readCoordiMate().orElseThrow());
        }
    }

    @Test
    public void eventCommands_formatCharacters_preservedAfterReloadAndUsableInCommands() throws Exception {
        for (String character : List.of("\u180e", "\u200b", "\u2060", "\ufeff")) {
            String name = "Planning" + character + "c/review";
            logic.execute("addevent evn/" + name + " st/01-01-2027 et/01-01-2027");
            model.setCoordiMate(storage.readCoordiMate().orElseThrow());
            assertEquals(name, findEvent(name).getName());

            String renamed = "Final" + character + "c/review";
            logic.execute("editevent evn/" + name + " nevn/" + renamed);
            logic.execute("archiveevent evn/" + renamed);
            assertTrue(findEvent(renamed).isArchived());
            logic.execute("unarchiveevent evn/" + renamed);
            logic.execute("deleteevent evn/" + renamed);
            assertEquals(model.getCoordiMate(), storage.readCoordiMate().orElseThrow());
        }
    }

    @Test
    public void eventCommands_hiddenPrefixes_rejectedWithoutChangingModelOrFile() throws Exception {
        for (String space : UnicodeWhitespace.ALL) {
            CoordiMate before = new CoordiMate(model.getCoordiMate());
            String beforeFile = Files.readString(storage.getCoordiMateFilePath());
            assertThrows(ParseException.class, () -> logic.execute(
                    "addevent evn/Planning" + space + "c/review st/01-01-2027 et/01-01-2027"));
            assertThrows(ParseException.class, () -> logic.execute(
                    "editevent evn/Open Day nevn/Planning" + space + "c/review"));
            assertEquals(before, model.getCoordiMate());
            assertEquals(beforeFile, Files.readString(storage.getCoordiMateFilePath()));
        }
    }

    private void assertRejected(String command, String expectedMessage) {
        CoordiMate before = new CoordiMate(model.getCoordiMate());
        String message = assertThrows(CommandException.class, () -> logic.execute(command)).getMessage();
        assertTrue(message.startsWith(expectedMessage), message);
        assertEquals(before, model.getCoordiMate());
    }

    private Person findPerson(Name name) {
        return model.getCoordiMate().getPersonList().stream()
                .filter(person -> person.getName().equals(name)).findFirst().orElseThrow();
    }

    private Event findEvent(String name) {
        return model.getCoordiMate().findEvent(name).orElseThrow();
    }
}
