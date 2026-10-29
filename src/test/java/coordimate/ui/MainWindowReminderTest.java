package coordimate.ui;

import static coordimate.testutil.TypicalPersons.ALICE;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
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
import coordimate.logic.commands.EditCommand;
import coordimate.logic.reminder.ReminderDismissalException;
import coordimate.model.ModelManager;
import coordimate.model.ReadOnlyCoordiMate;
import coordimate.model.UserPrefs;
import coordimate.model.event.AttendanceStatus;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.storage.JsonCoordiMateStorage;
import coordimate.storage.JsonUserPrefsStorage;
import coordimate.storage.StorageManager;
import coordimate.testutil.PersonBuilder;
import javafx.animation.Animation;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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

                assertEquals(List.of("1. Upcoming event: Student Life Fair", "2. Upcoming event: Final Concert"),
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

                assertEquals(List.of("1. Upcoming event: Later"), names(root));
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

                assertEquals(List.of("1. Upcoming event: Final Concert", "2. Upcoming event: Later"), names(root));
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
    public void periodicCheck_leaveEndsAtMidnight_hidesBadgeWithoutCommand() throws Exception {
        onJavaFxThread(() -> {
            ModelManager model = new ModelManager();
            model.addPerson(new PersonBuilder(ALICE).withoutMemberId().withLeaveDate("19-10-2026").build());
            ControlledStorage storage = new ControlledStorage(temporaryFolder.resolve("data.json"));
            storage.saveCoordiMate(model.getCoordiMate());
            Fixture fixture = openWindow(model, storage, new AdjustableClock(), true, 1100, 800);
            try {
                Parent root = fixture.stage().getScene().getRoot();
                assertEquals(List.of("On leave until 19-10-2026"), leaveBadges(root));

                fixture.clock().advance(java.time.Duration.ofHours(8));
                fixture.timer().getKeyFrames().getFirst().getOnFinished().handle(new ActionEvent());
                assertEquals(List.of("On leave until 19-10-2026"), leaveBadges(root));

                fixture.clock().advance(java.time.Duration.ofHours(1));
                fixture.timer().getKeyFrames().getFirst().getOnFinished().handle(new ActionEvent());
                assertEquals(List.of(), leaveBadges(root));
            } finally {
                fixture.stage().close();
            }
            return null;
        });
    }

    @Test
    public void dismissCommand_validIndex_updatesPanelAndShowsSuccessAfterRejectedIndex() throws Exception {
        onJavaFxThread(() -> {
            Fixture fixture = openWindow(true);
            try {
                Parent root = fixture.stage().getScene().getRoot();
                TextField input = (TextField) root.lookup("#commandTextField");
                input.setText("dismiss 2");
                input.fireEvent(new ActionEvent());
                assertEquals("Rejected", ((Label) root.lookup("#feedbackStatus")).getText());
                assertEquals(1, rows(root).getChildren().size());
                assertEquals("dismiss 2", input.getText());

                input.setText("dismiss 1");
                input.fireEvent(new ActionEvent());

                assertEquals(0, rows(root).getChildren().size());
                assertPanelHidden(root);
                assertEquals("Reminder dismissed: Final Concert.", ((Label) root.lookup("#resultDisplay")).getText());
                assertEquals("OK", ((Label) root.lookup("#feedbackStatus")).getText());
                assertEquals("", input.getText());
                assertTrue(fixture.storage().readCoordiMate().orElseThrow().getEventList().getFirst()
                        .isReminderDismissed());
            } finally {
                fixture.stage().close();
            }
            return null;
        });
    }

    @Test
    public void dismissCommand_repeatedIndex_renumbersPanelAndPreservesSelectedTabAndFocus() throws Exception {
        onJavaFxThread(() -> {
            Fixture fixture = openWindow(true);
            try {
                fixture.model().addEvent(event("alpha", "20-10-2026 15:00", "20-10-2026 18:00"));
                fixture.model().addEvent(event("Zulu", "20-10-2026 15:00", "20-10-2026 18:00"));
                fixture.logic().refreshReminders();
                Parent root = fixture.stage().getScene().getRoot();
                assertEquals(List.of("1. Upcoming event: alpha", "2. Upcoming event: Final Concert",
                        "3. Upcoming event: Zulu"), names(root));
                ToggleButton tags = (ToggleButton) root.lookup("#tagsView");
                tags.fire();
                TextField input = (TextField) root.lookup("#commandTextField");
                input.requestFocus();

                input.setText("dismiss 2");
                input.fireEvent(new ActionEvent());
                assertEquals(List.of("1. Upcoming event: alpha", "2. Upcoming event: Zulu"), names(root));
                assertEquals("Reminder dismissed: Final Concert.", ((Label) root.lookup("#resultDisplay")).getText());
                assertTrue(tags.isSelected());
                assertSame(input, fixture.stage().getScene().getFocusOwner());

                input.setText("dismiss 2");
                input.fireEvent(new ActionEvent());
                assertEquals(List.of("1. Upcoming event: alpha"), names(root));
                assertEquals("Reminder dismissed: Zulu.", ((Label) root.lookup("#resultDisplay")).getText());
                assertTrue(tags.isSelected());
                assertSame(input, fixture.stage().getScene().getFocusOwner());
            } finally {
                fixture.stage().close();
            }
            return null;
        });
    }

    @Test
    public void dismissCommand_saveFailure_showsNotSavedWarningAndHidesReminder() throws Exception {
        onJavaFxThread(() -> {
            Fixture fixture = openWindow(true);
            try {
                fixture.storage().failSave = true;
                Parent root = fixture.stage().getScene().getRoot();
                TextField input = (TextField) root.lookup("#commandTextField");
                input.setText("dismiss 1");
                input.fireEvent(new ActionEvent());

                assertEquals(0, rows(root).getChildren().size());
                assertEquals(ReminderDismissalException.MESSAGE, ((Label) root.lookup("#resultDisplay")).getText());
                assertEquals("Not saved", ((Label) root.lookup("#feedbackStatus")).getText());
                assertEquals("", input.getText());
                assertFalse(fixture.model().getCoordiMate().getEventList().getFirst().isReminderDismissed());
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

                assertEquals(List.of("1. Upcoming event: Final Concert"), names(root));
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
    public void navigationMode_releasesDismissFocusAndBlocksControlKeys() throws Exception {
        onJavaFxThread(() -> {
            Fixture fixture = openWindow(true);
            try {
                Parent root = fixture.stage().getScene().getRoot();
                Button dismiss = (Button) root.lookup(".reminder-dismiss");
                Label mode = (Label) root.lookup("#interactionModeLabel");
                ToggleButton eventsView = (ToggleButton) root.lookup("#eventsView");
                dismiss.requestFocus();
                assertSame(dismiss, fixture.stage().getScene().getFocusOwner());

                dismiss.fireEvent(keyEvent(KeyCode.ESCAPE));

                assertEquals(MainWindow.NAVIGATION_MODE_TEXT, mode.getText());
                assertSame(mode, fixture.stage().getScene().getFocusOwner());
                List<String> remindersBeforeBlockedKeys = names(root);
                for (KeyCode blockedKey : List.of(KeyCode.ENTER, KeyCode.SPACE, KeyCode.TAB,
                        KeyCode.UP, KeyCode.DOWN, KeyCode.LEFT, KeyCode.RIGHT)) {
                    dismiss.fireEvent(keyEvent(blockedKey));
                    assertEquals(remindersBeforeBlockedKeys, names(root));
                    assertSame(mode, fixture.stage().getScene().getFocusOwner());
                }

                dismiss.fireEvent(keyEvent(KeyCode.S));
                assertTrue(eventsView.isSelected());

                dismiss.fire();
                assertPanelHidden(root);
                assertSame(mode, fixture.stage().getScene().getFocusOwner());
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

                    assertEquals(List.of("1. Upcoming event: Final Concert"), names(root));
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

                assertEquals(List.of("1. Upcoming event: Grand Concert"), names(root));
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
            for (int width : List.of(1400, 1200)) {
                Fixture fixture = openWindow(true, width, 800);
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
                    snapshot(root, width == 1200 ? "minimum-window.png" : "desktop.png");
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

    @Test
    public void periodicCheck_eventMemberLeaveEnds_preservesAttendanceAndSelection() throws Exception {
        onJavaFxThread(() -> {
            ModelManager model = new ModelManager();
            model.addPerson(new PersonBuilder(ALICE).withoutMemberId().withLeaveDate("19-10-2026")
                    .withNote("Contact by email while on leave").build());
            Event event = event("Team Dinner", "25-10-2026", "25-10-2026")
                    .withMembersAndAttendance(List.of(ALICE.getName()),
                            Map.of(ALICE.getName(), AttendanceStatus.ABSENT));
            model.addEvent(event);
            ControlledStorage storage = new ControlledStorage(temporaryFolder.resolve("data.json"));
            storage.saveCoordiMate(model.getCoordiMate());
            Fixture fixture = openWindow(model, storage, new AdjustableClock(), true, 1100, 800);
            try {
                Parent root = fixture.stage().getScene().getRoot();
                ToggleButton eventsView = (ToggleButton) root.lookup("#eventsView");
                eventsView.fire();
                ListView<?> events = (ListView<?>) root.lookup("#eventListView");
                events.getSelectionModel().selectFirst();
                VBox members = (VBox) root.lookup("#members");
                assertEquals(List.of("On leave until 19-10-2026"), leaveBadges(members));
                snapshot(root, "event-member-leave.png");
                byte[] savedBeforeExpiry = Files.readAllBytes(storage.getCoordiMateFilePath());

                var originalRow = members.getChildren().getFirst();
                fixture.clock().advance(java.time.Duration.ofHours(8));
                fixture.timer().getKeyFrames().getFirst().getOnFinished().handle(new ActionEvent());
                assertSame(originalRow, members.getChildren().getFirst());
                assertEquals(List.of("On leave until 19-10-2026"), leaveBadges(members));

                fixture.clock().advance(java.time.Duration.ofHours(1));
                fixture.timer().getKeyFrames().getFirst().getOnFinished().handle(new ActionEvent());
                assertEquals(List.of(), leaveBadges(members));
                assertSame(event, events.getSelectionModel().getSelectedItem());
                assertEquals("1 Absent", ((Label) root.lookup("#absentCount")).getText());
                assertEquals("● absent", ((Label) members.lookup(".attendance-badge")).getText());
                assertArrayEquals(savedBeforeExpiry, Files.readAllBytes(storage.getCoordiMateFilePath()));

                events.getSelectionModel().clearSelection();
                fixture.clock().advance(java.time.Duration.ofDays(1));
                fixture.timer().getKeyFrames().getFirst().getOnFinished().handle(new ActionEvent());
                assertTrue(events.getSelectionModel().isEmpty());
                assertTrue(root.lookup("#selectionHint").isVisible());
            } finally {
                fixture.stage().close();
            }
            return null;
        });
    }

    @Test
    public void showWindow_afterHiddenAcrossMidnight_refreshesLeaveBeforeNextTick() throws Exception {
        onJavaFxThread(() -> {
            ModelManager model = new ModelManager();
            model.addPerson(new PersonBuilder(ALICE).withoutMemberId().withLeaveDate("19-10-2026").build());
            Event event = event("Team Dinner", "25-10-2026", "25-10-2026")
                    .withMembersAndAttendance(List.of(ALICE.getName()), Map.of());
            model.addEvent(event);
            ControlledStorage storage = new ControlledStorage(temporaryFolder.resolve("data.json"));
            storage.saveCoordiMate(model.getCoordiMate());
            Fixture fixture = openWindow(model, storage, new AdjustableClock(), true, 1100, 800);
            try {
                Parent root = fixture.stage().getScene().getRoot();
                ToggleButton eventsView = (ToggleButton) root.lookup("#eventsView");
                eventsView.fire();
                ListView<?> events = (ListView<?>) root.lookup("#eventListView");
                events.getSelectionModel().selectFirst();
                VBox members = (VBox) root.lookup("#members");
                assertEquals(List.of("On leave until 19-10-2026"), leaveBadges(members));

                fixture.stage().hide();
                assertEquals(Animation.Status.STOPPED, fixture.timer().getStatus());
                fixture.clock().advance(java.time.Duration.ofHours(9));
                fixture.stage().show();

                assertEquals(List.of(), leaveBadges(members));
                assertSame(event, events.getSelectionModel().getSelectedItem());
                assertEquals(Animation.Status.RUNNING, fixture.timer().getStatus());
                ToggleButton contactsView = (ToggleButton) root.lookup("#contactsView");
                contactsView.fire();
                assertEquals(List.of(), leaveBadges(root));
            } finally {
                fixture.stage().close();
            }
            return null;
        });
    }

    @Test
    public void commands_createEventAssignMemberAndEditLeave_updateSelectedEventAndSavedData() throws Exception {
        onJavaFxThread(() -> {
            ModelManager model = new ModelManager();
            model.addPerson(new PersonBuilder(ALICE).withoutMemberId().withName("Aisha")
                    .withLeaveDate("19-10-2026").build());
            model.addEvent(event("Existing Event", "25-10-2026", "25-10-2026"));
            ControlledStorage storage = new ControlledStorage(temporaryFolder.resolve("data.json"));
            storage.saveCoordiMate(model.getCoordiMate());
            Fixture fixture = openWindow(model, storage, new AdjustableClock(), false, 1100, 800);
            try {
                Parent root = fixture.stage().getScene().getRoot();
                submitCommand(root, "addevent evn/Planning Session st/25-10-2026 et/25-10-2026");
                ToggleButton eventsView = (ToggleButton) root.lookup("#eventsView");
                ListView<?> events = (ListView<?>) root.lookup("#eventListView");
                assertTrue(eventsView.isSelected());
                assertEquals(1, events.getSelectionModel().getSelectedIndex());
                assertEquals("Planning Session", ((Label) root.lookup("#selectedName")).getText());

                submitCommand(root, "assign evn/Planning Session c/1");
                VBox members = (VBox) root.lookup("#members");
                assertEquals(List.of("On leave until 19-10-2026"), leaveBadges(members));
                assertEquals("A", ((Label) members.lookup(".event-member-avatar")).getText());
                submitCommand(root, "markattendance evn/Planning Session mem/Aisha att/present");
                assertEquals("1 Present", ((Label) root.lookup("#presentCount")).getText());

                String future = LocalDate.now().plusYears(1).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                submitCommand(root, "edit 1 lv/" + future);
                eventsView.fire();
                assertEquals(List.of("On leave until " + future), leaveBadges(members));
                assertEquals(future, storage.readCoordiMate().orElseThrow().getPersonList().getFirst()
                        .getLeaveDate().orElseThrow().getValue());

                submitCommand(root, "edit 1 lv/");
                eventsView.fire();
                assertEquals(List.of(), leaveBadges(members));
                assertEquals("Planning Session", ((Label) root.lookup("#selectedName")).getText());
                assertEquals("1 Present", ((Label) root.lookup("#presentCount")).getText());
                assertEquals("● present", ((Label) members.lookup(".attendance-badge")).getText());
                assertTrue(storage.readCoordiMate().orElseThrow().getPersonList().getFirst().getLeaveDate().isEmpty());
            } finally {
                fixture.stage().close();
            }
            return null;
        });
    }

    @Test
    public void editLeave_saveFailure_preservesEventIndicatorAttendanceAndSavedData() throws Exception {
        onJavaFxThread(() -> {
            ModelManager model = new ModelManager();
            model.addPerson(new PersonBuilder(ALICE).withoutMemberId().withLeaveDate("19-10-2026").build());
            Event event = event("Team Dinner", "25-10-2026", "25-10-2026")
                    .withMembersAndAttendance(List.of(ALICE.getName()),
                            Map.of(ALICE.getName(), AttendanceStatus.ABSENT));
            model.addEvent(event);
            ControlledStorage storage = new ControlledStorage(temporaryFolder.resolve("data.json"));
            storage.saveCoordiMate(model.getCoordiMate());
            Fixture fixture = openWindow(model, storage, new AdjustableClock(), true, 1100, 800);
            try {
                Parent root = fixture.stage().getScene().getRoot();
                ToggleButton eventsView = (ToggleButton) root.lookup("#eventsView");
                eventsView.fire();
                ListView<?> events = (ListView<?>) root.lookup("#eventListView");
                events.getSelectionModel().selectFirst();
                byte[] savedBeforeEdit = Files.readAllBytes(storage.getCoordiMateFilePath());
                storage.failSave = true;

                submitCommand(root, "edit 1 lv/");

                assertTrue(((Label) root.lookup("#resultDisplay")).getText()
                        .startsWith(EditCommand.MESSAGE_SAVE_ERROR));
                assertEquals(List.of("On leave until 19-10-2026"), leaveBadges((VBox) root.lookup("#members")));
                assertTrue(eventsView.isSelected());
                assertSame(event, events.getSelectionModel().getSelectedItem());
                assertEquals("1 Absent", ((Label) root.lookup("#absentCount")).getText());
                assertEquals("19-10-2026", model.getCoordiMate().getPersonList().getFirst()
                        .getLeaveDate().orElseThrow().getValue());
                assertArrayEquals(savedBeforeEdit, Files.readAllBytes(storage.getCoordiMateFilePath()));
            } finally {
                fixture.stage().close();
            }
            return null;
        });
    }

    private static void submitCommand(Parent root, String command) {
        TextField input = (TextField) root.lookup("#commandTextField");
        input.setText(command);
        input.fireEvent(new ActionEvent());
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
        MainWindow window = new MainWindow(stage, logic, storage.getCoordiMateFilePath(), timer, clock);
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

    private static KeyEvent keyEvent(KeyCode keyCode) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", keyCode,
                false, false, false, false);
    }

    private static VBox rows(Parent root) {
        return (VBox) ((ScrollPane) root.lookup("#reminderScroll")).getContent();
    }

    private static List<String> names(Parent root) {
        return rows(root).getChildren().stream()
                .map(row -> ((Label) row.lookup(".reminder-name")).getText()).toList();
    }

    private static List<String> leaveBadges(Parent root) {
        layout(root);
        return root.lookupAll(".leave-badge").stream()
                .filter(node -> node.isVisible())
                .map(node -> ((Label) node).getText())
                .toList();
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
