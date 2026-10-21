package coordimate.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ReminderPanelTest {
    @BeforeAll
    public static void startJavaFx() throws InterruptedException {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "JavaFX UI tests require a display");
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
        } catch (IllegalStateException alreadyRunning) {
            Platform.runLater(started::countDown);
        }
        assertTrue(started.await(10, TimeUnit.SECONDS));
        Platform.setImplicitExit(false);
    }

    @Test
    public void reminders_renderSavedDetailsInSourceOrderWithoutInventingDateOnlyTimes() throws Exception {
        onJavaFxThread(() -> {
            Event allDay = event("Student Life Fair", "20-10-2026", "21-10-2026");
            Event concert = event("Final Concert", "20-10-2026 15:00", "20-10-2026 18:00");
            ReminderPanel panel = new ReminderPanel(FXCollections.observableArrayList(allDay, concert), ignored -> { });
            Region root = panel.getRoot();
            new Scene(root, 830, 220);
            root.applyCss();
            root.layout();

            assertEquals("Upcoming events (2)", label(root, "reminderHeading").getText());
            assertEquals(List.of("1. Upcoming event: Student Life Fair", "Starts: 20-10-2026", "Ends: 21-10-2026"),
                    rowTexts(rows(root).getFirst()));
            assertEquals(List.of("2. Upcoming event: Final Concert",
                            "Starts: 20-10-2026 15:00", "Ends: 20-10-2026 18:00"),
                    rowTexts(rows(root).getLast()));
            assertTrue(root.isVisible());
            assertTrue(root.isManaged());
            panel.close();
        });
    }

    @Test
    public void reminderChanges_replaceDetailsAndHideEmptyPanel() throws Exception {
        onJavaFxThread(() -> {
            Event original = event("Concert", "20-10-2026 15:00", "20-10-2026 18:00");
            ObservableList<Event> reminders = FXCollections.observableArrayList(original);
            ReminderPanel panel = new ReminderPanel(reminders, ignored -> { });
            Event revised = original.withDetails("Final Concert", new EventTime("20-10-2026 16:00"),
                    new EventTime("20-10-2026 19:00"));

            reminders.setAll(revised);
            assertEquals(1, rows(panel.getRoot()).size());
            assertEquals(List.of("1. Upcoming event: Final Concert",
                            "Starts: 20-10-2026 16:00", "Ends: 20-10-2026 19:00"),
                    rowTexts(rows(panel.getRoot()).getFirst()));
            reminders.clear();
            assertFalse(panel.getRoot().isVisible());
            assertFalse(panel.getRoot().isManaged());
            assertEquals(0, rows(panel.getRoot()).size());
            reminders.add(original);
            assertTrue(panel.getRoot().isVisible());
            assertTrue(panel.getRoot().isManaged());
            panel.close();
        });
    }

    @Test
    public void reminderChanges_renumberAfterInsertionReorderingAndRemoval() throws Exception {
        onJavaFxThread(() -> {
            Event first = event("First", "20-10-2026", "20-10-2026");
            Event second = event("Second", "20-10-2026 15:00", "20-10-2026 18:00");
            ObservableList<Event> reminders = FXCollections.observableArrayList(second);
            ReminderPanel panel = new ReminderPanel(reminders, reminders::remove);
            try {
                reminders.addFirst(first);
                assertEquals(List.of("1. Upcoming event: First", "2. Upcoming event: Second"),
                        rows(panel.getRoot()).stream().map(row -> rowTexts(row).getFirst()).toList());

                reminders.setAll(second, first);
                assertEquals(List.of("1. Upcoming event: Second", "2. Upcoming event: First"),
                        rows(panel.getRoot()).stream().map(row -> rowTexts(row).getFirst()).toList());

                dismissButton(rows(panel.getRoot()).getFirst()).fire();
                assertEquals(List.of(first), reminders);
                assertEquals("1. Upcoming event: First", rowTexts(rows(panel.getRoot()).getFirst()).getFirst());

                reminders.clear();
                reminders.add(second);
                assertEquals("1. Upcoming event: Second", rowTexts(rows(panel.getRoot()).getFirst()).getFirst());
            } finally {
                panel.close();
            }
        });
    }

    @Test
    public void dismiss_keyboardAndButtonActivation_preserveFocusAsRemindersDisappear() throws Exception {
        onJavaFxThread(() -> {
            Event first = event("First", "20-10-2026", "20-10-2026");
            Event second = event("Second", "20-10-2026 15:00", "20-10-2026 18:00");
            ObservableList<Event> reminders = FXCollections.observableArrayList(first, second);
            ReminderPanel panel = new ReminderPanel(reminders, reminders::remove);
            Stage stage = new Stage();
            stage.setScene(new Scene(panel.getRoot(), 830, 260));
            try {
                stage.show();
                panel.getRoot().applyCss();
                panel.getRoot().layout();
                Button firstDismiss = dismissButton(rows(panel.getRoot()).getFirst());
                assertTrue(firstDismiss.isFocusTraversable());
                assertEquals("Dismiss reminder for First", firstDismiss.getAccessibleText());
                firstDismiss.requestFocus();
                firstDismiss.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.TAB,
                        false, false, false, false));
                assertSame(dismissButton(rows(panel.getRoot()).getLast()), stage.getScene().getFocusOwner());
                firstDismiss.requestFocus();
                KeyEvent enter = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER,
                        false, false, false, false);
                firstDismiss.getOnKeyPressed().handle(enter);

                assertTrue(enter.isConsumed());
                assertEquals(List.of(second), reminders);
                assertEquals("1. Upcoming event: Second", rowTexts(rows(panel.getRoot()).getFirst()).getFirst());
                Button nextDismiss = dismissButton(rows(panel.getRoot()).getFirst());
                assertSame(nextDismiss, stage.getScene().getFocusOwner());
                panel.getRoot().applyCss();
                panel.getRoot().layout();
                KeyEvent otherKey = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.A,
                        false, false, false, false);
                nextDismiss.getOnKeyPressed().handle(otherKey);
                assertFalse(otherKey.isConsumed());
                nextDismiss.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.SPACE,
                        false, false, false, false));
                nextDismiss.fireEvent(new KeyEvent(KeyEvent.KEY_RELEASED, "", "", KeyCode.SPACE,
                        false, false, false, false));
                assertTrue(reminders.isEmpty());
                assertFalse(panel.getRoot().isVisible());
                assertFalse(panel.getRoot().isManaged());
            } finally {
                panel.close();
                stage.close();
            }
        });
    }

    @Test
    public void manyReminders_longNamesAndEnlargedText_fitWidthAndScrollWithinHeightLimit() throws Exception {
        onJavaFxThread(() -> {
            ObservableList<Event> reminders = FXCollections.observableArrayList();
            for (int index = 0; index < 12; index++) {
                reminders.add(event("Concert ".repeat(11) + index, "20-10-2026 15:00", "20-10-2026 18:00"));
            }
            ReminderPanel panel = new ReminderPanel(reminders, reminders::remove);
            Region root = panel.getRoot();
            Scene scene = new Scene(root, 830, 270);
            scene.getStylesheets().add(getClass().getResource("/view/DarkTheme.css").toExternalForm());
            root.setStyle("-fx-font-size: 26px;");
            root.applyCss();
            root.layout();
            root.layout();

            ScrollPane scroll = (ScrollPane) root.lookup("#reminderScroll");
            assertEquals(ScrollPane.ScrollBarPolicy.NEVER, scroll.getHbarPolicy());
            assertTrue(scroll.getPrefViewportHeight() <= 168);
            assertTrue(((VBox) scroll.getContent()).getHeight() > scroll.getViewportBounds().getHeight());
            for (HBox row : rows(root)) {
                Button dismiss = dismissButton(row);
                assertTrue(dismiss.getBoundsInParent().getMaxX() <= row.getWidth() + 1);
                for (var node : ((VBox) row.getChildren().getFirst()).getChildren()) {
                    Label text = (Label) node;
                    assertTrue(text.isWrapText());
                    assertTrue(text.getHeight() + 1 >= text.prefHeight(text.getWidth()));
                }
            }
            panel.close();
        });
    }

    @Test
    public void close_detachesReminderListenerAndIsSafeToRepeat() throws Exception {
        onJavaFxThread(() -> {
            ObservableList<Event> reminders = FXCollections.observableArrayList();
            ReminderPanel panel = new ReminderPanel(reminders, ignored -> { });

            panel.close();
            panel.close();
            reminders.add(event("Concert", "20-10-2026", "20-10-2026"));

            assertEquals(0, rows(panel.getRoot()).size());
        });
    }

    private static Event event(String name, String start, String end) {
        return new Event(name, new EventTime(start), new EventTime(end));
    }

    private static List<HBox> rows(Region root) {
        ScrollPane scroll = (ScrollPane) root.lookup("#reminderScroll");
        return ((VBox) scroll.getContent()).getChildren().stream().map(HBox.class::cast).toList();
    }

    private static List<String> rowTexts(HBox row) {
        return ((VBox) row.getChildren().getFirst()).getChildren().stream()
                .map(Label.class::cast).map(Label::getText).toList();
    }

    private static Button dismissButton(HBox row) {
        return (Button) row.getChildren().getLast();
    }

    private static Label label(Region root, String id) {
        return (Label) root.lookup("#" + id);
    }

    private static void onJavaFxThread(Runnable assertion) throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            assertion.run();
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }
}
