package coordimate.ui;

import java.awt.Point;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import coordimate.commons.core.GuiSettings;
import coordimate.commons.core.LogsCenter;
import coordimate.logic.Logic;
import coordimate.logic.commands.AddEventCommand;
import coordimate.logic.commands.CommandResult;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.logic.reminder.ReminderDismissalException;
import coordimate.model.event.Event;
import coordimate.model.person.Person;
import coordimate.model.tag.Tag;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.AccessibleRole;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

/**
 * The Main Window. Provides the basic application layout containing
 * a menu bar and space where other JavaFX elements can be placed.
 */
public class MainWindow extends UiPart<Stage> {

    private static final String FXML = "MainWindow.fxml";
    private static final Map<String, String> COMMAND_HELP_TEXT = Map.ofEntries(
            Map.entry("add", "Format: add n/NAME p/PHONE e/EMAIL r/ROLE [b/BIRTHDAY] [a/ADDRESS]"
                    + " [o/ORGANISATION] [t/TAG]... [m/NOTE]\n"
                    + "Example: add n/Aisha Tan p/+6591234567 e/aisha@example.com r/Logistics t/EXCO"),
            Map.entry("edit", "Format: edit INDEX [n/NAME] [p/PHONE] [e/EMAIL] [r/ROLE] [b/BIRTHDAY]"
                    + " [a/ADDRESS] [o/ORGANISATION] [m/NOTE] [t/TAG]... [at/TAG]... [rt/TAG]...\n"
                    + "Or: edit target/IDENTIFIER [n/NAME] [p/PHONE] [e/EMAIL] [r/ROLE] [b/BIRTHDAY]"
                    + " [a/ADDRESS] [o/ORGANISATION] [m/NOTE] [t/TAG]... [at/TAG]... [rt/TAG]...\n"
                    + "Example: edit 1 p/91234567 e/johndoe@example.com"),
            Map.entry("delete", "Format: delete INDEX (or n/NAME, p/PHONE, or e/EMAIL)\n"
                    + "Example: delete e/aisha@example.com"),
            Map.entry("search", "Format: search KEYWORD\nExample: search jurong west"),
            Map.entry("filter", "Format: filter FIELD/VALUE [FIELD/VALUE]...\n"
                    + "Example: filter tag/friend organisation/NUS"),
            Map.entry("list", "Format: list\nExample: list"),
            Map.entry("addevent", "Format: addevent evn/EVENT_NAME st/START_TIME et/END_TIME\n"
                    + "Example: addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00"),
            Map.entry("editevent", "Format: editevent evn/EVENT_NAME [nevn/NEW_EVENT_NAME]"
                    + " [st/NEW_START_TIME] [et/NEW_END_TIME]\n"
                    + "Provide at least one field to edit.\n"
                    + "Example: editevent evn/Final Concert nevn/Grand Concert"),
            Map.entry("deleteevent", "Format: deleteevent evn/EVENT_NAME\n"
                    + "Example: deleteevent evn/Final Concert"),
            Map.entry("assign", "Format: assign evn/EVENT_NAME c/CONTACT_INDEX [MORE_CONTACT_INDEXES]...\n"
                    + "Example: assign evn/Final Concert c/2 3"),
            Map.entry("unassign", "Format: unassign evn/EVENT_NAME c/CONTACT_INDEX [MORE_CONTACT_INDEXES]...\n"
                    + "Example: unassign evn/Final Concert c/2 3"),
            Map.entry("members", "Format: members evn/EVENT_NAME\nExample: members evn/Final Concert"),
            Map.entry("markattendance", "Format: markattendance evn/EVENT_NAME mem/MEMBER_NAME att/STATUS\n"
                    + "Example: markattendance evn/Final Concert mem/Alex Yeoh att/present"),
            Map.entry("listtags", "Format: listtags\nExample: listtags"),
            Map.entry("addtag", "Format: addtag t/TAG\nExample: addtag t/ProductionCrew"),
            Map.entry("edittag", "Format: edittag OLD_TAG t/NEW_TAG\n"
                    + "Example: edittag Production t/ProductionCrew"),
            Map.entry("deletetag", "Format: deletetag t/TAG\nExample: deletetag t/ProductionCrew"));

