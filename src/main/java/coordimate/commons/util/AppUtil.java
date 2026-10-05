package coordimate.commons.util;

import static java.util.Objects.requireNonNull;

import coordimate.MainApp;
import javafx.scene.image.Image;

/**
 * A container for app-specific utility functions.
 */
public class AppUtil {

    /**
     * Gets an {@code Image} from the specified path.
     */
    public static Image getImage(String imagePath) {
        requireNonNull(imagePath);
        return new Image(MainApp.class.getResourceAsStream(imagePath));
    }

    /**
     * Checks that {@code isConditionMet} is true. Used for validating arguments to methods.
     *
     * @throws IllegalArgumentException if {@code isConditionMet} is false.
     */
    public static void checkArgument(Boolean isConditionMet) {
        if (!isConditionMet) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Checks that {@code isConditionMet} is true. Used for validating arguments to methods.
     *
     * @throws IllegalArgumentException with {@code errorMessage} if {@code isConditionMet} is false.
     */
    public static void checkArgument(Boolean isConditionMet, String errorMessage) {
        if (!isConditionMet) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
