package coordimate.model.person;

import static coordimate.commons.util.CollectionUtil.requireAllNonNull;
import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
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

    private final MemberId memberId;

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
    private final LeaveDate leaveDate;
    private final Set<Tag> tags = new HashSet<>();
    private final boolean archived;

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
     * Creates a person with optional birthday, organization, and note details, who is not on leave.
     */
    public Person(Name name, Phone phone, Email email, Role role, Birthday birthday, Address address,
            Organisation organisation, Note note, Set<Tag> tags) {
        this(name, phone, email, role, birthday, address, organisation, note, null, tags);
    }

    /**
     * Creates a person with optional birthday, organization, note, and leave end date details, but no ID.
     */
    public Person(Name name, Phone phone, Email email, Role role, Birthday birthday, Address address,
            Organisation organisation, Note note, LeaveDate leaveDate, Set<Tag> tags) {
        this(null, name, phone, email, role, birthday, address, organisation, note, leaveDate, tags, false);
    }

    /**
     * Creates a person with the given ID, or no ID for a contact not yet accepted by the model, and archive status.
     * Code that copies an existing contact must use this constructor, so that it passes the archive status on.
     */
    public Person(MemberId memberId, Name name, Phone phone, Email email, Role role, Birthday birthday, Address address,
            Organisation organisation, Note note, LeaveDate leaveDate, Set<Tag> tags, boolean archived) {
        requireAllNonNull(name, phone, email, role, tags);
        this.memberId = memberId;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.role = role;
        this.birthday = birthday;
        this.address = address;
        this.organisation = organisation;
        this.note = note;
        this.leaveDate = leaveDate;
        this.tags.addAll(tags);
        this.archived = archived;
    }

    /**
     * Returns the permanent ID, which is absent until the model accepts a new contact.
     */
    public Optional<MemberId> getMemberId() {
        return Optional.ofNullable(memberId);
    }

    /**
     * Returns an identified copy of a new contact, rejecting changes to an existing ID.
     */
    public Person withMemberId(MemberId id) {
        requireNonNull(id);
        if (memberId != null) {
            if (!memberId.equals(id)) {
                throw new IllegalArgumentException("A member's ID cannot be changed.");
            }
            return this;
        }
        return new Person(id, name, phone, email, role, birthday, address, organisation, note, leaveDate, tags,
                archived);
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
     * Returns the saved last day of leave, including a date that has already passed.
     */
    public Optional<LeaveDate> getLeaveDate() {
        return Optional.ofNullable(leaveDate);
    }

    /**
     * Returns true if the contact is on leave on {@code today}, that is, a leave end date is saved and
     * {@code today} is not after it.
     */
    public boolean isOnLeave(LocalDate today) {
        requireNonNull(today);
        return leaveDate != null && leaveDate.covers(today);
    }

    /**
     * Returns whether the contact is archived, which hides it from the contact list but keeps its details
     * and event history.
     */
    public boolean isArchived() {
        return archived;
    }

    /**
     * Returns a copy of this contact with the given archive status, preserving all other details.
     */
    public Person withArchived(boolean archived) {
        if (archived == this.archived) {
            return this;
        }
        return new Person(memberId, name, phone, email, role, birthday, address, organisation, note, leaveDate, tags,
                archived);
    }

    /**
     * Returns an immutable tag set, which throws {@code UnsupportedOperationException}
     * if modification is attempted.
     */
    public Set<Tag> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    /**
     * Returns true if the names match ignoring case, or either the normalized phone or email matches.
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

        return Objects.equals(memberId, otherPerson.memberId)
                && name.equals(otherPerson.name)
                && phone.equals(otherPerson.phone)
                && email.equals(otherPerson.email)
                && role.equals(otherPerson.role)
                && Objects.equals(birthday, otherPerson.birthday)
                && Objects.equals(address, otherPerson.address)
                && Objects.equals(organisation, otherPerson.organisation)
                && Objects.equals(note, otherPerson.note)
                && Objects.equals(leaveDate, otherPerson.leaveDate)
                && tags.equals(otherPerson.tags)
                && archived == otherPerson.archived;
    }

    @Override
    public int hashCode() {
        // use this method for custom fields hashing instead of implementing your own
        return Objects.hash(memberId, name, phone, email, role, birthday, address, organisation, note, leaveDate,
                tags, archived);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("memberId", memberId)
                .add("name", name)
                .add("phone", phone)
                .add("email", email)
                .add("role", role)
                .add("birthday", birthday)
                .add("address", address)
                .add("organisation", organisation)
                .add("note", note)
                .add("leaveDate", leaveDate)
                .add("tags", tags)
                .add("archived", archived)
                .toString();
    }

}
