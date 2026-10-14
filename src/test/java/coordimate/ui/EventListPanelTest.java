package coordimate.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

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
    }

    @Test
    public void selectEvent_showsEveryMemberStatusAndSummary() throws Exception {
        runOnJavaFxThread(() -> {
            Event event = event(List.of(ALICE, BOB, CHARLIE),
                    Map.of(ALICE, AttendanceStatus.PRESENT, BOB, AttendanceStatus.ABSENT));
            Region root = new EventListPanel(FXCollections.observableArrayList(event)).getRoot();
            new Scene(root);
            root.applyCss();

            assertEquals("Members (3)", getLabel(root, "memberCount").getText());
            assertCounts(root, "1 Present", "1 Absent", "1 Not marked");
            assertMembers(root, "Alice Tan", "● present", "Bob Lim", "● absent", "Charlie Ng", "● unmarked");
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

            HBox row = (HBox) getMembers(root).getChildren().getFirst();
            assertEquals("1.", ((Label) row.getChildren().get(0)).getText());
            VBox memberDetails = (VBox) row.getChildren().get(2);
            FlowPane identityLine = (FlowPane) memberDetails.getChildren().get(0);
            assertEquals("Alice Tan", ((Label) identityLine.getChildren().get(0)).getText());
            assertEquals("Finance", ((Label) identityLine.getChildren().get(1)).getText());
            assertEquals("Publicity", ((Label) identityLine.getChildren().get(2)).getText());
            assertEquals("Treasurer · Student Union", ((Label) memberDetails.getChildren().get(1)).getText());
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
