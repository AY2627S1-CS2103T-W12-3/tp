package coordimate.ui;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;

import coordimate.model.event.AttendanceStatus;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.person.Name;
import coordimate.model.person.Person;
import coordimate.model.tag.Tag;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Keeps event selection and details connected to the live model list.
 */
public class EventListPanel extends UiPart<Region> {
    static final String MESSAGE_NO_EVENTS = "No events yet. Create one with addevent.";
    static final String MESSAGE_NO_ARCHIVED_EVENTS = "No archived events.";
    private static final String FXML = "EventListPanel.fxml";

    @FXML
    private SplitPane eventSplitPane;

    @FXML
    private ScrollPane detailsScroll;

    @FXML
    private ListView<Event> eventListView;

    @FXML
    private Label eventCount;

    @FXML
    private Label selectedName;

    @FXML
    private VBox eventDetails;

    @FXML
    private Label startTime;

    @FXML
    private Label endTime;

    @FXML
    private Label duration;

    @FXML
    private Label memberCount;

    @FXML
    private Label presentCount;

    @FXML
    private Label absentCount;

    @FXML
    private Label notMarkedCount;

    @FXML
    private VBox members;

    @FXML
    private Label selectionHint;

    private int lastSelectedIndex = -1;
    private final ObservableList<Person> persons;
    private final Label emptyListMessage = new Label(MESSAGE_NO_EVENTS);
    private final BooleanProperty showingArchived = new SimpleBooleanProperty(false);

    /**
     * Observes saved events and stacks the panes when the window is narrow.
     */
    public EventListPanel(ObservableList<Event> events) {
        this(events, javafx.collections.FXCollections.observableArrayList());
    }

    /**
     * Observes events and contacts so assigned members show their saved contact details.
     */
    public EventListPanel(ObservableList<Event> events, ObservableList<Person> persons) {
        super(FXML);
        this.persons = getPersonSource(persons);
        eventListView.setItems(events);
        eventListView.setCellFactory(list -> new EventListCell());
        eventListView.setPlaceholder(emptyListMessage);
        eventCount.textProperty().bind(Bindings.createStringBinding(() -> countText(events.size()),
                events, showingArchived));
        eventListView.getSelectionModel().selectedItemProperty().addListener((observable, oldEvent, event) ->
                showDetails(event));
        eventListView.getSelectionModel().selectedIndexProperty().addListener((observable, oldIndex, index) -> {
            if (index.intValue() >= 0) {
                lastSelectedIndex = index.intValue();
            }
        });
        events.addListener((ListChangeListener<Event>) change -> keepSelectionAfterChange());
        this.persons.addListener((ListChangeListener<Person>) change ->
                showDetails(eventListView.getSelectionModel().getSelectedItem()));
        eventSplitPane.widthProperty().addListener((observable, oldWidth, width) ->
                eventSplitPane.setOrientation(width.doubleValue() < 760
                ? Orientation.VERTICAL : Orientation.HORIZONTAL));
        showDetails(null);
    }

    /**
     * Keeps an event selected after the list changes, since the list view clears its selection when
     * events are replaced, and refreshes the details so they show the updated event.
     */
    private void keepSelectionAfterChange() {
        MultipleSelectionModel<Event> selection = eventListView.getSelectionModel();
        if (lastSelectedIndex >= 0 && selection.getSelectedIndex() < 0 && !eventListView.getItems().isEmpty()) {
            selection.select(Math.max(0, Math.min(lastSelectedIndex, eventListView.getItems().size() - 1)));
        }
        showDetails(selection.getSelectedItem());
    }

    /**
     * Selects and reveals the next displayed event. If no event is selected, selects the first event.
     * At the end of the list, keeps the last event selected.
     *
     * @return {@code true} if the list contains an event and handled the navigation request
     */
    boolean selectNextCard() {
        int itemCount = eventListView.getItems().size();
        if (itemCount == 0) {
            return false;
        }

        int selectedIndex = eventListView.getSelectionModel().getSelectedIndex();
        int targetIndex = selectedIndex < 0 ? 0 : Math.min(selectedIndex + 1, itemCount - 1);
        selectAndReveal(targetIndex);
        return true;
    }

    /**
     * Selects and reveals the previous displayed event. If no event is selected, selects the last event.
     * At the start of the list, keeps the first event selected.
     *
     * @return {@code true} if the list contains an event and handled the navigation request
     */
    boolean selectPreviousCard() {
        int itemCount = eventListView.getItems().size();
        if (itemCount == 0) {
            return false;
        }

        int selectedIndex = eventListView.getSelectionModel().getSelectedIndex();
        int targetIndex = selectedIndex < 0 ? itemCount - 1 : Math.max(selectedIndex - 1, 0);
        selectAndReveal(targetIndex);
        return true;
    }

