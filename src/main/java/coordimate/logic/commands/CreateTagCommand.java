package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.Model;
import coordimate.model.tag.Tag;

/**
 * Creates a custom tag unless a tag with the same name already exists.
 */
public class CreateTagCommand extends MutatingTagCommand {

    public static final String COMMAND_WORD = "addtag";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Creates a custom tag.\n"
            + "Parameters: t/TAG\n"
            + "Example: " + COMMAND_WORD + " t/Publicity";

    public static final String MESSAGE_SUCCESS = "Created tag: %s.";
    public static final String MESSAGE_DUPLICATE_TAG = "A tag with this name already exists. No changes were made.";
    private final Tag toAdd;

    /**
     * Creates a command that adds {@code tag} to the saved tag list.
     */
    public CreateTagCommand(Tag tag) {
        toAdd = requireNonNull(tag);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        if (model.hasTag(toAdd)) {
            throw new CommandException(MESSAGE_DUPLICATE_TAG);
        }

        model.addTag(toAdd);
        return new CommandResult(String.format(MESSAGE_SUCCESS, toAdd.getTagName()), false, false, true);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CreateTagCommand otherCommand && toAdd.equals(otherCommand.toAdd);
    }

    @Override
    public int hashCode() {
        return toAdd.hashCode();
    }
}
