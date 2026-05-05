package dev.example.likes.database;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Single-threaded executor for all SQLite write operations.
 * <p>
 * Serializes all writes through one background thread to avoid SQLite write
 * conflicts. Each write task is submitted as a {@link CompletableFuture} so
 * callers can chain main-thread callbacks.
 * </p>
 * <p>
 * <strong>Important:</strong> Never call Bukkit/Paper API directly from tasks
 * submitted to this executor. Capture all required values on the main thread
 * before submitting.
 * </p>
 */
public final class DatabaseWriteExecutor {

    private static final Logger LOGGER = Logger.getLogger(DatabaseWriteExecutor.class.getName());

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "likes-db-writer"));

    /**
     * Submits a write task and returns a CompletableFuture for the result.
     *
     * @param task the write task; may throw any checked exception
     * @param <T>  the result type
     * @return a future that completes with the task result or exceptionally on
     *         failure
     */
    public <T> CompletableFuture<T> submit(Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                future.complete(task.call());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Submits a void write task and returns a CompletableFuture&lt;Void&gt;.
     *
     * @param task the write task
     * @return a future that completes when the task finishes
     */
    public CompletableFuture<Void> submit(ThrowingRunnable task) {
        return submit(() -> {
            task.run();
            return null;
        });
    }

    /**
     * Shuts down the executor gracefully.
     * Waits up to 5 seconds for in-flight tasks to finish before forcing shutdown.
     * Must be called on plugin disable.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                LOGGER.warning("DatabaseWriteExecutor did not terminate within 5 s; forcing shutdown.");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    /**
     * A {@link Runnable}-like functional interface that can throw checked
     * exceptions.
     */
    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}
