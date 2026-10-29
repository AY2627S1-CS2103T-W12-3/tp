package coordimate.ui;

import java.util.Locale;

import coordimate.model.person.Person;
import coordimate.model.tag.Tag;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;

/**
 * Builds the member-identity presentation (avatar, name, tag chips, subtitle) shared by the
 * Tags member list and the Events member rows. Each call returns freshly built nodes so a
 * node is never attached to more than one parent.
 */
final class MemberIdentity {

    private static final String AVATAR_STYLE_CLASS = "event-member-avatar";
    private static final String AVATAR_TEAL = "event-member-avatar-teal";
    private static final String AVATAR_AMBER = "event-member-avatar-amber";
    private static final String AVATAR_PURPLE = "event-member-avatar-purple";
    private static final String NAME_STYLE_CLASS = "event-member-name";
    private static final String IDENTITY_LINE_STYLE_CLASS = "event-member-identity";
    private static final String SUBTITLE_STYLE_CLASS = "event-member-metadata";
    private static final String CONTACT_UNAVAILABLE_TEXT = "Contact details unavailable";

    private MemberIdentity() {
    }

    /**
     * Returns the initials for {@code fullName}: the first letter of its first word, plus the
     * first letter of its last word when the name has more than one word.
     */
    static String initials(String fullName) {
        String[] parts = fullName.strip().split("\\s+");
        String result = parts[0].substring(0, 1);
        if (parts.length > 1) {
            result += parts[parts.length - 1].substring(0, 1);
        }
        return result.toUpperCase(Locale.ROOT);
    }

    /**
     * Returns the avatar color class for {@code person}: teal for a Logistics tag, amber for a
     * Sponsor tag (checked in that order, case-insensitively), purple otherwise or when
     * {@code person} is null.
     */
    static String avatarColorClass(Person person) {
        if (person == null) {
            return AVATAR_PURPLE;
        }
        if (person.getTags().stream().anyMatch(tag -> tag.getTagName().equalsIgnoreCase("logistics"))) {
            return AVATAR_TEAL;
        }
        if (person.getTags().stream().anyMatch(tag -> tag.getTagName().equalsIgnoreCase("sponsor"))) {
            return AVATAR_AMBER;
        }
        return AVATAR_PURPLE;
    }

    /**
     * Returns {@code person}'s role, with their organisation appended when present, or a
     * missing-contact fallback message when {@code person} is null.
     */
    static String formatDetails(Person person) {
        if (person == null) {
            return CONTACT_UNAVAILABLE_TEXT;
        }
        StringBuilder details = new StringBuilder(person.getRole().getValue());
        person.getOrganisation().ifPresent(organisation -> details.append(" · ").append(organisation.getValue()));
        return details.toString();
    }

    /** Creates a fresh avatar label showing {@code fullName}'s initials, colored for {@code person}. */
    static Label createAvatar(String fullName, Person person) {
        Label avatar = new Label(initials(fullName));
        avatar.getStyleClass().addAll(AVATAR_STYLE_CLASS, avatarColorClass(person));
        return avatar;
    }

    /** Creates a fresh name label for {@code fullName}, marked archived when {@code isArchived} is true. */
    static Label createNameLabel(String fullName, boolean isArchived) {
        Label name = new Label(fullName + (isArchived ? " (archived)" : ""));
        name.setWrapText(true);
        name.setMinWidth(0);
        name.getStyleClass().add(NAME_STYLE_CLASS);
        return name;
    }

    /**
     * Creates a fresh identity line containing only {@code nameLabel}. Callers may insert
     * further content (such as a member-ID label) before appending tag chips.
     */
    static FlowPane createIdentityLine(Label nameLabel) {
        FlowPane identityLine = new FlowPane(8, 4);
        identityLine.setAlignment(Pos.CENTER_LEFT);
        identityLine.getStyleClass().add(IDENTITY_LINE_STYLE_CLASS);
        identityLine.getChildren().add(nameLabel);
        return identityLine;
    }

    /**
     * Appends {@code person}'s tags, sorted case-insensitively by name, as styled chips to
     * {@code identityLine}. Does nothing when {@code person} is null.
     */
    static void appendTagChips(FlowPane identityLine, Person person) {
        if (person == null) {
            return;
        }
        person.getTags().stream()
                .sorted((left, right) -> String.CASE_INSENSITIVE_ORDER.compare(left.getTagName(), right.getTagName()))
                .map(Tag::getTagName)
                .map(TagStyle::createTagLabel)
                .forEach(identityLine.getChildren()::add);
    }

    /**
     * Creates a fresh subtitle label showing {@code person}'s role/organisation, or the
     * missing-contact fallback when {@code person} is null.
     */
    static Label createSubtitleLabel(Person person) {
        Label subtitle = new Label(formatDetails(person));
        subtitle.setWrapText(true);
        subtitle.setMinWidth(0);
        subtitle.getStyleClass().add(SUBTITLE_STYLE_CLASS);
        return subtitle;
    }
}
