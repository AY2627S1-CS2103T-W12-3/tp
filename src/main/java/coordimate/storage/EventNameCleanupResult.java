package coordimate.storage;

import java.util.List;

/** Changes committed by the startup event-name migration. */
public record EventNameCleanupResult(List<String> deletedNames, List<Rename> renamedEvents) {
    /** Copies the changes so the committed result cannot be modified by its caller. */
    public EventNameCleanupResult {
        deletedNames = List.copyOf(deletedNames);
        renamedEvents = List.copyOf(renamedEvents);
    }

    public boolean isEmpty() {
        return deletedNames.isEmpty() && renamedEvents.isEmpty();
    }

    /** A saved event renamed to keep it distinct after Unicode whitespace normalization. */
    public record Rename(String originalName, String newName) {}
}
