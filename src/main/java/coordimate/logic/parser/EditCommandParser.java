package coordimate.logic.parser;

import static coordimate.logic.parser.CliSyntax.PREFIX_ADDRESS;
import static coordimate.logic.parser.CliSyntax.PREFIX_ADD_TAG;
import static coordimate.logic.parser.CliSyntax.PREFIX_BIRTHDAY;
import static coordimate.logic.parser.CliSyntax.PREFIX_EMAIL;
import static coordimate.logic.parser.CliSyntax.PREFIX_LEAVE;
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

import coordimate.commons.core.index.Index;
import coordimate.logic.Messages;
import coordimate.logic.commands.EditCommand;
import coordimate.logic.commands.EditCommand.EditPersonDescriptor;
import coordimate.logic.commands.EditCommand.TagOperation;
import coordimate.logic.commands.EditCommand.TagOperationType;
import coordimate.logic.parser.ArgumentMultimap.PrefixedValue;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.tag.Tag;

/**
 * Parses input arguments and creates a new EditCommand object.
 */
public class EditCommandParser implements Parser<EditCommand> {

    public static final String MESSAGE_INVALID_INDEX = "Invalid contact index. Example: edit 2 r/President";
    public static final String MESSAGE_REPEATED_PARAMETER =
            Messages.MESSAGE_REPEATED_PARAMETER + "\n" + EditCommand.MESSAGE_USAGE;
    public static final String MESSAGE_UNKNOWN_PARAMETER =
            Messages.MESSAGE_UNKNOWN_PARAMETER + "\n" + EditCommand.MESSAGE_USAGE;
    public static final String MESSAGE_MULTIPLE_IDENTIFIERS =
            "Specify exactly one contact identifier.\n" + EditCommand.MESSAGE_USAGE;
    public static final String MESSAGE_EMPTY_TARGET =
            "Contact identifier must not be empty. Example: target/aisha@example.com";
    public static final String MESSAGE_CONFLICTING_TAG_OPERATIONS =
            "Use either t/ to replace tags, or at/ and rt/ to add or remove them.\n" + EditCommand.MESSAGE_USAGE;

    private static final Set<String> ALLOWED_PREFIXES = Set.of(PREFIX_NAME.toString(), PREFIX_PHONE.toString(),
            PREFIX_EMAIL.toString(), PREFIX_ROLE.toString(), PREFIX_BIRTHDAY.toString(), PREFIX_ADDRESS.toString(),
            PREFIX_ORGANISATION.toString(), PREFIX_NOTE.toString(), PREFIX_LEAVE.toString(), PREFIX_TAG.toString(),
            PREFIX_ADD_TAG.toString(), PREFIX_REMOVE_TAG.toString(), PREFIX_TARGET.toString());

    /**
     * Parses the given {@code String} of arguments in the context of the EditCommand
     * and returns an EditCommand object for execution.
     *
     * @throws ParseException if the user input does not conform to the expected format.
     */
    public EditCommand parse(String args) throws ParseException {
        requireNonNull(args);
        if (args.isBlank()) {
            throw ParserUtil.createInvalidFormatException(EditCommand.MESSAGE_USAGE);
        }
        PrefixParser.rejectUnknownParameters(args, PrefixParser.CONTACT_PARAMETER_PATTERN,
                ALLOWED_PREFIXES, MESSAGE_UNKNOWN_PARAMETER);
        ArgumentMultimap argMultimap =
                ArgumentTokenizer.tokenize(" " + args, PREFIX_NAME, PREFIX_PHONE, PREFIX_EMAIL, PREFIX_ROLE,
                        PREFIX_BIRTHDAY, PREFIX_ADDRESS, PREFIX_ORGANISATION, PREFIX_NOTE, PREFIX_LEAVE,
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
            editPersonDescriptor.setRole(ParserUtil.parseRole(argMultimap.getValue(PREFIX_ROLE).get()));
        }
        if (argMultimap.getValue(PREFIX_BIRTHDAY).isPresent()) {
            String birthday = argMultimap.getValue(PREFIX_BIRTHDAY).orElseThrow();
            editPersonDescriptor.setBirthday(birthday.isEmpty() ? null : ParserUtil.parseBirthday(birthday));
        }
        if (argMultimap.getValue(PREFIX_ADDRESS).isPresent()) {
            String address = argMultimap.getValue(PREFIX_ADDRESS).orElseThrow();
            editPersonDescriptor.setAddress(address.isEmpty() ? null : ParserUtil.parseAddress(address));
        }
        if (argMultimap.getValue(PREFIX_ORGANISATION).isPresent()) {
            String organisation = argMultimap.getValue(PREFIX_ORGANISATION).orElseThrow();
            editPersonDescriptor.setOrganisation(
                    organisation.isEmpty() ? null : ParserUtil.parseOrganisation(organisation));
        }
        if (argMultimap.getValue(PREFIX_NOTE).isPresent()) {
            String note = argMultimap.getValue(PREFIX_NOTE).orElseThrow();
            editPersonDescriptor.setNote(note.isEmpty() ? null : ParserUtil.parseNote(note));
        }
        if (argMultimap.getValue(PREFIX_LEAVE).isPresent()) {
            String leaveDate = argMultimap.getValue(PREFIX_LEAVE).orElseThrow();
            editPersonDescriptor.setLeaveDate(leaveDate.isEmpty() ? null : ParserUtil.parseLeaveDate(leaveDate));
        }
        parseTagEdits(argMultimap, editPersonDescriptor);

        if (!editPersonDescriptor.isAnyFieldEdited()) {
            throw new ParseException(EditCommand.MESSAGE_NOT_EDITED);
        }

        return targetIdentifier == null
                ? new EditCommand(index, editPersonDescriptor)
                : new EditCommand(targetIdentifier, editPersonDescriptor);
    }

    private static void rejectRepeatedParameters(ArgumentMultimap arguments) throws ParseException {
        for (Prefix prefix : new Prefix[] {PREFIX_NAME, PREFIX_PHONE, PREFIX_EMAIL, PREFIX_ROLE,
            PREFIX_BIRTHDAY, PREFIX_ADDRESS, PREFIX_ORGANISATION, PREFIX_NOTE, PREFIX_LEAVE, PREFIX_TARGET}) {
            if (arguments.getAllValues(prefix).size() > 1) {
                throw new ParseException(String.format(MESSAGE_REPEATED_PARAMETER, prefix));
            }
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
                        new TagOperation(TagOperationType.ADD, ParserUtil.parseTag(argument.value(),
                                argument.prefix())));
            } else if (argument.prefix().equals(PREFIX_REMOVE_TAG)) {
                descriptor.addTagOperation(
                        new TagOperation(TagOperationType.REMOVE, ParserUtil.parseTag(argument.value(),
                                argument.prefix())));
            }
        }
    }

}
