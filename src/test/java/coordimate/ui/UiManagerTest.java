package coordimate.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import coordimate.logic.LogicManager;
import coordimate.model.ModelManager;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;

public class UiManagerTest {

    @Test
    public void start_startupWarning_showsDeletedNamesAfterWindowOpens() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            ModelManager model = new ModelManager();
            Stage primaryStage = new Stage();
            String warning = "Deleted 1 saved event:\n\n\"Planning\\u2003c/review\"";
            boolean[] shown = {false};
            UiManager manager = new UiManager(new LogicManager(model, null), Path.of("data.json"), warning) {
                @Override
                void showAlertDialogAndWait(Alert.AlertType type, String title, String header, String content) {
                    assertTrue(primaryStage.isShowing());
                    assertEquals(Alert.AlertType.WARNING, type);
                    assertEquals("Saved files", title);
                    assertEquals("Saved files need attention", header);
                    assertEquals(warning, content);
                    assertFalse(primaryStage.getScene().lookup("#tutorialOverlay").isVisible());
                    shown[0] = true;
                }
            };
            try {
                manager.start(primaryStage);
                assertTrue(shown[0]);
                assertTrue(primaryStage.getScene().lookup("#tutorialOverlay").isVisible());
            } finally {
                manager.stop();
                primaryStage.close();
            }
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void start_startupWarning_dialogShowsScrollableDeletionList() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            ModelManager model = new ModelManager();
            model.setHasSeenTutorial(true);
            Stage primaryStage = new Stage();
            String warning = "Deleted 40 saved events:\n\n" + "\"Planning\\u2003c/review\"\n".repeat(40);
            UiManager manager = new UiManager(new LogicManager(model, null), Path.of("data.json"), warning);
            AtomicReference<Throwable> dialogFailure = new AtomicReference<>();
            checkVisibleDialogAndClose(primaryStage, dialogFailure, pane -> {
                assertTrue(primaryStage.isShowing());
                ScrollPane scrollPane = (ScrollPane) pane.getContent();
                Label message = (Label) scrollPane.getContent();
                assertEquals(warning, message.getText());
                assertTrue(message.isWrapText());
                assertTrue(scrollPane.isFitToWidth());
                assertTrue(message.getHeight() > scrollPane.getViewportBounds().getHeight());
                assertTrue(scrollPane.getViewportBounds().getHeight() <= 340);
            });
            try {
                manager.start(primaryStage);
                assertNull(dialogFailure.get());
            } finally {
                manager.stop();
                primaryStage.close();
            }
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void showAlertDialogAndWait_errorMessage_keepsCompleteScrollableContent() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            ModelManager model = new ModelManager();
            model.setHasSeenTutorial(true);
            Stage primaryStage = new Stage();
            UiManager manager = new UiManager(new LogicManager(model, null), Path.of("data.json"));
            String errorMessage = "Could not load data. ".repeat(100);
            AtomicReference<Throwable> dialogFailure = new AtomicReference<>();
            try {
                manager.start(primaryStage);
                checkVisibleDialogAndClose(primaryStage, dialogFailure, pane -> {
                    ScrollPane scrollPane = (ScrollPane) pane.getContent();
                    Label message = (Label) scrollPane.getContent();
                    assertEquals(errorMessage, message.getText());
                    assertTrue(message.isWrapText());
                    assertTrue(scrollPane.isFitToWidth());
                    assertTrue(message.getHeight() > scrollPane.getViewportBounds().getHeight());
                    assertTrue(scrollPane.getViewportBounds().getHeight() <= 340);
                });
                manager.showAlertDialogAndWait(Alert.AlertType.ERROR, "Saved event data", "Load failed",
                        errorMessage);
                assertNull(dialogFailure.get());
            } finally {
                manager.stop();
                primaryStage.close();
            }
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

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
    public void start_startupWarning_notifiesAfterWindowIsShown() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            ModelManager model = new ModelManager();
            model.setHasSeenTutorial(true);
            Stage primaryStage = new Stage();
            String warning = "Deleted events with year 0000: Year Zero";
            boolean[] notified = {false};
            UiManager manager = new UiManager(new LogicManager(model, null), Path.of("data.json"), warning) {
                @Override
                void showAlertDialogAndWait(Alert.AlertType type, String title, String header, String content) {
                    assertTrue(primaryStage.isShowing());
                    assertEquals(Alert.AlertType.WARNING, type);
                    assertEquals("Saved files", title);
                    assertEquals("Saved files need attention", header);
                    assertEquals(warning, content);
                    notified[0] = true;
                }
            };
            try {
                manager.start(primaryStage);
                assertTrue(notified[0]);
            } finally {
                manager.stop();
                primaryStage.close();
            }
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void start_unseenTutorial_opensAndRecordsPreferenceWhenClosed() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            ModelManager model = new ModelManager();
            LogicManager logic = new LogicManager(model, null);
            Stage primaryStage = new Stage();
            UiManager uiManager = new UiManager(logic, Path.of("data", "coordimate.json"));

