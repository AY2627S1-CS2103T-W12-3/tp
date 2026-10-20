package coordimate.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import coordimate.commons.core.GuiSettings;
import coordimate.logic.LogicManager;
import coordimate.model.ModelManager;
import coordimate.model.person.Person;
import coordimate.storage.JsonCoordiMateStorage;
import coordimate.storage.JsonUserPrefsStorage;
import coordimate.storage.StorageManager;
import coordimate.testutil.PersonBuilder;
import coordimate.testutil.TypicalPersons;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Screen;
import javafx.stage.Stage;
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
        task.get(10, TimeUnit.SECONDS);
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
            KeyEvent enter = keyEvent(KeyCode.ENTER);
            addRow.getOnKeyPressed().handle(enter);
            assertTrue(enter.isConsumed());
            assertEquals("add", commandInput.getText());

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
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", keyCode,
                false, false, false, false);
    }

    private static String getPrefixHelpText(TextFlow prefixHelp) {
        return ((Text) prefixHelp.getChildren().getFirst()).getText();
    }
}
