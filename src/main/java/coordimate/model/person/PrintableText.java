package coordimate.model.person;

import static java.util.Objects.requireNonNull;

/**
 * Validation shared by fields that allow printable Unicode text, such as addresses, notes, and event names.
 */
public final class PrintableText {

    private PrintableText() {}

    /**
     * Returns true if {@code value} contains a visible character, has at most {@code maxLength} code points,
     * and contains no control, surrogate, or line-separator characters.
     */
    public static boolean isValid(String value, int maxLength) {
        requireNonNull(value);
        int length = value.codePointCount(0, value.length());
        return length > 0 && length <= maxLength
                && value.codePoints().allMatch(PrintableText::isPrintable)
                && value.codePoints().anyMatch(PrintableText::isVisible);
    }

    private static boolean isPrintable(int codePoint) {
        int type = Character.getType(codePoint);
        return type != Character.CONTROL && type != Character.SURROGATE
                && type != Character.LINE_SEPARATOR && type != Character.PARAGRAPH_SEPARATOR;
    }

    private static boolean isVisible(int codePoint) {
        int type = Character.getType(codePoint);
        return !Character.isWhitespace(codePoint) && type != Character.SPACE_SEPARATOR
                && type != Character.FORMAT && type != Character.NON_SPACING_MARK
                && type != Character.COMBINING_SPACING_MARK && type != Character.ENCLOSING_MARK;
    }
}
