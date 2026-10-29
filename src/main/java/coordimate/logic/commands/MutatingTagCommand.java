package coordimate.logic.commands;

import coordimate.logic.Messages;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.Model;
import coordimate.model.tag.Tag;

/**
 * A command that changes the saved tag list and must be persisted atomically.
 */
public abstract class MutatingTagCommand extends Command {

    public static final String MESSAGE_SAVE_ERROR = "Tag could not be saved. No changes were made.";
    public static final String MESSAGE_LOAD_ERROR =
            Messages.MESSAGE_LOAD_ERROR;

    /**
     * Returns the saved tag named {@code tagName}, ignoring case.
     *
     * @throws CommandException if no saved tag has that name.
     */
    protected static Tag findTag(Model model, String tagName) throws CommandException {
        return model.getTagList().stream()
                .filter(tag -> tag.getTagName().equalsIgnoreCase(tagName))
                .findFirst()
                .orElseThrow(() -> new CommandException(String.format(Messages.MESSAGE_TAG_NOT_FOUND, tagName)));
    }
}
