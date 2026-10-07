package coordimate.logic.parser;

import static coordimate.logic.parser.CliSyntax.PREFIX_ADDRESS;
import static coordimate.logic.parser.CliSyntax.PREFIX_ADD_TAG;
import static coordimate.logic.parser.CliSyntax.PREFIX_BIRTHDAY;
import static coordimate.logic.parser.CliSyntax.PREFIX_EMAIL;
import static coordimate.logic.parser.CliSyntax.PREFIX_NAME;
import static coordimate.logic.parser.CliSyntax.PREFIX_NOTE;
import static coordimate.logic.parser.CliSyntax.PREFIX_ORGANISATION;
import static coordimate.logic.parser.CliSyntax.PREFIX_PHONE;
import static coordimate.logic.parser.CliSyntax.PREFIX_REMOVE_TAG;
import static coordimate.logic.parser.CliSyntax.PREFIX_ROLE;
import static coordimate.logic.parser.CliSyntax.PREFIX_TAG;
import static coordimate.logic.parser.CliSyntax.PREFIX_TARGET;
import static java.util.Objects.requireNonNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import coordimate.commons.core.index.Index;
import coordimate.logic.commands.EditCommand;
import coordimate.logic.commands.EditCommand.EditPersonDescriptor;
import coordimate.logic.commands.EditCommand.TagOperation;
import coordimate.logic.commands.EditCommand.TagOperationType;
import coordimate.logic.parser.ArgumentMultimap.PrefixedValue;
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
    public static final String MESSAGE_MULTIPLE_IDENTIFIERS =
            "Specify either a contact index or target/IDENTIFIER, not both.";
    public static final String MESSAGE_EMPTY_TARGET =
            "Contact identifier must not be empty. Example: target/aisha@example.com";
    public static final String MESSAGE_CONFLICTING_TAG_OPERATIONS =
            "Use either t/ to replace tags, or at/ and rt/ to add or remove them, not both.";

    private static final Pattern PARAMETER_PATTERN = Pattern.compile("(?<!\\S)([A-Za-z]+/)");
    private static final Set<String> ALLOWED_PREFIXES =
            Set.of("n/", "p/", "e/", "r/", "b/", "a/", "o/", "m/", "t/", "at/", "rt/", "target/");

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
                ArgumentTokenizer.tokenize(" " + args, PREFIX_NAME, PREFIX_PHONE, PREFIX_EMAIL, PREFIX_ROLE,
                        PREFIX_BIRTHDAY, PREFIX_ADDRESS, PREFIX_ORGANISATION, PREFIX_NOTE,
                        PREFIX_TAG, PREFIX_ADD_TAG, PREFIX_REMOVE_TAG, PREFIX_TARGET);

        rejectRepeatedParameters(argMultimap);
        Index index = null;
        String targetIdentifier = null;
        if (argMultimap.getValue(PREFIX_TARGET).isPresent()) {
            if (!argMultimap.getPreamble().isEmpty()) {
                throw new ParseException(MESSAGE_MULTIPLE_IDENTIFIERS);
            }
            targetIdentifier = argMultimap.getValue(PREFIX_TARGET).orElseThrow();
            if (targetIdentifier.isEmpty()) {
                throw new ParseException(MESSAGE_EMPTY_TARGET);
            }
        } else {
            try {
                index = ParserUtil.parseIndex(argMultimap.getPreamble());
            } catch (ParseException pe) {
                throw new ParseException(MESSAGE_INVALID_INDEX, pe);
            }
        }

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
        parseTagEdits(argMultimap, editPersonDescriptor);

        if (!editPersonDescriptor.isAnyFieldEdited()) {
            throw new ParseException(EditCommand.MESSAGE_NOT_EDITED);
        }

        return targetIdentifier == null
                ? new EditCommand(index, editPersonDescriptor)
                : new EditCommand(targetIdentifier, editPersonDescriptor);
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
                || arguments.getAllValues(PREFIX_NOTE).size() > 1
                || arguments.getAllValues(PREFIX_TARGET).size() > 1) {
            throw new ParseException(MESSAGE_REPEATED_PARAMETER);
        }
    }

    private static void parseTagEdits(ArgumentMultimap arguments, EditPersonDescriptor descriptor)
            throws ParseException {
        List<String> replacements = arguments.getAllValues(PREFIX_TAG);
        if (!replacements.isEmpty()
                && (!arguments.getAllValues(PREFIX_ADD_TAG).isEmpty()
                || !arguments.getAllValues(PREFIX_REMOVE_TAG).isEmpty())) {
            throw new ParseException(MESSAGE_CONFLICTING_TAG_OPERATIONS);
        }
        if (!replacements.isEmpty()) {
            Set<Tag> replacementTags = new HashSet<>();
            for (String value : replacements) {
                if (value.isEmpty()) {
                    replacementTags.clear();
                    continue;
                }
                Tag tag = ParserUtil.parseTag(value);
                if (replacementTags.stream().noneMatch(tag::isSameTag)) {
                    replacementTags.add(tag);
                }
            }
            descriptor.setTags(replacementTags);
        }
        for (PrefixedValue argument : arguments.getArgumentsInOrder()) {
            if (argument.prefix().equals(PREFIX_ADD_TAG)) {
                descriptor.addTagOperation(
                        new TagOperation(TagOperationType.ADD, ParserUtil.parseTag(argument.value())));
            } else if (argument.prefix().equals(PREFIX_REMOVE_TAG)) {
                descriptor.addTagOperation(
                        new TagOperation(TagOperationType.REMOVE, ParserUtil.parseTag(argument.value())));
            }
        }
    }

}