            uiManager.start(primaryStage);

            StackPane tutorialOverlay = (StackPane) primaryStage.getScene().lookup("#tutorialOverlay");
            assertFalse(logic.hasSeenTutorial());
            assertTrue(tutorialOverlay.isVisible());
            assertTrue(primaryStage.getScene().lookup("#commandTextField").isDisabled());
            assertTrue(primaryStage.getScene().lookup("#submitButton").isDisabled());
            Button nextButton = (Button) primaryStage.getScene().lookup("#nextButton");
            ToggleButton contactsView = (ToggleButton) primaryStage.getScene().lookup("#contactsView");
            ToggleButton eventsView = (ToggleButton) primaryStage.getScene().lookup("#eventsView");
            ToggleButton tagsView = (ToggleButton) primaryStage.getScene().lookup("#tagsView");
            nextButton.fire();
            assertTrue(contactsView.isSelected());
            for (int step = 0; step < 4; step++) {
                nextButton.fire();
            }
            assertTrue(contactsView.isSelected());
            assertFalse(eventsView.isSelected());
            assertFalse(tagsView.isSelected());
            Button closeButton = (Button) primaryStage.getScene().lookup("#closeButton");
            closeButton.fire();
            assertTrue(logic.hasSeenTutorial());
            assertFalse(tutorialOverlay.isVisible());
            assertTrue(logic.getPersonList().isEmpty());
            assertTrue(logic.getEventList().isEmpty());
            assertTrue(contactsView.isSelected());
            assertFalse(primaryStage.getScene().lookup("#commandTextField").isDisabled());
            uiManager.stop();
            primaryStage.close();
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void start_tutorialAlreadySeen_doesNotOpenAutomatically() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            ModelManager model = new ModelManager();
            model.setHasSeenTutorial(true);
            LogicManager logic = new LogicManager(model, null);
            Stage primaryStage = new Stage();
            UiManager uiManager = new UiManager(logic, Path.of("data", "coordimate.json"));

            uiManager.start(primaryStage);

