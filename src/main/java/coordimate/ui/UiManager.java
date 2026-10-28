package coordimate.ui;

import java.nio.file.Path;
import java.util.logging.Logger;

import coordimate.MainApp;
import coordimate.commons.core.LogsCenter;
import coordimate.commons.util.StringUtil;
import coordimate.logic.Logic;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * The manager of the UI component.
 */
public class UiManager implements Ui {

    public static final String ALERT_DIALOG_PANE_FIELD_ID = "alertDialogPane";
    public static final String DATA_RECOVERY_ALERT_TITLE = "Saved data could not be loaded";
    public static final String DATA_RECOVERY_ALERT_HEADER = "CoordiMate cannot start with the saved data";
    public static final String DATA_RECOVERY_ALERT_CONTENT = "The original data file has not been changed. "
            + "CoordiMate will now close.\n\n%sRepair or restore this file, then restart CoordiMate:\n%s";

    private static final Logger logger = LogsCenter.getLogger(UiManager.class);
    private static final String ICON_APPLICATION = "/images/coordimate_32.png";

    private Logic logic;
    private Path dataFilePath;
    private final boolean isDataRecoveryMode;
    private final String dataRecoveryDetail;
    private final String preferencesWarning;
    private final Runnable exitAction;
    private MainWindow mainWindow;
    private final String startupWarning;

    /**
     * Creates a {@code UiManager} with the given {@code Logic} and the data file path
     * to show in the status bar.
     */
    public UiManager(Logic logic, Path dataFilePath) {
        this(logic, dataFilePath, "", false, "", Platform::exit);
    }

    UiManager(Logic logic, Path dataFilePath, boolean isDataRecoveryMode, Runnable exitAction) {
        this(logic, dataFilePath, "", isDataRecoveryMode, "", exitAction);
    }

    UiManager(Logic logic, Path dataFilePath, boolean isDataRecoveryMode, String dataRecoveryDetail,
            Runnable exitAction) {
        this(logic, dataFilePath, "", isDataRecoveryMode, dataRecoveryDetail, exitAction);
    }

    /**
     * Creates the UI with a warning to show after startup, without asking for confirmation.
     */
    public UiManager(Logic logic, Path dataFilePath, String startupWarning) {
        this(logic, dataFilePath, startupWarning, false);
    }

    /**
     * Creates the UI with a startup warning and recovery state.
     */
    public UiManager(Logic logic, Path dataFilePath, String startupWarning, boolean isDataRecoveryMode) {
        this(logic, dataFilePath, startupWarning, isDataRecoveryMode, "");
    }

    /**
     * Creates the UI with a startup warning and an optional reason for data recovery.
     */
    public UiManager(Logic logic, Path dataFilePath, String startupWarning, boolean isDataRecoveryMode,
            String dataRecoveryDetail) {
        this(logic, dataFilePath, startupWarning, isDataRecoveryMode, dataRecoveryDetail, "");
    }

    /** Creates the UI with a preference warning to include if saved data also needs recovery. */
    public UiManager(Logic logic, Path dataFilePath, String startupWarning, boolean isDataRecoveryMode,
            String dataRecoveryDetail, String preferencesWarning) {
        this(logic, dataFilePath, startupWarning, isDataRecoveryMode, dataRecoveryDetail, preferencesWarning,
                Platform::exit);
    }

    UiManager(Logic logic, Path dataFilePath, String startupWarning, boolean isDataRecoveryMode,
            String dataRecoveryDetail, Runnable exitAction) {
        this(logic, dataFilePath, startupWarning, isDataRecoveryMode, dataRecoveryDetail, "", exitAction);
    }

    UiManager(Logic logic, Path dataFilePath, String startupWarning, boolean isDataRecoveryMode,
            String dataRecoveryDetail, String preferencesWarning, Runnable exitAction) {
        this.logic = logic;
        this.dataFilePath = dataFilePath;
        this.startupWarning = startupWarning;
        this.isDataRecoveryMode = isDataRecoveryMode;
        this.dataRecoveryDetail = dataRecoveryDetail;
        this.preferencesWarning = preferencesWarning;
        this.exitAction = exitAction;
    }

    @Override
    public void start(Stage primaryStage) {
        logger.info("Starting UI...");

        //Set the application icon.
        primaryStage.getIcons().add(getImage(ICON_APPLICATION));

        try {
            mainWindow = new MainWindow(primaryStage, logic, dataFilePath);
            mainWindow.show(); //This should be called before creating other UI parts
            mainWindow.fillInnerParts();
            if (isDataRecoveryMode) {
                showAlertDialogAndWait(Alert.AlertType.WARNING, DATA_RECOVERY_ALERT_TITLE,
                        DATA_RECOVERY_ALERT_HEADER,
                        recoveryAlertContent());
                exitAction.run();
                return;
            } else if (!startupWarning.isEmpty()) {
                showAlertDialogAndWait(AlertType.WARNING, "Saved files", "Saved files need attention",
                        startupWarning);
            }
            mainWindow.showTutorialOnFirstLaunch();

        } catch (Throwable e) {
            logger.severe(StringUtil.getDetails(e));
            showFatalErrorDialogAndShutdown("Fatal error during initializing", e);
        }
    }

    private String recoveryAlertContent() {
        String detail = dataRecoveryDetail.isEmpty() ? "" : "Problem: " + dataRecoveryDetail + "\n\n";
        String dataWarning = String.format(DATA_RECOVERY_ALERT_CONTENT, detail, dataFilePath.toAbsolutePath());
        return preferencesWarning.isEmpty() ? dataWarning : dataWarning + "\n\n" + preferencesWarning;
    }

    @Override
    public void stop() {
        if (mainWindow != null) {
            mainWindow.stop();
        }
    }

    private Image getImage(String imagePath) {
        return new Image(MainApp.class.getResourceAsStream(imagePath));
    }

    void showAlertDialogAndWait(Alert.AlertType type, String title, String headerText, String contentText) {
        showAlertDialogAndWait(mainWindow.getPrimaryStage(), type, title, headerText, contentText);
    }

    /**
     * Shows an alert dialog on {@code owner} with the given parameters.
     * This method only returns after the user has closed the alert dialog.
     */
    private static void showAlertDialogAndWait(Stage owner, AlertType type, String title, String headerText,
            String contentText) {
        final Alert alert = new Alert(type);
        alert.getDialogPane().getStylesheets().add("view/DarkTheme.css");
        alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        Label message = new Label(contentText);
        message.setWrapText(true);
        message.setPadding(new Insets(4, 8, 4, 0));
        ScrollPane scrollPane = new ScrollPane(message);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportWidth(440);
        scrollPane.setPrefViewportHeight(100);
        alert.getDialogPane().setContent(scrollPane);
        alert.getDialogPane().setId(ALERT_DIALOG_PANE_FIELD_ID);
        alert.showAndWait();
    }

    /**
     * Shows an error alert dialog with {@code title} and error message, {@code e},
     * and exits the application after the user has closed the alert dialog.
     */
    private void showFatalErrorDialogAndShutdown(String title, Throwable e) {
        logger.severe(title + " " + e.getMessage() + StringUtil.getDetails(e));
        showAlertDialogAndWait(Alert.AlertType.ERROR, title, e.getMessage(), e.toString());
        Platform.exit();
        System.exit(1);
    }

}
