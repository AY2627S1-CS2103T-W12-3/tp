package coordimate.logic.commands;

import static coordimate.logic.parser.CliSyntax.PREFIX_ADDRESS;
import static coordimate.logic.parser.CliSyntax.PREFIX_BIRTHDAY;
import static coordimate.logic.parser.CliSyntax.PREFIX_EMAIL;
import static coordimate.logic.parser.CliSyntax.PREFIX_NAME;
import static coordimate.logic.parser.CliSyntax.PREFIX_NOTE;
import static coordimate.logic.parser.CliSyntax.PREFIX_ORGANISATION;
import static coordimate.logic.parser.CliSyntax.PREFIX_PHONE;
import static coordimate.logic.parser.CliSyntax.PREFIX_ROLE;
import static coordimate.logic.parser.CliSyntax.PREFIX_TAG;
import static coordimate.logic.parser.CliSyntax.PREFIX_TARGET;
import static coordimate.model.Model.PREDICATE_SHOW_ALL_PERSONS;
import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import coordimate.commons.core.index.Index;
import coordimate.commons.util.CollectionUtil;
import coordimate.commons.util.ToStringBuilder;
import coordimate.logic.Messages;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.Model;
import coordimate.model.event.MemberNameConflictException;
import coordimate.model.person.Address;
import coordimate.model.person.Birthday;
import coordimate.model.person.Email;
import coordimate.model.person.Name;
import coordimate.model.person.Note;
import coordimate.model.person.Organisation;
import coordimate.model.person.Person;
import coordimate.model.person.Phone;
import coordimate.model.person.Role;
import coordimate.model.tag.Tag;

/**
 * Edits the details of an existing person in the CoordiMate.
 */
public class EditCommand extends Command {

    public static final String COMMAND_WORD = "edit";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Edits the details of a saved contact. "
            + "Existing values will be overwritten by the input values.\n"
            + "Parameters: INDEX or " + PREFIX_TARGET + "IDENTIFIER "
            + "[" + PREFIX_NAME + "NAME] "
            + "[" + PREFIX_PHONE + "PHONE] "
            + "[" + PREFIX_EMAIL + "EMAIL] "
            + "[" + PREFIX_ROLE + "ROLE] "
            + "[" + PREFIX_BIRTHDAY + "BIRTHDAY] "
            + "[" + PREFIX_ADDRESS + "ADDRESS] "
            + "[" + PREFIX_ORGANISATION + "ORGANISATION] "
            + "[" + PREFIX_NOTE + "NOTE] "
            + "[" + PREFIX_TAG + "TAG]...\n"
            + "Example: " + COMMAND_WORD + " 1 "
            + PREFIX_PHONE + "91234567 "
            + PREFIX_EMAIL + "johndoe@example.com";

    public static final String MESSAGE_EDIT_PERSON_SUCCESS = "Edited person: %1$s";
    public static final String MESSAGE_NOT_EDITED =
            "Please provide at least one field or tag operation to edit. Example: edit 2 r/Logistics";
    public static final String MESSAGE_DUPLICATE_PERSON =
            "This update conflicts with another saved contact. No changes were made.";
    public static final String MESSAGE_MEMBER_NAME_CONFLICT =
            "Cannot rename this contact because an event already has a member with that name. No changes were made.";
    public static final String MESSAGE_NO_MATCH =
            "No contact matches that identifier. Use an exact saved name, phone number, or email address.";
    public static final String MESSAGE_AMBIGUOUS_MATCH = "Multiple contacts match that identifier:\n%1$s\n"
            + "Retry with a unique phone or email, or a displayed index if shown.";
    public static final String MESSAGE_INVALID_INDEX =
            "No contact exists at index %1$d. Please use an index from the current list.";

    private final Index index;
    private final String targetIdentifier;
    private final EditPersonDescriptor editPersonDescriptor;

