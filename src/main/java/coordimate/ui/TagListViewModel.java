package coordimate.ui;

import static java.util.Objects.requireNonNull;

import coordimate.model.tag.Tag;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * Supplies display-ready tag names and a live tag-count label to the Tags view.
 */
public class TagListViewModel {

    private final ObservableList<String> tagNames = FXCollections.observableArrayList();
    private final ObservableList<String> unmodifiableTagNames =
            FXCollections.unmodifiableObservableList(tagNames);
    private final StringBinding tagCountText;

    /**
     * Creates a view model that stays synchronized with {@code tags}.
     */
    public TagListViewModel(ObservableList<Tag> tags) {
        requireNonNull(tags);
        updateTagNames(tags);
        tags.addListener((ListChangeListener<Tag>) change -> updateTagNames(tags));
        tagCountText = Bindings.size(tags).asString("Tags (%d)");
    }

    public ObservableList<String> getTagNames() {
        return unmodifiableTagNames;
    }

    public StringBinding tagCountTextProperty() {
        return tagCountText;
    }

    private void updateTagNames(ObservableList<Tag> tags) {
        tagNames.setAll(tags.stream().map(Tag::getTagName).toList());
    }
}
