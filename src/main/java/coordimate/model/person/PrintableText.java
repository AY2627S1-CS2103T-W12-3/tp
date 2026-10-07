package coordimate.model.person;

import static java.util.Objects.requireNonNull;

/**
 * Validation shared by contact fields that allow printable Unicode text.
 */
final class PrintableText {

    private PrintableText() {}

    static boolean isValid(String value, int maxLength) {
        requireNonNull(value);
        int length = value.codePointCount(0, value.length());
        return length > 0 && length <= maxLength && !value.isBlank()
                && value.codePoints().allMatch(PrintableText::isPrintable);
    }

    private static boolean isPrintable(int codePoint) {
        int type = Character.getType(codePoint);
        return type != Character.CONTROL && type != Character.SURROGATE
                && type != Character.LINE_SEPARATOR && type != Character.PARAGRAPH_SEPARATOR;
    }
}
