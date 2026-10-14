package coordimate.logic.parser;

import static coordimate.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static coordimate.logic.parser.CliSyntax.PREFIX_ADDRESS;
import static coordimate.logic.parser.CliSyntax.PREFIX_BIRTHDAY;
import static coordimate.logic.parser.CliSyntax.PREFIX_EMAIL;
import static coordimate.logic.parser.CliSyntax.PREFIX_NAME;
import static coordimate.logic.parser.CliSyntax.PREFIX_NOTE;
import static coordimate.logic.parser.CliSyntax.PREFIX_ORGANISATION;
import static coordimate.logic.parser.CliSyntax.PREFIX_PHONE;
import static coordimate.logic.parser.CliSyntax.PREFIX_ROLE;
import static coordimate.logic.parser.CliSyntax.PREFIX_TAG;
import static java.util.Objects.requireNonNull;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import coordimate.logic.commands.AddCommand;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.person.Address;
import coordimate.model.person.Birthday;
import coordimate.model.person.Email;
import coordimate.model.person.Name;
import coordimate.model.person.Note;
import coordimate.model.person.Organisation;
import coordimate.model.person.Person;
import coordimate.model.person.Phone;
import coordimate.model.person.Role;
import coordimate.model.tag.Tag;

/**
 * Parses the required and optional contact fields of an add command.
 */
public class AddCommandParser implements Parser<AddCommand> {
    public static final String MESSAGE_UNKNOWN_PARAMETER = "Unknown parameter. Example: r/Logistics";
    public static final String MESSAGE_REPEATED_REQUIRED_PARAMETER =
            "Each required parameter may only be specified once.";
    public static final String MESSAGE_EMPTY_REQUIRED_VALUE = "This field cannot be empty. Example: n/Aisha Tan";

    private static final Pattern PARAMETER_PATTERN = Pattern.compile("(?<!\\S)([A-Za-z]+/)");
    private static final Set<String> ALLOWED_PREFIXES = Set.of(PREFIX_NAME.toString(), PREFIX_PHONE.toString(),
            PREFIX_EMAIL.toString(), PREFIX_ROLE.toString(), PREFIX_BIRTHDAY.toString(), PREFIX_ADDRESS.toString(),
            PREFIX_ORGANISATION.toString(), PREFIX_TAG.toString(), PREFIX_NOTE.toString());

    @Override
    public AddCommand parse(String args) throws ParseException {
        requireNonNull(args);
        rejectUnknownParameters(args);
        ArgumentMultimap arguments = ArgumentTokenizer.tokenize(args, PREFIX_NAME, PREFIX_PHONE, PREFIX_EMAIL,
                PREFIX_ROLE, PREFIX_BIRTHDAY, PREFIX_ADDRESS, PREFIX_ORGANISATION, PREFIX_TAG, PREFIX_NOTE);
        if (!hasRequiredPrefixes(arguments) || !arguments.getPreamble().isEmpty()) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, AddCommand.MESSAGE_USAGE));
        }
        rejectRepeatedRequiredParameters(arguments);
        arguments.verifyNoDuplicatePrefixesFor(PREFIX_BIRTHDAY, PREFIX_ADDRESS, PREFIX_ORGANISATION, PREFIX_NOTE);
        rejectEmptyRequiredValues(arguments);

        Name name = ParserUtil.parseName(arguments.getValue(PREFIX_NAME).orElseThrow());
        Phone phone = ParserUtil.parsePhone(arguments.getValue(PREFIX_PHONE).orElseThrow());
        Email email = ParserUtil.parseEmail(arguments.getValue(PREFIX_EMAIL).orElseThrow());
        Role role = ParserUtil.parseRole(arguments.getValue(PREFIX_ROLE).orElseThrow());
        Birthday birthday = parseBirthday(arguments.getValue(PREFIX_BIRTHDAY).orElse(""));
        Address address = parseAddress(arguments.getValue(PREFIX_ADDRESS).orElse(""));
        Organisation organisation = parseOrganisation(arguments.getValue(PREFIX_ORGANISATION).orElse(""));
        Note note = parseNote(arguments.getValue(PREFIX_NOTE).orElse(""));
        Set<Tag> tags = parseTags(arguments);
        return new AddCommand(new Person(name, phone, email, role, birthday, address, organisation, note, tags));
    }

    private static void rejectUnknownParameters(String args) throws ParseException {
        Matcher matcher = PARAMETER_PATTERN.matcher(args);
        while (matcher.find()) {
            if (!ALLOWED_PREFIXES.contains(matcher.group())) {
                throw new ParseException(MESSAGE_UNKNOWN_PARAMETER);
            }
        }
    }

    private static boolean hasRequiredPrefixes(ArgumentMultimap arguments) {
        return arguments.getValue(PREFIX_NAME).isPresent() && arguments.getValue(PREFIX_PHONE).isPresent()
                && arguments.getValue(PREFIX_EMAIL).isPresent() && arguments.getValue(PREFIX_ROLE).isPresent();
    }

    private static void rejectRepeatedRequiredParameters(ArgumentMultimap arguments) throws ParseException {
        if (arguments.getAllValues(PREFIX_NAME).size() > 1 || arguments.getAllValues(PREFIX_PHONE).size() > 1
                || arguments.getAllValues(PREFIX_EMAIL).size() > 1 || arguments.getAllValues(PREFIX_ROLE).size() > 1) {
            throw new ParseException(MESSAGE_REPEATED_REQUIRED_PARAMETER);
        }
    }

    private static void rejectEmptyRequiredValues(ArgumentMultimap arguments) throws ParseException {
        if (arguments.getValue(PREFIX_NAME).orElseThrow().isEmpty()
                || arguments.getValue(PREFIX_PHONE).orElseThrow().isEmpty()
                || arguments.getValue(PREFIX_EMAIL).orElseThrow().isEmpty()
                || arguments.getValue(PREFIX_ROLE).orElseThrow().isEmpty()) {
            throw new ParseException(MESSAGE_EMPTY_REQUIRED_VALUE);
        }
    }

    private static Birthday parseBirthday(String value) throws ParseException {
        return value.isEmpty() ? null : ParserUtil.parseBirthday(value);
    }

    private static Address parseAddress(String value) throws ParseException {
        return value.isEmpty() ? null : ParserUtil.parseAddress(value);
    }

    private static Organisation parseOrganisation(String value) throws ParseException {
        return value.isEmpty() ? null : ParserUtil.parseOrganisation(value);
    }

    private static Note parseNote(String value) throws ParseException {
        return value.isEmpty() ? null : ParserUtil.parseNote(value);
    }

    private static Set<Tag> parseTags(ArgumentMultimap arguments) throws ParseException {
        Set<Tag> tags = new HashSet<>();
        for (String value : arguments.getAllValues(PREFIX_TAG)) {
            if (value.isEmpty()) {
                tags.clear();
                continue;
            }
            Tag tag = ParserUtil.parseTag(value);
            if (tags.stream().noneMatch(tag::isSameTag)) {
                tags.add(tag);
            }
        }
        return tags;
    }
}
