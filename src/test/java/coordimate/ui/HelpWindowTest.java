package coordimate.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

public class HelpWindowTest {

    private static final String EXPECTED_USER_GUIDE_URL = "https://ay2627s1-cs2103t-w12-3.github.io/tp/";
    private static final List<String> COMMAND_WORDS = List.of(
            "add", "addevent", "addtag", "assign", "clear", "delete", "deleteevent", "deletetag", "dismiss",
            "edit", "editevent", "edittag", "exit", "filter", "help", "list", "listtags", "markattendance",
            "members", "search", "tutorial", "unassign");

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
    public void tutorialShortcut_runsHandlerOnlyForF2() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            HelpWindow helpWindow = new HelpWindow(new Stage());
            Node root = helpWindow.getRoot().getScene().getRoot();
            AtomicInteger runs = new AtomicInteger();

            KeyEvent beforeHandlerSet = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F2,
                    false, false, false, false);
            javafx.event.Event.fireEvent(root, beforeHandlerSet);

            helpWindow.setTutorialShortcutHandler(runs::incrementAndGet);
            javafx.event.Event.fireEvent(root, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.A,
                    false, false, false, false));
            assertEquals(0, runs.get());
            javafx.event.Event.fireEvent(root, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F2,
                    false, false, false, false));
            assertEquals(1, runs.get());
            helpWindow.hide();
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void constructor_configuresResizableCommandReference() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            HelpWindow helpWindow = new HelpWindow(new Stage());
            Stage root = helpWindow.getRoot();
            @SuppressWarnings("unchecked")
            TableView<HelpWindow.CommandUsage> commandTable =
                    (TableView<HelpWindow.CommandUsage>) root.getScene().lookup("#commandTable");
            VBox keyboardHelp = (VBox) root.getScene().lookup("#keyboardHelp");
            TextFlow commandModeHelp = (TextFlow) root.getScene().lookup("#commandModeHelp");
            TextFlow navigationModeHelp = (TextFlow) root.getScene().lookup("#navigationModeHelp");
            Text commandModeText = (Text) commandModeHelp.getChildren().get(0);
            Text navigationModeText = (Text) navigationModeHelp.getChildren().get(0);
            TextFlow syntaxNote = (TextFlow) root.getScene().lookup("#syntaxNote");
            Text syntaxNoteText = (Text) syntaxNote.getChildren().get(0);
            Label helpMessage = (Label) root.getScene().lookup("#helpMessage");
            Button copyButton = (Button) root.getScene().lookup("#copyButton");
            Button closeButton = (Button) root.getScene().lookup("#closeButton");

            assertTrue(root.isResizable());
            assertEquals(680.0, root.getMinWidth());
            assertEquals(450.0, root.getMinHeight());
            assertEquals(HelpWindow.COMMAND_USAGES, commandTable.getItems());
            assertEquals(TableView.CONSTRAINED_RESIZE_POLICY, commandTable.getColumnResizePolicy());
            assertTrue(helpMessage.getText().contains("Fn + F1"));
            assertTrue(helpMessage.getText().contains("Fn + F2"));
            assertNull(root.getScene().lookup("#tutorialButton"));
            assertFalse(commandTable.isEditable());
            assertEquals("Command mode (default): type commands; press Esc to navigate cards.",
                    commandModeText.getText());
            assertEquals("Navigation mode: press j for next, k for previous, s to switch views, or i to focus "
                    + "the command box. Other unmodified keys are disabled. Click the command box or a card list "
                    + "to switch modes.", navigationModeText.getText());
            assertEquals("Command mode is the default. Press Escape to enter Navigation mode. "
                    + "In Navigation mode, press J for the next card, K for the previous card, S to switch views, "
                    + "or I to focus the command box. Other unmodified keys are disabled. Clicking the command "
                    + "box enters Command mode; clicking a card list enters Navigation mode.",
                    keyboardHelp.getAccessibleText());
            assertEquals("Note: [ ] marks optional parameters; (A | B) means choose one. "
                    + "For edit, provide at least one field or tag operation. "
                    + "For editevent, provide at least one field.", syntaxNoteText.getText());
            assertEquals(List.of("Command", "Usage format"), commandTable.getColumns().stream()
                    .map(TableColumn::getText).toList());
            assertEquals("add", commandTable.getColumns().get(0).getCellObservableValue(0).getValue());
            assertEquals(HelpWindow.COMMAND_USAGES.get(0).format(),
                    commandTable.getColumns().get(1).getCellObservableValue(0).getValue());

            root.setWidth(root.getMinWidth());
            Platform.setImplicitExit(false);
            root.show();
            commandTable.scrollTo(COMMAND_WORDS.indexOf("edit"));
            root.getScene().getRoot().applyCss();
            root.getScene().getRoot().layout();
            assertTrue(commandModeHelp.getWidth() > 0);
            assertTrue(navigationModeHelp.getWidth() > 0);
            assertTrue(navigationModeHelp.getHeight() > navigationModeText.getFont().getSize() * 2);
            assertTrue(syntaxNote.getHeight() > syntaxNoteText.getFont().getSize() * 2);
            assertEquals(Region.USE_PREF_SIZE, copyButton.getMinWidth());
            assertEquals(Region.USE_PREF_SIZE, closeButton.getMinWidth());
            assertTrue(copyButton.getWidth() >= copyButton.prefWidth(-1));
            assertTrue(closeButton.getWidth() >= closeButton.prefWidth(-1));
            Text editUsage = root.getScene().getRoot().lookupAll(".usage-text").stream()
                    .map(Text.class::cast)
                    .filter(text -> text.getText().startsWith("edit "))
                    .findFirst()
                    .orElseThrow();
            assertTrue(editUsage.getWrappingWidth() > 0);
            assertTrue(editUsage.getLayoutBounds().getHeight() > editUsage.getFont().getSize() * 2, () ->
                    "Expected wrapped usage text, but text was " + editUsage.getWrappingWidth() + "x"
                            + editUsage.getLayoutBounds().getHeight() + " at font size "
                            + editUsage.getFont().getSize());
            assertTrue(closeButton.isCancelButton());
            closeButton.fire();
            assertFalse(root.isShowing());
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

}
