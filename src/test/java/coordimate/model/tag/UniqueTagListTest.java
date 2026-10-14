package coordimate.model.tag;

import static coordimate.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import coordimate.model.tag.exceptions.DuplicateTagException;

public class UniqueTagListTest {

    private static final Tag EXCO = new Tag("EXCO");
    private static final Tag SPONSOR = new Tag("Sponsor");

    private final UniqueTagList uniqueTagList = new UniqueTagList();

    @Test
    public void contains_nullTag_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> uniqueTagList.contains(null));
    }

    @Test
    public void contains_tagWithSameNameIgnoringCase_returnsTrue() {
        uniqueTagList.add(EXCO);

        assertTrue(uniqueTagList.contains(new Tag("exco")));
        assertFalse(uniqueTagList.contains(SPONSOR));
    }

    @Test
    public void add_nullTag_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> uniqueTagList.add(null));
    }

    @Test
    public void add_duplicateTagIgnoringCase_throwsDuplicateTagException() {
        uniqueTagList.add(EXCO);

        assertThrows(DuplicateTagException.class, () -> uniqueTagList.add(new Tag("exco")));
    }

    @Test
    public void setTags_nullList_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> uniqueTagList.setTags((List<Tag>) null));
    }

    @Test
    public void setTags_listContainingNull_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> uniqueTagList.setTags(Arrays.asList(EXCO, null)));
    }

    @Test
    public void setTags_listContainingDuplicateNames_throwsDuplicateTagException() {
        List<Tag> tags = List.of(EXCO, new Tag("exco"));

        assertThrows(DuplicateTagException.class, () -> uniqueTagList.setTags(tags));
    }

    @Test
    public void setTags_validList_replacesTags() {
        uniqueTagList.add(EXCO);
        uniqueTagList.setTags(List.of(SPONSOR));

        assertEquals(List.of(SPONSOR), uniqueTagList.asUnmodifiableObservableList());
    }

    @Test
    public void setTags_uniqueTagList_replacesTags() {
        UniqueTagList replacement = new UniqueTagList();
        replacement.add(SPONSOR);

        uniqueTagList.setTags(replacement);

        assertEquals(replacement, uniqueTagList);
    }

    @Test
    public void setTags_nullUniqueTagList_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> uniqueTagList.setTags((UniqueTagList) null));
    }

    @Test
    public void getTagList_modifyList_throwsUnsupportedOperationException() {
        uniqueTagList.add(EXCO);

        assertThrows(UnsupportedOperationException.class, () -> uniqueTagList.asUnmodifiableObservableList().remove(0));
    }

    @Test
    public void iterator_iteratesOverTags() {
        uniqueTagList.setTags(List.of(EXCO, SPONSOR));

        assertEquals(List.of(EXCO, SPONSOR),
                java.util.stream.StreamSupport.stream(uniqueTagList.spliterator(), false).toList());
    }

    @Test
    public void equals() {
        UniqueTagList copy = new UniqueTagList();

        assertEquals(uniqueTagList, uniqueTagList);
        assertEquals(uniqueTagList, copy);
        assertNotEquals(uniqueTagList, new Object());

        copy.add(EXCO);
        assertNotEquals(uniqueTagList, copy);
    }

    @Test
    public void hashCodeAndToString_sameTags_match() {
        uniqueTagList.setTags(List.of(EXCO, SPONSOR));
        UniqueTagList copy = new UniqueTagList();
        copy.setTags(List.of(EXCO, SPONSOR));

        assertEquals(copy.hashCode(), uniqueTagList.hashCode());
        assertEquals(List.of(EXCO, SPONSOR).toString(), uniqueTagList.toString());
    }
}
