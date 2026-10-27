package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.stream.Collectors;

import coordimate.model.Model;
import coordimate.model.tag.Tag;

/**
 * Lists all saved tags in CoordiMate.
 */
public class ListTagsCommand extends Command {

    public static final String COMMAND_WORD = "listtags";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Lists all saved tags.\n"
            + "Parameters: none\n"
            + "Example: " + COMMAND_WORD;

    public static final String MESSAGE_SUCCESS = "Here is the list of all existing tags.%nTags: %s.";
    public static final String MESSAGE_NO_TAGS = "There are no tags. Create one with addtag.";

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        if (model.getTagList().isEmpty()) {
            return new CommandResult(MESSAGE_NO_TAGS, CommandResult.View.TAGS);
        }
        String tagNames = model.getTagList().stream()
                .map(Tag::getTagName)
                .collect(Collectors.joining(", "));
        return new CommandResult(String.format(MESSAGE_SUCCESS, tagNames), CommandResult.View.TAGS);
    }
}
