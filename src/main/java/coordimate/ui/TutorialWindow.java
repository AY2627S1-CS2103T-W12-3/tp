package coordimate.ui;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/** A guided introduction shown as an overlay on the main window. */
public class TutorialWindow extends UiPart<StackPane> {

    private static final String FXML = "TutorialWindow.fxml";
    private static final double CARD_MAX_WIDTH = 620;
    private static final double CARD_MIN_WIDTH = 260;
    private static final double WINDOW_MARGIN = 24;
    private static final double TARGET_MARGIN = 6;

    private static final List<TutorialStep> STEPS = List.of(
            new TutorialStep("Welcome to CoordiMate!",
                    "CoordiMate helps you manage CCA contacts, organise events, and track attendance. "
                            + "Let's take a quick look at where to find things.",
                    TutorialPlacement.CENTER, TutorialTarget.NONE),
            new TutorialStep("Explore the Contacts tab",
                    "The Contacts tab shows the people you've saved. Browse the list and select a contact "
                            + "to learn more about them.",
                    TutorialPlacement.LEFT_BAR, TutorialTarget.CONTACTS_TAB),
            new TutorialStep("Explore contact commands",
                    "The left panel lists commands for managing contacts. Select a command to place its name "
                            + "in the command box and see its format.",
                    TutorialPlacement.LEFT_BAR, TutorialTarget.CONTACT_COMMANDS),
            new TutorialStep("Use the command box",
                    "Enter commands here to work with your contacts. You can type a command yourself or start "
                            + "with one from the list on the left.",
                    TutorialPlacement.ABOVE_TARGET, TutorialTarget.COMMAND_BOX),
            new TutorialStep("Select a contact card",
                    "Each card gives you a quick view of a contact. Select one to open their full details.",
                    TutorialPlacement.BESIDE_TARGET, TutorialTarget.CONTACT_CARD),
            new TutorialStep("View contact details",
                    "The details pane shows a contact's phone number, email, role, tags, notes, and assigned events. "
                            + "You can use commands to add, edit, delete, search, filter, archive, or unarchive "
                            + "contacts.",
                    TutorialPlacement.BESIDE_TARGET, TutorialTarget.CONTACT_DETAILS),
            new TutorialStep("Explore the Events tab",
                    "The Events tab shows your saved events. Select an event to see its details and assigned members.",
                    TutorialPlacement.LEFT_BAR, TutorialTarget.EVENTS_TAB),
            new TutorialStep("Explore an event card",
                    "Each card shows the event's name, start and end times, number of assigned members, and an "
                            + "attendance summary. Select a card to open the event's details.",
                    TutorialPlacement.BESIDE_TARGET, TutorialTarget.EVENT_CARD),
            new TutorialStep("View event details",
                    "The details pane shows the event's name, start and end times, duration, and attendance counts.",
                    TutorialPlacement.BESIDE_TARGET, TutorialTarget.EVENT_DETAILS),
            new TutorialStep("View the member list",
                    "The member list shows everyone assigned to the event and whether each person is Present, "
                            + "Absent, or Not marked. Use commands to add, edit, delete, archive, or unarchive "
                            + "events; assign or unassign members; and mark attendance.",
                    TutorialPlacement.BESIDE_TARGET, TutorialTarget.EVENT_MEMBERS),
            new TutorialStep("Explore the Tags tab",
                    "The Tags tab shows the tags you use to group contacts by committee, role, or responsibility.",
                    TutorialPlacement.LEFT_BAR, TutorialTarget.TAGS_TAB),
            new TutorialStep("Explore the tag list",
                    "The tag list shows your available tags. Select a tag to see which contacts belong to it.",
                    TutorialPlacement.BESIDE_TARGET, TutorialTarget.TAG_LIST),
            new TutorialStep("View tag details",
                    "The details pane shows the selected tag's name, how many contacts use it, and a list of those "
                            + "contacts.",
                    TutorialPlacement.BESIDE_TARGET, TutorialTarget.TAG_DETAILS),
            new TutorialStep("Navigate with your keyboard",
                    "Press Esc to enter Navigation mode. Press J to select the next item or K for the previous one "
                            + "in the current tab. Press I to return to Command mode and focus the command box.",
                    TutorialPlacement.CENTER, TutorialTarget.NONE),
            new TutorialStep("Get help anytime",
                    "Select Help in the top bar to see command formats and keyboard shortcuts.",
                    TutorialPlacement.BESIDE_TARGET, TutorialTarget.HELP),
            new TutorialStep("Take the tour again",
                    "Select Tutorial in the top bar whenever you need a reminder.",
                    TutorialPlacement.CENTER, TutorialTarget.TUTORIAL));

