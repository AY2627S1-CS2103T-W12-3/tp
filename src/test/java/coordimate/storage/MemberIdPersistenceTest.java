package coordimate.storage;

import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BENSON;
import static coordimate.testutil.TypicalPersons.CARL;
import static coordimate.testutil.TypicalPersons.getTypicalCoordiMate;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import coordimate.commons.exceptions.DataLoadingException;
import coordimate.commons.util.JsonUtil;
import coordimate.model.CoordiMate;
import coordimate.model.ModelManager;
import coordimate.model.ReadOnlyCoordiMate;
import coordimate.model.UserPrefs;
import coordimate.model.person.MemberId;
import coordimate.model.person.Person;
import coordimate.testutil.PersonBuilder;

public class MemberIdPersistenceTest {
    @TempDir
    public Path testFolder;

    @Test
    public void saveAndRestart_editedMemberAndDeletedHighestId_preservesIdsAndAllocation() throws Exception {
        CoordiMate source = new CoordiMate();
        Person alice = new PersonBuilder(ALICE).withMemberId(12).build();
        Person benson = new PersonBuilder(BENSON).withMemberId(45).build();
        source.addPerson(alice);
        source.addPerson(benson);
        source.removePerson(benson);
        Person editedAlice = new PersonBuilder(alice).withName("Alice Tan").withRole("President").build();
        source.setPerson(alice, editedAlice);
        Path path = testFolder.resolve("members.json");
        new JsonCoordiMateStorage(path).saveCoordiMate(source);

        ReadOnlyCoordiMate restored = new JsonCoordiMateStorage(path).readCoordiMate().orElseThrow();
        assertEquals(source, restored);
        ModelManager restarted = new ModelManager(restored, new UserPrefs());
        restarted.addPerson(new PersonBuilder(CARL).withoutMemberId().build());
        assertEquals(List.of(editedAlice, new PersonBuilder(CARL).withMemberId(46).build()),
                restarted.getCoordiMate().getPersonList());
        assertEquals(47, restarted.getCoordiMate().getNextMemberId());

        new JsonCoordiMateStorage(path).saveCoordiMate(restarted.getCoordiMate());
        assertEquals(restarted.getCoordiMate(), new JsonCoordiMateStorage(path).readCoordiMate().orElseThrow());
    }

    @Test
    public void saveAndRestart_emptyAfterDeletion_doesNotReuseIds() throws Exception {
        CoordiMate source = new CoordiMate();
        Person member = new PersonBuilder(ALICE).withMemberId(12).build();
        source.addPerson(member);
        source.removePerson(member);
        Path path = testFolder.resolve("empty.json");
        new JsonCoordiMateStorage(path).saveCoordiMate(source);

        CoordiMate restarted = new CoordiMate(new JsonCoordiMateStorage(path).readCoordiMate().orElseThrow());
        assertEquals(source, restarted);
        restarted.addPerson(new PersonBuilder(BENSON).withoutMemberId().build());
        assertEquals(new MemberId(13), restarted.getPersonList().getFirst().getMemberId().orElseThrow());
    }

    @Test
    public void legacyFile_assignsDeterministicIdsAndSavesThemForSubsequentRestarts() throws Exception {
        Path path = testFolder.resolve("legacy.json");
        Files.copy(Path.of("src", "test", "data", "JsonSerializableCoordiMateTest",
                "typicalPersonsCoordiMate.json"), path);
        byte[] original = Files.readAllBytes(path);
        JsonCoordiMateStorage storage = new JsonCoordiMateStorage(path);
        ReadOnlyCoordiMate migrated = storage.readCoordiMate().orElseThrow();
        assertEquals(getTypicalCoordiMate(), migrated);
        assertEquals(migrated, new JsonCoordiMateStorage(path).readCoordiMate().orElseThrow());
        assertArrayEquals(original, Files.readAllBytes(path));

        storage.saveCoordiMate(migrated);
        JsonNode saved = JsonUtil.fromJsonString(Files.readString(path), JsonNode.class);
        assertTrue(saved.get("nextMemberId").isIntegralNumber());
        assertEquals(8, saved.get("nextMemberId").longValue());
        for (int i = 0; i < migrated.getPersonList().size(); i++) {
            assertTrue(saved.get("persons").get(i).get("memberId").isIntegralNumber());
            assertEquals(i + 1, saved.get("persons").get(i).get("memberId").intValue());
        }
        assertEquals(migrated, new JsonCoordiMateStorage(path).readCoordiMate().orElseThrow());
    }

    @Test
    public void mixedRecords_missingIdsReserveAllExistingIdsBeforeAllocation() throws Exception {
        Path path = writeStore(storeJson(null, personJson(ALICE, null), personJson(BENSON, "1"),
                personJson(CARL, "12")));
        ReadOnlyCoordiMate restored = new JsonCoordiMateStorage(path).readCoordiMate().orElseThrow();
        assertEquals(List.of(new PersonBuilder(ALICE).withMemberId(13).build(),
                new PersonBuilder(BENSON).withMemberId(1).build(),
                new PersonBuilder(CARL).withMemberId(12).build()), restored.getPersonList());
        assertEquals(14, restored.getNextMemberId());

        CoordiMate reordered = new CoordiMate(restored);
        reordered.setPersons(List.of(restored.getPersonList().getLast(), restored.getPersonList().getFirst(),
                restored.getPersonList().get(1)));
        new JsonCoordiMateStorage(path).saveCoordiMate(reordered);
        assertEquals(reordered, new JsonCoordiMateStorage(path).readCoordiMate().orElseThrow());
    }

