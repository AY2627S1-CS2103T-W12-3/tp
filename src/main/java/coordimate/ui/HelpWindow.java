package coordimate.ui;

import java.util.List;
import java.util.logging.Logger;

import coordimate.commons.core.LogsCenter;
import coordimate.logic.commands.AddCommand;
import coordimate.logic.commands.AddEventCommand;
import coordimate.logic.commands.AssignCommand;
import coordimate.logic.commands.ClearCommand;
import coordimate.logic.commands.CreateTagCommand;
import coordimate.logic.commands.DeleteCommand;
import coordimate.logic.commands.DeleteEventCommand;
import coordimate.logic.commands.DeleteTagCommand;
import coordimate.logic.commands.DismissCommand;
import coordimate.logic.commands.EditCommand;
import coordimate.logic.commands.EditEventCommand;
import coordimate.logic.commands.EditTagCommand;
import coordimate.logic.commands.ExitCommand;
import coordimate.logic.commands.FilterCommand;
import coordimate.logic.commands.HelpCommand;
import coordimate.logic.commands.ListCommand;
import coordimate.logic.commands.ListTagsCommand;
import coordimate.logic.commands.MarkAttendanceCommand;
import coordimate.logic.commands.MembersCommand;
import coordimate.logic.commands.SearchCommand;
import coordimate.logic.commands.UnassignCommand;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 * Controller for a help page.
 */
public class HelpWindow extends UiPart<Stage> {

    public static final String USERGUIDE_URL = "https://ay2627s1-cs2103t-w12-3.github.io/tp/";
    public static final String HELP_SHORTCUT = "Fn + F1";
    public static final String HELP_MESSAGE = "Keyboard shortcut: " + HELP_SHORTCUT
            + ". Refer to the user guide: " + USERGUIDE_URL;

    static final List<CommandUsage> COMMAND_USAGES = List.of(
            commandUsage(AddCommand.COMMAND_WORD,
                    "n/NAME p/PHONE e/EMAIL r/ROLE [b/BIRTHDAY] [a/ADDRESS] [o/ORGANISATION] [lv/LEAVE_END_DATE] "
                            + "[t/TAG]... [m/NOTE]"),
            commandUsage(AddEventCommand.COMMAND_WORD, "evn/EVENT_NAME st/START_TIME et/END_TIME"),
            commandUsage(CreateTagCommand.COMMAND_WORD, "t/TAG"),
            commandUsage(AssignCommand.COMMAND_WORD,
                    "evn/EVENT_NAME c/MEMBER_ID [MORE_MEMBER_IDS]..."),
            commandUsage(ClearCommand.COMMAND_WORD, ""),
            commandUsage(DeleteCommand.COMMAND_WORD,
                    "MEMBER_ID | delete n/NAME | delete p/PHONE | delete e/EMAIL"),
            commandUsage(DeleteEventCommand.COMMAND_WORD, "evn/EVENT_NAME"),
            commandUsage(DeleteTagCommand.COMMAND_WORD, "t/TAG"),
            commandUsage(DismissCommand.COMMAND_WORD, "INDEX"),
            commandUsage(EditCommand.COMMAND_WORD,
                    "(MEMBER_ID | target/IDENTIFIER) [n/NAME] [p/PHONE] [e/EMAIL] [r/ROLE] [b/BIRTHDAY] "
                            + "[a/ADDRESS] [o/ORGANISATION] [m/NOTE] [lv/LEAVE_END_DATE] [t/TAG]... [at/TAG]... "
                            + "[rt/TAG]..."),
            commandUsage(EditEventCommand.COMMAND_WORD,
                    "evn/EVENT_NAME [nevn/NEW_EVENT_NAME] [st/NEW_START_TIME] [et/NEW_END_TIME]"),
            commandUsage(EditTagCommand.COMMAND_WORD, "OLD_TAG t/NEW_TAG"),
            commandUsage(ExitCommand.COMMAND_WORD, ""),
            commandUsage(FilterCommand.COMMAND_WORD, "FIELD/VALUE [FIELD/VALUE]..."),
            commandUsage(HelpCommand.COMMAND_WORD, ""),
            commandUsage(ListCommand.COMMAND_WORD, ""),
            commandUsage(ListTagsCommand.COMMAND_WORD, ""),
            commandUsage(MarkAttendanceCommand.COMMAND_WORD,
                    "evn/EVENT_NAME mem/MEMBER_NAME att/STATUS"),
            commandUsage(MembersCommand.COMMAND_WORD, "evn/EVENT_NAME"),
            commandUsage(SearchCommand.COMMAND_WORD, "KEYWORD"),
            commandUsage(UnassignCommand.COMMAND_WORD,
                    "evn/EVENT_NAME c/MEMBER_ID [MORE_MEMBER_IDS]..."));

