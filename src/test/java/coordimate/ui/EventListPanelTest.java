package coordimate.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import coordimate.model.ModelManager;
import coordimate.model.event.AttendanceStatus;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.person.Name;
import coordimate.model.person.Person;
import coordimate.testutil.PersonBuilder;
import coordimate.testutil.TypicalPersons;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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
    private static final Clock CLOCK = Clock.fixed(LocalDate.of(2026, 11, 30)
            .atStartOfDay(ZoneId.of("Asia/Makassar")).toInstant(), ZoneId.of("Asia/Makassar"));

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
    public void selectEvent_dateOnlyDuration_countsDatesInclusively() throws Exception {
        runOnJavaFxThread(() -> {
            assertDuration("27-10-2026", "27-10-2026", "1 day");
            assertDuration("27-10-2026", "29-10-2026", "3 days");
            assertDuration("31-10-2026", "01-11-2026", "2 days");
            assertDuration("31-12-2026", "02-01-2027", "3 days");
            assertDuration("28-02-2028", "01-03-2028", "3 days");
        });
    }

    @Test
    public void selectEvent_mixedTimesDuration_identifiesMissingTime() throws Exception {
        runOnJavaFxThread(() -> {
            assertDuration("27-10-2026", "29-10-2026 10:30", "Unknown (start time not specified)");
            assertDuration("27-10-2026 09:00", "29-10-2026", "Unknown (end time not specified)");
            assertDuration("27-10-2026", "27-10-2026 00:00", "Unknown (start time not specified)");
            assertDuration("27-10-2026 23:59", "27-10-2026", "Unknown (end time not specified)");
        });
    }

    @Test
    public void selectEvent_timedDuration_formatsDaysHoursAndMinutes() throws Exception {
        runOnJavaFxThread(() -> {
            assertDuration("27-10-2026 09:00", "29-10-2026 10:30", "2 days 1 hour 30 minutes");
            assertDuration("27-10-2026 09:00", "29-10-2026 09:00", "2 days");
            assertDuration("27-10-2026 09:00", "28-10-2026 09:00", "1 day");
            assertDuration("27-10-2026 09:00", "28-10-2026 10:01", "1 day 1 hour 1 minute");
            assertDuration("27-10-2026 09:00", "29-10-2026 09:30", "2 days 30 minutes");
            assertDuration("27-10-2026 09:00", "29-10-2026 11:00", "2 days 2 hours");
        });
    }

    @Test
    public void selectEvent_timedDuration_omitsZeroUnitsAndPluralises() throws Exception {
        runOnJavaFxThread(() -> {
            assertDuration("27-10-2026 09:00", "27-10-2026 09:00", "0 minutes");
            assertDuration("27-10-2026 09:00", "27-10-2026 09:01", "1 minute");
            assertDuration("27-10-2026 09:00", "27-10-2026 09:30", "30 minutes");
            assertDuration("27-10-2026 09:00", "27-10-2026 10:00", "1 hour");
            assertDuration("27-10-2026 09:00", "27-10-2026 11:00", "2 hours");
            assertDuration("27-10-2026 09:00", "27-10-2026 10:01", "1 hour 1 minute");
            assertDuration("27-10-2026 09:00", "27-10-2026 11:30", "2 hours 30 minutes");
        });
    }

    @Test
    public void selectEvent_timedDuration_countsElapsedTimeAcrossDateBoundaries() throws Exception {
        runOnJavaFxThread(() -> {
            assertDuration("27-10-2026 23:59", "28-10-2026 00:00", "1 minute");
            assertDuration("27-10-2026 09:00", "28-10-2026 08:59", "23 hours 59 minutes");
            assertDuration("31-10-2026 23:00", "01-11-2026 01:30", "2 hours 30 minutes");
            assertDuration("31-12-2026 09:00", "02-01-2027 10:00", "2 days 1 hour");
        });
    }

    @Test
    public void selectEvent_durationRefreshesWhenSwitchingEvents() throws Exception {
        runOnJavaFxThread(() -> {
            String[][] cases = {
                {"27-10-2026", "29-10-2026", "3 days"},
                {"27-10-2026 09:00", "29-10-2026 10:30", "2 days 1 hour 30 minutes"},
                {"27-10-2026", "29-10-2026 10:30", "Unknown (start time not specified)"},
                {"27-10-2026 09:00", "29-10-2026", "Unknown (end time not specified)"}
            };
            ObservableList<Event> events = FXCollections.observableArrayList();
            for (int i = 0; i < cases.length; i++) {
                events.add(new Event("Event " + i, new EventTime(cases[i][0]), new EventTime(cases[i][1])));
            }
            Region root = new EventListPanel(events).getRoot();
            new Scene(root);
            root.applyCss();
            ListView<Event> list = getEventList(root);

            for (int i = 0; i < cases.length; i++) {
                list.getSelectionModel().select(i);
                assertDurationDetails(root, events.get(i), cases[i][2]);
            }
            list.getSelectionModel().selectFirst();
            assertDurationDetails(root, events.getFirst(), "3 days");
        });
    }

    @Test
    public void replaceSelectedEvent_durationRefreshesWhenTimesChange() throws Exception {
        runOnJavaFxThread(() -> {
            Event original = new Event("Concert", new EventTime("27-10-2026"), new EventTime("29-10-2026"));
            ObservableList<Event> events = FXCollections.observableArrayList(original);
            Region root = new EventListPanel(events).getRoot();
            new Scene(root);
            root.applyCss();
            ListView<Event> list = getEventList(root);
            list.getSelectionModel().selectFirst();
            assertDurationDetails(root, original, "3 days");

            String[][] edits = {
                {"27-10-2026 09:00", "29-10-2026", "Unknown (end time not specified)"},
                {"27-10-2026 09:00", "29-10-2026 10:30", "2 days 1 hour 30 minutes"},
                {"27-10-2026", "29-10-2026 10:30", "Unknown (start time not specified)"},
                {"27-10-2026", "29-10-2026", "3 days"},
                {"27-10-2026 09:00", "27-10-2026 09:00", "0 minutes"}
            };
            for (String[] edit : edits) {
                Event updated = new Event(original.getName(), new EventTime(edit[0]), new EventTime(edit[1]));
                events.set(0, updated);

                assertSame(updated, list.getSelectionModel().getSelectedItem());
                assertDurationDetails(root, updated, edit[2]);
            }
        });
    }

    @Test
    public void memberIds_ignoreContactFilteringAndFollowRosterReorderingAndEdits() throws Exception {
        runOnJavaFxThread(() -> {
            Person alice = new PersonBuilder(TypicalPersons.ALICE).withMemberId(12)
                    .withName(ALICE.getFullName()).build();
            Person bob = new PersonBuilder(TypicalPersons.BENSON).withMemberId(45).withName(BOB.getFullName()).build();
            Person charlie = new PersonBuilder(TypicalPersons.CARL).withMemberId(99)
                    .withName(CHARLIE.getFullName()).build();
            ObservableList<Person> persons = FXCollections.observableArrayList(alice, bob, charlie);
            FilteredList<Person> displayed = new FilteredList<>(persons, person -> person.equals(alice));
            ObservableList<Event> events = FXCollections.observableArrayList(
                    event(List.of(BOB, ALICE, CHARLIE), Map.of()));
            Region root = new EventListPanel(events, displayed).getRoot();
            new Scene(root);
            root.applyCss();
            getEventList(root).getSelectionModel().selectFirst();
            assertEquals(List.of("ID: 45", "ID: 12", "ID: 99"), memberIds(root));

            displayed.setPredicate(person -> false);
            assertEquals(List.of("ID: 45", "ID: 12", "ID: 99"), memberIds(root));
            persons.set(0, new PersonBuilder(alice).withRole("President").build());
            events.setAll(event(List.of(CHARLIE, ALICE, BOB), Map.of()));
            assertEquals(List.of("ID: 99", "ID: 12", "ID: 45"), memberIds(root));
            assertEquals("President", memberMetadata((HBox) getMembers(root).getChildren().get(1)));
        });
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
            assertEquals(List.of("ID: unavailable", "ID: unavailable", "ID: unavailable"), memberIds(root));
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
            Person alice = new PersonBuilder().withMemberId(12).withName(ALICE.getFullName()).withRole("Treasurer")
                    .withOrganisation("Student Union").withTags("Finance", "Publicity").build();
            Region root = new EventListPanel(FXCollections.observableArrayList(event(List.of(ALICE), Map.of())),
                    FXCollections.observableArrayList(alice)).getRoot();
            new Scene(root);
            root.applyCss();
            getEventList(root).getSelectionModel().selectFirst();

            HBox row = (HBox) getMembers(root).getChildren().getFirst();
            assertEquals("ID: 12", ((Label) row.getChildren().get(0)).getText());
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
            Person alice = new PersonBuilder().withMemberId(12).withName(ALICE.getFullName()).withRole("Logistics")
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
            Person alice = new PersonBuilder().withMemberId(12).withName(ALICE.getFullName()).withoutNote().build();
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
            Person alice = new PersonBuilder().withMemberId(12).withName(ALICE.getFullName())
                    .withNote("Vegetarian").build();
            Person bob = new PersonBuilder().withMemberId(45).withName(BOB.getFullName())
                    .withNote("Arriving early").build();
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
            Person alice = new PersonBuilder().withMemberId(12).withName(ALICE.getFullName())
                    .withNote("Vegetarian").build();
            Person bob = new PersonBuilder().withMemberId(45).withName(BOB.getFullName())
                    .withNote("Arriving early").build();
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
            Person original = new PersonBuilder().withMemberId(12).withName(ALICE.getFullName()).withRole("Treasurer")
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

            persons.setAll(new PersonBuilder().withMemberId(12).withName(ALICE.getFullName())
                    .withRole("Logistics Lead")
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

            persons.setAll(new PersonBuilder().withMemberId(12).withName(ALICE.getFullName())
                    .withRole("Sponsor Liaison")
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

            pane.resize(850, 500);
            assertEquals(Orientation.VERTICAL, pane.getOrientation());
        });
    }

    private static void assertLongNoteLayout(String note) {
        ObservableList<Person> persons = FXCollections.observableArrayList();
        for (Name name : List.of(ALICE, BOB, CHARLIE)) {
            persons.add(new PersonBuilder().withMemberId(Integer.MAX_VALUE - persons.size())
                    .withName(name.getFullName()).withRole("Logistics")
                    .withOrganisation("NUS").withTags("Logistics", "Publicity").withNote(note)
                    .withLeaveDate("30-11-2026").build());
        }
        Event event = event(List.of(ALICE, BOB, CHARLIE),
                Map.of(ALICE, AttendanceStatus.PRESENT, BOB, AttendanceStatus.ABSENT));
        EventListPanel panel = new EventListPanel(FXCollections.observableArrayList(event), persons, CLOCK);
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
                Label memberId = (Label) row.getChildren().getFirst();
                Label noteLabel = (Label) row.lookup(".event-member-note");
                Label leave = (Label) row.lookup(".leave-badge");
                VBox details = (VBox) row.getChildren().get(2);

                assertTrue(memberId.getWidth() + 1 >= memberId.prefWidth(-1),
                        "Member ID should retain its full width at width " + width);
                assertEquals(memberId.getText(), ((Text) memberId.lookup(".text")).getText());

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
                Bounds leaveBounds = leave.localToScene(leave.getLayoutBounds());
                Bounds badgeBounds = badge.localToScene(badge.getLayoutBounds());
                var identityLine = details.getChildren().getFirst();
                Bounds identityBounds = identityLine.localToScene(identityLine.getLayoutBounds());
                assertTrue(noteBounds.getMaxX() <= badgeBounds.getMinX(), "Note should not overlap badge");
                assertTrue(leaveBounds.getMaxX() <= badgeBounds.getMinX(), "Leave should not overlap attendance");
                assertTrue(identityBounds.getMaxY() <= leaveBounds.getMinY(), "Leave should remain below tags");
                assertTrue(leaveBounds.getMaxY() <= noteBounds.getMinY(), "Note should remain below leave");
                assertEquals("On leave until 30-11-2026", ((Text) leave.lookup(".text")).getText(),
                        "Leave date should not be truncated");
                assertTrue(leave.getHeight() + 1 >= leave.prefHeight(leave.getWidth()),
                        "Complete leave date should fit at width " + width);
                assertTrue(noteLabel.getBoundsInParent().getMaxY() <= details.getHeight(),
                        "Complete note should fit inside the details column");
                assertTrue(details.getBoundsInParent().getMaxY() <= row.getHeight() - row.getInsets().getBottom(),
                        "Member row should grow to contain the details column");
            }
            assertCounts(root, "1 Present", "1 Absent", "1 Not marked");
            assertMembers(root, "Alice Tan", "● present", "Bob Lim", "● absent", "Charlie Ng", "● unmarked");
        }
    }

    @Test
    public void selectEvent_showsCurrentLeaveSeparatelyFromAttendance() throws Exception {
        runOnJavaFxThread(() -> {
            Person alice = new PersonBuilder().withMemberId(12).withName(ALICE.getFullName())
                    .withLeaveDate("30-11-2026").withNote("Contact by email").build();
            Person bob = new PersonBuilder().withMemberId(45).withName(BOB.getFullName())
                    .withLeaveDate("15-12-2026").build();
            Event event = event(List.of(ALICE, BOB), Map.of(ALICE, AttendanceStatus.PRESENT));
            Region root = new EventListPanel(FXCollections.observableArrayList(event),
                    FXCollections.observableArrayList(alice, bob), CLOCK).getRoot();
            new Scene(root);
            root.applyCss();
            getEventList(root).getSelectionModel().selectFirst();

            assertEquals(List.of("On leave until 30-11-2026", "On leave until 15-12-2026"), leaveBadges(root));
            HBox row = (HBox) getMembers(root).getChildren().getFirst();
            VBox details = (VBox) row.getChildren().get(2);
            Label leave = (Label) row.lookup(".leave-badge");
            assertTrue(leave.isVisible());
            assertTrue(leave.isManaged());
            assertTrue(leave.isWrapText());
            assertEquals(2, details.getChildren().indexOf(leave));
            assertEquals(3, details.getChildren().indexOf(row.lookup(".event-member-note")));
            assertCounts(root, "1 Present", "0 Absent", "1 Not marked");
            assertMembers(root, "Alice Tan", "● present", "Bob Lim", "● unmarked");
        });
    }

    @Test
    public void selectEvent_endedLeaveNoLeaveOrMissingContact_omitsLeaveIndicator() throws Exception {
        runOnJavaFxThread(() -> {
            Person alice = new PersonBuilder().withMemberId(12).withName(ALICE.getFullName())
                    .withLeaveDate("29-11-2026").build();
            Person bob = new PersonBuilder().withMemberId(45).withName(BOB.getFullName()).withoutLeaveDate().build();
            Region root = new EventListPanel(FXCollections.observableArrayList(
                    event(List.of(ALICE, BOB, CHARLIE), Map.of(BOB, AttendanceStatus.ABSENT))),
                    FXCollections.observableArrayList(alice, bob), CLOCK).getRoot();
            new Scene(root);
            root.applyCss();
            getEventList(root).getSelectionModel().selectFirst();

            assertEquals(List.of(), leaveBadges(root));
            assertCounts(root, "0 Present", "1 Absent", "2 Not marked");
            assertMembers(root, "Alice Tan", "● unmarked", "Bob Lim", "● absent", "Charlie Ng", "● unmarked");
        });
    }

    @Test
    public void editContactLeave_refreshesSelectedEventEvenWhenContactIsFilteredOut() throws Exception {
        runOnJavaFxThread(() -> {
            Person alice = new PersonBuilder().withMemberId(12).withName(ALICE.getFullName()).build();
            ObservableList<Person> persons = FXCollections.observableArrayList(alice);
            FilteredList<Person> filtered = new FilteredList<>(persons, person -> false);
            Event event = event(List.of(ALICE), Map.of(ALICE, AttendanceStatus.ABSENT));
            Region root = new EventListPanel(FXCollections.observableArrayList(event), filtered, CLOCK).getRoot();
            new Scene(root);
            root.applyCss();
            getEventList(root).getSelectionModel().selectFirst();
            assertEquals(List.of(), leaveBadges(root));

            for (String endDate : List.of("30-11-2026", "15-12-2026")) {
                persons.set(0, new PersonBuilder(alice).withLeaveDate(endDate).build());
                assertEquals(List.of("On leave until " + endDate), leaveBadges(root));
                assertSame(event, getEventList(root).getSelectionModel().getSelectedItem());
                assertCounts(root, "0 Present", "1 Absent", "0 Not marked");
                assertMembers(root, "Alice Tan", "● absent");
            }
            persons.set(0, new PersonBuilder(alice).withoutLeaveDate().build());
            assertEquals(List.of(), leaveBadges(root));
            assertSame(event, getEventList(root).getSelectionModel().getSelectedItem());
            assertMembers(root, "Alice Tan", "● absent");
        });
    }

    @Test
    public void refreshIfDateChanged_handlesDifferentExpiryDatesAndClockMovingBackward() throws Exception {
        runOnJavaFxThread(() -> {
            MutableClock clock = new MutableClock(LocalDate.of(2026, 11, 30));
            Person alice = new PersonBuilder(TypicalPersons.ALICE).withName(ALICE.getFullName())
                    .withLeaveDate("30-11-2026").build();
            Person bob = new PersonBuilder(TypicalPersons.BENSON).withName(BOB.getFullName())
                    .withLeaveDate("15-12-2026").build();
            Event event = event(List.of(ALICE, BOB), Map.of(ALICE, AttendanceStatus.PRESENT));
            EventListPanel panel = new EventListPanel(FXCollections.observableArrayList(event),
                    FXCollections.observableArrayList(alice, bob), clock);
            Region root = panel.getRoot();
            new Scene(root);
            root.applyCss();
            getEventList(root).getSelectionModel().selectFirst();
            var originalRow = getMembers(root).getChildren().getFirst();
            panel.refreshIfDateChanged();
            assertSame(originalRow, getMembers(root).getChildren().getFirst());

            clock.setDate(LocalDate.of(2026, 12, 1));
            panel.refreshIfDateChanged();
            assertEquals(List.of("On leave until 15-12-2026"), leaveBadges(root));
            assertCounts(root, "1 Present", "0 Absent", "1 Not marked");
            assertSame(event, getEventList(root).getSelectionModel().getSelectedItem());

            clock.setDate(LocalDate.of(2026, 11, 30));
            panel.refreshIfDateChanged();
            assertEquals(List.of("On leave until 30-11-2026", "On leave until 15-12-2026"), leaveBadges(root));

            clock.setDate(LocalDate.of(2027, 1, 1));
            panel.refreshIfDateChanged();
            assertEquals(List.of(), leaveBadges(root));
            assertMembers(root, "Alice Tan", "● present", "Bob Lim", "● unmarked");
        });
    }

    @Test
    public void archivedEvent_showsAndRefreshesLeaveForContactsHiddenByArchiveAndFilter() throws Exception {
        runOnJavaFxThread(() -> {
            ModelManager model = new ModelManager();
            Person alice = new PersonBuilder(TypicalPersons.ALICE).withName(ALICE.getFullName())
                    .withLeaveDate("30-11-2026").withArchived(true).build();
            Person bob = new PersonBuilder(TypicalPersons.BENSON).withName(BOB.getFullName())
                    .withLeaveDate("15-12-2026").build();
            model.addPerson(alice);
            model.addPerson(bob);
            Event archived = event(List.of(ALICE, BOB), Map.of(BOB, AttendanceStatus.ABSENT)).withArchived(true);
            model.addEvent(archived);
            model.setShowingArchived(true);
            model.updateFilteredPersonList(person -> false);
            MutableClock clock = new MutableClock(LocalDate.of(2026, 11, 30));
            EventListPanel panel = new EventListPanel(model.getFilteredEventList(),
                    model.getFilteredPersonList(), clock);
            Region root = panel.getRoot();
            new Scene(root);
            root.applyCss();
            getEventList(root).getSelectionModel().selectFirst();

            assertTrue(model.getFilteredPersonList().isEmpty());
            assertEquals(List.of("On leave until 30-11-2026", "On leave until 15-12-2026"), leaveBadges(root));
            assertMembers(root, "Alice Tan (archived)", "● unmarked", "Bob Lim", "● absent");

            clock.setDate(LocalDate.of(2026, 12, 1));
            panel.refreshIfDateChanged();
            assertEquals(List.of("On leave until 15-12-2026"), leaveBadges(root));
            model.setPerson(bob, new PersonBuilder(bob).withoutLeaveDate().build());
            assertEquals(List.of(), leaveBadges(root));
            assertSame(archived, getEventList(root).getSelectionModel().getSelectedItem());
            assertCounts(root, "0 Present", "1 Absent", "1 Not marked");
        });
    }

    private static List<String> leaveBadges(Region root) {
        return getMembers(root).getChildren().stream()
                .flatMap(row -> row.lookupAll(".leave-badge").stream())
                .map(node -> ((Label) node).getText()).toList();
    }

    private static List<String> memberIds(Region root) {
        return getMembers(root).getChildren().stream()
                .map(row -> ((Label) ((HBox) row).getChildren().getFirst()).getText()).toList();
    }

    @Test
    public void setShowingArchived_marksArchivedMembersAndRelabelsList() throws Exception {
        runOnJavaFxThread(() -> {
            Person archived = new PersonBuilder().withMemberId(1).withName(ALICE.getFullName()).build()
                    .withArchived(true);
            Person active = new PersonBuilder().withMemberId(2).withName(BOB.getFullName())
                    .withPhone("98765432").withEmail("bob@example.com").build();
            ObservableList<Event> events = FXCollections.observableArrayList(event(List.of(ALICE, BOB), Map.of()));
            EventListPanel panel = new EventListPanel(events, FXCollections.observableArrayList(archived, active));
            Region root = panel.getRoot();
            new Scene(root);
            root.applyCss();
            getEventList(root).getSelectionModel().selectFirst();

            assertEquals(List.of("Alice Tan (archived)", "Bob Lim"), root.lookupAll(".event-member-name").stream()
                    .map(node -> ((Label) node).getText()).toList());

            Label count = (Label) root.lookup("#eventCount");
            assertEquals("Events (1)", count.getText());
            panel.setShowingArchived(true);
            assertEquals("Archived events (1)", count.getText());
            events.clear();
            assertEquals("Archived events (0)", count.getText());
            Label placeholder = (Label) getEventList(root).getPlaceholder();
            assertEquals(EventListPanel.MESSAGE_NO_ARCHIVED_EVENTS, placeholder.getText());
            panel.setShowingArchived(false);
            assertEquals("Events (0)", count.getText());
            assertEquals(EventListPanel.MESSAGE_NO_EVENTS, placeholder.getText());
        });
    }

    private static void assertDuration(String start, String end, String expected) {
        Event event = new Event("Duration test", new EventTime(start), new EventTime(end));
        Region root = new EventListPanel(FXCollections.observableArrayList(event)).getRoot();
        new Scene(root);
        root.applyCss();
        getEventList(root).getSelectionModel().selectFirst();

        assertDurationDetails(root, event, expected);
    }

    private static void assertDurationDetails(Region root, Event event, String expected) {
        assertEquals(expected, getLabel(root, "duration").getText(),
                event.getStartTime() + " to " + event.getEndTime());
        assertEquals(event.getName(), getLabel(root, "selectedName").getText());
        assertEquals(event.getStartTime().toString(), getLabel(root, "startTime").getText());
        assertEquals(event.getEndTime().toString(), getLabel(root, "endTime").getText());
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

    /** Supplies a controllable local date for leave-expiry tests. */
    private static class MutableClock extends Clock {
        private Instant current;

        MutableClock(LocalDate date) {
            setDate(date);
        }

        void setDate(LocalDate date) {
            current = date.atStartOfDay(CLOCK.getZone()).toInstant();
        }

        @Override
        public ZoneId getZone() {
            return CLOCK.getZone();
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
