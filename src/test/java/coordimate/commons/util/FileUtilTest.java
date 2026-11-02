package coordimate.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.cert.Certificate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class FileUtilTest {

    private static final Path CURRENT_FOLDER = Paths.get("");

    @TempDir
    public Path testFolder;

    @Test
    public void getHomeFolder_jarFile_returnsJarFolder() throws IOException {
        Path jarFile = Files.createFile(testFolder.resolve("coordimate.jar"));
        assertEquals(testFolder, FileUtil.getHomeFolder(codeSourceAt(jarFile.toUri().toURL())));
    }

    @Test
    public void getHomeFolder_classesFolder_returnsCurrentFolder() throws MalformedURLException {
        assertEquals(CURRENT_FOLDER, FileUtil.getHomeFolder(codeSourceAt(testFolder.toUri().toURL())));
    }

    @Test
    public void getHomeFolder_missingFile_returnsCurrentFolder() throws MalformedURLException {
        URL missingJar = testFolder.resolve("missing.jar").toUri().toURL();
        assertEquals(CURRENT_FOLDER, FileUtil.getHomeFolder(codeSourceAt(missingJar)));
    }

    @Test
    public void getHomeFolder_unknownLocation_returnsCurrentFolder() {
        assertEquals(CURRENT_FOLDER, FileUtil.getHomeFolder((CodeSource) null));
        assertEquals(CURRENT_FOLDER, FileUtil.getHomeFolder(codeSourceAt(null)));
    }

    @Test
    public void getHomeFolder_invalidFileLocation_returnsCurrentFolder() throws MalformedURLException {
        // A file URI with a query component cannot be converted to a path
        URL invalidLocation = URI.create("file:/coordimate.jar?query").toURL();
        assertEquals(CURRENT_FOLDER, FileUtil.getHomeFolder(codeSourceAt(invalidLocation)));
    }

    @Test
    public void getHomeFolder_notRunningFromJar_returnsCurrentFolder() {
        // Tests run from compiled class folders, not from a JAR file
        assertEquals(CURRENT_FOLDER, FileUtil.getHomeFolder());
    }

    private static CodeSource codeSourceAt(URL location) {
        return new CodeSource(location, (Certificate[]) null);
    }
}
