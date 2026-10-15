package coordimate.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

public class HelpWindowTest {

    private static final String EXPECTED_USER_GUIDE_URL = "https://ay2627s1-cs2103t-w12-3.github.io/tp/";
    private static final List<String> COMMAND_WORDS = List.of(
            "add", "addevent", "addtag", "assign", "clear", "delete", "deleteevent", "deletetag", "edit",
            "editevent", "edittag", "exit", "filter", "help", "list", "listtags", "markattendance", "members",
            "search", "unassign");

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
    public void commandUsages_containsEverySupportedCommand() {
        assertEquals(EXPECTED_USER_GUIDE_URL, HelpWindow.USERGUIDE_URL);
        for (String commandWord : COMMAND_WORDS) {
            assertTrue(HelpWindow.COMMAND_USAGES.stream()
                    .anyMatch(usage -> usage.command().equals(commandWord)), commandWord);
        }
        assertEquals(COMMAND_WORDS.size(), HelpWindow.COMMAND_USAGES.size());
        assertEquals("filter FIELD/VALUE [FIELD/VALUE]...", HelpWindow.COMMAND_USAGES.stream()
                .filter(usage -> usage.command().equals("filter"))
                .findFirst()
                .orElseThrow()
                .format());
    }

    @Test
    public void constructor_configuresResizableCommandReference() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            HelpWindow helpWindow = new HelpWindow(new Stage());
            Stage root = helpWindow.getRoot();
            @SuppressWarnings("unchecked")
            TableView<HelpWindow.CommandUsage> commandTable =
                    (TableView<HelpWindow.CommandUsage>) root.getScene().lookup("#commandTable");
            Label syntaxNote = (Label) root.getScene().lookup("#syntaxNote");
            Button closeButton = (Button) root.getScene().lookup("#closeButton");

            assertTrue(root.isResizable());
            assertEquals(680.0, root.getMinWidth());
            assertEquals(450.0, root.getMinHeight());
            assertEquals(HelpWindow.COMMAND_USAGES, commandTable.getItems());
            assertFalse(commandTable.isEditable());
            assertEquals("Note: [ ] marks optional parameters; (A | B) means choose one. "
                    + "For edit, provide at least one field or tag operation. "
                    + "For editevent, provide at least one field.", syntaxNote.getText());
            assertEquals(List.of("Command", "Usage format"), commandTable.getColumns().stream()
                    .map(TableColumn::getText).toList());
            assertEquals("add", commandTable.getColumns().get(0).getCellObservableValue(0).getValue());
            assertEquals(HelpWindow.COMMAND_USAGES.get(0).format(),
                    commandTable.getColumns().get(1).getCellObservableValue(0).getValue());
            assertTrue(closeButton.isCancelButton());
            closeButton.fire();
            assertFalse(root.isShowing());
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }
}