    @FXML
    private Pane overlayPane;

    @FXML
    private Region backdropFull;

    @FXML
    private Region backdropTop;

    @FXML
    private Region backdropBottom;

    @FXML
    private Region backdropLeft;

    @FXML
    private Region backdropRight;

    @FXML
    private Region targetBlocker;

    @FXML
    private Region targetHighlight;

    @FXML
    private VBox tutorialCard;

    @FXML
    private Label stepCounter;

    @FXML
    private Label stepTitle;

    @FXML
    private TextFlow stepDescription;

    @FXML
    private Button closeButton;

    @FXML
    private Button backButton;

    @FXML
    private Button nextButton;

    private Consumer<TutorialStep> stepChangeHandler = step -> { };
    private Runnable dismissHandler = () -> { };
    private Node target;
    private int stepIndex;

    /** Creates a tutorial overlay that is initially hidden. */
    public TutorialWindow() {
        super(FXML, new StackPane());
        StackPane root = getRoot();
        root.setId("tutorialOverlay");
        root.setPickOnBounds(true);
        root.setVisible(false);
        root.setManaged(false);
        root.addEventFilter(MouseEvent.ANY, event -> {
            if (!isInteractiveTarget(event.getTarget())) {
                event.consume();
            }
        });
        root.addEventFilter(KeyEvent.ANY, event -> {
            if (handleShortcut(event) || !isInteractiveTarget(event.getTarget())) {
                event.consume();
            }
        });
        root.widthProperty().addListener((observable, oldWidth, newWidth) -> layoutOverlay());
        root.heightProperty().addListener((observable, oldHeight, newHeight) -> layoutOverlay());
        updateStep();
    }

    /** Shows the tutorial, starting again from its welcome step. */
    public void show() {
        stepIndex = 0;
        StackPane root = getRoot();
        root.setManaged(true);
        root.setVisible(true);
        updateStep();
        root.applyCss();
        root.requestLayout();
        Platform.runLater(() -> {
            layoutOverlay();
            nextButton.requestFocus();
        });
    }

    /** Returns whether the tutorial overlay is visible. */
    public boolean isShowing() {
        return getRoot().isVisible();
    }

    /** Hides the tutorial without changing its completion preference. */
    public void hide() {
        getRoot().setVisible(false);
        getRoot().setManaged(false);
        targetHighlight.setVisible(false);
        targetBlocker.setVisible(false);
        target = null;
    }

    /** Sets the action to run when the tutorial is finished or explicitly closed. */
    public void setDismissHandler(Runnable handler) {
        dismissHandler = handler;
    }

    /** Sets the action that updates the main window for the current step. */
    public void setStepChangeHandler(Consumer<TutorialStep> handler) {
        stepChangeHandler = handler;
        if (isShowing()) {
            stepChangeHandler.accept(STEPS.get(stepIndex));
        }
    }

    /** Returns the current tutorial step. */
    TutorialStep getCurrentStep() {
        return STEPS.get(stepIndex);
    }

    /** Highlights the supplied main-window node and positions the card away from it. */
    void highlight(Node node) {
        target = node;
        layoutOverlay();
    }