    private final Logger logger = LogsCenter.getLogger(getClass());

    private Stage primaryStage;
    private Logic logic;
    private Path dataFilePath;
    private Rectangle2D normalWindowBounds;
    private boolean isNormalBoundsUpdateScheduled;

    // Independent Ui parts residing in this Ui container
    private PersonListPanel personListPanel;
    private EventListPanel eventListPanel;
    private ResultDisplay resultDisplay;
    private HelpWindow helpWindow;
    private CommandBox commandBox;
    private ReminderPanel reminderPanel;
    private final Timeline reminderTimer;

    @FXML
    private StackPane reminderPanelPlaceholder;

    @FXML
    private StackPane commandBoxPlaceholder;

    @FXML
    private MenuItem helpMenuItem;

    @FXML
    private StackPane personListPanelPlaceholder;

    @FXML
    private StackPane eventListPanelPlaceholder;

    @FXML
    private ListView<String> tagListView;

    @FXML
    private Label selectedTagName;

    @FXML
    private Label tagMemberCount;

    @FXML
    private ListView<Person> tagMemberListView;

    @FXML
    private SplitPane tagSplitPane;

    @FXML
    private VBox contactsCommands;

    @FXML
    private VBox eventsCommands;

    @FXML
    private VBox tagsCommands;

    @FXML
    private TextFlow prefixHelp;

    @FXML
    private TabPane views;

    @FXML
    private Tab contactsTab;

    @FXML
    private Tab eventsTab;

    @FXML
    private Tab tagsTab;

    @FXML
    private ToggleButton contactsView;

    @FXML
    private ToggleButton eventsView;

    @FXML
    private ToggleButton tagsView;

    @FXML
    private StackPane resultDisplayPlaceholder;

    @FXML
    private StackPane statusbarPlaceholder;

    /**
     * Creates a {@code MainWindow} with the given {@code Stage}, {@code Logic},
     * and the data file path to show in the status bar.
     */
    public MainWindow(Stage primaryStage, Logic logic, Path dataFilePath) {
        this(primaryStage, logic, dataFilePath, new Timeline());
    }

