package coordimate;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.logging.Logger;

import coordimate.commons.core.LogsCenter;
import coordimate.commons.exceptions.DataLoadingException;
import coordimate.commons.util.StringUtil;
import coordimate.logic.Logic;
import coordimate.logic.LogicManager;
import coordimate.model.CoordiMate;
import coordimate.model.Model;
import coordimate.model.ModelManager;
import coordimate.model.ReadOnlyCoordiMate;
import coordimate.model.ReadOnlyUserPrefs;
import coordimate.model.UserPrefs;
import coordimate.model.util.SampleDataUtil;
import coordimate.storage.JsonCoordiMateStorage;
import coordimate.storage.JsonUserPrefsStorage;
import coordimate.storage.Storage;
import coordimate.storage.StorageManager;
import coordimate.ui.Ui;
import coordimate.ui.UiManager;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Runs the application.
 */
public class MainApp extends Application {

    public static final String VERSION = "V0.5.1";

    private static final Logger logger = LogsCenter.getLogger(MainApp.class);
    private static final Path USER_PREFS_FILE_PATH = Paths.get("preferences.json");
    private static final Path COORDIMATE_FILE_PATH = Paths.get("data", "coordimate.json");

    protected Ui ui;
    protected Logic logic;
    protected Storage storage;
    protected Model model;

    private final Path userPrefsFilePath;
    private final Path coordiMateFilePath;

    /**
     * Creates an application using the default data and preference file paths.
     */
    public MainApp() {
        this(USER_PREFS_FILE_PATH, COORDIMATE_FILE_PATH);
    }

    /**
     * Creates a {@code MainApp} that stores its data at the given file paths.
     */
    MainApp(Path userPrefsFilePath, Path coordiMateFilePath) {
        this.userPrefsFilePath = userPrefsFilePath;
        this.coordiMateFilePath = coordiMateFilePath;
    }

    @Override
    public void init() throws Exception {
        logger.info("=============================[ Initializing CoordiMate ]===========================");
        super.init();

        JsonUserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(userPrefsFilePath);
        UserPrefs userPrefs = initPrefs(userPrefsStorage);
        JsonCoordiMateStorage coordiMateStorage = new JsonCoordiMateStorage(coordiMateFilePath);
        storage = new StorageManager(coordiMateStorage, userPrefsStorage);

        model = initModelManager(storage, userPrefs);

        logic = new LogicManager(model, storage);

        ui = new UiManager(logic, storage.getCoordiMateFilePath());
    }

    /**
     * Returns a {@code ModelManager} with the data from {@code storage}'s CoordiMate and {@code userPrefs}. <br>
     * The data from the sample CoordiMate will be used instead if {@code storage}'s CoordiMate is not found,
     * or an empty CoordiMate will be used instead if errors occur when reading {@code storage}'s CoordiMate.
     */
    private Model initModelManager(Storage storage, ReadOnlyUserPrefs userPrefs) {
        logger.info("Using data file : " + storage.getCoordiMateFilePath());

        ReadOnlyCoordiMate initialData;
        try {
            Optional<ReadOnlyCoordiMate> coordiMateOptional = storage.readCoordiMate();
            if (coordiMateOptional.isEmpty()) {
                logger.info("Creating a new data file " + storage.getCoordiMateFilePath()
                        + " populated with a sample CoordiMate.");
            }
            initialData = coordiMateOptional.orElseGet(SampleDataUtil::getSampleCoordiMate);
        } catch (DataLoadingException e) {
            logger.warning("Data file at " + storage.getCoordiMateFilePath() + " could not be loaded."
                    + " Will be starting with an empty CoordiMate.");
            initialData = new CoordiMate();
        }

        return new ModelManager(initialData, userPrefs);
    }

    /**
     * Returns a {@code UserPrefs} using the file at {@code storage}'s user prefs file path,
     * or a new {@code UserPrefs} with default configuration if errors occur when
     * reading from the file.
     */
    protected UserPrefs initPrefs(JsonUserPrefsStorage storage) {
        Path prefsFilePath = storage.getUserPrefsFilePath();
        logger.info("Using preference file : " + prefsFilePath);

        UserPrefs initializedPrefs;
        try {
            Optional<UserPrefs> prefsOptional = storage.readUserPrefs();
            if (prefsOptional.isEmpty()) {
                logger.info("Creating new preference file " + prefsFilePath);
            }
            initializedPrefs = prefsOptional.orElse(new UserPrefs());
        } catch (DataLoadingException e) {
            logger.warning("Preference file at " + prefsFilePath + " could not be loaded."
                    + " Using default preferences.");
            initializedPrefs = new UserPrefs();
        }

        //Update prefs file in case it was missing to begin with or there are new/unused fields
        try {
            storage.saveUserPrefs(initializedPrefs);
        } catch (IOException e) {
            logger.warning("Failed to save preference file : " + StringUtil.getDetails(e));
        }

        return initializedPrefs;
    }

    @Override
    public void start(Stage primaryStage) {
        logger.info("Starting CoordiMate " + MainApp.VERSION);
        ui.start(primaryStage);
    }

    @Override
    public void stop() {
        logger.info("============================ [ Stopping CoordiMate ] =============================");
        try {
            storage.saveUserPrefs(model.getUserPrefs());
        } catch (IOException e) {
            logger.severe("Failed to save preferences " + StringUtil.getDetails(e));
        }
    }
}
