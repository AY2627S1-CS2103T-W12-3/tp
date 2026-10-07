package coordimate.logic.parser;

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

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import coordimate.commons.core.index.Index;
import coordimate.logic.commands.EditCommand;
import coordimate.logic.commands.EditCommand.EditPersonDescriptor;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.person.Birthday;
import coordimate.model.person.Note;
import coordimate.model.person.Organisation;
import coordimate.model.person.Role;
import coordimate.model.tag.Tag;

/**
 * Parses input arguments and creates a new EditCommand object.
 */
public class EditCommandParser implements Parser<EditCommand> {

    public static final String MESSAGE_INVALID_INDEX = "Invalid contact index. Example: edit 2 r/President";
    public static final String MESSAGE_REPEATED_PARAMETER = "A parameter may only be specified once.";
    public static final String MESSAGE_UNKNOWN_PARAMETER = "Unknown parameter. Example: edit 2 r/Logistics";

    private static final Pattern PARAMETER_PATTERN = Pattern.compile("(?<!\\S)([A-Za-z]+/)");
    private static final Set<String> ALLOWED_PREFIXES = Set.of("n/", "p/", "e/", "r/", "b/", "a/", "o/", "m/", "t/");

    /**
     * Parses the given {@code String} of arguments in the context of the EditCommand
     * and returns an EditCommand object for execution.
     *
     * @throws ParseException if the user input does not conform to the expected format.
     */
    public EditCommand parse(String args) throws ParseException {
        requireNonNull(args);
        rejectUnknownParameters(args);
        ArgumentMultimap argMultimap =
                ArgumentTokenizer.tokenize(args, PREFIX_NAME, PREFIX_PHONE, PREFIX_EMAIL, PREFIX_ROLE,
                        PREFIX_BIRTHDAY, PREFIX_ADDRESS, PREFIX_ORGANISATION, PREFIX_NOTE, PREFIX_TAG);

        Index index;

        try {
            index = ParserUtil.parseIndex(argMultimap.getPreamble());
        } catch (ParseException pe) {
            throw new ParseException(MESSAGE_INVALID_INDEX, pe);
        }

        rejectRepeatedParameters(argMultimap);

        EditPersonDescriptor editPersonDescriptor = new EditPersonDescriptor();

        if (argMultimap.getValue(PREFIX_NAME).isPresent()) {
            editPersonDescriptor.setName(ParserUtil.parseName(argMultimap.getValue(PREFIX_NAME).get()));
        }
        if (argMultimap.getValue(PREFIX_PHONE).isPresent()) {
            editPersonDescriptor.setPhone(ParserUtil.parsePhone(argMultimap.getValue(PREFIX_PHONE).get()));
        }
        if (argMultimap.getValue(PREFIX_EMAIL).isPresent()) {
            editPersonDescriptor.setEmail(ParserUtil.parseEmail(argMultimap.getValue(PREFIX_EMAIL).get()));
        }
        if (argMultimap.getValue(PREFIX_ROLE).isPresent()) {
            String role = argMultimap.getValue(PREFIX_ROLE).orElseThrow();
            if (!Role.isValidRole(role)) {
                throw new ParseException(Role.MESSAGE_CONSTRAINTS);
            }
            editPersonDescriptor.setRole(new Role(role));
        }
        if (argMultimap.getValue(PREFIX_BIRTHDAY).isPresent()) {
            String birthday = argMultimap.getValue(PREFIX_BIRTHDAY).orElseThrow();
            if (!birthday.isEmpty() && !Birthday.isValidBirthday(birthday)) {
                throw new ParseException(Birthday.MESSAGE_CONSTRAINTS);
            }
            editPersonDescriptor.setBirthday(birthday.isEmpty() ? null : new Birthday(birthday));
        }
        if (argMultimap.getValue(PREFIX_ADDRESS).isPresent()) {
            String address = argMultimap.getValue(PREFIX_ADDRESS).orElseThrow();
            editPersonDescriptor.setAddress(address.isEmpty() ? null : ParserUtil.parseAddress(address));
        }
        if (argMultimap.getValue(PREFIX_ORGANISATION).isPresent()) {
            String organisation = argMultimap.getValue(PREFIX_ORGANISATION).orElseThrow();
            if (!organisation.isEmpty() && !Organisation.isValidOrganisation(organisation)) {
                throw new ParseException(Organisation.MESSAGE_CONSTRAINTS);
            }
            editPersonDescriptor.setOrganisation(organisation.isEmpty() ? null : new Organisation(organisation));
        }
        if (argMultimap.getValue(PREFIX_NOTE).isPresent()) {
            String note = argMultimap.getValue(PREFIX_NOTE).orElseThrow();
            if (!note.isEmpty() && !Note.isValidNote(note)) {
                throw new ParseException(Note.MESSAGE_CONSTRAINTS);
            }
            editPersonDescriptor.setNote(note.isEmpty() ? null : new Note(note));
        }
        parseTagsForEdit(argMultimap.getAllValues(PREFIX_TAG)).ifPresent(editPersonDescriptor::setTags);

        if (!editPersonDescriptor.isAnyFieldEdited()) {
            throw new ParseException(EditCommand.MESSAGE_NOT_EDITED);
        }

        return new EditCommand(index, editPersonDescriptor);
    }

    private static void rejectUnknownParameters(String args) throws ParseException {
        Matcher matcher = PARAMETER_PATTERN.matcher(args);
        while (matcher.find()) {
            if (!ALLOWED_PREFIXES.contains(matcher.group())) {
                throw new ParseException(MESSAGE_UNKNOWN_PARAMETER);
            }
        }
    }

    private static void rejectRepeatedParameters(ArgumentMultimap arguments) throws ParseException {
        if (arguments.getAllValues(PREFIX_NAME).size() > 1 || arguments.getAllValues(PREFIX_PHONE).size() > 1
                || arguments.getAllValues(PREFIX_EMAIL).size() > 1 || arguments.getAllValues(PREFIX_ROLE).size() > 1
                || arguments.getAllValues(PREFIX_BIRTHDAY).size() > 1
                || arguments.getAllValues(PREFIX_ADDRESS).size() > 1
                || arguments.getAllValues(PREFIX_ORGANISATION).size() > 1
                || arguments.getAllValues(PREFIX_NOTE).size() > 1) {
            throw new ParseException(MESSAGE_REPEATED_PARAMETER);
        }
    }

    /**
     * Parses {@code Collection<String> tags} into a {@code Set<Tag>} if {@code tags} is non-empty.
     * If {@code tags} contains only one element which is an empty string, it will be parsed into a
     * {@code Set<Tag>} containing zero tags.
     */
    private Optional<Set<Tag>> parseTagsForEdit(Collection<String> tags) throws ParseException {
        assert tags != null;

        if (tags.isEmpty()) {
            return Optional.empty();
        }
        Collection<String> tagNames = tags.size() == 1 && tags.contains("") ? Collections.emptySet() : tags;
        return Optional.of(ParserUtil.parseTags(tagNames));
    }

}
