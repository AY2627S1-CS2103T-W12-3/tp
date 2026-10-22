package coordimate.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

public class TutorialWindowTest {

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
    public void navigation_showsSixteenStepsAndCloseOrFinishHidesOverlay() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            TutorialWindow tutorial = new TutorialWindow();
            Stage stage = createStage(tutorial, 1100, 760);
            tutorial.show();
            applyLayout(stage);

            Label stepCounter = (Label) stage.getScene().lookup("#stepCounter");
            Label stepTitle = (Label) stage.getScene().lookup("#stepTitle");
            Button backButton = (Button) stage.getScene().lookup("#backButton");
            Button nextButton = (Button) stage.getScene().lookup("#nextButton");
            List<String> titles = List.of("Welcome to CoordiMate!", "Explore the Contacts tab",
                    "Explore contact commands", "Use the command box", "Select a contact card",
                    "View contact details", "Explore the Events tab", "Explore an event card",
                    "View event details", "View the member list", "Explore the Tags tab",
                    "Explore the tag list", "View tag details", "Navigate with your keyboard",
                    "Get help anytime", "Take the tour again");
            assertTrue(backButton.isDisabled());
            for (int index = 0; index < titles.size(); index++) {
                assertEquals("Step " + (index + 1) + " of 16", stepCounter.getText());
                assertEquals(titles.get(index), stepTitle.getText());
                if (index == 5) {
                    assertTrue(tutorial.getCurrentStep().description().contains("assigned events"));
                }
                if (index == 9) {
                    assertTrue(tutorial.getCurrentStep().description().contains("mark attendance"));
                }
                if (index < titles.size() - 1) {
                    nextButton.fire();
                }
            }
            assertTrue(tutorial.getCurrentStep().description().contains("whenever you need a reminder"));
            assertEquals("Finish →", nextButton.getText());
            backButton.fire();
            assertEquals("Step 15 of 16", stepCounter.getText());
            nextButton.fire();
            nextButton.fire();
            assertFalse(tutorial.isShowing());

            tutorial.show();
            assertEquals("Step 1 of 16", stepCounter.getText());
            Button closeButton = (Button) stage.getScene().lookup("#closeButton");
            closeButton.fire();
            assertFalse(tutorial.isShowing());
            stage.close();
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void keyboardShortcuts_navigateAndDismiss() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            TutorialWindow tutorial = new TutorialWindow();
            Stage stage = createStage(tutorial, 900, 650);
            tutorial.show();
            applyLayout(stage);

            Button nextButton = (Button) stage.getScene().lookup("#nextButton");
            Label counter = (Label) stage.getScene().lookup("#stepCounter");
            Node backdrop = stage.getScene().lookup("#backdropFull");
            AtomicBoolean backdropReceivedKey = new AtomicBoolean();
            backdrop.addEventHandler(KeyEvent.KEY_PRESSED, event -> backdropReceivedKey.set(true));
            backdrop.fireEvent(keyEvent(KeyCode.J));
            assertFalse(backdropReceivedKey.get());
            assertEquals("Step 1 of 16", counter.getText());
            backdrop.fireEvent(keyEvent(KeyCode.RIGHT));
            assertEquals("Step 2 of 16", counter.getText());

