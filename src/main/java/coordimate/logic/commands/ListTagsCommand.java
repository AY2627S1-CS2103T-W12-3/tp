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

    public static final String MESSAGE_SUCCESS = "Here is the list of all existing tags.%nTags: %s.";

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        String tagNames = model.getTagList().stream()
                .map(Tag::getTagName)
                .collect(Collectors.joining(", "));
        return new CommandResult(String.format(MESSAGE_SUCCESS, tagNames), false, false, true);
    }
}
