package coordimate.ui;

import static java.util.Objects.requireNonNull;

import javafx.beans.binding.Bindings;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

/**
 * A UI component that displays the result of a command execution.
 */
public class ResultDisplay extends UiPart<Region> {

    private static final String FXML = "ResultDisplay.fxml";
    private static final PseudoClass ERROR = PseudoClass.getPseudoClass("command-error");
    private static final PseudoClass SUCCESS = PseudoClass.getPseudoClass("command-success");

    private final Image infoIcon = new Image(requireNonNull(getClass().getResourceAsStream("/images/info_icon.png")));
    private final Image errorIcon = new Image(requireNonNull(getClass().getResourceAsStream("/images/fail.png")));

    @FXML
    private HBox feedbackPane;

    @FXML
    private ImageView feedbackIcon;

    @FXML
    private Label feedbackStatus;

    @FXML
    private Label resultDisplay;

    @FXML
    private ScrollPane feedbackMessageScroll;

    @FXML
    private Label feedbackScrollHint;

    /**
     * Starts with neutral feedback until a command succeeds or fails.
     */
    public ResultDisplay() {
        super(FXML);
        feedbackIcon.setImage(infoIcon);
        resultDisplay.setText("Ready. Choose Contacts, Events, or Tags, then enter a command.");
        feedbackScrollHint.visibleProperty().bind(Bindings.createBooleanBinding(() ->
                resultDisplay.getLayoutBounds().getHeight()
                        > feedbackMessageScroll.getViewportBounds().getHeight() + 1,
                resultDisplay.layoutBoundsProperty(), feedbackMessageScroll.viewportBoundsProperty()));
        feedbackScrollHint.managedProperty().bind(feedbackScrollHint.visibleProperty());
    }

    /**
     * Displays successful command feedback with the success status and information icon.
     */
    public void setFeedbackToUser(String feedbackToUser) {
        requireNonNull(feedbackToUser);
        resultDisplay.setText(feedbackToUser);
        feedbackMessageScroll.setVvalue(0);
        feedbackPane.pseudoClassStateChanged(ERROR, false);
        feedbackPane.pseudoClassStateChanged(SUCCESS, true);
        feedbackIcon.setImage(infoIcon);
        feedbackStatus.setText("OK");
    }

    /**
     * Marks feedback as rejected while preserving the command for correction.
     */
    public void setErrorFeedback(String message) {
        resultDisplay.setText(requireNonNull(message));
        feedbackMessageScroll.setVvalue(0);
        feedbackPane.pseudoClassStateChanged(ERROR, true);
        feedbackPane.pseudoClassStateChanged(SUCCESS, false);
        feedbackIcon.setImage(errorIcon);
        feedbackStatus.setText("Rejected");
    }

    /**
     * Reports that a session-only dismissal could not be saved.
     */
    public void setWarningFeedback(String message) {
        setErrorFeedback(message);
        feedbackStatus.setText("Not saved");
    }

}
