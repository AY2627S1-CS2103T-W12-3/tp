package coordimate.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
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
            assertEquals("Events", views.getSelectionModel().getSelectedItem().getText().split(" ")[0]);
            assertTrue(prefixHelp.getText().contains("evn/Event"));

            tags.fire();
            assertEquals("Tags", views.getSelectionModel().getSelectedItem().getText());
            assertTrue(prefixHelp.getText().contains("t/Tag"));
            ListView<?> tagList = (ListView<?>) stage.getScene().getRoot().lookup("#tagListView");
            tagList.getSelectionModel().selectFirst();
            assertEquals("EXCO", ((Label) stage.getScene().getRoot().lookup("#selectedTagName")).getText());
            ListView<?> tagMembers = (ListView<?>) stage.getScene().getRoot().lookup("#tagMemberListView");
            assertEquals(List.of("Alice Pauline"), tagMembers.getItems().stream()
                    .map(person -> ((Person) person).getName().getFullName()).toList());
            assertEquals("Members (1)", ((Label) stage.getScene().getRoot().lookup("#tagMemberCount")).getText());

            contacts.fire();
            assertTrue(views.getSelectionModel().getSelectedItem().getText().startsWith("Contacts"));
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
}
