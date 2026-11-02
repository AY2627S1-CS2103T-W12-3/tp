package coordimate.commons.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;

/**
 * Writes and reads files.
 */
public class FileUtil {

    private static final String CHARSET = "UTF-8";

    /**
     * Returns the folder containing the running JAR file, so that app files are kept next to the JAR
     * regardless of the terminal's current folder. Returns the current folder if the app is not running
     * from a JAR file (e.g. from an IDE or Gradle).
     */
    public static Path getHomeFolder() {
        return getHomeFolder(FileUtil.class.getProtectionDomain().getCodeSource());
    }

    /**
     * Returns the parent folder of the JAR file at {@code codeSource}'s location,
     * or the current folder if the location is unknown or is not a file.
     */
    static Path getHomeFolder(CodeSource codeSource) {
        if (codeSource == null || codeSource.getLocation() == null) {
            return Paths.get("");
        }
        try {
            Path location = Paths.get(codeSource.getLocation().toURI());
            if (Files.isRegularFile(location)) {
                return location.getParent();
            }
        } catch (URISyntaxException | IllegalArgumentException | FileSystemNotFoundException e) {
            // Falls back to the current folder below
        }
        return Paths.get("");
    }

    /**
     * Creates a file if it does not exist along with its missing parent directories.
     *
     * @throws IOException if the file or directory cannot be created.
     */
    public static void createIfMissing(Path file) throws IOException {
        if (Files.exists(file)) {
            return;
        }

        createParentDirsOfFile(file);

        Files.createFile(file);
    }

    /**
     * Creates parent directories of file if it has a parent directory.
     */
    private static void createParentDirsOfFile(Path file) throws IOException {
        Path parentDir = file.getParent();

        if (parentDir != null) {
            Files.createDirectories(parentDir);
        }
    }

    /**
     * Returns the UTF-8 contents of an existing file.
     */
    public static String readFromFile(Path file) throws IOException {
        return new String(Files.readAllBytes(file), CHARSET);
    }

    /**
     * Writes given string to a file.
     * Will create the file if it does not exist yet.
     */
    public static void writeToFile(Path file, String content) throws IOException {
        Files.write(file, content.getBytes(CHARSET));
    }

}
