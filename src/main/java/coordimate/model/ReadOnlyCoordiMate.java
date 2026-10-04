package coordimate.model;

import coordimate.model.person.Person;
import javafx.collections.ObservableList;

/**
 * Unmodifiable view of an CoordiMate
 */
public interface ReadOnlyCoordiMate {

    /**
     * Returns an unmodifiable view of the persons list.
     * This list will not contain any duplicate persons.
     */
    ObservableList<Person> getPersonList();

}
