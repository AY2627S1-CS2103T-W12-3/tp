package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.Model;
import coordimate.model.tag.Tag;

/**
 * Creates a custom tag unless a tag with the same name already exists.
 */
public class CreateTagCommand extends Command {

    public static final String COMMAND_WORD = "newtag";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Creates a custom tag. "
            + "Parameters: t/TAG\n"
            + "Example: " + COMMAND_WORD + " t/Publicity";

    public static final String MESSAGE_SUCCESS = "Created tag: %s.";
    public static final String MESSAGE_DUPLICATE_TAG = "This tag already exists.";

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