    /**
     * Creates a command that edits the person at the given index using the supplied details.
     *
     * @param index of the person in the filtered person list to edit.
     * @param editPersonDescriptor details to edit the person with.
     */
    public EditCommand(Index index, EditPersonDescriptor editPersonDescriptor) {
        requireNonNull(index);
        requireNonNull(editPersonDescriptor);

        this.index = index;
        targetIdentifier = null;
        this.editPersonDescriptor = new EditPersonDescriptor(editPersonDescriptor);
    }

    /**
     * Creates a command that identifies a saved contact by exact name, phone, or email.
     */
    public EditCommand(String targetIdentifier, EditPersonDescriptor editPersonDescriptor) {
        requireNonNull(targetIdentifier);
        requireNonNull(editPersonDescriptor);
        if (targetIdentifier.isBlank()) {
            throw new IllegalArgumentException("Contact identifier must not be blank.");
        }

        index = null;
        this.targetIdentifier = targetIdentifier.strip();
        this.editPersonDescriptor = new EditPersonDescriptor(editPersonDescriptor);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        Person personToEdit = resolvePersonToEdit(model);
        Person editedPerson = createEditedPerson(personToEdit, editPersonDescriptor);

        boolean duplicatesAnotherPerson = model.getCoordiMate().getPersonList().stream()
                .anyMatch(person -> !person.equals(personToEdit) && person.isSamePerson(editedPerson));
        if (duplicatesAnotherPerson) {
            throw new CommandException(MESSAGE_DUPLICATE_PERSON);
        }

        try {
            model.setPerson(personToEdit, editedPerson);
        } catch (MemberNameConflictException e) {
            throw new CommandException(MESSAGE_MEMBER_NAME_CONFLICT, e);
        }
        model.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
        return new CommandResult(String.format(MESSAGE_EDIT_PERSON_SUCCESS, Messages.format(editedPerson)));
    }

    private Person resolvePersonToEdit(Model model) throws CommandException {
        List<Person> displayedPersons = model.getFilteredPersonList();
        if (index != null) {
            if (index.getZeroBased() >= displayedPersons.size()) {
                throw new CommandException(String.format(MESSAGE_INVALID_INDEX, index.getOneBased()));
            }
            return displayedPersons.get(index.getZeroBased());
        }
        return resolveTarget(targetIdentifier, model.getCoordiMate().getPersonList(), displayedPersons);
    }

    /**
     * Finds a contact across all saved contacts, independently of the displayed list.
     */
    static Person resolveTarget(String identifier, List<Person> savedPersons, List<Person> displayedPersons)
            throws CommandException {
        String trimmedIdentifier = identifier.strip();
        String normalizedPhone = trimmedIdentifier.replace(" ", "").replace("-", "")
                .replace("(", "").replace(")", "");
        List<Person> matches = savedPersons.stream().filter(person ->
                person.getName().getFullName().equalsIgnoreCase(trimmedIdentifier)
                || person.getEmail().getValue().equalsIgnoreCase(trimmedIdentifier)
                || person.getPhone().getNormalizedValue().equals(normalizedPhone)).toList();
        if (matches.isEmpty()) {
            throw new CommandException(MESSAGE_NO_MATCH);
        }
        if (matches.size() > 1) {
            String matchDetails = matches.stream().map(person -> formatMatch(person, displayedPersons))
                    .collect(Collectors.joining("\n"));
            throw new CommandException(String.format(MESSAGE_AMBIGUOUS_MATCH, matchDetails));
        }
        return matches.getFirst();
    }

    private static String formatMatch(Person person, List<Person> displayedPersons) {
        String details = "- " + person.getName() + " | Phone: " + person.getPhone()
                + " | Email: " + person.getEmail();
        int displayedIndex = displayedPersons.indexOf(person);
        return displayedIndex < 0 ? details : details + " | Current-list index: " + (displayedIndex + 1);
    }

