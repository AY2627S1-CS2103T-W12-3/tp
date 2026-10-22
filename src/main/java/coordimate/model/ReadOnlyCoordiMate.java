package coordimate.model;

import coordimate.model.event.Event;
import coordimate.model.person.Person;
import coordimate.model.tag.Tag;
import javafx.collections.ObservableList;

/**
 * Unmodifiable view of an CoordiMate.
 */
public interface ReadOnlyCoordiMate {

    /**
     * Returns the next ID to allocate, or {@code Integer.MAX_VALUE + 1L} when IDs are exhausted.
     * Deleted members' IDs remain below this value.
     */
    long getNextMemberId();

    /**
     * Returns an unmodifiable view of the persons list.
     * Every stored person has a unique member ID. This list will not contain duplicate persons.
     */
    ObservableList<Person> getPersonList();

    /**
     * Returns an unmodifiable view of the saved tags.
     * This list does not contain duplicate tag names.
     */
    ObservableList<Tag> getTagList();

    /**
     * Returns events in insertion order, with unique names and no mutable list access.
     */
    ObservableList<Event> getEventList();

}