            nextButton.fireEvent(keyEvent(KeyCode.LEFT));
            assertEquals("Step 1 of 16", counter.getText());
            nextButton.fireEvent(keyEvent(KeyCode.LEFT));
            assertEquals("Step 1 of 16", counter.getText());
            nextButton.fireEvent(new KeyEvent(KeyEvent.KEY_RELEASED, "", "", KeyCode.RIGHT,
                    false, false, false, false));
            nextButton.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.RIGHT,
                    false, true, false, false));
            nextButton.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.RIGHT,
                    false, false, true, false));
            nextButton.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.RIGHT,
                    false, false, false, true));
            nextButton.fireEvent(keyEvent(KeyCode.J));
            assertEquals("Step 1 of 16", counter.getText());

            nextButton.fireEvent(keyEvent(KeyCode.RIGHT));
            assertEquals("Step 2 of 16", counter.getText());
            nextButton.fireEvent(keyEvent(KeyCode.ESCAPE));
            assertFalse(tutorial.isShowing());
            assertFalse(tutorial.handleShortcut(keyEvent(KeyCode.RIGHT)));
            stage.close();
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void mouseFilter_blocksBackdropButAllowsCardControls() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            TutorialWindow tutorial = new TutorialWindow();
            Stage stage = createStage(tutorial, 900, 650);
            tutorial.show();
            applyLayout(stage);
            Node backdrop = stage.getScene().lookup("#backdropFull");
            Button nextButton = (Button) stage.getScene().lookup("#nextButton");
            AtomicBoolean backdropReceivedClick = new AtomicBoolean();
            AtomicBoolean cardReceivedClick = new AtomicBoolean();
            backdrop.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> backdropReceivedClick.set(true));
            nextButton.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> cardReceivedClick.set(true));

            backdrop.fireEvent(mouseClickEvent());
            nextButton.fireEvent(mouseClickEvent());
            assertFalse(backdropReceivedClick.get());
            assertTrue(cardReceivedClick.get());
            assertFalse(tutorial.isInteractiveTarget(new Object()));
            tutorial.hide();
            stage.close();
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void stepChangeHandler_setWhileShowingReceivesCurrentStep() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            TutorialWindow tutorial = new TutorialWindow();
            Stage stage = createStage(tutorial, 900, 650);
            tutorial.show();
            Button nextButton = (Button) stage.getScene().lookup("#nextButton");
            nextButton.fire();
            List<String> titles = new ArrayList<>();
            tutorial.setStepChangeHandler(step -> titles.add(step.title()));
            assertEquals(List.of("Explore the Contacts tab"), titles);
            tutorial.hide();
            stage.close();
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void tabKey_keepsFocusWithinCardButtons() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            TutorialWindow tutorial = new TutorialWindow();
            Stage stage = createStage(tutorial, 900, 650);
            Button outsideButton = new Button("Outside");
            StackPane sceneRoot = (StackPane) stage.getScene().getRoot();
            sceneRoot.getChildren().add(0, outsideButton);
            tutorial.show();
            applyLayout(stage);

            Scene scene = stage.getScene();
            Button closeButton = (Button) scene.lookup("#closeButton");
            Button backButton = (Button) scene.lookup("#backButton");
            Button nextButton = (Button) scene.lookup("#nextButton");
            AtomicBoolean buttonReceivedTab = new AtomicBoolean();
            nextButton.addEventHandler(KeyEvent.KEY_PRESSED, event -> buttonReceivedTab.set(true));

            // Back is disabled on the first step, so Tab cycles between Close and Next only.
            nextButton.requestFocus();
            nextButton.fireEvent(keyEvent(KeyCode.TAB));
            assertFalse(buttonReceivedTab.get());
            assertEquals(closeButton, scene.getFocusOwner());
            closeButton.fireEvent(keyEvent(KeyCode.TAB));
            assertEquals(nextButton, scene.getFocusOwner());
            nextButton.fireEvent(shiftTabEvent());
            assertEquals(closeButton, scene.getFocusOwner());

            nextButton.fire();
            closeButton.fireEvent(keyEvent(KeyCode.TAB));
            assertEquals(backButton, scene.getFocusOwner());
            closeButton.requestFocus();
            closeButton.fireEvent(shiftTabEvent());
            assertEquals(nextButton, scene.getFocusOwner());

            outsideButton.requestFocus();
            assertTrue(tutorial.handleShortcut(keyEvent(KeyCode.TAB)));
            assertEquals(closeButton, scene.getFocusOwner());
            outsideButton.requestFocus();
            assertTrue(tutorial.handleShortcut(shiftTabEvent()));
            assertEquals(nextButton, scene.getFocusOwner());

            tutorial.hide();
            assertFalse(tutorial.handleShortcut(keyEvent(KeyCode.TAB)));
            stage.close();
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    private static KeyEvent keyEvent(KeyCode code) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code,
                false, false, false, false);
    }

    private static KeyEvent shiftTabEvent() {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.TAB,
                true, false, false, false);
    }

    private static MouseEvent mouseClickEvent() {
        return new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, 1,
                false, false, false, false, false, false, false, false, false, false, null);
    }

    @Test
    public void overlayCard_staysWithinWindowAfterResize() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            TutorialWindow tutorial = new TutorialWindow();
            Stage stage = createStage(tutorial, 720, 520);
            tutorial.show();
            stage.setWidth(680);
            stage.setHeight(480);
            applyLayout(stage);

            Button nextButton = (Button) stage.getScene().lookup("#nextButton");
            for (int step = 0; step < 9; step++) {
                nextButton.fire();
            }
            applyLayout(stage);

            Node card = stage.getScene().lookup("#tutorialCard");
            Bounds cardBounds = card.localToScene(card.getBoundsInLocal());
            Bounds nextBounds = nextButton.localToScene(nextButton.getBoundsInLocal());
            assertTrue(cardBounds.getMinX() >= 0);
            assertTrue(cardBounds.getMinY() >= 0);
            assertTrue(cardBounds.getMaxX() <= stage.getScene().getWidth());
            assertTrue(cardBounds.getMaxY() <= stage.getScene().getHeight());
            assertTrue(nextBounds.getMaxY() <= cardBounds.getMaxY());
            assertTrue(stage.getScene().lookup("#commandExamples") == null);
            tutorial.hide();
            stage.close();
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void highlightedTarget_nearTopOrBottomPlacesCardOnVisibleSide() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            TutorialWindow tutorial = new TutorialWindow();
            Region target = new Region();
            target.setPrefSize(700, 50);
            Pane appContent = new Pane(target);
            Stage stage = new Stage();
            stage.setScene(new Scene(new StackPane(appContent, tutorial.getRoot()), 1000, 700));
            addTutorialStyles(stage);
            stage.show();
            tutorial.show();
            Button nextButton = (Button) stage.getScene().lookup("#nextButton");
            for (int step = 0; step < 4; step++) {
                nextButton.fire();
            }
            Node card = stage.getScene().lookup("#tutorialCard");
            Node highlight = stage.getScene().lookup("#targetHighlight");

            target.relocate(0, 0);
            applyLayout(stage);
            tutorial.highlight(target);
            assertTrue(highlight.isVisible());
            assertFalse(stage.getScene().lookup("#backdropTop").isVisible());
            assertFalse(stage.getScene().lookup("#backdropLeft").isVisible());

            target.relocate(150, 50);
            applyLayout(stage);
            tutorial.highlight(target);
            assertTrue(card.localToScene(card.getLayoutBounds()).getMinY()
                    > highlight.localToScene(highlight.getLayoutBounds()).getMaxY());

            target.relocate(150, 580);
            applyLayout(stage);
            tutorial.highlight(target);
            assertTrue(card.localToScene(card.getLayoutBounds()).getMaxY()
                    < highlight.localToScene(highlight.getLayoutBounds()).getMinY());

            target.relocate(1050, 150);
            applyLayout(stage);
            tutorial.highlight(target);
            assertFalse(highlight.isVisible());
            assertTrue(stage.getScene().lookup("#backdropFull").isVisible());

            target.relocate(-800, 150);
            applyLayout(stage);
            tutorial.highlight(target);
            assertFalse(highlight.isVisible());
            assertTrue(stage.getScene().lookup("#backdropFull").isVisible());

            target.relocate(150, 750);
            applyLayout(stage);
            tutorial.highlight(target);
            assertFalse(highlight.isVisible());

            target.setVisible(false);
            tutorial.highlight(target);
            assertFalse(highlight.isVisible());
            assertTrue(stage.getScene().lookup("#backdropFull").isVisible());
            tutorial.highlight(new Region());
            assertFalse(highlight.isVisible());
            tutorial.hide();
            stage.close();
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void cardPlacement_highlightsTargetsWithoutExposingAppControls() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            TutorialWindow tutorial = new TutorialWindow();
            Region sidebarTab = new Region();
            sidebarTab.setPrefSize(150, 40);
            sidebarTab.relocate(20, 200);
            Region commandList = new Region();
            commandList.setPrefSize(150, 240);
            commandList.relocate(20, 260);
            Region commandInput = new Region();
            commandInput.setPrefSize(720, 40);
            commandInput.relocate(220, 620);
            Region contactCard = new Region();
            contactCard.setPrefSize(300, 80);
            contactCard.relocate(220, 170);
            Region contactDetails = new Region();
            contactDetails.setPrefSize(400, 400);
            contactDetails.relocate(550, 150);
            Region eventsTab = new Region();
            eventsTab.setPrefSize(150, 40);
            eventsTab.relocate(20, 200);
            Region eventCard = new Region();
            eventCard.setPrefSize(300, 80);
            eventCard.relocate(220, 170);
            Region eventDetails = new Region();
            eventDetails.setPrefSize(400, 400);
            eventDetails.relocate(550, 150);
            Region eventMembers = new Region();
            eventMembers.setPrefSize(400, 200);
            eventMembers.relocate(550, 330);
            Region tagsTab = new Region();
            tagsTab.setPrefSize(150, 40);
            tagsTab.relocate(20, 200);
            Region tagList = new Region();
            tagList.setPrefSize(300, 400);
            tagList.relocate(220, 150);
            Region tagDetails = new Region();
            tagDetails.setPrefSize(400, 400);
            tagDetails.relocate(550, 150);
            Region help = new Region();
            help.setPrefSize(100, 30);
            help.relocate(20, 20);
            Region tutorialMenuItem = new Region();
            tutorialMenuItem.setPrefSize(100, 30);
            tutorialMenuItem.relocate(130, 20);
            Pane appContent = new Pane(sidebarTab, commandList, commandInput, contactCard, contactDetails,
                    eventsTab, eventCard, eventDetails, eventMembers, tagsTab, tagList, tagDetails, help,
                    tutorialMenuItem);
            StackPane sceneRoot = new StackPane(appContent, tutorial.getRoot());
            StackPane.setAlignment(appContent, Pos.CENTER_LEFT);
            Stage stage = new Stage();
            stage.setScene(new Scene(sceneRoot, 1000, 700));
            addTutorialStyles(stage);
            stage.show();
            tutorial.show();
            applyLayout(stage);

            Node card = stage.getScene().lookup("#tutorialCard");
            assertCardCentered(stage, card);
            assertTrue(stage.getScene().lookup("#backdropFull").isVisible());
            Node highlight = stage.getScene().lookup("#targetHighlight");
            Region targetBlocker = (Region) stage.getScene().lookup("#targetBlocker");
            Button nextButton = (Button) stage.getScene().lookup("#nextButton");
            Node[] targets = {sidebarTab, commandList, commandInput, contactCard, contactDetails,
                eventsTab, eventCard, eventDetails, eventMembers, tagsTab, tagList, tagDetails};
            for (Node target : targets) {
                nextButton.fire();
                tutorial.highlight(target);
                applyLayout(stage);
                assertTrue(highlight.isVisible());
                Bounds cardBounds = card.localToScene(card.getLayoutBounds());
                Bounds highlightBounds = highlight.localToScene(highlight.getLayoutBounds());
                assertFalse(cardBounds.intersects(highlightBounds),
                        tutorial.getCurrentStep().title() + ": card=" + cardBounds + ", highlight=" + highlightBounds);
                assertFalse(stage.getScene().lookup("#backdropFull").isVisible());
                assertTrue(targetBlocker.isVisible());
                assertTrue(targetBlocker.isPickOnBounds());
                assertEquals(Color.TRANSPARENT, targetBlocker.getBackground().getFills().getFirst().getFill());
                Bounds targetBounds = target.localToScene(target.getLayoutBounds());
                assertTrue(targetBlocker.localToScene(targetBlocker.getLayoutBounds())
                        .contains(targetBounds.getCenterX(), targetBounds.getCenterY()));
                for (String shadeId : List.of("#backdropTop", "#backdropBottom", "#backdropLeft", "#backdropRight")) {
                    Node shade = stage.getScene().lookup(shadeId);
                    if (shade.isVisible()) {
                        assertFalse(shade.localToScene(shade.getLayoutBounds())
                                .contains(targetBounds.getCenterX(), targetBounds.getCenterY()));
                    }
                }
                assertFalse(tutorial.isInteractiveTarget(target));
            }
            nextButton.fire();
            tutorial.highlight(null);
            applyLayout(stage);
            assertFalse(highlight.isVisible());
            assertCardCentered(stage, card);
            nextButton.fire();
            tutorial.highlight(help);
            applyLayout(stage);
            assertTrue(highlight.isVisible());
            assertFalse(card.localToScene(card.getLayoutBounds())
                    .intersects(highlight.localToScene(highlight.getLayoutBounds())));
            nextButton.fire();
            tutorial.highlight(tutorialMenuItem);
            applyLayout(stage);
            assertTrue(highlight.isVisible());
            assertCardCentered(stage, card);
            assertFalse(card.localToScene(card.getLayoutBounds())
                    .intersects(highlight.localToScene(highlight.getLayoutBounds())));
            assertTrue(tutorial.isInteractiveTarget(nextButton));
            tutorial.hide();
            stage.close();
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void explanationText_usesLightThemeColor() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            TutorialWindow tutorial = new TutorialWindow();
            Stage stage = createStage(tutorial, 900, 650);
            tutorial.show();
            applyLayout(stage);
            TextFlow description = (TextFlow) stage.getScene().lookup("#stepDescription");
            Text text = (Text) description.getChildren().getFirst();
            assertEquals(Color.web("#f0f6fc"), text.getFill());
            tutorial.hide();
            stage.close();
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void highlightInsideScrollPane_staysWithinVisibleViewport() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            TutorialWindow tutorial = new TutorialWindow();
            Region spacer = new Region();
            spacer.setPrefHeight(250);
            Region memberList = new Region();
            memberList.setPrefWidth(350);
            memberList.setPrefHeight(500);
            VBox content = new VBox(spacer, memberList);
            ScrollPane details = new ScrollPane(content);
            details.setFitToWidth(true);
            details.setPrefSize(400, 220);
            details.relocate(550, 140);
            Pane appContent = new Pane(details);
            StackPane sceneRoot = new StackPane(appContent, tutorial.getRoot());
            Stage stage = new Stage();
            stage.setScene(new Scene(sceneRoot, 1000, 700));
            addTutorialStyles(stage);
            stage.show();
            tutorial.show();
            applyLayout(stage);
            details.setVvalue(0.5);
            applyLayout(stage);

            tutorial.highlight(memberList);
            Node highlight = stage.getScene().lookup("#targetHighlight");
            Node viewport = details.lookup(".viewport");
            Bounds highlightBounds = highlight.localToScene(highlight.getLayoutBounds());
            Bounds viewportBounds = viewport.localToScene(viewport.getLayoutBounds());
            assertTrue(highlight.isVisible());
            assertTrue(highlightBounds.getMinY() >= viewportBounds.getMinY() - 6);
            assertTrue(highlightBounds.getMaxY() <= viewportBounds.getMaxY() + 6);
            details.setVvalue(1);
            applyLayout(stage);
            tutorial.highlight(spacer);
            assertFalse(highlight.isVisible());
            assertTrue(stage.getScene().lookup("#backdropFull").isVisible());
            tutorial.hide();
            stage.close();
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    private static Stage createStage(TutorialWindow tutorial, double width, double height) {
        Stage stage = new Stage();
        StackPane sceneRoot = new StackPane(tutorial.getRoot());
        stage.setScene(new Scene(sceneRoot, width, height));
        addTutorialStyles(stage);
        stage.show();
        return stage;
    }

    private static void assertCardCentered(Stage stage, Node card) {
        Bounds bounds = card.localToScene(card.getLayoutBounds());
        assertEquals(stage.getScene().getWidth() / 2, bounds.getCenterX(), 1);
        assertEquals(stage.getScene().getHeight() / 2, bounds.getCenterY(), 1);
    }

    private static void addTutorialStyles(Stage stage) {
        stage.getScene().getStylesheets().add(TutorialWindow.class.getResource("/view/TutorialWindow.css")
                .toExternalForm());
    }

    private static void applyLayout(Stage stage) {
        stage.getScene().getRoot().applyCss();
        stage.getScene().getRoot().layout();
    }
}
