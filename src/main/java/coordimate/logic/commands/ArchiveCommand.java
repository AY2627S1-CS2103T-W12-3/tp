package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.List;

import coordimate.commons.core.index.Index;
import coordimate.logic.Messages;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.Model;
import coordimate.model.person.Person;

/**
 * Archives or unarchives the contact at an index in the displayed contact list.
 * An archived contact keeps its details and event history, but is hidden from the contact list.
 */
public class ArchiveCommand extends Command {
    public static final String COMMAND_WORD = "archive";
    public static final String UNARCHIVE_COMMAND_WORD = "unarchive";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Archives a contact, hiding it from the "
            + "contact list without deleting it.\n"
            + "Parameters: INDEX (a positive integer from the displayed contact list)\n"
            + "Example: " + COMMAND_WORD + " 2";
    public static final String MESSAGE_UNARCHIVE_USAGE = UNARCHIVE_COMMAND_WORD + ": Restores an archived "
            + "contact to the contact list.\n"
            + "Parameters: INDEX (a positive integer from the displayed archived contact list)\n"
            + "Example: " + UNARCHIVE_COMMAND_WORD + " 1";
    public static final String MESSAGE_ARCHIVE_SUCCESS = "Archived contact: %s.";
    public static final String MESSAGE_UNARCHIVE_SUCCESS = "Unarchived contact: %s.";
    public static final String MESSAGE_ALREADY_ARCHIVED = "%s is already archived. No changes were made.";
    public static final String MESSAGE_NOT_ARCHIVED = "%s is not archived. No changes were made.";
    public static final String MESSAGE_INVALID_INDEX = Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX;
    public static final String MESSAGE_SAVE_ERROR = "Contact archive status could not be saved. "
            + "No changes were made.";

    private final Index targetIndex;
    private final boolean archive;

    /**
     * Creates a command that archives the contact at {@code targetIndex} if {@code archive} is true,
     * or unarchives it otherwise.
     */
    public ArchiveCommand(Index targetIndex, boolean archive) {
        this.targetIndex = requireNonNull(targetIndex);
        this.archive = archive;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        List<Person> displayedPersons = model.getFilteredPersonList();
        if (targetIndex.getZeroBased() >= displayedPersons.size()) {
            throw new CommandException(String.format(MESSAGE_INVALID_INDEX, targetIndex.getOneBased()));
        }
        Person target = displayedPersons.get(targetIndex.getZeroBased());
        if (target.isArchived() == archive) {
            throw new CommandException(String.format(archive ? MESSAGE_ALREADY_ARCHIVED : MESSAGE_NOT_ARCHIVED,
                    target.getName()));
        }
        model.setPerson(target, target.withArchived(archive));
        return new CommandResult(String.format(archive ? MESSAGE_ARCHIVE_SUCCESS : MESSAGE_UNARCHIVE_SUCCESS,
                target.getName()));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ArchiveCommand otherCommand && targetIndex.equals(otherCommand.targetIndex)
                && archive == otherCommand.archive;
    }

    @Override
    public int hashCode() {
        return targetIndex.getZeroBased() * 31 + Boolean.hashCode(archive);
    }
}