    @FXML
    private void showPreviousStep() {
        if (stepIndex > 0) {
            stepIndex--;
            updateStep();
        }
    }

    @FXML
    private void showNextStep() {
        if (stepIndex == STEPS.size() - 1) {
            dismiss();
            return;
        }
        stepIndex++;
        updateStep();
    }

    @FXML
    private void close() {
        dismiss();
    }

    /** Handles tutorial shortcuts before keys can reach the application beneath the overlay. */
    boolean handleShortcut(KeyEvent event) {
        if (!isShowing() || event.getEventType() != KeyEvent.KEY_PRESSED) {
            return false;
        }
        if (event.getCode() == KeyCode.TAB) {
            moveFocusWithinCard(!event.isShiftDown());
            return true;
        }
        if (event.isAltDown() || event.isControlDown() || event.isMetaDown()) {
            return false;
        }
        KeyCode code = event.getCode();
        if (code == KeyCode.LEFT) {
            showPreviousStep();
        } else if (code == KeyCode.RIGHT) {
            showNextStep();
        } else if (code == KeyCode.ESCAPE) {
            close();
        } else {
            return false;
        }
        return true;
    }

    /**
     * Moves focus to the next or previous enabled card button, wrapping around so that focus never leaves the card.
     * If focus is outside the card, moves it to the first or last button.
     */
    private void moveFocusWithinCard(boolean isForward) {
        List<Button> buttons = Stream.of(closeButton, backButton, nextButton)
                .filter(button -> !button.isDisabled())
                .toList();
        Scene scene = getRoot().getScene();
        int focusedIndex = scene == null ? -1 : buttons.indexOf(scene.getFocusOwner());
        int targetIndex;
        if (focusedIndex < 0) {
            targetIndex = isForward ? 0 : buttons.size() - 1;
        } else {
            targetIndex = Math.floorMod(focusedIndex + (isForward ? 1 : -1), buttons.size());
        }
        buttons.get(targetIndex).requestFocus();
    }

    private void dismiss() {
        hide();
        dismissHandler.run();
    }

    private void updateStep() {
        TutorialStep step = STEPS.get(stepIndex);
        String progress = "Step " + (stepIndex + 1) + " of " + STEPS.size();
        stepCounter.setText(progress);
        stepCounter.setAccessibleText(progress);
        stepTitle.setText(step.title());
        Text description = new Text(step.description());
        description.getStyleClass().add("tutorial-body-text");
        stepDescription.getChildren().setAll(description);
        stepDescription.setAccessibleText(step.description());
        backButton.setDisable(stepIndex == 0);
        boolean isLastStep = stepIndex == STEPS.size() - 1;
        nextButton.setText(isLastStep ? "Finish →" : "Next →");
        nextButton.setAccessibleText(isLastStep ? "Finish, Right Arrow" : "Next, Right Arrow");
        stepChangeHandler.accept(step);
        Platform.runLater(this::layoutOverlay);
    }

