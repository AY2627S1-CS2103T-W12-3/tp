package coordimate.model.person;

import static java.util.Objects.requireNonNull;

import java.util.Locale;
import java.util.function.Predicate;

import coordimate.commons.util.ToStringBuilder;

/**
 * Tests that a {@code Person}'s name, phone, email, address, or organisation contains
 * the given keyword as a case-insensitive substring.
 */
public class ContactMatchesKeywordPredicate implements Predicate<Person> {
    private final String keyword;

    /**
     * Creates a predicate that matches contacts whose name, phone, email, address, or
     * organisation contains {@code keyword}, ignoring case.
     */
    public ContactMatchesKeywordPredicate(String keyword) {
        requireNonNull(keyword);
        this.keyword = keyword;
    }

    /**
     * Returns true if {@code person}'s name, phone, email, address, or organisation
     * contains the keyword as a case-insensitive substring.
     */
    @Override
    public boolean test(Person person) {
        String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
        return person.getName().getFullName().toLowerCase(Locale.ROOT).contains(lowerKeyword)
                || person.getPhone().getValue().toLowerCase(Locale.ROOT).contains(lowerKeyword)
                || person.getEmail().getValue().toLowerCase(Locale.ROOT).contains(lowerKeyword)
                || person.getAddress().map(address -> address.getValue().toLowerCase(Locale.ROOT)
                        .contains(lowerKeyword))
                        .orElse(false)
                || person.getOrganisation().map(organisation -> organisation.getValue().toLowerCase(Locale.ROOT)
                        .contains(lowerKeyword))
                        .orElse(false);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof ContactMatchesKeywordPredicate otherPredicate)) {
            return false;
        }
        return keyword.equals(otherPredicate.keyword);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("keyword", keyword).toString();
    }
}
