package coordimate.ui;

import static coordimate.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import coordimate.model.tag.Tag;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class TagListViewModelTest {

    @Test
    public void constructor_nullTags_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new TagListViewModel(null));
    }

    @Test
    public void constructor_tags_exposesNamesAndCount() {
        ObservableList<Tag> tags = FXCollections.observableArrayList(
                new Tag("EXCO"), new Tag("Sponsor"));

        TagListViewModel viewModel = new TagListViewModel(tags);

        assertEquals(List.of("EXCO", "Sponsor"), viewModel.getTagNames());
        assertEquals("Tags (2)", viewModel.tagCountTextProperty().get());
    }

    @Test
    public void sourceListChanges_updatesNamesAndCount() {
        ObservableList<Tag> tags = FXCollections.observableArrayList(new Tag("EXCO"));
        TagListViewModel viewModel = new TagListViewModel(tags);

        tags.add(new Tag("Publicity"));

        assertEquals(List.of("EXCO", "Publicity"), viewModel.getTagNames());
        assertEquals("Tags (2)", viewModel.tagCountTextProperty().get());
    }

    @Test
    public void getTagNames_modifyList_throwsUnsupportedOperationException() {
        TagListViewModel viewModel = new TagListViewModel(
                FXCollections.observableArrayList(new Tag("EXCO")));

        assertThrows(UnsupportedOperationException.class, () -> viewModel.getTagNames().remove(0));
    }
}
