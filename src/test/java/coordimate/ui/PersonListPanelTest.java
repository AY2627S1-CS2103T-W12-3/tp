package coordimate.ui;

import static coordimate.testutil.TypicalPersons.ALICE;
import static coordimate.testutil.TypicalPersons.BENSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import coordimate.model.event.AttendanceStatus;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.person.Person;
import coordimate.testutil.PersonBuilder;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 * Tests the contact list's selection and details states on the JavaFX application thread.
 */
public class PersonListPanelTest {

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
    public void memberIds_onCardsAndDetails_remainStableAcrossFiltering() throws Exception {
        PanelFixture fixture = callOnJavaFxThread(PersonListPanelTest::createIdFixture);
        runOnJavaFxThread(() -> {
            assertEquals(List.of("ID: 12", "ID: 45"), cardIds(fixture.root()));
            assertEquals("ID: 45", getLabel(fixture.root(), "selectedMemberId").getText());
            fixture.displayedPersons().setPredicate(person -> person.getMemberId().orElseThrow().getValue() == 45);
        });
        runOnJavaFxThread(() -> {
            assertEquals(List.of("ID: 45"), cardIds(fixture.root()));
            assertEquals("ID: 45", getLabel(fixture.root(), "selectedMemberId").getText());
            assertEquals(0, fixture.list().getSelectionModel().getSelectedIndex());
            fixture.displayedPersons().setPredicate(person -> true);
        });
        runOnJavaFxThread(() -> {
            assertEquals(List.of("ID: 12", "ID: 45"), cardIds(fixture.root()));
            assertEquals("ID: 45", getLabel(fixture.root(), "selectedMemberId").getText());
            assertEquals(1, fixture.list().getSelectionModel().getSelectedIndex());
        });
    }

    @Test
    public void bulkEditAndReorder_selectionFollowsIdAndClearsIfIdIsRemoved() throws Exception {
        PanelFixture fixture = callOnJavaFxThread(PersonListPanelTest::createIdFixture);
        Person alice = fixture.persons().getFirst();
        Person editedBenson = new PersonBuilder(fixture.persons().getLast()).withName("Benson Tan")
                .withRole("President").build();
        runOnJavaFxThread(() -> fixture.persons().setAll(editedBenson, alice));
        runOnJavaFxThread(() -> {
            assertEquals(editedBenson, fixture.list().getSelectionModel().getSelectedItem());
            assertEquals("Benson Tan", getLabel(fixture.root(), "selectedName").getText());
            assertEquals("ID: 45", getLabel(fixture.root(), "selectedMemberId").getText());
            assertEquals(List.of("ID: 12", "ID: 45"), cardIds(fixture.root()));
        });

        Person replacement = new PersonBuilder(editedBenson).withMemberId(99).withName("Replacement").build();
        runOnJavaFxThread(() -> fixture.persons().setAll(replacement, alice));
        runOnJavaFxThread(() -> {
            assertNull(fixture.list().getSelectionModel().getSelectedItem());
            assertEquals("", getLabel(fixture.root(), "selectedMemberId").getText());
            assertFalse(getDetails(fixture.root()).isVisible());
            assertEquals(List.of("ID: 12", "ID: 99"), cardIds(fixture.root()));
        });
    }

