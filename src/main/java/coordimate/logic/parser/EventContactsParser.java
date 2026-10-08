package coordimate.logic.parser;

import static coordimate.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static coordimate.logic.parser.CliSyntax.PREFIX_CONTACT;
import static coordimate.logic.parser.CliSyntax.PREFIX_EVENT_NAME;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import coordimate.commons.core.index.Index;
import coordimate.commons.util.StringUtil;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.event.Event;

/**
 * Parses the {@code evn/EVENT_NAME [c/CONTACT_INDEX [MORE_CONTACT_INDEXES]...]} arguments shared by the commands
 * that view or change an event's members, rejecting unknown or repeated parameters.
 */
final class EventContactsParser {
    static final String MESSAGE_REPEATED_PARAMETER = "Each parameter may only be specified once.";
    static final String MESSAGE_INVALID_CONTACT_INDEXES =
            "Contact indexes must be positive integers separated by spaces. Example: c/1 3 5";

    // Same parameter pattern as the other event commands, so all of them reject the same unknown parameters.
    private static final Pattern PARAMETER_PATTERN = Pattern.compile("(?<!\\S)([A-Za-z]+/|/[A-Za-z]+)");

    /**
     * The event name and contact indexes given to a command.
     */
    record Parsed(String eventName, List<Index> contactIndexes) {}

    private EventContactsParser() {}

    /**
     * Parses {@code evn/EVENT_NAME c/CONTACT_INDEX [MORE_CONTACT_INDEXES]...} into an event name and the contact
     * indexes in the order given.
     *
     * @param usage The command's usage text, shown when {@code evn/} or {@code c/} is missing.
     * @param unknownParameterMessage The error shown when an unknown parameter is used.
     * @param noContactsMessage The error shown when {@code c/} has no indexes.
     * @throws ParseException If the arguments do not follow the expected format.
     */
    static Parsed parse(String args, String usage, String unknownParameterMessage, String noContactsMessage)
            throws ParseException {
        ArgumentMultimap argMultimap = tokenize(args, usage, unknownParameterMessage,
                PREFIX_EVENT_NAME, PREFIX_CONTACT);
        return new Parsed(readEventName(argMultimap),
                parseContactIndexes(argMultimap.getValue(PREFIX_CONTACT).get(), noContactsMessage));
    }

    /**
     * Parses {@code evn/EVENT_NAME} into an event name.
     *
     * @param usage The command's usage text, shown when {@code evn/} is missing.
     * @param unknownParameterMessage The error shown when an unknown parameter is used.
     * @throws ParseException If the arguments do not follow the expected format.
     */
    static String parseEventName(String args, String usage, String unknownParameterMessage) throws ParseException {
        return readEventName(tokenize(args, usage, unknownParameterMessage, PREFIX_EVENT_NAME));
    }

    /**
     * Splits {@code args} by the {@code allowed} prefixes, each of which must appear exactly once with nothing
     * before the first one.
     */
    private static ArgumentMultimap tokenize(String args, String usage, String unknownParameterMessage,
            Prefix... allowed) throws ParseException {
        requireNonNull(args);
        List<String> allowedPrefixes = Arrays.stream(allowed).map(Prefix::toString).toList();
        Matcher matcher = PARAMETER_PATTERN.matcher(args);
        while (matcher.find()) {
            if (!allowedPrefixes.contains(matcher.group())) {
                throw new ParseException(unknownParameterMessage);
            }
        }

        ArgumentMultimap argMultimap = ArgumentTokenizer.tokenize(args, allowed);
        boolean isMissingPrefix = Arrays.stream(allowed).anyMatch(prefix -> argMultimap.getValue(prefix).isEmpty());
        if (!argMultimap.getPreamble().isEmpty() || isMissingPrefix) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, usage));
        }
        if (Arrays.stream(allowed).anyMatch(prefix -> argMultimap.getAllValues(prefix).size() > 1)) {
            throw new ParseException(MESSAGE_REPEATED_PARAMETER);
        }
        return argMultimap;
    }

    private static String readEventName(ArgumentMultimap argMultimap) throws ParseException {
        String eventName = argMultimap.getValue(PREFIX_EVENT_NAME).get().strip();
        if (eventName.isEmpty()) {
            throw new ParseException(Event.MESSAGE_EMPTY_NAME);
        }
        return eventName;
    }

    private static List<Index> parseContactIndexes(String value, String noContactsMessage) throws ParseException {
        String trimmedValue = value.strip();
        if (trimmedValue.isEmpty()) {
            throw new ParseException(noContactsMessage);
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
