package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.Model;
import coordimate.model.tag.Tag;

/**
 * Renames an existing tag and updates every contact that uses it.
 */
public class EditTagCommand extends Command {

    public static final String COMMAND_WORD = "edittag";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Renames a tag. "
            + "Parameters: OLD_TAG t/NEW_TAG\n"
            + "Example: " + COMMAND_WORD + " Media t/Publicity";

    public static final String MESSAGE_SUCCESS = "%s successfully renamed to %s.";
    public static final String MESSAGE_TAG_NOT_FOUND = "No such tag exists: %s.";
    public static final String MESSAGE_SAME_TAG = "%s is the same as %s.";
    public static final String MESSAGE_DUPLICATE_TAG = "This tag already exists.";

    private final String currentTagName;
    private final Tag editedTag;

    /**
     * Creates a command that renames {@code currentTagName} to {@code editedTag}.
     */
    public EditTagCommand(String currentTagName, Tag editedTag) {
        this.currentTagName = requireNonNull(currentTagName);
        this.editedTag = requireNonNull(editedTag);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        Optional<Tag> currentTag = model.getTagList().stream()
                .filter(tag -> tag.getTagName().equalsIgnoreCase(currentTagName))
                .findFirst();
        if (currentTag.isEmpty()) {
            throw new CommandException(String.format(MESSAGE_TAG_NOT_FOUND, currentTagName));
        }

        Tag tagToEdit = currentTag.get();
        if (tagToEdit.isSameTag(editedTag)) {
            throw new CommandException(String.format(MESSAGE_SAME_TAG,
                    editedTag.getTagName(), tagToEdit.getTagName()));
        }
        if (model.hasTag(editedTag)) {
            throw new CommandException(MESSAGE_DUPLICATE_TAG);
        }

        model.setTag(tagToEdit, editedTag);
        return new CommandResult(String.format(MESSAGE_SUCCESS,
                tagToEdit.getTagName(), editedTag.getTagName()), false, false, true);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof EditTagCommand otherCommand
                && currentTagName.equals(otherCommand.currentTagName)
                && editedTag.equals(otherCommand.editedTag);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(currentTagName, editedTag);
    }
}
