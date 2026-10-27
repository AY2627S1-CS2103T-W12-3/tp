package coordimate.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import coordimate.commons.core.GuiSettings;
import coordimate.logic.LogicManager;
import coordimate.model.ModelManager;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.person.Person;
import coordimate.model.tag.Tag;
import coordimate.storage.JsonCoordiMateStorage;
import coordimate.storage.JsonUserPrefsStorage;
import coordimate.storage.StorageManager;
import coordimate.testutil.PersonBuilder;
import coordimate.testutil.TypicalPersons;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

/**
 * Checks that the complete desktop shell loads and its view controls stay connected.
 */
public class MainWindowTest {

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
    public void switchViews_updatesWorkspaceAndCommandHints() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            Stage stage = new Stage();
            ModelManager model = new ModelManager();
            model.addPerson(new PersonBuilder(TypicalPersons.ALICE).withTags("EXCO").build());
            model.updateFilteredPersonList(person -> false);
            MainWindow window = new MainWindow(stage, new LogicManager(model, null),
                    Path.of("data", "coordimate.json"));
            window.fillInnerParts();
            stage.getScene().getRoot().applyCss();

            TabPane views = (TabPane) stage.getScene().getRoot().lookup("#views");
            TextFlow prefixHelp = (TextFlow) stage.getScene().getRoot().lookup("#prefixHelp");
            ToggleButton events = (ToggleButton) stage.getScene().getRoot().lookup("#eventsView");
            ToggleButton tags = (ToggleButton) stage.getScene().getRoot().lookup("#tagsView");
            ToggleButton contacts = (ToggleButton) stage.getScene().getRoot().lookup("#contactsView");

            events.fire();
            stage.getScene().getRoot().applyCss();
            assertEquals("Events", views.getSelectionModel().getSelectedItem().getText().split(" ")[0]);
            assertEquals("Bold", events.getFont().getStyle());
            assertTrue(getPrefixHelpText(prefixHelp).contains("evn/Event"));

            tags.fire();
            stage.getScene().getRoot().applyCss();
            assertEquals("Tags", views.getSelectionModel().getSelectedItem().getText());
            assertEquals("Bold", tags.getFont().getStyle());
            assertEquals("Regular", events.getFont().getStyle());
            assertTrue(getPrefixHelpText(prefixHelp).contains("t/Tag"));
            @SuppressWarnings("unchecked")
            ListView<String> tagList = (ListView<String>) stage.getScene().getRoot().lookup("#tagListView");
            tagList.getSelectionModel().selectFirst();
            assertEquals("EXCO", ((Label) stage.getScene().getRoot().lookup("#selectedTagName")).getText());
            ListView<?> tagMembers = (ListView<?>) stage.getScene().getRoot().lookup("#tagMemberListView");
            assertEquals(List.of("Alice Pauline"), tagMembers.getItems().stream()
                    .map(person -> ((Person) person).getName().getFullName()).toList());
            assertEquals("Members (1)", ((Label) stage.getScene().getRoot().lookup("#tagMemberCount")).getText());

            contacts.fire();
            stage.getScene().getRoot().applyCss();
            assertTrue(views.getSelectionModel().getSelectedItem().getText().startsWith("Contacts"));
            assertEquals("Bold", contacts.getFont().getStyle());
            assertTrue(getPrefixHelpText(prefixHelp).contains("n/Name"));
            stage.close();
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void topMenuHelpItem_opensOwnedHelpWindowAndShowsShortcutTooltip() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            Stage stage = new Stage();
            MainWindow window = new MainWindow(stage, new LogicManager(new ModelManager(), null),
                    Path.of("data", "coordimate.json"));
            window.fillInnerParts();

            Label helpLabel = (Label) stage.getScene().getRoot().lookup("#helpLabel");
            Label tutorialLabel = (Label) stage.getScene().getRoot().lookup("#tutorialLabel");
            HBox topMenuRow = (HBox) stage.getScene().getRoot().lookup("#topMenuRow");
            MenuBar menuBar = (MenuBar) stage.getScene().getRoot().lookup("#menuBar");
            assertEquals("Help", helpLabel.getText());
            assertEquals("Tutorial", tutorialLabel.getText());
            assertEquals(helpLabel.getStyleClass(), tutorialLabel.getStyleClass());
            assertEquals(helpLabel.getAccessibleRole(), tutorialLabel.getAccessibleRole());
            assertEquals("Open the CoordiMate tutorial", tutorialLabel.getAccessibleText());
            assertEquals("Open Help (Fn + F1)", helpLabel.getTooltip().getText());
            assertEquals(200, helpLabel.getTooltip().getShowDelay().toMillis());
            assertEquals(List.of("File"), menuBar.getMenus().stream().map(menu -> menu.getText()).toList());
            assertSame(topMenuRow, menuBar.getParent());
            assertSame(topMenuRow, helpLabel.getParent());
            assertSame(topMenuRow, tutorialLabel.getParent());
            assertEquals(List.of(menuBar, helpLabel, tutorialLabel), topMenuRow.getChildren());

            window.show();
            javafx.event.Event.fireEvent(helpLabel, mouseClickEvent());
            Stage helpStage = Window.getWindows().stream()
                    .filter(Stage.class::isInstance)
                    .map(Stage.class::cast)
                    .filter(candidate -> "Help".equals(candidate.getTitle()))
                    .filter(candidate -> candidate.getOwner() == stage)
                    .findFirst()
                    .orElseThrow();
            assertTrue(helpStage.isShowing());
            assertSame(stage, helpStage.getOwner());

            KeyEvent enterEvent = keyEvent(KeyCode.ENTER);
            helpLabel.getOnKeyPressed().handle(enterEvent);
            assertTrue(enterEvent.isConsumed());
            assertTrue(helpStage.isShowing());

            KeyEvent otherKeyEvent = keyEvent(KeyCode.A);
            helpLabel.getOnKeyPressed().handle(otherKeyEvent);
            assertFalse(otherKeyEvent.isConsumed());

            helpStage.hide();
            KeyEvent spaceEvent = keyEvent(KeyCode.SPACE);
            helpLabel.getOnKeyPressed().handle(spaceEvent);
            assertTrue(spaceEvent.isConsumed());
            assertTrue(helpStage.isShowing());

            helpStage.hide();
            javafx.event.Event.fireEvent(stage.getScene().getRoot(), keyEvent(KeyCode.F1));
            assertTrue(helpStage.isShowing());

            // F2 pressed while the Help window is in front opens the tutorial on the main window.
            javafx.event.Event.fireEvent(helpStage.getScene().getRoot(), keyEvent(KeyCode.F2));
            assertFalse(helpStage.isShowing());
            assertTrue(stage.getScene().lookup("#tutorialOverlay").isVisible());
            window.stop();
            stage.close();
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void topMenuTutorialItem_opensTutorialOverlay() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            Stage stage = new Stage();
            ModelManager model = new ModelManager();
            Person contact = new PersonBuilder(TypicalPersons.ALICE).build();
            model.addPerson(contact);
            EventTime time = new EventTime("25-10-2026");
            model.addEvent(new Event("Welcome Fair", time, time, List.of(contact.getName()), Map.of()));
            LogicManager logic = new LogicManager(model, null);
            MainWindow window = new MainWindow(stage, logic, Path.of("data", "coordimate.json"));
            window.fillInnerParts();
            window.show();
            TabPane views = (TabPane) stage.getScene().lookup("#views");
            views.getSelectionModel().select(1);
            window.handleHelp();
            Stage helpStage = findOwnedStage(stage, "Help");
            Label tutorialLabel = (Label) stage.getScene().lookup("#tutorialLabel");
            javafx.event.Event.fireEvent(tutorialLabel, mouseClickEvent());
            stage.getScene().getRoot().applyCss();
            stage.getScene().getRoot().layout();

