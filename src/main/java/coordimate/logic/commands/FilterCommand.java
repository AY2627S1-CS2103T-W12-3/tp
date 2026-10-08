package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.function.Predicate;

import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.Model;
import coordimate.model.event.Event;
import coordimate.model.person.FilterCriterion;
import coordimate.model.person.Person;

/**
 * Filters the displayed contact list to contacts matching every given criterion.
 */
public class FilterCommand extends Command {

    public static final String COMMAND_WORD = "filter";
    public static final String MESSAGE_USAGE = "filter FIELD/VALUE";
    public static final String MESSAGE_SUCCESS = "%1$d contact(s) found.";
    public static final String MESSAGE_NO_MATCHES = "No contacts match the specified filters.";
    public static final String MESSAGE_NO_MATCHING_VALUE = "No matching value found.";

    private final List<FilterCriterion> criteria;

    public FilterCommand(List<FilterCriterion> criteria) {
        this.criteria = requireNonNull(criteria);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        Predicate<Person> combined = person -> true;
        for (FilterCriterion criterion : criteria) {
            combined = combined.and(resolvePredicate(criterion, model));
        }

        model.updateFilteredPersonList(combined);
        int matchCount = model.getFilteredPersonList().size();
        if (matchCount == 0) {
            return new CommandResult(MESSAGE_NO_MATCHES);
        }
        return new CommandResult(String.format(MESSAGE_SUCCESS, matchCount));
    }

    /**
     * Resolves one criterion against saved data, throwing if its value does not exist.
     */
    private Predicate<Person> resolvePredicate(FilterCriterion criterion, Model model) throws CommandException {
        String value = criterion.value();
        switch (criterion.field()) {
            case TAG -> {
                boolean tagExists = model.getTagList().stream()
                        .anyMatch(tag -> tag.getTagName().equalsIgnoreCase(value));
                if (!tagExists) {
                    throw new CommandException(MESSAGE_NO_MATCHING_VALUE);
                }
                return person -> person.getTags().stream().anyMatch(tag -> tag.getTagName().equalsIgnoreCase(value));
            }
            case EVENT -> {
                Event event = model.getCoordiMate().getEventList().stream()
                        .filter(candidate -> candidate.getName().equalsIgnoreCase(value))
                        .findFirst()
                        .orElseThrow(() -> new CommandException(MESSAGE_NO_MATCHING_VALUE));
                return person -> event.hasMember(person.getName());
            }
            default -> throw new IllegalStateException("Unhandled filter field: " + criterion.field());
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof FilterCommand otherCommand)) {
            return false;
        }
        return criteria.equals(otherCommand.criteria);
    }
}