            assertTrue(logic.hasSeenTutorial());
            StackPane tutorialOverlay = (StackPane) primaryStage.getScene().lookup("#tutorialOverlay");
            assertFalse(tutorialOverlay.isVisible());
            uiManager.stop();
            primaryStage.close();
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void start_dataRecoveryMode_showsWarningAndExits() throws Exception {
        FutureTask<RecordingUiManager> task = new FutureTask<>(() -> {
            ModelManager model = new ModelManager();
            LogicManager logic = new LogicManager(model, null);
            Path dataFilePath = Path.of("data", "coordimate.json");
            boolean[] isExitRequested = {false};
            Runnable exitAction = () -> isExitRequested[0] = true;
            RecordingUiManager uiManager = new RecordingUiManager(logic, dataFilePath, true, exitAction);
            Stage primaryStage = new Stage();

            uiManager.start(primaryStage);

            assertTrue(primaryStage.isShowing());
            assertEquals(Alert.AlertType.WARNING, uiManager.alertType);
            assertEquals(UiManager.DATA_RECOVERY_ALERT_TITLE, uiManager.alertTitle);
            assertEquals(UiManager.DATA_RECOVERY_ALERT_HEADER, uiManager.alertHeader);
            assertTrue(uiManager.alertContent.contains("original data file has not been changed"));
            assertTrue(uiManager.alertContent.contains("will now close"));
            assertTrue(uiManager.alertContent.contains(dataFilePath.toAbsolutePath().toString()));
            assertTrue(isExitRequested[0]);
            assertFalse(logic.hasSeenTutorial());
            assertFalse(primaryStage.getScene().lookup("#tutorialOverlay").isVisible());
            uiManager.stop();
            primaryStage.close();
            return uiManager;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void start_dataRecoveryModeWithDetail_showsReasonAndExits() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            LogicManager logic = new LogicManager(new ModelManager(), null);
            Path dataFilePath = Path.of("data", "coordimate.json");
            String detail = "Contact \"Alex Yeoh\": archived must be true or false.";
            boolean[] isExitRequested = {false};
            RecordingUiManager uiManager = new RecordingUiManager(logic, dataFilePath, true, detail, () ->
                    isExitRequested[0] = true);
            Stage primaryStage = new Stage();

            uiManager.start(primaryStage);

            assertTrue(uiManager.alertContent.contains("Problem: " + detail));
            assertTrue(uiManager.alertContent.contains(dataFilePath.toAbsolutePath().toString()));
            assertTrue(isExitRequested[0]);
            uiManager.stop();
            primaryStage.close();
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void start_dataAndPreferencesRecovery_showsBothProblemsAndExits() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            LogicManager logic = new LogicManager(new ModelManager(), null);
            Path dataFilePath = Path.of("data", "coordimate.json");
            Path prefsFilePath = Path.of("preferences.json");
            String dataDetail = "Invalid JSON type at persons[0].phone: expected a string.";
            String prefsWarning = "Preferences could not be loaded.\n\nProblem: Invalid JSON type at "
                    + "guiSettings.windowMaximized: expected a boolean.\n\nRepair or restore this file:\n"
                    + prefsFilePath.toAbsolutePath();
            boolean[] isExitRequested = {false};
            Runnable exitAction = () -> isExitRequested[0] = true;
            RecordingUiManager uiManager = new RecordingUiManager(logic, dataFilePath, dataDetail, prefsWarning,
                    exitAction);
            Stage primaryStage = new Stage();

            uiManager.start(primaryStage);

            assertEquals(1, uiManager.alertCount);
            assertTrue(uiManager.alertContent.contains(dataDetail));
            assertTrue(uiManager.alertContent.contains(dataFilePath.toAbsolutePath().toString()));
            assertTrue(uiManager.alertContent.contains("guiSettings.windowMaximized"));
            assertTrue(uiManager.alertContent.contains(prefsFilePath.toAbsolutePath().toString()));
            assertTrue(isExitRequested[0]);
            uiManager.stop();
            primaryStage.close();
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void start_standardRecoveryMessage_showsBothRepairPathsWithoutScrolling() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            LogicManager logic = new LogicManager(new ModelManager(), null);
            Path dataPath = Path.of("data", "coordimate.json").toAbsolutePath();
            Path prefsPath = Path.of("preferences.json").toAbsolutePath();
            String prefsWarning = "Preferences could not be loaded. Default preferences are in use for this "
                    + "session. The original file will not be changed.\n\n"
                    + "Problem: Invalid JSON type at guiSettings.windowMaximized: expected a boolean.\n\n"
                    + "Repair or restore this file before restarting CoordiMate:\n" + prefsPath;
            UiManager manager = new UiManager(logic, dataPath, "", true,
                    "Invalid JSON type at persons[0].phone: expected a string.", prefsWarning, () -> {});
            Stage stage = new Stage();
            AtomicReference<Throwable> failure = new AtomicReference<>();
            checkVisibleDialogPathsAndClose(stage, failure, dataPath.toString(), prefsPath.toString());
            try {
                manager.start(stage);
                if (failure.get() != null) {
                    throw new AssertionError(failure.get());
                }
            } finally {
                manager.stop();
                stage.close();
            }
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void start_preferenceWarning_showsRepairPathWithoutScrolling() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            ModelManager model = new ModelManager();
            model.setHasSeenTutorial(true);
            Path prefsPath = Path.of("preferences.json").toAbsolutePath();
            String warning = "Preferences could not be loaded. Default preferences are in use for this "
                    + "session. The original file will not be changed.\n\n"
                    + "Problem: Invalid JSON type at guiSettings.windowMaximized: expected a boolean.\n\n"
                    + "Repair or restore this file before restarting CoordiMate:\n" + prefsPath;
            UiManager manager = new UiManager(new LogicManager(model, null), Path.of("data.json"), warning);
            Stage stage = new Stage();
            AtomicReference<Throwable> failure = new AtomicReference<>();
            checkVisibleDialogPathsAndClose(stage, failure, prefsPath.toString());
            try {
                manager.start(stage);
                if (failure.get() != null) {
                    throw new AssertionError(failure.get());
                }
            } finally {
                manager.stop();
                stage.close();
            }
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    private static void checkVisibleDialogPathsAndClose(Stage owner, AtomicReference<Throwable> failure,
            String... paths) {
        checkVisibleDialogAndClose(owner, failure, dialogPane -> {
            ScrollPane scrollPane = (ScrollPane) dialogPane.getContent();
            Label message = (Label) scrollPane.getContent();
            for (String path : paths) {
                assertTrue(message.getText().contains(path));
            }
            assertTrue(message.getHeight() <= scrollPane.getViewportBounds().getHeight() + 1,
                    "Message bounds: " + message.getLayoutBounds() + ", viewport: "
                            + scrollPane.getViewportBounds() + ", preferred height at actual width: "
                            + message.prefHeight(message.getWidth()) + ", font: " + message.getFont());
        });
    }

    /** Measures the dialog after a layout pulse, then closes it outside the pulse's layout processing. */
    private static void checkVisibleDialogAndClose(Stage owner, AtomicReference<Throwable> failure,
            Consumer<DialogPane> assertions) {
        Platform.runLater(() -> {
            try {
                DialogPane dialogPane = Window.getWindows().stream().filter(Window::isShowing)
                        .filter(Stage.class::isInstance).map(Stage.class::cast)
                        .filter(window -> window.getOwner() == owner)
                        .map(window -> window.getScene().getRoot())
                        .filter(DialogPane.class::isInstance).map(DialogPane.class::cast)
                        .findFirst().orElseThrow();
                checkDialogAfterLayoutAndClose(dialogPane, failure, assertions);
            } catch (Throwable error) {
                failure.set(error);
                Window.getWindows().stream().filter(Stage.class::isInstance).map(Stage.class::cast)
                        .filter(window -> window.getOwner() == owner).toList().forEach(Window::hide);
            }
        });
    }

    private static void checkDialogAfterLayoutAndClose(DialogPane dialogPane, AtomicReference<Throwable> failure,
            Consumer<DialogPane> assertions) {
        Scene scene = dialogPane.getScene();
        scene.addPostLayoutPulseListener(new Runnable() {
            @Override
            public void run() {
                scene.removePostLayoutPulseListener(this);
                try {
                    ScrollPane scrollPane = (ScrollPane) dialogPane.getContent();
                    assertTrue(scrollPane.getViewportBounds().getHeight() > 0, "Dialog viewport must be laid out");
                    assertions.accept(dialogPane);
                } catch (Throwable error) {
                    failure.set(error);
                } finally {
                    Platform.runLater(() -> ((Button) dialogPane.lookupButton(ButtonType.OK)).fire());
                }
            }
        });
        Platform.requestNextPulse();
    }

    @Test
    public void start_longRecoveryMessage_isScrollableAndKeepsRepairPath() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            LogicManager logic = new LogicManager(new ModelManager(), null);
            Path dataFilePath = Path.of("data", "coordimate.json");
            String detail = "Invalid JSON type at persons[0].phone. ".repeat(30);
            boolean[] isExitRequested = {false};
            UiManager uiManager = new UiManager(logic, dataFilePath, true, detail, () ->
                    isExitRequested[0] = true);
            Stage primaryStage = new Stage();
            AtomicReference<Throwable> failure = new AtomicReference<>();
            checkVisibleDialogAndClose(primaryStage, failure, dialogPane -> {
                ScrollPane scrollPane = (ScrollPane) dialogPane.getContent();
                Label message = (Label) scrollPane.getContent();
                assertTrue(message.getText().contains(dataFilePath.toAbsolutePath().toString()));
                assertTrue(message.getText().contains("Repair or restore this file"));
                assertTrue(message.getLayoutBounds().getHeight() > scrollPane.getViewportBounds().getHeight());
                assertTrue(scrollPane.isFitToWidth());
                assertTrue(scrollPane.getViewportBounds().getHeight() <= 340,
                        "Viewport height: " + scrollPane.getViewportBounds().getHeight());
            });
            try {
                uiManager.start(primaryStage);
                if (failure.get() != null) {
                    throw new AssertionError(failure.get());
                }
                assertTrue(isExitRequested[0]);
            } finally {
                uiManager.stop();
                primaryStage.close();
            }
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void finishingTutorial_recordsPreferenceAndClosesOverlay() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            LogicManager logic = new LogicManager(new ModelManager(), null);
            Stage primaryStage = new Stage();
            UiManager uiManager = new UiManager(logic, Path.of("data", "coordimate.json"));
            uiManager.start(primaryStage);

            Button nextButton = (Button) primaryStage.getScene().lookup("#nextButton");
            for (int step = 0; step < 15; step++) {
                nextButton.fire();
            }
            assertEquals("Step 16 of 16", ((Label) primaryStage.getScene().lookup("#stepCounter")).getText());
            assertEquals("Finish →", nextButton.getText());
            assertFalse(logic.hasSeenTutorial());

            nextButton.fire();
            assertTrue(logic.hasSeenTutorial());
            assertFalse(primaryStage.getScene().lookup("#tutorialOverlay").isVisible());
            assertTrue(((ToggleButton) primaryStage.getScene().lookup("#tagsView")).isSelected());
            uiManager.stop();
            primaryStage.close();
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    private static class RecordingUiManager extends UiManager {
        private Alert.AlertType alertType;
        private String alertTitle;
        private String alertHeader;
        private String alertContent;
        private int alertCount;

        RecordingUiManager(LogicManager logic, Path dataFilePath, boolean isDataRecoveryMode, Runnable exitAction) {
            super(logic, dataFilePath, isDataRecoveryMode, exitAction);
        }

        RecordingUiManager(LogicManager logic, Path dataFilePath, boolean isDataRecoveryMode,
                String dataRecoveryDetail, Runnable exitAction) {
            super(logic, dataFilePath, isDataRecoveryMode, dataRecoveryDetail, exitAction);
        }

        RecordingUiManager(LogicManager logic, Path dataFilePath, String dataRecoveryDetail,
                String preferencesWarning, Runnable exitAction) {
            super(logic, dataFilePath, "", true, dataRecoveryDetail, preferencesWarning, exitAction);
        }

        @Override
        void showAlertDialogAndWait(Alert.AlertType type, String title, String headerText, String contentText) {
            alertType = type;
            alertTitle = title;
            alertHeader = headerText;
            alertContent = contentText;
            alertCount++;
        }
    }

}