    private static final Logger logger = LogsCenter.getLogger(HelpWindow.class);
    private static final String FXML = "HelpWindow.fxml";

    @FXML
    private Button copyButton;

    @FXML
    private Button closeButton;

    @FXML
    private Label helpMessage;

    @FXML
    private TableView<CommandUsage> commandTable;

    @FXML
    private TableColumn<CommandUsage, String> commandColumn;

    @FXML
    private TableColumn<CommandUsage, String> usageColumn;

    /**
     * Creates a new HelpWindow.
     *
     * @param root Stage to use as the root of the HelpWindow.
     */
    public HelpWindow(Stage root) {
        super(FXML, root);
        helpMessage.setText(HELP_MESSAGE);
        copyButton.setMinWidth(Region.USE_PREF_SIZE);
        closeButton.setMinWidth(Region.USE_PREF_SIZE);
        commandColumn.setCellValueFactory(usage -> new ReadOnlyStringWrapper(usage.getValue().command()));
        usageColumn.setCellValueFactory(usage -> new ReadOnlyStringWrapper(usage.getValue().format()));
        usageColumn.setCellFactory(column -> new WrappingUsageCell());
        commandTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        commandTable.getItems().setAll(COMMAND_USAGES);
    }

    /**
     * Creates a new HelpWindow.
     */
    public HelpWindow() {
        this(new Stage());
    }

    /**
     * Shows the help window.
     *
     * @throws IllegalStateException
     *     <ul>
     *         <li>
     *             if this method is called on a thread other than the JavaFX Application Thread.
     *         </li>
     *         <li>
     *             if this method is called during animation or layout processing.
     *         </li>
     *         <li>
     *             if this method is called on the primary stage.
     *         </li>
     *         <li>
     *             if {@code dialogStage} is already showing.
     *         </li>
     *     </ul>
     */
    public void show() {
        logger.fine("Showing help page about the application.");
        getRoot().show();
        getRoot().centerOnScreen();
    }

    /**
     * Returns true if the help window is currently being shown.
     */
    public boolean isShowing() {
        return getRoot().isShowing();
    }

    /**
     * Hides the help window.
     */
    public void hide() {
        getRoot().hide();
    }

    /**
     * Focuses on the help window.
     */
    public void focus() {
        getRoot().requestFocus();
    }

    /**
     * Copies the URL to the user guide to the clipboard.
     */
    @FXML
    private void copyUrl() {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent url = new ClipboardContent();
        url.putString(USERGUIDE_URL);
        clipboard.setContent(url);
    }

    /**
     * Closes the help window.
     */
    @FXML
    private void close() {
        getRoot().close();
    }

    private static CommandUsage commandUsage(String commandWord, String parameters) {
        String separator = parameters.isEmpty() ? "" : " ";
        return new CommandUsage(commandWord, commandWord + separator + parameters);
    }

    private static class WrappingUsageCell extends TableCell<CommandUsage, String> {
        private static final double HORIZONTAL_PADDING = 16;
        private static final double VERTICAL_PADDING = 16;
        private final Text content = new Text();

        WrappingUsageCell() {
            content.getStyleClass().add("usage-text");
            content.wrappingWidthProperty().bind(widthProperty().subtract(HORIZONTAL_PADDING));
            prefHeightProperty().bind(Bindings.createDoubleBinding(() ->
                    content.getLayoutBounds().getHeight() + VERTICAL_PADDING,
                    content.layoutBoundsProperty()));
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override
        protected void updateItem(String usage, boolean empty) {
            super.updateItem(usage, empty);
            if (empty) {
                content.setText("");
                setGraphic(null);
                return;
            }
            content.setText(usage);
            setGraphic(content);
        }
    }

    record CommandUsage(String command, String format) {}
}