    private void selectAndReveal(int targetIndex) {
        eventListView.getSelectionModel().select(targetIndex);
        eventListView.scrollTo(targetIndex);
    }

    /** Runs the given action whenever the displayed event list is clicked. */
    void setCardListClickHandler(Runnable handler) {
        Objects.requireNonNull(handler);
        eventListView.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> handler.run());
    }

    /**
     * Reveals the newly saved event after a successful addevent command.
     */
    public void selectNewestEvent() {
        eventListView.getSelectionModel().selectLast();
        eventListView.scrollTo(eventListView.getItems().size() - 1);
    }

    /** Returns the selected event card, or the empty event list when no events exist. */
    Node getTutorialEventCardTarget() {
        if (eventListView.getItems().isEmpty()) {
            return eventListView;
        }
        selectTutorialEventIfNeeded();
        int selectedIndex = eventListView.getSelectionModel().getSelectedIndex();
        eventListView.applyCss();
        eventListView.layout();
        return eventListView.lookupAll(".list-cell").stream()
                .filter(node -> node instanceof ListCell<?> cell && !cell.isEmpty() && cell.isVisible()
                        && cell.getIndex() == selectedIndex)
                .findFirst()
                .orElse(eventListView);
    }

    /** Returns the visible event details pane or its empty-state hint. */
    Region getTutorialDetailsTarget() {
        selectTutorialEventIfNeeded();
        return eventDetails.isVisible() ? detailsScroll : selectionHint;
    }

    /** Returns only the member list, or the event details hint when no event exists. */
    Region getTutorialMemberListTarget() {
        selectTutorialEventIfNeeded();
        return eventDetails.isVisible() ? members : selectionHint;
    }

    /** Scrolls the details pane until its member list starts near the top of the viewport. */
    void revealTutorialMemberList() {
        if (!eventDetails.isVisible()) {
            return;
        }
        detailsScroll.applyCss();
        detailsScroll.layout();
        Bounds memberBounds = detailsScroll.getContent()
                .sceneToLocal(members.localToScene(members.getLayoutBounds()));
        double scrollableHeight = detailsScroll.getContent().getLayoutBounds().getHeight()
                - detailsScroll.getViewportBounds().getHeight();
        if (scrollableHeight > 0) {
            double scrollOffset = Math.max(0, memberBounds.getMinY() - 12);
            detailsScroll.setVvalue(Math.min(1, scrollOffset / scrollableHeight));
        }
    }

    private void selectTutorialEventIfNeeded() {
        if (!eventListView.getItems().isEmpty() && eventListView.getSelectionModel().isEmpty()) {
            selectAndReveal(0);
        }
    }

    /**
     * Shows the selected event's details, or a selection hint when no event is selected.
     */
    private void showDetails(Event event) {
        boolean isSelected = event != null;
        eventDetails.setVisible(isSelected);
        eventDetails.setManaged(isSelected);
        selectionHint.setVisible(!isSelected);
        selectionHint.setManaged(!isSelected);
        selectedName.setText(isSelected ? event.getName() : "No event selected");
        if (isSelected) {
            startTime.setText(event.getStartTime().toString());
            endTime.setText(event.getEndTime().toString());
            duration.setText(formatDuration(event));
            memberCount.setText("Members (" + event.getMembers().size() + ")");
            showMembers(event);
        }
    }

    /**
     * Shows each assigned member's attendance for the selected event.
     */
    private void showMembers(Event event) {
        updateAttendanceCounts(event);

        members.getChildren().clear();
        if (event.getMembers().isEmpty()) {
            members.getChildren().add(createEmptyMembersHint());
            return;
        }
        for (Name member : event.getMembers()) {
            members.getChildren().add(createMemberRow(member,
                    event.getAttendance(member).orElse(null), findPerson(member)));
        }
    }

    private Person findPerson(Name memberName) {
        return persons.stream()
                .filter(person -> person.getName().equals(memberName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Labels the list header and empty-list message for the archived view if {@code isShowingArchived} is true.
     * The normal empty-list message suggests {@code addevent}, which would be misleading there.
     */
    public void setShowingArchived(boolean isShowingArchived) {
        showingArchived.set(isShowingArchived);
        emptyListMessage.setText(isShowingArchived ? MESSAGE_NO_ARCHIVED_EVENTS : MESSAGE_NO_EVENTS);
    }

    private String countText(int count) {
        return (showingArchived.get() ? "Archived events (" : "Events (") + count + ")";
    }

    @SuppressWarnings("unchecked")
    private static ObservableList<Person> getPersonSource(ObservableList<Person> persons) {
        if (persons instanceof FilteredList<?> filteredPersons) {
            return (ObservableList<Person>) filteredPersons.getSource();
        }
        return persons;
    }

    private void updateAttendanceCounts(Event event) {
        long present = event.getAttendanceRecord().values().stream()
                .filter(status -> status == AttendanceStatus.PRESENT).count();
        long absent = event.getAttendanceRecord().values().stream()
                .filter(status -> status == AttendanceStatus.ABSENT).count();
        presentCount.setText(present + " Present");
        absentCount.setText(absent + " Absent");
        notMarkedCount.setText((event.getMembers().size() - present - absent) + " Not marked");
    }

    private Label createEmptyMembersHint() {
        Label hint = new Label("No members assigned yet. Assign contacts with assign.");
        hint.setWrapText(true);
        hint.getStyleClass().add("detail-value");
        return hint;
    }

    private HBox createMemberRow(Name member, AttendanceStatus status, Person person) {
        Label memberId = new Label(person == null ? "ID: unavailable" : "ID: " + person.getMemberId().orElseThrow());
        memberId.setMinWidth(Region.USE_PREF_SIZE);
        memberId.getStyleClass().add("event-member-id");

        Label avatar = new Label(initials(member.getFullName()));
        avatar.getStyleClass().add("event-member-avatar");
        avatar.getStyleClass().add(getAvatarColorClass(person));

        Label name = new Label(member.getFullName()
                + (person != null && person.isArchived() ? " (archived)" : ""));
        name.setWrapText(true);
        name.setMinWidth(0);
        name.getStyleClass().add("event-member-name");

        FlowPane identityLine = new FlowPane(8, 4);
        identityLine.setAlignment(Pos.CENTER_LEFT);
        identityLine.getStyleClass().add("event-member-identity");
        identityLine.getChildren().add(name);
        if (person != null) {
            person.getTags().stream()
                    .sorted((left, right) -> String.CASE_INSENSITIVE_ORDER.compare(
                            left.getTagName(), right.getTagName()))
                    .map(Tag::getTagName)
                    .map(TagStyle::createTagLabel)
                    .forEach(identityLine.getChildren()::add);
        }

        Label subtitle = new Label(formatMemberDetails(person));
        subtitle.setWrapText(true);
        subtitle.setMinWidth(0);
        subtitle.getStyleClass().add("event-member-metadata");

        VBox memberDetails = new VBox(4, identityLine, subtitle);
        if (person != null) {
            person.getNote().ifPresent(note -> {
                Label noteLabel = new Label("Note: " + note.getValue());
                noteLabel.setWrapText(true);
                noteLabel.setMinWidth(0);
                noteLabel.getStyleClass().addAll("event-member-metadata", "event-member-note");
                memberDetails.getChildren().add(noteLabel);
            });
        }
        memberDetails.getStyleClass().add("event-member-details");
        memberDetails.setMinWidth(0);
        HBox.setHgrow(memberDetails, Priority.ALWAYS);

        HBox row = new HBox(12, memberId, avatar, memberDetails, createStatusBadge(status));
        row.getStyleClass().add("attendance-member-row");
        return row;
    }

    private String getAvatarColorClass(Person person) {
        if (person == null) {
            return "event-member-avatar-purple";
        }
        if (person.getTags().stream().anyMatch(tag -> tag.getTagName().equalsIgnoreCase("logistics"))) {
            return "event-member-avatar-teal";
        }
        if (person.getTags().stream().anyMatch(tag -> tag.getTagName().equalsIgnoreCase("sponsor"))) {
            return "event-member-avatar-amber";
        }
        return "event-member-avatar-purple";
    }

    private String initials(String fullName) {
        String[] parts = fullName.strip().split("\\s+");
        String result = parts[0].substring(0, 1);
        if (parts.length > 1) {
            result += parts[parts.length - 1].substring(0, 1);
        }
        return result.toUpperCase(Locale.ROOT);
    }

    private String formatMemberDetails(Person person) {
        if (person == null) {
            return "Contact details unavailable";
        }
        StringBuilder details = new StringBuilder(person.getRole().getValue());
        person.getOrganisation().ifPresent(organisation -> details.append(" · ").append(organisation.getValue()));
        return details.toString();
    }

    private Label createStatusBadge(AttendanceStatus status) {
        String text;
        String styleClass;
        if (status == null) {
            text = "● unmarked";
            styleClass = "attendance-unmarked";
        } else if (status == AttendanceStatus.PRESENT) {
            text = "● present";
            styleClass = "attendance-present";
        } else {
            text = "● absent";
            styleClass = "attendance-absent";
        }
        Label badge = new Label(text);
        badge.getStyleClass().addAll("attendance-badge", styleClass);
        badge.setMinWidth(Region.USE_PREF_SIZE);
        return badge;
    }

    /**
     * Counts date-only events inclusively, measures timed events in 24-hour days, hours and minutes,
     * and identifies the missing time for mixed inputs.
     */
    private String formatDuration(Event event) {
        EventTime start = event.getStartTime();
        EventTime end = event.getEndTime();
        var startDateTime = start.getDateTime();
        var endDateTime = end.getDateTime();
        if (startDateTime.isEmpty() && endDateTime.isEmpty()) {
            long days = ChronoUnit.DAYS.between(start.getDate(), end.getDate()) + 1;
            return days + (days == 1 ? " day" : " days");
        }
        if (startDateTime.isEmpty()) {
            return "Unknown (start time not specified)";
        }
        if (endDateTime.isEmpty()) {
            return "Unknown (end time not specified)";
        }
        long minutes = Duration.between(startDateTime.orElseThrow(), endDateTime.orElseThrow()).toMinutes();
        if (minutes == 0) {
            return "0 minutes";
        }
        long days = minutes / (24 * 60);
        long hours = minutes / 60 % 24;
        long remainder = minutes % 60;
        StringJoiner units = new StringJoiner(" ");
        if (days > 0) {
            units.add(days + (days == 1 ? " day" : " days"));
        }
        if (hours > 0) {
            units.add(hours + (hours == 1 ? " hour" : " hours"));
        }
        if (remainder > 0) {
            units.add(remainder + (remainder == 1 ? " minute" : " minutes"));
        }
        return units.toString();
    }

    /**
     * Displays an event's timing and attendance overview in a list card.
     */
    private static class EventListCell extends ListCell<Event> {
        private final Label name = new Label();
        private final Label times = new Label();
        private final Label memberTotal = new Label();
        private final Label presentTotal = new Label();
        private final Label absentTotal = new Label();
        private final Label unmarkedTotal = new Label();
        private final HBox overview = new HBox(10, memberTotal, presentTotal, absentTotal, unmarkedTotal);
        private final VBox card = new VBox(8, name, times, overview);

        EventListCell() {
            card.getStyleClass().add("event-card");
            name.getStyleClass().add("event-card-name");
            times.getStyleClass().add("detail-caption");
            memberTotal.getStyleClass().add("event-member-total");
            presentTotal.getStyleClass().add("attendance-present-text");
            absentTotal.getStyleClass().add("attendance-absent-text");
            unmarkedTotal.getStyleClass().add("attendance-unmarked-text");
            overview.getStyleClass().add("event-overview");
            name.setWrapText(true);
            times.setWrapText(true);
            card.maxWidthProperty().bind(widthProperty().subtract(24));
        }

        @Override
        protected void updateItem(Event event, boolean isEmpty) {
            super.updateItem(event, isEmpty);
            setText(null);
            if (isEmpty || event == null) {
                setGraphic(null);
            } else {
                name.setText((getIndex() + 1) + ". " + event.getName());
                times.setText(event.getStartTime() + " to " + event.getEndTime());
                updateOverview(event);
                setGraphic(card);
            }
        }

        private void updateOverview(Event event) {
            int members = event.getMembers().size();
            if (members == 0) {
                memberTotal.setText("No members assigned");
            } else {
                memberTotal.setText(members + (members == 1 ? " member" : " members"));
            }
            long present = event.getAttendanceRecord().values().stream()
                    .filter(status -> status == AttendanceStatus.PRESENT).count();
            long absent = event.getAttendanceRecord().values().stream()
                    .filter(status -> status == AttendanceStatus.ABSENT).count();
            presentTotal.setText(present + " present");
            absentTotal.setText(absent + " absent");
            unmarkedTotal.setText((members - present - absent) + " unmarked");
            for (Label count : new Label[]{presentTotal, absentTotal, unmarkedTotal}) {
                count.setVisible(members > 0);
                count.setManaged(members > 0);
            }
        }
    }
}
