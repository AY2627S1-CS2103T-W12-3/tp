package coordimate.model.tag;

import static coordimate.commons.util.CollectionUtil.requireAllNonNull;
import static java.util.Objects.requireNonNull;

import java.util.Iterator;
import java.util.List;

import coordimate.model.tag.exceptions.DuplicateTagException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * A list of tags that enforces case-insensitive uniqueness and does not allow nulls.
 */
public class UniqueTagList implements Iterable<Tag> {

    private final ObservableList<Tag> internalTags = FXCollections.observableArrayList();
    private final ObservableList<Tag> unmodifiableTags = FXCollections.unmodifiableObservableList(internalTags);

    /**
     * Returns true if the list contains a tag with the same name, ignoring case.
     */
    public boolean contains(Tag toCheck) {
        requireNonNull(toCheck);
        return internalTags.stream().anyMatch(toCheck::isSameTag);
    }

    /**
     * Adds a tag to the list.
     * The tag must not already exist in the list, ignoring case.
     */
    public void add(Tag toAdd) {
        requireNonNull(toAdd);
        if (contains(toAdd)) {
            throw new DuplicateTagException();
        }
        internalTags.add(toAdd);
    }

    /**
     * Replaces the contents of this list with {@code tags}.
     * {@code tags} must not contain duplicate tag names, ignoring case.
     */
    public void setTags(List<Tag> tags) {
        requireAllNonNull(tags);
        if (!areTagsUnique(tags)) {
            throw new DuplicateTagException();
        }

        internalTags.setAll(tags);
    }

    /**
     * Replaces this list's contents with the contents of {@code replacement}.
     */
    public void setTags(UniqueTagList replacement) {
        requireNonNull(replacement);
        internalTags.setAll(replacement.internalTags);
    }

    /**
     * Returns the backing list as an unmodifiable {@code ObservableList}.
     */
    public ObservableList<Tag> asUnmodifiableObservableList() {
        return unmodifiableTags;
    }

    @Override
    public Iterator<Tag> iterator() {
        return internalTags.iterator();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof UniqueTagList otherUniqueTagList)) {
            return false;
        }

        return internalTags.equals(otherUniqueTagList.internalTags);
    }

    @Override
    public int hashCode() {
        return internalTags.hashCode();
    }

    @Override
    public String toString() {
        return internalTags.toString();
    }

    private boolean areTagsUnique(List<Tag> tags) {
        for (int i = 0; i < tags.size() - 1; i++) {
            for (int j = i + 1; j < tags.size(); j++) {
                if (tags.get(i).isSameTag(tags.get(j))) {
                    return false;
                }
            }
        }
        return true;
    }
}
