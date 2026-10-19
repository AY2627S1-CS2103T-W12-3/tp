package coordimate.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import coordimate.logic.commands.CommandResult;
import coordimate.logic.parser.exceptions.ParseException;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;

public class CommandBoxTest {
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
    }

    @Test
    public void blankEnter_onlySentWhenDeletionConfirmationIsPending() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            List<String> submitted = new ArrayList<>();
            AtomicBoolean pending = new AtomicBoolean(false);
            Region root = new CommandBox(text -> {
                submitted.add(text);
                return new CommandResult("Result");
            }, pending::get).getRoot();
            new Scene(root);
            root.applyCss();
            Button button = (Button) root.lookup("#submitButton");
            TextField input = (TextField) root.lookup("#commandTextField");

            button.fire();
            assertTrue(submitted.isEmpty());

            pending.set(true);
            button.fire();
            assertEquals(List.of(""), submitted);

            pending.set(false);
            input.setText("list");
            button.fire();
            assertEquals(List.of("", "list"), submitted);
            assertEquals("", input.getText());
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void arrowKeys_recallCommandsAndRestoreDraft() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            List<String> submitted = new ArrayList<>();
            Region root = new CommandBox(text -> {
                submitted.add(text);
                return new CommandResult("Result");
            }, () -> false).getRoot();
            new Scene(root);
            TextField input = (TextField) root.lookup("#commandTextField");
            Button button = (Button) root.lookup("#submitButton");

            pressArrow(input, KeyCode.UP);
            assertEquals("", input.getText());

            for (String command : List.of("list", "search Aisha", "list", "  filter tag/EXCO  ")) {
                input.setText(command);
                button.fire();
            }
            input.setText("unfinished draft");

            pressArrow(input, KeyCode.UP);
            assertEquals("  filter tag/EXCO  ", input.getText());
            input.setText("edited filter");
            pressArrow(input, KeyCode.UP);
            assertEquals("list", input.getText());
            pressArrow(input, KeyCode.UP);
            assertEquals("search Aisha", input.getText());
            pressArrow(input, KeyCode.UP);
            assertEquals("list", input.getText());
            pressArrow(input, KeyCode.UP);
            assertEquals("list", input.getText());

            pressArrow(input, KeyCode.DOWN);
            assertEquals("search Aisha", input.getText());
            pressArrow(input, KeyCode.DOWN);
            assertEquals("list", input.getText());
            pressArrow(input, KeyCode.DOWN);
            assertEquals("  filter tag/EXCO  ", input.getText());
            pressArrow(input, KeyCode.DOWN);
            assertEquals("unfinished draft", input.getText());
            pressArrow(input, KeyCode.DOWN);
            assertEquals("unfinished draft", input.getText());
            assertEquals(List.of("list", "search Aisha", "list", "  filter tag/EXCO  "), submitted);
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void failedCommand_isRecalledAndCanBeEditedBeforeResubmission() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            List<String> submitted = new ArrayList<>();
            Region root = new CommandBox(text -> {
                submitted.add(text);
                if (text.equals("bad")) {
                    throw new ParseException("Invalid command");
                }
                return new CommandResult("Result");
            }, () -> false).getRoot();
            new Scene(root);
            TextField input = (TextField) root.lookup("#commandTextField");
            Button button = (Button) root.lookup("#submitButton");

            input.setText("bad");
            button.fire();
            assertEquals("bad", input.getText());
            input.setText("unfinished correction");
            pressArrow(input, KeyCode.UP);
            assertEquals("bad", input.getText());
            pressArrow(input, KeyCode.DOWN);
            assertEquals("unfinished correction", input.getText());

            pressArrow(input, KeyCode.UP);
            input.setText("list");
            button.fire();
            assertEquals("", input.getText());
            pressArrow(input, KeyCode.UP);
            assertEquals("list", input.getText());
            pressArrow(input, KeyCode.UP);
            assertEquals("bad", input.getText());
            assertEquals(List.of("bad", "list"), submitted);
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void deleteConfirmation_pausesHistoryAndDoesNotStoreResponses() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            List<String> submitted = new ArrayList<>();
            AtomicBoolean pending = new AtomicBoolean(false);
            Region root = new CommandBox(text -> {
                submitted.add(text);
                return new CommandResult("Result");
            }, pending::get).getRoot();
            new Scene(root);
            TextField input = (TextField) root.lookup("#commandTextField");
            Button button = (Button) root.lookup("#submitButton");

            input.setText("delete 1");
            button.fire();
            pending.set(true);
            input.setText("y");
            pressArrow(input, KeyCode.UP);
            pressArrow(input, KeyCode.DOWN);
            assertEquals("y", input.getText());
            button.fire();

            pending.set(true);
            button.fire();
            pending.set(false);
            input.setText("   ");
            button.fire();
            pressArrow(input, KeyCode.UP);
            assertEquals("delete 1", input.getText());
            pressArrow(input, KeyCode.DOWN);
            assertEquals("   ", input.getText());
            assertEquals(List.of("delete 1", "y", ""), submitted);
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void sidebarCommand_resetsHistoryNavigation() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            CommandBox commandBox = new CommandBox(text -> new CommandResult("Result"), () -> false);
            Region root = commandBox.getRoot();
            new Scene(root);
            TextField input = (TextField) root.lookup("#commandTextField");
            Button button = (Button) root.lookup("#submitButton");

            input.setText("list");
            button.fire();
            input.setText("search Aisha");
            button.fire();
            pressArrow(input, KeyCode.UP);
            assertEquals("search Aisha", input.getText());

            commandBox.setCommand("add");
            pressArrow(input, KeyCode.UP);
            assertEquals("search Aisha", input.getText());
            pressArrow(input, KeyCode.DOWN);
            assertEquals("add", input.getText());
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    private static void pressArrow(TextField input, KeyCode keyCode) {
        input.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", keyCode,
                false, false, false, false));
    }
}
