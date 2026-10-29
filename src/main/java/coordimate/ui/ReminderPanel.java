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

    /**
     * Rebuilds the reminder rows and preserves the focused dismissal button when possible.
     */
    private void showReminders() {
        int focusedRow = findFocusedRow();
        reminderHeading.setText("Upcoming events (" + reminders.size() + ")");
        boolean isEmpty = reminders.isEmpty();
        getRoot().setVisible(!isEmpty);
        getRoot().setManaged(!isEmpty);
        reminderScroll.setPrefViewportHeight(Math.min(168, reminders.size() * 112));
        reminderRows.getChildren().setAll(IntStream.range(0, reminders.size())
                .mapToObj(index -> createReminderRow(reminders.get(index), index + 1)).toList());
        if (focusedRow >= 0 && !isEmpty) {
            HBox nextRow = (HBox) reminderRows.getChildren().get(Math.min(focusedRow, reminders.size() - 1));
            nextRow.getChildren().getLast().requestFocus();
        }
    }

    /**
     * Returns the index of the focused dismissal button, or -1 if no row has focus.
     */
    private int findFocusedRow() {
        for (int index = 0; index < reminderRows.getChildren().size(); index++) {
            HBox row = (HBox) reminderRows.getChildren().get(index);
            if (row.getScene() != null && row.getScene().getFocusOwner() == row.getChildren().getLast()) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Builds an indexed reminder with its event times and keyboard-accessible dismissal button.
     */
    private HBox createReminderRow(Event event, int index) {
        Label name = new Label(index + ". Upcoming event: " + event.getName());
        name.getStyleClass().add("reminder-name");
        name.setWrapText(true);
        name.setMinWidth(0);
        Label start = createTimeLabel("Starts: " + event.getStartTime());
        Label end = createTimeLabel("Ends: " + event.getEndTime());
        VBox details = new VBox(3, name, start, end);
        details.setMinWidth(0);
        HBox.setHgrow(details, Priority.ALWAYS);

        Button dismiss = createDismissButton(event);
        HBox row = new HBox(16, details, dismiss);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("reminder-row");
        return row;
    }

    private Label createTimeLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("reminder-time");
        return label;
    }

    /**
     * Creates a dismissal button with an event-specific accessible label and Enter key support.
     */
    private Button createDismissButton(Event event) {
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
        return dismiss;
    }

    /**
     * Detaches listeners when the window closes.
     */
    public void close() {
        reminders.removeListener(reminderListener);
    }
}
