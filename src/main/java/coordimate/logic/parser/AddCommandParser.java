package coordimate.logic.parser;

import static coordimate.logic.parser.CliSyntax.PREFIX_ADDRESS;
import static coordimate.logic.parser.CliSyntax.PREFIX_BIRTHDAY;
import static coordimate.logic.parser.CliSyntax.PREFIX_EMAIL;
import static coordimate.logic.parser.CliSyntax.PREFIX_LEAVE;
import static coordimate.logic.parser.CliSyntax.PREFIX_NAME;
import static coordimate.logic.parser.CliSyntax.PREFIX_NOTE;
import static coordimate.logic.parser.CliSyntax.PREFIX_ORGANISATION;
import static coordimate.logic.parser.CliSyntax.PREFIX_PHONE;
import static coordimate.logic.parser.CliSyntax.PREFIX_ROLE;
import static coordimate.logic.parser.CliSyntax.PREFIX_TAG;
import static java.util.Objects.requireNonNull;

import java.util.HashSet;
import java.util.Set;

import coordimate.logic.Messages;
import coordimate.logic.commands.AddCommand;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.person.Address;
import coordimate.model.person.Birthday;
import coordimate.model.person.Email;
import coordimate.model.person.LeaveDate;
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
    public static final String MESSAGE_UNKNOWN_PARAMETER =
            Messages.MESSAGE_UNKNOWN_PARAMETER + "\n" + AddCommand.MESSAGE_USAGE;
    public static final String MESSAGE_REPEATED_REQUIRED_PARAMETER =
            Messages.MESSAGE_REPEATED_REQUIRED_PARAMETER + "\n" + AddCommand.MESSAGE_USAGE;

    private static final Set<String> ALLOWED_PREFIXES = Set.of(PREFIX_NAME.toString(), PREFIX_PHONE.toString(),
            PREFIX_EMAIL.toString(), PREFIX_ROLE.toString(), PREFIX_BIRTHDAY.toString(), PREFIX_ADDRESS.toString(),
            PREFIX_ORGANISATION.toString(), PREFIX_LEAVE.toString(), PREFIX_TAG.toString(), PREFIX_NOTE.toString());

    @Override
    public AddCommand parse(String args) throws ParseException {
        requireNonNull(args);
        PrefixParser.rejectUnknownParameters(args, PrefixParser.CONTACT_PARAMETER_PATTERN,
                ALLOWED_PREFIXES, MESSAGE_UNKNOWN_PARAMETER);
        ArgumentMultimap arguments = ArgumentTokenizer.tokenize(args, PREFIX_NAME, PREFIX_PHONE, PREFIX_EMAIL,
                PREFIX_ROLE, PREFIX_BIRTHDAY, PREFIX_ADDRESS, PREFIX_ORGANISATION, PREFIX_LEAVE, PREFIX_TAG,
                PREFIX_NOTE);
        if (!hasRequiredPrefixes(arguments) || !arguments.getPreamble().isEmpty()) {
            throw ParserUtil.createInvalidFormatException(AddCommand.MESSAGE_USAGE);
        }
        rejectRepeatedRequiredParameters(arguments);
        try {
            arguments.verifyNoDuplicatePrefixesFor(PREFIX_BIRTHDAY, PREFIX_ADDRESS, PREFIX_ORGANISATION, PREFIX_LEAVE,
                    PREFIX_NOTE);
        } catch (ParseException e) {
            throw new ParseException(e.getMessage() + "\n" + AddCommand.MESSAGE_USAGE, e);
        }

        Name name = ParserUtil.parseName(arguments.getValue(PREFIX_NAME).orElseThrow());
        Phone phone = ParserUtil.parsePhone(arguments.getValue(PREFIX_PHONE).orElseThrow());
        Email email = ParserUtil.parseEmail(arguments.getValue(PREFIX_EMAIL).orElseThrow());
        Role role = ParserUtil.parseRole(arguments.getValue(PREFIX_ROLE).orElseThrow());
        Birthday birthday = parseBirthday(arguments.getValue(PREFIX_BIRTHDAY).orElse(""));
        Address address = parseAddress(arguments.getValue(PREFIX_ADDRESS).orElse(""));
        Organisation organisation = parseOrganisation(arguments.getValue(PREFIX_ORGANISATION).orElse(""));
        Note note = parseNote(arguments.getValue(PREFIX_NOTE).orElse(""));
        LeaveDate leaveDate = parseLeaveDate(arguments.getValue(PREFIX_LEAVE).orElse(""));
        Set<Tag> tags = parseTags(arguments);
        return new AddCommand(new Person(name, phone, email, role, birthday, address, organisation, note, leaveDate,
                tags));
    }

    private static boolean hasRequiredPrefixes(ArgumentMultimap arguments) {
        return arguments.getValue(PREFIX_NAME).isPresent() && arguments.getValue(PREFIX_PHONE).isPresent()
                && arguments.getValue(PREFIX_EMAIL).isPresent() && arguments.getValue(PREFIX_ROLE).isPresent();
    }

    private static void rejectRepeatedRequiredParameters(ArgumentMultimap arguments) throws ParseException {
        for (Prefix prefix : new Prefix[] {PREFIX_NAME, PREFIX_PHONE, PREFIX_EMAIL, PREFIX_ROLE}) {
            if (arguments.getAllValues(prefix).size() > 1) {
                throw new ParseException(String.format(MESSAGE_REPEATED_REQUIRED_PARAMETER, prefix));
            }
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

    private static LeaveDate parseLeaveDate(String value) throws ParseException {
        return value.isEmpty() ? null : ParserUtil.parseLeaveDate(value);
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
