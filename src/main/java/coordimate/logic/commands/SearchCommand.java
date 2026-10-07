package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import coordimate.model.Model;
import coordimate.model.person.ContactMatchesKeywordPredicate;

/**
 * Filters the displayed contact list to those whose name, phone, email, or address
 * contains the given keyword, ignoring case.
 */
public class SearchCommand extends Command {

    public static final String COMMAND_WORD = "search";
    public static final String MESSAGE_USAGE = "search KEYWORD";
    public static final String MESSAGE_SUCCESS = "%1$d contact(s) found matching \"%2$s\".";
    public static final String MESSAGE_NO_MATCHES = "No contacts found matching \"%1$s\".";

    private final ContactMatchesKeywordPredicate predicate;
    private final String keyword;

    /**
     * Creates a command that filters the contact list using {@code predicate},
     * reporting results against the original {@code keyword}.
     */
    public SearchCommand(ContactMatchesKeywordPredicate predicate, String keyword) {
        this.predicate = requireNonNull(predicate);
        this.keyword = requireNonNull(keyword);
    }

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.updateFilteredPersonList(predicate);
        int matchCount = model.getFilteredPersonList().size();
        if (matchCount == 0) {
            return new CommandResult(String.format(MESSAGE_NO_MATCHES, keyword));
        }
        return new CommandResult(String.format(MESSAGE_SUCCESS, matchCount, keyword));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof SearchCommand otherCommand)) {
            return false;
        }
        return predicate.equals(otherCommand.predicate) && keyword.equals(otherCommand.keyword);
    }
}
