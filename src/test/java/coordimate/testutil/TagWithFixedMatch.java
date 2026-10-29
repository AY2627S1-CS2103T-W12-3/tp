package coordimate.testutil;

import coordimate.model.tag.Tag;

/**
 * A tag that reports the same {@link Tag#isSameTag} result for every other tag, so tests can break tag-list
 * invariants and check that the model's assertions catch it.
 */
public class TagWithFixedMatch extends Tag {

    private final boolean isSameTagResult;

    /**
     * Creates a tag named {@code tagName} whose {@code isSameTag} always returns {@code isSameTagResult}.
     */
    public TagWithFixedMatch(String tagName, boolean isSameTagResult) {
        super(tagName);
        this.isSameTagResult = isSameTagResult;
    }

    @Override
    public boolean isSameTag(Tag otherTag) {
        return isSameTagResult;
    }
}
