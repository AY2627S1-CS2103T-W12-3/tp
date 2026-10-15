package coordimate.ui;

import static coordimate.testutil.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import coordimate.model.person.Person;
import coordimate.model.tag.Tag;
import coordimate.testutil.PersonBuilder;
import coordimate.testutil.TypicalPersons;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class TagListViewModelTest {

    @Test
    public void constructor_nullTags_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new TagListViewModel(null, FXCollections.observableArrayList()));
        assertThrows(NullPointerException.class, () -> new TagListViewModel(
                FXCollections.observableArrayList(), null));
    }

    @Test
    public void constructor_tags_exposesNamesAndCount() {
        ObservableList<Tag> tags = FXCollections.observableArrayList(
                new Tag("EXCO"), new Tag("Sponsor"));

        TagListViewModel viewModel = new TagListViewModel(tags, FXCollections.observableArrayList());

        assertEquals(List.of("EXCO", "Sponsor"), viewModel.getTagNames());
        assertEquals("Tags (2)", viewModel.tagCountTextProperty().get());
    }

    @Test
    public void sourceListChanges_updatesNamesAndCount() {
        ObservableList<Tag> tags = FXCollections.observableArrayList(new Tag("EXCO"));
        TagListViewModel viewModel = new TagListViewModel(tags, FXCollections.observableArrayList());

        tags.add(new Tag("Publicity"));

        assertEquals(List.of("EXCO", "Publicity"), viewModel.getTagNames());
        assertEquals("Tags (2)", viewModel.tagCountTextProperty().get());
    }

    @Test
    public void getTagNames_modifyList_throwsUnsupportedOperationException() {
        TagListViewModel viewModel = new TagListViewModel(
                FXCollections.observableArrayList(new Tag("EXCO")), FXCollections.observableArrayList());

        assertThrows(UnsupportedOperationException.class, () -> viewModel.getTagNames().remove(0));
    }

    @Test
    public void selectTag_showsAndTracksMatchingContacts() {
        ObservableList<Tag> tags = FXCollections.observableArrayList(new Tag("friends"));
        ObservableList<Person> persons = FXCollections.observableArrayList(
                TypicalPersons.ALICE, TypicalPersons.BENSON, TypicalPersons.CARL);
        TagListViewModel viewModel = new TagListViewModel(tags, persons);

        viewModel.selectTag("FRIENDS");
        assertEquals(List.of("Alice Pauline", "Benson Meier"), viewModel.getMembers().stream()
                .map(person -> person.getName().getFullName()).toList());
        assertEquals("Members (2)", viewModel.memberCountTextProperty().get());

        persons.add(new PersonBuilder().withName("Aaron Tan").withTags("Friends").build());
        assertEquals(List.of("Aaron Tan", "Alice Pauline", "Benson Meier"), viewModel.getMembers().stream()
                .map(person -> person.getName().getFullName()).toList());
        viewModel.selectTag(null);
        assertEquals(List.of(), viewModel.getMembers());
        assertEquals("Members (0)", viewModel.memberCountTextProperty().get());
    }
}
