package coordimate.ui;

import java.util.Locale;

import javafx.scene.control.Label;

/**
 * Creates tag labels with colors based on CoordiMate's built-in tag categories.
 */
final class TagStyle {

    private TagStyle() {
    }

    /**
     * Creates a styled label for a tag name.
     *
     * @param tagName name of the tag
     * @return a label styled with the tag's color
     */
    static Label createTagLabel(String tagName) {
        Label tag = new Label(tagName);
        tag.getStyleClass().addAll("tag-chip", getColorClass(tagName));
        return tag;
    }

    private static String getColorClass(String tagName) {
        return switch (tagName.toLowerCase(Locale.ROOT)) {
            case "exco" -> "tag-chip-red";
            case "logistics" -> "tag-chip-teal";
            case "sponsor" -> "tag-chip-amber";
            case "universitystaff" -> "tag-chip-purple";
            default -> "tag-chip-blue";
        };
    }
}
