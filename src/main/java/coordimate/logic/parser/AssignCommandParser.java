package coordimate.logic.parser;

import static coordimate.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static coordimate.logic.parser.CliSyntax.PREFIX_CONTACT;
import static coordimate.logic.parser.CliSyntax.PREFIX_EVENT_NAME;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import coordimate.commons.core.index.Index;
import coordimate.commons.util.StringUtil;
import coordimate.logic.commands.AssignCommand;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.event.Event;

/**
 * Parses an event name and space-separated contact indexes, rejecting unknown or repeated parameters.
 */
public class AssignCommandParser implements Parser<AssignCommand> {
    public static final String MESSAGE_UNKNOWN_PARAMETER = "Unknown parameter. Example: assign evn/Final Concert c/2 3";
    public static final String MESSAGE_REPEATED_PARAMETER = "Each parameter may only be specified once.";
    public static final String MESSAGE_NO_CONTACTS = "Please specify at least one contact to assign.";
    public static final String MESSAGE_INVALID_CONTACT_INDEXES =
            "Contact indexes must be positive integers separated by spaces. Example: c/1 3 5";

    // Same parameter pattern as the other event commands, so all of them reject the same unknown parameters.
    private static final Pattern PARAMETER_PATTERN = Pattern.compile("(?<!\\S)([A-Za-z]+/|/[A-Za-z]+)");
    private static final Set<String> PREFIXES = Set.of(PREFIX_EVENT_NAME.toString(), PREFIX_CONTACT.toString());

    @Override
    public AssignCommand parse(String args) throws ParseException {
        requireNonNull(args);
        Matcher matcher = PARAMETER_PATTERN.matcher(args);
        while (matcher.find()) {
            if (!PREFIXES.contains(matcher.group())) {
                throw new ParseException(MESSAGE_UNKNOWN_PARAMETER);
            }
        }

        ArgumentMultimap argMultimap = ArgumentTokenizer.tokenize(args, PREFIX_EVENT_NAME, PREFIX_CONTACT);
        if (!argMultimap.getPreamble().isEmpty() || argMultimap.getValue(PREFIX_EVENT_NAME).isEmpty()
                || argMultimap.getValue(PREFIX_CONTACT).isEmpty()) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, AssignCommand.MESSAGE_USAGE));
        }
        if (argMultimap.getAllValues(PREFIX_EVENT_NAME).size() > 1
                || argMultimap.getAllValues(PREFIX_CONTACT).size() > 1) {
            throw new ParseException(MESSAGE_REPEATED_PARAMETER);
        }

        String eventName = argMultimap.getValue(PREFIX_EVENT_NAME).get().strip();
        if (eventName.isEmpty()) {
            throw new ParseException(Event.MESSAGE_EMPTY_NAME);
        }
        return new AssignCommand(eventName, parseContactIndexes(argMultimap.getValue(PREFIX_CONTACT).get()));
    }

    private List<Index> parseContactIndexes(String value) throws ParseException {
        String trimmedValue = value.strip();
        if (trimmedValue.isEmpty()) {
            throw new ParseException(MESSAGE_NO_CONTACTS);
        }
        List<Index> indexes = new ArrayList<>();
        for (String token : trimmedValue.split("\\s+")) {
            if (!StringUtil.isNonZeroUnsignedInteger(token)) {
                throw new ParseException(MESSAGE_INVALID_CONTACT_INDEXES);
            }
            indexes.add(Index.fromOneBased(Integer.parseInt(token)));
        }
        return indexes;
    }
}