    private void layoutOverlay() {
        if (!isShowing()) {
            return;
        }

        double width = getRoot().getWidth();
        double height = getRoot().getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        overlayPane.resize(width, height);
        Bounds targetBounds = getNodeBounds(target);
        Bounds highlightBounds = getHighlightBounds(targetBounds, width, height);
        layoutBackdrop(width, height, highlightBounds);
        TutorialPlacement placement = getCurrentStep().placement();
        double maxCardWidth = Math.min(CARD_MAX_WIDTH, width - 2 * WINDOW_MARGIN);
        if (targetBounds != null && placement == TutorialPlacement.LEFT_BAR) {
            maxCardWidth = Math.min(maxCardWidth, width - targetBounds.getMaxX() - 2 * WINDOW_MARGIN);
        } else if (targetBounds != null && placement == TutorialPlacement.BESIDE_TARGET) {
            double leftSpace = targetBounds.getMinX() - 2 * WINDOW_MARGIN;
            double rightSpace = width - targetBounds.getMaxX() - 2 * WINDOW_MARGIN;
            if (Math.max(leftSpace, rightSpace) >= CARD_MIN_WIDTH) {
                maxCardWidth = Math.min(maxCardWidth, Math.max(leftSpace, rightSpace));
            }
        }
        tutorialCard.setMaxWidth(Math.max(0, maxCardWidth));
        tutorialCard.setMaxHeight(Math.max(0, height - 2 * WINDOW_MARGIN));
        tutorialCard.setPrefWidth(tutorialCard.getMaxWidth());
        tutorialCard.setPrefHeight(Region.USE_COMPUTED_SIZE);
        tutorialCard.applyCss();
        tutorialCard.autosize();

        double cardWidth = Math.min(tutorialCard.prefWidth(-1), tutorialCard.getMaxWidth());
        double cardHeight = Math.min(tutorialCard.prefHeight(cardWidth), height - 2 * WINDOW_MARGIN);
        tutorialCard.resize(cardWidth, cardHeight);

        Position position = getCardPosition(width, height, cardWidth, cardHeight, placement, targetBounds);
        tutorialCard.relocate(position.x(), position.y());
        updateHighlight(highlightBounds);
    }

    private Bounds getHighlightBounds(Bounds targetBounds, double width, double height) {
        if (targetBounds == null) {
            return null;
        }
        double left = Math.max(0, targetBounds.getMinX() - TARGET_MARGIN);
        double top = Math.max(0, targetBounds.getMinY() - TARGET_MARGIN);
        double right = Math.min(width, targetBounds.getMaxX() + TARGET_MARGIN);
        double bottom = Math.min(height, targetBounds.getMaxY() + TARGET_MARGIN);
        if (right <= left || bottom <= top) {
            return null;
        }
        return new BoundingBox(left, top, Math.max(0, right - left), Math.max(0, bottom - top));
    }

    private void layoutBackdrop(double width, double height, Bounds highlightBounds) {
        boolean hasTarget = highlightBounds != null;
        backdropFull.setVisible(!hasTarget);
        if (!hasTarget) {
            backdropFull.resizeRelocate(0, 0, width, height);
            backdropTop.setVisible(false);
            backdropBottom.setVisible(false);
            backdropLeft.setVisible(false);
            backdropRight.setVisible(false);
            targetBlocker.setVisible(false);
            return;
        }

        double left = highlightBounds.getMinX();
        double top = highlightBounds.getMinY();
        double right = highlightBounds.getMaxX();
        double bottom = highlightBounds.getMaxY();
        resizeBackdrop(backdropTop, 0, 0, width, top);
        resizeBackdrop(backdropBottom, 0, bottom, width, height - bottom);
        resizeBackdrop(backdropLeft, 0, top, left, bottom - top);
        resizeBackdrop(backdropRight, right, top, width - right, bottom - top);
        targetBlocker.resizeRelocate(left, top, right - left, bottom - top);
        targetBlocker.setVisible(right > left && bottom > top);
    }

    private void resizeBackdrop(Region shade, double x, double y, double width, double height) {
        shade.setVisible(width > 0 && height > 0);
        shade.resizeRelocate(x, y, Math.max(0, width), Math.max(0, height));
    }

    private Bounds getNodeBounds(Node node) {
        Scene scene = getRoot().getScene();
        if (node == null || scene == null || node.getScene() != scene || !node.isVisible()) {
            return null;
        }
        Bounds sceneBounds = node.localToScene(node.getLayoutBounds());
        for (Node ancestor = node.getParent(); ancestor != null; ancestor = ancestor.getParent()) {
            if (ancestor instanceof ScrollPane scrollPane) {
                Node viewport = scrollPane.lookup(".viewport");
                if (viewport != null) {
                    sceneBounds = intersect(sceneBounds, viewport.localToScene(viewport.getLayoutBounds()));
                    if (sceneBounds == null) {
                        return null;
                    }
                }
            }
        }
        return getRoot().sceneToLocal(sceneBounds);
    }