    MainWindow(Stage primaryStage, Logic logic, Path dataFilePath, Timeline reminderTimer) {
        super(FXML, primaryStage);

        // Set dependencies
        this.primaryStage = primaryStage;
        this.logic = logic;
        this.dataFilePath = dataFilePath;
        this.reminderTimer = reminderTimer;

        // Configure the UI
        setWindowDefaultSize(logic.getGuiSettings());
        primaryStage.xProperty().addListener(observable -> scheduleNormalWindowBoundsUpdate());
        primaryStage.yProperty().addListener(observable -> scheduleNormalWindowBoundsUpdate());
        primaryStage.widthProperty().addListener(observable -> scheduleNormalWindowBoundsUpdate());
        primaryStage.heightProperty().addListener(observable -> scheduleNormalWindowBoundsUpdate());

        setAccelerators();

        helpWindow = new HelpWindow();
        primaryStage.addEventHandler(WindowEvent.WINDOW_HIDDEN, event -> stop());
        primaryStage.addEventHandler(WindowEvent.WINDOW_SHOWN, event -> {
            if (reminderPanel != null) {
                logic.refreshReminders();
                reminderTimer.play();
            }
        });
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    private void setAccelerators() {
        setAccelerator(helpMenuItem, KeyCombination.valueOf("F1"));
    }

    /**
     * Sets the accelerator of a MenuItem.
     *
     * @param menuItem The menu item whose accelerator is assigned.
     * @param keyCombination the KeyCombination value of the accelerator.
     */
    private void setAccelerator(MenuItem menuItem, KeyCombination keyCombination) {
        menuItem.setAccelerator(keyCombination);

        /*
         * TODO: the code below can be removed once the bug reported here
         * https://bugs.openjdk.java.net/browse/JDK-8131666
         * is fixed in a later version of the SDK.
         *
         * According to the bug report, TextInputControl (TextField, TextArea) will
         * consume function-key events. Because CommandBox contains a TextField and
         * ResultDisplay contains a TextArea, some accelerators (e.g., F1) will
         * not work when the focus is in them because the key event is consumed by
         * the TextInputControl(s).
         *
         * For now, we add the following event filter to capture such key events and open
         * the help window purposely so as to support accelerators even when focus is
         * in CommandBox or ResultDisplay.
         */
        getRoot().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getTarget() instanceof TextInputControl && keyCombination.match(event)) {
                menuItem.getOnAction().handle(new ActionEvent());
                event.consume();
            }
        });
    }

    /**
     * Fills up all the placeholders of this window.
     */
    void fillInnerParts() {
        personListPanel = new PersonListPanel(logic.getFilteredPersonList(), logic.getEventList());
        personListPanelPlaceholder.getChildren().add(personListPanel.getRoot());

        eventListPanel = new EventListPanel(logic.getEventList(), logic.getFilteredPersonList());
        eventListPanelPlaceholder.getChildren().add(eventListPanel.getRoot());
        TagListViewModel tagListViewModel = new TagListViewModel(logic.getTagList(), logic.getPersonList());
        tagListView.setItems(tagListViewModel.getTagNames());
        tagMemberListView.setItems(tagListViewModel.getMembers());
        tagMemberListView.setCellFactory(list -> new TagMemberListCell());
        tagMemberCount.textProperty().bind(tagListViewModel.memberCountTextProperty());
        tagMemberListView.setPlaceholder(new Label("Select a tag to view its members."));
        tagListView.getSelectionModel().selectedItemProperty().addListener((observable, oldTag, tag) -> {
            selectedTagName.setText(tag == null ? "Select a tag" : tag);
            tagListViewModel.selectTag(tag);
            tagMemberListView.setPlaceholder(new Label(tag == null
                    ? "Select a tag to view its members." : "No contacts use this tag."));
        });
        tagSplitPane.widthProperty().addListener((observable, oldWidth, width) ->
                tagSplitPane.setOrientation(width.doubleValue() < 650
                        ? Orientation.VERTICAL : Orientation.HORIZONTAL));
        contactsCommands.visibleProperty().bind(contactsView.selectedProperty());
        contactsCommands.managedProperty().bind(contactsCommands.visibleProperty());
        eventsCommands.visibleProperty().bind(eventsView.selectedProperty());
        eventsCommands.managedProperty().bind(eventsCommands.visibleProperty());
        tagsCommands.visibleProperty().bind(tagsView.selectedProperty());
        tagsCommands.managedProperty().bind(tagsCommands.visibleProperty());
        contactsTab.textProperty().bind(Bindings.size(logic.getFilteredPersonList()).asString("Contacts (%d)"));
        eventsTab.textProperty().bind(Bindings.size(logic.getEventList()).asString("Events (%d)"));
        contactsView.textProperty().bind(contactsTab.textProperty());
        eventsView.textProperty().bind(eventsTab.textProperty());
        tagsView.textProperty().bind(tagListViewModel.tagCountTextProperty());
        views.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, tab) -> {
            contactsView.setSelected(tab == contactsTab);
            eventsView.setSelected(tab == eventsTab);
            tagsView.setSelected(tab == tagsTab);
            setPrefixHelpText(tab == eventsTab ? "Prefixes: evn/Event  st/Start  et/End  c/Contact"
                    : tab == tagsTab ? "Prefixes: t/Tag"
                    : "Prefixes: n/Name  p/Phone  e/Email  r/Role  t/Tag  m/Note");
        });

        resultDisplay = new ResultDisplay();
        resultDisplayPlaceholder.getChildren().add(resultDisplay.getRoot());

        reminderPanel = new ReminderPanel(logic.getReminders(), this::dismissReminder);
        reminderPanelPlaceholder.getChildren().add(reminderPanel.getRoot());
        reminderTimer.getKeyFrames().setAll(new KeyFrame(Duration.minutes(1), event -> logic.refreshReminders()));
        reminderTimer.setCycleCount(Animation.INDEFINITE);
        if (primaryStage.isShowing()) {
            reminderTimer.play();
        }

        StatusBarFooter statusBarFooter = new StatusBarFooter(dataFilePath);
        statusbarPlaceholder.getChildren().add(statusBarFooter.getRoot());

        commandBox = new CommandBox(this::executeCommand, logic::isAwaitingDeleteConfirmation);
        commandBoxPlaceholder.getChildren().add(commandBox.getRoot());
        makeCommandRowsInteractive(contactsCommands);
        makeCommandRowsInteractive(eventsCommands);
        makeCommandRowsInteractive(tagsCommands);
    }

    private void makeCommandRowsInteractive(VBox commandList) {
        for (Node child : commandList.getChildren()) {
            if (!(child instanceof HBox row) || !row.getStyleClass().contains("cheat-row")) {
                continue;
            }
            String command = ((Label) row.getChildren().get(0)).getText();
            row.setAccessibleRole(AccessibleRole.BUTTON);
            row.setAccessibleText("Insert " + command + " command");
            row.setFocusTraversable(true);
            row.setCursor(Cursor.HAND);
            row.setOnMouseClicked(event -> selectCommand(command));
            row.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                    selectCommand(command);
                    event.consume();
                }
            });
        }
    }

    private void selectCommand(String command) {
        String commandHelp = COMMAND_HELP_TEXT.get(command);
        if (commandHelp != null) {
            commandBox.setCommand(command);
            setPrefixHelpText(commandHelp);
        }
    }

    private void setPrefixHelpText(String text) {
        Text hint = new Text(text);
        hint.getStyleClass().add("command-hint");
        prefixHelp.getChildren().setAll(hint);
        prefixHelp.setAccessibleText(text);
    }

    private String getAvatarColorClass(Person person) {
        if (person.getTags().stream().anyMatch(tag -> tag.getTagName().equalsIgnoreCase("logistics"))) {
            return "event-member-avatar-teal";
        }
        if (person.getTags().stream().anyMatch(tag -> tag.getTagName().equalsIgnoreCase("sponsor"))) {
            return "event-member-avatar-amber";
        }
        return "event-member-avatar-purple";
    }

    private String initials(String fullName) {
        String[] parts = fullName.strip().split("\\s+");
        String result = parts[0].substring(0, 1);
        if (parts.length > 1) {
            result += parts[parts.length - 1].substring(0, 1);
        }
        return result.toUpperCase(Locale.ROOT);
    }

    private String formatMemberDetails(Person person) {
        StringBuilder details = new StringBuilder(person.getRole().getValue());
        person.getOrganisation().ifPresent(organisation -> details.append(" · ").append(organisation.getValue()));
        return details.toString();
    }

    private class TagMemberListCell extends ListCell<Person> {
        @Override
        protected void updateItem(Person person, boolean empty) {
            super.updateItem(person, empty);
            setText(null);
            if (empty || person == null) {
                setGraphic(null);
                return;
            }

            Label avatar = new Label(initials(person.getName().getFullName()));
            avatar.getStyleClass().addAll("event-member-avatar", getAvatarColorClass(person));

            Label name = new Label(person.getName().getFullName());
            name.setWrapText(true);
            name.setMinWidth(0);
            name.getStyleClass().add("event-member-name");

            FlowPane identityLine = new FlowPane(8, 4);
            identityLine.setAlignment(Pos.CENTER_LEFT);
            identityLine.getStyleClass().add("event-member-identity");
            identityLine.getChildren().add(name);
            person.getTags().stream()
                    .sorted((left, right) -> String.CASE_INSENSITIVE_ORDER.compare(
                            left.getTagName(), right.getTagName()))
                    .map(Tag::getTagName)
                    .map(TagStyle::createTagLabel)
                    .forEach(identityLine.getChildren()::add);

            Label subtitle = new Label(formatMemberDetails(person));
            subtitle.setWrapText(true);
            subtitle.setMinWidth(0);
            subtitle.getStyleClass().add("event-member-metadata");

            VBox details = new VBox(4, identityLine, subtitle);
            details.getStyleClass().add("event-member-details");
            HBox.setHgrow(details, Priority.ALWAYS);

            HBox row = new HBox(12, avatar, details);
            row.getStyleClass().add("tag-member-row");
            setGraphic(row);
        }
    }

    /**
     * Sets the default size based on {@code guiSettings}.
     */
    private void setWindowDefaultSize(GuiSettings guiSettings) {
        Screen screen = getTargetScreen(guiSettings.getWindowCoordinates());
        Rectangle2D screenBounds = screen.getVisualBounds();
        Rectangle2D windowBounds = WindowBounds.fitToScreen(guiSettings, screenBounds);

        primaryStage.setMinWidth(Math.min(WindowBounds.PREFERRED_MIN_WIDTH, screenBounds.getWidth()));
        primaryStage.setMinHeight(Math.min(WindowBounds.PREFERRED_MIN_HEIGHT, screenBounds.getHeight()));
        primaryStage.setWidth(windowBounds.getWidth());
        primaryStage.setHeight(windowBounds.getHeight());
        primaryStage.setX(windowBounds.getMinX());
        primaryStage.setY(windowBounds.getMinY());
        normalWindowBounds = windowBounds;

        Point savedCoordinates = guiSettings.getWindowCoordinates();
        if (WindowBounds.wereAdjusted(guiSettings, windowBounds)) {
            logger.info("Adjusted saved window bounds from [coordinates=" + savedCoordinates
                    + ", width=" + guiSettings.getWindowWidth() + ", height=" + guiSettings.getWindowHeight()
                    + "] to fit the available screen area: " + windowBounds);
        }
    }

    private Screen getTargetScreen(Point savedCoordinates) {
        if (savedCoordinates == null) {
            return Screen.getPrimary();
        }
        List<Screen> screens = Screen.getScreensForRectangle(
                savedCoordinates.getX(), savedCoordinates.getY(), 1, 1);
        return getScreenOrPrimary(screens,
                "Saved window position is outside all connected screens; using the primary screen.");
    }

    /**
     * Opens the help window or focuses on it if it's already opened.
     */
    @FXML
    public void handleHelp() {
        if (!helpWindow.isShowing()) {
            helpWindow.show();
        } else {
            helpWindow.focus();
        }
    }

    void show() {
        boolean isMaximized = logic.getGuiSettings().isWindowMaximized();
        logger.fine("Restoring window with maximized state: " + isMaximized);
        primaryStage.setMaximized(isMaximized);
        primaryStage.show();
    }

    /**
     * Closes the application.
     */
    @FXML
    private void handleExit() {
        boolean isMaximized = primaryStage.isMaximized();
        if (isMaximized) {
            moveNormalWindowBoundsToCurrentScreen();
        } else {
            rememberNormalWindowBounds();
        }
        GuiSettings guiSettings = new GuiSettings(normalWindowBounds.getWidth(), normalWindowBounds.getHeight(),
                (int) normalWindowBounds.getMinX(), (int) normalWindowBounds.getMinY(), isMaximized);
        logger.fine("Saving window with maximized state: " + isMaximized);
        logic.setGuiSettings(guiSettings);
        stop();
        helpWindow.hide();
        primaryStage.hide();
    }

    private void moveNormalWindowBoundsToCurrentScreen() {
        Point normalCoordinates = new Point((int) normalWindowBounds.getMinX(), (int) normalWindowBounds.getMinY());
        Screen normalScreen = getTargetScreen(normalCoordinates);
        Screen currentScreen = getCurrentScreen();
        normalWindowBounds = WindowBounds.moveToScreen(normalWindowBounds,
                normalScreen.getVisualBounds(), currentScreen.getVisualBounds());
        logger.fine("Aligned saved normal window bounds with the screen where the maximized window was closed: "
                + normalWindowBounds);
    }

    private Screen getCurrentScreen() {
        List<Screen> screens = Screen.getScreensForRectangle(primaryStage.getX(), primaryStage.getY(),
                primaryStage.getWidth(), primaryStage.getHeight());
        return getScreenOrPrimary(screens,
                "Current window position is outside all connected screens; using the primary screen.");
    }

    private Screen getScreenOrPrimary(List<Screen> screens, String fallbackWarning) {
        if (screens.isEmpty()) {
            logger.warning(fallbackWarning);
        }
        return screens.isEmpty() ? Screen.getPrimary() : screens.getFirst();
    }

    private void rememberNormalWindowBounds() {
        normalWindowBounds = new Rectangle2D(primaryStage.getX(), primaryStage.getY(),
                primaryStage.getWidth(), primaryStage.getHeight());
    }

    void scheduleNormalWindowBoundsUpdate() {
        if (!primaryStage.isShowing() || isNormalBoundsUpdateScheduled) {
            return;
        }
        isNormalBoundsUpdateScheduled = true;
        Platform.runLater(() -> {
            isNormalBoundsUpdateScheduled = false;
            if (!primaryStage.isMaximized()) {
                rememberNormalWindowBounds();
            }
        });
    }

    private void dismissReminder(Event reminder) {
        try {
            logic.dismissReminder(reminder);
            resultDisplay.setFeedbackToUser("Reminder dismissed: " + reminder.getName() + ".");
        } catch (ReminderDismissalException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            resultDisplay.setWarningFeedback(e.getMessage());
        }
    }

    /**
     * Stops periodic checks and releases the reminder panel's subscription.
     */
    void stop() {
        reminderTimer.stop();
        if (reminderPanel != null) {
            reminderPanel.close();
        }
    }

    public PersonListPanel getPersonListPanel() {
        return personListPanel;
    }

    /**
     * Selects the contacts tab and its view button.
     */
    @FXML
    private void showContacts() {
        views.getSelectionModel().select(contactsTab);
        contactsView.setSelected(true);
    }

    /**
     * Selects the events tab and its view button.
     */
    @FXML
    private void showEvents() {
        views.getSelectionModel().select(eventsTab);
        eventsView.setSelected(true);
    }

    /**
     * Selects the tags tab and its view button.
     */
    @FXML
    private void showTags() {
        views.getSelectionModel().select(tagsTab);
        tagsView.setSelected(true);
    }

    /**
     * Executes the command and returns the result.
     *
     * @see coordimate.logic.Logic#execute(String)
     */
    private CommandResult executeCommand(String commandText) throws CommandException, ParseException {
        try {
            CommandResult commandResult = logic.execute(commandText);
            logger.info("Result: " + commandResult.getFeedbackToUser());
            resultDisplay.setFeedbackToUser(commandResult.getFeedbackToUser());

            if (commandText.strip().split("\\s+", 2)[0].equals(AddEventCommand.COMMAND_WORD)) {
                views.getSelectionModel().select(eventsTab);
                eventListPanel.selectNewestEvent();
            }

            if (commandResult.shouldShowHelp()) {
                handleHelp();
            }

            switch (commandResult.getViewToShow()) {
                case CONTACTS -> showContacts();
                case EVENTS -> showEvents();
                case TAGS -> showTags();
                default -> { } // NONE keeps the selected view.
            }

            if (commandResult.shouldExit()) {
                handleExit();
            }

            return commandResult;
        } catch (CommandException | ParseException e) {
            logger.info("An error occurred while executing command: " + commandText);
            resultDisplay.setErrorFeedback(e.getMessage());
            throw e;
        }
    }
}
