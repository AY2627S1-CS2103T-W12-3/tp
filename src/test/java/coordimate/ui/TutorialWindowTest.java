package coordimate.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

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
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
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
    public void navigation_showsSixStepsAndCloseOrFinishHidesOverlay() throws Exception {
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
                    "View contact details");
            assertTrue(backButton.isDisabled());
            for (int index = 0; index < titles.size(); index++) {
                assertEquals("Step " + (index + 1) + " of 6", stepCounter.getText());
                assertEquals(titles.get(index), stepTitle.getText());
                if (index < titles.size() - 1) {
                    nextButton.fire();
                }
            }
            assertTrue(tutorial.getCurrentStep().description().contains("assigned events"));
            assertEquals("Finish", nextButton.getText());
            backButton.fire();
            assertEquals("Step 5 of 6", stepCounter.getText());
            nextButton.fire();
            nextButton.fire();
            assertFalse(tutorial.isShowing());

            tutorial.show();
            assertEquals("Step 1 of 6", stepCounter.getText());
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
    public void overlayCard_staysWithinWindowAfterResize() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            TutorialWindow tutorial = new TutorialWindow();
            Stage stage = createStage(tutorial, 720, 520);
            tutorial.show();
            stage.setWidth(680);
            stage.setHeight(480);
            applyLayout(stage);

            Button nextButton = (Button) stage.getScene().lookup("#nextButton");
            for (int step = 0; step < 5; step++) {
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
            Pane appContent = new Pane(sidebarTab, commandList, commandInput, contactCard, contactDetails);
            StackPane sceneRoot = new StackPane(appContent, tutorial.getRoot());
            StackPane.setAlignment(appContent, Pos.CENTER_LEFT);
            Stage stage = new Stage();
            stage.setScene(new Scene(sceneRoot, 1000, 700));
            addTutorialStyles(stage);
            stage.show();
            tutorial.show();
            applyLayout(stage);

            Node card = stage.getScene().lookup("#tutorialCard");
            Bounds welcomeBounds = card.localToScene(card.getLayoutBounds());
            assertEquals(500, welcomeBounds.getCenterX(), 1);
            assertEquals(350, welcomeBounds.getCenterY(), 1);
            assertTrue(stage.getScene().lookup("#backdropFull").isVisible());
            Node highlight = stage.getScene().lookup("#targetHighlight");
            Region targetBlocker = (Region) stage.getScene().lookup("#targetBlocker");
            Button nextButton = (Button) stage.getScene().lookup("#nextButton");
            Node[] targets = {sidebarTab, commandList, commandInput, contactCard, contactDetails};
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

    private static Stage createStage(TutorialWindow tutorial, double width, double height) {
        Stage stage = new Stage();
        StackPane sceneRoot = new StackPane(tutorial.getRoot());
        stage.setScene(new Scene(sceneRoot, width, height));
        addTutorialStyles(stage);
        stage.show();
        return stage;
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
