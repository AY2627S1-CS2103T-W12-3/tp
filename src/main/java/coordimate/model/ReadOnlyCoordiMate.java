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
     * Returns an unmodifiable view of the persons list.
     * This list will not contain any duplicate persons.
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
