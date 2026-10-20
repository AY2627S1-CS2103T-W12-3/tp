package coordimate.model.person;

import static coordimate.commons.util.CollectionUtil.requireAllNonNull;
import static java.util.Objects.requireNonNull;

import java.util.Iterator;
import java.util.List;

import coordimate.model.person.exceptions.DuplicateMemberIdException;
import coordimate.model.person.exceptions.DuplicatePersonException;
import coordimate.model.person.exceptions.PersonNotFoundException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * A list of persons that enforces uniqueness between its elements and does not allow nulls.
 * Contacts must have distinct names (ignoring case), normalized phone numbers, and email addresses.
 * Assigned member IDs must also be unique; contacts awaiting allocation may have no ID.
 * Removal uses {@code Person#equals(Object)} so the exact contact is removed.
 *
 * Supports a minimal set of list operations.
 *
 * @see Person#isSamePerson(Person)
 */
public class UniquePersonList implements Iterable<Person> {

    private final ObservableList<Person> internalPersons = FXCollections.observableArrayList();
    private final ObservableList<Person> unmodifiablePersons =
            FXCollections.unmodifiableObservableList(internalPersons);

    /**
     * Returns true if the list contains an equivalent person as the given argument.
     */
    public boolean contains(Person toCheck) {
        requireNonNull(toCheck);
        return internalPersons.stream().anyMatch(toCheck::isSamePerson);
    }

    /**
     * Adds a person to the list.
     * The person must not already exist in the list.
     */
    public void add(Person toAdd) {
        requireNonNull(toAdd);
        if (contains(toAdd)) {
            throw new DuplicatePersonException();
        }
        if (internalPersons.stream().anyMatch(person -> hasSameMemberId(person, toAdd))) {
            throw new DuplicateMemberIdException();
        }
        internalPersons.add(toAdd);
    }

    /**
     * Replaces the person {@code target} in the list with {@code editedPerson}.
     * {@code target} must exist in the list.
     * The person identity of {@code editedPerson} must not be the same as another existing person in the list.
     */
    public void setPerson(Person target, Person editedPerson) {
        requireAllNonNull(target, editedPerson);

        int index = internalPersons.indexOf(target);
        if (index == -1) {
            throw new PersonNotFoundException();
        }

        for (int i = 0; i < internalPersons.size(); i++) {
            if (i != index && internalPersons.get(i).isSamePerson(editedPerson)) {
                throw new DuplicatePersonException();
            }
            if (i != index && hasSameMemberId(internalPersons.get(i), editedPerson)) {
                throw new DuplicateMemberIdException();
            }
        }

        if (target.getMemberId().isPresent()) {
            editedPerson = editedPerson.withMemberId(target.getMemberId().orElseThrow());
        }
        internalPersons.set(index, editedPerson);
    }

    /**
     * Removes the equivalent person from the list.
     * The person must exist in the list.
     */
    public void remove(Person toRemove) {
        requireNonNull(toRemove);
        if (!internalPersons.remove(toRemove)) {
            throw new PersonNotFoundException();
        }
    }

    /**
     * Replaces this list's contents with the contents of the given unique person list.
     */
    public void setPersons(UniquePersonList replacement) {
        requireNonNull(replacement);
        internalPersons.setAll(replacement.internalPersons);
    }

    /**
     * Replaces the contents of this list with {@code persons}.
     * {@code persons} must not contain duplicate persons.
     */
    public void setPersons(List<Person> persons) {
        requireAllNonNull(persons);
        if (!arePersonsUnique(persons)) {
            throw new DuplicatePersonException();
        }
        for (int i = 0; i < persons.size(); i++) {
            for (int j = i + 1; j < persons.size(); j++) {
                if (hasSameMemberId(persons.get(i), persons.get(j))) {
                    throw new DuplicateMemberIdException();
                }
            }
        }

        internalPersons.setAll(persons);
    }

    /**
     * Returns the backing list as an unmodifiable {@code ObservableList}.
     */
    public ObservableList<Person> asUnmodifiableObservableList() {
        return unmodifiablePersons;
    }

    @Override
    public Iterator<Person> iterator() {
        return internalPersons.iterator();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof UniquePersonList otherUniquePersonList)) {
            return false;
        }

        return internalPersons.equals(otherUniquePersonList.internalPersons);
    }

    @Override
    public int hashCode() {
        return internalPersons.hashCode();
    }

    @Override
    public String toString() {
        return internalPersons.toString();
    }

    /**
     * Returns true if {@code persons} contains only unique persons.
     */
    private boolean arePersonsUnique(List<Person> persons) {
        for (int i = 0; i < persons.size() - 1; i++) {
            for (int j = i + 1; j < persons.size(); j++) {
                if (persons.get(i).isSamePerson(persons.get(j))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean hasSameMemberId(Person first, Person second) {
        return first.getMemberId().isPresent() && first.getMemberId().equals(second.getMemberId());
    }
}
