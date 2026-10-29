package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import coordimate.commons.util.StringUtil;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.Model;
import coordimate.model.event.Event;
import coordimate.model.event.EventMembersPredicate;
import coordimate.model.person.ContactFilterPredicate;
import coordimate.model.person.FilterCriterion;
import coordimate.model.person.Person;
import coordimate.model.person.TagFilterPredicate;

/**
 * Filters the displayed contact list to contacts matching every given criterion.
 */
public class FilterCommand extends Command {

    public static final String COMMAND_WORD = "filter";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Filters contacts by one or more criteria.\n"
            + "Parameters: FIELD/VALUE [FIELD/VALUE]... (FIELD: tag, event, organisation)\n"
            + "Example: " + COMMAND_WORD + " tag/EXCO organisation/NUS Student Affairs";
    public static final String MESSAGE_SUCCESS = "%1$d contact(s) found.";
    public static final String MESSAGE_NO_MATCHES = "No contacts match the specified filters.";
    public static final String MESSAGE_NO_MATCHING_VALUE =
            "No saved tag, event, or organisation matches that value. Use a value from the saved data.";

    private final List<FilterCriterion> criteria;

    public FilterCommand(List<FilterCriterion> criteria) {
        this.criteria = requireNonNull(criteria);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        List<Predicate<Person>> resolved = new ArrayList<>();
        for (FilterCriterion criterion : criteria) {
            resolved.add(resolvePredicate(criterion, model));
        }

        model.updateFilteredPersonList(new ContactFilterPredicate(resolved));
        int matchCount = model.getFilteredPersonList().size();
        if (matchCount == 0) {
            return new CommandResult(MESSAGE_NO_MATCHES, CommandResult.View.CONTACTS);
        }
        return new CommandResult(String.format(MESSAGE_SUCCESS, matchCount), CommandResult.View.CONTACTS);
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
                // A named predicate, so that the filter can follow the tag when it is renamed.
                return new TagFilterPredicate(value);
            }
            case EVENT -> {
                String eventName = StringUtil.normalizeWhitespace(value);
                List<Event> events = model.getCoordiMate().getEventList();
                boolean eventExists = events.stream().anyMatch(event -> event.getName().equalsIgnoreCase(eventName));
                if (!eventExists) {
                    throw new CommandException(MESSAGE_NO_MATCHING_VALUE);
                }
                // Looks the event up on every test, so the filtered list follows later changes to its members.
                return new EventMembersPredicate(eventName, events);
            }
            case ORGANISATION -> {
                String organisationName = StringUtil.normalizeWhitespace(value);
                Predicate<Person> inOrganisation = person -> person.getOrganisation()
                        .map(organisation -> organisation.getValue().equalsIgnoreCase(organisationName))
                        .orElse(false);
                boolean organisationExists = model.getCoordiMate().getPersonList().stream().anyMatch(inOrganisation);
                if (!organisationExists) {
                    throw new CommandException(MESSAGE_NO_MATCHING_VALUE);
                }
                return inOrganisation;
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