    /**
     * Creates and returns a {@code Person} with the details of {@code personToEdit}
     * edited with {@code editPersonDescriptor}.
     */
    private static Person createEditedPerson(Person personToEdit, EditPersonDescriptor editPersonDescriptor) {
        assert personToEdit != null;

        Name updatedName = editPersonDescriptor.getName().orElse(personToEdit.getName());
        Phone updatedPhone = editPersonDescriptor.getPhone().orElse(personToEdit.getPhone());
        Email updatedEmail = editPersonDescriptor.getEmail().orElse(personToEdit.getEmail());
        Role updatedRole = editPersonDescriptor.getRole().orElse(personToEdit.getRole());
        Birthday updatedBirthday = editPersonDescriptor.isBirthdayEdited()
                ? editPersonDescriptor.getBirthday().orElse(null) : personToEdit.getBirthday().orElse(null);
        Address updatedAddress = editPersonDescriptor.isAddressEdited()
                ? editPersonDescriptor.getAddress().orElse(null) : personToEdit.getAddress().orElse(null);
        Organisation updatedOrganisation = editPersonDescriptor.isOrganisationEdited()
                ? editPersonDescriptor.getOrganisation().orElse(null) : personToEdit.getOrganisation().orElse(null);
        Note updatedNote = editPersonDescriptor.isNoteEdited()
                ? editPersonDescriptor.getNote().orElse(null) : personToEdit.getNote().orElse(null);
        Set<Tag> updatedTags = editPersonDescriptor.getTags().orElse(personToEdit.getTags());

        return new Person(updatedName, updatedPhone, updatedEmail, updatedRole,
                updatedBirthday, updatedAddress, updatedOrganisation, updatedNote, updatedTags);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof EditCommand otherEditCommand)) {
            return false;
        }

        return Objects.equals(index, otherEditCommand.index)
                && Objects.equals(targetIdentifier, otherEditCommand.targetIdentifier)
                && editPersonDescriptor.equals(otherEditCommand.editPersonDescriptor);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("index", index)
                .add("targetIdentifier", targetIdentifier)
                .add("editPersonDescriptor", editPersonDescriptor)
                .toString();
    }

    /**
     * Stores the details to edit the person with. Optional fields distinguish omission from clearing.
     */
    public static class EditPersonDescriptor {
        private Name name;
        private Phone phone;
        private Email email;
        private Role role;
        private Birthday birthday;
        private boolean isBirthdayEdited;
        private Address address;
        private boolean isAddressEdited;
        private Organisation organisation;
        private boolean isOrganisationEdited;
        private Note note;
        private boolean isNoteEdited;
        private Set<Tag> tags;

        /**
         * Creates a descriptor with no fields selected for editing.
         */
        public EditPersonDescriptor() {}

        /**
         * Creates a copy of the given edit descriptor.
         * A defensive copy of {@code tags} is used internally.
         */
        public EditPersonDescriptor(EditPersonDescriptor toCopy) {
            setName(toCopy.name);
            setPhone(toCopy.phone);
            setEmail(toCopy.email);
            setRole(toCopy.role);
            if (toCopy.isBirthdayEdited) {
                setBirthday(toCopy.birthday);
            }
            if (toCopy.isAddressEdited) {
                setAddress(toCopy.address);
            }
            if (toCopy.isOrganisationEdited) {
                setOrganisation(toCopy.organisation);
            }
            if (toCopy.isNoteEdited) {
                setNote(toCopy.note);
            }
            setTags(toCopy.tags);
        }

        /**
         * Returns true if at least one field is edited.
         */
        public boolean isAnyFieldEdited() {
            return CollectionUtil.isAnyNonNull(name, phone, email, role, tags)
                    || isBirthdayEdited || isAddressEdited || isOrganisationEdited || isNoteEdited;
        }

        public void setName(Name name) {
            this.name = name;
        }

        public Optional<Name> getName() {
            return Optional.ofNullable(name);
        }

        public void setPhone(Phone phone) {
            this.phone = phone;
        }

        public Optional<Phone> getPhone() {
            return Optional.ofNullable(phone);
        }

        public void setEmail(Email email) {
            this.email = email;
        }

        public Optional<Email> getEmail() {
            return Optional.ofNullable(email);
        }

        public void setRole(Role role) {
            this.role = role;
        }

        public Optional<Role> getRole() {
            return Optional.ofNullable(role);
        }

        public void setBirthday(Birthday birthday) {
            this.birthday = birthday;
            isBirthdayEdited = true;
        }

        public Optional<Birthday> getBirthday() {
            return Optional.ofNullable(birthday);
        }

        public boolean isBirthdayEdited() {
            return isBirthdayEdited;
        }

        public void setAddress(Address address) {
            this.address = address;
            isAddressEdited = true;
        }

        public Optional<Address> getAddress() {
            return Optional.ofNullable(address);
        }

        public boolean isAddressEdited() {
            return isAddressEdited;
        }

        public void setOrganisation(Organisation organisation) {
            this.organisation = organisation;
            isOrganisationEdited = true;
        }

        public Optional<Organisation> getOrganisation() {
            return Optional.ofNullable(organisation);
        }

        public boolean isOrganisationEdited() {
            return isOrganisationEdited;
        }

        public void setNote(Note note) {
            this.note = note;
            isNoteEdited = true;
        }

        public Optional<Note> getNote() {
            return Optional.ofNullable(note);
        }

        public boolean isNoteEdited() {
            return isNoteEdited;
        }

        /**
         * Sets {@code tags} to this object's {@code tags}.
         * A defensive copy of {@code tags} is used internally.
         */
        public void setTags(Set<Tag> tags) {
            this.tags = (tags != null) ? new HashSet<>(tags) : null;
        }

        /**
         * Returns an unmodifiable tag set, which throws {@code UnsupportedOperationException}
         * if modification is attempted.
         * Returns {@code Optional#empty()} if {@code tags} is null.
         */
        public Optional<Set<Tag>> getTags() {
            return (tags != null) ? Optional.of(Collections.unmodifiableSet(tags)) : Optional.empty();
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }

            // instanceof handles nulls
            if (!(other instanceof EditPersonDescriptor otherEditPersonDescriptor)) {
                return false;
            }

            return Objects.equals(name, otherEditPersonDescriptor.name)
                    && Objects.equals(phone, otherEditPersonDescriptor.phone)
                    && Objects.equals(email, otherEditPersonDescriptor.email)
                    && Objects.equals(role, otherEditPersonDescriptor.role)
                    && isBirthdayEdited == otherEditPersonDescriptor.isBirthdayEdited
                    && Objects.equals(birthday, otherEditPersonDescriptor.birthday)
                    && isAddressEdited == otherEditPersonDescriptor.isAddressEdited
                    && Objects.equals(address, otherEditPersonDescriptor.address)
                    && isOrganisationEdited == otherEditPersonDescriptor.isOrganisationEdited
                    && Objects.equals(organisation, otherEditPersonDescriptor.organisation)
                    && isNoteEdited == otherEditPersonDescriptor.isNoteEdited
                    && Objects.equals(note, otherEditPersonDescriptor.note)
                    && Objects.equals(tags, otherEditPersonDescriptor.tags);
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .add("name", name)
                    .add("phone", phone)
                    .add("email", email)
                    .add("role", role)
                    .add("birthdayEdited", isBirthdayEdited)
                    .add("birthday", birthday)
                    .add("addressEdited", isAddressEdited)
                    .add("address", address)
                    .add("organisationEdited", isOrganisationEdited)
                    .add("organisation", organisation)
                    .add("noteEdited", isNoteEdited)
                    .add("note", note)
                    .add("tags", tags)
                    .toString();
        }
    }
}