    @Test
    public void mixedRecords_missingIdStartsAtSavedNextId() throws Exception {
        Path path = writeStore(storeJson("50", personJson(ALICE, null), personJson(BENSON, "12"),
                personJson(CARL, "null")));
        ReadOnlyCoordiMate restored = new JsonCoordiMateStorage(path).readCoordiMate().orElseThrow();
        assertEquals(List.of(new PersonBuilder(ALICE).withMemberId(50).build(),
                new PersonBuilder(BENSON).withMemberId(12).build(),
                new PersonBuilder(CARL).withMemberId(51).build()), restored.getPersonList());
        assertEquals(52, restored.getNextMemberId());
    }

    @Test
    public void nullIdsAndCounter_areMigratedLikeMissingFields() throws Exception {
        Path path = writeStore(storeJson("null", personJson(ALICE, "null"), personJson(BENSON, null)));
        ReadOnlyCoordiMate restored = new JsonCoordiMateStorage(path).readCoordiMate().orElseThrow();
        assertEquals(List.of(ALICE, BENSON), restored.getPersonList());
        assertEquals(3, restored.getNextMemberId());
    }

    @Test
    public void duplicateIds_rejectsFileWithoutChangingIt() throws Exception {
        assertInvalidStore(storeJson(null, personJson(ALICE, "12"), personJson(BENSON, "12")),
                JsonSerializableCoordiMate.MESSAGE_DUPLICATE_MEMBER_ID);
    }

    @Test
    public void invalidIds_rejectsWrongTypesNonPositiveAndOutOfRangeValues() throws Exception {
        for (String invalidId : List.of("0", "-1", "2147483648", "999999999999999999999999", "1.5", "1.0",
                "1e0", "\"12\"", "true", "[]", "{}")) {
            assertInvalidStore(storeJson(null, personJson(ALICE, invalidId)), MemberId.MESSAGE_CONSTRAINTS);
        }
    }

    @Test
    public void invalidNextId_rejectsWrongTypesAndOutOfRangeValues() throws Exception {
        for (String invalidNextId : List.of("0", "-1", "2147483649", "999999999999999999999999", "1.5", "1.0",
                "1e0", "\"12\"", "false", "[]", "{}")) {
            assertInvalidStore(storeJson(invalidNextId), JsonSerializableCoordiMate.MESSAGE_INVALID_NEXT_MEMBER_ID);
        }
    }

    @Test
    public void nextIdNotGreaterThanSavedIds_rejectsInconsistentFile() throws Exception {
        for (String nextId : List.of("1", "11", "12")) {
            assertInvalidStore(storeJson(nextId, personJson(ALICE, "12")),
                    JsonSerializableCoordiMate.MESSAGE_INVALID_NEXT_MEMBER_ID);
        }
    }

    @Test
    public void exhaustedIdRange_canStillSaveAndReloadExistingMembers() throws Exception {
        Path path = writeStore(storeJson("2147483648", personJson(ALICE, "2147483647")));
        CoordiMate restored = new CoordiMate(new JsonCoordiMateStorage(path).readCoordiMate().orElseThrow());
        assertEquals(new MemberId(Integer.MAX_VALUE), restored.getPersonList().getFirst().getMemberId().orElseThrow());
        assertEquals(Integer.MAX_VALUE + 1L, restored.getNextMemberId());
        new JsonCoordiMateStorage(path).saveCoordiMate(restored);
        assertEquals(restored, new JsonCoordiMateStorage(path).readCoordiMate().orElseThrow());
        assertThrows(IllegalStateException.class, () ->
                restored.addPerson(new PersonBuilder(BENSON).withoutMemberId().build()));
    }

    @Test
    public void exhaustedIdRange_missingId_rejectsFileWithoutChangingIt() throws Exception {
        assertInvalidStore(storeJson(null, personJson(ALICE, "2147483647"), personJson(BENSON, null)),
                JsonSerializableCoordiMate.MESSAGE_MEMBER_IDS_EXHAUSTED);
    }

    private void assertInvalidStore(String json, String expectedMessage) throws Exception {
        Path path = writeStore(json);
        DataLoadingException error = assertThrows(DataLoadingException.class, () ->
                new JsonCoordiMateStorage(path).readCoordiMate(), json);
        Throwable cause = error;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        assertEquals(expectedMessage, cause.getMessage(), json);
        assertEquals(json, Files.readString(path));
    }

    private Path writeStore(String json) throws Exception {
        Path path = testFolder.resolve("members.json");
        Files.writeString(path, json);
        return path;
    }

    private static String storeJson(String nextId, String... persons) {
        return "{\"persons\":[" + String.join(",", persons) + "]"
                + (nextId == null ? "" : ",\"nextMemberId\":" + nextId) + "}";
    }

    private static String personJson(Person person, String id) throws IOException {
        ObjectNode record = JsonUtil.fromJsonString(JsonUtil.toJsonString(new JsonAdaptedPerson(person)),
                ObjectNode.class);
        if (id == null) {
            record.remove("memberId");
        } else {
            record.set("memberId", JsonUtil.fromJsonString(id, JsonNode.class));
        }
        return JsonUtil.toJsonString(record);
    }
}
