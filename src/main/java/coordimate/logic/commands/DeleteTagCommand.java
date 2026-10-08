package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.Model;
import coordimate.model.tag.Tag;

/**
 * Deletes an existing tag and removes it from every contact that uses it.
 */
public class DeleteTagCommand extends Command {

    public static final String COMMAND_WORD = "deletetag";
    public static final String MESSAGE_USAGE = COMMAND_WORD + " t/TAG";
    public static final String MESSAGE_SUCCESS = "%s successfully deleted.";
    public static final String MESSAGE_TAG_NOT_FOUND = "No such tag exists: %s.";

    private final String tagName;

    /**
     * Creates a command that deletes the tag matching {@code tagName}.
     */
    public DeleteTagCommand(String tagName) {
        this.tagName = requireNonNull(tagName);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        Optional<Tag> tagToDelete = model.getTagList().stream()
                .filter(tag -> tag.getTagName().equalsIgnoreCase(tagName))
                .findFirst();
        if (tagToDelete.isEmpty()) {
            throw new CommandException(String.format(MESSAGE_TAG_NOT_FOUND, tagName));
        }

        Tag target = tagToDelete.get();
        model.deleteTag(target);
        return new CommandResult(String.format(MESSAGE_SUCCESS, target.getTagName()), false, false, true);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof DeleteTagCommand otherCommand
                && tagName.equals(otherCommand.tagName);
    }

    @Override
    public int hashCode() {
        return tagName.hashCode();
    }
}
