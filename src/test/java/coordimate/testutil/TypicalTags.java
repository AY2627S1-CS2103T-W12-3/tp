package coordimate.testutil;

import java.util.List;
import java.util.stream.Stream;

import coordimate.model.tag.Tag;

/**
 * A utility class containing {@code Tag} objects to be used in tests.
 */
public class TypicalTags {
    /** Tags that every new CoordiMate starts with, in display order. */
    public static final List<Tag> DEFAULT_TAGS = List.of(new Tag("EXCO"), new Tag("Sponsor"),
            new Tag("UniversityStaff"), new Tag("Logistics"));

    private TypicalTags() {} // prevents instantiation

    /**
     * Returns the default tags followed by {@code extraTags}.
     */
    public static List<Tag> defaultTagsWith(Tag... extraTags) {
        return Stream.concat(DEFAULT_TAGS.stream(), Stream.of(extraTags)).toList();
    }
}
