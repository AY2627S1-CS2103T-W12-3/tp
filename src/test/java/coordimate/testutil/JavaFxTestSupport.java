package coordimate.testutil;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;

import javafx.application.Platform;

/**
 * Shared JavaFX toolkit startup and FX-thread execution helper for UI tests.
 */
public final class JavaFxTestSupport {

    public static final long DEFAULT_TIMEOUT_SECONDS = 10;

    private JavaFxTestSupport() {
    }

    /**
     * Starts the JavaFX toolkit if it is not already running, skipping the test when headless.
     * Applies {@code Platform.setImplicitExit(false)} so later test classes in the same JVM
     * can keep reusing the toolkit instead of it exiting when a stage closes.
     */
    public static void startToolkit() throws InterruptedException {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "JavaFX UI tests require a display");
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
        } catch (IllegalStateException alreadyRunning) {
            Platform.runLater(started::countDown);
        }
        assertTrue(started.await(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        Platform.setImplicitExit(false);
    }

    /**
     * Runs {@code action} on the FX application thread and blocks for its result,
     * using the {@link #DEFAULT_TIMEOUT_SECONDS} default timeout.
     */
    public static <T> T runOnFxThreadAndWait(Callable<T> action) throws Exception {
        return runOnFxThreadAndWait(action, DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Runs {@code action} on the FX application thread and blocks for its result until
     * {@code timeout}. Rejects reentrant calls from the FX thread itself, since those would
     * deadlock waiting for a task queued behind the caller.
     */
    public static <T> T runOnFxThreadAndWait(Callable<T> action, long timeout, TimeUnit unit) throws Exception {
        if (Platform.isFxApplicationThread()) {
            throw new IllegalStateException("runOnFxThreadAndWait must not be called from the FX application "
                    + "thread; it would block waiting for a task queued behind the caller");
        }
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(timeout, unit);
    }

    /**
     * {@link #runOnFxThreadAndWait(Callable, long, TimeUnit)} for actions with no result,
     * using the {@link #DEFAULT_TIMEOUT_SECONDS} default timeout.
     */
    public static void runOnFxThreadAndWait(FxAction action) throws Exception {
        runOnFxThreadAndWait(() -> {
            action.run();
            return null;
        });
    }

    /**
     * A FX-thread action that performs assertions and may throw, mirroring the body of the
     * {@code FutureTask<Void>} lambdas this helper replaces.
     */
    @FunctionalInterface
    public interface FxAction {
        void run() throws Exception;
    }
}
