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

    /**
     * Initializes a ModelManager with the given coordiMate and userPrefs.
     */
    public ModelManager(ReadOnlyCoordiMate coordiMate, ReadOnlyUserPrefs userPrefs) {
        requireAllNonNull(coordiMate, userPrefs);

        logger.fine("Initializing with CoordiMate: " + coordiMate + " and user prefs " + userPrefs);

        this.coordiMate = new CoordiMate(coordiMate);
        this.userPrefs = new UserPrefs(userPrefs);
        filteredPersons = new FilteredList<>(this.coordiMate.getPersonList());
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
        for (Tag tag : person.getTags()) {
            registerTag(tag);
        }
        updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
    }

    @Override
    public void setPerson(Person target, Person editedPerson) {
        requireAllNonNull(target, editedPerson);

        coordiMate.setPerson(target, editedPerson);
        for (Tag tag : editedPerson.getTags()) {
            registerTag(tag);
        }
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
        filteredPersons.setPredicate(predicate);
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
                && filteredPersons.equals(otherModelManager.filteredPersons);
    }

}
