package coordimate.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import coordimate.model.event.AttendanceStatus;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.person.Name;
import coordimate.model.person.Person;
import coordimate.testutil.PersonBuilder;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
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
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 * Tests attendance in the selected event's details on the JavaFX application thread.
 */
public class EventListPanelTest {
    private static final Name ALICE = new Name("Alice Tan");
    private static final Name BOB = new Name("Bob Lim");
    private static final Name CHARLIE = new Name("Charlie Ng");
    private static final EventTime DATE = new EventTime("25-10-2026");

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
    public void selectEvent_showsEveryMemberStatusAndSummary() throws Exception {
        runOnJavaFxThread(() -> {
            Event event = event(List.of(ALICE, BOB, CHARLIE),
                    Map.of(ALICE, AttendanceStatus.PRESENT, BOB, AttendanceStatus.ABSENT));
            Region root = new EventListPanel(FXCollections.observableArrayList(event)).getRoot();
            new Scene(root);
            root.applyCss();
            getEventList(root).getSelectionModel().selectFirst();

            assertEquals("Members (3)", getLabel(root, "memberCount").getText());
            assertCounts(root, "1 Present", "1 Absent", "1 Not marked");
            assertMembers(root, "Alice Tan", "● present", "Bob Lim", "● absent", "Charlie Ng", "● unmarked");
        });
    }

    @Test
    public void navigateCards_handlesUnselectedEmptyAndBoundaryStates() throws Exception {
        runOnJavaFxThread(() -> {
            Event first = event(List.of(ALICE), Map.of());
            Event second = new Event("Planning Session", DATE, DATE, List.of(), Map.of());
            EventListPanel panel = new EventListPanel(FXCollections.observableArrayList(first, second));
            Region root = panel.getRoot();
            new Scene(root);
            root.applyCss();
            ListView<Event> list = getEventList(root);

            assertNull(list.getSelectionModel().getSelectedItem());
            assertFalse(((VBox) root.lookup("#eventDetails")).isVisible());
            assertTrue(panel.selectPreviousCard());
            assertEquals(second, list.getSelectionModel().getSelectedItem());
            assertTrue(panel.selectPreviousCard());
            assertEquals(first, list.getSelectionModel().getSelectedItem());
            assertTrue(panel.selectPreviousCard());
            assertEquals(first, list.getSelectionModel().getSelectedItem());

            list.getSelectionModel().clearSelection();
            assertTrue(panel.selectNextCard());
            assertEquals(first, list.getSelectionModel().getSelectedItem());
            assertTrue(panel.selectNextCard());
            assertEquals(second, list.getSelectionModel().getSelectedItem());
            assertTrue(panel.selectNextCard());
            assertEquals(second, list.getSelectionModel().getSelectedItem());

            EventListPanel emptyPanel = new EventListPanel(FXCollections.observableArrayList());
            assertFalse(emptyPanel.selectNextCard());
            assertFalse(emptyPanel.selectPreviousCard());
        });
    }

    @Test
    public void tutorialTargets_selectExistingEventAndUseEmptyStates() throws Exception {
        runOnJavaFxThread(() -> {
            EventListPanel emptyPanel = new EventListPanel(FXCollections.observableArrayList());
            Region emptyRoot = emptyPanel.getRoot();
            new Scene(emptyRoot, 1000, 700);
            emptyRoot.applyCss();
            assertSame(getEventList(emptyRoot), emptyPanel.getTutorialEventCardTarget());
            assertSame(emptyRoot.lookup("#selectionHint"), emptyPanel.getTutorialDetailsTarget());
            assertSame(emptyRoot.lookup("#selectionHint"), emptyPanel.getTutorialMemberListTarget());

            EventListPanel panel = new EventListPanel(FXCollections.observableArrayList(event(List.of(), Map.of())));
            Region root = panel.getRoot();
            new Scene(root, 1000, 700);
            root.applyCss();
            panel.getTutorialEventCardTarget();
            assertEquals(0, getEventList(root).getSelectionModel().getSelectedIndex());
            assertSame(root.lookup("#detailsScroll"), panel.getTutorialDetailsTarget());
            assertSame(getMembers(root), panel.getTutorialMemberListTarget());
            assertEquals(1, getMembers(root).getChildren().size());
            Label emptyMembersHint = (Label) getMembers(root).getChildren().getFirst();
            assertEquals("No members assigned yet. Assign contacts with assign.", emptyMembersHint.getText());
        });
    }

