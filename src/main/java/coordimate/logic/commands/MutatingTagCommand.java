package coordimate.logic.commands;

import coordimate.logic.Messages;

/**
 * A command that changes the saved tag list and must be persisted atomically.
 */
public abstract class MutatingTagCommand extends Command {

    public static final String MESSAGE_SAVE_ERROR = "Tag could not be saved. No changes were made.";
    public static final String MESSAGE_LOAD_ERROR =
            Messages.MESSAGE_LOAD_ERROR;
}
