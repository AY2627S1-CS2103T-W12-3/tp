package coordimate.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
import coordimate.testutil.JavaFxTestSupport;
import coordimate.testutil.PersonBuilder;
import coordimate.testutil.TypicalPersons;
import javafx.animation.AnimationTimer;
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

    private static final Path DEFAULT_DATA_PATH = Path.of("data", "coordimate.json");
    private static final long LAYOUT_TIMEOUT_SECONDS = 20;

    private static final Person FIRST_CONTACT = new PersonBuilder(TypicalPersons.ALICE).withTags("Alpha").build();
    private static final Person SECOND_CONTACT = new PersonBuilder(TypicalPersons.BENSON).withTags("Beta").build();
    private static final EventTime EVENT_TIME = new EventTime("25-10-2026");
    private static final Event FIRST_EVENT = new Event("First Event", EVENT_TIME, EVENT_TIME);
    private static final Event SECOND_EVENT = new Event("Second Event", EVENT_TIME, EVENT_TIME);

    @TempDir
    public Path temporaryFolder;

    /**
     * Creates the main window under test for a fresh stage.
     */
    @FunctionalInterface
    private interface WindowFactory {
        MainWindow create(Stage stage) throws Exception;
    }

    /**
     * Exercises a main window whose inner parts have been filled.
     */
    @FunctionalInterface
    private interface WindowTest {
        void run(MainWindow window, Stage stage) throws Exception;
    }

    /**
     * Exercises card navigation through the controls it touches.
     */
    @FunctionalInterface
    private interface NavigationTest {
        void run(NavigationControls controls) throws Exception;
    }

    /**
     * The main window controls used by card navigation tests.
     */
    private record NavigationControls(TextField input, Button submit, Label mode, ListView<Person> contacts,
            ListView<Event> events, ListView<String> tags, ToggleButton contactsView,
            ToggleButton eventsView, ToggleButton tagsView, TabPane views) {

        static NavigationControls from(Stage stage) {
            return new NavigationControls(lookupNode(stage, "#commandTextField"),
                    lookupNode(stage, "#submitButton"), lookupNode(stage, "#interactionModeLabel"),
                    lookupNode(stage, "#personListView"), lookupNode(stage, "#eventListView"),
                    lookupNode(stage, "#tagListView"), lookupNode(stage, "#contactsView"),
                    lookupNode(stage, "#eventsView"), lookupNode(stage, "#tagsView"), lookupNode(stage, "#views"));
        }

        void enterNavigationMode() {
            assertFalse(keyReachesTarget(input, createKeyEvent(KeyCode.ESCAPE)));
            assertEquals(MainWindow.NAVIGATION_MODE_TEXT, mode.getText());
        }
    }

    @BeforeAll
    public static void startJavaFx() throws InterruptedException {
        JavaFxTestSupport.startToolkit();
    }

    @Test
    public void switchViews_updatesWorkspaceAndCommandHints() throws Exception {
        ModelManager model = new ModelManager();
        model.addPerson(new PersonBuilder(TypicalPersons.ALICE).withTags("EXCO").build());
        model.updateFilteredPersonList(person -> false);
        runInWindow(windowFor(model), (window, stage) -> {
            stage.getScene().getRoot().applyCss();

            TabPane views = lookupNode(stage, "#views");
            TextFlow prefixHelp = lookupNode(stage, "#prefixHelp");
            ToggleButton events = lookupNode(stage, "#eventsView");
            ToggleButton tags = lookupNode(stage, "#tagsView");
            ToggleButton contacts = lookupNode(stage, "#contactsView");

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
            ListView<String> tagList = lookupNode(stage, "#tagListView");
            tagList.getSelectionModel().selectFirst();
            assertEquals("EXCO", labelText(stage, "#selectedTagName"));
            ListView<Person> tagMembers = lookupNode(stage, "#tagMemberListView");
            assertEquals(List.of("Alice Pauline"), tagMembers.getItems().stream()
                    .map(person -> person.getName().getFullName()).toList());
            assertEquals("Members (1)", labelText(stage, "#tagMemberCount"));

            contacts.fire();
            stage.getScene().getRoot().applyCss();
            assertTrue(views.getSelectionModel().getSelectedItem().getText().startsWith("Contacts"));
            assertEquals("Bold", contacts.getFont().getStyle());
            assertTrue(getPrefixHelpText(prefixHelp).contains("n/Name"));
        });
    }

    @Test
    public void topMenuHelpItem_opensOwnedHelpWindowAndShowsShortcutTooltip() throws Exception {
        runInWindow(windowFor(new ModelManager()), (window, stage) -> {
            Label helpLabel = lookupNode(stage, "#helpLabel");
            Label tutorialLabel = lookupNode(stage, "#tutorialLabel");
            HBox topMenuRow = lookupNode(stage, "#topMenuRow");
            MenuBar menuBar = lookupNode(stage, "#menuBar");
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
            javafx.event.Event.fireEvent(helpLabel, createMouseClickEvent());
            Stage helpStage = findOwnedStage(stage, "Help");
            assertTrue(helpStage.isShowing());
            assertSame(stage, helpStage.getOwner());

            KeyEvent enterEvent = createKeyEvent(KeyCode.ENTER);
            helpLabel.getOnKeyPressed().handle(enterEvent);
            assertTrue(enterEvent.isConsumed());
            assertTrue(helpStage.isShowing());

            KeyEvent otherKeyEvent = createKeyEvent(KeyCode.A);
            helpLabel.getOnKeyPressed().handle(otherKeyEvent);
            assertFalse(otherKeyEvent.isConsumed());

            helpStage.hide();
            KeyEvent spaceEvent = createKeyEvent(KeyCode.SPACE);
            helpLabel.getOnKeyPressed().handle(spaceEvent);
            assertTrue(spaceEvent.isConsumed());
            assertTrue(helpStage.isShowing());

            helpStage.hide();
            javafx.event.Event.fireEvent(stage.getScene().getRoot(), createKeyEvent(KeyCode.F1));
            assertTrue(helpStage.isShowing());

            // F2 pressed while the Help window is in front opens the tutorial on the main window.
            javafx.event.Event.fireEvent(helpStage.getScene().getRoot(), createKeyEvent(KeyCode.F2));
            assertFalse(helpStage.isShowing());
            assertTrue(lookupNode(stage, "#tutorialOverlay").isVisible());
        });
    }

    /**
     * Opens the tutorial overlay on an already-created, already-filled {@code window} by clicking
     * the Tutorial label while the Events tab is selected and the Help window is open, matching
     * the entry point every split-out test below starts from. Takes the window/stage from the
     * caller's {@link #runInWindow} invocation so cleanup stays guaranteed by that helper even if
     * a step here throws, instead of registering cleanup only after this factory returns.
     */
    private static TutorialFixture openTutorialFromEventsWithHelpOpen(MainWindow window, Stage stage,
            LogicManager logic) {
        window.show();
        TabPane views = lookupNode(stage, "#views");
        views.getSelectionModel().select(1);
        window.handleHelp();
        Stage helpStage = findOwnedStage(stage, "Help");
        Label tutorialLabel = lookupNode(stage, "#tutorialLabel");
        javafx.event.Event.fireEvent(tutorialLabel, createMouseClickEvent());
        refreshLayout(stage);
        StackPane tutorialOverlay = lookupNode(stage, "#tutorialOverlay");
        return new TutorialFixture(stage, logic, window, views, helpStage, tutorialLabel, tutorialOverlay);
    }

    private record TutorialFixture(Stage stage, LogicManager logic, MainWindow window, TabPane views,
            Stage helpStage, Label tutorialLabel, StackPane tutorialOverlay) {}

    @Test
    public void topMenuTutorialItem_opensOverlayAndHidesHelp() throws Exception {
        LogicManager logic = new LogicManager(createModelWithAssignedEvent(), null);
        runInWindow(windowFor(logic), (window, stage) -> {
            TutorialFixture fixture = openTutorialFromEventsWithHelpOpen(window, stage, logic);
            assertTrue(fixture.tutorialOverlay().isVisible());
            assertFalse(fixture.helpStage().isShowing());
            assertTutorialStep(fixture.stage(), 1);
            assertEquals(0, fixture.views().getSelectionModel().getSelectedIndex());
        });
    }

    @Test
    public void tutorialOverlay_blocksCommandInputAndSubmission() throws Exception {
        LogicManager logic = new LogicManager(createModelWithAssignedEvent(), null);
        runInWindow(windowFor(logic), (window, stage) -> {
            openTutorialFromEventsWithHelpOpen(window, stage, logic);
            TextField commandInput = lookupNode(stage, "#commandTextField");
            Button submitButton = lookupNode(stage, "#submitButton");
            assertEquals("", commandInput.getText());
            assertTrue(commandInput.isDisabled());
            assertTrue(submitButton.isDisabled());
            commandInput.setText("add n/Tom p/9081811 e/tomjohn@email.com r/EXCO");
            commandInput.fireEvent(new ActionEvent());
            assertEquals(1, logic.getPersonList().size());
            assertTrue(lookupNode(stage, "#commandExamples") == null);
        });
    }

    @Test
    public void tutorialOverlay_highlightsEachStepAcrossContactsEventsAndTags() throws Exception {
        LogicManager logic = new LogicManager(createModelWithAssignedEvent(), null);
        runInWindow(windowFor(logic), (window, stage) -> {
            TutorialFixture fixture = openTutorialFromEventsWithHelpOpen(window, stage, logic);
            TabPane views = fixture.views();
            Button nextButton = lookupNode(stage, "#nextButton");
            fireRepeatedly(nextButton, 4);
            assertTutorialStep(stage, 5);
            assertTrue(lookupNode(stage, "#targetHighlight").isVisible());
            nextButton.fire();
            assertTutorialStep(stage, 6);
            ListView<?> contactList = lookupNode(stage, "#personListView");
            assertEquals(0, contactList.getSelectionModel().getSelectedIndex());
            assertTrue(lookupNode(stage, "#targetHighlight").isVisible());
            nextButton.fire();
            assertTutorialStep(stage, 7);
            assertEquals(1, views.getSelectionModel().getSelectedIndex());
            refreshLayout(stage);
            nextButton.fire();
            assertTutorialStep(stage, 8);
            ListView<?> eventList = lookupNode(stage, "#eventListView");
            assertEquals(0, eventList.getSelectionModel().getSelectedIndex());
            // The list is the fallback target until JavaFX creates the selected card on the next pulse.
            assertTutorialHighlights(stage, "#eventListView");
            nextButton.fire();
            assertTutorialStep(stage, 9);
            assertTrue(lookupNode(stage, "#targetHighlight").isVisible());
            nextButton.fire();
            assertTutorialStep(stage, 10);
            VBox members = lookupNode(stage, "#members");
            assertEquals(1, members.getChildren().size());
            Bounds memberBounds = members.localToScene(members.getLayoutBounds());
            Node highlight = lookupNode(stage, "#targetHighlight");
            Bounds highlightBounds = highlight.localToScene(highlight.getLayoutBounds());
            assertTrue(highlightBounds.intersects(memberBounds));
            Label memberCount = lookupNode(stage, "#memberCount");
            assertFalse(highlightBounds.intersects(memberCount.localToScene(memberCount.getLayoutBounds())));
            nextButton.fire();
            assertTutorialStep(stage, 11);
            assertEquals(2, views.getSelectionModel().getSelectedIndex());
            assertTrue(((ToggleButton) lookupNode(stage, "#tagsView")).isSelected());
            assertTutorialHighlights(stage, "#tagsView");
            nextButton.fire();
            assertTutorialStep(stage, 12);
            assertTutorialHighlights(stage, "#tagListView");
            nextButton.fire();
            assertTutorialStep(stage, 13);
            assertEquals(0, ((ListView<?>) lookupNode(stage, "#tagListView"))
                    .getSelectionModel().getSelectedIndex());
            assertTutorialHighlights(stage, "#tagDetailsPane");
            nextButton.fire();
            assertTutorialStep(stage, 14);
            assertEquals(2, views.getSelectionModel().getSelectedIndex());
            assertFalse(lookupNode(stage, "#targetHighlight").isVisible());
            nextButton.fire();
            assertTutorialStep(stage, 15);
            assertEquals(2, views.getSelectionModel().getSelectedIndex());
            assertTutorialHighlights(stage, "#helpLabel");
            nextButton.fire();
            assertTutorialStep(stage, 16);
            assertEquals(2, views.getSelectionModel().getSelectedIndex());
            assertTutorialHighlights(stage, "#tutorialLabel");
            assertEquals("Finish →", nextButton.getText());
        });
    }

    @Test
    public void tutorialOverlay_completingTourRecordsPreferenceAndReopensAtStepOne() throws Exception {
        LogicManager logic = new LogicManager(createModelWithAssignedEvent(), null);
        runInWindow(windowFor(logic), (window, stage) -> {
            TutorialFixture fixture = openTutorialFromEventsWithHelpOpen(window, stage, logic);
            TabPane views = fixture.views();
            Button nextButton = lookupNode(stage, "#nextButton");
            Button submitButton = lookupNode(stage, "#submitButton");
            fireRepeatedly(nextButton, 6);
            refreshLayout(stage);
            fireRepeatedly(nextButton, 9);
            assertTutorialStep(stage, 16);
            assertEquals(1, logic.getEventList().size());
            Button closeButton = lookupNode(stage, "#closeButton");
            closeButton.fire();
            assertTrue(logic.hasSeenTutorial());
            assertFalse(submitButton.isDisabled());
            assertEquals(2, views.getSelectionModel().getSelectedIndex());

            fixture.window().handleHelp();
            KeyEvent enterEvent = createKeyEvent(KeyCode.ENTER);
            fixture.tutorialLabel().getOnKeyPressed().handle(enterEvent);
            assertTrue(enterEvent.isConsumed());
            assertTrue(fixture.tutorialOverlay().isVisible());
            assertFalse(fixture.helpStage().isShowing());
            assertTutorialStep(stage, 1);
        });
    }

    @Test
    public void tutorialKeyboardShortcuts_blockAppAndRestoreInputOnClose() throws Exception {
        LogicManager logic = new LogicManager(new ModelManager(), null);
        runInWindow(windowFor(logic), (window, stage) -> {
            window.show();
            Label tutorialLabel = lookupNode(stage, "#tutorialLabel");
            Label helpLabel = lookupNode(stage, "#helpLabel");
            TextField commandInput = lookupNode(stage, "#commandTextField");
            Label counter = lookupNode(stage, "#stepCounter");

            tutorialLabel.fireEvent(createMouseClickEvent());
            assertTrue(commandInput.isDisabled());
            Button nextButton = lookupNode(stage, "#nextButton");
            assertTrue(keyReachesTarget(nextButton, createKeyEvent(KeyCode.F1)));
            assertTrue(keyReachesTarget(nextButton, createTypedKeyEvent("i")));
            assertTrue(commandInput.isDisabled());
            assertFalse(keyReachesTarget(helpLabel, createKeyEvent(KeyCode.RIGHT)));
            assertEquals("Step 2 of 16", counter.getText());
            tutorialLabel.fireEvent(createMouseClickEvent());
            assertEquals("Step 2 of 16", counter.getText());
            window.handleHelp();
            assertTrue(lookupNode(stage, "#tutorialOverlay").isVisible());

            assertFalse(keyReachesTarget(helpLabel, createKeyEvent(KeyCode.LEFT)));
            assertEquals("Step 1 of 16", counter.getText());
            assertFalse(keyReachesTarget(helpLabel, createKeyEvent(KeyCode.ESCAPE)));
            assertFalse(lookupNode(stage, "#tutorialOverlay").isVisible());
            assertTrue(logic.hasSeenTutorial());
            assertFalse(commandInput.isDisabled());

            commandInput.fireEvent(createKeyEvent(KeyCode.ESCAPE));
            Label modeLabel = lookupNode(stage, "#interactionModeLabel");
            assertEquals(MainWindow.NAVIGATION_MODE_TEXT, modeLabel.getText());
            tutorialLabel.fireEvent(createMouseClickEvent());
            assertEquals("Step 1 of 16", counter.getText());
            assertFalse(keyReachesTarget(helpLabel, createKeyEvent(KeyCode.ESCAPE)));
            assertEquals(MainWindow.NAVIGATION_MODE_TEXT, modeLabel.getText());
            assertTrue(commandInput.isDisabled());
        });
    }

    @Test
    public void tutorialCommandAndShortcut_openTutorialFromFirstStep() throws Exception {
        LogicManager logic = createLogicWithStorage(new ModelManager(), "tutorial");
        runInWindow(windowFor(logic, dataPath("tutorial")), (window, stage) -> {
            window.show();
            Node overlay = lookupNode(stage, "#tutorialOverlay");
            Label counter = lookupNode(stage, "#stepCounter");
            Label tutorialLabel = lookupNode(stage, "#tutorialLabel");
            TextField commandInput = lookupNode(stage, "#commandTextField");
            Button submitButton = lookupNode(stage, "#submitButton");
            Button nextButton = lookupNode(stage, "#nextButton");
            assertTrue(tutorialLabel.getTooltip().getText().contains(TutorialWindow.TUTORIAL_SHORTCUT));

            commandInput.setText("tutorial");
            submitButton.fire();
            assertTrue(overlay.isVisible());
            assertEquals("Step 1 of 16", counter.getText());
            assertTrue(commandInput.isDisabled());
            assertTrue(commandInput.getText().isEmpty());

            nextButton.fire();
            assertFalse(keyReachesTarget(commandInput, createKeyEvent(KeyCode.F2)));
            assertEquals("Step 2 of 16", counter.getText());
            assertFalse(keyReachesTarget(commandInput, createKeyEvent(KeyCode.ESCAPE)));
            assertFalse(overlay.isVisible());
            assertFalse(commandInput.isDisabled());

            assertFalse(keyReachesTarget(commandInput, createKeyEvent(KeyCode.F2)));
            assertTrue(overlay.isVisible());
            assertEquals("Step 1 of 16", counter.getText());
        });
    }

    @Test
    public void tutorialCardAndMemberSteps_rehighlightAfterJavaFxLayout() throws Exception {
        ModelManager model = createModelWithAssignedEvent();
        MainWindow window = JavaFxTestSupport.runOnFxThreadAndWait(() -> {
            Stage stage = new Stage();
            MainWindow mainWindow = new MainWindow(stage, new LogicManager(model, null), DEFAULT_DATA_PATH);
            mainWindow.fillInnerParts();
            mainWindow.show();
            Label tutorialLabel = lookupNode(stage, "#tutorialLabel");
            tutorialLabel.fireEvent(createMouseClickEvent());
            fireRepeatedly(lookupNode(stage, "#nextButton"), 4);
            assertTutorialStep(stage, 5);
            return mainWindow;
        });
        Stage stage = window.getPrimaryStage();

        try {
            JavaFxTestSupport.runOnFxThreadAndWait(() -> {
                ListView<?> contacts = lookupNode(stage, "#personListView");
                Node highlight = lookupNode(stage, "#targetHighlight");
                assertEquals(0, contacts.getSelectionModel().getSelectedIndex());
                assertTrue(highlight.localToScene(highlight.getLayoutBounds()).getHeight() < contacts.getHeight());
                fireRepeatedly(lookupNode(stage, "#nextButton"), 5);
                assertTutorialStep(stage, 10);
            });

            JavaFxTestSupport.runOnFxThreadAndWait(() -> assertTutorialHighlights(stage, "#members"));
        } finally {
            JavaFxTestSupport.runOnFxThreadAndWait(() -> {
                window.stop();
                stage.close();
            });
        }
    }

    @Test
    public void tutorialWithoutContacts_highlightsEmptyListAndKeepsDetailsHint() throws Exception {
        runInWindow(windowFor(new ModelManager()), (window, stage) -> {
            window.show();
            Label tutorialLabel = lookupNode(stage, "#tutorialLabel");
            javafx.event.Event.fireEvent(tutorialLabel, createMouseClickEvent());
            refreshLayout(stage);

            Button nextButton = lookupNode(stage, "#nextButton");
            fireRepeatedly(nextButton, 4);
            assertTutorialStep(stage, 5);
            assertTrue(lookupNode(stage, "#targetHighlight").isVisible());
            Label contactSelectionHint = (Label) lookupNode(stage, "#personListPanelPlaceholder")
                    .lookup("#selectionHint");
            assertEquals("Select a contact to view details.", contactSelectionHint.getText());
            nextButton.fire();
            assertTrue(lookupNode(stage, "#targetHighlight").isVisible());
            nextButton.fire();
            refreshLayout(stage);
            nextButton.fire();
            assertTutorialStep(stage, 8);
            assertEquals(0, ((ListView<?>) lookupNode(stage, "#eventListView")).getItems().size());
            assertTrue(lookupNode(stage, "#targetHighlight").isVisible());
            nextButton.fire();
            assertTutorialStep(stage, 9);
            Label eventSelectionHint = (Label) lookupNode(stage, "#eventListPanelPlaceholder")
                    .lookup("#selectionHint");
            assertTrue(eventSelectionHint.isVisible());
            nextButton.fire();
            assertTutorialStep(stage, 10);
            assertTrue(lookupNode(stage, "#targetHighlight").isVisible());
        });
    }

    private static ModelManager createModelWithAssignedEvent() {
        ModelManager model = new ModelManager();
        Person contact = new PersonBuilder(TypicalPersons.ALICE).build();
        model.addPerson(contact);
        model.addEvent(new Event("Welcome Fair", EVENT_TIME, EVENT_TIME, List.of(contact.getName()), Map.of()));
        return model;
    }

    private static void assertTutorialStep(Stage stage, int step) {
        assertEquals("Step " + step + " of 16", labelText(stage, "#stepCounter"));
    }

    private static void assertTutorialHighlights(Stage stage, String targetId) {
        refreshLayout(stage);
        Node highlight = lookupNode(stage, "#targetHighlight");
        Node target = lookupNode(stage, targetId);
        assertTrue(highlight.isVisible());
        assertTrue(highlight.localToScene(highlight.getLayoutBounds())
                .intersects(target.localToScene(target.getLayoutBounds())));
    }

    private static void fireRepeatedly(Button button, int times) {
        for (int i = 0; i < times; i++) {
            button.fire();
        }
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
    public void cardNavigation_escapeEntersNavigationModeAndMovesContactSelection() throws Exception {
        runInNavigationWindow(controls -> {
            TextField input = controls.input();
            ListView<Person> contacts = controls.contacts();

            assertEquals(MainWindow.COMMAND_MODE_TEXT, controls.mode().getText());
            assertFalse(input.isDisabled());
            assertFalse(controls.submit().isDisabled());
            assertTrue(keyReachesTarget(input, createKeyEvent(KeyCode.J)));
            input.clear();
            controls.enterNavigationMode();
            assertFalse(keyReachesTarget(controls.mode(), createKeyEvent(KeyCode.ESCAPE)));
            assertTrue(input.isDisabled());
            assertTrue(controls.submit().isDisabled());

            assertFalse(keyReachesTarget(input, createKeyEvent(KeyCode.J)));
            assertEquals(FIRST_CONTACT, contacts.getSelectionModel().getSelectedItem());
            assertTrue(input.getText().isEmpty());
            assertFalse(keyReachesTarget(input, createKeyEvent(KeyCode.J)));
            assertEquals(SECOND_CONTACT, contacts.getSelectionModel().getSelectedItem());
            assertTrue(input.getText().isEmpty());
            assertFalse(keyReachesTarget(input, createKeyEvent(KeyCode.K)));
            assertEquals(FIRST_CONTACT, contacts.getSelectionModel().getSelectedItem());
            assertFalse(keyReachesTarget(input, createKeyEvent(KeyCode.K)));
            assertEquals(FIRST_CONTACT, contacts.getSelectionModel().getSelectedItem());
        });
    }

    @Test
    public void cardNavigation_eventsView_movesEventSelection() throws Exception {
        runInNavigationWindow(controls -> {
            TextField input = controls.input();
            ListView<Event> events = controls.events();
            controls.enterNavigationMode();

            controls.eventsView().fire();
            assertFalse(keyReachesTarget(input, createKeyEvent(KeyCode.J)));
            assertEquals(FIRST_EVENT, events.getSelectionModel().getSelectedItem());
            assertFalse(keyReachesTarget(input, createKeyEvent(KeyCode.J)));
            assertEquals(SECOND_EVENT, events.getSelectionModel().getSelectedItem());
            assertFalse(keyReachesTarget(input, createKeyEvent(KeyCode.K)));
            assertEquals(FIRST_EVENT, events.getSelectionModel().getSelectedItem());
        });
    }

    @Test
    public void cardNavigation_tagsView_movesTagSelectionWithinBounds() throws Exception {
        runInNavigationWindow(controls -> {
            TextField input = controls.input();
            ListView<String> tags = controls.tags();
            int lastIndex = tags.getItems().size() - 1;
            controls.enterNavigationMode();

            controls.tagsView().fire();
            assertFalse(keyReachesTarget(input, createKeyEvent(KeyCode.K)));
            assertEquals(lastIndex, tags.getSelectionModel().getSelectedIndex());
            assertFalse(keyReachesTarget(input, createKeyEvent(KeyCode.J)));
            assertEquals(lastIndex, tags.getSelectionModel().getSelectedIndex());
            assertFalse(keyReachesTarget(input, createKeyEvent(KeyCode.K)));
            assertEquals(lastIndex - 1, tags.getSelectionModel().getSelectedIndex());
            tags.getSelectionModel().clearSelection();
            assertFalse(keyReachesTarget(input, createKeyEvent(KeyCode.J)));
            assertEquals(0, tags.getSelectionModel().getSelectedIndex());
        });
    }

    @Test
    public void cardNavigation_switchViewShortcut_cyclesViewsAndKeepsSelections() throws Exception {
        runInNavigationWindow(controls -> {
            Label mode = controls.mode();
            controls.enterNavigationMode();
            assertFalse(keyReachesTarget(controls.input(), createKeyEvent(KeyCode.J)));
            controls.eventsView().fire();
            assertFalse(keyReachesTarget(controls.input(), createKeyEvent(KeyCode.J)));
            controls.tagsView().fire();
            assertFalse(keyReachesTarget(controls.input(), createKeyEvent(KeyCode.J)));

            controls.contactsView().fire();
            assertEquals(FIRST_CONTACT, controls.contacts().getSelectionModel().getSelectedItem());
            assertFalse(keyReachesTarget(controls.contactsView(), createKeyEvent(KeyCode.J)));
            assertEquals(SECOND_CONTACT, controls.contacts().getSelectionModel().getSelectedItem());
            assertFalse(keyReachesTarget(mode, createKeyEvent(KeyCode.S)));
            assertTrue(controls.eventsView().isSelected());
            assertEquals(FIRST_EVENT, controls.events().getSelectionModel().getSelectedItem());
            assertFalse(keyReachesTarget(mode, createKeyEvent(KeyCode.S)));
            assertTrue(controls.tagsView().isSelected());
            assertEquals(0, controls.tags().getSelectionModel().getSelectedIndex());
            assertFalse(keyReachesTarget(mode, createKeyEvent(KeyCode.S)));
            assertTrue(controls.contactsView().isSelected());
            assertEquals(SECOND_CONTACT, controls.contacts().getSelectionModel().getSelectedItem());
        });
    }

    @Test
    public void cardNavigation_clickingInput_returnsToCommandModeAndKeepsDraft() throws Exception {
        runInNavigationWindow(controls -> {
            TextField input = controls.input();
            Label mode = controls.mode();
            controls.enterNavigationMode();

            input.setText("list");
            fireMousePress(input);
            assertEquals(MainWindow.COMMAND_MODE_TEXT, mode.getText());
            assertEquals("list", input.getText());
            assertFalse(input.isDisabled());
            assertFalse(controls.submit().isDisabled());
            fireMousePress(input);
            assertEquals(MainWindow.COMMAND_MODE_TEXT, mode.getText());
            assertTrue(keyReachesTarget(input, createTypedKeyEvent("i")));
        });
    }

    @Test
    public void cardNavigation_releasingUnmodifiedI_returnsToCommandMode() throws Exception {
        runInNavigationWindow(controls -> {
            TextField input = controls.input();
            Label mode = controls.mode();
            controls.enterNavigationMode();

            assertFalse(keyReachesTarget(controls.contactsView(), createKeyEvent(KeyCode.I)));
            assertEquals(MainWindow.NAVIGATION_MODE_TEXT, mode.getText());
            assertTrue(input.isDisabled());
            assertTrue(controls.submit().isDisabled());
            assertTrue(keyReachesTarget(mode, createKeyReleasedEvent(KeyCode.A)));
            assertEquals(MainWindow.NAVIGATION_MODE_TEXT, mode.getText());
            assertFalse(keyReachesTarget(mode, createKeyReleasedEvent(KeyCode.I)));
            assertEquals(MainWindow.COMMAND_MODE_TEXT, mode.getText());
            assertFalse(input.isDisabled());
            assertFalse(controls.submit().isDisabled());
            assertTrue(keyReachesTarget(input, createKeyReleasedEvent(KeyCode.I)));

            controls.enterNavigationMode();
            assertTrue(keyReachesTarget(mode, createKeyReleasedEvent(KeyCode.I, true, false, false, false)));
            assertEquals(MainWindow.NAVIGATION_MODE_TEXT, mode.getText());
            assertFalse(keyReachesTarget(mode, createKeyEvent(KeyCode.I)));
        });
    }

    @Test
    public void cardNavigation_clickingList_entersNavigationMode() throws Exception {
        runInNavigationWindow(controls -> {
            TextField input = controls.input();
            Label mode = controls.mode();

            fireMousePress(controls.contacts());
            assertEquals(MainWindow.NAVIGATION_MODE_TEXT, mode.getText());
            fireMousePress(input);
            assertEquals(MainWindow.COMMAND_MODE_TEXT, mode.getText());
            fireMousePress(controls.events());
            assertEquals(MainWindow.NAVIGATION_MODE_TEXT, mode.getText());
            fireMousePress(input);
            assertEquals(MainWindow.COMMAND_MODE_TEXT, mode.getText());
            fireMousePress(controls.tags());
            assertEquals(MainWindow.NAVIGATION_MODE_TEXT, mode.getText());
        });
    }

    @Test
    public void cardNavigation_commandMode_leavesShortcutsToInputAndKeepsDraft() throws Exception {
        runInNavigationWindow(controls -> {
            TextField input = controls.input();
            ListView<Person> contacts = controls.contacts();
            Label mode = controls.mode();

            input.setText("list");
            assertTrue(keyReachesTarget(input, createKeyEvent(KeyCode.J)));
            assertEquals(-1, contacts.getSelectionModel().getSelectedIndex());
            input.setText("list");
            controls.enterNavigationMode();
            assertFalse(keyReachesTarget(input, createKeyEvent(KeyCode.J)));
            assertEquals(FIRST_CONTACT, contacts.getSelectionModel().getSelectedItem());
            assertEquals("list", input.getText());
            assertFalse(keyReachesTarget(input, createKeyEvent(KeyCode.I)));
            assertEquals(MainWindow.NAVIGATION_MODE_TEXT, mode.getText());
            assertFalse(keyReachesTarget(mode, createKeyReleasedEvent(KeyCode.I)));
            assertEquals(MainWindow.COMMAND_MODE_TEXT, mode.getText());
            assertEquals("list", input.getText());
        });
    }

    @Test
    public void cardNavigation_navigationMode_consumesUnsupportedKeysAndIgnoresModifiedShortcuts()
            throws Exception {
        runInNavigationWindow(controls -> {
            Label mode = controls.mode();
            controls.enterNavigationMode();

            assertFalse(keyReachesTarget(controls.input(), createKeyEvent(KeyCode.A)));
            assertTrue(controls.input().getText().isEmpty());
            assertTrue(keyReachesTarget(mode, createKeyEvent(KeyCode.J, true, false, false, false)));
            assertTrue(keyReachesTarget(mode, createKeyEvent(KeyCode.J, false, true, false, false)));
            assertTrue(keyReachesTarget(mode, createKeyEvent(KeyCode.J, false, false, true, false)));
            assertTrue(keyReachesTarget(mode, createKeyEvent(KeyCode.J, false, false, false, true)));
            assertEquals(-1, controls.contacts().getSelectionModel().getSelectedIndex());
        });
    }

    @Test
    public void cardNavigation_navigationMode_handlesShortcutsWhereverFocusIs() throws Exception {
        runInNavigationWindow(controls -> {
            ListView<Person> contacts = controls.contacts();
            Label mode = controls.mode();
            controls.enterNavigationMode();

            TextField otherInput = new TextField();
            StackPane commandPlaceholder = (StackPane) controls.input().getScene().lookup("#commandBoxPlaceholder");
            commandPlaceholder.getChildren().add(otherInput);
            otherInput.requestFocus();
            assertSame(otherInput, otherInput.getScene().getFocusOwner());
            assertFalse(keyReachesTarget(otherInput, createKeyEvent(KeyCode.J)));
            assertEquals(FIRST_CONTACT, contacts.getSelectionModel().getSelectedItem());

            contacts.requestFocus();
            assertFalse(keyReachesTarget(contacts, createKeyEvent(KeyCode.I)));
            assertEquals(MainWindow.NAVIGATION_MODE_TEXT, mode.getText());
            assertFalse(keyReachesTarget(mode, createKeyReleasedEvent(KeyCode.I)));
            assertEquals(MainWindow.COMMAND_MODE_TEXT, mode.getText());
        });
    }

    @Test
    public void cardNavigation_withoutSelectedView_ignoresNavigationShortcuts() throws Exception {
        runInNavigationWindow(controls -> {
            TabPane views = controls.views();
            views.getSelectionModel().clearSelection();
            controls.enterNavigationMode();

            assertFalse(keyReachesTarget(controls.input(), createKeyEvent(KeyCode.J)));
            assertEquals(-1, controls.contacts().getSelectionModel().getSelectedIndex());
            views.getTabs().clear();
            assertFalse(keyReachesTarget(controls.mode(), createKeyEvent(KeyCode.S)));
            assertTrue(views.getTabs().isEmpty());
        });
    }

    @Test
    public void cardNavigation_emptyListsConsumeNavigationKeysWithoutChangingSelection() throws Exception {
        ModelManager model = new ModelManager();
        List.copyOf(model.getTagList()).forEach(model::deleteTag);
        runInWindow(windowFor(model), (window, stage) -> {
            stage.getScene().getRoot().applyCss();

            TextField input = lookupNode(stage, "#commandTextField");
            ToggleButton eventsView = lookupNode(stage, "#eventsView");
            ToggleButton tagsView = lookupNode(stage, "#tagsView");

            assertFalse(keyReachesTarget(input, createKeyEvent(KeyCode.ESCAPE)));
            assertFalse(keyReachesTarget(input, createKeyEvent(KeyCode.J)));
            eventsView.fire();
            assertFalse(keyReachesTarget(input, createKeyEvent(KeyCode.K)));
            tagsView.fire();
            assertFalse(keyReachesTarget(input, createKeyEvent(KeyCode.J)));
        });
    }

    @Test
    public void cardNavigation_deleteConfirmationLeavesNavigationKeysForInput() throws Exception {
        ModelManager model = new ModelManager();
        model.addPerson(new PersonBuilder(TypicalPersons.ALICE).build());
        LogicManager logic = createLogicWithStorage(model, "navigation");
        runInWindow(windowFor(logic, dataPath("navigation")), (window, stage) -> {
            stage.getScene().getRoot().applyCss();

            TextField input = lookupNode(stage, "#commandTextField");
            Button submit = lookupNode(stage, "#submitButton");
            input.setText("delete 1");
            submit.fire();

            assertTrue(logic.isAwaitingDeleteConfirmation());
            assertTrue(input.getText().isEmpty());
            assertTrue(keyReachesTarget(input, createKeyEvent(KeyCode.ESCAPE)));
            assertTrue(keyReachesTarget(input, createKeyEvent(KeyCode.A)));
            assertTrue(keyReachesTarget(input, createKeyEvent(KeyCode.J)));
            ListView<Person> contacts = lookupNode(stage, "#personListView");
            Label mode = lookupNode(stage, "#interactionModeLabel");
            fireMousePress(contacts);
            assertEquals(MainWindow.COMMAND_MODE_TEXT, mode.getText());
            assertEquals(-1, contacts.getSelectionModel().getSelectedIndex());
        });
    }

    private static void runInNavigationWindow(NavigationTest test) throws Exception {
        ModelManager model = new ModelManager();
        model.addPerson(FIRST_CONTACT);
        model.addPerson(SECOND_CONTACT);
        model.addEvent(FIRST_EVENT);
        model.addEvent(SECOND_EVENT);
        runInWindow(windowFor(model), (window, stage) -> {
            window.show();
            stage.getScene().getRoot().applyCss();
            test.run(NavigationControls.from(stage));
        });
    }

    @Test
    public void listCommand_opensContactsView() throws Exception {
        LogicManager logic = createLogicWithStorage(new ModelManager(), "list-command");
        runInWindow(windowFor(logic, dataPath("list-command")), (window, stage) -> {
            stage.getScene().getRoot().applyCss();

            ToggleButton events = lookupNode(stage, "#eventsView");
            ToggleButton contacts = lookupNode(stage, "#contactsView");
            TabPane views = lookupNode(stage, "#views");
            TextField input = lookupNode(stage, "#commandTextField");
            Button submit = lookupNode(stage, "#submitButton");
            Label feedback = lookupNode(stage, "#resultDisplay");

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
        });
    }

    @Test
    public void longFeedback_atMinimumWindowSize_keepsWorkspaceAndCommandInputVisible() throws Exception {
        ModelManager model = createMinimumSizeModel();
        model.addPerson(new PersonBuilder().withName("Alexandria ".repeat(7).strip())
                .withEmail("a".repeat(64) + "@" + "b".repeat(63) + "."
                        + "c".repeat(60) + "." + "d".repeat(60) + ".com").build());
        for (int index = 1; index <= 86; index++) {
            model.addTag(new Tag("Custom" + index + "LongNameLongName"));
        }
        LogicManager logic = createLogicWithStorage(model, "long-feedback");
        runInWindow(windowFor(logic, dataPath("long-feedback")), (window, stage) -> {
            window.show();

            TextField input = lookupNode(stage, "#commandTextField");
            Button submit = lookupNode(stage, "#submitButton");
            Label feedback = lookupNode(stage, "#resultDisplay");
            ScrollPane feedbackScroll = lookupNode(stage, "#feedbackMessageScroll");
            TabPane views = lookupNode(stage, "#views");

            String keyword = "a".repeat(3000);
            String noMatchFeedback = "No contacts found matching \"" + keyword + "\".";
            input.setText("search " + keyword);
            submit.fire();
            refreshLayout(stage);
            assertEquals(noMatchFeedback, feedback.getText());
            assertLongFeedbackFitsWindow(stage, input, feedbackScroll, views);

            ToggleButton tagsView = lookupNode(stage, "#tagsView");
            tagsView.fire();
            refreshLayout(stage);
            assertEquals(noMatchFeedback, feedback.getText());
            assertEquals(90, ((ListView<?>) lookupNode(stage, "#tagListView")).getItems().size());
            assertLongFeedbackFitsWindow(stage, input, feedbackScroll, views);
            assertTrue(lookupNode(stage, "#tagListView").getBoundsInLocal().getHeight() > 30);

            input.setText("delete 1");
            submit.fire();
            refreshLayout(stage);
            assertTrue(feedback.getText().contains("Confirm deletion? [y/N]"));
            assertTrue(feedbackScroll.getHeight() > 84);
            assertTrue(feedbackScroll.getContent().getLayoutBounds().getHeight()
                    <= feedbackScroll.getViewportBounds().getHeight() + 1);
            assertFalse(lookupNode(stage, "#feedbackScrollHint").isVisible());
            assertTrue(views.getHeight() > 100);
            assertTrue(input.localToScene(input.getBoundsInLocal()).getMaxY() <= stage.getScene().getHeight());

            input.setText("n");
            submit.fire();

            input.setText("list");
            submit.fire();
            refreshLayout(stage);
            assertEquals("Listed all contacts.", feedback.getText());
            assertTrue(feedbackScroll.getHeight() < 84);
            assertFalse(lookupNode(stage, "#feedbackScrollHint").isVisible());
        }, LAYOUT_TIMEOUT_SECONDS);
    }

    private static void assertLongFeedbackFitsWindow(Stage stage, TextField input, ScrollPane feedbackScroll,
                                                     TabPane views) {
        assertTrue(feedbackScroll.getHeight() <= 220);
        assertTrue(feedbackScroll.getContent().getLayoutBounds().getHeight()
                > feedbackScroll.getViewportBounds().getHeight());
        assertTrue(lookupNode(stage, "#feedbackScrollHint").isVisible());
        assertTrue(feedbackScroll.lookup(".scroll-bar:vertical").isVisible());
        assertTrue(views.getHeight() > 100);
        Bounds inputBounds = input.localToScene(input.getBoundsInLocal());
        assertTrue(inputBounds.getMaxY() <= stage.getScene().getHeight());
    }

    @Test
    public void feedbackLayout_longSentenceAndRepeatedMediumMessage_staysConsistent() throws Exception {
        LogicManager logic = createLogicWithStorage(createMinimumSizeModel(), "feedback-layout");
        MainWindow window = JavaFxTestSupport.runOnFxThreadAndWait(() -> {
            MainWindow mainWindow = new MainWindow(new Stage(), logic, dataPath("feedback-layout"));
            mainWindow.fillInnerParts();
            mainWindow.show();
            return mainWindow;
        });
        Stage stage = window.getPrimaryStage();
        String mediumSearch = "search " + "a".repeat(130);
        try {
            submitFeedbackCommand(stage, "list");
            submitFeedbackCommand(stage, mediumSearch);
            double mediumWidth = JavaFxTestSupport.runOnFxThreadAndWait(() -> {
                ScrollPane scroll = lookupNode(stage, "#feedbackMessageScroll");
                assertFalse(lookupNode(stage, "#feedbackScrollHint").isVisible());
                return scroll.getWidth();
            });

            submitFeedbackCommand(stage, "search " + "a".repeat(3000));
            JavaFxTestSupport.runOnFxThreadAndWait(() -> {
                ScrollPane scroll = lookupNode(stage, "#feedbackMessageScroll");
                assertTrue(scroll.getHeight() >= 60);
                assertTrue(scroll.getHeight() <= 220);
                assertTrue(lookupNode(stage, "#feedbackScrollHint").isVisible());
            });

            submitFeedbackCommand(stage, mediumSearch);
            JavaFxTestSupport.runOnFxThreadAndWait(() -> {
                ScrollPane scroll = lookupNode(stage, "#feedbackMessageScroll");
                assertFalse(lookupNode(stage, "#feedbackScrollHint").isVisible());
                assertEquals(mediumWidth, scroll.getWidth(), 1);
            });
        } finally {
            JavaFxTestSupport.runOnFxThreadAndWait(() -> {
                window.stop();
                stage.close();
            });
        }
    }

    private static ModelManager createMinimumSizeModel() {
        ModelManager model = new ModelManager();
        model.setGuiSettings(new GuiSettings(WindowBounds.PREFERRED_MIN_WIDTH,
                WindowBounds.PREFERRED_MIN_HEIGHT, 0, 0, false));
        return model;
    }

    private static void submitFeedbackCommand(Stage stage, String command) throws Exception {
        JavaFxTestSupport.runOnFxThreadAndWait(() -> {
            TextField input = lookupNode(stage, "#commandTextField");
            Button submit = lookupNode(stage, "#submitButton");
            input.setText(command);
            submit.fire();
        });
        waitForJavaFxPulses();
    }

    private static void waitForJavaFxPulses() throws InterruptedException {
        CountDownLatch completed = new CountDownLatch(1);
        Platform.runLater(() -> new AnimationTimer() {
            private int pulseCount;

            @Override
            public void handle(long now) {
                if (++pulseCount == 3) {
                    stop();
                    completed.countDown();
                }
            }
        }.start());
        assertTrue(completed.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void minimumWindowSize_keepsDetailsBesideListsAndEventTimingVisible() throws Exception {
        boolean isScreenTooSmall = JavaFxTestSupport.runOnFxThreadAndWait(() -> {
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            return screenBounds.getWidth() < WindowBounds.PREFERRED_MIN_WIDTH
                    || screenBounds.getHeight() < WindowBounds.PREFERRED_MIN_HEIGHT;
        });
        if (isScreenTooSmall) {
            return;
        }
        ModelManager model = createMinimumSizeModel();
        model.addPerson(TypicalPersons.ALICE);
        EventTime start = new EventTime("01-10-2026 18:00");
        EventTime end = new EventTime("01-10-2026 21:00");
        model.addEvent(new Event("Final Concert", start, end));
        runInWindow(windowFor(model), (window, stage) -> {
            window.show();
            refreshLayout(stage);

            SplitPane contacts = lookupNode(stage, "#contactSplitPane");
            assertEquals(Orientation.HORIZONTAL, contacts.getOrientation());
            assertTrue(contacts.getItems().get(1).getBoundsInParent().getWidth() >= 460);

            ToggleButton eventsView = lookupNode(stage, "#eventsView");
            eventsView.fire();
            ListView<?> eventList = lookupNode(stage, "#eventListView");
            eventList.getSelectionModel().select(0);
            refreshLayout(stage);
            SplitPane events = lookupNode(stage, "#eventSplitPane");
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

            ToggleButton tagsView = lookupNode(stage, "#tagsView");
            tagsView.fire();
            refreshLayout(stage);
            SplitPane tags = lookupNode(stage, "#tagSplitPane");
            assertEquals(Orientation.HORIZONTAL, tags.getOrientation());
            assertTrue(lookupNode(stage, "#tagMemberListView").getBoundsInLocal().getHeight() > 100);
        }, LAYOUT_TIMEOUT_SECONDS);
    }

    @Test
    public void assignmentCommands_keepCurrentView() throws Exception {
        ModelManager model = new ModelManager();
        model.addPerson(TypicalPersons.ALICE);
        EventTime date = new EventTime("08-08-2026");
        model.addEvent(new Event("Orientation", date, date));
        LogicManager logic = createLogicWithStorage(model, "assignment");
        runInWindow(windowFor(logic, dataPath("assignment")), (window, stage) -> {
            stage.getScene().getRoot().applyCss();

            ToggleButton contacts = lookupNode(stage, "#contactsView");
            ToggleButton events = lookupNode(stage, "#eventsView");
            TextField input = lookupNode(stage, "#commandTextField");
            Button submit = lookupNode(stage, "#submitButton");

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
        });
    }

    @Test
    public void deleteCommand_opensContactsForConfirmation() throws Exception {
        ModelManager model = new ModelManager();
        model.addPerson(TypicalPersons.ALICE);
        LogicManager logic = createLogicWithStorage(model, "delete-routing");
        runInWindow(windowFor(logic, dataPath("delete-routing")), (window, stage) -> {
            stage.getScene().getRoot().applyCss();

            ToggleButton contacts = lookupNode(stage, "#contactsView");
            ToggleButton events = lookupNode(stage, "#eventsView");
            TextField input = lookupNode(stage, "#commandTextField");
            Button submit = lookupNode(stage, "#submitButton");
            Label feedback = lookupNode(stage, "#resultDisplay");

            events.fire();
            input.setText("delete 1");
            submit.fire();
            assertTrue(contacts.isSelected());
            assertTrue(feedback.getText().contains("Confirm deletion? [y/N]"));

            input.setText("n");
            submit.fire();
            assertTrue(contacts.isSelected());
            assertEquals("Deletion cancelled. No changes were made.", feedback.getText());
        });
    }

    @Test
    public void contactCommands_switchToContactsAndFocusAffectedContact() throws Exception {
        ModelManager model = new ModelManager();
        model.addPerson(TypicalPersons.ALICE);
        model.addPerson(TypicalPersons.BENSON);
        LogicManager logic = createLogicWithStorage(model, "contact-focus");
        runInWindow(windowFor(logic, dataPath("contact-focus")), (window, stage) -> {
            stage.getScene().getRoot().applyCss();

            ToggleButton contactsView = lookupNode(stage, "#contactsView");
            ToggleButton eventsView = lookupNode(stage, "#eventsView");
            TextField input = lookupNode(stage, "#commandTextField");
            Button submit = lookupNode(stage, "#submitButton");
            ListView<Person> contacts = lookupNode(stage, "#personListView");
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
        });
    }

    @Test
    public void showWindow_startsMaximized() throws Exception {
        JavaFxTestSupport.runOnFxThreadAndWait(() -> {
            Stage stage = new Stage();
            MainWindow window = new MainWindow(stage, new LogicManager(new ModelManager(), null),
                    DEFAULT_DATA_PATH);
            window.fillInnerParts();

            window.show();
            assertTrue(stage.isMaximized());
            window.scheduleNormalWindowBoundsUpdate();
            window.scheduleNormalWindowBoundsUpdate();

            Platform.runLater(stage::close);
            return null;
        }, LAYOUT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    public void showWindow_savedAsNotMaximized_startsNotMaximized() throws Exception {
        JavaFxTestSupport.runOnFxThreadAndWait(() -> {
            Stage stage = new Stage();
            ModelManager model = new ModelManager();
            model.setGuiSettings(new GuiSettings(900, 700, 0, 0, false));
            MainWindow window = new MainWindow(stage, new LogicManager(model, null), DEFAULT_DATA_PATH);
            window.fillInnerParts();
            window.scheduleNormalWindowBoundsUpdate();

            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            stage.setMinWidth(0);
            stage.setMinHeight(0);
            stage.setWidth(Math.min(900, screenBounds.getWidth() - 100));
            stage.setHeight(Math.min(700, screenBounds.getHeight() - 100));
            window.show();
            assertFalse(stage.isMaximized());
            window.scheduleNormalWindowBoundsUpdate();
            window.scheduleNormalWindowBoundsUpdate();

            stage.setWidth(Math.min(950, screenBounds.getWidth() - 100));
            stage.setHeight(Math.min(700, screenBounds.getHeight() - 100));
            stage.setX(screenBounds.getMinX());
            stage.setY(screenBounds.getMinY());
            double expectedWidth = stage.getWidth();
            double expectedHeight = stage.getHeight();
            int expectedX = (int) stage.getX();
            int expectedY = (int) stage.getY();
            stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));

            assertEquals(new GuiSettings(expectedWidth, expectedHeight, expectedX, expectedY, false),
                    model.getGuiSettings());
        });
    }

    @Test
    public void createWindow_savedPositionOutsideAllScreens_usesPrimaryScreen() throws Exception {
        JavaFxTestSupport.runOnFxThreadAndWait(() -> {
            Stage stage = new Stage();
            ModelManager model = new ModelManager();
            model.setGuiSettings(new GuiSettings(900, 700, Integer.MAX_VALUE, Integer.MAX_VALUE, false));

            new MainWindow(stage, new LogicManager(model, null), DEFAULT_DATA_PATH);

            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            assertTrue(screenBounds.contains(stage.getX(), stage.getY()));
            stage.close();
        });
    }

    @Test
    public void closeWindow_whileMaximized_savesNormalBoundsAndMaximizedState() throws Exception {
        JavaFxTestSupport.runOnFxThreadAndWait(() -> {
            Stage stage = new Stage();
            ModelManager model = new ModelManager();
            MainWindow window = new MainWindow(stage, new LogicManager(model, null), DEFAULT_DATA_PATH);
            window.fillInnerParts();
            double normalWidth = stage.getWidth();
            double normalHeight = stage.getHeight();
            int normalX = (int) stage.getX();
            int normalY = (int) stage.getY();

            window.show();
            stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));

            assertEquals(new GuiSettings(normalWidth, normalHeight, normalX, normalY, true), model.getGuiSettings());
        });
    }

    @Test
    public void clickCommandRow_insertsCommandAndShowsFormatAndExample() throws Exception {
        runInWindow(windowFor(new ModelManager()), (window, stage) -> {
            stage.getScene().getRoot().applyCss();

            HBox addRow = lookupNode(stage, ".cheat-row");
            addRow.getOnMouseClicked().handle(null);
            TextField commandInput = lookupNode(stage, ".text-field");
            TextFlow prefixHelp = lookupNode(stage, "#prefixHelp");
            assertEquals("add", commandInput.getText());
            String hintText = getPrefixHelpText(prefixHelp);
            assertTrue(hintText.startsWith("Format: add n/NAME p/PHONE e/EMAIL r/ROLE"));
            assertTrue(hintText.contains("[t/TAG]... [m/NOTE]"));
            assertTrue(hintText.contains("\nExample: add n/Aisha Tan"));
        });
    }

    @Test
    public void clickFilterCommandRow_showsExampleWithFullOrganisationName() throws Exception {
        runInWindow(windowFor(new ModelManager()), (window, stage) -> {
            stage.getScene().getRoot().applyCss();

            HBox filterRow = (HBox) stage.getScene().getRoot().lookupAll(".cheat-row").stream()
                    .filter(row -> "filter".equals(((Label) row.lookup(".cheat-name")).getText()))
                    .findFirst().orElseThrow();
            filterRow.getOnMouseClicked().handle(null);
            TextField commandInput = lookupNode(stage, ".text-field");
            TextFlow prefixHelp = lookupNode(stage, "#prefixHelp");
            assertEquals("filter", commandInput.getText());
            assertEquals("Format: filter FIELD/VALUE [FIELD/VALUE]...\n"
                    + "Example: filter tag/EXCO organisation/NUS Student Affairs", getPrefixHelpText(prefixHelp));
        });
    }

    @Test
    public void tagMembers_showRolesOrganisationsAndTagColors() throws Exception {
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
        runInWindow(windowFor(model), (window, stage) -> {
            window.show();

            refreshLayout(stage);
            ToggleButton tags = lookupNode(stage, "#tagsView");
            tags.fire();
            ListView<String> tagList = lookupNode(stage, "#tagListView");
            tagList.getSelectionModel().select("EXCO");
            refreshLayout(stage);

            Node root = stage.getScene().getRoot();
            assertEquals(3, root.lookupAll(".event-member-name").size());
            assertEquals(List.of("ID: 1", "ID: 2", "ID: 3"),
                    root.lookupAll(".tag-member-row .event-member-id").stream()
                            .map(node -> ((Label) node).getText()).sorted().toList());
            assertEquals(1, root.lookupAll(".tag-chip-teal").size());
            assertEquals(1, root.lookupAll(".tag-chip-amber").size());
            assertEquals(1, root.lookupAll(".tag-chip-purple").size());
            assertEquals(3, root.lookupAll(".tag-chip-red").size());
            assertEquals(1, root.lookupAll(".tag-chip-blue").size());
            assertEquals(1, root.lookupAll(".event-member-avatar-teal").size());
            assertEquals(1, root.lookupAll(".event-member-avatar-amber").size());
            assertEquals(1, root.lookupAll(".event-member-avatar-purple").size());
            assertTrue(root.lookupAll(".event-member-metadata").stream()
                    .anyMatch(node -> ((Label) node).getText().equals("Logistics Lead · NUS")));

            tagList.getSelectionModel().clearSelection();
            assertEquals("Select a tag", labelText(stage, "#selectedTagName"));
            ListView<Person> tagMembers = lookupNode(stage, "#tagMemberListView");
            assertEquals("Select a tag to view its members.", placeholderText(tagMembers));

            SplitPane tagSplitPane = lookupNode(stage, "#tagSplitPane");
            tagSplitPane.resize(900, 500);
            assertEquals(Orientation.HORIZONTAL, tagSplitPane.getOrientation());
            tagSplitPane.resize(600, 500);
            assertEquals(Orientation.VERTICAL, tagSplitPane.getOrientation());
        });
    }

    @Test
    public void tagMembers_switchingTags_clearsStaleContentAndShowsNewMembers() throws Exception {
        ModelManager model = new ModelManager();
        model.addPerson(new PersonBuilder().withName("Aisha Tan").withPhone("91234567")
                .withEmail("aisha@example.com").withRole("Logistics Lead")
                .withOrganisation("NUS").withTags("Logistics").build());
        model.addPerson(new PersonBuilder().withName("Ben Lim").withPhone("91234568")
                .withEmail("ben@example.com").withRole("Sponsor Liaison")
                .withOrganisation("Student Union").withTags("Sponsor").build());
        model.updateFilteredPersonList(person -> false);
        runInWindow(windowFor(model), (window, stage) -> {
            window.show();
            refreshLayout(stage);
            ToggleButton tags = lookupNode(stage, "#tagsView");
            tags.fire();
            ListView<String> tagList = lookupNode(stage, "#tagListView");
            Node root = stage.getScene().getRoot();

            tagList.getSelectionModel().select("Logistics");
            refreshLayout(stage);
            assertEquals(List.of("Aisha Tan"), root.lookupAll(".tag-member-row .event-member-name").stream()
                    .map(node -> ((Label) node).getText()).toList());
            assertEquals(1, root.lookupAll(".event-member-avatar-teal").size());
            assertEquals(0, root.lookupAll(".event-member-avatar-amber").size());

            tagList.getSelectionModel().select("Sponsor");
            refreshLayout(stage);
            assertEquals(List.of("Ben Lim"), root.lookupAll(".tag-member-row .event-member-name").stream()
                    .map(node -> ((Label) node).getText()).toList());
            assertEquals(0, root.lookupAll(".event-member-avatar-teal").size());
            assertEquals(1, root.lookupAll(".event-member-avatar-amber").size());

            tagList.getSelectionModel().clearSelection();
            refreshLayout(stage);
            assertEquals(0, root.lookupAll(".tag-member-row").size());

            tagList.getSelectionModel().select("Logistics");
            refreshLayout(stage);
            assertEquals(List.of("Aisha Tan"), root.lookupAll(".tag-member-row .event-member-name").stream()
                    .map(node -> ((Label) node).getText()).toList());
            assertEquals(1, root.lookupAll(".event-member-avatar-teal").size());
            assertEquals(0, root.lookupAll(".event-member-avatar-amber").size());
        });
    }

    @Test
    public void commandRow_supportsKeyboardActivation() throws Exception {
        runInWindow(windowFor(new ModelManager()), (window, stage) -> {
            stage.getScene().getRoot().applyCss();

            HBox addRow = lookupNode(stage, ".cheat-row");
            TextField commandInput = lookupNode(stage, ".text-field");
            Label mode = lookupNode(stage, "#interactionModeLabel");
            assertFalse(keyReachesTarget(commandInput, createKeyEvent(KeyCode.ESCAPE)));
            assertEquals(MainWindow.NAVIGATION_MODE_TEXT, mode.getText());

            KeyEvent enter = createKeyEvent(KeyCode.ENTER);
            addRow.getOnKeyPressed().handle(enter);
            assertTrue(enter.isConsumed());
            assertEquals("add", commandInput.getText());
            assertEquals(MainWindow.COMMAND_MODE_TEXT, mode.getText());

            KeyEvent space = createKeyEvent(KeyCode.SPACE);
            addRow.getOnKeyPressed().handle(space);
            assertTrue(space.isConsumed());

            KeyEvent otherKey = createKeyEvent(KeyCode.A);
            addRow.getOnKeyPressed().handle(otherKey);
            assertFalse(otherKey.isConsumed());
            assertEquals("add", commandInput.getText());
        });
    }

    @Test
    public void archivedContacts_markedInTagsViewAndCommandPanelListsArchiveCommands() throws Exception {
        ModelManager model = new ModelManager();
        model.addPerson(TypicalPersons.ALICE);
        model.addPerson(TypicalPersons.BENSON);
        model.setPerson(TypicalPersons.ALICE, TypicalPersons.ALICE.withArchived(true));
        runInWindow(windowFor(model), (window, stage) -> {
            window.show();
            stage.getScene().getRoot().applyCss();

            assertEquals(List.of("add", "edit", "delete", "archive", "unarchive", "search", "filter", "list"),
                    commandNames(stage, "#contactsCommands"));
            assertTrue(commandNames(stage, "#eventsCommands").containsAll(
                    List.of("deleteevent", "archiveevent", "unarchiveevent")));

            ToggleButton tagsView = lookupNode(stage, "#tagsView");
            tagsView.fire();
            ListView<String> tagList = lookupNode(stage, "#tagListView");
            tagList.getSelectionModel().select("friends");
            refreshLayout(stage);

            assertEquals(List.of("Alice Pauline (archived)", "Benson Meier"),
                    stage.getScene().getRoot().lookupAll(".tag-member-row .event-member-name").stream()
                            .map(node -> ((Label) node).getText()).sorted().toList());
        });
    }

    @Test
    public void listArchived_showsArchivedMessagesHeaderAndCommands() throws Exception {
        LogicManager logic = createLogicWithStorage(new ModelManager(), "archived-empty");
        runInWindow(windowFor(logic, dataPath("archived-empty")), (window, stage) -> {
            stage.getScene().getRoot().applyCss();
            ListView<Person> contacts = lookupNode(stage, "#personListView");
            ListView<Event> events = lookupNode(stage, "#eventListView");
            TextField input = lookupNode(stage, "#commandTextField");
            Button submit = lookupNode(stage, "#submitButton");

            assertEquals(PersonListPanel.MESSAGE_NO_CONTACTS, placeholderText(contacts));
            assertEquals(EventListPanel.MESSAGE_NO_EVENTS, placeholderText(events));

            ToggleButton eventsView = lookupNode(stage, "#eventsView");
            Label eventCount = lookupNode(stage, "#eventCount");
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
        });
    }

    @Test
    public void openingTutorialInArchivedView_returnsToActiveView() throws Exception {
        ModelManager model = new ModelManager();
        model.addPerson(TypicalPersons.ALICE);
        model.addPerson(TypicalPersons.BENSON);
        model.setPerson(TypicalPersons.ALICE, TypicalPersons.ALICE.withArchived(true));
        model.setShowingArchived(true);
        LogicManager logic = new LogicManager(model, null);
        runInWindow(windowFor(logic), (window, stage) -> {
            window.show();
            stage.getScene().getRoot().applyCss();
            assertEquals(List.of("unarchive", "list"), visibleCommandNames(stage));

            Label tutorialLabel = lookupNode(stage, "#tutorialLabel");
            javafx.event.Event.fireEvent(tutorialLabel, createMouseClickEvent());
            stage.getScene().getRoot().applyCss();

            assertTrue(lookupNode(stage, "#tutorialOverlay").isVisible());
            assertFalse(logic.showingArchivedProperty().get());
            assertEquals(List.of(TypicalPersons.BENSON), logic.getFilteredPersonList());
            assertTrue(visibleCommandNames(stage).containsAll(List.of("add", "archive", "list")));
            assertTrue(lookupNode(stage, "#contactsCommands").isVisible());
        });
    }

    private static WindowFactory windowFor(ModelManager model) {
        return windowFor(new LogicManager(model, null));
    }

    private static WindowFactory windowFor(LogicManager logic) {
        return windowFor(logic, DEFAULT_DATA_PATH);
    }

    private static WindowFactory windowFor(LogicManager logic, Path dataPath) {
        return stage -> new MainWindow(stage, logic, dataPath);
    }

    private Path dataPath(String name) {
        return temporaryFolder.resolve(name + "-data.json");
    }

    /**
     * Returns logic backed by temporary storage files named after {@code name}, with {@code model} already saved.
     */
    private LogicManager createLogicWithStorage(ModelManager model, String name) throws IOException {
        StorageManager storage = new StorageManager(new JsonCoordiMateStorage(dataPath(name)),
                new JsonUserPrefsStorage(temporaryFolder.resolve(name + "-prefs.json")));
        storage.saveCoordiMate(model.getCoordiMate());
        return new LogicManager(model, storage);
    }

    private static void runInWindow(WindowFactory factory, WindowTest test) throws Exception {
        runInWindow(factory, test, JavaFxTestSupport.DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * Creates a main window on the JavaFX thread, fills its inner parts and runs {@code test} on it,
     * then stops the window and closes its stage even if the test fails.
     */
    private static void runInWindow(WindowFactory factory, WindowTest test, long timeoutSeconds) throws Exception {
        JavaFxTestSupport.runOnFxThreadAndWait(() -> {
            Stage stage = new Stage();
            MainWindow window = null;
            try {
                window = factory.create(stage);
                window.fillInnerParts();
                test.run(window, stage);
            } finally {
                if (window != null) {
                    window.stop();
                }
                stage.close();
            }
            return null;
        }, timeoutSeconds, TimeUnit.SECONDS);
    }

    /**
     * Returns the first node in {@code stage} matching {@code selector}, cast to the type the caller expects.
     */
    @SuppressWarnings("unchecked")
    private static <T extends Node> T lookupNode(Stage stage, String selector) {
        return (T) stage.getScene().lookup(selector);
    }

    private static String labelText(Stage stage, String selector) {
        return ((Label) lookupNode(stage, selector)).getText();
    }

    private static void refreshLayout(Stage stage) {
        stage.getScene().getRoot().applyCss();
        stage.getScene().getRoot().layout();
    }

    private static KeyEvent createKeyEvent(KeyCode keyCode) {
        return createKeyEvent(keyCode, false, false, false, false);
    }

    private static KeyEvent createKeyEvent(KeyCode keyCode, boolean shiftDown, boolean controlDown,
                                           boolean altDown, boolean metaDown) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", keyCode,
                shiftDown, controlDown, altDown, metaDown);
    }

    private static KeyEvent createKeyReleasedEvent(KeyCode keyCode) {
        return createKeyReleasedEvent(keyCode, false, false, false, false);
    }

    private static KeyEvent createKeyReleasedEvent(KeyCode keyCode, boolean shiftDown, boolean controlDown,
                                                   boolean altDown, boolean metaDown) {
        return new KeyEvent(KeyEvent.KEY_RELEASED, "", "", keyCode,
                shiftDown, controlDown, altDown, metaDown);
    }

    private static KeyEvent createTypedKeyEvent(String character) {
        return new KeyEvent(KeyEvent.KEY_TYPED, character, character, KeyCode.UNDEFINED,
                false, false, false, false);
    }

    private static MouseEvent createMouseClickEvent() {
        return new MouseEvent(MouseEvent.MOUSE_CLICKED,
                0, 0, 0, 0, MouseButton.PRIMARY, 1,
                false, false, false, false, false, false, false, false, false, false, null);
    }

    private static boolean keyReachesTarget(Node target, KeyEvent event) {
        AtomicBoolean hasReachedTarget = new AtomicBoolean();
        EventHandler<KeyEvent> observer = ignored -> hasReachedTarget.set(true);
        target.addEventHandler(event.getEventType(), observer);
        target.fireEvent(event.copyFor(target, target));
        target.removeEventHandler(event.getEventType(), observer);
        return hasReachedTarget.get();
    }

    private static void fireMousePress(Node target) {
        MouseEvent event = new MouseEvent(MouseEvent.MOUSE_PRESSED, 0, 0, 0, 0, MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, true, false, false,
                new PickResult(target, 0, 0));
        target.fireEvent(event.copyFor(target, target));
    }

    private static String getPrefixHelpText(TextFlow prefixHelp) {
        return ((Text) prefixHelp.getChildren().getFirst()).getText();
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
        return lookupNode(stage, commandListId).lookupAll(".cheat-name").stream()
                .map(node -> ((Label) node).getText()).toList();
    }
}