    @Test
    public void tutorialEventCardTarget_usesVisibleSelectedCard() throws Exception {
        runOnJavaFxThread(() -> {
            Event first = event(List.of(), Map.of());
            Event second = new Event("Welcome Fair", DATE, DATE, List.of(), Map.of());
            EventListPanel panel = new EventListPanel(FXCollections.observableArrayList(first, second));
            Stage stage = new Stage();
            stage.setScene(new Scene(panel.getRoot(), 1000, 700));
            stage.show();
            try {
                Region root = panel.getRoot();
                root.applyCss();
                root.layout();
                ListView<Event> list = getEventList(root);

                ListCell<?> firstCard = assertInstanceOf(ListCell.class, panel.getTutorialEventCardTarget());
                assertEquals(0, firstCard.getIndex());
                assertEquals(first, list.getSelectionModel().getSelectedItem());

                list.getSelectionModel().select(second);
                ListCell<?> selectedCard = assertInstanceOf(ListCell.class, panel.getTutorialEventCardTarget());
                assertEquals(1, selectedCard.getIndex());
                assertEquals(second, list.getSelectionModel().getSelectedItem());
            } finally {
                stage.close();
            }
        });
    }

    @Test
    public void revealTutorialMemberList_scrollsToMembersWhenPaneIsShort() throws Exception {
        runOnJavaFxThread(() -> {
            Event event = event(List.of(ALICE, BOB, CHARLIE), Map.of());
            EventListPanel panel = new EventListPanel(FXCollections.observableArrayList(event));
            Stage stage = new Stage();
            stage.setScene(new Scene(panel.getRoot(), 900, 350));
            stage.show();
            try {
                Region root = panel.getRoot();
                root.applyCss();
                root.layout();
                assertSame(getMembers(root), panel.getTutorialMemberListTarget());
                ScrollPane detailsScroll = (ScrollPane) root.lookup("#detailsScroll");
                assertTrue(detailsScroll.getViewportBounds().getHeight() > 0);
                panel.revealTutorialMemberList();
                assertTrue(detailsScroll.getVvalue() > 0);
            } finally {
                stage.close();
            }
        });
    }

    @Test
    public void eventListChanges_preserveIntentionalSelectionState() throws Exception {
        runOnJavaFxThread(() -> {
            ObservableList<Event> events = FXCollections.observableArrayList();
            EventListPanel panel = new EventListPanel(events);
            Region root = panel.getRoot();
            new Scene(root);
            root.applyCss();

            events.add(event(List.of(), Map.of()));

            assertNull(getEventList(root).getSelectionModel().getSelectedItem());
            assertFalse(((VBox) root.lookup("#eventDetails")).isVisible());
            assertTrue(((Label) root.lookup("#selectionHint")).isVisible());

            assertTrue(panel.selectNextCard());
            Event selectedEvent = events.getFirst();
            events.add(new Event("Planning Session", DATE, DATE, List.of(), Map.of()));
            assertEquals(selectedEvent, getEventList(root).getSelectionModel().getSelectedItem());

            events.clear();
            assertNull(getEventList(root).getSelectionModel().getSelectedItem());
            assertFalse(((VBox) root.lookup("#eventDetails")).isVisible());
        });
    }

