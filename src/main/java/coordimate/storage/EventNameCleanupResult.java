package coordimate.storage;

import java.util.List;

/**
 * Changes committed by the startup event cleanup.
 */
public record EventNameCleanupResult(List<String> deletedNames, List<Rename> renamedEvents,
        List<String> yearZeroDeletedNames) {
    /**
     * Creates a result containing only event-name changes.
     */
    public EventNameCleanupResult(List<String> deletedNames, List<Rename> renamedEvents) {
        this(deletedNames, renamedEvents, List.of());
    }

    /**
     * Copies the changes so the committed result cannot be modified by its caller.
     */
    public EventNameCleanupResult {
        deletedNames = List.copyOf(deletedNames);
        renamedEvents = List.copyOf(renamedEvents);
        yearZeroDeletedNames = List.copyOf(yearZeroDeletedNames);
    }

    public boolean isEmpty() {
        return deletedNames.isEmpty() && renamedEvents.isEmpty() && yearZeroDeletedNames.isEmpty();
    }

    /**
     * A saved event renamed to keep it distinct after Unicode whitespace normalization.
     */
    public record Rename(String originalName, String newName) {}
}
