package coordimate.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import coordimate.model.CoordiMate;
import coordimate.model.ModelManager;
import coordimate.model.UserPrefs;
import coordimate.model.event.AttendanceStatus;
import coordimate.model.person.Name;
import coordimate.model.person.Person;
import coordimate.storage.JsonCoordiMateStorage;
import coordimate.storage.JsonUserPrefsStorage;
import coordimate.storage.StorageManager;

/**
 * Checks that names containing {@code s/o} or {@code d/o} work in every command that takes a contact name.
 */
public class RelationshipNameIntegrationTest {
    @TempDir
    public Path temporaryFolder;

    private ModelManager model;
    private Logic logic;
    private JsonCoordiMateStorage storage;

    @BeforeEach
    public void setUp() {
        model = new ModelManager(new CoordiMate(), new UserPrefs());
        storage = new JsonCoordiMateStorage(temporaryFolder.resolve("data.json"));
        logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));
    }

    @Test
    public void addAndEdit_relationshipWords_savedAsTyped() throws Exception {
        logic.execute("add n/Rajesh s/o Kumar p/90000001 e/rajesh@example.com r/Member");
        logic.execute("add n/Priya D/O Raman p/90000002 e/priya@example.com r/Member");
        logic.execute("add n/Alex Yeoh p/90000003 e/alex@example.com r/Member");
        logic.execute("edit 3 n/Alex s/o Yeoh");

        List<String> expectedNames = List.of("Rajesh s/o Kumar", "Priya D/O Raman", "Alex s/o Yeoh");
        assertEquals(expectedNames, names(model.getCoordiMate().getPersonList()));
        assertEquals(expectedNames, names(storage.readCoordiMate().orElseThrow().getPersonList()));
    }

    @Test
    public void markAttendanceAndDelete_relationshipWords_findContact() throws Exception {
        logic.execute("add n/Rajesh s/o Kumar p/90000001 e/rajesh@example.com r/Member");
        logic.execute("addevent evn/Camp st/01-12-2026 et/02-12-2026");
        logic.execute("assign evn/Camp c/1");

        logic.execute("markattendance evn/Camp mem/rajesh s/o kumar att/present");
        assertEquals(Map.of(new Name("Rajesh s/o Kumar"), AttendanceStatus.PRESENT),
                model.getCoordiMate().getEventList().getFirst().getAttendanceRecord());

        logic.execute("delete n/Rajesh S/O Kumar");
        logic.execute("y");
        assertEquals(List.of(), model.getCoordiMate().getPersonList());
    }

    private static List<String> names(List<Person> persons) {
        return persons.stream().map(person -> person.getName().getFullName()).toList();
    }
}
