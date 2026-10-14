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
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
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
}
