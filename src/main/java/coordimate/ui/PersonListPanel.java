package coordimate.ui;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

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
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Shows the displayed contact list and details of the selected contact.
 */
public class PersonListPanel extends UiPart<Region> {
    static final String MESSAGE_NO_CONTACTS = "No contacts found.";
    static final String MESSAGE_NO_ARCHIVED_CONTACTS = "No archived contacts.";
    private static final String FXML = "PersonListPanel.fxml";
    private static final double SPLIT_PANE_DIVIDER_WIDTH = 2;
    private static final String NOT_SPECIFIED = "— (not specified)";
    private static final String NOT_ON_LEAVE = "— (not on leave)";

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
    private Label selectedMemberId;

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
    private Label leave;

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
    private final Clock clock;
    private LocalDate shownDate;
    private long focusRequestVersion;
    private final Label emptyListMessage = new Label(MESSAGE_NO_CONTACTS);

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
        this(persons, events, Clock.systemDefaultZone());
    }

    /**
     * Shows contacts and their assignments, using {@code clock} to decide whether a contact is on leave.
     */
    PersonListPanel(ObservableList<Person> persons, ObservableList<Event> events, Clock clock) {
        super(FXML);
        this.events = events;
        this.clock = Objects.requireNonNull(clock);
        shownDate = LocalDate.now(clock);
        persons.addListener((ListChangeListener<Person>) change -> {
            if (selectedPerson != null) {
                Person previouslySelected = selectedPerson;
                long versionAtChange = focusRequestVersion;
                Platform.runLater(() -> restoreSelection(persons, previouslySelected, versionAtChange));
            }
        });
        events.addListener((ListChangeListener<Event>) change -> showDetails(selectedPerson));
        personListView.setItems(persons);
        personListView.setCellFactory(listView -> new PersonListViewCell());
        personListView.setPlaceholder(emptyListMessage);
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
    private void restoreSelection(ObservableList<Person> persons,
            Person previouslySelected, long versionAtChange) {
        if (versionAtChange != focusRequestVersion) {
            return;
        }

        Person current = persons.stream()
                .filter(person -> person.getMemberId().equals(previouslySelected.getMemberId()))
                .findFirst()
                .orElse(null);
        personListView.getSelectionModel().select(current);
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

    /** Selects a displayed contact and reveals it in the list. */
    boolean selectContact(Person contact) {
        Objects.requireNonNull(contact);
        return selectContactAt(personListView.getItems().indexOf(contact));
    }

    /** Selects the row at the supplied displayed index, clamping to the last row after a deletion. */
    boolean selectContactAt(int targetIndex) {
        if (targetIndex < 0) {
            return false;
        }
        focusRequestVersion++;
        int itemCount = personListView.getItems().size();
        if (itemCount == 0) {
            personListView.getSelectionModel().clearSelection();
            return false;
        }
        selectAndReveal(Math.min(targetIndex, itemCount - 1));
        return true;
    }

    /**
     * Redraws the contact cards and selected contact's details if the date has changed since they were last
     * shown, so that leave which has just ended is no longer displayed.
     */
    void refreshIfDateChanged() {
        LocalDate today = LocalDate.now(clock);
        if (today.equals(shownDate)) {
            return;
        }
        shownDate = today;
        personListView.refresh();
        showDetails(selectedPerson);
    }

    /**
     * Shows an empty-list message suited to the archived view if {@code isShowingArchived} is true.
     */
    void setShowingArchived(boolean isShowingArchived) {
        emptyListMessage.setText(isShowingArchived ? MESSAGE_NO_ARCHIVED_CONTACTS : MESSAGE_NO_CONTACTS);
    }

    /** Runs the given action whenever the displayed contact list is clicked. */
    void setCardListClickHandler(Runnable handler) {
        Objects.requireNonNull(handler);
        personListView.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> handler.run());
    }

    /** Returns the first displayed contact card, or the list area when there are no contacts. */
    Node getTutorialContactCardTarget() {
        if (personListView.getItems().isEmpty()) {
            return contactListSection;
        }
        if (personListView.getSelectionModel().isEmpty()) {
            personListView.getSelectionModel().selectFirst();
        }
        personListView.scrollTo(personListView.getSelectionModel().getSelectedIndex());
        personListView.applyCss();
        personListView.layout();
        int selectedIndex = personListView.getSelectionModel().getSelectedIndex();
        return personListView.lookupAll(".list-cell").stream()
                .filter(node -> node instanceof ListCell<?> cell && !cell.isEmpty() && cell.isVisible()
                        && cell.getIndex() == selectedIndex)
                .findFirst()
                .orElse(contactListSection);
    }

    /** Returns the pane containing the selected contact's details. */
    Region getTutorialContactDetailsTarget() {
        return contactDetailsSection;
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
            selectedMemberId.setText("");
            return;
        }

        selectedName.setText(person.getName().getFullName());
        selectedMemberId.setText("ID: " + person.getMemberId().orElseThrow());
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
        leave.setText(person.isOnLeave(LocalDate.now(clock))
                ? person.getLeaveDate().orElseThrow().getValue() : NOT_ON_LEAVE);
        detailTags.getChildren().clear();
        person.getTags().stream()
                .sorted(Comparator.comparing(tag -> tag.getTagName()))
                .forEach(tag -> detailTags.getChildren().add(TagStyle.createTagLabel(tag.getTagName())));
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
            Label title = new Label((index + 1) + ". " + event.getName()
                    + (event.isArchived() ? " (archived)" : ""));
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
                Region card = new PersonCard(person, LocalDate.now(clock)).getRoot();
                card.prefWidthProperty().bind(widthProperty().subtract(12));
                setGraphic(card);
            }
        }
    }

}
