package coordimate.logic.parser;

import static coordimate.logic.parser.CliSyntax.PREFIX_CONTACT;
import static coordimate.logic.parser.CliSyntax.PREFIX_EVENT_NAME;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import coordimate.logic.Messages;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.person.MemberId;

/**
 * Parses the {@code evn/EVENT_NAME [c/MEMBER_ID [MORE_MEMBER_IDS]...]} arguments shared by the commands
 * that view or change an event's members, rejecting unknown or repeated parameters.
 */
final class EventContactsParser {
    static final String MESSAGE_REPEATED_PARAMETER =
            Messages.MESSAGE_REPEATED_PARAMETER;
    static final String MESSAGE_INVALID_MEMBER_IDS =
            "Member IDs must be positive integers separated by spaces. Example: c/1 3 5";

    /**
     * The event name and permanent member IDs given to a command.
     */
    record Parsed(String eventName, List<MemberId> memberIds) {}

    private EventContactsParser() {}

    /**
     * Parses {@code evn/EVENT_NAME c/MEMBER_ID [MORE_MEMBER_IDS]...} into an event name and member IDs
     * in command order.
     *
     * @param usage The command's usage text, shown when {@code evn/} or {@code c/} is missing.
     * @param unknownParameterMessage The error shown when an unknown parameter is used.
     * @param noContactsMessage The error shown when {@code c/} has no member IDs.
     * @throws ParseException If the arguments do not follow the expected format.
     */
    static Parsed parse(String args, String usage, String unknownParameterMessage, String noContactsMessage)
            throws ParseException {
        ArgumentMultimap argMultimap = tokenize(args, usage, unknownParameterMessage,
                PREFIX_EVENT_NAME, PREFIX_CONTACT);
        return new Parsed(readEventName(argMultimap),
                parseMemberIds(argMultimap.getValue(PREFIX_CONTACT).get(), noContactsMessage));
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
        PrefixParser.rejectUnknownParameters(args, PrefixParser.EVENT_PARAMETER_PATTERN,
                allowedPrefixes, unknownParameterMessage);

        ArgumentMultimap argMultimap = ArgumentTokenizer.tokenize(args, allowed);
        boolean isMissingPrefix = Arrays.stream(allowed).anyMatch(prefix -> argMultimap.getValue(prefix).isEmpty());
        if (!argMultimap.getPreamble().isEmpty() || isMissingPrefix) {
            throw ParserUtil.createInvalidFormatException(usage);
        }
        for (Prefix prefix : allowed) {
            if (argMultimap.getAllValues(prefix).size() > 1) {
                throw new ParseException(String.format(MESSAGE_REPEATED_PARAMETER, prefix) + "\n" + usage);
            }
        }
        return argMultimap;
    }

    private static String readEventName(ArgumentMultimap argMultimap) throws ParseException {
        return ParserUtil.parseEventName(argMultimap.getValue(PREFIX_EVENT_NAME).get());
    }

    private static List<MemberId> parseMemberIds(String value, String noContactsMessage) throws ParseException {
        String trimmedValue = value.strip();
        if (trimmedValue.isEmpty()) {
            throw new ParseException(noContactsMessage);
        }
        List<MemberId> memberIds = new ArrayList<>();
        for (String token : trimmedValue.split("\\s+")) {
            try {
                memberIds.add(ParserUtil.parseMemberId(token));
            } catch (ParseException e) {
                throw new ParseException(MESSAGE_INVALID_MEMBER_IDS, e);
            }
        }
        return memberIds;
    }
}
