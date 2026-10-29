package coordimate.storage;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Holds the shared data-file lock and checks revisions before replacement.
 */
final class DataFileGuard implements AutoCloseable {
    private final Path dataPath;
    private final FileChannel channel;
    private final FileLock lock;

    DataFileGuard(Path dataPath) throws IOException {
        this.dataPath = dataPath.toAbsolutePath();
        Files.createDirectories(this.dataPath.getParent());
        Path lockPath = this.dataPath.resolveSibling(this.dataPath.getFileName() + ".lock");
        channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            lock = channel.lock();
        } catch (IOException | RuntimeException e) {
            channel.close();
            if (e instanceof OverlappingFileLockException) {
                throw new IOException("The data file is being saved by another window. Try again.", e);
            }
            throw e;
        }
    }

    void checkRevision(Optional<String> expectedRevision) throws IOException {
        if (!expectedRevision.equals(readRevision(dataPath))) {
            throw new DataConflictException();
        }
    }

    /**
     * Returns the current content digest, or an empty optional when the data file is absent.
     */
    static Optional<String> readRevision(Path dataPath) throws IOException {
        try {
            return Optional.of(revisionOf(Files.readAllBytes(dataPath)));
        } catch (NoSuchFileException e) {
            return Optional.empty();
        }
    }

    /**
     * Returns the SHA-256 digest used to detect changes to the saved data.
     */
    static String revisionOf(byte[] contents) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(contents));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 must be available", e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            lock.release();
        } finally {
            channel.close();
        }
    }
}
