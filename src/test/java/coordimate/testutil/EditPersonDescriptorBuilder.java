package coordimate.testutil;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import coordimate.logic.commands.EditCommand.EditPersonDescriptor;
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
 * A utility class to help with building EditPersonDescriptor objects.
 */
public class EditPersonDescriptorBuilder {

    private EditPersonDescriptor descriptor;

    public EditPersonDescriptorBuilder() {
        descriptor = new EditPersonDescriptor();
    }

    public EditPersonDescriptorBuilder(EditPersonDescriptor descriptor) {
        this.descriptor = new EditPersonDescriptor(descriptor);
    }

    /**
     * Returns an {@code EditPersonDescriptor} with fields containing {@code person}'s details.
     */
    public EditPersonDescriptorBuilder(Person person) {
        descriptor = new EditPersonDescriptor();
        descriptor.setName(person.getName());
        descriptor.setPhone(person.getPhone());
        descriptor.setEmail(person.getEmail());
        descriptor.setRole(person.getRole());
        person.getBirthday().ifPresent(descriptor::setBirthday);
        person.getAddress().ifPresent(descriptor::setAddress);
        person.getOrganisation().ifPresent(descriptor::setOrganisation);
        person.getNote().ifPresent(descriptor::setNote);
        descriptor.setTags(person.getTags());
    }

    /**
     * Sets the {@code Name} of the {@code EditPersonDescriptor} that we are building.
     */
    public EditPersonDescriptorBuilder withName(String name) {
        descriptor.setName(new Name(name));
        return this;
    }

    /**
     * Sets the {@code Phone} of the {@code EditPersonDescriptor} that we are building.
     */
    public EditPersonDescriptorBuilder withPhone(String phone) {
        descriptor.setPhone(new Phone(phone));
        return this;
    }

    /**
     * Sets the {@code Email} of the {@code EditPersonDescriptor} that we are building.
     */
    public EditPersonDescriptorBuilder withEmail(String email) {
        descriptor.setEmail(new Email(email));
        return this;
    }

    /**
     * Sets the {@code Address} of the {@code EditPersonDescriptor} that we are building.
     */
    public EditPersonDescriptorBuilder withAddress(String address) {
        descriptor.setAddress(new Address(address));
        return this;
    }

    /** Sets the role to edit. */
    public EditPersonDescriptorBuilder withRole(String role) {
        descriptor.setRole(new Role(role));
        return this;
    }

    /** Sets the birthday to edit. */
    public EditPersonDescriptorBuilder withBirthday(String birthday) {
        descriptor.setBirthday(new Birthday(birthday));
        return this;
    }

    /** Marks the birthday for clearing. */
    public EditPersonDescriptorBuilder withoutBirthday() {
        descriptor.setBirthday(null);
        return this;
    }

    /** Marks the address for clearing. */
    public EditPersonDescriptorBuilder withoutAddress() {
        descriptor.setAddress(null);
        return this;
    }

    /** Sets the organisation to edit. */
    public EditPersonDescriptorBuilder withOrganisation(String organisation) {
        descriptor.setOrganisation(new Organisation(organisation));
        return this;
    }

    /** Marks the organisation for clearing. */
    public EditPersonDescriptorBuilder withoutOrganisation() {
        descriptor.setOrganisation(null);
        return this;
    }

    /** Sets the note to edit. */
    public EditPersonDescriptorBuilder withNote(String note) {
        descriptor.setNote(new Note(note));
        return this;
    }

    /** Marks the note for clearing. */
    public EditPersonDescriptorBuilder withoutNote() {
        descriptor.setNote(null);
        return this;
    }

    /**
     * Parses the {@code tags} into a {@code Set<Tag>} and sets it to the {@code EditPersonDescriptor}
     * that we are building.
     */
    public EditPersonDescriptorBuilder withTags(String... tags) {
        Set<Tag> tagSet = Stream.of(tags).map(Tag::new).collect(Collectors.toSet());
        descriptor.setTags(tagSet);
        return this;
    }

    public EditPersonDescriptor build() {
        return descriptor;
    }
}
