package coordimate.model.person;

import static coordimate.commons.util.AppUtil.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.Locale;

/**
 * Represents a Person's email in the CoordiMate.
 * Guarantees: immutable; is valid as declared in {@link #isValidEmail(String)}
 */
public class Email {

    public static final String MESSAGE_CONSTRAINTS = "Invalid email. Example: e/aish@example.com";

    private static final String LOCAL_PART_REGEX = "[A-Za-z0-9_%+-](?:[A-Za-z0-9._%+-]*[A-Za-z0-9_%+-])?";
    private static final String DOMAIN_LABEL_REGEX = "[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?";
    private static final String VALIDATION_REGEX = LOCAL_PART_REGEX + "@(?:" + DOMAIN_LABEL_REGEX
            + "\\.)+[A-Za-z]{2,}";

    private final String value;

    /**
     * Constructs an {@code Email}.
     *
     * @param email A valid email address.
     */
    public Email(String email) {
        requireNonNull(email);
        checkArgument(isValidEmail(email), MESSAGE_CONSTRAINTS);
        value = email;
    }

    public String getValue() {
        return value;
    }

    /**
     * Returns the email without surrounding spaces or letter case for duplicate checks.
     */
    public String getNormalizedValue() {
        return value.strip().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns true if a given string is a valid email.
     */
    public static boolean isValidEmail(String test) {
        requireNonNull(test);
        return test.matches(VALIDATION_REGEX) && !test.substring(0, test.indexOf('@')).contains("..");
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Email otherEmail)) {
            return false;
        }

        return value.equals(otherEmail.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

}
