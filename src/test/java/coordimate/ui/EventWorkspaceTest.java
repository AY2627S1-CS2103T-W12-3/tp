package coordimate.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import coordimate.commons.core.GuiSettings;
import coordimate.logic.LogicManager;
import coordimate.model.ModelManager;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.storage.JsonCoordiMateStorage;
import coordimate.storage.JsonUserPrefsStorage;
import coordimate.storage.StorageManager;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

public class EventWorkspaceTest {
    @TempDir
    public Path temporaryFolder;

    @BeforeAll
    public static void startJavaFx() throws Exception {
        FutureTask<Void> started = new FutureTask<>(() -> {
            Platform.setImplicitExit(false);
            return null;
        });
        Platform.startup(started);
        started.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void submitEvent_selectsSavedEventAndShowsDetails() throws Exception {
        onFxThread(() -> {
            ModelManager model = new ModelManager();
            model.addEvent(new Event("Student Life Fair", new EventTime("09-10-2026"), new EventTime("10-10-2026")));
            MainWindow window = createWindow(model);
            try {
                submit(window, "addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00");
                TabPane tabs = (TabPane) window.getPrimaryStage().getScene().lookup("#views");
                assertEquals("Events (2)", tabs.getSelectionModel().getSelectedItem().getText());
                assertEquals("Final Concert", label(window, "selectedName").getText());
                assertEquals("3 hours", label(window, "duration").getText());
                assertEquals("OK", label(window, "feedbackStatus").getText());
                assertEquals("", input(window).getText());
                assertEquals(model.getCoordiMate(),
                        new JsonCoordiMateStorage(temporaryFolder.resolve("data.json")).readCoordiMate().orElseThrow());
                snapshot(window, "events-success.png");
                ListView<?> list = (ListView<?>) window.getPrimaryStage().getScene().lookup("#eventListView");
                list.getSelectionModel().selectFirst();
                assertEquals("Student Life Fair", label(window, "selectedName").getText());
                assertEquals("Time of day not specified", label(window, "duration").getText());
                list.requestFocus();
                list.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.DOWN,
                        false, false, false, false));
                assertEquals("Final Concert", label(window, "selectedName").getText());
                ToggleButton contacts = (ToggleButton) window.getPrimaryStage().getScene().lookup("#contactsView");
                contacts.fire();
                assertTrue(tabs.getSelectionModel().getSelectedItem().getText().startsWith("Contacts"));
            } finally {
                window.getPrimaryStage().close();
            }
        });
    }

    @Test
    public void rejectedCommand_isRetainedAndHighlighted_retryRestoresSuccess() throws Exception {
        onFxThread(() -> {
            MainWindow window = createWindow(new ModelManager());
            try {
                String invalid = "addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 24:00";
                submit(window, invalid);
                assertEquals(invalid, input(window).getText());
                assertTrue(input(window).getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));
                assertEquals("Retry", ((Button) window.getPrimaryStage().getScene().lookup("#submitButton")).getText());
                assertEquals("Rejected", label(window, "feedbackStatus").getText());
                assertTrue(window.getPrimaryStage().getScene().lookup("#feedbackPane").getPseudoClassStates()
                        .contains(PseudoClass.getPseudoClass("command-error")));
                assertTrue(label(window, "resultDisplay").getText().contains("dd-MM-yyyy"));
                snapshot(window, "events-error.png");
                input(window).setText("addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00");
                assertFalse(input(window).getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));
                Button retry = (Button) window.getPrimaryStage().getScene().lookup("#submitButton");
                retry.fire();
                assertEquals("OK", label(window, "feedbackStatus").getText());
                assertFalse(window.getPrimaryStage().getScene().lookup("#feedbackPane").getPseudoClassStates()
                        .contains(PseudoClass.getPseudoClass("command-error")));
            } finally {
                window.getPrimaryStage().close();
            }
        });
    }

    @Test
    public void emptyEventsAndNarrowWindow_showGuidanceAndStackDetails() throws Exception {
        onFxThread(() -> {
            MainWindow window = createWindow(new ModelManager(), 450);
            try {
                TabPane tabs = (TabPane) window.getPrimaryStage().getScene().lookup("#views");
                ToggleButton events = (ToggleButton) window.getPrimaryStage().getScene().lookup("#eventsView");
                events.fire();
                assertEquals("No event selected", label(window, "selectedName").getText());
                assertFalse(window.getPrimaryStage().getScene().lookup("#eventDetails").isManaged());
                assertTrue(label(window, "selectionHint").getText().contains("addevent"));
                window.getPrimaryStage().getScene().getRoot().applyCss();
                window.getPrimaryStage().getScene().getRoot().layout();
                SplitPane panes = (SplitPane) window.getPrimaryStage().getScene().lookup("#eventSplitPane");
                assertEquals(Orientation.VERTICAL, panes.getOrientation());
                snapshot(window, "events-narrow.png");
                submit(window, "addevent evn/Student Life Fair st/09-10-2026 et/10-10-2026");
                snapshot(window, "events-narrow-populated.png");
                submit(window, "addevent evn/Student Life Fair st/09-10-2026 et/10-10-2026");
                assertEquals("Rejected", label(window, "feedbackStatus").getText());
                snapshot(window, "events-narrow-error.png");
            } finally {
                window.getPrimaryStage().close();
            }
        });
    }

    private MainWindow createWindow(ModelManager model) {
        return createWindow(model, 1100);
    }

    private MainWindow createWindow(ModelManager model, double width) {
        model.setGuiSettings(new GuiSettings(width, 800, 0, 0));
        Path path = temporaryFolder.resolve("data.json");
        LogicManager logic = new LogicManager(model, new StorageManager(new JsonCoordiMateStorage(path),
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));
        MainWindow window = new MainWindow(new Stage(), logic, path);
        window.fillInnerParts();
        window.show();
        return window;
    }

    private TextField input(MainWindow window) {
        return (TextField) window.getPrimaryStage().getScene().lookup("#commandTextField");
    }

    private Label label(MainWindow window, String id) {
        return (Label) window.getPrimaryStage().getScene().lookup("#" + id);
    }

    private void submit(MainWindow window, String command) {
        input(window).setText(command);
        input(window).fireEvent(new ActionEvent());
    }

    private void snapshot(MainWindow window, String name) throws Exception {
        WritableImage image = window.getPrimaryStage().getScene().snapshot(null);
        BufferedImage output = new BufferedImage((int) image.getWidth(), (int) image.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < output.getHeight(); y++) {
            for (int x = 0; x < output.getWidth(); x++) {
                output.setRGB(x, y, image.getPixelReader().getArgb(x, y));
            }
        }
        Path directory = Path.of("build", "ui-preview");
        Files.createDirectories(directory);
        ImageIO.write(output, "png", directory.resolve(name).toFile());
    }

    private void onFxThread(FxAction action) throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            action.run();
            return null;
        });
        Platform.runLater(task);
        task.get(15, TimeUnit.SECONDS);
    }

    @FunctionalInterface
    private interface FxAction {
        void run() throws Exception;
    }
}
