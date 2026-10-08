package coordimate.testutil;

import static coordimate.logic.parser.CliSyntax.PREFIX_ADDRESS;
import static coordimate.logic.parser.CliSyntax.PREFIX_BIRTHDAY;
import static coordimate.logic.parser.CliSyntax.PREFIX_EMAIL;
import static coordimate.logic.parser.CliSyntax.PREFIX_NAME;
import static coordimate.logic.parser.CliSyntax.PREFIX_NOTE;
import static coordimate.logic.parser.CliSyntax.PREFIX_ORGANISATION;
import static coordimate.logic.parser.CliSyntax.PREFIX_PHONE;
import static coordimate.logic.parser.CliSyntax.PREFIX_ROLE;
import static coordimate.logic.parser.CliSyntax.PREFIX_TAG;

import java.util.Set;

import coordimate.logic.commands.AddCommand;
import coordimate.logic.commands.EditCommand.EditPersonDescriptor;
import coordimate.model.person.Person;
import coordimate.model.tag.Tag;

/**
 * A utility class for Person.
 */
public class PersonUtil {

    /**
     * Returns an add command string for adding the {@code person}.
     */
    public static String getAddCommand(Person person) {
        return AddCommand.COMMAND_WORD + " " + getPersonDetails(person);
    }

    /**
     * Returns the part of command string for the given {@code person}'s details.
     */
    public static String getPersonDetails(Person person) {
        StringBuilder sb = new StringBuilder();
        sb.append(PREFIX_NAME + person.getName().getFullName() + " ");
        sb.append(PREFIX_PHONE + person.getPhone().getValue() + " ");
        sb.append(PREFIX_EMAIL + person.getEmail().getValue() + " ");
        sb.append(PREFIX_ROLE + person.getRole().getValue() + " ");
        person.getBirthday().ifPresent(birthday -> sb.append(PREFIX_BIRTHDAY).append(birthday.getValue()).append(" "));
        person.getAddress().ifPresent(address -> sb.append(PREFIX_ADDRESS).append(address.getValue()).append(" "));
        person.getOrganisation().ifPresent(organisation -> sb.append(PREFIX_ORGANISATION)
                .append(organisation.getValue()).append(" "));
        person.getNote().ifPresent(note -> sb.append(PREFIX_NOTE).append(note.getValue()).append(" "));
        person.getTags().stream().forEach(
                s -> sb.append(PREFIX_TAG + s.getTagName() + " ")
        );
        return sb.toString();
    }

    /**
     * Returns the part of command string for the given {@code EditPersonDescriptor}'s details.
     */
    public static String getEditPersonDescriptorDetails(EditPersonDescriptor descriptor) {
        StringBuilder sb = new StringBuilder();
        descriptor.getName().ifPresent(name -> sb.append(PREFIX_NAME).append(name.getFullName()).append(" "));
        descriptor.getPhone().ifPresent(phone -> sb.append(PREFIX_PHONE).append(phone.getValue()).append(" "));
        descriptor.getEmail().ifPresent(email -> sb.append(PREFIX_EMAIL).append(email.getValue()).append(" "));
        descriptor.getRole().ifPresent(role -> sb.append(PREFIX_ROLE).append(role.getValue()).append(" "));
        if (descriptor.isBirthdayEdited()) {
            sb.append(PREFIX_BIRTHDAY).append(descriptor.getBirthday().map(birthday -> birthday.getValue())
                    .orElse("")).append(" ");
        }
        if (descriptor.isAddressEdited()) {
            sb.append(PREFIX_ADDRESS).append(descriptor.getAddress().map(address -> address.getValue())
                    .orElse("")).append(" ");
        }
        if (descriptor.isOrganisationEdited()) {
            sb.append(PREFIX_ORGANISATION).append(descriptor.getOrganisation()
                    .map(organisation -> organisation.getValue()).orElse("")).append(" ");
        }
        if (descriptor.isNoteEdited()) {
            sb.append(PREFIX_NOTE).append(descriptor.getNote().map(note -> note.getValue())
                    .orElse("")).append(" ");
        }
        if (descriptor.getTags().isPresent()) {
            Set<Tag> tags = descriptor.getTags().get();
            if (tags.isEmpty()) {
                sb.append(PREFIX_TAG);
            } else {
                tags.forEach(s -> sb.append(PREFIX_TAG).append(s.getTagName()).append(" "));
            }
        }
        return sb.toString();
    }
}
