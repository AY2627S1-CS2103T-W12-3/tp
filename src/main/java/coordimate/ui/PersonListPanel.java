package coordimate.ui;

import java.util.Comparator;

import coordimate.model.person.Address;
import coordimate.model.person.Birthday;
import coordimate.model.person.Note;
import coordimate.model.person.Organisation;
import coordimate.model.person.Person;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;

/**
 * Shows the displayed contact list and details of the selected contact.
 */
public class PersonListPanel extends UiPart<Region> {
    private static final String FXML = "PersonListPanel.fxml";
    private static final double VERTICAL_LAYOUT_WIDTH = 560;
    private static final String NOT_SPECIFIED = "— (not specified)";

    @FXML
    private SplitPane contactSplitPane;

    @FXML
    private ListView<Person> personListView;

    @FXML
    private Label contactCount;

    @FXML
    private Label selectionHint;

    @FXML
    private ScrollPane detailsScroll;

    @FXML
    private Label selectedName;

    @FXML
    private Label selectedIndex;

    @FXML
    private Label phone;

    @FXML
    private Label email;

    @FXML
    private Label role;

    @FXML
    private Label birthday;

    @FXML
    private Label address;

    @FXML
    private Label organisation;

    @FXML
    private Label note;

    @FXML
    private FlowPane detailTags;

    private Person selectedPerson;

    /**
     * Creates a {@code PersonListPanel} with the given {@code ObservableList}.
     */
    public PersonListPanel(ObservableList<Person> persons) {
        super(FXML);
        persons.addListener((ListChangeListener<Person>) change -> {
            if (selectedPerson != null) {
                Person previouslySelected = selectedPerson;
                Person replacement = findReplacement(change, previouslySelected);
                Platform.runLater(() -> restoreSelection(persons, previouslySelected, replacement));
            }
        });
        personListView.setItems(persons);
        personListView.setCellFactory(listView -> new PersonListViewCell());
        personListView.setPlaceholder(new Label("No contacts found."));
        contactCount.textProperty().bind(Bindings.size(persons).asString("Records: %d"));
        personListView.getSelectionModel().selectedItemProperty().addListener((observable, oldPerson, person) ->
                showDetails(person));
        contactSplitPane.widthProperty().addListener((observable, oldWidth, width) ->
                contactSplitPane.setOrientation(width.doubleValue() < VERTICAL_LAYOUT_WIDTH
                        ? Orientation.VERTICAL : Orientation.HORIZONTAL));
        showDetails(null);
    }

    /**
     * Reconciles selection after the list view has processed a change to its items.
     */
    private Person findReplacement(ListChangeListener.Change<? extends Person> change, Person previouslySelected) {
        while (change.next()) {
            if (change.wasReplaced() && change.getRemovedSize() == change.getAddedSize()) {
                int offset = change.getRemoved().indexOf(previouslySelected);
                if (offset >= 0 && offset < change.getAddedSize()) {
                    return change.getAddedSubList().get(offset);
                }
            }
        }
        return null;
    }

    private void restoreSelection(ObservableList<Person> persons, Person previouslySelected, Person replacement) {
        if (persons.contains(previouslySelected)) {
            personListView.getSelectionModel().select(previouslySelected);
            selectedIndex.setText("Index: " + (persons.indexOf(previouslySelected) + 1));
        } else if (replacement != null && !isStillInSource(persons, previouslySelected)
                && persons.contains(replacement)) {
            personListView.getSelectionModel().select(replacement);
        } else {
            personListView.getSelectionModel().clearSelection();
        }
    }

    private boolean isStillInSource(ObservableList<Person> persons, Person previouslySelected) {
        return persons instanceof FilteredList<?> filteredPersons
                && filteredPersons.getSource().contains(previouslySelected);
    }

    /**
     * Shows the selected contact's current details or a hint when no contact is selected.
     */
    private void showDetails(Person person) {
        selectedPerson = person;
        boolean isSelected = person != null;
        detailsScroll.setVisible(isSelected);
        detailsScroll.setManaged(isSelected);
        selectionHint.setVisible(!isSelected);
        selectionHint.setManaged(!isSelected);
        if (!isSelected) {
            return;
        }

        selectedName.setText(person.getName().getFullName());
        selectedIndex.setText("Index: " + (personListView.getItems().indexOf(person) + 1));
        phone.setText(person.getPhone().getValue());
        email.setText(person.getEmail().getValue());
        role.setText(person.getRole().getValue());
        birthday.setText(person.getBirthday().map(Birthday::getValue).orElse(NOT_SPECIFIED));
        address.setText(person.getAddress().map(Address::getValue).orElse(NOT_SPECIFIED));
        organisation.setText(person.getOrganisation().map(Organisation::getValue).orElse(NOT_SPECIFIED));
        note.setText(person.getNote().map(Note::getValue).orElse(NOT_SPECIFIED));
        detailTags.getChildren().clear();
        person.getTags().stream()
                .sorted(Comparator.comparing(tag -> tag.getTagName()))
                .forEach(tag -> detailTags.getChildren().add(new Label(tag.getTagName())));
    }

    /**
     * Custom {@code ListCell} that displays the graphics of a {@code Person} using a {@code PersonCard}.
     */
    class PersonListViewCell extends ListCell<Person> {
        @Override
        protected void updateItem(Person person, boolean isEmpty) {
            super.updateItem(person, isEmpty);

            if (isEmpty || person == null) {
                setGraphic(null);
                setText(null);
            } else {
                setGraphic(new PersonCard(person, getIndex() + 1).getRoot());
            }
        }
    }

}
