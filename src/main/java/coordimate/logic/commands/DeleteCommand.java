package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import coordimate.commons.core.index.Index;
import coordimate.commons.util.ToStringBuilder;
import coordimate.logic.Messages;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.Model;
import coordimate.model.person.Person;

/** Deletes a contact identified by its displayed index or an exact saved detail. */
public class DeleteCommand extends Command {

    public static final String COMMAND_WORD = "delete";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Deletes a contact.\n"
            + "Parameters: INDEX | n/NAME | p/PHONE | e/EMAIL\n"
            + "Example: " + COMMAND_WORD + " n/Aisha Tan";
    public static final String MESSAGE_DELETE_PERSON_SUCCESS = "Contact deleted successfully: %s.";
    public static final String MESSAGE_INVALID_INDEX = Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX;
    public static final String MESSAGE_NO_MATCH = Messages.MESSAGE_CONTACT_NO_MATCH;
    public static final String MESSAGE_AMBIGUOUS_MATCH = Messages.MESSAGE_CONTACT_AMBIGUOUS_MATCH;
    public static final String MESSAGE_SAVE_ERROR = "Contact could not be saved. No changes were made.";
    public static final String MESSAGE_LOAD_ERROR =
            Messages.MESSAGE_LOAD_ERROR;

    /** The detail used to identify a contact outside the displayed list. */
    public enum IdentifierType {
        NAME, PHONE, EMAIL
    }

    private final Index targetIndex;
    private final IdentifierType identifierType;
    private final String identifier;
    private final Person resolvedTarget;

    /** Creates an index-based deletion request. */
    public DeleteCommand(Index targetIndex) {
        this.targetIndex = requireNonNull(targetIndex);
        identifierType = null;
        identifier = null;
        resolvedTarget = null;
    }

    /** Creates a deletion request using a saved name, phone number, or email address. */
    public DeleteCommand(IdentifierType identifierType, String identifier) {
        this.identifierType = requireNonNull(identifierType);
        this.identifier = requireNonNull(identifier).strip();
        if (this.identifier.isEmpty()) {
            throw new IllegalArgumentException("Contact identifier must not be empty.");
        }
        targetIndex = null;
        resolvedTarget = null;
    }

    private DeleteCommand(Person resolvedTarget) {
        this.resolvedTarget = requireNonNull(resolvedTarget);
        targetIndex = null;
        identifierType = null;
        identifier = null;
    }

    /** Creates a confirmed request for exactly the contact shown in the confirmation prompt. */
    public static DeleteCommand forResolvedPerson(Person person) {
        return new DeleteCommand(person);
    }

    /** Resolves a single saved contact without changing the model. */
    public Person resolvePerson(Model model) throws CommandException {
        requireNonNull(model);
        if (resolvedTarget != null) {
            return model.getCoordiMate().getPersonList().stream().filter(resolvedTarget::equals).findFirst()
                    .orElseThrow(() -> new CommandException(MESSAGE_NO_MATCH));
        }
        if (targetIndex != null) {
            List<Person> displayed = model.getFilteredPersonList();
            if (targetIndex.getZeroBased() >= displayed.size()) {
                throw new CommandException(String.format(MESSAGE_INVALID_INDEX, targetIndex.getOneBased()));
            }
            return displayed.get(targetIndex.getZeroBased());
        }
        return resolveMatches(model.getCoordiMate().getPersonList(), model.getFilteredPersonList());
    }

    Person resolveMatches(List<Person> saved, List<Person> displayed) throws CommandException {
        requireNonNull(saved);
        requireNonNull(displayed);
        List<Person> matches = saved.stream()
                .filter(this::matchesIdentifier).toList();
        if (matches.isEmpty()) {
            throw new CommandException(MESSAGE_NO_MATCH);
        }
        if (matches.size() > 1) {
            String details = matches.stream().map(person -> formatMatch(person, displayed))
                    .collect(Collectors.joining("\n"));
            throw new CommandException(String.format(MESSAGE_AMBIGUOUS_MATCH, details));
        }
        return matches.getFirst();
    }

    private boolean matchesIdentifier(Person person) {
        return switch (identifierType) {
            case NAME -> person.getName().getFullName().equalsIgnoreCase(identifier);
            case PHONE -> person.getPhone().getNormalizedValue().equals(normalizePhone(identifier));
            case EMAIL -> person.getEmail().getValue().equalsIgnoreCase(identifier);
        };
    }

    private static String normalizePhone(String phone) {
        return phone.replace(" ", "").replace("-", "").replace("(", "").replace(")", "");
    }

    private static String formatMatch(Person person, List<Person> displayed) {
        String details = "- " + person.getName() + " | Phone: " + person.getPhone()
                + " | Email: " + person.getEmail();
        int index = displayed.indexOf(person);
        return index < 0 ? details : details + " | Current-list index: " + (index + 1);
    }

    /** Formats the contact details and the confirmation prompt. */
    public static String confirmationPrompt(Person person) {
        requireNonNull(person);
        return "Contact found:\nName: " + person.getName()
                + "\nPhone: " + person.getPhone()
                + "\nEmail: " + person.getEmail()
                + "\nRole: " + person.getRole()
                + "\nConfirm deletion? [y/N]";
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        Person personToDelete = resolvePerson(model);
        model.deletePerson(personToDelete);
        assert !model.getCoordiMate().getPersonList().contains(personToDelete)
                : "Deleted contact must not remain in the model.";
        return new CommandResult(String.format(MESSAGE_DELETE_PERSON_SUCCESS, personToDelete.getName()));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof DeleteCommand command
                && Objects.equals(targetIndex, command.targetIndex)
                && identifierType == command.identifierType
                && Objects.equals(identifier, command.identifier)
                && Objects.equals(resolvedTarget, command.resolvedTarget);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetIndex, identifierType, identifier, resolvedTarget);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("targetIndex", targetIndex)
                .add("identifierType", identifierType)
                .add("identifier", identifier)
                .add("resolvedTarget", resolvedTarget)
                .toString();
    }
}
