package coordimate.commons.util;

import java.util.regex.Pattern;

/** Parameter boundaries shared by command parsing and legacy event cleanup. */
public final class ParameterSyntax {
    public static final String BOUNDARY = "(?<![^" + StringUtil.WHITESPACE_CHARACTERS + "])";
    public static final Pattern EVENT_PARAMETER_PATTERN = Pattern.compile(BOUNDARY + "([A-Za-z]+/|/[A-Za-z]+)");

    private ParameterSyntax() {}

    /**
     * Detects parameters inside a saved name after the same whitespace normalization used when loading it.
     * A prefix at the start of the name is safe because commands place it directly after {@code evn/}.
     */
    public static boolean hasEmbeddedEventParameter(String name) {
        return EVENT_PARAMETER_PATTERN.matcher(StringUtil.normalizeWhitespace(name)).results()
                .anyMatch(match -> match.start() > 0);
    }
}
