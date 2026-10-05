package coordimate.ui;

import static java.util.Objects.requireNonNull;

import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
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

    /**
     * Starts with neutral feedback until a command succeeds or fails.
     */
    public ResultDisplay() {
        super(FXML);
        feedbackIcon.setImage(infoIcon);
        resultDisplay.setText("Ready. Choose Contacts or Events, then enter a command.");
    }

    public void setFeedbackToUser(String feedbackToUser) {
        requireNonNull(feedbackToUser);
        resultDisplay.setText(feedbackToUser);
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
        feedbackPane.pseudoClassStateChanged(ERROR, true);
        feedbackPane.pseudoClassStateChanged(SUCCESS, false);
        feedbackIcon.setImage(errorIcon);
        feedbackStatus.setText("Rejected");
    }

}
