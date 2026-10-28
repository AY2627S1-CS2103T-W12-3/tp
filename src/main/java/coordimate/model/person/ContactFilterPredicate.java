package coordimate.model.person;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import coordimate.commons.util.ToStringBuilder;

/**
 * Tests that a {@code Person} matches every criterion of a {@code filter} command.
 * The criteria are kept separately so that one can be replaced later, such as when a filtered event is renamed.
 */
public class ContactFilterPredicate implements Predicate<Person> {
    private final List<Predicate<Person>> criteria;

    /**
     * Creates a predicate that matches contacts matching all of {@code criteria}, which must not be empty.
     */
    public ContactFilterPredicate(List<Predicate<Person>> criteria) {
        requireNonNull(criteria);
        assert !criteria.isEmpty() : "A filter must have at least one criterion.";
        this.criteria = List.copyOf(criteria);
    }

    @Override
    public boolean test(Person person) {
        return criteria.stream().allMatch(criterion -> criterion.test(person));
    }

    /**
     * Returns a predicate in which each criterion is replaced by the result of {@code replacer} for it.
     */
    public ContactFilterPredicate withCriteriaReplaced(UnaryOperator<Predicate<Person>> replacer) {
        requireNonNull(replacer);
        return new ContactFilterPredicate(criteria.stream().map(replacer).toList());
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ContactFilterPredicate otherPredicate && criteria.equals(otherPredicate.criteria);
    }

    @Override
    public int hashCode() {
        return criteria.hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("criteria", criteria).toString();
    }
}
