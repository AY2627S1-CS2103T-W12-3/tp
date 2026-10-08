package coordimate.ui;

import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BENSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import coordimate.model.person.Person;
import coordimate.testutil.PersonBuilder;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;

/**
 * Tests the contact list's selection and details states on the JavaFX application thread.
 */
public class PersonListPanelTest {

    @BeforeAll
    public static void startJavaFx() throws InterruptedException {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "JavaFX UI tests require a display");
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        assertTrue(started.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void selectContact_showsItsDetails() throws Exception {
        runOnJavaFxThread(() -> {
            ObservableList<Person> persons = FXCollections.observableArrayList(ALICE, BENSON);
            Region root = new PersonListPanel(persons).getRoot();
            new Scene(root);
            root.applyCss();
            ListView<Person> list = getList(root);

            assertTrue(getHint(root).isVisible());
            assertFalse(getDetails(root).isVisible());
            assertEquals("Records: 2", getLabel(root, "contactCount").getText());

            list.getSelectionModel().select(BENSON);

            assertFalse(getHint(root).isVisible());
            assertTrue(getDetails(root).isVisible());
            assertEquals("Benson Meier", getLabel(root, "selectedName").getText());
            assertEquals("Index: 2", getLabel(root, "selectedIndex").getText());
            assertEquals("98765432", getLabel(root, "phone").getText());
            assertEquals("johnd@example.com", getLabel(root, "email").getText());
            assertEquals("NA", getLabel(root, "role").getText());
            assertEquals("311, Clementi Ave 2, #02-25", getLabel(root, "address").getText());
            FlowPane tags = (FlowPane) root.lookup("#detailTags");
            assertEquals(2, tags.getChildren().size());
        });
    }

    @Test
    public void contactWithAllDetails_showsSavedValues() throws Exception {
        runOnJavaFxThread(() -> {
            Person contact = new PersonBuilder(ALICE).withRole("Logistics Lead")
                    .withBirthday("18-06-2004").withAddress("21 Kent Ridge Road, #03-12")
                    .withOrganisation("NUS Student Affairs").withNote("Handles venue bookings")
                    .withTags("EXCO", "Logistics").build();
            Region root = new PersonListPanel(FXCollections.observableArrayList(contact)).getRoot();
            new Scene(root);
            root.applyCss();
            getList(root).getSelectionModel().select(contact);

            assertEquals("Logistics Lead", getLabel(root, "role").getText());
            assertEquals("18-06-2004", getLabel(root, "birthday").getText());
            assertEquals("21 Kent Ridge Road, #03-12", getLabel(root, "address").getText());
            assertEquals("NUS Student Affairs", getLabel(root, "organisation").getText());
            assertEquals("Handles venue bookings", getLabel(root, "note").getText());
            assertTrue(getLabel(root, "note").isWrapText());
            FlowPane tags = (FlowPane) root.lookup("#detailTags");
            assertEquals(2, tags.getChildren().size());
        });
    }

    @Test
    public void contactWithoutOptionalFields_showsPlaceholdersOnlyInDetails() throws Exception {
        runOnJavaFxThread(() -> {
            Person contact = new PersonBuilder(ALICE).withoutAddress().withTags().build();
            Region root = new PersonListPanel(FXCollections.observableArrayList(contact)).getRoot();
            new Scene(root);
            root.applyCss();
            getList(root).getSelectionModel().select(contact);

            assertEquals("— (not specified)", getLabel(root, "birthday").getText());
            assertEquals("— (not specified)", getLabel(root, "address").getText());
            assertEquals("— (not specified)", getLabel(root, "organisation").getText());
            assertEquals("— (not specified)", getLabel(root, "note").getText());
            assertTrue(((FlowPane) root.lookup("#detailTags")).getChildren().isEmpty());

            Region card = new PersonCard(contact, 1).getRoot();
            new Scene(card);
            card.applyCss();
            Label cardAddress = (Label) card.lookup("#address");
            assertFalse(cardAddress.isVisible());
            assertFalse(cardAddress.isManaged());
        });
    }

    @Test
    public void resize_narrowWindowStacksContactPanes() throws Exception {
        runOnJavaFxThread(() -> {
            ObservableList<Person> persons = FXCollections.observableArrayList(ALICE);
            SplitPane pane = (SplitPane) new PersonListPanel(persons).getRoot();
            new Scene(pane);

            pane.resize(700, 500);
            assertEquals(Orientation.HORIZONTAL, pane.getOrientation());

            pane.resize(500, 500);
            assertEquals(Orientation.VERTICAL, pane.getOrientation());
        });
    }

    @Test
    public void filterContacts_keepsVisibleSelectionAndClearsHiddenSelection() throws Exception {
        PanelFixture fixture = callOnJavaFxThread(() -> {
            ObservableList<Person> persons = FXCollections.observableArrayList(ALICE, BENSON);
            FilteredList<Person> displayedPersons = new FilteredList<>(persons);
            Region root = new PersonListPanel(displayedPersons).getRoot();
            new Scene(root);
            root.applyCss();
            ListView<Person> list = getList(root);
            list.getSelectionModel().select(BENSON);
            return new PanelFixture(persons, displayedPersons, root, list);
        });

        runOnJavaFxThread(() -> fixture.displayedPersons().setPredicate(person -> person != ALICE));

        runOnJavaFxThread(() -> {
            assertEquals(BENSON, fixture.list().getSelectionModel().getSelectedItem());
            assertEquals("Benson Meier", getLabel(fixture.root(), "selectedName").getText());
            assertEquals("Index: 1", getLabel(fixture.root(), "selectedIndex").getText());
            assertEquals("Records: 1", getLabel(fixture.root(), "contactCount").getText());
        });

        runOnJavaFxThread(() -> fixture.displayedPersons().setPredicate(person -> person == ALICE));

        runOnJavaFxThread(() -> {
            assertNull(fixture.list().getSelectionModel().getSelectedItem());
            assertTrue(getHint(fixture.root()).isVisible());
            assertFalse(getDetails(fixture.root()).isVisible());
        });

        runOnJavaFxThread(() -> fixture.persons().clear());

        runOnJavaFxThread(() -> assertEquals("Records: 0", getLabel(fixture.root(), "contactCount").getText()));
    }

    @Test
    public void replaceSelectedContact_showsUpdatedDetails() throws Exception {
        PanelFixture fixture = callOnJavaFxThread(() -> {
            ObservableList<Person> persons = FXCollections.observableArrayList(ALICE, BENSON);
            FilteredList<Person> displayedPersons = new FilteredList<>(persons);
            Region root = new PersonListPanel(displayedPersons).getRoot();
            new Scene(root);
            root.applyCss();
            ListView<Person> list = getList(root);
            list.getSelectionModel().select(BENSON);
            return new PanelFixture(persons, displayedPersons, root, list);
        });
        Person updatedBenson = new PersonBuilder(BENSON).withName("Benson Tan")
                .withRole("President").withoutAddress().withNote("Updated note")
                .withTags().build();

        runOnJavaFxThread(() -> fixture.persons().setAll(ALICE, updatedBenson));

        runOnJavaFxThread(() -> {
            assertEquals(updatedBenson, fixture.list().getSelectionModel().getSelectedItem());
            assertEquals("Benson Tan", getLabel(fixture.root(), "selectedName").getText());
            assertEquals("Index: 2", getLabel(fixture.root(), "selectedIndex").getText());
            assertEquals("President", getLabel(fixture.root(), "role").getText());
            assertEquals("Updated note", getLabel(fixture.root(), "note").getText());
            assertEquals("— (not specified)", getLabel(fixture.root(), "address").getText());
            assertTrue(((FlowPane) fixture.root().lookup("#detailTags")).getChildren().isEmpty());
        });
    }

    @Test
    public void removeSelectedContact_clearsSelectionInsteadOfSelectingNextContact() throws Exception {
        PanelFixture fixture = callOnJavaFxThread(() -> {
            ObservableList<Person> persons = FXCollections.observableArrayList(ALICE, BENSON);
            FilteredList<Person> displayedPersons = new FilteredList<>(persons);
            Region root = new PersonListPanel(displayedPersons).getRoot();
            new Scene(root);
            root.applyCss();
            ListView<Person> list = getList(root);
            list.getSelectionModel().select(ALICE);
            return new PanelFixture(persons, displayedPersons, root, list);
        });

        runOnJavaFxThread(() -> fixture.persons().setAll(BENSON));

        runOnJavaFxThread(() -> {
            assertNull(fixture.list().getSelectionModel().getSelectedItem());
            assertTrue(getHint(fixture.root()).isVisible());
        });
    }

    private static ListView<Person> getList(Region root) {
        @SuppressWarnings("unchecked")
        ListView<Person> list = (ListView<Person>) root.lookup("#personListView");
        return list;
    }

    private static Label getLabel(Region root, String id) {
        return (Label) root.lookup("#" + id);
    }

    private static Label getHint(Region root) {
        return getLabel(root, "selectionHint");
    }

    private static ScrollPane getDetails(Region root) {
        return (ScrollPane) root.lookup("#detailsScroll");
    }

    private static void runOnJavaFxThread(Runnable assertion) throws Exception {
        callOnJavaFxThread(() -> {
            assertion.run();
            return null;
        });
    }

    private static <T> T callOnJavaFxThread(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }

    private record PanelFixture(ObservableList<Person> persons, FilteredList<Person> displayedPersons,
                                Region root, ListView<Person> list) {}
}
