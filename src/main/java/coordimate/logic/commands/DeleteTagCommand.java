package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import coordimate.logic.Messages;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.Model;
import coordimate.model.tag.Tag;

/**
 * Deletes an existing tag and removes it from every contact that uses it.
 */
public class DeleteTagCommand extends MutatingTagCommand {

    public static final String COMMAND_WORD = "deletetag";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Deletes a saved tag.\n"
            + "Parameters: t/TAG\n"
            + "Example: " + COMMAND_WORD + " t/Publicity";
    public static final String MESSAGE_SUCCESS = "%s successfully deleted.";
    public static final String MESSAGE_TAG_NOT_FOUND = Messages.MESSAGE_TAG_NOT_FOUND;

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
        Tag target = findTag(model, tagName);
        model.deleteTag(target);
        return new CommandResult(String.format(MESSAGE_SUCCESS, target.getTagName()), CommandResult.View.TAGS);
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