    @Test
    public void replaceSelectedEvent_refreshesStatusAndCountsImmediately() throws Exception {
        runOnJavaFxThread(() -> {
            Event event = event(List.of(ALICE, BOB), Map.of());
            ObservableList<Event> events = FXCollections.observableArrayList(event);
            Region root = new EventListPanel(events).getRoot();
            new Scene(root);
            root.applyCss();
            getEventList(root).getSelectionModel().selectFirst();

            events.setAll(event.withAttendance(ALICE, AttendanceStatus.ABSENT));
            assertCounts(root, "0 Present", "1 Absent", "1 Not marked");
            assertMembers(root, "Alice Tan", "● absent", "Bob Lim", "● unmarked");

            events.setAll(events.getFirst().withAttendance(ALICE, AttendanceStatus.PRESENT));
            assertCounts(root, "1 Present", "0 Absent", "1 Not marked");
            assertMembers(root, "Alice Tan", "● present", "Bob Lim", "● unmarked");
            assertEquals(events.getFirst(), getEventList(root).getSelectionModel().getSelectedItem());

            events.setAll(event(List.of(BOB), Map.of()));
            assertCounts(root, "0 Present", "0 Absent", "1 Not marked");
            assertMembers(root, "Bob Lim", "● unmarked");
        });
    }

    @Test
    public void eventWithoutMembers_showsZeroCountsAndAssignmentHint() throws Exception {
        runOnJavaFxThread(() -> {
            Region root = new EventListPanel(FXCollections.observableArrayList(event(List.of(), Map.of())))
                    .getRoot();
            new Scene(root);
            root.applyCss();
            getEventList(root).getSelectionModel().selectFirst();

            assertCounts(root, "0 Present", "0 Absent", "0 Not marked");
            assertEquals("Members (0)", getLabel(root, "memberCount").getText());
            Label hint = (Label) getMembers(root).getChildren().getFirst();
            assertEquals("No members assigned yet. Assign contacts with assign.", hint.getText());
        });
    }

    @Test
    public void selectEvent_showsMemberListStyleWithContactRoleOrganisationAndTags() throws Exception {
        runOnJavaFxThread(() -> {
            Person alice = new PersonBuilder().withName(ALICE.getFullName()).withRole("Treasurer")
                    .withOrganisation("Student Union").withTags("Finance", "Publicity").build();
            Region root = new EventListPanel(FXCollections.observableArrayList(event(List.of(ALICE), Map.of())),
                    FXCollections.observableArrayList(alice)).getRoot();
            new Scene(root);
            root.applyCss();
            getEventList(root).getSelectionModel().selectFirst();

            HBox row = (HBox) getMembers(root).getChildren().getFirst();
            assertEquals("1.", ((Label) row.getChildren().get(0)).getText());
            VBox memberDetails = (VBox) row.getChildren().get(2);
            FlowPane identityLine = (FlowPane) memberDetails.getChildren().get(0);
            assertEquals("Alice Tan", ((Label) identityLine.getChildren().get(0)).getText());
            assertEquals("Finance", ((Label) identityLine.getChildren().get(1)).getText());
            assertEquals("Publicity", ((Label) identityLine.getChildren().get(2)).getText());
            assertTrue(((Label) identityLine.getChildren().get(2)).getStyleClass().contains("tag-chip-blue"));
            assertEquals("Treasurer · Student Union", ((Label) memberDetails.getChildren().get(1)).getText());
        });
    }

