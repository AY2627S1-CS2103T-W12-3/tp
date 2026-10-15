package coordimate.logic;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import coordimate.logic.parser.Prefix;
import coordimate.model.person.Address;
import coordimate.model.person.Person;

/**
 * Container for user visible messages.
 */
public class Messages {

    public static final String MESSAGE_UNKNOWN_COMMAND = "Unknown command. Use help to see the available commands. "
            + "Example: help";
    public static final String MESSAGE_UNKNOWN_PARAMETER = "Unknown parameter: %s.";
    public static final String MESSAGE_REPEATED_PARAMETER = "Parameter %s may only be specified once.";
    public static final String MESSAGE_REPEATED_REQUIRED_PARAMETER =
            MESSAGE_REPEATED_PARAMETER;
    public static final String MESSAGE_LOAD_ERROR = "Saved data could not be loaded. Check the local data file.";
    public static final String MESSAGE_EMPTY_FIELD = "%s must not be empty. Example: %s";
    public static final String MESSAGE_INVALID_COMMAND_FORMAT = "Invalid command format.\n%1$s";
    public static final String MESSAGE_INVALID_PERSON_DISPLAYED_INDEX =
            "No contact exists at index %1$d. Use an index from the current contact list.";
    public static final String MESSAGE_EVENT_NOT_FOUND =
            "Event %s does not exist. Use a saved event name.";
    public static final String MESSAGE_TAG_NOT_FOUND =
            "Tag %s does not exist. Use a saved tag name.";
    public static final String MESSAGE_CONTACT_NO_MATCH =
            "No contact matches that identifier. Use an exact saved name, phone number, or email address.";
    public static final String MESSAGE_CONTACT_AMBIGUOUS_MATCH = "Multiple contacts match that identifier:\n%s\n"
            + "Use a displayed index, phone number, or email address to identify the contact.";
    public static final String MESSAGE_PERSONS_LISTED_OVERVIEW = "%1$d contact(s) listed!";

    /**
     * Returns an error message indicating the duplicate prefixes.
     */
    public static String getErrorMessageForDuplicatePrefixes(Prefix... duplicatePrefixes) {
        assert duplicatePrefixes.length > 0;

        Set<String> duplicateFields =
                Stream.of(duplicatePrefixes).map(Prefix::toString).collect(Collectors.toSet());

        return duplicateFields.stream().sorted()
                .map(prefix -> String.format(MESSAGE_REPEATED_PARAMETER, prefix))
                .collect(Collectors.joining(" "));
    }

    /**
     * Appends command usage once, retaining the original explanation and example.
     */
    public static String withUsage(String message, String usage) {
        return message.endsWith(usage) ? message : message + "\n" + usage;
    }

    /**
     * Formats the {@code person} for display to the user.
     */
    public static String format(Person person) {
        final StringBuilder builder = new StringBuilder();
        builder.append(person.getName())
                .append("; Phone: ")
                .append(person.getPhone())
                .append("; Email: ")
                .append(person.getEmail())
                .append("; Address: ")
                .append(person.getAddress().map(Address::toString).orElse("— (not specified)"))
                .append("; Tags: ");
        person.getTags().forEach(builder::append);
        return builder.toString();
    }

}
