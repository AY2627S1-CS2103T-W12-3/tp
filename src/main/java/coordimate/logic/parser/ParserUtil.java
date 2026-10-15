package coordimate.logic.parser;

import static coordimate.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import coordimate.commons.core.index.Index;
import coordimate.commons.util.StringUtil;
import coordimate.logic.Messages;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.person.Address;
import coordimate.model.person.Birthday;
import coordimate.model.person.Email;
import coordimate.model.person.Name;
import coordimate.model.person.Note;
import coordimate.model.person.Organisation;
import coordimate.model.person.Phone;
import coordimate.model.person.Role;
import coordimate.model.tag.Tag;

/**
 * Contains utility methods used for parsing strings in the various *Parser classes.
 */
public class ParserUtil {

    public static final String MESSAGE_INVALID_INDEX = "Index must be a positive integer. Example: 1";

    /**
     * Creates a format error containing the command's usage instructions.
     */
    public static ParseException createInvalidFormatException(String usage) {
        return new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, usage));
    }

    /**
     * Parses {@code oneBasedIndex} into an {@code Index} and returns it. Leading and trailing whitespaces will be
     * trimmed.
     *
     * @throws ParseException if the specified index is invalid (not a non-zero unsigned integer).
     */
    public static Index parseIndex(String oneBasedIndex) throws ParseException {
        String trimmedIndex = oneBasedIndex.trim();
        if (!StringUtil.isNonZeroUnsignedInteger(trimmedIndex)) {
            throw new ParseException(MESSAGE_INVALID_INDEX);
        }
        return Index.fromOneBased(Integer.parseInt(trimmedIndex));
    }

    /**
     * Parses a {@code String name} into a {@code Name}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code name} is invalid.
     */
    public static Name parseName(String name) throws ParseException {
        return parseName(name, CliSyntax.PREFIX_NAME);
    }

    /**
     * Parses a contact or member name with an example for its parameter.
     */
    public static Name parseName(String name, Prefix prefix) throws ParseException {
        requireNonNull(name);
        String trimmedName = name.trim();
        requireValue(trimmedName, prefix.equals(CliSyntax.PREFIX_MEMBER) ? "Member name" : "Name",
                prefix + "Aisha Tan");
        if (!Name.isValidName(trimmedName)) {
            throw new ParseException(Name.MESSAGE_CONSTRAINTS.replace("Example: n/", "Example: " + prefix));
        }
        return new Name(trimmedName);
    }

    /**
     * Parses a {@code String phone} into a {@code Phone}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code phone} is invalid.
     */
    public static Phone parsePhone(String phone) throws ParseException {
        requireNonNull(phone);
        String trimmedPhone = phone.trim();
        requireValue(trimmedPhone, "Phone number", "p/+6591234567");
        if (!Phone.isValidInputPhone(trimmedPhone)) {
            throw new ParseException(Phone.MESSAGE_CONSTRAINTS);
        }
        return new Phone(trimmedPhone);
    }

    /**
     * Parses a {@code String address} into an {@code Address}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code address} is invalid.
     */
    public static Address parseAddress(String address) throws ParseException {
        requireNonNull(address);
        String trimmedAddress = address.trim();
        if (!Address.isValidAddress(trimmedAddress)) {
            throw new ParseException(Address.MESSAGE_CONSTRAINTS);
        }
        return new Address(trimmedAddress);
    }

    /**
     * Parses a {@code String email} into an {@code Email}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code email} is invalid.
     */
    public static Email parseEmail(String email) throws ParseException {
        requireNonNull(email);
        String trimmedEmail = email.trim();
        requireValue(trimmedEmail, "Email", "e/aish@example.com");
        if (!Email.isValidEmail(trimmedEmail)) {
            throw new ParseException(Email.MESSAGE_CONSTRAINTS);
        }
        return new Email(trimmedEmail);
    }

    /**
     * Parses a {@code String role} into a {@code Role}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code role} is invalid.
     */
    public static Role parseRole(String role) throws ParseException {
        requireNonNull(role);
        String trimmedRole = role.trim();
        requireValue(trimmedRole, "Role", "r/Logistics Lead");
        if (!Role.isValidRole(trimmedRole)) {
            throw new ParseException(Role.MESSAGE_CONSTRAINTS);
        }
        return new Role(trimmedRole);
    }

    /**
     * Parses a {@code String birthday} into a {@code Birthday}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code birthday} is invalid.
     */
    public static Birthday parseBirthday(String birthday) throws ParseException {
        requireNonNull(birthday);
        String trimmedBirthday = birthday.trim();
        if (!Birthday.isValidBirthday(trimmedBirthday)) {
            throw new ParseException(Birthday.MESSAGE_CONSTRAINTS);
        }
        return new Birthday(trimmedBirthday);
    }

    /**
     * Parses a {@code String organisation} into an {@code Organisation}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code organisation} is invalid.
     */
    public static Organisation parseOrganisation(String organisation) throws ParseException {
        requireNonNull(organisation);
        String trimmedOrganisation = organisation.trim();
        if (!Organisation.isValidOrganisation(trimmedOrganisation)) {
            throw new ParseException(Organisation.MESSAGE_CONSTRAINTS);
        }
        return new Organisation(trimmedOrganisation);
    }

    /**
     * Parses a {@code String note} into a {@code Note}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code note} is invalid.
     */
    public static Note parseNote(String note) throws ParseException {
        requireNonNull(note);
        String trimmedNote = note.trim();
        if (!Note.isValidNote(trimmedNote)) {
            throw new ParseException(Note.MESSAGE_CONSTRAINTS);
        }
        return new Note(trimmedNote);
    }

    /**
     * Parses a {@code String eventName} into a trimmed event name.
     *
     * @throws ParseException if the given {@code eventName} is empty or otherwise invalid.
     */
    public static String parseEventName(String eventName) throws ParseException {
        return parseEventName(eventName, CliSyntax.PREFIX_EVENT_NAME);
    }

    /**
     * Parses an event name with an example for the parameter being entered.
     */
    public static String parseEventName(String eventName, Prefix prefix) throws ParseException {
        requireNonNull(eventName);
        String trimmedEventName = eventName.strip();
        String field = prefix.equals(CliSyntax.PREFIX_NEW_EVENT_NAME) ? "New event name" : "Event name";
        requireValue(trimmedEventName, field, prefix + "Final Concert");
        if (trimmedEventName.codePointCount(0, trimmedEventName.length()) > Event.MAX_NAME_LENGTH) {
            throw new ParseException(eventNameMessage(Event.MESSAGE_NAME_LENGTH_CONSTRAINTS, prefix));
        }
        if (!Event.isValidName(trimmedEventName)) {
            throw new ParseException(eventNameMessage(Event.MESSAGE_NAME_CONSTRAINTS, prefix));
        }
        return trimmedEventName;
    }

    private static String eventNameMessage(String message, Prefix prefix) {
        return message.replace("Example: evn/", "Example: " + prefix);
    }

    /**
     * Rejects an empty value with the field's name and an applicable example.
     */
    public static void requireValue(String value, String field, String example) throws ParseException {
        if (value.isBlank()) {
            throw new ParseException(String.format(Messages.MESSAGE_EMPTY_FIELD, field, example));
        }
    }

    /**
     * Parses a start or end time with a field-specific error and example.
     */
    public static EventTime parseEventTime(String value, Prefix prefix) throws ParseException {
        requireNonNull(value);
        boolean isEnd = prefix.equals(CliSyntax.PREFIX_END_TIME);
        requireValue(value, isEnd ? "End time" : "Start time",
                prefix + (isEnd ? "17-09-2026 18:30" : "17-09-2026 16:30"));
        try {
            return new EventTime(value);
        } catch (IllegalArgumentException e) {
            throw new ParseException(e.getMessage().replace("Example: st/", "Example: " + prefix), e);
        }
    }

    /**
     * Parses a {@code String tag} into a {@code Tag}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code tag} is invalid.
     */
    public static Tag parseTag(String tag) throws ParseException {
        return parseTag(tag, CliSyntax.PREFIX_TAG);
    }

    /**
     * Parses a tag with an example for a replacement, addition, or removal.
     */
    public static Tag parseTag(String tag, Prefix prefix) throws ParseException {
        requireNonNull(tag);
        String trimmedTag = tag.trim();
        requireValue(trimmedTag, "Tag name", prefix + "Publicity");
        if (trimmedTag.length() > Tag.MAX_LENGTH) {
            throw new ParseException(Tag.MESSAGE_LENGTH_CONSTRAINTS.replace("Example: t/", "Example: " + prefix));
        }
        if (!Tag.isValidInputTagName(trimmedTag)) {
            throw new ParseException(Tag.MESSAGE_CONSTRAINTS.replace("Example: t/", "Example: " + prefix));
        }
        return new Tag(trimmedTag);
    }

    /**
     * Parses {@code Collection<String> tags} into a {@code Set<Tag>}.
     */
    public static Set<Tag> parseTags(Collection<String> tags) throws ParseException {
        requireNonNull(tags);
        final Set<Tag> parsedTags = new HashSet<>();
        for (String tagName : tags) {
            parsedTags.add(parseTag(tagName));
        }
        return parsedTags;
    }
}
