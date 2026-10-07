package coordimate.ui;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

import coordimate.model.event.Event;
import coordimate.model.person.Name;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Keeps event selection and details connected to the live model list.
 */
public class EventListPanel extends UiPart<Region> {
    private static final String FXML = "EventListPanel.fxml";
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd-MM-uuuu HH:mm");

    @FXML
    private SplitPane eventSplitPane;

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
    private Label members;

    @FXML
    private Label selectionHint;

    private int lastSelectedIndex = -1;

    /**
     * Observes saved events and stacks the panes when the window is narrow.
     */
    public EventListPanel(ObservableList<Event> events) {
        super(FXML);
        eventListView.setItems(events);
        eventListView.setCellFactory(list -> new EventListCell());
        eventListView.setPlaceholder(new Label("No events yet. Create one with addevent."));
        eventCount.textProperty().bind(Bindings.size(events).asString("Events (%d)"));
        eventListView.getSelectionModel().selectedItemProperty().addListener((observable, oldEvent, event) ->
                showDetails(event));
        eventListView.getSelectionModel().selectedIndexProperty().addListener((observable, oldIndex, index) -> {
            if (index.intValue() >= 0) {
                lastSelectedIndex = index.intValue();
            }
        });
        events.addListener((ListChangeListener<Event>) change -> keepSelectionAfterChange());
        eventSplitPane.widthProperty().addListener((observable, oldWidth, width) ->
                eventSplitPane.setOrientation(width.doubleValue() < 680
                ? Orientation.VERTICAL : Orientation.HORIZONTAL));
        showDetails(null);
        eventListView.getSelectionModel().selectFirst();
    }

    /**
     * Keeps an event selected after the list changes, since the list view clears its selection when
     * events are replaced, and refreshes the details so they show the updated event.
     */
    private void keepSelectionAfterChange() {
        MultipleSelectionModel<Event> selection = eventListView.getSelectionModel();
        if (selection.getSelectedIndex() < 0 && !eventListView.getItems().isEmpty()) {
            selection.select(Math.max(0, Math.min(lastSelectedIndex, eventListView.getItems().size() - 1)));
        }
        showDetails(selection.getSelectedItem());
    }

    /**
     * Reveals the newly saved event after a successful addevent command.
     */
    public void selectNewestEvent() {
        eventListView.getSelectionModel().selectLast();
        eventListView.scrollTo(eventListView.getItems().size() - 1);
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
            members.setText(formatMembers(event));
        }
    }

    /**
     * Returns the event's member names, one per line in assignment order, or a hint when it has none.
     */
    private String formatMembers(Event event) {
        if (event.getMembers().isEmpty()) {
            return "No members assigned yet. Assign contacts with assign.";
        }
        return event.getMembers().stream().map(Name::toString).collect(Collectors.joining("\n"));
    }

    /**
     * Returns the event's duration in hours and minutes when both times of day are specified.
     * Returns a placeholder message for date-only times.
     */
    private String formatDuration(Event event) {
        String start = event.getStartTime().toString();
        String end = event.getEndTime().toString();
        if (start.length() == 10 || end.length() == 10) {
            return "Time of day not specified";
        }
        long minutes = Duration.between(LocalDateTime.parse(start, DATE_TIME_FORMAT),
                LocalDateTime.parse(end, DATE_TIME_FORMAT)).toMinutes();
        long hours = minutes / 60;
        long remainder = minutes % 60;
        return hours + (hours == 1 ? " hour" : " hours")
                + (remainder == 0 ? "" : " " + remainder + (remainder == 1 ? " minute" : " minutes"));
    }

    /**
     * Displays an event's name and start and end times in a list card.
     */
    private static class EventListCell extends ListCell<Event> {
        private final Label name = new Label();
        private final Label times = new Label();
        private final VBox card = new VBox(8, name, times);

        EventListCell() {
            card.getStyleClass().add("event-card");
            name.getStyleClass().add("event-card-name");
            times.getStyleClass().add("detail-caption");
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
                setGraphic(card);
            }
        }
    }
}
