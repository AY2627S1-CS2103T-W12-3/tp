package coordimate.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import coordimate.logic.LogicManager;
import coordimate.logic.reminder.ReminderDismissalException;
import coordimate.model.ModelManager;
import coordimate.model.ReadOnlyCoordiMate;
import coordimate.model.UserPrefs;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.storage.JsonCoordiMateStorage;
import coordimate.storage.JsonUserPrefsStorage;
import coordimate.storage.StorageManager;
import javafx.animation.Animation;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MainWindowReminderTest {
    @TempDir
    public Path temporaryFolder;

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
    public void startup_remindersAreChronologicalAndVisibleAcrossAllTabs() throws Exception {
        onJavaFxThread(() -> {
            Fixture fixture = openWindow(true);
            try {
                fixture.model().addEvent(event("Student Life Fair", "20-10-2026", "21-10-2026"));
                fixture.logic().refreshReminders();
                Parent root = fixture.stage().getScene().getRoot();

                assertEquals(List.of("Upcoming event: Student Life Fair", "Upcoming event: Final Concert"),
                        names(root));
                assertEquals("Starts: 20-10-2026", ((Label) root.lookup(".reminder-time")).getText());
                for (String view : List.of("eventsView", "tagsView", "contactsView")) {
                    ToggleButton tab = (ToggleButton) root.lookup("#" + view);
                    tab.fire();
                    assertTrue(root.lookup("#reminderPanel").isVisible());
                    assertEquals(2, rows(root).getChildren().size());
                }
            } finally {
                fixture.stage().close();
            }
            return null;
        });
    }

    @Test
    public void startup_noReminders_collapsesPanelAndPeriodicCheckShowsNewReminder() throws Exception {
        onJavaFxThread(() -> {
            ModelManager model = new ModelManager();
            model.addEvent(event("Later", "20-10-2026 15:01", "20-10-2026 18:00"));
            ControlledStorage storage = new ControlledStorage(temporaryFolder.resolve("data.json"));
            storage.saveCoordiMate(model.getCoordiMate());
            Fixture fixture = openWindow(model, storage, new AdjustableClock(), false, 1100, 800);
            try {
                Parent root = fixture.stage().getScene().getRoot();
                assertPanelHidden(root);
                assertEquals(Animation.Status.RUNNING, fixture.timer().getStatus());
                double emptyWorkspaceHeight = root.lookup("#views").getBoundsInParent().getHeight();

                fixture.clock().advance(java.time.Duration.ofMinutes(1));
                fixture.timer().getKeyFrames().getFirst().getOnFinished().handle(new ActionEvent());
                layout(root);

                assertEquals(List.of("Upcoming event: Later"), names(root));
                assertTrue(root.lookup("#reminderPanel").isVisible());
                assertTrue(root.lookup("#reminderPanelPlaceholder").isManaged());
                assertTrue(root.lookup("#views").getBoundsInParent().getHeight() < emptyWorkspaceHeight);

                Button dismiss = (Button) root.lookup(".reminder-dismiss");
                dismiss.fire();
                assertPanelHidden(root);
                assertEquals(emptyWorkspaceHeight, root.lookup("#views").getBoundsInParent().getHeight(), 1);
                assertEquals(Animation.Status.RUNNING, fixture.timer().getStatus());
            } finally {
                fixture.stage().close();
            }
            return null;
        });
    }

    @Test
    public void periodicCheck_entersWindowWithoutCommandsAndStopsWhenWindowCloses() throws Exception {
        onJavaFxThread(() -> {
            Fixture fixture = openWindow(false);
            try {
                fixture.model().addEvent(event("Later", "20-10-2026 15:01", "20-10-2026 18:00"));
                fixture.logic().refreshReminders();
                Parent root = fixture.stage().getScene().getRoot();
                assertEquals(1, rows(root).getChildren().size());
                assertEquals(Animation.Status.RUNNING, fixture.timer().getStatus());
                assertEquals(Animation.INDEFINITE, fixture.timer().getCycleCount());
                assertEquals(Duration.minutes(1), fixture.timer().getKeyFrames().getFirst().getTime());
                byte[] before = Files.readAllBytes(fixture.storage().getCoordiMateFilePath());

                fixture.clock().advance(java.time.Duration.ofMinutes(1));
                fixture.timer().getKeyFrames().getFirst().getOnFinished().handle(new ActionEvent());

                assertEquals(List.of("Upcoming event: Final Concert", "Upcoming event: Later"), names(root));
                assertArrayEquals(before, Files.readAllBytes(fixture.storage().getCoordiMateFilePath()));
                fixture.stage().close();
                assertEquals(Animation.Status.STOPPED, fixture.timer().getStatus());
                fixture.model().addEvent(event("Another", "19-10-2026 18:00", "19-10-2026 19:00"));
                fixture.logic().refreshReminders();
                assertEquals(2, rows(root).getChildren().size());
            } finally {
                fixture.stage().close();
            }
            return null;
        });
    }

    @Test
    public void dismissButton_savesDismissalAndHidesEmptyPanel() throws Exception {
        onJavaFxThread(() -> {
            Fixture fixture = openWindow(true);
            try {
                Parent root = fixture.stage().getScene().getRoot();

                Button dismiss = (Button) root.lookup(".reminder-dismiss");
                dismiss.fire();

                assertEquals(0, rows(root).getChildren().size());
                assertPanelHidden(root);
                assertSame(root.lookup("#commandTextField"), fixture.stage().getScene().getFocusOwner());
                assertEquals("Reminder dismissed: Final Concert.", ((Label) root.lookup("#resultDisplay")).getText());
                assertEquals("OK", ((Label) root.lookup("#feedbackStatus")).getText());
                assertTrue(fixture.storage().readCoordiMate().orElseThrow().getEventList().getFirst()
                        .isReminderDismissed());
            } finally {
                fixture.stage().close();
            }
            return null;
        });
    }

    @Test
    public void dismissButton_multipleReminders_focusesCommandOnlyAfterLastDismissal() throws Exception {
        onJavaFxThread(() -> {
            Fixture fixture = openWindow(true);
            try {
                fixture.model().addEvent(event("Student Life Fair", "20-10-2026", "21-10-2026"));
                fixture.logic().refreshReminders();
                Parent root = fixture.stage().getScene().getRoot();
                Button firstDismiss = (Button) rows(root).getChildren().getFirst().lookup(".reminder-dismiss");
                firstDismiss.requestFocus();
                assertSame(firstDismiss, fixture.stage().getScene().getFocusOwner());

                firstDismiss.fire();

                assertEquals(List.of("Upcoming event: Final Concert"), names(root));
                assertTrue(root.lookup("#reminderPanel").isVisible());
                Button lastDismiss = (Button) rows(root).getChildren().getFirst().lookup(".reminder-dismiss");
                assertSame(lastDismiss, fixture.stage().getScene().getFocusOwner());

                lastDismiss.fire();

                assertEquals(List.of(), names(root));
                assertPanelHidden(root);
                assertSame(root.lookup("#commandTextField"), fixture.stage().getScene().getFocusOwner());
            } finally {
                fixture.stage().close();
            }
            return null;
        });
    }

    @Test
    public void dismissButton_restart_keepsReminderHiddenDuringScheduledChecks() throws Exception {
        onJavaFxThread(() -> {
            Fixture fixture = openWindow(true);
            try {
                Parent root = fixture.stage().getScene().getRoot();
                Button dismiss = (Button) root.lookup(".reminder-dismiss");
                dismiss.fire();
                fixture.stage().close();
                assertEquals(Animation.Status.STOPPED, fixture.timer().getStatus());

                ControlledStorage reloadedStorage = new ControlledStorage(fixture.storage().getCoordiMateFilePath());
                ModelManager reloadedModel = new ModelManager(reloadedStorage.readCoordiMate().orElseThrow(),
                        new UserPrefs());
                Fixture restarted = openWindow(reloadedModel, reloadedStorage, fixture.clock(), false, 1100, 800);
                try {
                    Parent restartedRoot = restarted.stage().getScene().getRoot();
                    assertEquals(List.of(), names(restartedRoot));
                    assertPanelHidden(restartedRoot);
                    byte[] before = Files.readAllBytes(reloadedStorage.getCoordiMateFilePath());

                    restarted.clock().advance(java.time.Duration.ofMinutes(1));
                    restarted.timer().getKeyFrames().getFirst().getOnFinished().handle(new ActionEvent());

                    assertEquals(List.of(), names(restartedRoot));
                    assertArrayEquals(before, Files.readAllBytes(reloadedStorage.getCoordiMateFilePath()));
                } finally {
                    restarted.stage().close();
                }
            } finally {
                fixture.stage().close();
            }
            return null;
        });
    }

    @Test
    public void periodicCheck_dateOnly_entersPreviousDayAndExpiresAfterEventDay() throws Exception {
        onJavaFxThread(() -> {
            Fixture fixture = openWindow(true);
            try {
                fixture.logic().execute("editevent evn/Final Concert st/21-10-2026 et/21-10-2026 18:00");
                Parent root = fixture.stage().getScene().getRoot();
                assertEquals(List.of(), names(root));
                byte[] before = Files.readAllBytes(fixture.storage().getCoordiMateFilePath());

                for (java.time.Duration elapsed : List.of(java.time.Duration.ofHours(9),
                        java.time.Duration.ofHours(24), java.time.Duration.ofHours(24).minusNanos(1))) {
                    fixture.clock().advance(elapsed);
                    fixture.timer().getKeyFrames().getFirst().getOnFinished().handle(new ActionEvent());

                    assertEquals(List.of("Upcoming event: Final Concert"), names(root));
                    assertEquals("Starts: 21-10-2026", ((Label) root.lookup(".reminder-time")).getText());
                }
                fixture.clock().advance(java.time.Duration.ofNanos(1));
                fixture.timer().getKeyFrames().getFirst().getOnFinished().handle(new ActionEvent());

                assertEquals(List.of(), names(root));
                assertPanelHidden(root);
                assertArrayEquals(before, Files.readAllBytes(fixture.storage().getCoordiMateFilePath()));
            } finally {
                fixture.stage().close();
            }
            return null;
        });
    }

    @Test
    public void dismissButton_failedSave_hidesReminderAndDisplaysRestartWarning() throws Exception {
        onJavaFxThread(() -> {
            Fixture fixture = openWindow(true);
            try {
                Parent root = fixture.stage().getScene().getRoot();
                byte[] before = Files.readAllBytes(fixture.storage().getCoordiMateFilePath());
                fixture.storage().failSave = true;

                Button dismiss = (Button) root.lookup(".reminder-dismiss");
                dismiss.fire();

                assertEquals(0, rows(root).getChildren().size());
                assertEquals(ReminderDismissalException.MESSAGE, ((Label) root.lookup("#resultDisplay")).getText());
                assertEquals("Not saved", ((Label) root.lookup("#feedbackStatus")).getText());
                assertFalse(fixture.model().getCoordiMate().getEventList().getFirst().isReminderDismissed());
                assertArrayEquals(before, Files.readAllBytes(fixture.storage().getCoordiMateFilePath()));
                layout(root);
                snapshot(root, "warning.png");
            } finally {
                fixture.stage().close();
            }
            return null;
        });
    }

    @Test
    public void eventCommands_updateDisplayedDetailsAndRemoveDeletedReminder() throws Exception {
        onJavaFxThread(() -> {
            Fixture fixture = openWindow(true);
            try {
                Parent root = fixture.stage().getScene().getRoot();
                fixture.logic().execute("editevent evn/Final Concert nevn/Grand Concert st/19-10-2026 17:00");

                assertEquals(List.of("Upcoming event: Grand Concert"), names(root));
                assertEquals("Starts: 19-10-2026 17:00", ((Label) root.lookup(".reminder-time")).getText());
                fixture.logic().execute("deleteevent evn/Grand Concert");
                assertPanelHidden(root);
                assertEquals(0, rows(root).getChildren().size());
            } finally {
                fixture.stage().close();
            }
            return null;
        });
    }

    @Test
    public void minimumWindowSize_manyLongNamesKeepWorkspaceAndCommandInputUsable() throws Exception {
        onJavaFxThread(() -> {
            for (int width : List.of(1100, 830)) {
                Fixture fixture = openWindow(true, width, width == 830 ? 650 : 800);
                try {
                    Parent root = fixture.stage().getScene().getRoot();
                    for (int index = 0; index < 8; index++) {
                        fixture.model().addEvent(event("Concert ".repeat(11) + index,
                                "19-10-2026 17:00", "19-10-2026 19:00"));
                    }
                    fixture.logic().refreshReminders();
                    layout(root);

                    assertTrue(root.lookup("#views").getBoundsInParent().getHeight() >= 120);
                    TextField command = (TextField) root.lookup("#commandTextField");
                    assertTrue(command.isVisible());
                    assertTrue(command.localToScene(command.getBoundsInLocal()).getMaxY()
                            <= fixture.stage().getScene().getHeight());
                    assertTrue(root.lookup("#reminderPanel").getBoundsInParent().getHeight() <= 220);
                    assertTrue(root.getLayoutBounds().getWidth() <= width);
                    ScrollPane reminders = (ScrollPane) root.lookup("#reminderScroll");
                    assertTrue(reminders.getViewportBounds().getHeight() >= 160);
                    ScrollPane navigation = (ScrollPane) root.lookup("#navigationScroll");
                    assertEquals(ScrollPane.ScrollBarPolicy.NEVER, navigation.getHbarPolicy());
                    navigation.setVvalue(1);
                    layout(root);
                    snapshot(root, width == 830 ? "minimum-window.png" : "desktop.png");
                } finally {
                    fixture.stage().close();
                }
            }
            return null;
        });
    }

    @Test
    public void exitCommand_stopsTimerAndHidesWindow() throws Exception {
        onJavaFxThread(() -> {
            Fixture fixture = openWindow(true);
            try {
                TextField input = (TextField) fixture.stage().getScene().getRoot().lookup("#commandTextField");
                input.setText("exit");

                input.fireEvent(new ActionEvent());

                assertFalse(fixture.stage().isShowing());
                assertEquals(Animation.Status.STOPPED, fixture.timer().getStatus());
                fixture.window().stop();
            } finally {
                fixture.stage().close();
            }
            return null;
        });
    }

    @Test
    public void uiManagerStop_releasesPanelSubscriptionWithoutClosingStage() throws Exception {
        onJavaFxThread(() -> {
            ModelManager model = new ModelManager();
            Clock clock = Clock.fixed(ZonedDateTime.parse("2026-10-19T15:00:00+08:00").toInstant(),
                    ZoneId.of("Asia/Makassar"));
            LogicManager logic = new LogicManager(model, null, clock);
            UiManager ui = new UiManager(logic, temporaryFolder.resolve("data.json"));
            Stage stage = new Stage();
            try {
                ui.start(stage);
                Parent root = stage.getScene().getRoot();

                ui.stop();
                model.addEvent(event("Tomorrow", "20-10-2026", "21-10-2026"));
                logic.refreshReminders();

                assertEquals(1, logic.getReminders().size());
                assertEquals(0, rows(root).getChildren().size());
                assertTrue(stage.isShowing());
            } finally {
                ui.stop();
                stage.close();
            }
            return null;
        });
    }

    private Fixture openWindow(boolean showBeforeFilling) throws IOException {
        return openWindow(showBeforeFilling, 1100, 800);
    }

    private Fixture openWindow(boolean showBeforeFilling, int width, int height) throws IOException {
        ModelManager model = new ModelManager();
        model.addEvent(event("Final Concert", "20-10-2026 15:00", "20-10-2026 18:00"));
        ControlledStorage storage = new ControlledStorage(temporaryFolder.resolve("data.json"));
        storage.saveCoordiMate(model.getCoordiMate());
        AdjustableClock clock = new AdjustableClock();
        return openWindow(model, storage, clock, showBeforeFilling, width, height);
    }

    private Fixture openWindow(ModelManager model, ControlledStorage storage, AdjustableClock clock,
            boolean showBeforeFilling, int width, int height) {
        LogicManager logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))), clock);
        Stage stage = new Stage();
        Timeline timer = new Timeline();
        MainWindow window = new MainWindow(stage, logic, storage.getCoordiMateFilePath(), timer);
        stage.setWidth(width);
        stage.setHeight(height);
        if (showBeforeFilling) {
            stage.show();
        }
        window.fillInnerParts();
        if (!showBeforeFilling) {
            assertEquals(Animation.Status.STOPPED, timer.getStatus());
            stage.show();
        }
        layout(stage.getScene().getRoot());
        return new Fixture(stage, window, model, logic, storage, clock, timer);
    }

    private static Event event(String name, String start, String end) {
        return new Event(name, new EventTime(start), new EventTime(end));
    }

    private static VBox rows(Parent root) {
        return (VBox) ((ScrollPane) root.lookup("#reminderScroll")).getContent();
    }

    private static List<String> names(Parent root) {
        return rows(root).getChildren().stream()
                .map(row -> ((Label) row.lookup(".reminder-name")).getText()).toList();
    }

    private static void layout(Parent root) {
        root.applyCss();
        root.layout();
        root.layout();
    }

    private static void assertPanelHidden(Parent root) {
        layout(root);
        assertFalse(root.lookup("#reminderPanel").isVisible());
        assertFalse(root.lookup("#reminderPanel").isManaged());
        assertFalse(root.lookup("#reminderPanelPlaceholder").isVisible());
        assertFalse(root.lookup("#reminderPanelPlaceholder").isManaged());
    }

    private static void snapshot(Parent root, String filename) throws IOException {
        WritableImage image = root.snapshot(null, null);
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                buffered.setRGB(x, y, image.getPixelReader().getArgb(x, y));
            }
        }
        Path directory = Path.of("build", "reports", "reminders");
        Files.createDirectories(directory);
        ImageIO.write(buffered, "png", directory.resolve(filename).toFile());
    }

    private static void onJavaFxThread(Callable<Void> assertion) throws Exception {
        FutureTask<Void> task = new FutureTask<>(assertion);
        Platform.runLater(task);
        task.get(15, TimeUnit.SECONDS);
    }

    private record Fixture(Stage stage, MainWindow window, ModelManager model, LogicManager logic,
                           ControlledStorage storage, AdjustableClock clock, Timeline timer) { }

    private static class ControlledStorage extends JsonCoordiMateStorage {
        private boolean failSave;

        ControlledStorage(Path path) {
            super(path);
        }

        @Override
        public void saveCoordiMate(ReadOnlyCoordiMate data) throws IOException {
            if (failSave) {
                throw new IOException("Disk full");
            }
            super.saveCoordiMate(data);
        }
    }

    private static class AdjustableClock extends Clock {
        private Instant current = ZonedDateTime.parse("2026-10-19T15:00:00+08:00").toInstant();

        void advance(java.time.Duration duration) {
            current = current.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("Asia/Makassar");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(current, zone);
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
