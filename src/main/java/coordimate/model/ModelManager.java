package coordimate.model;

import static coordimate.commons.util.CollectionUtil.requireAllNonNull;
import static java.util.Objects.requireNonNull;

import java.util.function.Predicate;
import java.util.logging.Logger;

import coordimate.commons.core.GuiSettings;
import coordimate.commons.core.LogsCenter;
import coordimate.model.event.Event;
import coordimate.model.person.Person;
import coordimate.model.tag.Tag;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

/**
 * Represents the in-memory model of the CoordiMate data.
 */
public class ModelManager implements Model {
    private static final Logger logger = LogsCenter.getLogger(ModelManager.class);

    private final CoordiMate coordiMate;
    private final UserPrefs userPrefs;
    private final FilteredList<Person> filteredPersons;
    private final FilteredList<Event> filteredEvents;
    private Predicate<Person> personPredicate;
    private final SimpleBooleanProperty showingArchived = new SimpleBooleanProperty(false);

    /**
     * Initializes a ModelManager with the given coordiMate and userPrefs.
     */
    public ModelManager(ReadOnlyCoordiMate coordiMate, ReadOnlyUserPrefs userPrefs) {
        requireAllNonNull(coordiMate, userPrefs);

        logger.fine("Initializing with CoordiMate: " + coordiMate + " and user prefs " + userPrefs);

        this.coordiMate = new CoordiMate(coordiMate);
        this.userPrefs = new UserPrefs(userPrefs);
        filteredPersons = new FilteredList<>(this.coordiMate.getPersonList(), this::isShownPerson);
        filteredEvents = new FilteredList<>(this.coordiMate.getEventList(), this::isShownEvent);
    }

    /**
     * Creates a model with no contacts or events and default user preferences.
     */
    public ModelManager() {
        this(new CoordiMate(), new UserPrefs());
    }

    //=========== UserPrefs ==================================================================================

    @Override
    public ReadOnlyUserPrefs getUserPrefs() {
        return userPrefs;
    }

    @Override
    public GuiSettings getGuiSettings() {
        return userPrefs.getGuiSettings();
    }

    @Override
    public void setGuiSettings(GuiSettings guiSettings) {
        requireNonNull(guiSettings);
        userPrefs.setGuiSettings(guiSettings);
    }

    @Override
    public boolean hasSeenTutorial() {
        return userPrefs.hasSeenTutorial();
    }

    @Override
    public void setHasSeenTutorial(boolean hasSeenTutorial) {
        userPrefs.setHasSeenTutorial(hasSeenTutorial);
    }

    //=========== CoordiMate ================================================================================

    @Override
    public void setCoordiMate(ReadOnlyCoordiMate coordiMate) {
        this.coordiMate.resetData(coordiMate);
    }

    @Override
    public ReadOnlyCoordiMate getCoordiMate() {
        return coordiMate;
    }

    @Override
    public boolean hasEvent(Event event) {
        return coordiMate.hasEvent(event);
    }

    @Override
    public void addEvent(Event event) {
        coordiMate.addEvent(event);
    }

    @Override
    public ObservableList<Tag> getTagList() {
        return coordiMate.getTagList();
    }

    @Override
    public void deleteTag(Tag target) {
        requireNonNull(target);
        coordiMate.removeTag(target);
    }

    @Override
    public void registerTag(Tag tag) {
        requireNonNull(tag);
        if (!coordiMate.hasTag(tag)) {
            coordiMate.addTag(tag);
        }
    }

    @Override
    public boolean hasTag(Tag tag) {
        requireNonNull(tag);
        return coordiMate.hasTag(tag);
    }

    @Override
    public void addTag(Tag tag) {
        requireNonNull(tag);
        coordiMate.addTag(tag);
    }

    @Override
    public void setTag(Tag target, Tag editedTag) {
        requireAllNonNull(target, editedTag);
        coordiMate.setTag(target, editedTag);
    }

    @Override
    public void setEvent(Event target, Event editedEvent) {
        coordiMate.setEvent(target, editedEvent);
    }

    @Override
    public void deleteEvent(Event target) {
        coordiMate.removeEvent(target);
    }

    @Override
    public boolean hasPerson(Person person) {
        requireNonNull(person);
        return coordiMate.hasPerson(person);
    }

    @Override
    public void deletePerson(Person target) {
        coordiMate.removePerson(target);
    }

    @Override
    public void addPerson(Person person) {
        coordiMate.addPerson(person);
        // A new contact is active, so return to the active view to show it.
        setShowingArchived(false);
        updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
    }

    @Override
    public void setPerson(Person target, Person editedPerson) {
        requireAllNonNull(target, editedPerson);

        coordiMate.setPerson(target, editedPerson);
    }

    //=========== Filtered Person List Accessors =============================================================

    /**
     * Returns an unmodifiable view of the list of {@code Person} backed by the internal list of
     * {@code coordiMate}.
     */
    @Override
    public ObservableList<Person> getFilteredPersonList() {
        return filteredPersons;
    }

    @Override
    public void updateFilteredPersonList(Predicate<Person> predicate) {
        requireNonNull(predicate);
        personPredicate = predicate;
        filteredPersons.setPredicate(this::isShownPerson);
    }

    @Override
    public Predicate<? super Person> getFilteredPersonPredicate() {
        return personPredicate;
    }

    @Override
    public void setShowingArchived(boolean isShowingArchived) {
        showingArchived.set(isShowingArchived);
        filteredPersons.setPredicate(this::isShownPerson);
        filteredEvents.setPredicate(this::isShownEvent);
    }

    @Override
    public boolean isShowingArchived() {
        return showingArchived.get();
    }

    @Override
    public ReadOnlyBooleanProperty showingArchivedProperty() {
        return showingArchived;
    }

    /**
     * Shows only contacts in the current archive view that also match the latest filter, if any.
     */
    private boolean isShownPerson(Person person) {
        return person.isArchived() == isShowingArchived()
                && (personPredicate == null || personPredicate.test(person));
    }

    private boolean isShownEvent(Event event) {
        return event.isArchived() == isShowingArchived();
    }

    //=========== Filtered Event List Accessors ==============================================================

    @Override
    public ObservableList<Event> getFilteredEventList() {
        return filteredEvents;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof ModelManager otherModelManager)) {
            return false;
        }

        return coordiMate.equals(otherModelManager.coordiMate)
                && userPrefs.equals(otherModelManager.userPrefs)
                && isShowingArchived() == otherModelManager.isShowingArchived()
                && filteredPersons.equals(otherModelManager.filteredPersons);
    }

}
