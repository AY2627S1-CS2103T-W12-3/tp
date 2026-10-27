package coordimate.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import coordimate.logic.LogicManager;
import coordimate.model.ModelManager;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class UiManagerTest {

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

        RecordingUiManager(LogicManager logic, Path dataFilePath, boolean isDataRecoveryMode, Runnable exitAction) {
            super(logic, dataFilePath, isDataRecoveryMode, exitAction);
        }

        RecordingUiManager(LogicManager logic, Path dataFilePath, boolean isDataRecoveryMode,
                String dataRecoveryDetail, Runnable exitAction) {
            super(logic, dataFilePath, isDataRecoveryMode, dataRecoveryDetail, exitAction);
        }

        @Override
        void showAlertDialogAndWait(Alert.AlertType type, String title, String headerText, String contentText) {
            alertType = type;
            alertTitle = title;
            alertHeader = headerText;
            alertContent = contentText;
        }
    }

}
