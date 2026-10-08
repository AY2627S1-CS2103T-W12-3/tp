package coordimate.model.person;

import java.util.Arrays;
import java.util.Optional;

/**
 * The fields a {@code filter} command can filter contacts by.
 */
public enum FilterField {
    TAG("tag"),
    EVENT("event");

    private final String fieldName;

    FilterField(String fieldName) {
        this.fieldName = fieldName;
    }

    /**
     * Returns the field matching {@code fieldName}, ignoring case, or empty if none matches.
     */
    public static Optional<FilterField> fromFieldName(String fieldName) {
        return Arrays.stream(values()).filter(field -> field.fieldName.equalsIgnoreCase(fieldName)).findFirst();
    }
}
