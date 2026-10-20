package coordimate.ui;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import coordimate.model.event.AttendanceStatus;
import coordimate.model.event.Event;
import coordimate.model.person.Address;
import coordimate.model.person.Birthday;
import coordimate.model.person.Note;
import coordimate.model.person.Organisation;
import coordimate.model.person.Person;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Shows the displayed contact list and details of the selected contact.
 */
public class PersonListPanel extends UiPart<Region> {
    private static final String FXML = "PersonListPanel.fxml";
    private static final double SPLIT_PANE_DIVIDER_WIDTH = 2;
    private static final String NOT_SPECIFIED = "— (not specified)";

    @FXML
    private SplitPane contactSplitPane;

    @FXML
    private Region contactListSection;

    @FXML
    private Region contactDetailsSection;

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
    private Label selectedAvatar;

    @FXML
    private Label selectedSubtitle;

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

    @FXML
    private Label assignedEventCount;

    @FXML
    private VBox assignedEvents;

    private Person selectedPerson;
    private final ObservableList<Event> events;

    /**
     * Creates a {@code PersonListPanel} with the given {@code ObservableList}.
     */
    public PersonListPanel(ObservableList<Person> persons) {
        this(persons, FXCollections.observableArrayList());
    }

    /**
     * Shows contacts and their assignments from the live event list.
     */
    public PersonListPanel(ObservableList<Person> persons, ObservableList<Event> events) {
        super(FXML);
        this.events = events;
        persons.addListener((ListChangeListener<Person>) change -> {
            if (selectedPerson != null) {
                Person previouslySelected = selectedPerson;
                Person replacement = findReplacement(change, previouslySelected);
                Platform.runLater(() -> restoreSelection(persons, previouslySelected, replacement));
            }
        });
        events.addListener((ListChangeListener<Event>) change -> showDetails(selectedPerson));
        personListView.setItems(persons);
        personListView.setCellFactory(listView -> new PersonListViewCell());
        personListView.setPlaceholder(new Label("No contacts found."));
        contactCount.textProperty().bind(Bindings.size(persons).asString("Records: %d"));
        personListView.getSelectionModel().selectedItemProperty().addListener((observable, oldPerson, person) ->
                showDetails(person));
        contactSplitPane.widthProperty().addListener((observable, oldWidth, width) -> updateOrientation(width));
        showDetails(null);
    }

    private void updateOrientation(Number width) {
        double horizontalMinimum = contactListSection.getMinWidth() + contactDetailsSection.getMinWidth()
                + contactSplitPane.getDividers().size() * SPLIT_PANE_DIVIDER_WIDTH;
        contactSplitPane.setOrientation(width.doubleValue() < horizontalMinimum
                ? Orientation.VERTICAL : Orientation.HORIZONTAL);
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
     * Selects and reveals the next displayed contact. If no contact is selected, selects the first contact.
     * At the end of the list, keeps the last contact selected.
     *
     * @return {@code true} if the list contains a contact and handled the navigation request
     */
    boolean selectNextCard() {
        int itemCount = personListView.getItems().size();
        if (itemCount == 0) {
            return false;
        }

        int selectedIndex = personListView.getSelectionModel().getSelectedIndex();
        int targetIndex = selectedIndex < 0 ? 0 : Math.min(selectedIndex + 1, itemCount - 1);
        selectAndReveal(targetIndex);
        return true;
    }

    /**
     * Selects and reveals the previous displayed contact. If no contact is selected, selects the last contact.
     * At the start of the list, keeps the first contact selected.
     *
     * @return {@code true} if the list contains a contact and handled the navigation request
     */
    boolean selectPreviousCard() {
        int itemCount = personListView.getItems().size();
        if (itemCount == 0) {
            return false;
        }

        int selectedIndex = personListView.getSelectionModel().getSelectedIndex();
        int targetIndex = selectedIndex < 0 ? itemCount - 1 : Math.max(selectedIndex - 1, 0);
        selectAndReveal(targetIndex);
        return true;
    }

    private void selectAndReveal(int targetIndex) {
        personListView.getSelectionModel().select(targetIndex);
        personListView.scrollTo(targetIndex);
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
        selectedAvatar.setText(initials(person.getName().getFullName()));
        String roleText = person.getRole().getValue();
        selectedSubtitle.setText(person.getOrganisation()
                .map(organisation -> roleText + " · " + organisation.getValue()).orElse(roleText));
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
        showAssignedEvents(person);
    }

    private String initials(String fullName) {
        String[] parts = fullName.strip().split("\\s+");
        String result = parts[0].substring(0, 1);
        if (parts.length > 1) {
            result += parts[parts.length - 1].substring(0, 1);
        }
        return result.toUpperCase(Locale.ROOT);
    }

    private void showAssignedEvents(Person person) {
        List<Event> matchingEvents = events.stream()
                .filter(event -> event.hasMember(person.getName())).toList();
        assignedEventCount.setText(matchingEvents.size() + (matchingEvents.size() == 1 ? " event" : " events"));
        assignedEvents.getChildren().clear();
        if (matchingEvents.isEmpty()) {
            Label hint = new Label("No events assigned.");
            hint.getStyleClass().add("detail-caption");
            assignedEvents.getChildren().add(hint);
            return;
        }
        for (int index = 0; index < matchingEvents.size(); index++) {
            Event event = matchingEvents.get(index);
            Label title = new Label((index + 1) + ". " + event.getName());
            title.setWrapText(true);
            title.getStyleClass().add("assigned-event-name");
            HBox.setHgrow(title, Priority.ALWAYS);

            Label time = new Label(event.getStartTime().toString());
            time.getStyleClass().add("detail-caption");
            Label status = createAttendanceBadge(event.getAttendance(person.getName()).orElse(null));
            HBox row = new HBox(10, title, time, status);
            row.getStyleClass().add("assigned-event-row");
            assignedEvents.getChildren().add(row);
        }
    }

    private Label createAttendanceBadge(AttendanceStatus status) {
        String text;
        String style;
        if (status == null) {
            text = "Not marked";
            style = "attendance-unmarked";
        } else if (status == AttendanceStatus.PRESENT) {
            text = "Present";
            style = "attendance-present";
        } else {
            text = "Absent";
            style = "attendance-absent";
        }
        Label badge = new Label(text);
        badge.getStyleClass().addAll("attendance-badge", style);
        return badge;
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
                Region card = new PersonCard(person, getIndex() + 1).getRoot();
                card.prefWidthProperty().bind(widthProperty().subtract(12));
                setGraphic(card);
            }
        }
    }

}
