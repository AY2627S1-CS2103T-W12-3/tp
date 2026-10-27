package coordimate.logic.commands;

import static coordimate.logic.parser.CliSyntax.PREFIX_ADDRESS;
import static coordimate.logic.parser.CliSyntax.PREFIX_BIRTHDAY;
import static coordimate.logic.parser.CliSyntax.PREFIX_EMAIL;
import static coordimate.logic.parser.CliSyntax.PREFIX_LEAVE;
import static coordimate.logic.parser.CliSyntax.PREFIX_NAME;
import static coordimate.logic.parser.CliSyntax.PREFIX_NOTE;
import static coordimate.logic.parser.CliSyntax.PREFIX_ORGANISATION;
import static coordimate.logic.parser.CliSyntax.PREFIX_PHONE;
import static coordimate.logic.parser.CliSyntax.PREFIX_ROLE;
import static coordimate.logic.parser.CliSyntax.PREFIX_TAG;
import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import coordimate.commons.core.LogsCenter;
import coordimate.commons.util.ToStringBuilder;
import coordimate.logic.Messages;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.Model;
import coordimate.model.person.Person;
import coordimate.model.tag.Tag;

/**
 * Adds a person to the CoordiMate.
 */
public class AddCommand extends Command {

    public static final String COMMAND_WORD = "add";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Saves a contact in CoordiMate.\n"
            + "Parameters: "
            + PREFIX_NAME + "NAME "
            + PREFIX_PHONE + "PHONE "
            + PREFIX_EMAIL + "EMAIL "
            + PREFIX_ROLE + "ROLE "
            + "[" + PREFIX_BIRTHDAY + "BIRTHDAY] "
            + "[" + PREFIX_ADDRESS + "ADDRESS] "
            + "[" + PREFIX_ORGANISATION + "ORGANISATION] "
            + "[" + PREFIX_LEAVE + "LEAVE_END_DATE] "
            + "[" + PREFIX_TAG + "TAG]... "
            + "[" + PREFIX_NOTE + "NOTE]\n"
            + "Example: " + COMMAND_WORD + " "
            + PREFIX_NAME + "Aisha Tan "
            + PREFIX_PHONE + "+6591234567 "
            + PREFIX_EMAIL + "aisha@example.com "
            + PREFIX_ROLE + "Logistics "
            + PREFIX_TAG + "EXCO "
            + PREFIX_NOTE + "Handles venue bookings";

    public static final String MESSAGE_SUCCESS = "Contact saved successfully: %1$s";
    public static final String MESSAGE_DUPLICATE_PERSON = "A contact with this name, phone number, or email "
            + "already exists. No changes were made.";
    public static final String MESSAGE_SAVE_ERROR = "Contact could not be saved. No changes were made.";
    public static final String MESSAGE_NO_MORE_MEMBER_IDS = "No more member IDs are available.";
    public static final String MESSAGE_LOAD_ERROR =
            Messages.MESSAGE_LOAD_ERROR;

    private static final Logger logger = LogsCenter.getLogger(AddCommand.class);

    private final Person toAdd;

    /**
     * Creates an AddCommand to add the specified {@code Person}.
     */
    public AddCommand(Person person) {
        requireNonNull(person);
        toAdd = person;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);

        if (model.hasPerson(toAdd)) {
            throw new CommandException(MESSAGE_DUPLICATE_PERSON);
        }

        Person personToAdd = withSavedTags(toAdd, model.getTagList());
        try {
            model.addPerson(personToAdd);
        } catch (IllegalStateException e) {
            throw new CommandException(MESSAGE_NO_MORE_MEMBER_IDS, e);
        }
        assert model.hasPerson(personToAdd) : "Added contact must be present in the model.";
        Person savedPerson = model.getFilteredPersonList().stream()
                .filter(personToAdd::isSamePerson)
                .findFirst()
                .orElseThrow();
        toAdd.getLeaveDate().ifPresent(leaveDate ->
                logger.info("Added contact " + toAdd.getName() + " on leave until " + leaveDate));
        return CommandResult.withContactToFocus(String.format(MESSAGE_SUCCESS, toAdd.getName()),
                CommandResult.View.CONTACTS, savedPerson);
    }

    /**
     * Returns a copy of {@code person} whose tags use the capitalization of matching saved tags.
     * Tags with no saved match keep their typed capitalization.
     */
    private static Person withSavedTags(Person person, List<Tag> savedTags) {
        Set<Tag> canonicalTags = person.getTags().stream()
                .map(tag -> tag.findSavedTag(savedTags))
                .collect(Collectors.toSet());
        if (canonicalTags.equals(person.getTags())) {
            return person;
        }
        logger.fine("Using saved tag capitalization for new contact " + person.getName() + ": " + canonicalTags);
        return new Person(person.getMemberId().orElse(null), person.getName(), person.getPhone(), person.getEmail(),
                person.getRole(),
                person.getBirthday().orElse(null), person.getAddress().orElse(null),
                person.getOrganisation().orElse(null), person.getNote().orElse(null),
                person.getLeaveDate().orElse(null), canonicalTags, person.isArchived());
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof AddCommand otherAddCommand)) {
            return false;
        }

        return toAdd.equals(otherAddCommand.toAdd);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("toAdd", toAdd)
                .toString();
    }
}
