package coordimate.model.person;

import static java.util.Objects.requireNonNull;

import java.util.Locale;
import java.util.function.Predicate;

import coordimate.commons.util.ToStringBuilder;

/**
 * Tests that a {@code Person} has the tag with the given name, ignoring case.
 * The tag is identified by its name, so a filter that shows it must be given the new name after a rename.
 */
public class TagFilterPredicate implements Predicate<Person> {
    private final String tagName;

    /**
     * Creates a predicate for contacts that have the tag named {@code tagName}, ignoring case.
     */
    public TagFilterPredicate(String tagName) {
        this.tagName = requireNonNull(tagName);
    }

    public String getTagName() {
        return tagName;
    }

    /**
     * Returns a predicate for the tag named {@code newName}, for following a renamed tag.
     */
    public TagFilterPredicate withTagName(String newName) {
        return new TagFilterPredicate(newName);
    }

    @Override
    public boolean test(Person person) {
        return person.getTags().stream().anyMatch(tag -> tag.getTagName().equalsIgnoreCase(tagName));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TagFilterPredicate otherPredicate && tagName.equalsIgnoreCase(otherPredicate.tagName);
    }

    @Override
    public int hashCode() {
        return tagName.toLowerCase(Locale.ROOT).hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("tagName", tagName).toString();
    }
}