    private Bounds intersect(Bounds first, Bounds second) {
        double left = Math.max(first.getMinX(), second.getMinX());
        double top = Math.max(first.getMinY(), second.getMinY());
        double right = Math.min(first.getMaxX(), second.getMaxX());
        double bottom = Math.min(first.getMaxY(), second.getMaxY());
        return right > left && bottom > top
                ? new BoundingBox(left, top, right - left, bottom - top) : null;
    }

    private Position getCardPosition(double width, double height, double cardWidth, double cardHeight,
            TutorialPlacement placement, Bounds targetBounds) {
        double x = (width - cardWidth) / 2;
        double y = (height - cardHeight) / 2;
        if (targetBounds != null && placement == TutorialPlacement.LEFT_BAR) {
            x = targetBounds.getMaxX() + WINDOW_MARGIN;
        } else if (targetBounds != null && placement == TutorialPlacement.ABOVE_TARGET) {
            y = targetBounds.getMinY() - cardHeight - WINDOW_MARGIN;
        } else if (targetBounds != null && placement == TutorialPlacement.BESIDE_TARGET) {
            double rightSpace = width - targetBounds.getMaxX() - 2 * WINDOW_MARGIN;
            double leftSpace = targetBounds.getMinX() - 2 * WINDOW_MARGIN;
            if (rightSpace >= cardWidth && rightSpace >= leftSpace) {
                x = targetBounds.getMaxX() + WINDOW_MARGIN;
            } else if (leftSpace >= cardWidth) {
                x = targetBounds.getMinX() - cardWidth - WINDOW_MARGIN;
            } else if (targetBounds.getMinY() >= cardHeight + 2 * WINDOW_MARGIN) {
                y = targetBounds.getMinY() - cardHeight - WINDOW_MARGIN;
            } else {
                y = targetBounds.getMaxY() + WINDOW_MARGIN;
            }
        }
        x = Math.max(WINDOW_MARGIN, Math.min(x, width - cardWidth - WINDOW_MARGIN));
        y = Math.max(WINDOW_MARGIN, Math.min(y, height - cardHeight - WINDOW_MARGIN));
        return new Position(x, y);
    }

    private void updateHighlight(Bounds highlightBounds) {
        targetHighlight.setVisible(highlightBounds != null);
        if (highlightBounds != null) {
            targetHighlight.resizeRelocate(highlightBounds.getMinX(), highlightBounds.getMinY(),
                    highlightBounds.getWidth(), highlightBounds.getHeight());
        }
    }

    /** Returns whether an event belongs to the tutorial card. */
    boolean isInteractiveTarget(Object eventTarget) {
        if (!(eventTarget instanceof Node node)) {
            return false;
        }
        for (Node current = node; current != null; current = current.getParent()) {
            if (current == tutorialCard) {
                return true;
            }
        }
        return false;
    }

    /** Where the card appears relative to the main application. */
    enum TutorialPlacement {
        CENTER,
        LEFT_BAR,
        ABOVE_TARGET,
        BESIDE_TARGET
    }

    /** The main-window node highlighted by the step. */
    enum TutorialTarget {
        NONE,
        CONTACTS_TAB,
        CONTACT_COMMANDS,
        COMMAND_BOX,
        CONTACT_CARD,
        CONTACT_DETAILS,
        EVENTS_TAB,
        EVENT_CARD,
        EVENT_DETAILS,
        EVENT_MEMBERS,
        TAGS_TAB,
        TAG_LIST,
        TAG_DETAILS,
        HELP,
        TUTORIAL
    }

    /** Content and UI target for one tutorial step. */
    record TutorialStep(String title, String description, TutorialPlacement placement, TutorialTarget target) { }

    private record Position(double x, double y) { }
}