    @Test
    public void selectEvent_showsSavedNoteBelowMemberMetadata() throws Exception {
        runOnJavaFxThread(() -> {
            String note = "Vegetarian; requires step-free access";
            Person alice = new PersonBuilder().withName(ALICE.getFullName()).withRole("Logistics")
                    .withOrganisation("NUS").withTags("Logistics", "Publicity").withNote(note).build();
            Event event = event(List.of(ALICE, BOB), Map.of(ALICE, AttendanceStatus.PRESENT));
            Region root = new EventListPanel(FXCollections.observableArrayList(event),
                    FXCollections.observableArrayList(alice)).getRoot();
            new Scene(root);
            root.applyCss();
            getEventList(root).getSelectionModel().selectFirst();

            HBox row = (HBox) getMembers(root).getChildren().getFirst();
            VBox details = (VBox) row.getChildren().get(2);
            Label noteLabel = (Label) row.lookup(".event-member-note");
            assertNotNull(noteLabel);
            assertEquals("Note: " + note, noteLabel.getText());
            assertEquals(2, details.getChildren().indexOf(noteLabel));
            assertTrue(noteLabel.isWrapText());
            assertTrue(noteLabel.isVisible());
            assertTrue(noteLabel.isManaged());
            FlowPane identityLine = (FlowPane) details.getChildren().getFirst();
            assertEquals("Logistics", ((Label) identityLine.getChildren().get(1)).getText());
            assertEquals("Publicity", ((Label) identityLine.getChildren().get(2)).getText());
            assertCounts(root, "1 Present", "0 Absent", "1 Not marked");
            assertMembers(root, "Alice Tan", "● present", "Bob Lim", "● unmarked");
        });
    }

    @Test
    public void selectEvent_withoutNoteOrSavedContact_omitsNoteLine() throws Exception {
        runOnJavaFxThread(() -> {
            Person alice = new PersonBuilder().withName(ALICE.getFullName()).withoutNote().build();
            Event event = event(List.of(ALICE, BOB), Map.of(BOB, AttendanceStatus.ABSENT));
            Region root = new EventListPanel(FXCollections.observableArrayList(event),
                    FXCollections.observableArrayList(alice)).getRoot();
            new Scene(root);
            root.applyCss();
            getEventList(root).getSelectionModel().selectFirst();

            for (var memberRow : getMembers(root).getChildren()) {
                HBox row = (HBox) memberRow;
                VBox details = (VBox) row.getChildren().get(2);
                assertNull(row.lookup(".event-member-note"));
                assertEquals(2, details.getChildren().size());
            }
            HBox unavailableContactRow = (HBox) getMembers(root).getChildren().get(1);
            assertEquals("Contact details unavailable", memberMetadata(unavailableContactRow));
            assertCounts(root, "0 Present", "1 Absent", "1 Not marked");
            assertMembers(root, "Alice Tan", "● unmarked", "Bob Lim", "● absent");
        });
    }

    @Test
    public void editContactNote_refreshesSelectedEventImmediately() throws Exception {
        runOnJavaFxThread(() -> {
            Person alice = new PersonBuilder().withName(ALICE.getFullName()).withNote("Vegetarian").build();
            Person bob = new PersonBuilder().withName(BOB.getFullName()).withNote("Arriving early").build();
            ObservableList<Person> persons = FXCollections.observableArrayList(alice, bob);
            Event selectedEvent = event(List.of(ALICE, BOB), Map.of(ALICE, AttendanceStatus.PRESENT));
            Region root = new EventListPanel(
                    FXCollections.observableArrayList(event(List.of(), Map.of()), selectedEvent), persons).getRoot();
            new Scene(root);
            root.applyCss();
            getEventList(root).getSelectionModel().select(1);
            HBox row = (HBox) getMembers(root).getChildren().getFirst();
            assertEquals("Note: Vegetarian", ((Label) row.lookup(".event-member-note")).getText());

            persons.set(0, new PersonBuilder(alice).withNote("Vegetarian; requires step-free access").build());

            row = (HBox) getMembers(root).getChildren().getFirst();
            Label updatedNote = (Label) row.lookup(".event-member-note");
            assertEquals("Note: Vegetarian; requires step-free access", updatedNote.getText());
            HBox otherRow = (HBox) getMembers(root).getChildren().get(1);
            assertEquals("Note: Arriving early", ((Label) otherRow.lookup(".event-member-note")).getText());
            assertSame(selectedEvent, getEventList(root).getSelectionModel().getSelectedItem());
            assertCounts(root, "1 Present", "0 Absent", "1 Not marked");
            assertMembers(root, "Alice Tan", "● present", "Bob Lim", "● unmarked");
        });
    }

