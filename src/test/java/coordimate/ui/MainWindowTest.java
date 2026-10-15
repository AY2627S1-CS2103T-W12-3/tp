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

import coordimate.logic.LogicManager;
import coordimate.model.ModelManager;
import coordimate.model.person.Person;
import coordimate.testutil.PersonBuilder;
import coordimate.testutil.TypicalPersons;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * Checks that the complete desktop shell loads and its view controls stay connected.
 */
public class MainWindowTest {

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
            Label prefixHelp = (Label) stage.getScene().getRoot().lookup("#prefixHelp");
            ToggleButton events = (ToggleButton) stage.getScene().getRoot().lookup("#eventsView");
            ToggleButton tags = (ToggleButton) stage.getScene().getRoot().lookup("#tagsView");
            ToggleButton contacts = (ToggleButton) stage.getScene().getRoot().lookup("#contactsView");

            events.fire();
            stage.getScene().getRoot().applyCss();
            assertEquals("Events", views.getSelectionModel().getSelectedItem().getText().split(" ")[0]);
            assertEquals("Bold", events.getFont().getStyle());
            assertTrue(prefixHelp.getText().contains("evn/Event"));

            tags.fire();
            stage.getScene().getRoot().applyCss();
            assertEquals("Tags", views.getSelectionModel().getSelectedItem().getText());
            assertEquals("Bold", tags.getFont().getStyle());
            assertEquals("Regular", events.getFont().getStyle());
            assertTrue(prefixHelp.getText().contains("t/Tag"));
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
            assertTrue(prefixHelp.getText().contains("n/Name"));
            stage.close();
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

            stage.close();
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
            Label prefixHelp = (Label) stage.getScene().getRoot().lookup("#prefixHelp");
            assertEquals("add", commandInput.getText());
            assertTrue(prefixHelp.getText().startsWith("Format: add n/NAME p/PHONE e/EMAIL r/ROLE"));
            assertTrue(prefixHelp.getText().contains("[t/TAG]... [m/NOTE]"));
            assertTrue(prefixHelp.getText().contains("Example: add n/Aisha Tan"));

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
            assertEquals(1, stage.getScene().getRoot().lookupAll(".event-member-tag-teal").size());
            assertEquals(1, stage.getScene().getRoot().lookupAll(".event-member-tag-amber").size());
            assertEquals(2, stage.getScene().getRoot().lookupAll(".event-member-tag-purple").size());
            assertEquals(3, stage.getScene().getRoot().lookupAll(".event-member-tag-blue").size());
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
}
