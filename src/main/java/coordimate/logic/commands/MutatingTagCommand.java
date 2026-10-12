package coordimate.logic.commands;

/**
 * A command that changes the saved tag list and must be persisted atomically.
 */
public abstract class MutatingTagCommand extends Command {

    public static final String MESSAGE_SAVE_ERROR = "Tag could not be saved. No changes were made.";
    public static final String MESSAGE_LOAD_ERROR =
            "Contact data could not be loaded. Please check the local data file.";
}