    @Test
    public void clearContactNote_removesSelectedEventNoteLineImmediately() throws Exception {
        runOnJavaFxThread(() -> {
            Person alice = new PersonBuilder().withName(ALICE.getFullName()).withNote("Vegetarian").build();
            Person bob = new PersonBuilder().withName(BOB.getFullName()).withNote("Arriving early").build();
            ObservableList<Person> persons = FXCollections.observableArrayList(alice, bob);
            Event selectedEvent = event(List.of(ALICE, BOB), Map.of(ALICE, AttendanceStatus.ABSENT));
            Region root = new EventListPanel(FXCollections.observableArrayList(selectedEvent), persons).getRoot();
            new Scene(root);
            root.applyCss();
            getEventList(root).getSelectionModel().selectFirst();
            HBox row = (HBox) getMembers(root).getChildren().getFirst();
            assertEquals("Note: Vegetarian", ((Label) row.lookup(".event-member-note")).getText());

            persons.set(0, new PersonBuilder(alice).withoutNote().build());

            row = (HBox) getMembers(root).getChildren().getFirst();
            assertNull(row.lookup(".event-member-note"));
            assertEquals(2, ((VBox) row.getChildren().get(2)).getChildren().size());
            HBox otherRow = (HBox) getMembers(root).getChildren().get(1);
            assertEquals("Note: Arriving early", ((Label) otherRow.lookup(".event-member-note")).getText());
            assertSame(selectedEvent, getEventList(root).getSelectionModel().getSelectedItem());
            assertCounts(root, "0 Present", "1 Absent", "1 Not marked");
            assertMembers(root, "Alice Tan", "● absent", "Bob Lim", "● unmarked");
        });
    }

    @Test
    public void contactChanges_refreshSelectedEventMemberDetails() throws Exception {
        runOnJavaFxThread(() -> {
            Person original = new PersonBuilder().withName(ALICE.getFullName()).withRole("Treasurer")
                    .withOrganisation("Student Union").withTags("Finance").build();
            ObservableList<Person> persons = FXCollections.observableArrayList(original);
            Event event = event(List.of(ALICE), Map.of(ALICE, AttendanceStatus.ABSENT));
            Region root = new EventListPanel(FXCollections.observableArrayList(event), persons).getRoot();
            new Scene(root);
            root.applyCss();
            getEventList(root).getSelectionModel().selectFirst();

            HBox row = (HBox) getMembers(root).getChildren().getFirst();
            assertTrue(row.getChildren().get(3).getStyleClass().contains("attendance-absent"));
            assertEquals("Treasurer · Student Union", memberMetadata(row));

            persons.setAll(new PersonBuilder().withName(ALICE.getFullName()).withRole("Logistics Lead")
                    .withOrganisation("NUS").withTags("Logistics", "Sponsor", "UniversityStaff").build());

            row = (HBox) getMembers(root).getChildren().getFirst();
            VBox details = (VBox) row.getChildren().get(2);
            FlowPane identityLine = (FlowPane) details.getChildren().get(0);
            assertEquals("Logistics", ((Label) identityLine.getChildren().get(1)).getText());
            assertEquals("Sponsor", ((Label) identityLine.getChildren().get(2)).getText());
            assertEquals("UniversityStaff", ((Label) identityLine.getChildren().get(3)).getText());
            assertTrue(((Label) identityLine.getChildren().get(1)).getStyleClass()
                    .contains("tag-chip-teal"));
            assertTrue(((Label) identityLine.getChildren().get(2)).getStyleClass()
                    .contains("tag-chip-amber"));
            assertTrue(((Label) identityLine.getChildren().get(3)).getStyleClass()
                    .contains("tag-chip-purple"));
            assertTrue(((Label) row.getChildren().get(1)).getStyleClass().contains("event-member-avatar-teal"));
            assertEquals("Logistics Lead · NUS", memberMetadata(row));

            persons.setAll(new PersonBuilder().withName(ALICE.getFullName()).withRole("Sponsor Liaison")
                    .withOrganisation("Student Union").withTags("Sponsor").build());
            row = (HBox) getMembers(root).getChildren().getFirst();
            assertTrue(((Label) row.getChildren().get(1)).getStyleClass().contains("event-member-avatar-amber"));
        });
    }

