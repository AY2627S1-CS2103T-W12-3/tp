package coordimate.model.person;

import static java.util.Objects.requireNonNull;

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
        return containsIgnoreCase(person.getName().getFullName(), keyword)
                || containsIgnoreCase(person.getPhone().getValue(), keyword)
                || containsIgnoreCase(person.getEmail().getValue(), keyword)
                || person.getAddress().map(address -> containsIgnoreCase(address.getValue(), keyword))
                        .orElse(false)
                || person.getOrganisation().map(organisation -> containsIgnoreCase(organisation.getValue(), keyword))
                        .orElse(false);
    }

    private static boolean containsIgnoreCase(String value, String keyword) {
        for (int index = 0; index <= value.length() - keyword.length(); index++) {
            if (value.regionMatches(true, index, keyword, 0, keyword.length())) {
                return true;
            }
        }
        return false;
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
