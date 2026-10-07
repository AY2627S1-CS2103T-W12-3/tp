package coordimate.model;

import java.util.function.Predicate;

import coordimate.commons.core.GuiSettings;
import coordimate.model.event.Event;
import coordimate.model.person.Person;
import javafx.collections.ObservableList;

/**
 * The API of the Model component.
 */
public interface Model {
    /** {@code Predicate} that always evaluates to true. */
    Predicate<Person> PREDICATE_SHOW_ALL_PERSONS = unused -> true;

    /**
     * Returns the user prefs.
     */
    ReadOnlyUserPrefs getUserPrefs();

    /**
     * Returns the user prefs' GUI settings.
     */
    GuiSettings getGuiSettings();

    /**
     * Sets the user prefs' GUI settings.
     */
    void setGuiSettings(GuiSettings guiSettings);

    /**
     * Replaces CoordiMate data with the data in {@code coordiMate}.
     */
    void setCoordiMate(ReadOnlyCoordiMate coordiMate);

    /**
     * Returns the CoordiMate.
     */
    ReadOnlyCoordiMate getCoordiMate();

    /**
     * Checks for a duplicate event name, regardless of timings.
     */
    boolean hasEvent(Event event);

    /**
     * Adds an event whose name must not already be in use.
     */
    void addEvent(Event event);

    /**
     * Replaces an existing event without duplicating another event's name.
     */
    void setEvent(Event target, Event editedEvent);

    /**
     * Deletes an event that exists in the CoordiMate.
     */
    void deleteEvent(Event target);

    /**
     * Returns true if a person with the same identity as {@code person} exists in the CoordiMate.
     */
    boolean hasPerson(Person person);

    /**
     * Deletes the given person.
     * The person must exist in the CoordiMate.
     */
    void deletePerson(Person target);

    /**
     * Adds the given person.
     * {@code person} must not already exist in the CoordiMate.
     */
    void addPerson(Person person);

    /**
     * Replaces the given person {@code target} with {@code editedPerson}.
     * {@code target} must exist in the CoordiMate.
     * The person identity of {@code editedPerson} must not be the same as another existing person in the CoordiMate.
     */
    void setPerson(Person target, Person editedPerson);

    /**
     * Returns an unmodifiable view of the filtered person list.
     */
    ObservableList<Person> getFilteredPersonList();

    /**
     * Updates the filter of the filtered person list to filter by the given {@code predicate}.
     *
     * @throws NullPointerException if {@code predicate} is null.
     */
    void updateFilteredPersonList(Predicate<Person> predicate);
}
