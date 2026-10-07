package coordimate.ui;

import java.util.Comparator;

import coordimate.model.person.Person;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
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
    private Label address;

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
                Platform.runLater(() -> restoreSelection(persons, previouslySelected));
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
    private void restoreSelection(ObservableList<Person> persons, Person previouslySelected) {
        if (persons.contains(previouslySelected)) {
            personListView.getSelectionModel().select(previouslySelected);
            selectedIndex.setText("Index: " + (persons.indexOf(previouslySelected) + 1));
        } else {
            personListView.getSelectionModel().clearSelection();
        }
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
        address.setText(person.getAddress().getValue());
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
