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
            HBox topMenuRow = (HBox) stage.getScene().getRoot().lookup("#topMenuRow");
            MenuBar menuBar = (MenuBar) stage.getScene().getRoot().lookup("#menuBar");
            assertEquals("Help", helpLabel.getText());
            assertEquals("Open Help (Fn + F1)", helpLabel.getTooltip().getText());
            assertEquals(200, helpLabel.getTooltip().getShowDelay().toMillis());
            assertEquals(List.of("File"), menuBar.getMenus().stream().map(menu -> menu.getText()).toList());
            assertSame(topMenuRow, menuBar.getParent());
            assertSame(topMenuRow, helpLabel.getParent());
            assertEquals(List.of(menuBar, helpLabel), topMenuRow.getChildren());

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
            helpStage.hide();
            stage.close();
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void helpWindowTutorialButton_opensTutorialOverlay() throws Exception {
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
            Button tutorialButton = (Button) helpStage.getScene().lookup("#tutorialButton");
            tutorialButton.fire();
            stage.getScene().getRoot().applyCss();
            stage.getScene().getRoot().layout();

            StackPane tutorialOverlay = (StackPane) stage.getScene().lookup("#tutorialOverlay");
            assertTrue(tutorialOverlay.isVisible());
            assertFalse(helpStage.isShowing());
            assertEquals("Step 1 of 15", ((Label) stage.getScene().lookup("#stepCounter")).getText());
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
            assertEquals("Step 5 of 15", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            assertTrue(stage.getScene().lookup("#targetHighlight").isVisible());
            nextButton.fire();
            assertEquals("Step 6 of 15", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            ListView<?> contactList = (ListView<?>) stage.getScene().lookup("#personListView");
            assertEquals(0, contactList.getSelectionModel().getSelectedIndex());
            assertTrue(stage.getScene().lookup("#targetHighlight").isVisible());
            nextButton.fire();
            assertEquals("Step 7 of 15", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            assertEquals(1, views.getSelectionModel().getSelectedIndex());
            stage.getScene().getRoot().applyCss();
            stage.getScene().getRoot().layout();
            nextButton.fire();
            assertEquals("Step 8 of 15", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            ListView<?> eventList = (ListView<?>) stage.getScene().lookup("#eventListView");
            assertEquals(0, eventList.getSelectionModel().getSelectedIndex());
            assertTrue(stage.getScene().lookup("#targetHighlight").isVisible());
            Bounds eventCardHighlight = stage.getScene().lookup("#targetHighlight")
                    .localToScene(stage.getScene().lookup("#targetHighlight").getLayoutBounds());
            assertTrue(eventCardHighlight.getHeight() < eventList.getHeight());
            nextButton.fire();
            assertEquals("Step 9 of 15", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            assertTrue(stage.getScene().lookup("#targetHighlight").isVisible());
            nextButton.fire();
            assertEquals("Step 10 of 15", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            VBox members = (VBox) stage.getScene().lookup("#members");
            assertEquals(1, members.getChildren().size());
            Bounds memberBounds = members.localToScene(members.getLayoutBounds());
            Bounds highlightBounds = stage.getScene().lookup("#targetHighlight")
                    .localToScene(stage.getScene().lookup("#targetHighlight").getLayoutBounds());
            assertTrue(highlightBounds.intersects(memberBounds));
            Label memberCount = (Label) stage.getScene().lookup("#memberCount");
            assertFalse(highlightBounds.intersects(memberCount.localToScene(memberCount.getLayoutBounds())));
            nextButton.fire();
            assertEquals("Step 11 of 15", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            assertEquals(2, views.getSelectionModel().getSelectedIndex());
            assertTrue(((ToggleButton) stage.getScene().lookup("#tagsView")).isSelected());
            assertTutorialHighlights(stage, "#tagsView");
            nextButton.fire();
            assertEquals("Step 12 of 15", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            assertTutorialHighlights(stage, "#tagListView");
            nextButton.fire();
            assertEquals("Step 13 of 15", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            assertEquals(0, ((ListView<?>) stage.getScene().lookup("#tagListView"))
                    .getSelectionModel().getSelectedIndex());
            assertTutorialHighlights(stage, "#tagDetailsPane");
            nextButton.fire();
            assertEquals("Step 14 of 15", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            assertEquals(2, views.getSelectionModel().getSelectedIndex());
            assertFalse(stage.getScene().lookup("#targetHighlight").isVisible());
            nextButton.fire();
            assertEquals("Step 15 of 15", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            assertEquals(2, views.getSelectionModel().getSelectedIndex());
            assertTutorialHighlights(stage, "#helpLabel");
            assertEquals("Finish", nextButton.getText());
            assertEquals(1, logic.getEventList().size());
            Button closeButton = (Button) stage.getScene().lookup("#closeButton");
            closeButton.fire();
            assertTrue(logic.hasSeenTutorial());
            assertFalse(submitButton.isDisabled());
            assertEquals(2, views.getSelectionModel().getSelectedIndex());

            window.handleHelp();
            tutorialButton.fire();
            assertTrue(tutorialOverlay.isVisible());
            assertFalse(helpStage.isShowing());
            assertEquals("Step 1 of 15", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            window.stop();
            stage.close();
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void tutorialWithoutContacts_highlightsEmptyListAndKeepsDetailsHint() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            Stage stage = new Stage();
            MainWindow window = new MainWindow(stage, new LogicManager(new ModelManager(), null),
                    Path.of("data", "coordimate.json"));
            window.fillInnerParts();
            window.show();
            window.handleHelp();
            Button tutorialButton = (Button) findOwnedStage(stage, "Help").getScene().lookup("#tutorialButton");
            tutorialButton.fire();
            stage.getScene().getRoot().applyCss();
            stage.getScene().getRoot().layout();

            Button nextButton = (Button) stage.getScene().lookup("#nextButton");
            for (int step = 0; step < 4; step++) {
                nextButton.fire();
            }
            assertEquals("Step 5 of 15", ((Label) stage.getScene().lookup("#stepCounter")).getText());
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
            assertEquals("Step 8 of 15", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            assertEquals(0, ((ListView<?>) stage.getScene().lookup("#eventListView")).getItems().size());
            assertTrue(stage.getScene().lookup("#targetHighlight").isVisible());
            nextButton.fire();
            assertEquals("Step 9 of 15", ((Label) stage.getScene().lookup("#stepCounter")).getText());
            Label eventSelectionHint = (Label) stage.getScene().lookup("#eventListPanelPlaceholder")
                    .lookup("#selectionHint");
            assertTrue(eventSelectionHint.isVisible());
            nextButton.fire();
            assertEquals("Step 10 of 15", ((Label) stage.getScene().lookup("#stepCounter")).getText());
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
    public void cardNavigation_routesJAndKToActiveView() throws Exception {
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
                assertTrue(keyReachesTarget(input, typedKeyEvent("j")));
                input.clear();
                assertFalse(keyReachesTarget(input, keyEvent(KeyCode.ESCAPE)));
                assertEquals("NAVIGATION · j/k move · i to type", mode.getText());
                assertTrue(input.isDisabled());
                assertTrue(submit.isDisabled());
                assertFalse(keyReachesTarget(input, typedKeyEvent("j")));
                assertEquals(firstContact, contacts.getSelectionModel().getSelectedItem());
                assertTrue(input.getText().isEmpty());
                assertFalse(keyReachesTarget(input, typedKeyEvent("j")));
                assertEquals(secondContact, contacts.getSelectionModel().getSelectedItem());
                assertTrue(input.getText().isEmpty());
                assertFalse(keyReachesTarget(input, typedKeyEvent("k")));
                assertEquals(firstContact, contacts.getSelectionModel().getSelectedItem());
                assertFalse(keyReachesTarget(input, typedKeyEvent("k")));
                assertEquals(firstContact, contacts.getSelectionModel().getSelectedItem());

                eventsView.fire();
                assertFalse(keyReachesTarget(input, typedKeyEvent("j")));
                assertEquals(firstEvent, events.getSelectionModel().getSelectedItem());
                assertFalse(keyReachesTarget(input, typedKeyEvent("j")));
                assertEquals(secondEvent, events.getSelectionModel().getSelectedItem());
                assertFalse(keyReachesTarget(input, typedKeyEvent("k")));
                assertEquals(firstEvent, events.getSelectionModel().getSelectedItem());

                tagsView.fire();
                assertFalse(keyReachesTarget(input, typedKeyEvent("k")));
                assertEquals(tags.getItems().size() - 1, tags.getSelectionModel().getSelectedIndex());
                assertFalse(keyReachesTarget(input, typedKeyEvent("j")));
                assertEquals(tags.getItems().size() - 1, tags.getSelectionModel().getSelectedIndex());
                assertFalse(keyReachesTarget(input, typedKeyEvent("k")));
                assertEquals(tags.getItems().size() - 2, tags.getSelectionModel().getSelectedIndex());
                tags.getSelectionModel().clearSelection();
                assertFalse(keyReachesTarget(input, typedKeyEvent("j")));
                assertEquals(0, tags.getSelectionModel().getSelectedIndex());

                contactsView.fire();
                assertEquals(firstContact, contacts.getSelectionModel().getSelectedItem());
                assertFalse(keyReachesTarget(contactsView, typedKeyEvent("j")));
                assertEquals(secondContact, contacts.getSelectionModel().getSelectedItem());

                fireMousePress(input);
                assertEquals("NAVIGATION · j/k move · i to type", mode.getText());
                assertFalse(keyReachesTarget(contactsView, typedKeyEvent("i")));
                assertEquals("COMMAND · Esc to navigate", mode.getText());
                assertFalse(input.isDisabled());
                assertFalse(submit.isDisabled());
                fireMousePress(contacts);
                assertEquals("NAVIGATION · j/k move · i to type", mode.getText());
                fireMousePress(input);
                assertEquals("NAVIGATION · j/k move · i to type", mode.getText());
                fireMousePress(events);
                assertEquals("NAVIGATION · j/k move · i to type", mode.getText());
                fireMousePress(input);
                fireMousePress(tags);
                assertEquals("NAVIGATION · j/k move · i to type", mode.getText());
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
    public void cardNavigation_ignoresUnsafeInputContexts() throws Exception {
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
                assertTrue(keyReachesTarget(input, typedKeyEvent("j")));
                assertEquals(-1, contacts.getSelectionModel().getSelectedIndex());
                input.setText("list");
                assertFalse(keyReachesTarget(input, keyEvent(KeyCode.ESCAPE)));
                assertEquals("NAVIGATION · j/k move · i to type", mode.getText());
                assertFalse(keyReachesTarget(input, typedKeyEvent("j")));
                assertEquals(contact, contacts.getSelectionModel().getSelectedItem());
                assertEquals("list", input.getText());
                assertFalse(keyReachesTarget(input, typedKeyEvent("i")));
                assertEquals("COMMAND · Esc to navigate", mode.getText());
                assertEquals("list", input.getText());
                input.clear();
                contacts.getSelectionModel().clearSelection();

                assertFalse(keyReachesTarget(input, keyEvent(KeyCode.ESCAPE)));

                assertFalse(keyReachesTarget(input, typedKeyEvent("a")));
                assertTrue(input.getText().isEmpty());
                assertFalse(keyReachesTarget(input, typedKeyEvent("j", true, false, false, false)));
                input.clear();
                assertFalse(keyReachesTarget(input, typedKeyEvent("j", false, true, false, false)));
                input.clear();
                assertFalse(keyReachesTarget(input, typedKeyEvent("j", false, false, true, false)));
                input.clear();
                assertFalse(keyReachesTarget(input, typedKeyEvent("j", false, false, false, true)));
                input.clear();
                assertEquals(-1, contacts.getSelectionModel().getSelectedIndex());

                input.requestFocus();
                assertSame(input, stage.getScene().getFocusOwner());
                assertFalse(keyReachesTarget(input, typedKeyEvent("j")));
                assertEquals(contact, contacts.getSelectionModel().getSelectedItem());
                contacts.getSelectionModel().clearSelection();

                TextField otherInput = new TextField();
                StackPane commandPlaceholder =
                        (StackPane) stage.getScene().getRoot().lookup("#commandBoxPlaceholder");
                commandPlaceholder.getChildren().add(otherInput);
                otherInput.requestFocus();
                assertSame(otherInput, stage.getScene().getFocusOwner());
                assertTrue(keyReachesTarget(otherInput, typedKeyEvent("j")));
                assertEquals(-1, contacts.getSelectionModel().getSelectedIndex());

                contacts.requestFocus();
                assertFalse(keyReachesTarget(contacts, typedKeyEvent("i")));
                assertEquals("COMMAND · Esc to navigate", mode.getText());

                TabPane views = (TabPane) stage.getScene().getRoot().lookup("#views");
                views.getSelectionModel().clearSelection();
                assertFalse(keyReachesTarget(input, keyEvent(KeyCode.ESCAPE)));
                assertFalse(keyReachesTarget(input, typedKeyEvent("j")));
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
                assertFalse(keyReachesTarget(input, typedKeyEvent("j")));
                eventsView.fire();
                assertFalse(keyReachesTarget(input, typedKeyEvent("k")));
                tagsView.fire();
                assertFalse(keyReachesTarget(input, typedKeyEvent("j")));
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
                assertTrue(keyReachesTarget(input, typedKeyEvent("j")));
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
    public void switchCommand_selectsViewAndRejectsInvalidView() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            Stage stage = new Stage();
            MainWindow window = new MainWindow(stage, new LogicManager(new ModelManager(), null),
                    Path.of("data", "coordimate.json"));
            window.fillInnerParts();
            stage.getScene().getRoot().applyCss();

            TabPane views = (TabPane) stage.getScene().getRoot().lookup("#views");
            TextField input = (TextField) stage.getScene().getRoot().lookup("#commandTextField");
            Button submit = (Button) stage.getScene().getRoot().lookup("#submitButton");
            Label feedback = (Label) stage.getScene().getRoot().lookup("#resultDisplay");
            ToggleButton contacts = (ToggleButton) stage.getScene().getRoot().lookup("#contactsView");
            ToggleButton events = (ToggleButton) stage.getScene().getRoot().lookup("#eventsView");
            ToggleButton tags = (ToggleButton) stage.getScene().getRoot().lookup("#tagsView");

            input.setText("switch events");
            submit.fire();
            assertTrue(events.isSelected());
            assertTrue(views.getSelectionModel().getSelectedItem().getText().startsWith("Events"));
            assertEquals("Switched to Events view.", feedback.getText());

            input.setText("switch tags");
            submit.fire();
            assertTrue(tags.isSelected());
            assertEquals("Tags", views.getSelectionModel().getSelectedItem().getText());

            input.setText("switch contacts");
            submit.fire();
            assertTrue(contacts.isSelected());
            assertTrue(views.getSelectionModel().getSelectedItem().getText().startsWith("Contacts"));

            input.setText("switch contacts");
            submit.fire();
            assertTrue(contacts.isSelected());
            assertEquals("Switched to Contacts view.", feedback.getText());

            input.setText("switch missing");
            submit.fire();
            assertTrue(contacts.isSelected());
            assertEquals("switch missing", input.getText());
            assertTrue(feedback.getText().startsWith("Invalid command format."));

            stage.close();
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void listCommand_keepsSelectedView() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            Stage stage = new Stage();
            try {
                StorageManager storage = new StorageManager(
                        new JsonCoordiMateStorage(temporaryFolder.resolve("data.json")),
                        new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json")));
                MainWindow window = new MainWindow(stage, new LogicManager(new ModelManager(), storage),
                        temporaryFolder.resolve("data.json"));
                try {
                    assertListCommandKeepsSelectedView(window, stage);
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

    private static void assertListCommandKeepsSelectedView(MainWindow window, Stage stage) {
        window.fillInnerParts();
        stage.getScene().getRoot().applyCss();

        ToggleButton events = (ToggleButton) stage.getScene().getRoot().lookup("#eventsView");
        TextField input = (TextField) stage.getScene().getRoot().lookup("#commandTextField");
        Button submit = (Button) stage.getScene().getRoot().lookup("#submitButton");
        Label feedback = (Label) stage.getScene().getRoot().lookup("#resultDisplay");

        events.fire();
        input.setText("list");
        submit.fire();

        assertTrue(events.isSelected());
        assertEquals("Listed all contacts.", feedback.getText());
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
            assertEquals("NAVIGATION · j/k move · i to type", mode.getText());

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

    private static KeyEvent typedKeyEvent(String character) {
        return typedKeyEvent(character, false, false, false, false);
    }

    private static KeyEvent typedKeyEvent(String character, boolean shiftDown, boolean controlDown,
                                          boolean altDown, boolean metaDown) {
        return new KeyEvent(KeyEvent.KEY_TYPED, character, character, KeyCode.UNDEFINED,
                shiftDown, controlDown, altDown, metaDown);
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
}
