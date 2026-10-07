package coordimate.model.person;

import static coordimate.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import coordimate.commons.util.ToStringBuilder;
import coordimate.model.tag.Tag;

/**
 * Represents a Person in the CoordiMate.
 * Guarantees: details are present and not null, field values are validated, immutable.
 */
public class Person {

    // Contact details used for duplicate checks
    private final Phone phone;
    private final Email email;

    // Data fields
    private final Name name;
    private final Role role;
    private final Birthday birthday;
    private final Address address;
    private final Organisation organisation;
    private final Note note;
    private final Set<Tag> tags = new HashSet<>();

    /**
     * Creates a person with the given non-null details and a defensive copy of the tags.
     */
    public Person(Name name, Phone phone, Email email, Address address, Set<Tag> tags) {
        this(name, phone, email, Role.NOT_APPLICABLE, address, tags);
    }

    /**
     * Creates a person with a required role and a defensive copy of the tags.
     */
    public Person(Name name, Phone phone, Email email, Role role, Address address, Set<Tag> tags) {
        this(name, phone, email, role, null, address, null, null, tags);
    }

    /**
     * Creates a person with optional birthday, organisation, and note details.
     */
    public Person(Name name, Phone phone, Email email, Role role, Birthday birthday, Address address,
            Organisation organisation, Note note, Set<Tag> tags) {
        requireAllNonNull(name, phone, email, role, tags);
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.role = role;
        this.birthday = birthday;
        this.address = address;
        this.organisation = organisation;
        this.note = note;
        this.tags.addAll(tags);
    }

    public Name getName() {
        return name;
    }

    public Phone getPhone() {
        return phone;
    }

    public Email getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public Optional<Birthday> getBirthday() {
        return Optional.ofNullable(birthday);
    }

    public Optional<Address> getAddress() {
        return Optional.ofNullable(address);
    }

    public Optional<Organisation> getOrganisation() {
        return Optional.ofNullable(organisation);
    }

    public Optional<Note> getNote() {
        return Optional.ofNullable(note);
    }

    /**
     * Returns an immutable tag set, which throws {@code UnsupportedOperationException}
     * if modification is attempted.
     */
    public Set<Tag> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    /**
     * Returns true if the names match ignoring case, or either the normalised phone or email matches.
     * Names are already trimmed when constructed.
     */
    public boolean isSamePerson(Person otherPerson) {
        if (otherPerson == this) {
            return true;
        }

        return otherPerson != null
                && (name.getFullName().equalsIgnoreCase(otherPerson.name.getFullName())
                || phone.getNormalizedValue().equals(otherPerson.phone.getNormalizedValue())
                || email.getNormalizedValue().equals(otherPerson.email.getNormalizedValue()));
    }

    /**
     * Returns true if both persons have the same identity and data fields.
     * This defines a stronger notion of equality between two persons.
     */
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Person otherPerson)) {
            return false;
        }

        return name.equals(otherPerson.name)
                && phone.equals(otherPerson.phone)
                && email.equals(otherPerson.email)
                && role.equals(otherPerson.role)
                && Objects.equals(birthday, otherPerson.birthday)
                && Objects.equals(address, otherPerson.address)
                && Objects.equals(organisation, otherPerson.organisation)
                && Objects.equals(note, otherPerson.note)
                && tags.equals(otherPerson.tags);
    }

    @Override
    public int hashCode() {
        // use this method for custom fields hashing instead of implementing your own
        return Objects.hash(name, phone, email, role, birthday, address, organisation, note, tags);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("name", name)
                .add("phone", phone)
                .add("email", email)
                .add("role", role)
                .add("birthday", birthday)
                .add("address", address)
                .add("organisation", organisation)
                .add("note", note)
                .add("tags", tags)
                .toString();
    }

}
