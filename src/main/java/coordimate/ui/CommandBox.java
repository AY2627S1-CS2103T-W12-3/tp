package coordimate.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import coordimate.logic.commands.CommandResult;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.logic.parser.exceptions.ParseException;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;

/**
 * The UI component that is responsible for receiving user command inputs.
 */
public class CommandBox extends UiPart<Region> {

    public static final String ERROR_STYLE_CLASS = "error";
    private static final String FXML = "CommandBox.fxml";

    private final CommandExecutor commandExecutor;
    private final BooleanSupplier isAwaitingDeleteConfirmation;
    private final List<String> commandHistory = new ArrayList<>();
    private int historyIndex;
    private String draft = "";

    @FXML
    private TextField commandTextField;

    @FXML
    private Button submitButton;

    /**
     * Creates a {@code CommandBox} with the given {@code CommandExecutor}.
     */
    public CommandBox(CommandExecutor commandExecutor, BooleanSupplier isAwaitingDeleteConfirmation) {
        super(FXML);
        this.commandExecutor = commandExecutor;
        this.isAwaitingDeleteConfirmation = isAwaitingDeleteConfirmation;
        // calls #setStyleToDefault() whenever there is a change to the text of the command box.
        commandTextField.textProperty().addListener((unused1, unused2, unused3) -> setStyleToDefault());
        commandTextField.addEventFilter(KeyEvent.KEY_PRESSED, this::handleHistoryNavigation);
    }

    /**
     * Handles the Enter button pressed event.
     */
    @FXML
    private void handleCommandEntered() {
        String commandText = commandTextField.getText();
        boolean isDeleteConfirmation = isAwaitingDeleteConfirmation.getAsBoolean();
        if (commandText.isBlank() && !isDeleteConfirmation) {
            return;
        }

        if (!isDeleteConfirmation) {
            commandHistory.add(commandText);
        }
        resetHistoryNavigation();

        try {
            commandExecutor.execute(commandText);
            commandTextField.setText("");
        } catch (CommandException | ParseException e) {
            setStyleToIndicateCommandFailure();
        }
    }

    /** Places a command word in the input, ready for the user to complete. */
    void setCommand(String command) {
        resetHistoryNavigation();
        commandTextField.setText(command);
        commandTextField.positionCaret(command.length());
        commandTextField.requestFocus();
    }

    private void handleHistoryNavigation(KeyEvent event) {
        if (event.getCode() != KeyCode.UP && event.getCode() != KeyCode.DOWN) {
            return;
        }

        if (!isAwaitingDeleteConfirmation.getAsBoolean()) {
            if (event.getCode() == KeyCode.UP) {
                showPreviousCommand();
            } else {
                showNextCommand();
            }
        }
        event.consume();
    }

    private void showPreviousCommand() {
        if (historyIndex == 0) {
            return;
        }
        if (historyIndex == commandHistory.size()) {
            draft = commandTextField.getText();
        }
        historyIndex--;
        showHistoryText(commandHistory.get(historyIndex));
    }

    private void showNextCommand() {
        if (historyIndex == commandHistory.size()) {
            return;
        }
        historyIndex++;
        showHistoryText(historyIndex == commandHistory.size() ? draft : commandHistory.get(historyIndex));
    }

    private void showHistoryText(String text) {
        commandTextField.setText(text);
        commandTextField.positionCaret(text.length());
    }

    private void resetHistoryNavigation() {
        historyIndex = commandHistory.size();
        draft = "";
    }

    /**
     * Sets the command box style to use the default style.
     */
    private void setStyleToDefault() {
        commandTextField.getStyleClass().remove(ERROR_STYLE_CLASS);
        submitButton.setText("Enter");
    }

    /**
     * Sets the command box style to indicate a failed command.
     */
    private void setStyleToIndicateCommandFailure() {
        ObservableList<String> styleClasses = commandTextField.getStyleClass();

        if (styleClasses.contains(ERROR_STYLE_CLASS)) {
            return;
        }

        styleClasses.add(ERROR_STYLE_CLASS);
        submitButton.setText("Retry");
    }

    /**
     * Represents a function that can execute commands.
     */
    @FunctionalInterface
    public interface CommandExecutor {
        /**
         * Executes the command and returns the result.
         *
         * @see coordimate.logic.Logic#execute(String)
         */
        CommandResult execute(String commandText) throws CommandException, ParseException;
    }

}
