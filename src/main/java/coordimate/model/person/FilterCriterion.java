package coordimate.model.person;

import static java.util.Objects.requireNonNull;

/**
 * One field-value pair from a {@code filter} command, before it is resolved against saved data.
 */
public record FilterCriterion(FilterField field, String value) {
    /**
     * Creates a filter criterion for the given field and value.
     */
    public FilterCriterion {
        requireNonNull(field);
        requireNonNull(value);
    }
}
