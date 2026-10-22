package coordimate.model.person;

import static coordimate.commons.util.AppUtil.checkArgument;

/**
 * A permanent member identifier, independent of the member's position in a list.
 */
public final class MemberId {
    public static final String MESSAGE_CONSTRAINTS = "Member ID must be a positive integer.";

    private final int value;

    /**
     * Creates an ID from a positive integer.
     */
    public MemberId(int value) {
        checkArgument(value > 0, MESSAGE_CONSTRAINTS);
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof MemberId otherId && value == otherId.value;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(value);
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