    @Test
    public void eventCard_showsMemberAndAttendanceOverview() throws Exception {
        runOnJavaFxThread(() -> {
            Event event = event(List.of(ALICE, BOB, CHARLIE),
                    Map.of(ALICE, AttendanceStatus.PRESENT, BOB, AttendanceStatus.ABSENT));
            Region root = new EventListPanel(FXCollections.observableArrayList(event)).getRoot();
            new Scene(root, 900, 600);
            root.resize(900, 600);
            root.applyCss();
            root.layout();

            HBox overview = (HBox) root.lookup(".event-overview");
            assertNotNull(overview);
            assertEquals("3 members", ((Label) overview.getChildren().get(0)).getText());
            assertEquals("1 present", ((Label) overview.getChildren().get(1)).getText());
            assertEquals("1 absent", ((Label) overview.getChildren().get(2)).getText());
            assertEquals("1 unmarked", ((Label) overview.getChildren().get(3)).getText());
        });
    }

    @Test
    public void resize_longNotes_wrapWithoutCompressingAttendanceBadges() throws Exception {
        runOnJavaFxThread(() -> assertLongNoteLayout("Requires step-free access. ".repeat(20).substring(0, 500)));
    }

    @Test
    public void resize_unbrokenNotes_wrapWithoutCompressingAttendanceBadges() throws Exception {
        runOnJavaFxThread(() -> assertLongNoteLayout("W".repeat(500)));
    }

    @Test
    public void resize_narrowWindowStacksEventPanes() throws Exception {
        runOnJavaFxThread(() -> {
            SplitPane pane = (SplitPane) new EventListPanel(FXCollections.observableArrayList()).getRoot();
            new Scene(pane);
            pane.resize(900, 500);
            assertEquals(Orientation.HORIZONTAL, pane.getOrientation());
            assertEquals(460, ((Region) pane.getItems().get(1)).getMinWidth());

            pane.resize(700, 500);
            assertEquals(Orientation.VERTICAL, pane.getOrientation());
        });
    }