            StackPane tutorialOverlay = (StackPane) stage.getScene().lookup("#tutorialOverlay");
            assertTrue(tutorialOverlay.isVisible());
            assertFalse(helpStage.isShowing());
            assertEquals("Step 1 of 16", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            assertEquals(0, views.getSelectionModel().getSelectedIndex());
            Button nextButton = (Button) stage.getScene().lookup("#nextButton");
            TextField commandInput = (TextField) stage.getScene().lookup("#commandTextField");
            Button submitButton = (Button) stage.getScene().lookup("#submitButton");
            assertEquals("", commandInput.getText());
            assertTrue(commandInput.isDisabled());
            assertTrue(submitButton.isDisabled());
            commandInput.setText("add n/Tom p/9081811 e/tomjohn@email.com r/EXCO");
            commandInput.fireEvent(new ActionEvent());
            assertEquals(1, logic.getPersonList().size());
            assertTrue(stage.getScene().lookup("#commandExamples") == null);
            for (int step = 0; step < 4; step++) {
                nextButton.fire();
            }
            assertEquals("Step 5 of 16", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            assertTrue(stage.getScene().lookup("#targetHighlight").isVisible());
            nextButton.fire();
            assertEquals("Step 6 of 16", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            ListView<?> contactList = (ListView<?>) stage.getScene().lookup("#personListView");
            assertEquals(0, contactList.getSelectionModel().getSelectedIndex());
            assertTrue(stage.getScene().lookup("#targetHighlight").isVisible());
            nextButton.fire();
            assertEquals("Step 7 of 16", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            assertEquals(1, views.getSelectionModel().getSelectedIndex());
            stage.getScene().getRoot().applyCss();
            stage.getScene().getRoot().layout();
            nextButton.fire();
            assertEquals("Step 8 of 16", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            ListView<?> eventList = (ListView<?>) stage.getScene().lookup("#eventListView");
            assertEquals(0, eventList.getSelectionModel().getSelectedIndex());
            // The list is the fallback target until JavaFX creates the selected card on the next pulse.
            assertTutorialHighlights(stage, "#eventListView");
            nextButton.fire();
            assertEquals("Step 9 of 16", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            assertTrue(stage.getScene().lookup("#targetHighlight").isVisible());
            nextButton.fire();
            assertEquals("Step 10 of 16", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            VBox members = (VBox) stage.getScene().lookup("#members");
            assertEquals(1, members.getChildren().size());
            Bounds memberBounds = members.localToScene(members.getLayoutBounds());
            Bounds highlightBounds = stage.getScene().lookup("#targetHighlight")
                    .localToScene(stage.getScene().lookup("#targetHighlight").getLayoutBounds());
            assertTrue(highlightBounds.intersects(memberBounds));
            Label memberCount = (Label) stage.getScene().lookup("#memberCount");
            assertFalse(highlightBounds.intersects(memberCount.localToScene(memberCount.getLayoutBounds())));
            nextButton.fire();
            assertEquals("Step 11 of 16", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            assertEquals(2, views.getSelectionModel().getSelectedIndex());
            assertTrue(((ToggleButton) stage.getScene().lookup("#tagsView")).isSelected());
            assertTutorialHighlights(stage, "#tagsView");
            nextButton.fire();
            assertEquals("Step 12 of 16", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            assertTutorialHighlights(stage, "#tagListView");
            nextButton.fire();
            assertEquals("Step 13 of 16", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            assertEquals(0, ((ListView<?>) stage.getScene().lookup("#tagListView"))
                    .getSelectionModel().getSelectedIndex());
            assertTutorialHighlights(stage, "#tagDetailsPane");
            nextButton.fire();
            assertEquals("Step 14 of 16", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            assertEquals(2, views.getSelectionModel().getSelectedIndex());
            assertFalse(stage.getScene().lookup("#targetHighlight").isVisible());
            nextButton.fire();
            assertEquals("Step 15 of 16", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            assertEquals(2, views.getSelectionModel().getSelectedIndex());
            assertTutorialHighlights(stage, "#helpLabel");
            nextButton.fire();
            assertEquals("Step 16 of 16", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            assertEquals(2, views.getSelectionModel().getSelectedIndex());
            assertTutorialHighlights(stage, "#tutorialLabel");
            assertEquals("Finish →", nextButton.getText());
            assertEquals(1, logic.getEventList().size());
            Button closeButton = (Button) stage.getScene().lookup("#closeButton");
            closeButton.fire();
            assertTrue(logic.hasSeenTutorial());
            assertFalse(submitButton.isDisabled());
            assertEquals(2, views.getSelectionModel().getSelectedIndex());

            window.handleHelp();
            KeyEvent enterEvent = keyEvent(KeyCode.ENTER);
            tutorialLabel.getOnKeyPressed().handle(enterEvent);
            assertTrue(enterEvent.isConsumed());
            assertTrue(tutorialOverlay.isVisible());
            assertFalse(helpStage.isShowing());
            assertEquals("Step 1 of 16", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            window.stop();
            stage.close();
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void tutorialKeyboardShortcuts_blockAppAndRestoreInputOnClose() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            Stage stage = new Stage();
            LogicManager logic = new LogicManager(new ModelManager(), null);
            MainWindow window = new MainWindow(stage, logic, Path.of("data", "coordimate.json"));
            window.fillInnerParts();
            window.show();
            Label tutorialLabel = (Label) stage.getScene().lookup("#tutorialLabel");
            Label helpLabel = (Label) stage.getScene().lookup("#helpLabel");
            TextField commandInput = (TextField) stage.getScene().lookup("#commandTextField");
            Label counter = (Label) stage.getScene().lookup("#stepCounter");

            tutorialLabel.fireEvent(mouseClickEvent());
            assertTrue(commandInput.isDisabled());
            Button nextButton = (Button) stage.getScene().lookup("#nextButton");
            assertTrue(keyReachesTarget(nextButton, keyEvent(KeyCode.F1)));
            assertTrue(keyReachesTarget(nextButton, typedKeyEvent("i")));
            assertTrue(commandInput.isDisabled());
            assertFalse(keyReachesTarget(helpLabel, keyEvent(KeyCode.RIGHT)));
            assertEquals("Step 2 of 16", counter.getText());
            tutorialLabel.fireEvent(mouseClickEvent());
            assertEquals("Step 2 of 16", counter.getText());
            window.handleHelp();
            assertTrue(stage.getScene().lookup("#tutorialOverlay").isVisible());

            assertFalse(keyReachesTarget(helpLabel, keyEvent(KeyCode.LEFT)));
            assertEquals("Step 1 of 16", counter.getText());
            assertFalse(keyReachesTarget(helpLabel, keyEvent(KeyCode.ESCAPE)));
            assertFalse(stage.getScene().lookup("#tutorialOverlay").isVisible());
            assertTrue(logic.hasSeenTutorial());
            assertFalse(commandInput.isDisabled());

            commandInput.fireEvent(keyEvent(KeyCode.ESCAPE));
            Label modeLabel = (Label) stage.getScene().lookup("#interactionModeLabel");
            assertTrue(modeLabel.getText().startsWith("NAVIGATION"));
            tutorialLabel.fireEvent(mouseClickEvent());
            assertEquals("Step 1 of 16", counter.getText());
            assertFalse(keyReachesTarget(helpLabel, keyEvent(KeyCode.ESCAPE)));
            assertTrue(modeLabel.getText().startsWith("NAVIGATION"));
            assertTrue(commandInput.isDisabled());
            window.stop();
            stage.close();
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void tutorialCommandAndShortcut_openTutorialFromFirstStep() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            Stage stage = new Stage();
            MainWindow window = null;
            try {
                StorageManager storage = new StorageManager(
                        new JsonCoordiMateStorage(temporaryFolder.resolve("tutorial-data.json")),
                        new JsonUserPrefsStorage(temporaryFolder.resolve("tutorial-prefs.json")));
                LogicManager logic = new LogicManager(new ModelManager(), storage);
                window = new MainWindow(stage, logic, temporaryFolder.resolve("tutorial-data.json"));
                window.fillInnerParts();
                window.show();
                Node overlay = stage.getScene().lookup("#tutorialOverlay");
                Label counter = (Label) stage.getScene().lookup("#stepCounter");
                Label tutorialLabel = (Label) stage.getScene().lookup("#tutorialLabel");
                TextField commandInput = (TextField) stage.getScene().lookup("#commandTextField");
                Button submitButton = (Button) stage.getScene().lookup("#submitButton");
                Button nextButton = (Button) stage.getScene().lookup("#nextButton");
                assertTrue(tutorialLabel.getTooltip().getText().contains(TutorialWindow.TUTORIAL_SHORTCUT));

                commandInput.setText("tutorial");
                submitButton.fire();
                assertTrue(overlay.isVisible());
                assertEquals("Step 1 of 16", counter.getText());
                assertTrue(commandInput.isDisabled());
                assertTrue(commandInput.getText().isEmpty());

                nextButton.fire();
                assertFalse(keyReachesTarget(commandInput, keyEvent(KeyCode.F2)));
                assertEquals("Step 2 of 16", counter.getText());
                assertFalse(keyReachesTarget(commandInput, keyEvent(KeyCode.ESCAPE)));
                assertFalse(overlay.isVisible());
                assertFalse(commandInput.isDisabled());

                assertFalse(keyReachesTarget(commandInput, keyEvent(KeyCode.F2)));
                assertTrue(overlay.isVisible());
                assertEquals("Step 1 of 16", counter.getText());
            } finally {
                if (window != null) {
                    window.stop();
                }
                stage.close();
            }
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void tutorialCardAndMemberSteps_rehighlightAfterJavaFxLayout() throws Exception {
        FutureTask<MainWindow> setup = new FutureTask<>(() -> {
            Stage stage = new Stage();
            ModelManager model = new ModelManager();
            Person contact = new PersonBuilder(TypicalPersons.ALICE).build();
            model.addPerson(contact);
            EventTime time = new EventTime("25-10-2026");
            model.addEvent(new Event("Welcome Fair", time, time, List.of(contact.getName()), Map.of()));
            MainWindow window = new MainWindow(stage, new LogicManager(model, null),
                    Path.of("data", "coordimate.json"));
            window.fillInnerParts();
            window.show();
            Label tutorialLabel = (Label) stage.getScene().lookup("#tutorialLabel");
            tutorialLabel.fireEvent(mouseClickEvent());
            Button nextButton = (Button) stage.getScene().lookup("#nextButton");
            for (int step = 0; step < 4; step++) {
                nextButton.fire();
            }
            assertEquals("Step 5 of 16", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            return window;
        });
        Platform.runLater(setup);
        MainWindow window = setup.get(10, TimeUnit.SECONDS);

        FutureTask<Void> verifyCard = new FutureTask<>(() -> {
            Stage stage = window.getPrimaryStage();
            ListView<?> contacts = (ListView<?>) stage.getScene().lookup("#personListView");
            Node highlight = stage.getScene().lookup("#targetHighlight");
            assertEquals(0, contacts.getSelectionModel().getSelectedIndex());
            assertTrue(highlight.localToScene(highlight.getLayoutBounds()).getHeight() < contacts.getHeight());
            Button nextButton = (Button) stage.getScene().lookup("#nextButton");
            for (int step = 0; step < 5; step++) {
                nextButton.fire();
            }
            assertEquals("Step 10 of 16", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            return null;
        });
        Platform.runLater(verifyCard);
        verifyCard.get(10, TimeUnit.SECONDS);

        FutureTask<Void> verifyMembers = new FutureTask<>(() -> {
            Stage stage = window.getPrimaryStage();
            assertTutorialHighlights(stage, "#members");
            window.stop();
            stage.close();
            return null;
        });
        Platform.runLater(verifyMembers);
        verifyMembers.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void tutorialWithoutContacts_highlightsEmptyListAndKeepsDetailsHint() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            Stage stage = new Stage();
            MainWindow window = new MainWindow(stage, new LogicManager(new ModelManager(), null),
                    Path.of("data", "coordimate.json"));
            window.fillInnerParts();
            window.show();
            Label tutorialLabel = (Label) stage.getScene().lookup("#tutorialLabel");
            javafx.event.Event.fireEvent(tutorialLabel, mouseClickEvent());
            stage.getScene().getRoot().applyCss();
            stage.getScene().getRoot().layout();

            Button nextButton = (Button) stage.getScene().lookup("#nextButton");
            for (int step = 0; step < 4; step++) {
                nextButton.fire();
            }
            assertEquals("Step 5 of 16", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            assertTrue(stage.getScene().lookup("#targetHighlight").isVisible());
            Label contactSelectionHint = (Label) stage.getScene().lookup("#personListPanelPlaceholder")
                    .lookup("#selectionHint");
            assertEquals("Select a contact to view details.", contactSelectionHint.getText());
            nextButton.fire();
            assertTrue(stage.getScene().lookup("#targetHighlight").isVisible());
            nextButton.fire();
            stage.getScene().getRoot().applyCss();
            stage.getScene().getRoot().layout();
            nextButton.fire();
            assertEquals("Step 8 of 16", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            assertEquals(0, ((ListView<?>) stage.getScene().lookup("#eventListView")).getItems().size());
            assertTrue(stage.getScene().lookup("#targetHighlight").isVisible());
            nextButton.fire();
            assertEquals("Step 9 of 16", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            Label eventSelectionHint = (Label) stage.getScene().lookup("#eventListPanelPlaceholder")
                    .lookup("#selectionHint");
            assertTrue(eventSelectionHint.isVisible());
            nextButton.fire();
            assertEquals("Step 10 of 16", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            assertTrue(stage.getScene().lookup("#targetHighlight").isVisible());
            window.stop();
            stage.close();
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    private static void assertTutorialHighlights(Stage stage, String targetId) {
        stage.getScene().getRoot().applyCss();
        stage.getScene().getRoot().layout();
        Node highlight = stage.getScene().lookup("#targetHighlight");
        Node target = stage.getScene().lookup(targetId);
        assertTrue(highlight.isVisible());
        assertTrue(highlight.localToScene(highlight.getLayoutBounds())
                .intersects(target.localToScene(target.getLayoutBounds())));
    }

    private static Stage findOwnedStage(Stage owner, String title) {
        return Window.getWindows().stream()
                .filter(Stage.class::isInstance)
                .map(Stage.class::cast)
                .filter(candidate -> title.equals(candidate.getTitle()))
                .filter(candidate -> candidate.getOwner() == owner)
                .findFirst()
                .orElseThrow();
    }

    @Test
    public void cardNavigation_routesShortcutsToActiveView() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            Stage stage = new Stage();
            MainWindow window = null;
            try {
                ModelManager model = new ModelManager();
                Person firstContact = new PersonBuilder(TypicalPersons.ALICE).withTags("Alpha").build();
                Person secondContact = new PersonBuilder(TypicalPersons.BENSON).withTags("Beta").build();
                model.addPerson(firstContact);
                model.addPerson(secondContact);
                EventTime time = new EventTime("25-10-2026");
                Event firstEvent = new Event("First Event", time, time);
                Event secondEvent = new Event("Second Event", time, time);
                model.addEvent(firstEvent);
                model.addEvent(secondEvent);
                window = new MainWindow(stage, new LogicManager(model, null), Path.of("data", "coordimate.json"));
                window.show();
                window.fillInnerParts();
                stage.getScene().getRoot().applyCss();

                TextField input = (TextField) stage.getScene().getRoot().lookup("#commandTextField");
                @SuppressWarnings("unchecked")
                ListView<Person> contacts = (ListView<Person>) stage.getScene().getRoot().lookup("#personListView");
                @SuppressWarnings("unchecked")
                ListView<Event> events = (ListView<Event>) stage.getScene().getRoot().lookup("#eventListView");
                @SuppressWarnings("unchecked")
                ListView<String> tags = (ListView<String>) stage.getScene().getRoot().lookup("#tagListView");
                ToggleButton contactsView = (ToggleButton) stage.getScene().getRoot().lookup("#contactsView");
                ToggleButton eventsView = (ToggleButton) stage.getScene().getRoot().lookup("#eventsView");
                ToggleButton tagsView = (ToggleButton) stage.getScene().getRoot().lookup("#tagsView");
                Label mode = (Label) stage.getScene().getRoot().lookup("#interactionModeLabel");
                Button submit = (Button) stage.getScene().getRoot().lookup("#submitButton");

                assertEquals("COMMAND · Esc to navigate", mode.getText());
                assertFalse(input.isDisabled());
                assertFalse(submit.isDisabled());
                assertTrue(keyReachesTarget(input, keyEvent(KeyCode.J)));
                input.clear();
                assertFalse(keyReachesTarget(input, keyEvent(KeyCode.ESCAPE)));
                assertEquals("NAVIGATION · j/k move · s switch view · i to type", mode.getText());
                assertFalse(keyReachesTarget(mode, keyEvent(KeyCode.ESCAPE)));
                assertTrue(input.isDisabled());
                assertTrue(submit.isDisabled());
                assertFalse(keyReachesTarget(input, keyEvent(KeyCode.J)));
                assertEquals(firstContact, contacts.getSelectionModel().getSelectedItem());
                assertTrue(input.getText().isEmpty());
                assertFalse(keyReachesTarget(input, keyEvent(KeyCode.J)));
                assertEquals(secondContact, contacts.getSelectionModel().getSelectedItem());
                assertTrue(input.getText().isEmpty());
                assertFalse(keyReachesTarget(input, keyEvent(KeyCode.K)));
                assertEquals(firstContact, contacts.getSelectionModel().getSelectedItem());
                assertFalse(keyReachesTarget(input, keyEvent(KeyCode.K)));
                assertEquals(firstContact, contacts.getSelectionModel().getSelectedItem());

                eventsView.fire();
                assertFalse(keyReachesTarget(input, keyEvent(KeyCode.J)));
                assertEquals(firstEvent, events.getSelectionModel().getSelectedItem());
                assertFalse(keyReachesTarget(input, keyEvent(KeyCode.J)));
                assertEquals(secondEvent, events.getSelectionModel().getSelectedItem());
                assertFalse(keyReachesTarget(input, keyEvent(KeyCode.K)));
                assertEquals(firstEvent, events.getSelectionModel().getSelectedItem());

                tagsView.fire();
                assertFalse(keyReachesTarget(input, keyEvent(KeyCode.K)));
                assertEquals(tags.getItems().size() - 1, tags.getSelectionModel().getSelectedIndex());
                assertFalse(keyReachesTarget(input, keyEvent(KeyCode.J)));
                assertEquals(tags.getItems().size() - 1, tags.getSelectionModel().getSelectedIndex());
                assertFalse(keyReachesTarget(input, keyEvent(KeyCode.K)));
                assertEquals(tags.getItems().size() - 2, tags.getSelectionModel().getSelectedIndex());
                tags.getSelectionModel().clearSelection();
                assertFalse(keyReachesTarget(input, keyEvent(KeyCode.J)));
                assertEquals(0, tags.getSelectionModel().getSelectedIndex());

                contactsView.fire();
                assertEquals(firstContact, contacts.getSelectionModel().getSelectedItem());
                assertFalse(keyReachesTarget(contactsView, keyEvent(KeyCode.J)));
                assertEquals(secondContact, contacts.getSelectionModel().getSelectedItem());
                assertFalse(keyReachesTarget(mode, keyEvent(KeyCode.S)));
                assertTrue(eventsView.isSelected());
                assertEquals(firstEvent, events.getSelectionModel().getSelectedItem());
                assertFalse(keyReachesTarget(mode, keyEvent(KeyCode.S)));
                assertTrue(tagsView.isSelected());
                assertEquals(0, tags.getSelectionModel().getSelectedIndex());
                assertFalse(keyReachesTarget(mode, keyEvent(KeyCode.S)));
                assertTrue(contactsView.isSelected());
                assertEquals(secondContact, contacts.getSelectionModel().getSelectedItem());

                input.setText("list");
                fireMousePress(input);
                assertEquals("COMMAND · Esc to navigate", mode.getText());
                assertEquals("list", input.getText());
                assertFalse(input.isDisabled());
                assertFalse(submit.isDisabled());
                assertFalse(keyReachesTarget(input, keyEvent(KeyCode.ESCAPE)));
                assertFalse(keyReachesTarget(contactsView, keyEvent(KeyCode.I)));
                assertEquals("NAVIGATION · j/k move · s switch view · i to type", mode.getText());
                assertTrue(input.isDisabled());
                assertTrue(submit.isDisabled());
                assertTrue(keyReachesTarget(mode, keyReleasedEvent(KeyCode.A)));
                assertEquals("NAVIGATION · j/k move · s switch view · i to type", mode.getText());
                assertFalse(keyReachesTarget(mode, keyReleasedEvent(KeyCode.I)));
                assertEquals("COMMAND · Esc to navigate", mode.getText());
                assertFalse(input.isDisabled());
                assertFalse(submit.isDisabled());
                assertTrue(keyReachesTarget(input, keyReleasedEvent(KeyCode.I)));
                fireMousePress(input);
                assertEquals("COMMAND · Esc to navigate", mode.getText());
                input.clear();
                assertFalse(keyReachesTarget(input, keyEvent(KeyCode.ESCAPE)));
                assertTrue(keyReachesTarget(mode, keyReleasedEvent(KeyCode.I, true, false, false, false)));
                assertEquals("NAVIGATION · j/k move · s switch view · i to type", mode.getText());
                assertFalse(keyReachesTarget(mode, keyEvent(KeyCode.I)));
                fireMousePress(input);
                assertEquals("COMMAND · Esc to navigate", mode.getText());
                assertTrue(keyReachesTarget(input, typedKeyEvent("i")));
                fireMousePress(contacts);
                assertEquals("NAVIGATION · j/k move · s switch view · i to type", mode.getText());
                fireMousePress(input);
                assertEquals("COMMAND · Esc to navigate", mode.getText());
                fireMousePress(events);
                assertEquals("NAVIGATION · j/k move · s switch view · i to type", mode.getText());
                fireMousePress(input);
                assertEquals("COMMAND · Esc to navigate", mode.getText());
                fireMousePress(tags);
                assertEquals("NAVIGATION · j/k move · s switch view · i to type", mode.getText());
            } finally {
                if (window != null) {
                    window.stop();
                }
                stage.close();
            }
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void cardNavigation_consumesUnsupportedKeysAndLeavesModifiedShortcutsAlone() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            Stage stage = new Stage();
            MainWindow window = null;
            try {
                ModelManager model = new ModelManager();
                Person contact = new PersonBuilder(TypicalPersons.ALICE).build();
                model.addPerson(contact);
                window = new MainWindow(stage, new LogicManager(model, null), Path.of("data", "coordimate.json"));
                window.fillInnerParts();
                window.show();

                TextField input = (TextField) stage.getScene().getRoot().lookup("#commandTextField");
                @SuppressWarnings("unchecked")
                ListView<Person> contacts = (ListView<Person>) stage.getScene().getRoot().lookup("#personListView");
                Label mode = (Label) stage.getScene().getRoot().lookup("#interactionModeLabel");

                input.setText("list");
                assertTrue(keyReachesTarget(input, keyEvent(KeyCode.J)));
                assertEquals(-1, contacts.getSelectionModel().getSelectedIndex());
                input.setText("list");
                assertFalse(keyReachesTarget(input, keyEvent(KeyCode.ESCAPE)));
                assertEquals("NAVIGATION · j/k move · s switch view · i to type", mode.getText());
                assertFalse(keyReachesTarget(input, keyEvent(KeyCode.J)));
                assertEquals(contact, contacts.getSelectionModel().getSelectedItem());
                assertEquals("list", input.getText());
                assertFalse(keyReachesTarget(input, keyEvent(KeyCode.I)));
                assertEquals("NAVIGATION · j/k move · s switch view · i to type", mode.getText());
                assertFalse(keyReachesTarget(mode, keyReleasedEvent(KeyCode.I)));
                assertEquals("COMMAND · Esc to navigate", mode.getText());
                assertEquals("list", input.getText());
                input.clear();
                contacts.getSelectionModel().clearSelection();

                assertFalse(keyReachesTarget(input, keyEvent(KeyCode.ESCAPE)));

                assertFalse(keyReachesTarget(input, keyEvent(KeyCode.A)));
                assertTrue(input.getText().isEmpty());
                assertTrue(keyReachesTarget(mode, keyEvent(KeyCode.J, true, false, false, false)));
                assertTrue(keyReachesTarget(mode, keyEvent(KeyCode.J, false, true, false, false)));
                assertTrue(keyReachesTarget(mode, keyEvent(KeyCode.J, false, false, true, false)));
                assertTrue(keyReachesTarget(mode, keyEvent(KeyCode.J, false, false, false, true)));
                assertEquals(-1, contacts.getSelectionModel().getSelectedIndex());

                contacts.getSelectionModel().clearSelection();

                TextField otherInput = new TextField();
                StackPane commandPlaceholder =
                        (StackPane) stage.getScene().getRoot().lookup("#commandBoxPlaceholder");
                commandPlaceholder.getChildren().add(otherInput);
                otherInput.requestFocus();
                assertSame(otherInput, stage.getScene().getFocusOwner());
                assertFalse(keyReachesTarget(otherInput, keyEvent(KeyCode.J)));
                assertEquals(contact, contacts.getSelectionModel().getSelectedItem());

                contacts.requestFocus();
                assertFalse(keyReachesTarget(contacts, keyEvent(KeyCode.I)));
                assertEquals("NAVIGATION · j/k move · s switch view · i to type", mode.getText());
                assertFalse(keyReachesTarget(mode, keyReleasedEvent(KeyCode.I)));
                assertEquals("COMMAND · Esc to navigate", mode.getText());

                TabPane views = (TabPane) stage.getScene().getRoot().lookup("#views");
                contacts.getSelectionModel().clearSelection();
                views.getSelectionModel().clearSelection();
                assertFalse(keyReachesTarget(input, keyEvent(KeyCode.ESCAPE)));
                assertFalse(keyReachesTarget(input, keyEvent(KeyCode.J)));
                assertEquals(-1, contacts.getSelectionModel().getSelectedIndex());
                views.getTabs().clear();
                assertFalse(keyReachesTarget(mode, keyEvent(KeyCode.S)));
                assertTrue(views.getTabs().isEmpty());
            } finally {
                if (window != null) {
                    window.stop();
                }
                stage.close();
            }
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void cardNavigation_emptyListsConsumeNavigationKeysWithoutChangingSelection() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            Stage stage = new Stage();
            MainWindow window = null;
            try {
                ModelManager model = new ModelManager();
                List.copyOf(model.getTagList()).forEach(model::deleteTag);
                window = new MainWindow(stage, new LogicManager(model, null),
                        Path.of("data", "coordimate.json"));
                window.fillInnerParts();
                stage.getScene().getRoot().applyCss();

                TextField input = (TextField) stage.getScene().getRoot().lookup("#commandTextField");
                ToggleButton eventsView = (ToggleButton) stage.getScene().getRoot().lookup("#eventsView");
                ToggleButton tagsView = (ToggleButton) stage.getScene().getRoot().lookup("#tagsView");

                assertFalse(keyReachesTarget(input, keyEvent(KeyCode.ESCAPE)));
                assertFalse(keyReachesTarget(input, keyEvent(KeyCode.J)));
                eventsView.fire();
                assertFalse(keyReachesTarget(input, keyEvent(KeyCode.K)));
                tagsView.fire();
                assertFalse(keyReachesTarget(input, keyEvent(KeyCode.J)));
            } finally {
                if (window != null) {
                    window.stop();
                }
                stage.close();
            }
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void cardNavigation_deleteConfirmationLeavesNavigationKeysForInput() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            Stage stage = new Stage();
            MainWindow window = null;
            try {
                ModelManager model = new ModelManager();
                model.addPerson(new PersonBuilder(TypicalPersons.ALICE).build());
                StorageManager storage = new StorageManager(
                        new JsonCoordiMateStorage(temporaryFolder.resolve("navigation-data.json")),
                        new JsonUserPrefsStorage(temporaryFolder.resolve("navigation-prefs.json")));
                LogicManager logic = new LogicManager(model, storage);
                window = new MainWindow(stage, logic, temporaryFolder.resolve("navigation-data.json"));
                window.fillInnerParts();
                stage.getScene().getRoot().applyCss();

                TextField input = (TextField) stage.getScene().getRoot().lookup("#commandTextField");
                Button submit = (Button) stage.getScene().getRoot().lookup("#submitButton");
                input.setText("delete 1");
                submit.fire();

                assertTrue(logic.isAwaitingDeleteConfirmation());
                assertTrue(input.getText().isEmpty());
                assertTrue(keyReachesTarget(input, keyEvent(KeyCode.ESCAPE)));
                assertTrue(keyReachesTarget(input, keyEvent(KeyCode.A)));
                assertTrue(keyReachesTarget(input, keyEvent(KeyCode.J)));
                @SuppressWarnings("unchecked")
                ListView<Person> contacts = (ListView<Person>) stage.getScene().getRoot().lookup("#personListView");
                Label mode = (Label) stage.getScene().getRoot().lookup("#interactionModeLabel");
                fireMousePress(contacts);
                assertEquals("COMMAND · Esc to navigate", mode.getText());
                assertEquals(-1, contacts.getSelectionModel().getSelectedIndex());
            } finally {
                if (window != null) {
                    window.stop();
                }
                stage.close();
            }
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void listCommand_opensContactsView() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            Stage stage = new Stage();
            try {
                StorageManager storage = new StorageManager(
                        new JsonCoordiMateStorage(temporaryFolder.resolve("data.json")),
                        new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json")));
                MainWindow window = new MainWindow(stage, new LogicManager(new ModelManager(), storage),
                        temporaryFolder.resolve("data.json"));
                try {
                    assertListCommandOpensContactsView(window, stage);
                } finally {
                    window.stop();
                }
            } finally {
                stage.close();
            }
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    private static void assertListCommandOpensContactsView(MainWindow window, Stage stage) {
        window.fillInnerParts();
        stage.getScene().getRoot().applyCss();

        ToggleButton events = (ToggleButton) stage.getScene().getRoot().lookup("#eventsView");
        ToggleButton contacts = (ToggleButton) stage.getScene().getRoot().lookup("#contactsView");
        TabPane views = (TabPane) stage.getScene().getRoot().lookup("#views");
        TextField input = (TextField) stage.getScene().getRoot().lookup("#commandTextField");
        Button submit = (Button) stage.getScene().getRoot().lookup("#submitButton");
        Label feedback = (Label) stage.getScene().getRoot().lookup("#resultDisplay");

        events.fire();
        input.setText("list");
        submit.fire();

        assertTrue(contacts.isSelected());
        assertFalse(events.isSelected());
        assertTrue(views.getSelectionModel().getSelectedItem().getText().startsWith("Contacts"));
        assertEquals("Listed all contacts.", feedback.getText());

        input.setText("list archived");
        submit.fire();
        assertTrue(contacts.isSelected());
        assertEquals("Archived events (0)", events.getText());
        assertEquals("Archived contacts (0)", contacts.getText());

        input.setText("list");
        submit.fire();
        assertEquals("Events (0)", events.getText());
        assertEquals("Contacts (0)", contacts.getText());

        events.fire();
        input.setText("search");
        submit.fire();
        assertTrue(events.isSelected());
        assertTrue(feedback.getText().startsWith("Invalid command format."));
    }

    @Test
    public void longFeedback_atMinimumWindowSize_keepsWorkspaceAndCommandInputVisible() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            Stage stage = new Stage();
            ModelManager model = new ModelManager();
            model.setGuiSettings(new GuiSettings(WindowBounds.PREFERRED_MIN_WIDTH,
                    WindowBounds.PREFERRED_MIN_HEIGHT, 0, 0, false));
            for (int index = 1; index <= 86; index++) {
                model.addTag(new Tag("Custom" + index));
            }
            Path dataPath = temporaryFolder.resolve("long-feedback-data.json");
            StorageManager storage = new StorageManager(new JsonCoordiMateStorage(dataPath),
                    new JsonUserPrefsStorage(temporaryFolder.resolve("long-feedback-prefs.json")));
            storage.saveCoordiMate(model.getCoordiMate());
            MainWindow window = new MainWindow(stage, new LogicManager(model, storage), dataPath);
            try {
                window.fillInnerParts();
                window.show();

                TextField input = (TextField) stage.getScene().lookup("#commandTextField");
                Button submit = (Button) stage.getScene().lookup("#submitButton");
                Label feedback = (Label) stage.getScene().lookup("#resultDisplay");
                ScrollPane feedbackScroll = (ScrollPane) stage.getScene().lookup("#feedbackMessageScroll");
                TabPane views = (TabPane) stage.getScene().lookup("#views");

                String keyword = "a".repeat(3000);
                input.setText("search " + keyword);
                submit.fire();
                stage.getScene().getRoot().applyCss();
                stage.getScene().getRoot().layout();
                assertEquals("No contacts found matching \"" + keyword + "\".", feedback.getText());
                assertLongFeedbackFitsWindow(stage, input, feedbackScroll, views);

                input.setText("listtags");
                submit.fire();
                stage.getScene().getRoot().applyCss();
                stage.getScene().getRoot().layout();
                assertTrue(feedback.getText().startsWith("Here is the list of all existing tags."));
                assertTrue(feedback.getText().contains("Custom86"));
                assertEquals(90, ((ListView<?>) stage.getScene().lookup("#tagListView")).getItems().size());
                assertLongFeedbackFitsWindow(stage, input, feedbackScroll, views);
                assertTrue(stage.getScene().lookup("#tagListView").getBoundsInLocal().getHeight() > 30);

                input.setText("list");
                submit.fire();
                stage.getScene().getRoot().applyCss();
                stage.getScene().getRoot().layout();
                assertEquals("Listed all contacts.", feedback.getText());
                assertTrue(feedbackScroll.getHeight() < 84);
                assertFalse(stage.getScene().lookup("#feedbackScrollHint").isVisible());
            } finally {
                window.stop();
                stage.close();
            }
            return null;
        });
        Platform.runLater(task);
        task.get(20, TimeUnit.SECONDS);
    }

    private static void assertLongFeedbackFitsWindow(Stage stage, TextField input, ScrollPane feedbackScroll,
                                                     TabPane views) {
        assertTrue(feedbackScroll.getHeight() <= 84);
        assertTrue(feedbackScroll.getContent().getLayoutBounds().getHeight()
                > feedbackScroll.getViewportBounds().getHeight());
        assertTrue(stage.getScene().lookup("#feedbackScrollHint").isVisible());
        assertTrue(feedbackScroll.lookup(".scroll-bar:vertical").isVisible());
        assertTrue(views.getHeight() > 100);
        Bounds inputBounds = input.localToScene(input.getBoundsInLocal());
        assertTrue(inputBounds.getMaxY() <= stage.getScene().getHeight());
    }

    @Test
    public void minimumWindowSize_keepsDetailsBesideListsAndEventTimingVisible() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            if (Screen.getPrimary().getVisualBounds().getWidth() < WindowBounds.PREFERRED_MIN_WIDTH
                    || Screen.getPrimary().getVisualBounds().getHeight() < WindowBounds.PREFERRED_MIN_HEIGHT) {
                return null;
            }
            Stage stage = new Stage();
            ModelManager model = new ModelManager();
            model.setGuiSettings(new GuiSettings(WindowBounds.PREFERRED_MIN_WIDTH,
                    WindowBounds.PREFERRED_MIN_HEIGHT, 0, 0, false));
            model.addPerson(TypicalPersons.ALICE);
            EventTime start = new EventTime("01-10-2026 18:00");
            EventTime end = new EventTime("01-10-2026 21:00");
            model.addEvent(new Event("Final Concert", start, end));
            MainWindow window = new MainWindow(stage, new LogicManager(model, null),
                    Path.of("data", "coordimate.json"));
            try {
                window.fillInnerParts();
                window.show();
                stage.getScene().getRoot().applyCss();
                stage.getScene().getRoot().layout();

                SplitPane contacts = (SplitPane) stage.getScene().lookup("#contactSplitPane");
                assertEquals(Orientation.HORIZONTAL, contacts.getOrientation());
                assertTrue(contacts.getItems().get(1).getBoundsInParent().getWidth() >= 460);

                ToggleButton eventsView = (ToggleButton) stage.getScene().lookup("#eventsView");
                eventsView.fire();
                ListView<?> eventList = (ListView<?>) stage.getScene().lookup("#eventListView");
                eventList.getSelectionModel().select(0);
                stage.getScene().getRoot().applyCss();
                stage.getScene().getRoot().layout();
                SplitPane events = (SplitPane) stage.getScene().lookup("#eventSplitPane");
                assertEquals(Orientation.HORIZONTAL, events.getOrientation());
                ScrollPane eventDetails = (ScrollPane) events.getItems().get(1);
                Node viewport = eventDetails.lookup(".viewport");
                Bounds visibleBounds = viewport.localToScene(viewport.getBoundsInLocal());
                var timeTiles = eventDetails.lookupAll(".time-tile");
                assertEquals(2, timeTiles.size());
                for (Node tile : timeTiles) {
                    Bounds tileBounds = tile.localToScene(tile.getBoundsInLocal());
                    assertTrue(tileBounds.getMaxX() <= visibleBounds.getMaxX() + 1);
                }

                ToggleButton tagsView = (ToggleButton) stage.getScene().lookup("#tagsView");
                tagsView.fire();
                stage.getScene().getRoot().applyCss();
                stage.getScene().getRoot().layout();
                SplitPane tags = (SplitPane) stage.getScene().lookup("#tagSplitPane");
                assertEquals(Orientation.HORIZONTAL, tags.getOrientation());
                assertTrue(stage.getScene().lookup("#tagMemberListView").getBoundsInLocal().getHeight() > 100);
            } finally {
                window.stop();
                stage.close();
            }
            return null;
        });
        Platform.runLater(task);
        task.get(20, TimeUnit.SECONDS);
    }

    @Test
    public void assignmentCommands_keepCurrentView() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            Stage stage = new Stage();
            ModelManager model = new ModelManager();
            model.addPerson(TypicalPersons.ALICE);
            EventTime date = new EventTime("08-08-2026");
            model.addEvent(new Event("Orientation", date, date));

            Path dataPath = temporaryFolder.resolve("assignment-data.json");
            StorageManager storage = new StorageManager(new JsonCoordiMateStorage(dataPath),
                    new JsonUserPrefsStorage(temporaryFolder.resolve("assignment-prefs.json")));
            storage.saveCoordiMate(model.getCoordiMate());

            MainWindow window = new MainWindow(stage, new LogicManager(model, storage), dataPath);
            try {
                window.fillInnerParts();
                stage.getScene().getRoot().applyCss();

                ToggleButton contacts = (ToggleButton) stage.getScene().getRoot().lookup("#contactsView");
                ToggleButton events = (ToggleButton) stage.getScene().getRoot().lookup("#eventsView");
                TextField input = (TextField) stage.getScene().getRoot().lookup("#commandTextField");
                Button submit = (Button) stage.getScene().getRoot().lookup("#submitButton");

                if (!contacts.isSelected()) {
                    contacts.fire();
                }
                input.setText("assign evn/Orientation c/1");
                submit.fire();
                assertTrue(contacts.isSelected());

                events.fire();
                input.setText("unassign evn/Orientation c/1");
                submit.fire();
                assertTrue(events.isSelected());
            } finally {
                window.stop();
                stage.close();
            }
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void deleteCommand_opensContactsForConfirmation() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            Stage stage = new Stage();
            ModelManager model = new ModelManager();
            model.addPerson(TypicalPersons.ALICE);

            Path dataPath = temporaryFolder.resolve("delete-routing-data.json");
            StorageManager storage = new StorageManager(new JsonCoordiMateStorage(dataPath),
                    new JsonUserPrefsStorage(temporaryFolder.resolve("delete-routing-prefs.json")));
            storage.saveCoordiMate(model.getCoordiMate());

            MainWindow window = new MainWindow(stage, new LogicManager(model, storage), dataPath);
            try {
                window.fillInnerParts();
                stage.getScene().getRoot().applyCss();

                ToggleButton contacts = (ToggleButton) stage.getScene().getRoot().lookup("#contactsView");
                ToggleButton events = (ToggleButton) stage.getScene().getRoot().lookup("#eventsView");
                TextField input = (TextField) stage.getScene().getRoot().lookup("#commandTextField");
                Button submit = (Button) stage.getScene().getRoot().lookup("#submitButton");
                Label feedback = (Label) stage.getScene().getRoot().lookup("#resultDisplay");

                events.fire();
                input.setText("delete 1");
                submit.fire();
                assertTrue(contacts.isSelected());
                assertTrue(feedback.getText().contains("Confirm deletion? [y/N]"));

                input.setText("n");
                submit.fire();
                assertTrue(contacts.isSelected());
                assertEquals("Deletion cancelled. No changes were made.", feedback.getText());
            } finally {
                window.stop();
                stage.close();
            }
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void contactCommands_switchToContactsAndFocusAffectedContact() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            Stage stage = new Stage();
            MainWindow window = null;
            try {
                ModelManager model = new ModelManager();
                model.addPerson(TypicalPersons.ALICE);
                model.addPerson(TypicalPersons.BENSON);
                Path dataPath = temporaryFolder.resolve("contact-focus-data.json");
                StorageManager storage = new StorageManager(new JsonCoordiMateStorage(dataPath),
                        new JsonUserPrefsStorage(temporaryFolder.resolve("contact-focus-prefs.json")));
                storage.saveCoordiMate(model.getCoordiMate());
                LogicManager logic = new LogicManager(model, storage);
                window = new MainWindow(stage, logic, dataPath);
                window.fillInnerParts();
                stage.getScene().getRoot().applyCss();

                ToggleButton contactsView = (ToggleButton) stage.getScene().getRoot().lookup("#contactsView");
                ToggleButton eventsView = (ToggleButton) stage.getScene().getRoot().lookup("#eventsView");
                TextField input = (TextField) stage.getScene().getRoot().lookup("#commandTextField");
                Button submit = (Button) stage.getScene().getRoot().lookup("#submitButton");
                @SuppressWarnings("unchecked")
                ListView<Person> contacts = (ListView<Person>) stage.getScene().getRoot().lookup("#personListView");
                Label selectedName = (Label) window.getPersonListPanel().getRoot().lookup("#selectedName");

                eventsView.fire();
                input.setText("add n/Charlie Clark p/98765430 e/charlie@example.com r/Member");
                submit.fire();
                assertTrue(contactsView.isSelected());
                assertEquals("Charlie Clark", selectedName.getText());
                Label selectedEmail = (Label) window.getPersonListPanel().getRoot().lookup("#email");
                assertEquals("charlie@example.com", selectedEmail.getText());

                eventsView.fire();
                input.setText("edit target/charlie@example.com n/Charles Clark");
                submit.fire();
                assertTrue(contactsView.isSelected());
                assertEquals("Charles Clark", selectedName.getText());
                assertEquals("Charles Clark", contacts.getSelectionModel().getSelectedItem().getName().getFullName());

                eventsView.fire();
                input.setText("delete e/charlie@example.com");
                submit.fire();
                assertTrue(logic.isAwaitingDeleteConfirmation());
                assertEquals("Charles Clark", selectedName.getText());

                input.setText("y");
                submit.fire();
                assertFalse(logic.isAwaitingDeleteConfirmation());
                assertEquals("Benson Meier", selectedName.getText());
                assertEquals(TypicalPersons.BENSON, contacts.getSelectionModel().getSelectedItem());
            } finally {
                if (window != null) {
                    window.stop();
                }
                stage.close();
            }
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void showWindow_startsMaximized() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            Stage stage = new Stage();
            MainWindow window = new MainWindow(stage, new LogicManager(new ModelManager(), null),
                    Path.of("data", "coordimate.json"));
            window.fillInnerParts();

            window.show();
            assertTrue(stage.isMaximized());
            window.scheduleNormalWindowBoundsUpdate();
            window.scheduleNormalWindowBoundsUpdate();

            Platform.runLater(stage::close);
            return null;
        });
        Platform.runLater(task);
        task.get(20, TimeUnit.SECONDS);
    }

    @Test
    public void showWindow_savedAsNotMaximized_startsNotMaximized() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            Stage stage = new Stage();
            ModelManager model = new ModelManager();
            model.setGuiSettings(new GuiSettings(900, 700, 0, 0, false));
            MainWindow window = new MainWindow(stage, new LogicManager(model, null),
                    Path.of("data", "coordimate.json"));
            window.fillInnerParts();
            window.scheduleNormalWindowBoundsUpdate();

            window.show();
            assertFalse(stage.isMaximized());
            window.scheduleNormalWindowBoundsUpdate();
            window.scheduleNormalWindowBoundsUpdate();

            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            stage.setWidth(Math.min(950, screenBounds.getWidth()));
            stage.setHeight(Math.min(700, screenBounds.getHeight()));
            stage.setX(screenBounds.getMinX());
            stage.setY(screenBounds.getMinY());
            double expectedWidth = stage.getWidth();
            double expectedHeight = stage.getHeight();
            int expectedX = (int) stage.getX();
            int expectedY = (int) stage.getY();
            stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));

            assertEquals(new GuiSettings(expectedWidth, expectedHeight, expectedX, expectedY, false),
                    model.getGuiSettings());
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void createWindow_savedPositionOutsideAllScreens_usesPrimaryScreen() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            Stage stage = new Stage();
            ModelManager model = new ModelManager();
            model.setGuiSettings(new GuiSettings(900, 700, Integer.MAX_VALUE, Integer.MAX_VALUE, false));

            new MainWindow(stage, new LogicManager(model, null), Path.of("data", "coordimate.json"));

            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            assertTrue(screenBounds.contains(stage.getX(), stage.getY()));
            stage.close();
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void closeWindow_whileMaximized_savesNormalBoundsAndMaximizedState() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            Stage stage = new Stage();
            ModelManager model = new ModelManager();
            MainWindow window = new MainWindow(stage, new LogicManager(model, null),
                    Path.of("data", "coordimate.json"));
            window.fillInnerParts();
            double normalWidth = stage.getWidth();
            double normalHeight = stage.getHeight();
            int normalX = (int) stage.getX();
            int normalY = (int) stage.getY();

            window.show();
            stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));

            assertEquals(new GuiSettings(normalWidth, normalHeight, normalX, normalY, true), model.getGuiSettings());
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void clickCommandRow_insertsCommandAndShowsFormatAndExample() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            Stage stage = new Stage();
            MainWindow window = new MainWindow(stage, new LogicManager(new ModelManager(), null),
                    Path.of("data", "coordimate.json"));
            window.fillInnerParts();
            stage.getScene().getRoot().applyCss();

            HBox addRow = (HBox) stage.getScene().getRoot().lookup(".cheat-row");
            addRow.getOnMouseClicked().handle(null);
            TextField commandInput = (TextField) stage.getScene().getRoot().lookup(".text-field");
            TextFlow prefixHelp = (TextFlow) stage.getScene().getRoot().lookup("#prefixHelp");
            assertEquals("add", commandInput.getText());
            String hintText = getPrefixHelpText(prefixHelp);
            assertTrue(hintText.startsWith("Format: add n/NAME p/PHONE e/EMAIL r/ROLE"));
            assertTrue(hintText.contains("[t/TAG]... [m/NOTE]"));
            assertTrue(hintText.contains("\nExample: add n/Aisha Tan"));

            stage.close();
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void tagMembers_showRolesOrganisationsAndTagColors() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            Stage stage = new Stage();
            ModelManager model = new ModelManager();
            model.addPerson(new PersonBuilder().withName("Aisha Tan").withPhone("91234567")
                    .withEmail("aisha@example.com").withRole("Logistics Lead")
                    .withOrganisation("NUS").withTags("EXCO", "Logistics", "Publicity").build());
            model.addPerson(new PersonBuilder().withName("Ben Lim").withPhone("91234568")
                    .withEmail("ben@example.com").withRole("Sponsor Liaison")
                    .withOrganisation("Student Union").withTags("EXCO", "Sponsor", "UniversityStaff").build());
            model.addPerson(new PersonBuilder().withName("Cindy Ng").withPhone("91234569")
                    .withEmail("cindy@example.com").withRole("Member")
                    .withOrganisation("NUS").withTags("EXCO").build());
            model.updateFilteredPersonList(person -> false);
            MainWindow window = new MainWindow(stage, new LogicManager(model, null),
                    Path.of("data", "coordimate.json"));
            window.fillInnerParts();
            window.show();

            stage.getScene().getRoot().applyCss();
            stage.getScene().getRoot().layout();
            ToggleButton tags = (ToggleButton) stage.getScene().getRoot().lookup("#tagsView");
            tags.fire();
            @SuppressWarnings("unchecked")
            ListView<String> tagList = (ListView<String>) stage.getScene().getRoot().lookup("#tagListView");
            tagList.getSelectionModel().select("EXCO");
            stage.getScene().getRoot().applyCss();
            stage.getScene().getRoot().layout();

            assertEquals(3, stage.getScene().getRoot().lookupAll(".event-member-name").size());
            assertEquals(List.of("ID: 1", "ID: 2", "ID: 3"),
                    stage.getScene().getRoot().lookupAll(".tag-member-row .event-member-id").stream()
                            .map(node -> ((Label) node).getText()).sorted().toList());
            assertEquals(1, stage.getScene().getRoot().lookupAll(".tag-chip-teal").size());
            assertEquals(1, stage.getScene().getRoot().lookupAll(".tag-chip-amber").size());
            assertEquals(1, stage.getScene().getRoot().lookupAll(".tag-chip-purple").size());
            assertEquals(3, stage.getScene().getRoot().lookupAll(".tag-chip-red").size());
            assertEquals(1, stage.getScene().getRoot().lookupAll(".tag-chip-blue").size());
            assertEquals(1, stage.getScene().getRoot().lookupAll(".event-member-avatar-teal").size());
            assertEquals(1, stage.getScene().getRoot().lookupAll(".event-member-avatar-amber").size());
            assertEquals(1, stage.getScene().getRoot().lookupAll(".event-member-avatar-purple").size());
            assertTrue(stage.getScene().getRoot().lookupAll(".event-member-metadata").stream()
                    .anyMatch(node -> ((Label) node).getText().equals("Logistics Lead · NUS")));

            tagList.getSelectionModel().clearSelection();
            assertEquals("Select a tag", ((Label) stage.getScene().getRoot().lookup("#selectedTagName")).getText());
            @SuppressWarnings("unchecked")
            ListView<Person> tagMembers = (ListView<Person>) stage.getScene().getRoot().lookup("#tagMemberListView");
            assertEquals("Select a tag to view its members.", ((Label) tagMembers.getPlaceholder()).getText());

            SplitPane tagSplitPane = (SplitPane) stage.getScene().getRoot().lookup("#tagSplitPane");
            tagSplitPane.resize(900, 500);
            assertEquals(Orientation.HORIZONTAL, tagSplitPane.getOrientation());
            tagSplitPane.resize(600, 500);
            assertEquals(Orientation.VERTICAL, tagSplitPane.getOrientation());

            stage.close();
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void commandRow_supportsKeyboardActivation() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            Stage stage = new Stage();
            MainWindow window = new MainWindow(stage, new LogicManager(new ModelManager(), null),
                    Path.of("data", "coordimate.json"));
            window.fillInnerParts();
            stage.getScene().getRoot().applyCss();

            HBox addRow = (HBox) stage.getScene().getRoot().lookup(".cheat-row");
            TextField commandInput = (TextField) stage.getScene().getRoot().lookup(".text-field");
            Label mode = (Label) stage.getScene().getRoot().lookup("#interactionModeLabel");
            assertFalse(keyReachesTarget(commandInput, keyEvent(KeyCode.ESCAPE)));
            assertEquals("NAVIGATION · j/k move · s switch view · i to type", mode.getText());

            KeyEvent enter = keyEvent(KeyCode.ENTER);
            addRow.getOnKeyPressed().handle(enter);
            assertTrue(enter.isConsumed());
            assertEquals("add", commandInput.getText());
            assertEquals("COMMAND · Esc to navigate", mode.getText());

            KeyEvent space = keyEvent(KeyCode.SPACE);
            addRow.getOnKeyPressed().handle(space);
            assertTrue(space.isConsumed());

            KeyEvent otherKey = keyEvent(KeyCode.A);
            addRow.getOnKeyPressed().handle(otherKey);
            assertFalse(otherKey.isConsumed());
            assertEquals("add", commandInput.getText());
            stage.close();
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    private static KeyEvent keyEvent(KeyCode keyCode) {
        return keyEvent(keyCode, false, false, false, false);
    }

    private static KeyEvent keyEvent(KeyCode keyCode, boolean shiftDown, boolean controlDown,
                                     boolean altDown, boolean metaDown) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", keyCode,
                shiftDown, controlDown, altDown, metaDown);
    }

    private static KeyEvent keyReleasedEvent(KeyCode keyCode) {
        return keyReleasedEvent(keyCode, false, false, false, false);
    }

    private static KeyEvent keyReleasedEvent(KeyCode keyCode, boolean shiftDown, boolean controlDown,
                                             boolean altDown, boolean metaDown) {
        return new KeyEvent(KeyEvent.KEY_RELEASED, "", "", keyCode,
                shiftDown, controlDown, altDown, metaDown);
    }

    private static KeyEvent typedKeyEvent(String character) {
        return new KeyEvent(KeyEvent.KEY_TYPED, character, character, KeyCode.UNDEFINED,
                false, false, false, false);
    }

    private static boolean keyReachesTarget(Node target, KeyEvent event) {
        AtomicBoolean reachedTarget = new AtomicBoolean();
        EventHandler<KeyEvent> observer = ignored -> reachedTarget.set(true);
        target.addEventHandler(event.getEventType(), observer);
        target.fireEvent(event.copyFor(target, target));
        target.removeEventHandler(event.getEventType(), observer);
        return reachedTarget.get();
    }

    private static void fireMousePress(Node target) {
        MouseEvent event = new MouseEvent(MouseEvent.MOUSE_PRESSED, 0, 0, 0, 0, MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, true, false, false,
                new PickResult(target, 0, 0));
        target.fireEvent(event.copyFor(target, target));
    }

    private static MouseEvent mouseClickEvent() {
        return new MouseEvent(MouseEvent.MOUSE_CLICKED,
                0, 0, 0, 0, MouseButton.PRIMARY, 1,
                false, false, false, false, false, false, false, false, false, false, null);
    }

    private static String getPrefixHelpText(TextFlow prefixHelp) {
        return ((Text) prefixHelp.getChildren().getFirst()).getText();
    }

    @Test
    public void archivedContacts_markedInTagsViewAndCommandPanelListsArchiveCommands() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            Stage stage = new Stage();
            ModelManager model = new ModelManager();
            model.addPerson(TypicalPersons.ALICE);
            model.addPerson(TypicalPersons.BENSON);
            model.setPerson(TypicalPersons.ALICE, TypicalPersons.ALICE.withArchived(true));
            MainWindow window = new MainWindow(stage, new LogicManager(model, null),
                    Path.of("data", "coordimate.json"));
            try {
                window.fillInnerParts();
                window.show();
                stage.getScene().getRoot().applyCss();

                assertEquals(List.of("add", "edit", "delete", "archive", "unarchive", "search", "filter", "list"),
                        commandNames(stage, "#contactsCommands"));
                assertTrue(commandNames(stage, "#eventsCommands").containsAll(
                        List.of("deleteevent", "archiveevent", "unarchiveevent")));

                ToggleButton tagsView = (ToggleButton) stage.getScene().getRoot().lookup("#tagsView");
                tagsView.fire();
                @SuppressWarnings("unchecked")
                ListView<String> tagList = (ListView<String>) stage.getScene().getRoot().lookup("#tagListView");
                tagList.getSelectionModel().select("friends");
                stage.getScene().getRoot().applyCss();
                stage.getScene().getRoot().layout();

                assertEquals(List.of("Alice Pauline (archived)", "Benson Meier"),
                        stage.getScene().getRoot().lookupAll(".tag-member-row .event-member-name").stream()
                                .map(node -> ((Label) node).getText()).sorted().toList());
            } finally {
                window.stop();
                stage.close();
            }
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void listArchived_showsArchivedMessagesHeaderAndCommands() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            Stage stage = new Stage();
            Path dataPath = temporaryFolder.resolve("archived-empty-data.json");
            StorageManager storage = new StorageManager(new JsonCoordiMateStorage(dataPath),
                    new JsonUserPrefsStorage(temporaryFolder.resolve("archived-empty-prefs.json")));
            MainWindow window = new MainWindow(stage, new LogicManager(new ModelManager(), storage), dataPath);
            try {
                window.fillInnerParts();
                stage.getScene().getRoot().applyCss();
                @SuppressWarnings("unchecked")
                ListView<Person> contacts = (ListView<Person>) stage.getScene().getRoot().lookup("#personListView");
                @SuppressWarnings("unchecked")
                ListView<Event> events = (ListView<Event>) stage.getScene().getRoot().lookup("#eventListView");
                TextField input = (TextField) stage.getScene().getRoot().lookup("#commandTextField");
                Button submit = (Button) stage.getScene().getRoot().lookup("#submitButton");

                assertEquals(PersonListPanel.MESSAGE_NO_CONTACTS, placeholderText(contacts));
                assertEquals(EventListPanel.MESSAGE_NO_EVENTS, placeholderText(events));

                ToggleButton eventsView = (ToggleButton) stage.getScene().getRoot().lookup("#eventsView");
                Label eventCount = (Label) stage.getScene().getRoot().lookup("#eventCount");
                List<String> contactCommands = visibleCommandNames(stage);
                assertTrue(contactCommands.containsAll(List.of("add", "archive", "unarchive", "list")));
                assertEquals("Events (0)", eventCount.getText());

                input.setText("list archived");
                submit.fire();
                assertEquals(PersonListPanel.MESSAGE_NO_ARCHIVED_CONTACTS, placeholderText(contacts));
                assertEquals(EventListPanel.MESSAGE_NO_ARCHIVED_EVENTS, placeholderText(events));
                assertEquals("Archived events (0)", eventCount.getText());
                assertEquals(List.of("unarchive", "list"), visibleCommandNames(stage));
                eventsView.fire();
                assertEquals(List.of("unarchiveevent", "list"), visibleCommandNames(stage));

                input.setText("list");
                submit.fire();
                assertEquals(PersonListPanel.MESSAGE_NO_CONTACTS, placeholderText(contacts));
                assertEquals(EventListPanel.MESSAGE_NO_EVENTS, placeholderText(events));
                assertEquals("Events (0)", eventCount.getText());
                assertEquals(contactCommands, visibleCommandNames(stage));
                eventsView.fire();
                assertTrue(visibleCommandNames(stage).containsAll(List.of("addevent", "archiveevent", "assign")));
            } finally {
                window.stop();
                stage.close();
            }
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void openingTutorialInArchivedView_returnsToActiveView() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            Stage stage = new Stage();
            ModelManager model = new ModelManager();
            model.addPerson(TypicalPersons.ALICE);
            model.addPerson(TypicalPersons.BENSON);
            model.setPerson(TypicalPersons.ALICE, TypicalPersons.ALICE.withArchived(true));
            model.setShowingArchived(true);
            LogicManager logic = new LogicManager(model, null);
            MainWindow window = new MainWindow(stage, logic, Path.of("data", "coordimate.json"));
            try {
                window.fillInnerParts();
                window.show();
                stage.getScene().getRoot().applyCss();
                assertEquals(List.of("unarchive", "list"), visibleCommandNames(stage));

                Label tutorialLabel = (Label) stage.getScene().lookup("#tutorialLabel");
                javafx.event.Event.fireEvent(tutorialLabel, mouseClickEvent());
                stage.getScene().getRoot().applyCss();

                assertTrue(stage.getScene().lookup("#tutorialOverlay").isVisible());
                assertFalse(logic.showingArchivedProperty().get());
                assertEquals(List.of(TypicalPersons.BENSON), logic.getFilteredPersonList());
                assertTrue(visibleCommandNames(stage).containsAll(List.of("add", "archive", "list")));
                assertTrue(stage.getScene().lookup("#contactsCommands").isVisible());
            } finally {
                window.stop();
                stage.close();
            }
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    private static List<String> visibleCommandNames(Stage stage) {
        return stage.getScene().getRoot().lookupAll(".command-cheatsheet").stream()
                .filter(Node::isVisible)
                .flatMap(list -> list.lookupAll(".cheat-name").stream())
                .map(node -> ((Label) node).getText()).toList();
    }

    private static String placeholderText(ListView<?> list) {
        return ((Label) list.getPlaceholder()).getText();
    }

    private static List<String> commandNames(Stage stage, String commandListId) {
        return stage.getScene().getRoot().lookup(commandListId).lookupAll(".cheat-name").stream()
                .map(node -> ((Label) node).getText()).toList();
    }
}
