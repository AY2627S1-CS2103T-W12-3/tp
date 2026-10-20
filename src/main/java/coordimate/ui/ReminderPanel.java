package coordimate.ui;

import java.util.function.Consumer;
import java.util.stream.IntStream;

import coordimate.model.event.Event;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Shows the live reminder list without obscuring the workspace or command input.
 */
public class ReminderPanel extends UiPart<Region> {
    private static final String FXML = "ReminderPanel.fxml";

    private final ObservableList<Event> reminders;
    private final Consumer<Event> dismissHandler;
    private final ListChangeListener<Event> reminderListener = change -> showReminders();

    @FXML
    private Label reminderHeading;

    @FXML
    private Label reminderEmptyState;

    @FXML
    private ScrollPane reminderScroll;

    @FXML
    private VBox reminderRows;

    /**
     * Uses the logic layer's ordering and dismissal callback.
     */
    public ReminderPanel(ObservableList<Event> reminders, Consumer<Event> dismissHandler) {
        super(FXML);
        this.reminders = reminders;
        this.dismissHandler = dismissHandler;
        getRoot().setMinHeight(Region.USE_PREF_SIZE);
        reminders.addListener(reminderListener);
        showReminders();
    }

    private void showReminders() {
        int focusedRow = -1;
        for (int index = 0; index < reminderRows.getChildren().size(); index++) {
            HBox row = (HBox) reminderRows.getChildren().get(index);
            if (row.getScene() != null && row.getScene().getFocusOwner() == row.getChildren().getLast()) {
                focusedRow = index;
            }
        }
        reminderHeading.setText("Upcoming events (" + reminders.size() + ")");
        boolean empty = reminders.isEmpty();
        reminderEmptyState.setVisible(empty);
        reminderEmptyState.setManaged(empty);
        reminderScroll.setVisible(!empty);
        reminderScroll.setManaged(!empty);
        reminderScroll.setPrefViewportHeight(Math.min(168, reminders.size() * 112));
        reminderRows.getChildren().setAll(IntStream.range(0, reminders.size())
                .mapToObj(index -> createReminderRow(reminders.get(index), index + 1)).toList());
        if (focusedRow >= 0) {
            if (empty) {
                reminderEmptyState.requestFocus();
            } else {
                HBox nextRow = (HBox) reminderRows.getChildren().get(Math.min(focusedRow, reminders.size() - 1));
                nextRow.getChildren().getLast().requestFocus();
            }
        }
    }

    private HBox createReminderRow(Event event, int index) {
        Label name = new Label(index + ". Upcoming event: " + event.getName());
        name.getStyleClass().add("reminder-name");
        name.setWrapText(true);
        name.setMinWidth(0);
        Label start = new Label("Starts: " + event.getStartTime());
        Label end = new Label("Ends: " + event.getEndTime());
        start.setWrapText(true);
        end.setWrapText(true);
        start.getStyleClass().add("reminder-time");
        end.getStyleClass().add("reminder-time");
        VBox details = new VBox(3, name, start, end);
        details.setMinWidth(0);
        HBox.setHgrow(details, Priority.ALWAYS);

        Button dismiss = new Button("Dismiss");
        dismiss.getStyleClass().add("reminder-dismiss");
        dismiss.setMinWidth(Region.USE_PREF_SIZE);
        dismiss.setAccessibleText("Dismiss reminder for " + event.getName());
        dismiss.setOnAction(action -> dismissHandler.accept(event));
        dismiss.setOnKeyPressed(key -> {
            if (key.getCode() == KeyCode.ENTER) {
                dismiss.fire();
                key.consume();
            }
        });
        HBox row = new HBox(16, details, dismiss);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("reminder-row");
        return row;
    }

    /**
     * Detaches listeners when the window closes.
     */
    public void close() {
        reminders.removeListener(reminderListener);
    }
}
