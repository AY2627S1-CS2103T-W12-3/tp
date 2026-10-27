package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.Model;
import coordimate.model.person.MemberId;
import coordimate.model.person.Person;

/**
 * Archives or unarchives the contact with a permanent member ID, whether or not it is currently displayed.
 * An archived contact keeps its details and event history, but is hidden from the contact list.
 */
public class ArchiveCommand extends Command {
    public static final String COMMAND_WORD = "archive";
    public static final String UNARCHIVE_COMMAND_WORD = "unarchive";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Archives a contact, hiding it from the "
            + "contact list without deleting it.\n"
            + "Parameters: MEMBER_ID (the contact's permanent member ID)\n"
            + "Example: " + COMMAND_WORD + " 2";
    public static final String MESSAGE_UNARCHIVE_USAGE = UNARCHIVE_COMMAND_WORD + ": Restores an archived "
            + "contact to the contact list.\n"
            + "Parameters: MEMBER_ID (the contact's permanent member ID)\n"
            + "Example: " + UNARCHIVE_COMMAND_WORD + " 2";
    public static final String MESSAGE_ARCHIVE_SUCCESS = "Archived contact: %s.";
    public static final String MESSAGE_UNARCHIVE_SUCCESS = "Unarchived contact: %s.";
    public static final String MESSAGE_ALREADY_ARCHIVED = "%s is already archived. No changes were made.";
    public static final String MESSAGE_NOT_ARCHIVED = "%s is not archived. No changes were made.";
    public static final String MESSAGE_SAVE_ERROR = "Contact archive status could not be saved. "
            + "No changes were made.";

    private final MemberId memberId;
    private final boolean archive;

    /**
     * Creates a command that archives the contact with {@code memberId} if {@code archive} is true,
     * or unarchives it otherwise.
     */
    public ArchiveCommand(MemberId memberId, boolean archive) {
        this.memberId = requireNonNull(memberId);
        this.archive = archive;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        Person target = MemberIdResolver.resolve(model, memberId);
        if (target.isArchived() == archive) {
            throw new CommandException(String.format(archive ? MESSAGE_ALREADY_ARCHIVED : MESSAGE_NOT_ARCHIVED,
                    target.getName()));
        }
        model.setPerson(target, target.withArchived(archive));
        return new CommandResult(String.format(archive ? MESSAGE_ARCHIVE_SUCCESS : MESSAGE_UNARCHIVE_SUCCESS,
                target.getName()), CommandResult.View.CONTACTS);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ArchiveCommand otherCommand && memberId.equals(otherCommand.memberId)
                && archive == otherCommand.archive;
    }

    @Override
    public int hashCode() {
        return memberId.hashCode() * 31 + Boolean.hashCode(archive);
    }
}
