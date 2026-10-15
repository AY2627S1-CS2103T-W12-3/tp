package coordimate.ui;

import static java.util.Objects.requireNonNull;

import coordimate.model.person.Person;
import coordimate.model.tag.Tag;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * Supplies tag names, members, and live counts to the Tags view.
 */
public class TagListViewModel {

    private final ObservableList<String> tagNames = FXCollections.observableArrayList();
    private final ObservableList<String> unmodifiableTagNames =
            FXCollections.unmodifiableObservableList(tagNames);
    private final ObservableList<Person> members = FXCollections.observableArrayList();
    private final ObservableList<Person> unmodifiableMembers =
            FXCollections.unmodifiableObservableList(members);
    private final StringBinding tagCountText;
    private final StringBinding memberCountText;
    private final ObservableList<Person> persons;
    private String selectedTagName;

    /**
     * Creates a view model that stays synchronized with the saved tags and persons.
     */
    public TagListViewModel(ObservableList<Tag> tags, ObservableList<Person> persons) {
        requireNonNull(tags);
        this.persons = requireNonNull(persons);
        updateTagNames(tags);
        tags.addListener((ListChangeListener<Tag>) change -> updateTagNames(tags));
        persons.addListener((ListChangeListener<Person>) change -> updateMemberNames());
        tagCountText = Bindings.size(tags).asString("Tags (%d)");
        memberCountText = Bindings.size(members).asString("Members (%d)");
    }

    public ObservableList<String> getTagNames() {
        return unmodifiableTagNames;
    }

    public StringBinding tagCountTextProperty() {
        return tagCountText;
    }

    public ObservableList<Person> getMembers() {
        return unmodifiableMembers;
    }

    public StringBinding memberCountTextProperty() {
        return memberCountText;
    }

    /** Selects a tag whose matching contacts should be shown. */
    public void selectTag(String tagName) {
        selectedTagName = tagName;
        updateMemberNames();
    }

    private void updateTagNames(ObservableList<Tag> tags) {
        tagNames.setAll(tags.stream().map(Tag::getTagName).toList());
    }

    private void updateMemberNames() {
        if (selectedTagName == null) {
            members.clear();
            return;
        }
        members.setAll(persons.stream()
                .filter(person -> person.getTags().stream()
                        .anyMatch(tag -> tag.getTagName().equalsIgnoreCase(selectedTagName)))
                .sorted((first, second) -> String.CASE_INSENSITIVE_ORDER.compare(
                        first.getName().getFullName(), second.getName().getFullName()))
                .toList());
    }
}
