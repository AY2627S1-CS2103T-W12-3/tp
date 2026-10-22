package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import coordimate.commons.util.ToStringBuilder;
import coordimate.logic.Messages;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.Model;
import coordimate.model.person.MemberId;
import coordimate.model.person.Person;

/** Deletes a contact identified by its permanent member ID or an exact saved detail. */
public class DeleteCommand extends Command {

    public static final String COMMAND_WORD = "delete";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Deletes a contact.\n"
            + "Parameters: MEMBER_ID | n/NAME | p/PHONE | e/EMAIL\n"
            + "Example: " + COMMAND_WORD + " n/Aisha Tan";
    public static final String MESSAGE_DELETE_PERSON_SUCCESS = "Contact deleted successfully: %s.";
    public static final String MESSAGE_UNKNOWN_MEMBER_ID = Messages.MESSAGE_MEMBER_ID_NOT_FOUND;
    public static final String MESSAGE_TARGET_CHANGED =
            "Contact ID: %d has changed since deletion was requested. Run delete again.";
    public static final String MESSAGE_NO_MATCH = Messages.MESSAGE_CONTACT_NO_MATCH;
    public static final String MESSAGE_AMBIGUOUS_MATCH = Messages.MESSAGE_CONTACT_AMBIGUOUS_MATCH;
    public static final String MESSAGE_SAVE_ERROR = "Contact could not be saved. No changes were made.";
    public static final String MESSAGE_LOAD_ERROR =
            Messages.MESSAGE_LOAD_ERROR;

    /** The detail used to identify a contact outside the displayed list. */
    public enum IdentifierType {
        NAME, PHONE, EMAIL
    }

    private final MemberId memberId;
    private final IdentifierType identifierType;
    private final String identifier;
    private final Person resolvedTarget;

    /** Creates a deletion request using a permanent member ID. */
    public DeleteCommand(MemberId memberId) {
        this.memberId = requireNonNull(memberId);
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
        memberId = null;
        resolvedTarget = null;
    }

    private DeleteCommand(Person resolvedTarget) {
        this.resolvedTarget = requireNonNull(resolvedTarget);
        memberId = null;
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
            MemberId confirmedId = resolvedTarget.getMemberId().orElseThrow();
            Person current = MemberIdResolver.resolve(model, confirmedId);
            if (!current.equals(resolvedTarget)) {
                throw new CommandException(String.format(MESSAGE_TARGET_CHANGED, confirmedId.getValue()));
            }
            return current;
        }
        if (memberId != null) {
            return MemberIdResolver.resolve(model, memberId);
        }
        return resolveMatches(model.getCoordiMate().getPersonList());
    }

    Person resolveMatches(List<Person> saved) throws CommandException {
        requireNonNull(saved);
        List<Person> matches = saved.stream()
                .filter(this::matchesIdentifier).toList();
        if (matches.isEmpty()) {
            throw new CommandException(MESSAGE_NO_MATCH);
        }
        if (matches.size() > 1) {
            String details = matches.stream().map(Messages::formatContactMatch)
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

    /** Formats the contact details and the confirmation prompt. */
    public static String confirmationPrompt(Person person) {
        requireNonNull(person);
        return "Contact found:\nID: " + person.getMemberId().orElseThrow() + "\nName: " + person.getName()
                + "\nPhone: " + person.getPhone()
                + "\nEmail: " + person.getEmail()
                + "\nRole: " + person.getRole()
                + "\nConfirm deletion? [y/N]";
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        Person personToDelete = resolvePerson(model);
        int deletedContactIndex = model.getFilteredPersonList().indexOf(personToDelete);
        model.deletePerson(personToDelete);
        assert !model.getCoordiMate().getPersonList().contains(personToDelete)
                : "Deleted contact must not remain in the model.";
        String feedback = String.format(MESSAGE_DELETE_PERSON_SUCCESS, personToDelete.getName());
        return deletedContactIndex < 0
                ? new CommandResult(feedback, CommandResult.View.CONTACTS)
                : CommandResult.withContactIndexToFocus(feedback, CommandResult.View.CONTACTS, deletedContactIndex);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof DeleteCommand command
                && Objects.equals(memberId, command.memberId)
                && identifierType == command.identifierType
                && Objects.equals(identifier, command.identifier)
                && Objects.equals(resolvedTarget, command.resolvedTarget);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memberId, identifierType, identifier, resolvedTarget);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("memberId", memberId)
                .add("identifierType", identifierType)
                .add("identifier", identifier)
                .add("resolvedTarget", resolvedTarget)
                .toString();
    }
}
