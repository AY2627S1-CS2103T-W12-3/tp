package coordimate.testutil;

import java.util.List;
import java.util.stream.IntStream;

/** Unicode White_Space characters plus the additional separators recognised by Java's isWhitespace. */
public final class UnicodeWhitespace {
    public static final List<String> ALL = IntStream.of(
            0x0009, 0x000a, 0x000b, 0x000c, 0x000d, 0x001c, 0x001d, 0x001e, 0x001f,
            0x0020, 0x0085, 0x00a0, 0x1680, 0x2000, 0x2001, 0x2002, 0x2003, 0x2004,
            0x2005, 0x2006, 0x2007, 0x2008, 0x2009, 0x200a, 0x2028, 0x2029, 0x202f,
            0x205f, 0x3000).mapToObj(Character::toString).toList();

    private UnicodeWhitespace() {}
}