    @Test
    public void maximumId_onCardAndDetails_isFullyVisibleAtNarrowAndWideWidths() throws Exception {
        runOnJavaFxThread(() -> {
            Person member = new PersonBuilder(ALICE).withMemberId(Integer.MAX_VALUE)
                    .withName("Alexandria Catherine Montgomery").withTags("EXCO", "Logistics").build();
            Region card = new PersonCard(member).getRoot();
            Scene cardScene = new Scene(card, 300, 300);
            cardScene.getStylesheets().add(getTheme());
            card.applyCss();
            card.layout();
            assertFullId(getLabel(card, "id"), "ID: 2147483647");

            Region panel = new PersonListPanel(FXCollections.observableArrayList(member)).getRoot();
            StackPane container = new StackPane(panel);
            Scene scene = new Scene(container, 1100, 700);
            scene.getStylesheets().add(getTheme());
            container.applyCss();
            container.layout();
            getList(panel).getSelectionModel().selectFirst();
            for (int width : new int[] {1100, 560, 1100}) {
                container.resize(width, 700);
                container.applyCss();
                container.layout();
                Label id = getLabel(panel, "selectedMemberId");
                assertFullId(id, "ID: 2147483647");
                var bounds = id.localToScene(id.getLayoutBounds());
                var viewport = getDetails(panel).localToScene(getDetails(panel).getLayoutBounds());
                assertTrue(bounds.getMinX() >= viewport.getMinX());
                assertTrue(bounds.getMaxX() <= viewport.getMaxX());
            }
        });
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
            assertEquals("ID: 2", getLabel(root, "selectedMemberId").getText());
            assertEquals("98765432", getLabel(root, "phone").getText());
            assertEquals("johnd@example.com", getLabel(root, "email").getText());
            assertEquals("NA", getLabel(root, "role").getText());
            assertEquals("311, Clementi Ave 2, #02-25", getLabel(root, "address").getText());
            FlowPane tags = (FlowPane) root.lookup("#detailTags");
            assertEquals(2, tags.getChildren().size());
        });
    }

    @Test
    public void contactOnLeave_showsLeaveOnCardAndInDetails() throws Exception {
        String future = LocalDate.now().plusDays(30).format(DateTimeFormatter.ofPattern("dd-MM-uuuu"));
        runOnJavaFxThread(() -> {
            Person contact = new PersonBuilder(ALICE).withLeaveDate(future).build();
            Region root = new PersonListPanel(FXCollections.observableArrayList(contact)).getRoot();
            new Scene(root);
            root.applyCss();
            getList(root).getSelectionModel().select(contact);

            assertEquals(future, getLabel(root, "leave").getText());

            Region card = new PersonCard(contact).getRoot();
            new Scene(card);
            card.applyCss();
            Label cardLeave = (Label) card.lookup("#leave");
            assertTrue(cardLeave.isVisible());
            assertTrue(cardLeave.isManaged());
            assertEquals("On leave until " + future, cardLeave.getText());
        });
    }

    @Test
    public void contactWithEndedOrNoLeave_showsNotOnLeave() throws Exception {
        runOnJavaFxThread(() -> {
            for (Person contact : List.of(new PersonBuilder(ALICE).withLeaveDate("01-01-2020").build(), BENSON)) {
                Region root = new PersonListPanel(FXCollections.observableArrayList(contact)).getRoot();
                new Scene(root);
                root.applyCss();
                getList(root).getSelectionModel().select(contact);

                assertEquals("— (not on leave)", getLabel(root, "leave").getText());

                Region card = new PersonCard(contact).getRoot();
                new Scene(card);
                card.applyCss();
                Label cardLeave = (Label) card.lookup("#leave");
                assertFalse(cardLeave.isVisible());
                assertFalse(cardLeave.isManaged());
            }
        });
    }

    @Test
    public void refreshIfDateChanged_leaveEnded_updatesCardAndDetails() throws Exception {
        MutableClock clock = new MutableClock(LocalDate.of(2026, 11, 30));
        runOnJavaFxThread(() -> {
            Person contact = new PersonBuilder(ALICE).withLeaveDate("30-11-2026").build();
            PersonListPanel panel = new PersonListPanel(FXCollections.observableArrayList(contact),
                    FXCollections.observableArrayList(), clock);
            Region root = panel.getRoot();
            new Scene(root, 1100, 700);
            root.applyCss();
            getList(root).getSelectionModel().select(contact);

            assertEquals(List.of("On leave until 30-11-2026"), visibleLeaveBadges(root));
            assertEquals("30-11-2026", getDetailsLeave(root).getText());

            panel.refreshIfDateChanged();
            assertEquals(List.of("On leave until 30-11-2026"), visibleLeaveBadges(root));

            clock.setDate(LocalDate.of(2026, 12, 1));
            panel.refreshIfDateChanged();

            assertEquals(List.of(), visibleLeaveBadges(root));
            assertEquals("— (not on leave)", getDetailsLeave(root).getText());
            assertEquals(contact, getList(root).getSelectionModel().getSelectedItem());
        });
    }

    @Test
    public void navigateCards_handlesUnselectedEmptyAndBoundaryStates() throws Exception {
        runOnJavaFxThread(() -> {
            PersonListPanel panel = new PersonListPanel(FXCollections.observableArrayList(ALICE, BENSON));
            Region root = panel.getRoot();
            new Scene(root);
            root.applyCss();
            ListView<Person> list = getList(root);

            assertNull(list.getSelectionModel().getSelectedItem());
            assertTrue(panel.selectPreviousCard());
            assertEquals(BENSON, list.getSelectionModel().getSelectedItem());
            assertTrue(panel.selectPreviousCard());
            assertEquals(ALICE, list.getSelectionModel().getSelectedItem());
            assertTrue(panel.selectPreviousCard());
            assertEquals(ALICE, list.getSelectionModel().getSelectedItem());

            list.getSelectionModel().clearSelection();
            assertTrue(panel.selectNextCard());
            assertEquals(ALICE, list.getSelectionModel().getSelectedItem());
            assertTrue(panel.selectNextCard());
            assertEquals(BENSON, list.getSelectionModel().getSelectedItem());
            assertTrue(panel.selectNextCard());
            assertEquals(BENSON, list.getSelectionModel().getSelectedItem());

            PersonListPanel emptyPanel = new PersonListPanel(FXCollections.observableArrayList());
            assertFalse(emptyPanel.selectNextCard());
            assertFalse(emptyPanel.selectPreviousCard());
        });
    }

    @Test
    public void tutorialContactCardTarget_usesVisibleSelectedCard() throws Exception {
        runOnJavaFxThread(() -> {
            PersonListPanel panel = new PersonListPanel(FXCollections.observableArrayList(ALICE, BENSON));
            Stage stage = new Stage();
            stage.setScene(new Scene(panel.getRoot(), 1000, 700));
            stage.show();
            try {
                Region root = panel.getRoot();
                root.applyCss();
                root.layout();
                ListView<Person> list = getList(root);

                ListCell<?> firstCard = assertInstanceOf(ListCell.class, panel.getTutorialContactCardTarget());
                assertEquals(0, firstCard.getIndex());
                assertEquals(ALICE, list.getSelectionModel().getSelectedItem());

                list.getSelectionModel().select(BENSON);
                ListCell<?> selectedCard = assertInstanceOf(ListCell.class, panel.getTutorialContactCardTarget());
                assertEquals(1, selectedCard.getIndex());
                assertEquals(BENSON, list.getSelectionModel().getSelectedItem());
            } finally {
                stage.close();
            }
        });
    }

    @Test
    public void selectContact_revealsContactAndShowsItsDetails() throws Exception {
        runOnJavaFxThread(() -> {
            PersonListPanel panel = new PersonListPanel(FXCollections.observableArrayList(ALICE, BENSON));
            Region root = panel.getRoot();
            new Scene(root);
            root.applyCss();

            assertTrue(panel.selectContact(BENSON));

            assertEquals(BENSON, getList(root).getSelectionModel().getSelectedItem());
            assertEquals("Benson Meier", getLabel(root, "selectedName").getText());
            assertEquals("ID: 2", getLabel(root, "selectedMemberId").getText());
            assertTrue(getDetails(root).isVisible());
            assertFalse(getHint(root).isVisible());
        });
    }

    @Test
    public void selectContact_whenContactIsNotDisplayed_keepsCurrentSelection() throws Exception {
        runOnJavaFxThread(() -> {
            PersonListPanel panel = new PersonListPanel(FXCollections.observableArrayList(BENSON));
            Region root = panel.getRoot();
            new Scene(root);
            root.applyCss();
            ListView<Person> list = getList(root);

            assertTrue(panel.selectContact(BENSON));
            assertFalse(panel.selectContact(ALICE));
            assertFalse(panel.selectContactAt(-1));

            assertEquals(BENSON, list.getSelectionModel().getSelectedItem());
            assertEquals("Benson Meier", getLabel(root, "selectedName").getText());
        });
    }

    @Test
    public void selectContactAt_afterDeletionSelectsNearestRemainingContact() throws Exception {
        runOnJavaFxThread(() -> {
            ObservableList<Person> persons = FXCollections.observableArrayList(ALICE, BENSON);
            PersonListPanel panel = new PersonListPanel(persons);
            Region root = panel.getRoot();
            new Scene(root);
            root.applyCss();
            ListView<Person> list = getList(root);

            persons.remove(BENSON);
            assertTrue(panel.selectContactAt(1));
            assertEquals(ALICE, list.getSelectionModel().getSelectedItem());
            assertEquals("Alice Pauline", getLabel(root, "selectedName").getText());

            persons.clear();
            assertFalse(panel.selectContactAt(0));
            assertNull(list.getSelectionModel().getSelectedItem());
            assertTrue(getHint(root).isVisible());
        });
    }

    @Test
    public void explicitFocusAfterListChange_supersedesDeferredSelectionRestore() throws Exception {
        FocusFixture fixture = callOnJavaFxThread(() -> {
            ObservableList<Person> persons = FXCollections.observableArrayList(ALICE, BENSON);
            PersonListPanel panel = new PersonListPanel(persons);
            Region root = panel.getRoot();
            new Scene(root);
            root.applyCss();
            ListView<Person> list = getList(root);
            list.getSelectionModel().select(ALICE);
            return new FocusFixture(persons, panel, root, list);
        });

        runOnJavaFxThread(() -> {
            fixture.persons().setAll(ALICE, BENSON);
            assertTrue(fixture.panel().selectContact(BENSON));
        });

        runOnJavaFxThread(() -> {
            assertEquals(BENSON, fixture.list().getSelectionModel().getSelectedItem());
            assertEquals("Benson Meier", getLabel(fixture.root(), "selectedName").getText());
        });
    }

    @Test
    public void contactWithAllDetails_showsSavedValues() throws Exception {
        runOnJavaFxThread(() -> {
            Person contact = new PersonBuilder(ALICE).withRole("Logistics Lead")
                    .withBirthday("18-06-2004").withAddress("21 Kent Ridge Road, #03-12")
                    .withOrganisation("NUS Student Affairs").withNote("Handles venue bookings")
                    .withTags("EXCO", "Logistics", "Publicity", "Sponsor", "UniversityStaff").build();
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
            assertEquals(5, tags.getChildren().size());
            assertTagColor(tags, "EXCO", "tag-chip-red");
            assertTagColor(tags, "Logistics", "tag-chip-teal");
            assertTagColor(tags, "Publicity", "tag-chip-blue");
            assertTagColor(tags, "Sponsor", "tag-chip-amber");
            assertTagColor(tags, "UniversityStaff", "tag-chip-purple");

            Region card = new PersonCard(contact).getRoot();
            new Scene(card);
            card.applyCss();
            assertTagColor((FlowPane) card.lookup("#tags"), "Publicity", "tag-chip-blue");
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
            assertEquals("0 events", getLabel(root, "assignedEventCount").getText());

            Region card = new PersonCard(contact).getRoot();
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
            assertContactLayoutAtWidth(900, Orientation.HORIZONTAL);
            assertContactLayoutAtWidth(700, Orientation.VERTICAL);
            assertContactLayoutAtWidth(760, Orientation.VERTICAL);
            assertContactLayoutAtWidth(762, Orientation.HORIZONTAL);
        });
    }

    private static void assertContactLayoutAtWidth(double width, Orientation expectedOrientation) {
        SplitPane pane = (SplitPane) new PersonListPanel(FXCollections.observableArrayList(ALICE)).getRoot();
        StackPane container = new StackPane(pane);
        new Scene(container, width, 500);
        container.applyCss();
        container.layout();

        assertEquals(width, pane.getWidth());
        assertEquals(expectedOrientation, pane.getOrientation());
        assertEquals(0, pane.getMinWidth());
        assertEquals(300, ((Region) pane.getItems().get(0)).getMinWidth());
        assertEquals(460, ((Region) pane.getItems().get(1)).getMinWidth());
    }

    @Test
    public void assignedEvents_updateWithAttendanceWithoutChangingSelection() throws Exception {
        runOnJavaFxThread(() -> {
            EventTime time = new EventTime("25-10-2026");
            Event event = new Event("Final Concert", time, time, List.of(ALICE.getName()), Map.of());
            ObservableList<Event> events = FXCollections.observableArrayList(event);
            Region root = new PersonListPanel(FXCollections.observableArrayList(ALICE), events).getRoot();
            new Scene(root);
            root.applyCss();
            getList(root).getSelectionModel().select(ALICE);

            assertEquals("AP", getLabel(root, "selectedAvatar").getText());
            assertEquals("1 event", getLabel(root, "assignedEventCount").getText());
            VBox assigned = (VBox) root.lookup("#assignedEvents");
            assertEquals(1, assigned.getChildren().size());
            HBox row = (HBox) assigned.getChildren().getFirst();
            assertEquals("Not marked", ((Label) row.getChildren().getLast()).getText());

            events.setAll(event.withAttendance(ALICE.getName(), AttendanceStatus.PRESENT));
            assertEquals(ALICE, getList(root).getSelectionModel().getSelectedItem());
            assertEquals("1 event", getLabel(root, "assignedEventCount").getText());
            HBox updatedRow = (HBox) assigned.getChildren().getFirst();
            assertEquals("Present", ((Label) updatedRow.getChildren().getLast()).getText());

            events.setAll(event.withAttendance(ALICE.getName(), AttendanceStatus.ABSENT));
            HBox absentRow = (HBox) assigned.getChildren().getFirst();
            Label absentStatus = (Label) absentRow.getChildren().getLast();
            assertEquals("Absent", absentStatus.getText());
            assertTrue(absentStatus.getStyleClass().contains("attendance-absent"));

            events.clear();
            assertEquals("0 events", getLabel(root, "assignedEventCount").getText());
            assertEquals("No events assigned.", ((Label) assigned.getChildren().getFirst()).getText());
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
            assertEquals("ID: 2", getLabel(fixture.root(), "selectedMemberId").getText());
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
            assertEquals("ID: 2", getLabel(fixture.root(), "selectedMemberId").getText());
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

    private static PanelFixture createIdFixture() {
        Person alice = new PersonBuilder(ALICE).withMemberId(12).build();
        Person benson = new PersonBuilder(BENSON).withMemberId(45).build();
        ObservableList<Person> persons = FXCollections.observableArrayList(alice, benson);
        FilteredList<Person> displayed = new FilteredList<>(persons);
        Region root = new PersonListPanel(displayed).getRoot();
        Scene scene = new Scene(root, 1100, 700);
        scene.getStylesheets().add(getTheme());
        root.applyCss();
        root.layout();
        ListView<Person> list = getList(root);
        list.getSelectionModel().select(benson);
        return new PanelFixture(persons, displayed, root, list);
    }

    private static List<String> cardIds(Region root) {
        root.applyCss();
        root.layout();
        return root.lookupAll(".card-id").stream().map(node -> ((Label) node).getText()).sorted().toList();
    }

    private static void assertFullId(Label label, String expected) {
        assertEquals(expected, label.getText());
        assertEquals(expected, ((Text) label.lookup(".text")).getText());
        assertTrue(label.getWidth() + 1 >= label.prefWidth(-1));
    }

    private static String getTheme() {
        return PersonListPanelTest.class.getResource("/view/DarkTheme.css").toExternalForm();
    }

    private static ListView<Person> getList(Region root) {
        @SuppressWarnings("unchecked")
        ListView<Person> list = (ListView<Person>) root.lookup("#personListView");
        return list;
    }

    private static Label getLabel(Region root, String id) {
        return (Label) root.lookup("#" + id);
    }

    private static List<String> visibleLeaveBadges(Region root) {
        root.applyCss();
        root.layout();
        return root.lookupAll(".leave-badge").stream()
                .filter(node -> node.isVisible())
                .map(node -> ((Label) node).getText())
                .toList();
    }

    private static Label getDetailsLeave(Region root) {
        return (Label) root.lookupAll("#leave").stream()
                .filter(node -> node.getStyleClass().contains("detail-value"))
                .findFirst().orElseThrow();
    }

    private static void assertTagColor(FlowPane tagPane, String tagName, String colorClass) {
        assertTrue(tagPane.getChildren().stream()
                .filter(Label.class::isInstance)
                .map(Label.class::cast)
                .filter(label -> label.getText().equals(tagName))
                .anyMatch(label -> label.getStyleClass().contains(colorClass)),
                tagName + " should use " + colorClass);
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

    private record FocusFixture(ObservableList<Person> persons, PersonListPanel panel, Region root,
                                ListView<Person> list) {}

    /** A clock whose date can be changed during a test. */
    private static class MutableClock extends Clock {
        private Instant current;

        MutableClock(LocalDate date) {
            setDate(date);
        }

        void setDate(LocalDate date) {
            current = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(current, zone);
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