    private static void assertLongNoteLayout(String note) {
        ObservableList<Person> persons = FXCollections.observableArrayList();
        for (Name name : List.of(ALICE, BOB, CHARLIE)) {
            persons.add(new PersonBuilder().withName(name.getFullName()).withRole("Logistics")
                    .withOrganisation("NUS").withTags("Logistics", "Publicity").withNote(note).build());
        }
        Event event = event(List.of(ALICE, BOB, CHARLIE),
                Map.of(ALICE, AttendanceStatus.PRESENT, BOB, AttendanceStatus.ABSENT));
        EventListPanel panel = new EventListPanel(FXCollections.observableArrayList(event), persons);
        SplitPane root = (SplitPane) panel.getRoot();
        Scene scene = new Scene(root, 1400, 700);
        scene.getStylesheets().add(EventListPanelTest.class.getResource("/view/DarkTheme.css").toExternalForm());
        panel.selectNextCard();

        for (int width : new int[]{1400, 560, 1400}) {
            root.resize(width, 700);
            root.applyCss();
            root.layout();
            ScrollPane scroll = (ScrollPane) root.getItems().get(1);
            assertTrue(scroll.getContent().getLayoutBounds().getWidth() <= scroll.getViewportBounds().getWidth() + 1,
                    "Member details should fit the viewport at width " + width);
            for (var memberRow : getMembers(root).getChildren()) {
                HBox row = (HBox) memberRow;
                Label badge = (Label) row.getChildren().get(3);
                Label noteLabel = (Label) row.lookup(".event-member-note");
                VBox details = (VBox) row.getChildren().get(2);

                assertEquals(badge.prefWidth(-1), badge.getWidth(), 1,
                        "Attendance badge should retain its full width at width " + width);
                assertEquals(badge.prefHeight(-1), badge.getHeight(), 1);
                assertEquals(badge.getText(), ((Text) badge.lookup(".text")).getText(),
                        "Attendance badge text should not be truncated");
                assertEquals(row.getWidth() - row.getInsets().getRight(),
                        badge.getBoundsInParent().getMaxX(), 1, "Badge should stay at the right edge");
                assertEquals(row.getHeight() / 2, badge.getBoundsInParent().getCenterY(), 1,
                        "Badge should retain its vertical alignment");

                assertTrue(noteLabel.getHeight() > noteLabel.prefHeight(-1), "Long note should wrap");
                assertTrue(noteLabel.getHeight() + 1 >= noteLabel.prefHeight(noteLabel.getWidth()),
                        "Row should grow to show the complete note");
                assertEquals("Note: " + note, ((Text) noteLabel.lookup(".text")).getText(),
                        "Rendered note should not be truncated");
                Bounds noteBounds = noteLabel.localToScene(noteLabel.getLayoutBounds());
                Bounds badgeBounds = badge.localToScene(badge.getLayoutBounds());
                var identityLine = details.getChildren().getFirst();
                Bounds identityBounds = identityLine.localToScene(identityLine.getLayoutBounds());
                assertTrue(noteBounds.getMaxX() <= badgeBounds.getMinX(), "Note should not overlap badge");
                assertTrue(identityBounds.getMaxY() <= noteBounds.getMinY(), "Note should remain below tags");
                assertTrue(noteLabel.getBoundsInParent().getMaxY() <= details.getHeight(),
                        "Complete note should fit inside the details column");
                assertTrue(details.getBoundsInParent().getMaxY() <= row.getHeight() - row.getInsets().getBottom(),
                        "Member row should grow to contain the details column");
            }
            assertCounts(root, "1 Present", "1 Absent", "1 Not marked");
            assertMembers(root, "Alice Tan", "● present", "Bob Lim", "● absent", "Charlie Ng", "● unmarked");
        }
    }

    private static Event event(List<Name> members, Map<Name, AttendanceStatus> attendance) {
        return new Event("Team Dinner", DATE, DATE, members, attendance);
    }

    private static void assertCounts(Region root, String present, String absent, String notMarked) {
        assertEquals(present, getLabel(root, "presentCount").getText());
        assertEquals(absent, getLabel(root, "absentCount").getText());
        assertEquals(notMarked, getLabel(root, "notMarkedCount").getText());
    }

    private static void assertMembers(Region root, String... namesAndStatuses) {
        VBox members = getMembers(root);
        assertEquals(namesAndStatuses.length / 2, members.getChildren().size());
        for (int index = 0; index < namesAndStatuses.length; index += 2) {
            HBox row = (HBox) members.getChildren().get(index / 2);
            VBox details = (VBox) row.getChildren().get(2);
            FlowPane identityLine = (FlowPane) details.getChildren().get(0);
            Label name = (Label) identityLine.getChildren().get(0);
            assertEquals(namesAndStatuses[index], name.getText());
            assertEquals(namesAndStatuses[index + 1], ((Label) row.getChildren().get(3)).getText());
        }
    }

    private static VBox getMembers(Region root) {
        return (VBox) root.lookup("#members");
    }

    private static String memberMetadata(HBox row) {
        VBox details = (VBox) row.getChildren().get(2);
        return ((Label) details.getChildren().get(1)).getText();
    }

    private static Label getLabel(Region root, String id) {
        return (Label) root.lookup("#" + id);
    }

    @SuppressWarnings("unchecked")
    private static ListView<Event> getEventList(Region root) {
        return (ListView<Event>) root.lookup("#eventListView");
    }

    private static void runOnJavaFxThread(Runnable assertion) throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            assertion.run();
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }
}
